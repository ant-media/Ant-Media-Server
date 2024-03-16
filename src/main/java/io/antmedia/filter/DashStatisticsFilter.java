package io.antmedia.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;

public class DashStatisticsFilter extends StatisticsFilter {
	

	public boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException {
		Broadcast broadcast = getBroadcast(request, streamId); 

		if(broadcast != null
				&& broadcast.getDashViewerLimit() != -1
				&& broadcast.getDashViewerCount() >= broadcast.getDashViewerLimit()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Viewer Limit Reached");
			return true;
		}
		return false;
	}
	
	
	@Override
	public boolean isFilterMatching(String requestURI) {
		return requestURI != null && (requestURI.endsWith("m4s") || requestURI.endsWith("mpd"));
	}
	
	@Override
	public String getBeanName() {
		return DashViewerStats.BEAN_NAME;
	}


}