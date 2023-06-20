package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H265;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public class HLSMuxer extends Muxer  {

	private static final String SEGMENT_SUFFIX_TS = "%0"+SEGMENT_INDEX_LENGTH+"d.ts";

	protected static Logger logger = LoggerFactory.getLogger(HLSMuxer.class);
	private String  hlsListSize = "20";
	private String hlsTime = "5";
	private String hlsPlayListType = null;

	private long totalSize;
	private long startTime;
	private long currentTime;


	private boolean deleteFileOnExit = true;
	private String hlsFlags;

	private String hlsEncryptionKeyInfoFile = null;

	protected StorageClient storageClient = null;
	private String subFolder = null;
	private String s3StreamsFolderPath = "streams";
	private boolean uploadHLSToS3 = true;
	private String segmentFilename;

	private String httpEndpoint;
	public static final int S3_CONSTANT = 0b010;
	
	public HLSMuxer(Vertx vertx, StorageClient storageClient, String s3StreamsFolderPath, int uploadExtensionsToS3, String httpEndpoint, boolean addDateTimeToResourceName) {
		super(vertx);
		this.storageClient = storageClient;

		if((S3_CONSTANT & uploadExtensionsToS3) == 0){
			uploadHLSToS3 = false;
		}

		extension = ".m3u8";
		format = "hls";
		firstKeyFrameReceived = false;
		
		firstAudioDts = -1;
		firstVideoDts = -1;

		this.s3StreamsFolderPath  = s3StreamsFolderPath;
		this.httpEndpoint = httpEndpoint;
		setAddDateTimeToSourceName(addDateTimeToResourceName);
	}

	public void setHlsParameters(String hlsListSize, String hlsTime, String hlsPlayListType, String hlsFlags, String hlsEncryptionKeyInfoFile){
		if (hlsListSize != null && !hlsListSize.isEmpty()) {
			this.hlsListSize = hlsListSize;
		}

		if (hlsTime != null && !hlsTime.isEmpty()) {
			this.hlsTime = hlsTime;
		}

		if (hlsPlayListType != null && !hlsPlayListType.isEmpty()) {
			this.hlsPlayListType = hlsPlayListType;
		}

		if (hlsFlags != null && !hlsFlags.isEmpty()) {
			this.hlsFlags = hlsFlags;
		}
		else {
			this.hlsFlags = "";
		}
		if (hlsEncryptionKeyInfoFile != null && !hlsEncryptionKeyInfoFile.isEmpty()) {
			this.hlsEncryptionKeyInfoFile = hlsEncryptionKeyInfoFile;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IScope scope, String name, int resolutionHeight, String subFolder, int bitrate) {
		if (!isInitialized) {

			super.init(scope, name, resolutionHeight, subFolder, bitrate);

			streamId = name;
			this.subFolder = subFolder;
			options.put("hls_list_size", hlsListSize);
			options.put("hls_time", hlsTime);

			if(hlsEncryptionKeyInfoFile != null) {
				options.put("hls_key_info_file", hlsEncryptionKeyInfoFile);
			}

			logger.info("hls time: {}, hls list size: {} for stream:{}", hlsTime, hlsListSize, streamId);

			if (StringUtils.isNotBlank(httpEndpoint)) 			
			{
				segmentFilename = httpEndpoint + File.separator + (this.subFolder != null ? subFolder : "") + initialResourceNameWithoutExtension;
			}
			else {
				segmentFilename = file.getParentFile() + File.separator + initialResourceNameWithoutExtension;
			}
			segmentFilename += SEGMENT_SUFFIX_TS;
					
			options.put("hls_segment_filename", segmentFilename);

			if (hlsPlayListType != null && (hlsPlayListType.equals("event") || hlsPlayListType.equals("vod"))) {
				options.put("hls_playlist_type", hlsPlayListType);
			}

			if (this.hlsFlags != null && !this.hlsFlags.isEmpty()) {
				options.put("hls_flags", this.hlsFlags);
			}

			isInitialized = true;
		}

	}
	
	@Override
	public String getOutputURL() 
	{
		if (StringUtils.isNotBlank(httpEndpoint))
		{
			return httpEndpoint + File.separator + initialResourceNameWithoutExtension  + extension;
		}
		return super.getOutputURL();
	}


	public AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {

			outputFormatContext= new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, getOutputURL());
			if (ret < 0) {
				logger.info("Could not create output context for {}",  getOutputURL());
				return null;
			}
		}
		return outputFormatContext;
	}

	@Override
	public boolean isCodecSupported(int codecId) {
		return (codecId == AV_CODEC_ID_H264 || codecId == AV_CODEC_ID_AAC || codecId == AV_CODEC_ID_MP3 || codecId == AV_CODEC_ID_H265);
	}

	public long getAverageBitrate() {

		long duration = (currentTime - startTime) ;

		if (duration > 0)
		{
			return (totalSize / duration) * 8;
		}
		return 0;
	}
	

	@Override
	public synchronized void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType)
	{
		
		totalSize += pkt.size();
		
		currentTime = av_rescale_q(pkt.dts(), inputTimebase, avRationalTimeBase);
		if (startTime == 0) {
			startTime = currentTime;
		}
		
		super.writePacket(pkt, inputTimebase, outputTimebase, codecType);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {
		super.writeTrailer();
		
		if (!StringUtils.isNotBlank(this.httpEndpoint)) 
		{
			logger.info("Delete File onexit:{} upload to S3:{} stream:{} hls time:{} hlslist size:{}",
					deleteFileOnExit, uploadHLSToS3, streamId, hlsTime, hlsListSize);
			vertx.setTimer(Integer.parseInt(hlsTime) * Integer.parseInt(hlsListSize) * 1000, l -> {
				final String filenameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf(extension));
	
				//SEGMENT_SUFFIX_TS is %09d.ts
				//convert segmentFileName to regular expression
				String segmentFileWithoutSuffixTS = segmentFilename.substring(segmentFilename.lastIndexOf("/")+1, segmentFilename.indexOf(SEGMENT_SUFFIX_TS));
				String regularExpression = segmentFileWithoutSuffixTS + "[0-9]*\\.ts$";
				File[] files = file.getParentFile().listFiles((dir, name) -> 
				
					//matches m3u8 file or ts segment file
					name.equals(file.getName()) || name.matches(regularExpression)
				);
	
				if (files != null)
				{
	
					for (int i = 0; i < files.length; i++) 
					{
						try {
							if (!files[i].exists()) {
								continue;
							}
							if(uploadHLSToS3 && storageClient.isEnabled()) 
							{
								storageClient.save(s3StreamsFolderPath + File.separator + (subFolder != null ? subFolder + File.separator : "" ) + files[i].getName(), files[i], deleteFileOnExit);
							}
							else if (deleteFileOnExit) 
							{
								Files.delete(files[i].toPath());
							}
						} catch (IOException e) {
							logger.error(e.getMessage());
						}
					}
				}
				
			});
		}
		else {
			logger.info("http endpoint is {} so skipping delete or upload the m3u8 or ts files", httpEndpoint);
		}


	}


	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {
		AVCodecParameters codecParameter = new AVCodecParameters();
		int ret = avcodec_parameters_from_context(codecParameter, codecContext);
		if (ret < 0) {
			logger.error("Cannot get codec parameters for {}", streamId);
		}
		
		//call super directly because no need to add bit stream filter 
		return super.addStream(codecParameter, codecContext.time_base(), streamIndex);
	}
	

	@Override
	public synchronized boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) 
	{
		bsfVideoName = "h264_mp4toannexb";
		return super.addStream(codecParameters, timebase, streamIndex);
	}
	
	public String getHlsListSize() {
		return hlsListSize;
	}

	public void setHlsListSize(String hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public String getHlsTime() {
		return hlsTime;
	}

	public void setHlsTime(String hlsTime) {
		this.hlsTime = hlsTime;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public boolean isDeleteFileOnExit() {
		return deleteFileOnExit;
	}

	public void setDeleteFileOnExit(boolean deleteFileOnExist) {
		this.deleteFileOnExit = deleteFileOnExist;
	}

	public boolean isUploadingToS3(){
		return uploadHLSToS3;
	}

	public String getSegmentFilename() {
		return segmentFilename;

	}
}
