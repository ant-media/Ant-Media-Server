package io.antmedia.webrtc;


import static org.bytedeco.javacpp.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.av_rescale_q;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVRational;
import org.red5.server.api.scope.IScope;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.muxer.Muxer;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.webrtc.api.IWebRTCMuxer;

public class WebRTCMuxer extends Muxer implements IWebRTCMuxer {

	private IWebRTCAdaptor webRTCAdaptor;

	private ConcurrentLinkedQueue<IWebRTCClient> webRTCClientList =  new ConcurrentLinkedQueue<IWebRTCClient>();

	private String streamId;

	private int width;

	private int height;

	private int videoBitrate;

	private int audioBitrate;

	protected int videoStreamIndex = -1;

	protected int audioStreamIndex = -1;
	
	private AtomicInteger clientCount = new AtomicInteger(0);

	private boolean videoConfParsed = false;

	private byte[] sps;

	private byte[] pps;

	private byte[] keyFrame;

	private byte[] videoConf;

	private boolean isPrepareIOCalled = false;

	private AVRational videoTimebase;

	private AVRational audioTimebase;

	protected static Logger logger = LoggerFactory.getLogger(WebRTCMuxer.class);

	private AVRational timeBaseForMS;

	public WebRTCMuxer(QuartzSchedulingService scheduler, IWebRTCAdaptor webRTCAdaptor) {
		super(scheduler);
		this.webRTCAdaptor = webRTCAdaptor;
		timeBaseForMS = new AVRational();
		timeBaseForMS.num(1);
		timeBaseForMS.den(1000);
	}

	@Override
	public void init(IScope scope, String name, int resolutionHeight) {
		if (!isInitialized) {
			isInitialized = true;
			this.height = resolutionHeight;
			this.streamId = name;
		}

	}

