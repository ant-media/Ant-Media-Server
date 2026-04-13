package org.red5.server.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.classloading.ServerClassLoader;
import org.red5.server.tomcat.TomcatApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import jakarta.servlet.Servlet;
import jakarta.ws.rs.Path;
import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Handles plugin installation via ZIP upload, hot-loading Spring beans into active webapp
 * contexts, and Jersey reload for REST endpoints.
 */
public class PluginDeployer {

    private static final Logger log = LoggerFactory.getLogger(PluginDeployer.class);

    public static final String MANIFEST_PLUGIN_NAME = "AMS-Plugin-Name";
    public static final String MANIFEST_PLUGIN_VERSION = "AMS-Plugin-Version";
    public static final String MANIFEST_PLUGIN_AUTHOR = "AMS-Plugin-Author";
    public static final String MANIFEST_PLUGIN_REQUIRES_VERSION = "AMS-Plugin-Requires-Version";
    public static final String MANIFEST_PLUGIN_DESCRIPTION = "AMS-Plugin-Description";
    public static final String MANIFEST_PLUGIN_REQUIRES_RESTART = "AMS-Plugin-Requires-Restart";
    public static final String MANIFEST_PLUGIN_INSTALL_SCRIPT = "AMS-Plugin-Install-Script";
    public static final String SCAN_BASE_PACKAGE = "io.antmedia";

    private final ConcurrentHashMap<String, List<String>> springPluginBeanNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PluginRecord> pluginRecords = new ConcurrentHashMap<>();

    /**
     * Deploys a plugin from a ZIP file. Extracts, validates manifest, runs install.sh if
     * present, copies jar to {@code {pluginsDir}/{pluginId}.jar}, and either hot-loads or
     * defers to next restart based on the manifest's {@code AMS-Plugin-Requires-Restart}.
     */
    public Result loadPluginFromZip(File zipFile, File pluginsDir) {
        File extractDir = null;
        try {
            extractDir = extractZip(zipFile);
            if (extractDir == null) {
                return new Result(false, "Failed to extract ZIP file");
            }

            File pluginJar = new File(extractDir, "plugin.jar");
            if (!pluginJar.exists()) {
                return new Result(false, "plugin.jar not found in ZIP");
            }

            Result validationResult = validateManifest(pluginJar);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }

            Attributes attrs = readManifestAttributes(pluginJar);
            String pluginName = attrs.getValue(MANIFEST_PLUGIN_NAME);
            String pluginVersion = attrs.getValue(MANIFEST_PLUGIN_VERSION);
            boolean requiresRestart = "true".equalsIgnoreCase(attrs.getValue(MANIFEST_PLUGIN_REQUIRES_RESTART));

            if (isDuplicateInstall(pluginName)) {
                return new Result(false, "Plugin already installed: " + pluginName);
            }

            PluginRecord record = buildPluginRecord(attrs);
            record.setState(PluginState.INSTALLING);
            pluginRecords.put(pluginName, record);

            String pluginId = record.getPluginId();

            // Copy artifacts first, then run install.sh — so the script can see the
            // final jar at plugins/{pluginId}.jar and delete/move it if needed
            // (e.g. LL-HLS moves it to WEB-INF/lib/ and removes it from plugins/).
            File jarInPluginsDir = new File(pluginsDir, pluginId + ".jar");
            copyFile(pluginJar, jarInPluginsDir);

            File canonicalDir = new File(pluginsDir, pluginId);
            canonicalDir.mkdirs();

            File uninstallScript = new File(extractDir, "uninstall.sh");
            if (uninstallScript.exists()) {
                copyFile(uninstallScript, new File(canonicalDir, "uninstall.sh"));
            }

            File assetsDir = new File(extractDir, "assets");
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                copyDirectory(assetsDir, new File(canonicalDir, "assets"));
            }

            // install.sh runs AFTER artifacts are copied — it gets the last word on
            // where the jar lives. PLUGIN_JAR points to plugins/{pluginId}.jar.
            File installScript = new File(extractDir, "install.sh");
            if (installScript.exists()) {
                int exitCode = runInstallScript(installScript, extractDir, pluginName, pluginVersion,
                        jarInPluginsDir, pluginId);
                if (exitCode != 0) {
                    record.setState(PluginState.FAILED);
                    record.setLastError("install.sh exited with code " + exitCode);
                    return new Result(false, "install.sh failed with exit code " + exitCode);
                }
            }

