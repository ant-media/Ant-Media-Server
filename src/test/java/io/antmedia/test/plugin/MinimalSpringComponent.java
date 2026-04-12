package io.antmedia.test.plugin;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.plugin.api.IStreamListener;

/**
 * Minimal Spring {@code @Component} plugin used in {@code PluginDeployerTest}. Mimics the
 * real V1 plugin shape: implements {@code ApplicationContextAware} and {@code IStreamListener},
 * calls {@code app.addStreamListener(this)} in {@code setApplicationContext}.
 */
@Component(value = "plugin.minimal-component")
public class MinimalSpringComponent implements ApplicationContextAware, IStreamListener {

    private ApplicationContext applicationContext;
    private IAntMediaStreamHandler app;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.applicationContext = ctx;
        this.app = (IAntMediaStreamHandler) ctx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
        app.addStreamListener(this);
    }

    @Override
    public void streamStarted(Broadcast broadcast) { }

    @Override
    public void streamFinished(Broadcast broadcast) { }

    @Override
    public void joinedTheRoom(String roomId, String streamId) { }

    @Override
    public void leftTheRoom(String roomId, String streamId) { }

    public ApplicationContext getApplicationContext() { return applicationContext; }
    public IAntMediaStreamHandler getApp() { return app; }
}
