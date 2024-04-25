package io.antmedia.analytic.model;

public class ViewerCountEvent extends AnalyticEvent {
	public static final String EVENT_VIEWER_COUNT = "viewerCount";
	
	private int dashViewerCount;
	private int hlsViewerCount;
	private int webRTCViewerCount;
	
	public ViewerCountEvent() {
		setEvent(EVENT_VIEWER_COUNT);
	}

	public int getDashViewerCount() {
		return dashViewerCount;
	}

	public void setDashViewerCount(int dashViewerCount) {
		this.dashViewerCount = dashViewerCount;
	}

	public int getHlsViewerCount() {
		return hlsViewerCount;
	}

	public void setHlsViewerCount(int hlsViewerCount) {
		this.hlsViewerCount = hlsViewerCount;
	}

	public int getWebRTCViewerCount() {
		return webRTCViewerCount;
	}

	public void setWebRTCViewerCount(int webRTCViewerCount) {
		this.webRTCViewerCount = webRTCViewerCount;
	}	

}
