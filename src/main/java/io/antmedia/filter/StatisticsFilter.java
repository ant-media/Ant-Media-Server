package io.antmedia.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import io.antmedia.datastore.db.types.VoD;
import io.antmedia.logger.LoggerUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;
import org.apache.catalina.connector.ResponseFacade;

import java.util.concurrent.ConcurrentHashMap;

public abstract class StatisticsFilter extends AbstractFilter {

	protected static Logger logger = LoggerFactory.getLogger(StatisticsFilter.class);
	static Map<String,ServletResponse> fileOutputStreamMap = new ConcurrentHashMap<>();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
			String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");

			if (isViewerCountExceeded((HttpServletRequest) request, (HttpServletResponse) response, streamId)) { 
				logger.info("Number of viewers limits has exceeded so it's returning forbidden for streamId:{} and class:{}", streamId, getClass().getSimpleName());
				return; 
			}

			chain.doFilter(request, response);
			int status = ((HttpServletResponse) response).getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null)
			{
				logger.debug("req ip {} session id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
				IStreamStats stats = getStreamStats(getBeanName());

				if (stats != null) {
					if(status == HttpServletResponse.SC_OK )
						stats.registerNewViewer(streamId, sessionId, subscriberId);
					String filterName = this.config.getFilterName();
					if(filterName.equals("VodStatisticsFilter")){
						HttpServletResponse httpResponse = (HttpServletResponse) response;

						HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(httpResponse) {
							@Override
							public synchronized  ServletOutputStream getOutputStream() throws IOException {
								DataSizeCalculatorListener listener = new DataSizeCalculatorListener() {
									long duration;
									long fileSize;
									@Override
									public void updateFileStats(String vodId , long dataWritten){

									}
									@Override
									public void getFileInfo(String vodId) {
										VoD vod = getDataStore().getVoD(vodId);
										if(vod != null){
											this.duration = vod.getDuration();
											this.fileSize = vod.getFileSize();
										}
									}
								};
								return new DataSizeCalculator(super.getOutputStream(), streamId,subscriberId ,sessionId,listener);
							}

							@Override
							public PrintWriter getWriter() throws IOException {
								return new PrintWriter(super.getWriter());
							}
							@Override
							public void flushBuffer() throws IOException {
								PrintWriter writer =  getWriter();
							}
						};
						chain.doFilter(request, wrappedResponse);
					}
					else{
						stats.registerNewViewer(streamId, sessionId, subscriberId);
						int bufferSize = response.getBufferSize();
						stats.setBitrateStats(streamId, bufferSize);
					}
				}
			}
			startStreamingIfAutoStartStopEnabled((HttpServletRequest) request, streamId);

		}
		else if (HttpMethod.HEAD.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {
			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());

			chain.doFilter(request, response);

			startStreamingIfAutoStartStopEnabled((HttpServletRequest) request, streamId);

		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

	public void startStreamingIfAutoStartStopEnabled(HttpServletRequest request, String streamId) {
		//start if it's not found, it may be started 
		Broadcast broadcast = getBroadcast(request, streamId);
		if (broadcast != null && broadcast.isAutoStartStopEnabled() && !AntMediaApplicationAdapter.isStreaming(broadcast)) 
		{
			//startStreaming method starts streaming if stream is not streaming in local or in any node in the cluster
			logger.info("http play request(hls, dash) is received for stream id:{} and it's not streaming, so it's trying to start the stream", streamId);
			getAntMediaApplicationAdapter().startStreaming(broadcast);
		}

	}



	public abstract boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException;


	public abstract boolean isFilterMatching(String requestURI);

	public abstract String getBeanName();
}

class DataSizeCalculator extends ServletOutputStream {
	private long written = 0;
	private OutputStream stream;
	String streamId;
	String sessionId;
	Long logThreashhosd = 5000L;
	String subscriberId;
	DataSizeCalculatorListener listener;

	public DataSizeCalculator(OutputStream inner , String streamId ,String subscriberId, String sessionId, DataSizeCalculatorListener listener) {
		this.stream = inner;
		this.streamId = streamId;
		this.sessionId = sessionId;
		this.subscriberId = subscriberId;
		this.listener = listener;
	}
	@Override
	public void close() throws IOException {

		stream.close();

	}
	@Override
	public void flush() throws IOException {
		stream.flush();
	}
	@Override
	public void write(byte[] b) throws IOException {
		try {
			write(b, 0, b.length);
		}
		catch (Exception e){
			System.out.println("asdfasfd");
		}
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		stream.write(b, off, len);
		written += len;
		logWhenDataWriten();
	}
	@Override
	public void write(int b) throws IOException {
		written ++;
		stream.write(b);
	}

	@Override
	public boolean isReady() {
		return false;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {

	}
	void logWhenDataWriten(){
		if(written > logThreashhosd) {
			LoggerUtils.logJsonString("VodDataTransferred", "vodId", streamId, "subscriberId", subscriberId, "sessionId", sessionId, "dataSent", Long.toString(written));
			logThreashhosd = written + 100000;
			listener.updateFileStats(this.streamId,this.written);
		}
	}
}
