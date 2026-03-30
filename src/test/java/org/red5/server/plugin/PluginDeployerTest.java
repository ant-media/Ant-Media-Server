package org.red5.server.plugin;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.catalina.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IApplicationContext;
import org.red5.server.tomcat.TomcatApplicationContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ConfigurableApplicationContext;

import io.antmedia.rest.model.Result;

@SuppressWarnings("unchecked")
public class PluginDeployerTest {

    private PluginDeployer deployer;

    @Before
    public void before() {
        deployer = new PluginDeployer();
    }


    /** Spy that bypasses the system-CL check and JAR-adding; scanning still uses the real CL. */
    private PluginDeployer deployerSpy(Map<String, IApplicationContext> contexts) {
        PluginDeployer spy = Mockito.spy(deployer);
        doReturn(true).when(spy).isSystemClassLoaderServerClassLoader();
        doNothing().when(spy).addJarToSystemClassLoader(any());
        doReturn(contexts).when(spy).getApplicationContexts();
        return spy;
    }

    /** Mock TomcatApplicationContext at the given path backed by the given bean factory. */
    private TomcatApplicationContext mockStreamingContext(String path,
            DefaultListableBeanFactory beanFactory) {
        TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
        Context catalinaCtx = mock(Context.class);
        when(tomcatCtx.getContext()).thenReturn(catalinaCtx);
        when(catalinaCtx.getPath()).thenReturn(path);

        ConfigurableApplicationContext springCtx = mock(ConfigurableApplicationContext.class);
        lenient().when(springCtx.getId()).thenReturn(path);
        when(tomcatCtx.getSpringContext()).thenReturn(springCtx);
        when(springCtx.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

        return tomcatCtx;
    }

    /**
     * DefaultListableBeanFactory extends DefaultSingletonBeanRegistry and implements
     * AutowireCapableBeanFactory + ConfigurableListableBeanFactory — all cast targets in PluginDeployer.
     */
    private DefaultListableBeanFactory mockBeanFactory() {
        DefaultListableBeanFactory bf = mock(DefaultListableBeanFactory.class);
        try {
            when(bf.createBean(any(Class.class))).thenAnswer(inv -> {
                Class<?> clazz = inv.getArgument(0);
                return clazz.getDeclaredConstructor().newInstance();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bf;
    }

    @Test
    public void testLoadPlugin_systemCLNotServerCL_fails() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildComponentJar("sclCheck");

        Result result = deployer.loadPlugin(jar);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("ServerClassLoader"));
    }

    @Test
    public void testLoadSpringPlugin_emptyJar_noComponents() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildEmptyJar("emptyPlugin");
        PluginDeployer spy = deployerSpy(Collections.emptyMap());

        Result result = spy.loadPlugin(jar);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No @Component"));
    }

    @Test
    public void testLoadSpringPlugin_noContexts_fails() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildComponentJar("noCtx");
        PluginDeployer spy = deployerSpy(Collections.emptyMap());

        Result result = spy.loadPlugin(jar);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("none could be registered"));
    }

    @Test
    public void testLoadSpringPlugin_success() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("loadOk");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        assertTrue(spy.getSpringPluginNames().contains("loadOk"));
        verify(bf).createBean(any(Class.class));
        verify((ConfigurableListableBeanFactory) bf).registerSingleton(anyString(), any());
    }

    @Test
    public void testLoadSpringPlugin_skipsRootContext() throws Exception {
        DefaultListableBeanFactory rootBf = mockBeanFactory();
        TomcatApplicationContext rootCtx = mockStreamingContext("", rootBf);

        DefaultListableBeanFactory liveAppBf = mockBeanFactory();
        TomcatApplicationContext liveAppCtx = mockStreamingContext("/LiveApp", liveAppBf);

        PluginDeployer spy = deployerSpy(Map.of("", rootCtx, "/LiveApp", liveAppCtx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("skipRoot");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        verify(rootBf, never()).createBean(any(Class.class));
        verify(liveAppBf).createBean(any(Class.class));
    }

    @Test
    public void testLoadSpringPlugin_multipleContexts() throws Exception {
        DefaultListableBeanFactory bf1 = mockBeanFactory();
        DefaultListableBeanFactory bf2 = mockBeanFactory();
        TomcatApplicationContext ctx1 = mockStreamingContext("/LiveApp", bf1);
        TomcatApplicationContext ctx2 = mockStreamingContext("/live", bf2);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx1, "/live", ctx2));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("multiCtx");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        verify(bf1).createBean(any(Class.class));
        verify(bf2).createBean(any(Class.class));
    }

    @Test
    public void testLoadSpringPlugin_duplicate() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("dupSpring");
        spy.loadPlugin(jar);

        Result second = spy.loadPlugin(jar);

        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already loaded"));
    }

    @Test
    public void testLoadSpringPlugin_restComponent_derivesKey() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildRestComponentJar("restPlugin");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        List<String> restKeys = spy.getSpringPluginRestKeys().get("restPlugin");
        assertNotNull(restKeys);
        assertTrue(restKeys.contains("test-plugin"));
    }

    @Test
    public void testUnloadPlugin_notFound() {
        Result result = deployer.unloadPlugin("doesNotExist");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not found"));
    }

    @Test
    public void testUnloadPlugin_success() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("unloadMe");
        spy.loadPlugin(jar);
        assertTrue(spy.getSpringPluginNames().contains("unloadMe"));

        Result result = spy.unloadPlugin("unloadMe");

        assertTrue(result.isSuccess());
        assertFalse(spy.getSpringPluginNames().contains("unloadMe"));
        verify((DefaultSingletonBeanRegistry) bf).destroySingleton(anyString());
    }

    @Test
    public void testUnloadPlugin_removesRestKeys() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildRestComponentJar("restUnload");
        spy.loadPlugin(jar);
        assertNotNull(spy.getSpringPluginRestKeys().get("restUnload"));

        spy.unloadPlugin("restUnload");

        assertNull(spy.getSpringPluginRestKeys().get("restUnload"));
    }

    @Test
    public void testGetSpringPluginNames_emptyInitially() {
        assertTrue(deployer.getSpringPluginNames().isEmpty());
    }

    @Test
    public void testGetSpringPluginNames_afterLoad() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("nameCheck");
        spy.loadPlugin(jar);

        assertTrue(spy.getSpringPluginNames().contains("nameCheck"));
    }

    @Test
    public void testGetSpringPluginNames_afterUnload() throws Exception {
        DefaultListableBeanFactory bf = mockBeanFactory();
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp", bf);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("loadUnload");
        spy.loadPlugin(jar);
        spy.unloadPlugin("loadUnload");

        assertFalse(spy.getSpringPluginNames().contains("loadUnload"));
    }
}
