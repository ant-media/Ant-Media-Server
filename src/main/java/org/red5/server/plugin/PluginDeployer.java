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
 * Hot-loads and unloads Spring {@code @Component} plugin JARs at runtime.
 * Adds the JAR to {@link ServerClassLoader} and registers its beans in every
 * active streaming webapp context. JAR URL cannot be removed on undeploy —
 * classes remain in the JVM until restart.
 */
public class PluginDeployer {

    private static final Logger log = LoggerFactory.getLogger(PluginDeployer.class);

    // pluginId → bean names (for destroySingleton on undeploy)
    private final ConcurrentHashMap<String, List<String>> springPluginBeanNames = new ConcurrentHashMap<>();

    // pluginId → REST dispatcher keys (last @Path segment, for unregister on undeploy)
    private final ConcurrentHashMap<String, List<String>> springPluginRestKeys = new ConcurrentHashMap<>();


    /** Hot-loads a Spring {@code @Component} plugin JAR into every active webapp context. */
    public Result loadPlugin(File jarFile) {
        String pluginId = jarFile.getName().replaceAll("\\.jar$", "");
        if (springPluginBeanNames.containsKey(pluginId)) {
            log.warn("Spring plugin already loaded: {}", pluginId);
            return new Result(false, "Plugin already loaded: " + pluginId);
        }

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

        addJarToSystemClassLoader(jarUrl);

        // Scan only this JAR: system CL as parent so Spring annotations resolve,
        // getResources() overridden to findResources() to prevent scanning the whole classpath.
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

        List<String> registeredBeanNames = new ArrayList<>();
        List<String> registeredRestKeys = new ArrayList<>();
        for (String className : componentClassNames) {
            try {
                Class<?> clazz = Class.forName(className, true, systemCL);
                String beanName = resolveBeanName(clazz);

                // REST key = last segment of @Path (e.g. "/v1/hls-merger" → "hls-merger")
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
                    if (tomcatCtx.getContext().getPath().isEmpty()) { // skip admin console (root)
                        continue;
                    }
                    ApplicationContext springCtx = tomcatCtx.getSpringContext();
                    if (!(springCtx instanceof ConfigurableApplicationContext)) {
                        continue;
                    }
                    AutowireCapableBeanFactory beanFactory =
                            ((ConfigurableApplicationContext) springCtx).getAutowireCapableBeanFactory();
                    try {
                        Object instance = beanFactory.createBean(clazz);
                        ((org.springframework.beans.factory.config.ConfigurableListableBeanFactory) beanFactory)
                                .registerSingleton(beanName, instance);
                        anyContextRegistered = true;
                        log.info("Registered Spring plugin bean '{}' in context '{}'",
                                beanName, springCtx.getId());

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

        springPluginBeanNames.put(pluginId, registeredBeanNames);
        if (!registeredRestKeys.isEmpty()) {
            springPluginRestKeys.put(pluginId, registeredRestKeys);
        }
        log.info("Hot-loaded Spring plugin '{}', registered beans: {}, REST keys: {}",
                pluginId, registeredBeanNames, registeredRestKeys);
        return new Result(true);
    }

    /** @Component value if set, otherwise lowercase-first simple class name. */
    private static String resolveBeanName(Class<?> clazz) {
        Component comp = AnnotationUtils.findAnnotation(clazz, Component.class);
        if (comp != null && !comp.value().isEmpty()) {
            return comp.value();
        }
        String simple = clazz.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    /** Destroys plugin beans in all webapp contexts and unregisters REST handlers. */
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

    /** Returns the names of all hot-loaded Spring @Component plugins. */
    public Set<String> getSpringPluginNames() {
        return Collections.unmodifiableSet(springPluginBeanNames.keySet());
    }

    /** Returns the REST dispatcher keys tracked per plugin. Visible for testing. */
    public Map<String, List<String>> getSpringPluginRestKeys() {
        return Collections.unmodifiableMap(springPluginRestKeys);
    }
}
