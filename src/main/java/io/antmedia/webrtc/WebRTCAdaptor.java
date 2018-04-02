package io.antmedia.webrtc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.webrtc.api.IStreamInfo;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.webrtc.api.IWebRTCMuxer;

public class WebRTCAdaptor implements IWebRTCAdaptor {

	Map<String, List<IWebRTCMuxer>> muxerMap = new ConcurrentHashMap();

	protected static Logger logger = LoggerFactory.getLogger(WebRTCAdaptor.class);


	/* (non-Javadoc)
	 * @see io.antmedia.enterprise.webrtc.IWebRTCAdaptor#registerMuxer(java.lang.String, io.antmedia.enterprise.webrtc.api.IWebRTCMuxer)
	 */
	@Override
	public void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(webRTCMuxer);
		muxerMap.put(streamId, list);
	}

	/* (non-Javadoc)
	 * @see io.antmedia.enterprise.webrtc.IWebRTCAdaptor#deregisterMuxer(java.lang.String, io.antmedia.enterprise.webrtc.api.IWebRTCMuxer)
	 */
	@Override
	public void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		if (list != null) {
			list.remove(webRTCMuxer);
		}
		if (list == null || list.size() == 0) {
			muxerMap.remove(streamId);
		}
	}


	/* (non-Javadoc)
	 * @see io.antmedia.enterprise.webrtc.IWebRTCAdaptor#registerWebRTCClient(java.lang.String, io.antmedia.enterprise.webrtc.api.IWebRTCClient)
	 */
	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient) {
		boolean result = false;
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		if (list != null && list.size() > 0 ) {
			//list.get(0).getVideoBitrate()
			IWebRTCMuxer lowestBitrateMuxer = null;
			for (IWebRTCMuxer iWebRTCMuxer : list) 
			{	
				int videoBitrate = iWebRTCMuxer.getVideoBitrate();

				if (lowestBitrateMuxer == null || lowestBitrateMuxer.getVideoBitrate() > videoBitrate) {
					lowestBitrateMuxer = iWebRTCMuxer;
				}
			}

			if (lowestBitrateMuxer != null) {
				lowestBitrateMuxer.registerWebRTCClient(webRTCClient);
				result = true;
			}

		}
		return result;
	}
	
	
	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient, int resolutionHeight) {
		boolean result = false;
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		if (list != null && list.size() > 0 ) 
		{
			for (IWebRTCMuxer iWebRTCMuxer : list) 
			{	
				int videoHeight = iWebRTCMuxer.getVideoHeight();

				if (videoHeight == resolutionHeight) {
					iWebRTCMuxer.registerWebRTCClient(webRTCClient);
					result = true;
					break;
				}
			}

		}
		return result;
		
	}

	/* (non-Javadoc)
	 * @see io.antmedia.enterprise.webrtc.IWebRTCAdaptor#deregisterWebRTCClient(java.lang.String, io.antmedia.enterprise.webrtc.api.IWebRTCClient)
	 */
	/*
	@Override
	public boolean deregisterWebRTCClient(String streamId, IWebRTCClient webRTCClient) {
		
		IWebRTCMuxer webRTCMuxer = webRTCClient.getWebRTCMuxer();
		return webRTCMuxer.deregisterWebRTCClient(webRTCClient);
	
	}
	*/


	/* (non-Javadoc)
	 * @see io.antmedia.enterprise.webrtc.IWebRTCAdaptor#streamExists(java.lang.String)
	 */
	@Override
	public boolean streamExists(String streamId){
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		return (list != null) && (list.size() > 0);
	}

	@Override
	public List<IStreamInfo> getStreamOptions(String streamId) {
		List<? extends IStreamInfo> streamList = muxerMap.get(streamId);
		return (List<IStreamInfo>) streamList;
	}

	@Override
	public void adaptStreamingQuality(String streamId, IWebRTCClient webRTCClient) {
		if (webRTCClient instanceof WebRTCClient) {
			
		}
		List<IWebRTCMuxer> list = muxerMap.get(streamId);
		IWebRTCMuxer currentlyRegisteredMuxer = webRTCClient.getWebRTCMuxer();
		IWebRTCMuxer adaptedMuxer = null;
		int bitrate = webRTCClient.getTargetBitrate();
		int currentBitrateDiff = bitrate - (currentlyRegisteredMuxer.getVideoBitrate() + currentlyRegisteredMuxer.getAudioBitrate());

		for (Iterator<IWebRTCMuxer> iterator = list.iterator(); iterator.hasNext();) {
			IWebRTCMuxer iWebRTCMuxer = iterator.next();

			int bitrate_diff = bitrate - (iWebRTCMuxer.getVideoBitrate() + iWebRTCMuxer.getAudioBitrate());
			
			if (bitrate_diff > 0 && ((bitrate_diff < currentBitrateDiff) || currentBitrateDiff < 0)) {
				currentBitrateDiff = bitrate_diff;
				adaptedMuxer = iWebRTCMuxer;
			}
			
		}	
		
		if (adaptedMuxer != null && !currentlyRegisteredMuxer.equals(adaptedMuxer)) {
			// switch muxer if current registered muxer and adaptedMuxer are different
			logger.info(" switching muxer for the stream id: " + streamId 
									+ " adapted muxer video bitrate: " + adaptedMuxer.getVideoBitrate()
									+ " audio bitrate: " + adaptedMuxer.getAudioBitrate()
									+ " webrtc client target bitrate: " + bitrate);
			currentlyRegisteredMuxer.unRegisterWebRTCClient(webRTCClient);
			adaptedMuxer.registerWebRTCClient(webRTCClient);
		}
		else {
			//same muxer, do not switch
			logger.info("not found a more suitable adapted muxer for " + streamId
							+ " current muxer video bitrate: " + currentlyRegisteredMuxer.getVideoBitrate()
							+ " audio bitrate: " + currentlyRegisteredMuxer.getAudioBitrate()
							+ " webrtc client target bitrate: " + bitrate);
		}
		

	}

}