	@Override
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor) {
		this.webRTCAdaptor = webRTCAdaptor;
	}

	/**
	 * 
	 * @param streamId is the stream url 
	 * 
	 * stream url name is original stream name + "_" + resolutionHeight + "p"
	 */
	@Override
	public void registerToAdaptor() {
		webRTCAdaptor.registerMuxer(streamId, this);
	}

	@Override
	public String getStreamId() {
		return streamId;
	}

	@Override
	public int getVideoHeight() {
		return height;
	}

	@Override
	public int getVideoWidth() {
		return width;
	}

	@Override
	public int getVideoBitrate() {
		return videoBitrate;
	}

	@Override
	public int getAudioBitrate() {
		return audioBitrate;
	}

	@Override
	public void registerWebRTCClient(IWebRTCClient webRTCClient) {
		webRTCClientList.add(webRTCClient);
		clientCount.incrementAndGet();
		webRTCClient.setWebRTCMuxer(this);
		webRTCClient.setVideoResolution(width, height);
		if (videoConfParsed) {
			webRTCClient.sendVideoConfPacket(videoConf, keyFrame, 0);
		}
	}

	@Override
	public boolean unRegisterWebRTCClient(IWebRTCClient webRTCClient) {
		clientCount.decrementAndGet();
		return webRTCClientList.remove(webRTCClient);
	}

	@Override
	public void sendVideoPacket(byte[] videoPacket, boolean isKeyFrame, long timestamp) 
	{
		for (Iterator<IWebRTCClient> iterator = webRTCClientList.iterator(); iterator.hasNext();) {
			IWebRTCClient iWebRTCClient = iterator.next();
			iWebRTCClient.sendVideoPacket(videoPacket, isKeyFrame, timestamp);	
		}
	}

	public void sendVideoConfPacket(byte[] videoPacket, long timestamp)
	{
		for (Iterator<IWebRTCClient> iterator = webRTCClientList.iterator(); iterator.hasNext();) {
			IWebRTCClient iWebRTCClient = iterator.next();
			iWebRTCClient.sendVideoConfPacket(videoConf, videoPacket, timestamp);	
		}
	}

	@Override
	public void sendAudioPacket(byte[] audioPacket, long timestamp) 
	{
		for (Iterator<IWebRTCClient> iterator = webRTCClientList.iterator(); iterator.hasNext();) {
			IWebRTCClient iWebRTCClient = iterator.next();
			iWebRTCClient.sendAudioPacket(audioPacket, timestamp);
		}
		
	}

	@Override
	public boolean prepare(AVFormatContext inputFormatContext) {
		return true;
	}

	@Override
	public boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {
		if (codecContext.codec_type() == AVMEDIA_TYPE_VIDEO) {
			this.videoStreamIndex = streamIndex;
			this.width = codecContext.width();
			this.height = codecContext.height();
			this.videoBitrate = (int)codecContext.bit_rate();
			this.videoTimebase = codecContext.time_base();

		}
		else if (codecContext.codec_type() == AVMEDIA_TYPE_AUDIO) {
			this.audioStreamIndex = streamIndex;
			this.audioBitrate = (int)codecContext.bit_rate();
			this.audioTimebase = codecContext.time_base();
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean prepareIO() {	
		if (!isPrepareIOCalled ) {
			isPrepareIOCalled = true;
			registerToAdaptor();
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {
		webRTCAdaptor.unRegisterMuxer(streamId, this);

		for (Iterator<IWebRTCClient> iterator = webRTCClientList.iterator(); iterator.hasNext();) {
			IWebRTCClient iWebRTCClient = iterator.next();
			iWebRTCClient.stop();
		}
	}

	@Override
	public void writePacket(AVPacket avpacket, AVStream inStream) {	
	}



	public int findNALStartCode(byte[] data, int offset) 
	{
		int i = offset;
		for(; i < data.length; i++) {
			if (data[i] == 0 && data[i+1] == 0
					&& (data[i+2] == 1 || (data[i+2] == 0 && data[i+3] == 1))) 
			{
				return i;
			}
		}

		if (i == data.length) {
			//end of array
			return i;
		}
		//nal not found
		return -1;
	}

	public boolean parseVideoConfData(byte[] data) 
	{

		int nalStartIndex = findNALStartCode(data, 0);
		int nalEndIndex;

		while ((nalEndIndex = findNALStartCode(data, nalStartIndex + 3)) != -1) {

			int nalLength = 0;
			int startCodeLength = 3; // start code may be 0,0,1 or 0,0,0,1

			if (data[nalStartIndex + 3] == 1) 
			{ 
				//0, 0, 0 , 1
				startCodeLength = 4;
			}
			nalLength = nalEndIndex - nalStartIndex - startCodeLength;


			byte nalType = (byte)(data[nalStartIndex + startCodeLength] & 0x1F);
			if (nalType == 7) { //SPS
				sps = new byte[nalLength + startCodeLength];
				System.arraycopy(data, nalStartIndex, sps, 0, nalLength + startCodeLength);

			}
			else if (nalType == 8) { //PPS
				pps = new byte[nalLength + startCodeLength];
				System.arraycopy(data, nalStartIndex, pps, 0, nalLength + startCodeLength);

			}
			else if (nalType == 5) { //key frame
				keyFrame = new byte[nalLength + startCodeLength];
				System.arraycopy(data, nalStartIndex, keyFrame, 0, nalLength + startCodeLength);
			}
			nalStartIndex = nalEndIndex;
		}

		return (sps != null && pps != null && keyFrame != null);

	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		
		if (pkt.stream_index() == this.videoStreamIndex) 
		{
			long pts = av_rescale_q(pkt.pts(), videoTimebase, timeBaseForMS);
			//logger.info("send video packet pts: " + pts + " System current time millis: " + now);
			BytePointer data = pkt.data();
			byte[] byteArray = new byte[pkt.size()];
			data.get(byteArray, 0, byteArray.length);
			boolean isKeyFrame = false;
			if ((pkt.flags() & AV_PKT_FLAG_KEY) == 1) {
				isKeyFrame = true;
			}

			if (!videoConfParsed) {
				videoConfParsed = true;
				parseVideoConfData(byteArray);
				videoConf = new byte[sps.length + pps.length];
				System.arraycopy(sps, 0, videoConf, 0, sps.length);
				System.arraycopy(pps, 0, videoConf, sps.length, pps.length);

				sendVideoConfPacket(keyFrame, pts);
				//long diff = System.currentTimeMillis() - now;
				//logger.info("sent video conf packet after " + diff + " ms" );
			}
			else {
				if (isKeyFrame) {
					keyFrame = byteArray;
				}
				sendVideoPacket(byteArray, isKeyFrame, pts);
				//long diff = System.currentTimeMillis() - now;
				//logger.info("sent video packet after " + diff + " ms" );
			}

		}
		else  if (pkt.stream_index() == this.audioStreamIndex) 
		{
			long pts = av_rescale_q(pkt.pts(), audioTimebase, timeBaseForMS);
			//logger.info("send audio packet pts: " + pts + " System current time millis: " + System.currentTimeMillis());
			
			BytePointer data = pkt.data();
			byte[] byteArray = new byte[pkt.size()];
			data.get(byteArray, 0, byteArray.length);

			sendAudioPacket(byteArray, pts);
			
			//long diff = System.currentTimeMillis() - now;
			//logger.info("sent audio packet after " + diff + " ms" );
		}
	}

	public boolean isExtradata_parsed() {
		return videoConfParsed;
	}

	public void setExtradata_parsed(boolean extradata_parsed) {
		this.videoConfParsed = extradata_parsed;
	}

	public byte[] getKeyFrame() {
		return keyFrame;
	}

	public void setKeyFrame(byte[] keyFrame) {
		this.keyFrame = keyFrame;
	}

	public byte[] getPps() {
		return pps;
	}

	public void setPps(byte[] pps) {
		this.pps = pps;
	}

	public byte[] getSps() {
		return sps;
	}

	public void setSps(byte[] sps) {
		this.sps = sps;
	}

	@Override
	public boolean contains(IWebRTCClient webRTCClient) {
		return webRTCClientList.contains(webRTCClient);
	}

	public void setVideoResolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setBitrate(int videoBitrate, int audioBitrate) {
		this.videoBitrate = videoBitrate;
		this.audioBitrate = audioBitrate;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;

	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setVideoBitrate(int videoBitrate) {
		this.videoBitrate = videoBitrate;
	}

	public AVRational getVideoTimebase() {
		return videoTimebase;
	}

	public void setVideoTimebase(AVRational videoTimebase) {
		this.videoTimebase = videoTimebase;
	}

	public void setAudioBitrate(int audioBitrate) {
		this.audioBitrate = audioBitrate;
	}

	public AVRational getAudioTimebase() {
		return audioTimebase;
	}

	public void setAudioTimebase(AVRational audioTimebase) {
		this.audioTimebase = audioTimebase;
	}

	public void setVideoConfData(byte[] videoConfData) {
		this.videoConf = videoConfData;
		
	}

	public ConcurrentLinkedQueue<IWebRTCClient> getWebRTCClientList() {
		return webRTCClientList;
	}


	public int getClientCount() {
		return clientCount.intValue();
	}


}
