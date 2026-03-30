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

/**
 * Builds minimal Spring-plugin JARs on the fly for use in PluginDeployerTest.
 *
 *
 * Directory entries are added for every package level (e.g. {@code io/antmedia/})
 * because Spring's {@code classpath*:} scanner calls
 * {@code ClassLoader.getResources("io/antmedia")} to discover the package root.
 * Without an explicit directory entry for that path the JAR URL is never returned
 * and the scan finds nothing.
 */
public class SpringTestPluginJarBuilder {

    /**
     * Builds a JAR containing {@link MinimalSpringComponent} — a plain {@code @Component}
     * with no JAX-RS annotations.
     */
    public static File buildComponentJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), emptyManifest())) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Builds a JAR containing both {@link MinimalSpringComponent} and
     * {@link MinimalSpringRestComponent} (a {@code @Component + @Path("/test-plugin")} class).
     */
    public static File buildRestComponentJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        Set<String> addedDirs = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), emptyManifest())) {
            addClass(out, MinimalSpringComponent.class, addedDirs);
            addClass(out, MinimalSpringRestComponent.class, addedDirs);
        }
        return jar;
    }

    /**
     * Builds an empty JAR (no class entries) — for negative tests where the
     * Spring path must be reached but no {@code @Component} classes are found.
     */
    public static File buildEmptyJar(String jarName) throws Exception {
        File jar = tempFile(jarName + ".jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), emptyManifest())) {
            // no class entries
        }
        return jar;
    }

    private static Manifest emptyManifest() {
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return m;
    }

    private static void addClass(JarOutputStream out, Class<?> clazz,
            Set<String> addedDirs) throws Exception {
        String entryPath = clazz.getName().replace('.', '/') + ".class";

        // Add a directory entry for every package level.
        // Spring's ClassPathScanningCandidateComponentProvider uses classpath*: which
        // calls classLoader.getResources("io/antmedia") to discover the package root.
        // Without an explicit directory entry the JAR is invisible to the scanner.
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

        // Add the class file itself
        URL classUrl = clazz.getClassLoader().getResource(entryPath);
        if (classUrl == null) {
            return;
        }
        File classFile;
        try {
            classFile = new File(classUrl.toURI());
        } catch (URISyntaxException e) {
            return;
        }
        if (!classFile.exists()) {
            return;
        }
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
        File dir = new File(System.getProperty("java.io.tmpdir"), "ams-spring-plugin-test");
        dir.mkdirs();
        return new File(dir, name);
    }
}
