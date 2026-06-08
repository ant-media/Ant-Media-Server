package org.red5.server.plugin;

import io.antmedia.test.plugin.MinimalSpringComponent;
import io.antmedia.test.plugin.MinimalSpringRestComponent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds minimal plugin JARs and ZIPs at test time for {@link PluginDeployerTest}.
 *
 * <p>The produced JARs contain compiled {@code .class} files from {@link MinimalSpringComponent}
 * and/or {@link MinimalSpringRestComponent}, with explicit directory entries — required because
 * Spring's {@code classpath*:} scanner calls {@code classLoader.getResources("io/antmedia")} to
 * discover the package root, and without directory entries the JAR is invisible to it.</p>
 */
public class SpringTestPluginJarBuilder {

    /**
     * Builds a JAR with only the plain {@code @Component} class (no REST {@code @Path}).
     */
    public static File buildComponentJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Builds a JAR with both the plain {@code @Component} and the REST {@code @Component @Path}
     * class. Used by tests that verify REST dispatcher key detection.
     */
    public static File buildComponentWithRestJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
            addClass(out, MinimalSpringRestComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Builds a JAR with a valid V2 manifest (the {@code AMS-Plugin-*} entries that
     * {@link PluginDeployer#validateManifest} requires) + the plain {@code @Component} class.
     */
    public static File buildPluginJar(String jarName, String pluginName, String version,
                                      String author, String requiresVersion) throws Exception {
        File jar = tempFile(jarName + ".jar");
        Manifest manifest = pluginManifest(pluginName, version, author, requiresVersion, false);
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Shorthand for {@link #buildPluginJar(String, String, String, String, String)} with
     * sensible defaults for name, version, author.
     */
    public static File buildPluginJar(String pluginName) throws Exception {
        return buildPluginJar(pluginName, pluginName, "1.0.0", "Test Author", null);
    }

    /**
     * Builds a JAR with manifest + REST component. Used by ZIP-flow tests that also need
     * REST key registration.
     */
    public static File buildPluginWithRestJar(String pluginName) throws Exception {
        File jar = tempFile(pluginName + ".jar");
        Manifest manifest = pluginManifest(pluginName, "1.0.0", "Test Author", null, false);
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
            addClass(out, MinimalSpringRestComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Builds a JAR with manifest that has {@code AMS-Plugin-Requires-Restart: true}.
     */
    public static File buildPluginJarRequiresRestart(String pluginName) throws Exception {
        File jar = tempFile(pluginName + ".jar");
        Manifest manifest = pluginManifest(pluginName, "1.0.0", "Test Author", null, true);
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /** Builds a JAR with no {@code @Component} classes — just empty + valid manifest. */
    public static File buildEmptyJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            // no class entries
        }
        return jar;
    }

    /** Builds a JAR missing {@code AMS-Plugin-Name}. */
    public static File buildJarMissingName() throws Exception {
        File jar = tempFile("missing-name.jar");
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("AMS-Plugin-Version", "1.0.0");
        m.getMainAttributes().putValue("AMS-Plugin-Author", "Test");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /** Builds a JAR missing {@code AMS-Plugin-Version}. */
    public static File buildJarMissingVersion() throws Exception {
        File jar = tempFile("missing-version.jar");
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("AMS-Plugin-Name", "Test Plugin");
        m.getMainAttributes().putValue("AMS-Plugin-Author", "Test");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /** Builds a JAR missing {@code AMS-Plugin-Author}. */
    public static File buildJarMissingAuthor() throws Exception {
        File jar = tempFile("missing-author.jar");
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("AMS-Plugin-Name", "Test Plugin");
        m.getMainAttributes().putValue("AMS-Plugin-Version", "1.0.0");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /** Wraps a JAR in a ZIP with the JAR as {@code plugin.jar} at the root. */
    public static File wrapJarAsZip(File jarFile, String zipName) throws Exception {
        File zip = tempFile(zipName + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("plugin.jar"));
            try (FileInputStream fis = new FileInputStream(jarFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
            }
            zos.closeEntry();
        }
        return zip;
    }

    /** Builds a ZIP with no {@code plugin.jar} inside. */
    public static File buildZipNoPluginJar() throws Exception {
        File zip = tempFile("no-plugin-jar.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("no plugin.jar here".getBytes());
            zos.closeEntry();
        }
        return zip;
    }

    // ---- internals ----

    private static Manifest pluginManifest(String pluginName, String version, String author,
                                           String requiresVersion, boolean requiresRestart) {
        Manifest m = new Manifest();
        Attributes attrs = m.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("AMS-Plugin-Name", pluginName);
        attrs.putValue("AMS-Plugin-Version", version);
        attrs.putValue("AMS-Plugin-Author", author);
        if (requiresVersion != null) {
            attrs.putValue("AMS-Plugin-Requires-Version", requiresVersion);
        }
        if (requiresRestart) {
            attrs.putValue("AMS-Plugin-Requires-Restart", "true");
        }
        return m;
    }

    private static void addClass(JarOutputStream out, Class<?> clazz, Set<String> addedDirs) throws Exception {
        String entryPath = clazz.getName().replace('.', '/') + ".class";

        // Explicit directory entries — Spring's classpath*: scanner needs them.
        String[] segments = entryPath.split("/");
        StringBuilder dir = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            dir.append(segments[i]).append('/');
            String dirName = dir.toString();
            if (addedDirs.add(dirName)) {
                out.putNextEntry(new JarEntry(dirName));
                out.closeEntry();
            }
        }

        URL classUrl = clazz.getClassLoader().getResource(entryPath);
        if (classUrl == null) return;
        File classFile;
        try {
            classFile = new File(classUrl.toURI());
        } catch (URISyntaxException e) {
            return;
        }
        if (!classFile.exists()) return;

        out.putNextEntry(new JarEntry(entryPath));
        try (FileInputStream in = new FileInputStream(classFile)) {
            byte[] buf = new byte[2048];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
        out.closeEntry();
    }

    private static File tempFile(String name) throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "ams-plugin-test");
        dir.mkdirs();
        return new File(dir, name);
    }
}
