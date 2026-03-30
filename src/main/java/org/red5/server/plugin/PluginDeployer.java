package org.red5.server.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.classloading.ServerClassLoader;
import org.red5.server.tomcat.TomcatApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import io.antmedia.rest.PluginRestDispatcher;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.Path;

/**
 * Hot-loads and unloads Spring {@code @Component} plugin JARs at runtime
 * without a server restart.
 *
 * <p>The JAR is added to the {@link ServerClassLoader} (the JVM system
 * classloader) and its {@code @Component} classes in the {@code io.antmedia}
 * package hierarchy are registered as singleton beans into every active
 * streaming webapp context.  Classes also annotated with {@code @Path} are
 * registered with {@link PluginRestDispatcher} so their REST endpoints are
 * reachable at {@code /{appName}/rest/plugins/{key}/…}.</p>
 *
 * <p>Note: the JAR URL cannot be removed from the system classloader on
 * undeploy.  Bean instances are destroyed but classes remain in the JVM
 * until server restart.</p>
 */
public class PluginDeployer {

    private static final Logger log = LoggerFactory.getLogger(PluginDeployer.class);

    /**
     * Tracks bean names registered for each Spring @Component plugin.
     * Key: pluginId (JAR filename without .jar).
     * Value: Spring bean names — used for destroySingleton on undeploy.
     */
    private final ConcurrentHashMap<String, List<String>> springPluginBeanNames = new ConcurrentHashMap<>();

    /**
     * Tracks PluginRestDispatcher keys for plugins that expose REST services.
     * Key: pluginId.
     * Value: dispatcher keys (last segment of each REST class's @Path) —
     *        used to unregister from the dispatcher on undeploy.
     */
    private final ConcurrentHashMap<String, List<String>> springPluginRestKeys = new ConcurrentHashMap<>();


    /**
     * Loads a Spring {@code @Component} plugin JAR into the running JVM.
     *
     * <ol>
     *   <li>Verifies the system classloader is a {@link ServerClassLoader}.</li>
     *   <li>Adds the JAR URL to the system classloader.</li>
     *   <li>Scans the JAR for {@code @Component} classes in {@code io.antmedia}.</li>
     *   <li>Registers those classes as singleton beans in every active streaming
     *       webapp context.</li>
     *   <li>Registers REST service classes with {@link PluginRestDispatcher}.</li>
     * </ol>
     *
     * @param jarFile the plugin JAR on disk
     * @return {@link Result} with {@code success=true} on success
     */
    public Result loadPlugin(File jarFile) {
        // Duplicate check — reject if already hot-loaded by this name
        String pluginId = jarFile.getName().replaceAll("\\.jar$", "");
        if (springPluginBeanNames.containsKey(pluginId)) {
            log.warn("Spring plugin already loaded: {}", pluginId);
            return new Result(false, "Plugin already loaded: " + pluginId);
        }

        // 1. Verify the system classloader is a ServerClassLoader
        if (!isSystemClassLoaderServerClassLoader()) {
            log.warn("System classloader is not a ServerClassLoader ({}); Spring plugin loading is not supported in this environment",
                    ClassLoader.getSystemClassLoader().getClass().getName());
            return new Result(false, "Spring plugin loading requires ServerClassLoader as system classloader");
        }

        URL jarUrl;
        try {
            jarUrl = jarFile.toURI().toURL();
        } catch (Exception e) {
            return new Result(false, "Invalid JAR path: " + e.getMessage());
        }

        // 2. Add JAR to the system classloader so its classes are visible everywhere
        addJarToSystemClassLoader(jarUrl);

        // 3. Discover @Component classes by scanning only the new JAR.
        //
        //    Two conflicting requirements:
        //    a) Resource discovery must be limited to this JAR only — otherwise
        //       getResources() delegates to the parent CL and finds every @Component
        //       class in the entire AMS codebase, causing "already bound" errors.
        //    b) Annotation resolution (meta-annotation checks, e.g. @Indexed on @Component)
        //       requires loading Spring's annotation classes, which need a non-null parent.
        //
        //    Solution: use system CL as parent (satisfies b), but override getResources()
        //    to call findResources() which only searches this JAR's own URL entries (satisfies a).
        List<String> componentClassNames = new ArrayList<>();
        ClassLoader systemCL = ClassLoader.getSystemClassLoader();
        try (URLClassLoader scanCL = new URLClassLoader(new URL[]{jarUrl}, systemCL) {
            @Override
            public java.util.Enumeration<URL> getResources(String name) throws java.io.IOException {
                return findResources(name);
            }
        }) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
            scanner.setResourceLoader(new PathMatchingResourcePatternResolver(scanCL));

            for (BeanDefinition bd : scanner.findCandidateComponents("io.antmedia")) {
                componentClassNames.add(bd.getBeanClassName());
            }
        } catch (Exception e) {
            log.error("Error scanning JAR {} for @Component classes", jarFile.getName(), e);
            return new Result(false, "Error scanning JAR for components: " + e.getMessage());
        }

