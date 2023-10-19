package io.antmedia.statistic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;

public class DashViewerStats extends ViewerStats implements IStreamStats, ApplicationContextAware {

	protected static Logger logger = LoggerFactory.getLogger(DashViewerStats.class);
	
	public static final String BEAN_NAME = "dash.viewerstats";
	
	private Object lock = new Object();

	//TODO: Code duplication(HLSViewerStats) move this method to ViewerStats
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)  {
		dataStoreFactory = (DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME);
		
		setType(ViewerStats.DASH_TYPE);

		vertx = (Vertx) applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		serverSettings = (ServerSettings)applicationContext.getBean(ServerSettings.BEAN_NAME);

		AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
		timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS, DASH_TYPE);
		
		vertx.setPeriodic(DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT, yt-> 
		{
			synchronized (lock) {
				updateViewerCountProcess(DASH_TYPE);
			}
		});	
	}

}
