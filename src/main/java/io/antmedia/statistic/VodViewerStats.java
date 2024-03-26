package io.antmedia.statistic;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VodViewerStats extends ViewerStats implements IStreamStats, ApplicationContextAware {

    protected static Logger logger = LoggerFactory.getLogger(VodViewerStats.class);

    public static final String BEAN_NAME = "vod.viewerstats";

    private Object lock = new Object();
    private Map<String, List<Session>> vodOutputStreamMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)  {
        dataStoreFactory = (DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME);

        setType(ViewerStats.VOD_TYPE);

        vertx = (Vertx) applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

        serverSettings = (ServerSettings)applicationContext.getBean(ServerSettings.BEAN_NAME);

        AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
        timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS, VOD_TYPE);

        vertx.setPeriodic(DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT, yt->
        {
            synchronized (lock) {
                updateViewerCountProcess(VOD_TYPE);
            }
        });
    }

}