        if (componentClassNames.isEmpty()) {
            log.warn("No @Component classes found in io.antmedia package in {}", jarFile.getName());
            return new Result(false, "No @Component classes found in io.antmedia package");
        }

        // 4. Register beans in every active streaming webapp context
        List<String> registeredBeanNames = new ArrayList<>();
        List<String> registeredRestKeys = new ArrayList<>();
        for (String className : componentClassNames) {
            try {
                Class<?> clazz = Class.forName(className, true, systemCL);
                String beanName = resolveBeanName(clazz);

                // Derive dispatcher key if this class is a JAX-RS REST service.
                // Use the last segment of the class-level @Path value so that versioned
                // paths like @Path("/v1/hls-merger") map to key "hls-merger".
                Path pathAnnotation = AnnotationUtils.findAnnotation(clazz, Path.class);
                String restKey = null;
                if (pathAnnotation != null) {
                    String pathValue = pathAnnotation.value().replaceAll("^/+", "");
                    String[] segments = pathValue.split("/");
                    restKey = segments[segments.length - 1];
                }

                boolean anyContextRegistered = false;
                for (IApplicationContext appCtx : getApplicationContexts().values()) {
                    if (!(appCtx instanceof TomcatApplicationContext)) {
                        continue;
                    }
                    TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
                    // Skip the admin console — its Tomcat context path is "" (root webapp).
                    // Streaming apps have paths like /live, /LiveApp, /WebRTCAppEE, etc.
                    if (tomcatCtx.getContext().getPath().isEmpty()) {
                        continue;
                    }
                    ApplicationContext springCtx = tomcatCtx.getSpringContext();
                    if (!(springCtx instanceof ConfigurableApplicationContext)) {
                        continue;
                    }
                    AutowireCapableBeanFactory beanFactory =
                            ((ConfigurableApplicationContext) springCtx).getAutowireCapableBeanFactory();
                    try {
                        // createBean handles @Autowired, ApplicationContextAware, @PostConstruct, etc.
                        Object instance = beanFactory.createBean(clazz);
                        ((org.springframework.beans.factory.config.ConfigurableListableBeanFactory) beanFactory)
                                .registerSingleton(beanName, instance);
                        anyContextRegistered = true;
                        log.info("Registered Spring plugin bean '{}' in context '{}'",
                                beanName, springCtx.getId());

                        // Register REST service with the PluginRestDispatcher static registry.
                        // Static registration is required because Jersey creates its own instance
                        // of PluginRestDispatcher independently from the Spring bean — a static
                        // map keyed by context path is the only reliable shared state.
                        if (restKey != null) {
                            String ctxPath = tomcatCtx.getContext().getPath();
                            PluginRestDispatcher.registerHandler(ctxPath, restKey, instance);
                            log.info("Registered plugin REST handler '{}' at /plugins/{} for context path '{}'",
                                    beanName, restKey, ctxPath);
                        }
                    } catch (Exception e) {
                        log.error("Failed to create/register bean '{}' in context '{}'",
                                beanName, springCtx.getId(), e);
                    }
                }

                if (anyContextRegistered) {
                    if (!registeredBeanNames.contains(beanName)) {
                        registeredBeanNames.add(beanName);
                    }
                    if (restKey != null && !registeredRestKeys.contains(restKey)) {
                        registeredRestKeys.add(restKey);
                    }
                }
            } catch (ClassNotFoundException e) {
                log.error("Cannot load class {} from system classloader", className, e);
            }
        }

        if (registeredBeanNames.isEmpty()) {
            return new Result(false, "Plugin classes found but none could be registered in any active context");
        }

