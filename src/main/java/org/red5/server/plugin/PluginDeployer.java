package org.red5.server.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
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
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.plugin.api.AmsPlugin;
import io.antmedia.plugin.api.IServerListener;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.plugin.api.Inject;
import io.antmedia.plugin.api.Listener;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.plugin.api.Rest;

import io.antmedia.rest.PluginRestDispatcher;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;
import jakarta.ws.rs.Path;

/**
 * V2 Plugin Deployer: loads plugin JARs/ZIPs at runtime using custom AMS annotations
 * ({@code @AmsPlugin}, {@code @Listener}, {@code @Rest}, {@code @Inject}).
 *
 * Adds the JAR to {@link ServerClassLoader}, scans for V2 annotations, instantiates
 * classes, resolves {@code @Inject} fields from webapp Spring contexts, and registers
 * listeners and REST endpoints in every active streaming webapp.
 */
public class PluginDeployer {

    private static final Logger log = LoggerFactory.getLogger(PluginDeployer.class);

    public static final String MANIFEST_PLUGIN_NAME = "AMS-Plugin-Name";
    public static final String MANIFEST_PLUGIN_VERSION = "AMS-Plugin-Version";
    public static final String MANIFEST_PLUGIN_AUTHOR = "AMS-Plugin-Author";
    public static final String MANIFEST_PLUGIN_REQUIRES_VERSION = "AMS-Plugin-Requires-Version";
    public static final String MANIFEST_PLUGIN_DESCRIPTION = "AMS-Plugin-Description";
    public static final String MANIFEST_PLUGIN_LOADING_MODE = "AMS-Plugin-Loading-Mode";
    public static final String MANIFEST_PLUGIN_REQUIRES_RESTART = "AMS-Plugin-Requires-Restart";
    public static final String MANIFEST_PLUGIN_INSTALL_SCRIPT = "AMS-Plugin-Install-Script";

    public static final String LOADING_MODE_HOTLOAD = "HOTLOAD";
    public static final String LOADING_MODE_WEBAPP_LIB = "WEBAPP_LIB";

    private final ConcurrentHashMap<String, PluginRecord> pluginRecords = new ConcurrentHashMap<>();

    // pluginName → list of REST dispatcher keys registered (for cleanup on unload)
    private final ConcurrentHashMap<String, List<String>> pluginRestKeys = new ConcurrentHashMap<>();

    // pluginName → list of instantiated listener/rest objects per webapp context (for cleanup)
    private final ConcurrentHashMap<String, List<Object>> pluginInstances = new ConcurrentHashMap<>();

