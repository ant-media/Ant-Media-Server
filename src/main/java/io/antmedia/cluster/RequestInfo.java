package io.antmedia.cluster;

import io.antmedia.AppSettings;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.muxer.MuxAdaptor;

/**
 * Data class that holds info about cluster request info.
 * All actual logic is in RequestInfoUtils class.
 */
public class RequestInfo {
	public static final int HLS_REQUEST = 0;
	public static final int TS_REQUEST = 1;
	public static final int PREVIEW_REQUEST = 2;
	public static final int OTHER_REQUEST = -1;
	public static final int DASH_REQUEST = 3;
	public static final int DASH_SEGMENT = 4;
	public static final int LL_HLS_REQUEST = 5;

	public static final String ADAPTIVE_REGEX = ".*_[0-9]+p[0-9]+kbps\\..*";   //ex. X_720p600kbps.Y

	private final String requestURI;
	private final int type;
	private final boolean isDRM;
	private String streamId;
	private String scopeName;

	public RequestInfo(String reqURI) {
		this(reqURI, null);
	}

	public RequestInfo(String reqURI, AppSettings appSettings) {
		if (reqURI == null) {
			reqURI = "";
		}

		this.requestURI = reqURI;
		this.isDRM = appSettings != null && appSettings.getCustomSettings().containsKey("plugin.drm-plugin");

		String processedURI = reqURI;
		if (this.isDRM) {
			processedURI = RequestInfoUtils.preProcessDRMRequestURI(reqURI);
		}

		type = RequestInfoUtils.defineType(processedURI);
		if (type != OTHER_REQUEST) {
			streamId = RequestInfoUtils.defineStreamId(processedURI, type, appSettings != null ? appSettings.getHlsSegmentFileSuffixFormat() : "");
			scopeName = RequestInfoUtils.defineScopeName(processedURI, type);
		}
	}

	public boolean isStreamReq() {
		return type != OTHER_REQUEST;
	}

	public int getType() {
		return type;
	}

	public String toString() {
		return "request uri:" + requestURI;
	}

	public String getStreamId() {
		return streamId;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public String getScopeName() {
		return scopeName;
	}

	public boolean isDRM() {
		return isDRM;
	}
}