        // 5. Track by pluginId so the caller can undeploy by name
        springPluginBeanNames.put(pluginId, registeredBeanNames);
        if (!registeredRestKeys.isEmpty()) {
            springPluginRestKeys.put(pluginId, registeredRestKeys);
        }
        log.info("Hot-loaded Spring plugin '{}', registered beans: {}, REST keys: {}",
                pluginId, registeredBeanNames, registeredRestKeys);
        return new Result(true);
    }

    /**
     * Resolves the Spring bean name for a {@code @Component}-annotated class.
     * Uses the annotation's {@code value} if set, otherwise lowercase-first simple class name.
     */
    private static String resolveBeanName(Class<?> clazz) {
        Component comp = AnnotationUtils.findAnnotation(clazz, Component.class);
        if (comp != null && !comp.value().isEmpty()) {
            return comp.value();
        }
        String simple = clazz.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    /**
     * Stops and unloads a previously hot-loaded Spring plugin.
     *
     * <p>Beans are removed from all active webapp contexts and REST handlers
     * are unregistered from {@link PluginRestDispatcher}.  The JAR itself
     * cannot be removed from the system classloader; classes remain in the
     * JVM until server restart.</p>
     *
     * @param pluginName the plugin ID (JAR filename without .jar)
     * @return {@link Result} with {@code success=true} on success
     */
    public Result unloadPlugin(String pluginName) {
        List<String> beanNames = springPluginBeanNames.get(pluginName);
        if (beanNames == null) {
            log.warn("Plugin not found for unload: {}", pluginName);
            return new Result(false, "Plugin not found: " + pluginName);
        }

        List<String> restKeys = springPluginRestKeys.getOrDefault(pluginName, List.of());

        for (IApplicationContext appCtx : getApplicationContexts().values()) {
            if (!(appCtx instanceof TomcatApplicationContext)) {
                continue;
            }
            TomcatApplicationContext tomcatCtx = (TomcatApplicationContext) appCtx;
            if (tomcatCtx.getContext().getPath().isEmpty()) {
                continue;
            }
            ApplicationContext springCtx = tomcatCtx.getSpringContext();
            if (!(springCtx instanceof ConfigurableApplicationContext)) {
                continue;
            }

            if (!restKeys.isEmpty()) {
                String ctxPath = tomcatCtx.getContext().getPath();
                for (String key : restKeys) {
                    PluginRestDispatcher.unregisterHandler(ctxPath, key);
                    log.info("Unregistered plugin REST handler '{}' from context path '{}'", key, ctxPath);
                }
            }

            org.springframework.beans.factory.support.DefaultSingletonBeanRegistry beanRegistry =
                    (org.springframework.beans.factory.support.DefaultSingletonBeanRegistry)
                    ((ConfigurableApplicationContext) springCtx).getAutowireCapableBeanFactory();
            for (String beanName : beanNames) {
                try {
                    beanRegistry.destroySingleton(beanName);
                    log.info("Removed Spring plugin bean '{}' from context '{}'", beanName, springCtx.getId());
                } catch (Exception e) {
                    log.warn("Could not destroy bean '{}' in context '{}': {}", beanName, springCtx.getId(), e.getMessage());
                }
            }
        }

        springPluginBeanNames.remove(pluginName);
        springPluginRestKeys.remove(pluginName);
        log.info("Unloaded Spring plugin: {}", pluginName);
        return new Result(true);
    }


    /**
     * Returns {@code true} if the JVM system classloader is a {@link ServerClassLoader}.
     * Overridden in unit tests to skip the real system-CL check.
     */
    protected boolean isSystemClassLoaderServerClassLoader() {
        return ClassLoader.getSystemClassLoader() instanceof ServerClassLoader;
    }

    /**
     * Appends the given JAR URL to the system {@link ServerClassLoader}.
     * Overridden in unit tests to avoid modifying the real system classloader.
     */
    protected void addJarToSystemClassLoader(URL jarUrl) {
        ((ServerClassLoader) ClassLoader.getSystemClassLoader()).addPluginJar(jarUrl);
    }

    /**
     * Returns the map of active Red5 application contexts.
     * Overridden in unit tests to inject mock webapp contexts.
     */
    protected Map<String, IApplicationContext> getApplicationContexts() {
        return LoaderBase.getRed5ApplicationContexts();
    }

    /** Returns the names of all hot-loaded Spring @Component plugins. */
    public Set<String> getSpringPluginNames() {
        return Collections.unmodifiableSet(springPluginBeanNames.keySet());
    }

    /** Returns the REST dispatcher keys tracked per plugin. Visible for testing. */
    public Map<String, List<String>> getSpringPluginRestKeys() {
        return Collections.unmodifiableMap(springPluginRestKeys);
    }
}