    /**
     * Hot-loads a plugin JAR using V2 annotations.
     * Scans the JAR for @AmsPlugin, @Listener, @Rest, instantiates them,
     * resolves @Inject fields from each webapp's Spring context, and registers
     * listeners and REST endpoints.
     */
    public Result loadPlugin(File jarFile) {
        String pluginId = jarFile.getName().replaceAll("\\.jar$", "");

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

        // Scan the JAR for V2 annotated classes
        ClassLoader systemCL = ClassLoader.getSystemClassLoader();
        Class<?> mainClass = null;
        List<Class<?>> listenerClasses = new ArrayList<>();
        List<Class<?>> restClasses = new ArrayList<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                String className = name.replace('/', '.').replace(".class", "");
                try {
                    Class<?> cls = Class.forName(className, true, systemCL);
                    if (cls.isAnnotationPresent(AmsPlugin.class)) {
                        if (mainClass != null) {
                            return new Result(false, "Multiple @AmsPlugin classes found in " + jarFile.getName());
                        }
                        mainClass = cls;
                    }
                    if (cls.isAnnotationPresent(Listener.class)) {
                        listenerClasses.add(cls);
                    }
                    if (cls.isAnnotationPresent(Rest.class)) {
                        restClasses.add(cls);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    log.warn("Cannot load class {} from plugin: {}", className, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error scanning JAR {}", jarFile.getName(), e);
            return new Result(false, "Error scanning JAR: " + e.getMessage());
        }

        if (mainClass == null) {
            return new Result(false, "No @AmsPlugin class found in " + jarFile.getName());
        }

        // The @AmsPlugin class might also be a @Listener — make sure it's in the list
        if (mainClass.isAnnotationPresent(Listener.class) && !listenerClasses.contains(mainClass)) {
            listenerClasses.add(mainClass);
        }

        // Instantiate and register in every active webapp context
        List<Object> allInstances = new ArrayList<>();
        List<String> registeredRestKeys = new ArrayList<>();

        for (IApplicationContext appCtx : getApplicationContexts().values()) {
            if (!(appCtx instanceof TomcatApplicationContext)) {
                continue;
            }
            TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
            if (tomcatCtx.getContext().getPath().isEmpty()) {
                continue; // skip admin console (root)
            }
            ApplicationContext springCtx = tomcatCtx.getSpringContext();
            String ctxPath = tomcatCtx.getContext().getPath();

            // Instantiate @Listener classes
            for (Class<?> listenerClass : listenerClasses) {
                try {
                    Object instance = listenerClass.getDeclaredConstructor().newInstance();
                    injectFields(instance, springCtx);

                    // Register based on implemented interfaces
                    IAntMediaStreamHandler app = (IAntMediaStreamHandler) springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
                    if (instance instanceof IStreamListener) {
                        app.addStreamListener((IStreamListener) instance);
                    }
                    // IFrameListener and IPacketListener are registered per-stream
                    // by the plugin in its streamStarted() — we just need to keep the instance alive

                    allInstances.add(instance);
                    log.info("Registered @Listener '{}' in context '{}'", listenerClass.getSimpleName(), ctxPath);
                } catch (Exception e) {
                    log.error("Failed to instantiate @Listener '{}' in context '{}'",
                            listenerClass.getSimpleName(), ctxPath, e);
                }
            }

            // Instantiate @Rest classes
            for (Class<?> restClass : restClasses) {
                try {
                    Object instance = restClass.getDeclaredConstructor().newInstance();
                    injectFields(instance, springCtx);

                    // Also inject any @Listener instances the REST class depends on
                    injectPluginInstances(instance, allInstances);

                    Path pathAnnotation = restClass.getAnnotation(Path.class);
                    if (pathAnnotation != null) {
                        String pathValue = pathAnnotation.value().replaceAll("^/+", "");
                        String[] segments = pathValue.split("/");
                        String restKey = segments[segments.length - 1];

                        PluginRestDispatcher.registerHandler(ctxPath, restKey, instance);
                        if (!registeredRestKeys.contains(restKey)) {
                            registeredRestKeys.add(restKey);
                        }
                        log.info("Registered @Rest '{}' at /plugins/{} in context '{}'",
                                restClass.getSimpleName(), restKey, ctxPath);
                    }
                    allInstances.add(instance);
                } catch (Exception e) {
                    log.error("Failed to instantiate @Rest '{}' in context '{}'",
                            restClass.getSimpleName(), ctxPath, e);
                }
            }
        }

        // Call onPluginEnabled on @AmsPlugin class if it implements IServerListener
        for (Object inst : allInstances) {
            if (inst.getClass().equals(mainClass) && inst instanceof IServerListener) {
                try {
                    ((IServerListener) inst).onPluginEnabled(pluginId);
                } catch (Exception e) {
                    log.error("Error calling onPluginEnabled on {}", mainClass.getSimpleName(), e);
                }
                break; // only call once
            }
        }

        pluginInstances.put(pluginId, allInstances);
        if (!registeredRestKeys.isEmpty()) {
            pluginRestKeys.put(pluginId, registeredRestKeys);
        }

        log.info("Loaded plugin '{}': {} listeners, {} REST endpoints",
                pluginId, listenerClasses.size(), restClasses.size());
        return new Result(true);
    }

    /**
     * Unloads a plugin: removes stream listeners, unregisters REST handlers, calls onPluginDisabled.
     */
    public Result unloadPlugin(String pluginName) {
        String pluginId = slugify(pluginName);
        List<Object> instances = pluginInstances.get(pluginId);
        if (instances == null) {
            // try exact name as well
            instances = pluginInstances.get(pluginName);
            if (instances == null) {
                return new Result(false, "Plugin not found: " + pluginName);
            }
            pluginId = pluginName;
        }

        // Call onPluginDisabled
        for (Object inst : instances) {
            if (inst instanceof IServerListener) {
                try {
                    ((IServerListener) inst).onPluginDisabled(pluginId);
                } catch (Exception e) {
                    log.warn("Error calling onPluginDisabled on {}", inst.getClass().getSimpleName(), e);
                }
            }
        }

        // Remove stream listeners from all webapp contexts
        for (IApplicationContext appCtx : getApplicationContexts().values()) {
            if (!(appCtx instanceof TomcatApplicationContext)) {
                continue;
            }
            TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
            if (tomcatCtx.getContext().getPath().isEmpty()) {
                continue;
            }
            ApplicationContext springCtx = tomcatCtx.getSpringContext();
            try {
                IAntMediaStreamHandler app = (IAntMediaStreamHandler) springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
                for (Object inst : instances) {
                    if (inst instanceof IStreamListener) {
                        app.removeStreamListener((IStreamListener) inst);
                    }
                }
            } catch (Exception e) {
                log.warn("Error removing listeners from context {}", tomcatCtx.getContext().getPath(), e);
            }
        }

        // Unregister REST handlers
        List<String> restKeys = pluginRestKeys.getOrDefault(pluginId, List.of());
        if (!restKeys.isEmpty()) {
            for (IApplicationContext appCtx : getApplicationContexts().values()) {
                if (!(appCtx instanceof TomcatApplicationContext)) {
                    continue;
                }
                TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
                String ctxPath = tomcatCtx.getContext().getPath();
                for (String key : restKeys) {
                    PluginRestDispatcher.unregisterHandler(ctxPath, key);
                }
            }
        }

        pluginInstances.remove(pluginId);
        pluginRestKeys.remove(pluginId);
        log.info("Unloaded plugin: {}", pluginId);
        return new Result(true);
    }

    /**
     * Resolves @Inject fields on an object from the webapp's Spring context.
     * Only injects well-known AMS types.
     */
    private void injectFields(Object instance, ApplicationContext springCtx) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            Object value = resolveInjectable(field.getType(), springCtx);
            if (value != null) {
                field.setAccessible(true);
                try {
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    log.warn("Cannot inject field '{}' on {}: {}", field.getName(),
                            instance.getClass().getSimpleName(), e.getMessage());
                }
            } else {
                log.debug("No injectable found for type {} on {}.{}",
                        field.getType().getSimpleName(), instance.getClass().getSimpleName(), field.getName());
            }
        }
    }

