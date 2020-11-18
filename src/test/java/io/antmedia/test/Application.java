package io.antmedia.test;

import java.io.File;
import java.util.List;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.settings.ServerSettings;

public class Application extends MultiThreadedApplicationAdapter implements IAntMediaStreamHandler, IApplicationAdaptorFactory {

	public static String id = null;
	public static File file = null;
	public static long duration = 0;

	public static String notifyHookAction = null;
	public static String notitfyURL = null;
	public static String notifyId = null;
	public static String notifyStreamName = null;
	public static String notifyCategory = null;
	public static String notifyVodName = null;

	public static boolean enableSourceHealthUpdate = false;
	public static String notifyVodId = null;
	
	private AntMediaApplicationAdapter appAdaptor;
	private DataStoreFactory dataStoreFactory;
	private AppSettings appSettings;
	private List<IStreamPublishSecurity> streamPublishSecurityList;
	private StreamAcceptFilter streamAcceptFilter;
	private ServerSettings serverSettings;

	
	@Override
	public boolean appStart(IScope app) {
		appAdaptor = new AntMediaApplicationAdapter();
		appAdaptor.setAppSettings(getAppSettings());
		appAdaptor.setServerSettings(serverSettings);
		appAdaptor.setStreamPublishSecurityList(getStreamPublishSecurityList());

		if (getStreamPublishSecurityList() != null) {
			for (IStreamPublishSecurity streamPublishSecurity : getStreamPublishSecurityList()) {
				registerStreamPublishSecurity(streamPublishSecurity);
			}
		}
		
		appAdaptor.setStreamAcceptFilter(getStreamAcceptFilter());
		
		appAdaptor.setDataStoreFactory(getDataStoreFactory());
		appAdaptor.appStart(app);
		
		return super.appStart(app);
	}
	
	@Override
	public void muxingFinished(String id, File file, long duration, int resolution) {
		getAppAdaptor().muxingFinished(id, file, duration, resolution);
		Application.id = id;
		Application.file = file;
		Application.duration = duration;
	}

	public static void resetFields() {
		Application.id = null;
		Application.file = null;
		Application.duration = 0;
		notifyHookAction = null;
		notitfyURL = null;
		notifyId = null;
		notifyStreamName = null;
		notifyCategory = null;
		notifyVodName = null;

	}

	public StringBuilder notifyHook(String url, String id, String action, String streamName, String category,
			String vodName, String vodId) {
		notifyHookAction = action;
		notitfyURL = url;
		notifyId = id;
		notifyStreamName = streamName;
		notifyCategory = category;
		notifyVodName = vodName;
		notifyVodId  = vodId;

		return null;
	}

	@Override
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize) {
		if (enableSourceHealthUpdate) {
			getAppAdaptor().setQualityParameters(id, quality, speed, pendingPacketSize);
		}
	}

	@Override
	public void muxAdaptorAdded(MuxAdaptor muxAdaptor) {
		appAdaptor.muxAdaptorAdded(muxAdaptor);
	}

	@Override
	public void muxAdaptorRemoved(MuxAdaptor muxAdaptor) {
		appAdaptor.muxAdaptorRemoved(muxAdaptor);		
	}

	public void setAdaptor(AntMediaApplicationAdapter adaptor) {
		this.appAdaptor = adaptor;
	}

	@Override
	public AntMediaApplicationAdapter getAppAdaptor() {
		return appAdaptor;
	}
	
	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}

	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	public AppSettings getAppSettings() {
		return appSettings;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

	public List<IStreamPublishSecurity> getStreamPublishSecurityList() {
		return streamPublishSecurityList;
	}

	public void setStreamPublishSecurityList(List<IStreamPublishSecurity> streamPublishSecurityList) {
		this.streamPublishSecurityList = streamPublishSecurityList;
	}

	public StreamAcceptFilter getStreamAcceptFilter() {
		return streamAcceptFilter;
	}
	

	public void setStreamAcceptFilter(StreamAcceptFilter streamAcceptFilter) {
		this.streamAcceptFilter = streamAcceptFilter;
	}

	public boolean isValidStreamParameters(int width, int height, int fps, int bitrate, String streamId) {
		return streamAcceptFilter.isValidStreamParameters(width, height, fps, bitrate, streamId);
	}
	
	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}
}