            if (requiresRestart) {
                record.setState(PluginState.INSTALLED_PENDING_RESTART);
                log.info("Plugin {} installed. Server restart required to activate.", pluginName);
                return new Result(true, "Plugin installed. Server restart required to activate.");
            }

            Result loadResult = loadPlugin(jarInPluginsDir);
            if (loadResult.isSuccess()) {
                record.setState(PluginState.ACTIVE);
            } else {
                record.setState(PluginState.FAILED);
                record.setLastError(loadResult.getMessage());
            }
            return loadResult;

        } catch (Exception e) {
            log.error("Error loading plugin from ZIP", e);
            return new Result(false, "Error: " + e.getMessage());
        } finally {
            if (extractDir != null) {
                deleteDirectory(extractDir);
            }
        }
    }

    /**
     * Unloads a plugin installed via ZIP: destroys beans, runs uninstall.sh, deletes
     * the jar and the metadata subdirectory.
     */
    public Result unloadPluginFromZip(String pluginName, File pluginsDir) {
        PluginRecord record = pluginRecords.get(pluginName);
        String pluginId = record != null ? record.getPluginId() : slugify(pluginName);

        Result unloadResult = unloadPlugin(pluginId);

        if (record != null) {
            String pid = record.getPluginId();
            File canonicalDir = new File(pluginsDir, pid);
            File uninstallSh = new File(canonicalDir, "uninstall.sh");
            File flatJar = new File(pluginsDir, pid + ".jar");

            if (uninstallSh.exists()) {
                int exitCode = runInstallScript(uninstallSh, canonicalDir, record.getName(),
                        record.getVersion(), flatJar, pid);
                if (exitCode != 0) {
                    log.warn("uninstall.sh for {} exited with code {}", pluginName, exitCode);
                }
            }

            if (flatJar.exists() && !flatJar.delete()) {
                log.warn("Failed to delete plugin jar: {}", flatJar.getAbsolutePath());
            }
            deleteDirectory(canonicalDir);
            record.setState(PluginState.UNINSTALLED);
        }

        pluginRecords.remove(pluginName);
        return new Result(true, unloadResult.isSuccess()
                ? "Plugin removed: " + pluginName
                : "Plugin removed (was not active): " + pluginName);
    }

    /**
     * Scans a jar for Spring @Component classes, registers them as beans in each streaming
     * webapp context, and reloads Jersey if any have @Path annotations.
     */
    Result loadPlugin(File jarFile) {
        String pluginId = jarFile.getName().replaceAll("\\.jar$", "");

        if (springPluginBeanNames.containsKey(pluginId)) {
            return new Result(false, "Plugin already loaded: " + pluginId);
        }

        if (!isSystemClassLoaderServerClassLoader()) {
            return new Result(false, "Plugin loading requires ServerClassLoader as system classloader");
        }

        URL jarUrl;
        try {
            jarUrl = jarFile.toURI().toURL();
        } catch (Exception e) {
            return new Result(false, "Invalid JAR path: " + e.getMessage());
        }

        addJarToSystemClassLoader(jarUrl);

        URLClassLoader scanCL = new URLClassLoader(new URL[] { jarUrl }, ClassLoader.getSystemClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return findResources(name);
            }
        };

        Set<BeanDefinition> components;
        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false) {
                        @Override
                        protected boolean isCandidateComponent(
                                org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                            return beanDefinition.getMetadata().isIndependent();
                        }
                    };
            scanner.setResourceLoader(
                    new org.springframework.core.io.support.PathMatchingResourcePatternResolver(scanCL));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
            components = scanner.findCandidateComponents(SCAN_BASE_PACKAGE);
        } catch (Exception e) {
            log.error("Error scanning JAR {}", jarFile.getName(), e);
            closeSilently(scanCL);
            return new Result(false, "Error scanning JAR: " + e.getMessage());
        }
        closeSilently(scanCL);

        if (components.isEmpty()) {
            return new Result(false, "No @Component classes found in " + jarFile.getName());
        }

        ClassLoader systemCL = ClassLoader.getSystemClassLoader();
        List<String> registeredBeanNames = new ArrayList<>();
        List<Class<?>> restClasses = new ArrayList<>();

        for (BeanDefinition def : components) {
            String className = def.getBeanClassName();
            if (className == null) continue;

            Class<?> clazz;
            try {
                clazz = Class.forName(className, true, systemCL);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.warn("Cannot load class {} from plugin: {}", className, e.getMessage());
                continue;
            }

            String beanName = resolveBeanName(clazz);
            if (clazz.getAnnotation(Path.class) != null) {
                restClasses.add(clazz);
            }

            boolean registered = false;
            for (IApplicationContext appCtx : getApplicationContexts().values()) {
                if (!(appCtx instanceof TomcatApplicationContext)) continue;
                TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
                String ctxPath = tomcatCtx.getContext().getPath();
                if (ctxPath == null || ctxPath.isEmpty()) continue;
                ApplicationContext springCtx = tomcatCtx.getSpringContext();
                if (springCtx == null) continue;

                try {
                    AutowireCapableBeanFactory bf = springCtx.getAutowireCapableBeanFactory();
                    Object instance = bf.createBean(clazz);
                    if (bf instanceof SingletonBeanRegistry) {
                        ((SingletonBeanRegistry) bf).registerSingleton(beanName, instance);
                    } else {
                        continue;
                    }
                    registered = true;
                    log.info("Registered bean '{}' in context '{}'", beanName, ctxPath);
                } catch (Exception e) {
                    log.error("Failed to register bean {} in {}: {}", beanName, ctxPath, e.getMessage(), e);
                }
            }

            if (registered && !registeredBeanNames.contains(beanName)) {
                registeredBeanNames.add(beanName);
            }
        }

        if (registeredBeanNames.isEmpty()) {
            return new Result(false, "No beans could be registered for plugin " + pluginId);
        }

        springPluginBeanNames.put(pluginId, registeredBeanNames);

        if (!restClasses.isEmpty()) {
            reloadJersey(restClasses);
        }

        log.info("Loaded plugin '{}': {} beans, {} REST classes", pluginId, registeredBeanNames.size(), restClasses.size());
        return new Result(true);
    }

    /**
     * Destroys singleton beans for a plugin in each webapp context.
     */
    Result unloadPlugin(String pluginId) {
        List<String> beanNames = springPluginBeanNames.get(pluginId);
        if (beanNames == null) {
            return new Result(false, "Plugin not found: " + pluginId);
        }

        for (IApplicationContext appCtx : getApplicationContexts().values()) {
            if (!(appCtx instanceof TomcatApplicationContext)) continue;
            TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
            String ctxPath = tomcatCtx.getContext().getPath();
            if (ctxPath == null || ctxPath.isEmpty()) continue;
            ApplicationContext springCtx = tomcatCtx.getSpringContext();
            if (springCtx == null) continue;

            try {
                AutowireCapableBeanFactory bf = springCtx.getAutowireCapableBeanFactory();
                if (bf instanceof SingletonBeanRegistry) {
                    for (String beanName : beanNames) {
                        try {
                            ((org.springframework.beans.factory.support.DefaultSingletonBeanRegistry) bf)
                                    .destroySingleton(beanName);
                        } catch (Exception e) {
                            log.warn("Failed to destroy bean {} in {}: {}", beanName, ctxPath, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error unloading plugin from {}: {}", ctxPath, e.getMessage());
            }
        }

        springPluginBeanNames.remove(pluginId);
        log.info("Unloaded plugin: {}", pluginId);
        return new Result(true);
    }

    /**
     * Creates a new ResourceConfig from the existing Jersey config plus the new classes,
     * then reloads the Jersey servlet container in each streaming webapp. This gives
     * hot-loaded REST classes the same URL as startup-discovered ones.
     */
    private void reloadJersey(List<Class<?>> newClasses) {
        for (IApplicationContext appCtx : getApplicationContexts().values()) {
            if (!(appCtx instanceof TomcatApplicationContext)) continue;
            TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
            org.apache.catalina.Context catalinaCtx = tomcatCtx.getContext();
            String ctxPath = catalinaCtx.getPath();
            if (ctxPath == null || ctxPath.isEmpty()) continue;

            try {
                if (!(catalinaCtx instanceof StandardContext)) continue;
                Container wrapper = ((StandardContext) catalinaCtx).findChild("jersey-serlvet");
                if (!(wrapper instanceof Wrapper)) continue;

                Servlet servlet = ((Wrapper) wrapper).getServlet();
                if (!(servlet instanceof ServletContainer)) continue;

                ServletContainer jerseyContainer = (ServletContainer) servlet;
                ResourceConfig oldConfig = jerseyContainer.getConfiguration();

                ResourceConfig newConfig = new ResourceConfig();
                newConfig.registerClasses(oldConfig.getClasses());
                newConfig.registerInstances(oldConfig.getSingletons());
                newConfig.registerResources(oldConfig.getResources());
                newConfig.addProperties(oldConfig.getProperties());

                for (Class<?> cls : newClasses) {
                    newConfig.register(cls);
                }
                jerseyContainer.reload(newConfig);
                log.info("Reloaded Jersey in context '{}' with {} new REST classes", ctxPath, newClasses.size());

            } catch (Exception e) {
                log.error("Failed to reload Jersey in '{}': {}", ctxPath, e.getMessage(), e);
            }
        }
    }

    Result validateManifest(File jarFile) {
        Attributes attrs = readManifestAttributes(jarFile);
        if (attrs == null) {
            return new Result(false, "Cannot read MANIFEST.MF from plugin.jar");
        }
        if (attrs.getValue(MANIFEST_PLUGIN_NAME) == null || attrs.getValue(MANIFEST_PLUGIN_NAME).isEmpty()) {
            return new Result(false, "Missing " + MANIFEST_PLUGIN_NAME + " in MANIFEST.MF");
        }
        if (attrs.getValue(MANIFEST_PLUGIN_VERSION) == null) {
            return new Result(false, "Missing " + MANIFEST_PLUGIN_VERSION + " in MANIFEST.MF");
        }
        if (attrs.getValue(MANIFEST_PLUGIN_AUTHOR) == null) {
            return new Result(false, "Missing " + MANIFEST_PLUGIN_AUTHOR + " in MANIFEST.MF");
        }
        String requiresVersion = attrs.getValue(MANIFEST_PLUGIN_REQUIRES_VERSION);
        if (requiresVersion != null && !requiresVersion.isEmpty()) {
            Version amsVersion = RestServiceBase.getSoftwareVersion();
            if (amsVersion != null && !isVersionCompatible(amsVersion.getVersionName(), requiresVersion)) {
                return new Result(false, "Plugin requires AMS >= " + requiresVersion
                        + ", current version is " + amsVersion.getVersionName());
            }
        }
        return new Result(true);
    }

    PluginRecord buildPluginRecord(Attributes attrs) {
        PluginRecord record = new PluginRecord();
        record.setName(attrs.getValue(MANIFEST_PLUGIN_NAME));
        record.setVersion(attrs.getValue(MANIFEST_PLUGIN_VERSION));
        record.setAuthor(attrs.getValue(MANIFEST_PLUGIN_AUTHOR));
        record.setDescription(attrs.getValue(MANIFEST_PLUGIN_DESCRIPTION));
        record.setRequiresVersion(attrs.getValue(MANIFEST_PLUGIN_REQUIRES_VERSION));
        record.setRequiresRestart("true".equalsIgnoreCase(attrs.getValue(MANIFEST_PLUGIN_REQUIRES_RESTART)));
        record.setPluginId(slugify(record.getName()) + "-" + record.getVersion());
        return record;
    }

    boolean isDuplicateInstall(String pluginName) {
        PluginRecord existing = pluginRecords.get(pluginName);
        if (existing == null) return false;
        PluginState state = existing.getState();
        return state != PluginState.FAILED && state != PluginState.UNINSTALLED;
    }

    static String resolveBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    static String resolveRestKey(Class<?> clazz) {
        Path pathAnnotation = clazz.getAnnotation(Path.class);
        if (pathAnnotation == null) return null;
        String pathValue = pathAnnotation.value().replaceAll("^/+", "").replaceAll("/+$", "");
        if (pathValue.isEmpty()) return null;
        String[] segments = pathValue.split("/");
        return segments[segments.length - 1];
    }

    static boolean isVersionCompatible(String currentVersion, String requiredVersion) {
        if (currentVersion == null || requiredVersion == null) return true;
        try {
            int[] current = parseVersion(currentVersion);
            int[] required = parseVersion(requiredVersion);
            for (int i = 0; i < Math.min(current.length, required.length); i++) {
                if (current[i] > required[i]) return true;
                if (current[i] < required[i]) return false;
            }
            return current.length >= required.length;
        } catch (NumberFormatException e) {
            log.warn("Cannot compare versions: {} vs {}", currentVersion, requiredVersion);
            return true;
        }
    }

    static String slugify(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    public Set<String> getPluginNames() {
        return Collections.unmodifiableSet(springPluginBeanNames.keySet());
    }

    public PluginRecord getPluginRecord(String pluginName) {
        return pluginRecords.get(pluginName);
    }

    public List<PluginRecord> getAllPluginRecords() {
        return new ArrayList<>(pluginRecords.values());
    }

    protected boolean isSystemClassLoaderServerClassLoader() {
        return ClassLoader.getSystemClassLoader() instanceof ServerClassLoader;
    }

    protected void addJarToSystemClassLoader(URL jarUrl) {
        ((ServerClassLoader) ClassLoader.getSystemClassLoader()).addPluginJar(jarUrl);
    }

    protected Map<String, IApplicationContext> getApplicationContexts() {
        return LoaderBase.getRed5ApplicationContexts();
    }

    static Attributes readManifestAttributes(File jarFile) {
        try (JarFile jar = new JarFile(jarFile, false)) {
            Manifest manifest = jar.getManifest();
            return manifest != null ? manifest.getMainAttributes() : null;
        } catch (IOException e) {
            log.warn("Error reading manifest from {}", jarFile.getName(), e);
            return null;
        }
    }

    File extractZip(File zipFile) {
        try {
            java.nio.file.Path tempDir = Files.createTempDirectory("ams-plugin-");
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = new File(tempDir.toFile(), entry.getName());
                    if (!outFile.getCanonicalPath().startsWith(tempDir.toFile().getCanonicalPath())) {
                        log.error("Zip slip detected: {}", entry.getName());
                        deleteDirectory(tempDir.toFile());
                        return null;
                    }
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (OutputStream os = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = zis.read(buf)) > 0) {
                                os.write(buf, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            return tempDir.toFile();
        } catch (IOException e) {
            log.error("Error extracting ZIP", e);
            return null;
        }
    }

    int runInstallScript(File script, File workDir, String pluginName, String pluginVersion,
                         File pluginJar, String pluginId) {
        String amsHome = System.getProperty("red5.root", "/usr/local/antmedia");
        File assetsDir = new File(workDir, "assets");

        script.setExecutable(true);
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        pb.environment().put("AMS_HOME", amsHome);
        pb.environment().put("AMS_PLUGINS_DIR", amsHome + "/plugins");
        pb.environment().put("AMS_WEBAPPS_DIR", amsHome + "/webapps");
        pb.environment().put("PLUGIN_NAME", pluginName);
        pb.environment().put("PLUGIN_VERSION", pluginVersion);
        pb.environment().put("PLUGIN_JAR", pluginJar.getAbsolutePath());
        pb.environment().put("PLUGIN_ASSETS_DIR", assetsDir.getAbsolutePath());
        if (pluginId != null) {
            pb.environment().put("PLUGIN_ID", pluginId);
        }

        try {
            Process process = pb.start();
            try (InputStream is = process.getInputStream()) {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) { /* drain */ }
            }
            int exitCode = process.waitFor();
            log.info("{} exited with code {}", script.getName(), exitCode);
            return exitCode;
        } catch (Exception e) {
            log.error("Error running {}", script.getName(), e);
            return -1;
        }
    }

    private static int[] parseVersion(String version) {
        String clean = version.replaceAll("[^0-9.]", "");
        String[] parts = clean.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
        }
        return result;
    }

    private void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private void copyDirectory(File srcDir, File destDir) throws IOException {
        destDir.mkdirs();
        File[] files = srcDir.listFiles();
        if (files != null) {
            for (File f : files) {
                File dest = new File(destDir, f.getName());
                if (f.isDirectory()) {
                    copyDirectory(f, dest);
                } else {
                    copyFile(f, dest);
                }
            }
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void closeSilently(URLClassLoader cl) {
        try { cl.close(); } catch (IOException ignored) { }
    }
}
