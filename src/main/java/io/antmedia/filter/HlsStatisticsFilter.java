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
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;

public class HlsStatisticsFilter extends StatisticsFilter {
		

	public boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException {
		Broadcast broadcast = getBroadcast(request, streamId); 

		if(broadcast != null
				&& broadcast.getHlsViewerLimit() != -1
				&& broadcast.getHlsViewerCount() >= broadcast.getHlsViewerLimit()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Viewer Limit Reached");
			return true;
		}
		return false;
	}
	
	public boolean isFilterMatching(String requestURI) {
		return requestURI != null && requestURI.endsWith("m3u8");
	}


	@Override
	public String getBeanName() {
		return HlsViewerStats.BEAN_NAME;
	}

}