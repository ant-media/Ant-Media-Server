package io.antmedia.filter;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.VodViewerStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

	public class VodStatisticsFilter extends StatisticsFilter {
	

	public boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException {
		return false;
	}
	
	
	@Override
	public boolean isFilterMatching(String requestURI) {
		return requestURI != null && (requestURI.endsWith("mp4") || requestURI.endsWith("webm"));
	}
	
	@Override
	public String getBeanName() {
		return VodViewerStats.BEAN_NAME;
	}


}