    /**
     * Resolves well-known AMS types from the Spring context.
     */
    private Object resolveInjectable(Class<?> type, ApplicationContext springCtx) {
        try {
            if (IAntMediaStreamHandler.class.isAssignableFrom(type)) {
                return springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
            }
            if (type.equals(AppSettings.class)) {
                return springCtx.getBean(AppSettings.BEAN_NAME);
            }
            if (type.equals(ServerSettings.class)) {
                return springCtx.getBean(ServerSettings.BEAN_NAME);
            }
            if (type.equals(Vertx.class)) {
                return springCtx.getBean("vertxCore");
            }
            if (type.getName().equals("io.antmedia.datastore.db.DataStore")) {
                IAntMediaStreamHandler app = (IAntMediaStreamHandler) springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
                if (app instanceof AntMediaApplicationAdapter) {
                    return ((AntMediaApplicationAdapter) app).getDataStore();
                }
            }
            if (type.equals(ApplicationContext.class)) {
                return springCtx;
            }
            // Try generic bean lookup as last resort
            return springCtx.getBean(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Injects plugin's own instances into @Rest classes.
     * If a @Rest class has an @Inject field whose type matches one of the already
     * instantiated @Listener/@AmsPlugin objects, inject it.
     */
    private void injectPluginInstances(Object restInstance, List<Object> pluginObjects) {
        for (Field field : restInstance.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            for (Object obj : pluginObjects) {
                if (field.getType().isAssignableFrom(obj.getClass())) {
                    field.setAccessible(true);
                    try {
                        field.set(restInstance, obj);
                    } catch (IllegalAccessException e) {
                        log.warn("Cannot inject plugin instance into {}.{}", restInstance.getClass().getSimpleName(), field.getName());
                    }
                    break;
                }
            }
        }
    }

    // Protected for unit-test overrides via Mockito.spy()
    protected boolean isSystemClassLoaderServerClassLoader() {
        return ClassLoader.getSystemClassLoader() instanceof ServerClassLoader;
    }

    protected void addJarToSystemClassLoader(URL jarUrl) {
        ((ServerClassLoader) ClassLoader.getSystemClassLoader()).addPluginJar(jarUrl);
    }

    protected Map<String, IApplicationContext> getApplicationContexts() {
        return LoaderBase.getRed5ApplicationContexts();
    }

    /** Returns the names of all loaded plugins. */
    public Set<String> getPluginNames() {
        return Collections.unmodifiableSet(pluginInstances.keySet());
    }

    /** Returns the REST dispatcher keys tracked per plugin. Visible for testing. */
    public Map<String, List<String>> getPluginRestKeys() {
        return Collections.unmodifiableMap(pluginRestKeys);
    }

    // ========================================================================
    // ZIP-based deployment: extract, validate manifest, run scripts, then loadPlugin()
    // ========================================================================

    /**
     * Deploy a plugin from a ZIP file.
     * Extracts ZIP, validates manifest, runs install script, then hot-loads via loadPlugin().
     */
    public Result loadPluginFromZip(File zipFile, File pluginsDir) {
        File extractDir = null;
        try {
            // 1. Extract ZIP
            extractDir = extractZip(zipFile);
            if (extractDir == null) {
                return new Result(false, "Failed to extract ZIP file");
            }

            // 2. Locate plugin.jar
            File pluginJar = new File(extractDir, "plugin.jar");
            if (!pluginJar.exists()) {
                return new Result(false, "plugin.jar not found in ZIP");
            }

            // 3. Parse and validate manifest
            Result validationResult = validateManifest(pluginJar);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }

            Attributes attrs = readManifestAttributes(pluginJar);
            String pluginName = attrs.getValue(MANIFEST_PLUGIN_NAME);
            String pluginVersion = attrs.getValue(MANIFEST_PLUGIN_VERSION);
            String loadingMode = attrs.getValue(MANIFEST_PLUGIN_LOADING_MODE);
            if (loadingMode == null) {
                loadingMode = LOADING_MODE_HOTLOAD;
            }
            boolean requiresRestart = "true".equalsIgnoreCase(attrs.getValue(MANIFEST_PLUGIN_REQUIRES_RESTART));

            // 4. WEBAPP_LIB must require restart
            if (LOADING_MODE_WEBAPP_LIB.equals(loadingMode) && !requiresRestart) {
                return new Result(false, "WEBAPP_LIB loading mode requires AMS-Plugin-Requires-Restart=true");
            }

            // 5. Duplicate check
            if (pluginRecords.containsKey(pluginName) || pluginInstances.containsKey(slugify(pluginName))) {
                return new Result(false, "Plugin already loaded: " + pluginName);
            }

            // 6. Build record
            PluginRecord record = buildPluginRecord(attrs);
            record.setState(PluginState.INSTALLING);
            pluginRecords.put(pluginName, record);

            // 7. Run install script if present
            File installScript = new File(extractDir, "install.sh");
            if (installScript.exists()) {
                int exitCode = runInstallScript(installScript, extractDir, pluginName, pluginVersion, pluginJar);
                if (exitCode != 0) {
                    record.setState(PluginState.FAILED);
                    record.setLastError("install.sh exited with code " + exitCode);
                    return new Result(false, "install.sh failed with exit code " + exitCode);
                }
            }

            // 8. Copy artifacts to canonical plugin directory
            String pluginId = record.getPluginId();
            File canonicalDir = new File(pluginsDir, pluginId);
            canonicalDir.mkdirs();
            copyFile(pluginJar, new File(canonicalDir, "plugin.jar"));

            File uninstallScript = new File(extractDir, "uninstall.sh");
            if (uninstallScript.exists()) {
                copyFile(uninstallScript, new File(canonicalDir, "uninstall.sh"));
            }

            File assetsDir = new File(extractDir, "assets");
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                copyDirectory(assetsDir, new File(canonicalDir, "assets"));
            }

            // 9. If restart required, stop here
            if (requiresRestart) {
                record.setState(PluginState.INSTALLED_PENDING_RESTART);
                log.info("Plugin {} installed. Server restart required to activate.", pluginName);
                return new Result(true, "Plugin installed. Server restart required to activate.");
            }

            // 10. Hot-load the JAR using existing V1 mechanism
            File jarInCanonical = new File(canonicalDir, "plugin.jar");
            Result loadResult = loadPlugin(jarInCanonical);
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
     * Unload a V2 plugin and run its uninstall script if present.
     */
    public Result unloadPluginV2(String pluginName, File pluginsDir) {
        PluginRecord record = pluginRecords.get(pluginName);
        String pluginId = slugify(pluginName);

        // Unload the Spring beans via existing mechanism
        Result unloadResult = unloadPlugin(pluginId);

        // Run uninstall script if present
        if (record != null) {
            File canonicalDir = new File(pluginsDir, record.getPluginId());
            File uninstallSh = new File(canonicalDir, "uninstall.sh");
            if (uninstallSh.exists()) {
                int exitCode = runScript(uninstallSh, canonicalDir, pluginName,
                        record.getVersion(), record.getPluginId(), canonicalDir.getAbsolutePath());
                if (exitCode != 0) {
                    log.warn("uninstall.sh for {} exited with code {} — proceeding with removal", pluginName, exitCode);
                }
            }
            deleteDirectory(canonicalDir);
            record.setState(PluginState.UNINSTALLED);
        }

        pluginRecords.remove(pluginName);
        return unloadResult.isSuccess() ? new Result(true, "Plugin removed: " + pluginName) : unloadResult;
    }

    /**
     * Runs the uninstall script (if present) and removes the canonical plugin directory.
     */
    public void runUninstallScript(String pluginName, File pluginsDir) {
        PluginRecord record = pluginRecords.get(pluginName);
        if (record == null) {
            return;
        }

        File canonicalDir = new File(pluginsDir, record.getPluginId());
        File uninstallSh = new File(canonicalDir, "uninstall.sh");
        if (uninstallSh.exists()) {
            int exitCode = runScript(uninstallSh, canonicalDir, pluginName,
                    record.getVersion(), record.getPluginId(), canonicalDir.getAbsolutePath());
            if (exitCode != 0) {
                log.warn("uninstall.sh for {} exited with code {} — proceeding with removal", pluginName, exitCode);
            }
        }

        deleteDirectory(canonicalDir);
        record.setState(PluginState.UNINSTALLED);
        pluginRecords.remove(pluginName);
    }

    /** Validate required V2 manifest attributes and version compatibility. */
    Result validateManifest(File jarFile) {
        Attributes attrs = readManifestAttributes(jarFile);
        if (attrs == null) {
            return new Result(false, "Cannot read MANIFEST.MF from plugin.jar");
        }

        String name = attrs.getValue(MANIFEST_PLUGIN_NAME);
        if (name == null || name.isEmpty()) {
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

        String loadingMode = attrs.getValue(MANIFEST_PLUGIN_LOADING_MODE);
        record.setLoadingMode(loadingMode != null ? loadingMode : LOADING_MODE_HOTLOAD);

        boolean requiresRestart = "true".equalsIgnoreCase(attrs.getValue(MANIFEST_PLUGIN_REQUIRES_RESTART));
        record.setRequiresRestart(requiresRestart);

        record.setPluginId(slugify(record.getName()) + "-" + record.getVersion());
        return record;
    }

    public PluginRecord getPluginRecord(String pluginName) {
        return pluginRecords.get(pluginName);
    }

    public List<PluginRecord> getAllPluginRecords() {
        return new ArrayList<>(pluginRecords.values());
    }

    // --- V2 utility methods ---

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
                    // Guard against zip slip
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

    int runInstallScript(File script, File workDir, String pluginName, String pluginVersion, File pluginJar) {
        String amsHome = System.getProperty("red5.root", "/usr/local/antmedia");
        File assetsDir = new File(workDir, "assets");

        ProcessBuilder pb = buildScriptProcess(script, workDir);
        pb.environment().put("AMS_HOME", amsHome);
        pb.environment().put("AMS_PLUGINS_DIR", amsHome + "/plugins");
        pb.environment().put("AMS_WEBAPPS_DIR", amsHome + "/webapps");
        pb.environment().put("PLUGIN_NAME", pluginName);
        pb.environment().put("PLUGIN_VERSION", pluginVersion);
        pb.environment().put("PLUGIN_JAR", pluginJar.getAbsolutePath());
        pb.environment().put("PLUGIN_ASSETS_DIR", assetsDir.getAbsolutePath());

        return executeProcess(pb, script.getName());
    }

    private int runScript(File script, File workDir, String pluginName, String pluginVersion,
            String pluginId, String pluginInstallDir) {
        String amsHome = System.getProperty("red5.root", "/usr/local/antmedia");

        ProcessBuilder pb = buildScriptProcess(script, workDir);
        pb.environment().put("AMS_HOME", amsHome);
        pb.environment().put("AMS_PLUGINS_DIR", amsHome + "/plugins");
        pb.environment().put("AMS_WEBAPPS_DIR", amsHome + "/webapps");
        pb.environment().put("PLUGIN_NAME", pluginName);
        pb.environment().put("PLUGIN_VERSION", pluginVersion);
        if (pluginId != null) {
            pb.environment().put("PLUGIN_ID", pluginId);
            pb.environment().put("PLUGIN_INSTALL_DIR", pluginInstallDir);
            pb.environment().put("PLUGIN_JAR", pluginInstallDir + "/plugin.jar");
            pb.environment().put("PLUGIN_ASSETS_DIR", pluginInstallDir + "/assets");
            pb.environment().put("PLUGIN_METADATA_FILE", pluginInstallDir + "/install-metadata.json");
        }

        return executeProcess(pb, script.getName());
    }

    private ProcessBuilder buildScriptProcess(File script, File workDir) {
        script.setExecutable(true);
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        return pb;
    }

    private int executeProcess(ProcessBuilder pb, String scriptName) {
        try {
            Process process = pb.start();
            try (InputStream is = process.getInputStream()) {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) { /* consume to prevent blocking */ }
            }
            int exitCode = process.waitFor();
            log.info("Script {} exited with code {}", scriptName, exitCode);
            return exitCode;
        } catch (Exception e) {
            log.error("Error running script {}", scriptName, e);
            return -1;
        }
    }

    static boolean isVersionCompatible(String currentVersion, String requiredVersion) {
        if (currentVersion == null || requiredVersion == null) {
            return true;
        }
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

    private static int[] parseVersion(String version) {
        String clean = version.replaceAll("[^0-9.]", "");
        String[] parts = clean.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
        }
        return result;
    }

    static String slugify(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
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
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
