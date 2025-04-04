package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public class HLSMuxer extends Muxer  {

	
	public static final String SEI_USER_DATA = "sei_user_data";
	
	private static final String LETTER_DOT = ".";
	private static final String TS_EXTENSION = "ts";
	private static final String FMP4_EXTENSION = "fmp4";

	private static final String HLS_SEGMENT_TYPE_MPEGTS = "mpegts";
	private static final String HLS_SEGMENT_TYPE_FMP4 = "fmp4";



	protected static Logger logger = LoggerFactory.getLogger(HLSMuxer.class);
	private String  hlsListSize = "20";
	private String hlsTime = "5";
	private String hlsPlayListType = null;


	private boolean deleteFileOnExit = true;
	private String hlsFlags;
	
	private String segmentInitFilename;;

	private String hlsEncryptionKeyInfoFile = null;

	protected StorageClient storageClient = null;
	private String s3StreamsFolderPath = "streams";
	private boolean uploadHLSToS3 = true;
	private String segmentFilename;

	/**
	 * HLS Segment Type. It can be "mpegts" or "fmp4"
	 * 
	 * Note: The generated M3U8 for HEVC can be playable when it's fmp4 
	 * It's not playable when it's mpegts
	 */
	private String hlsSegmentType = HLS_SEGMENT_TYPE_MPEGTS;

	private String httpEndpoint;
	public static final int S3_CONSTANT = 0b010;

	//TODO: make this configurable
	private int id3StreamIndex = 2;
	private AVPacket id3DataPkt;

	private boolean id3Enabled = false;

	private ByteBuffer pendingSEIData;

	private AVPacket tmpPacketForSEI;

	private String segmentFileNameSuffix;

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
	
	public void setHlsParameters(String hlsListSize, String hlsTime, String hlsPlayListType, String hlsFlags, String hlsEncryptionKeyInfoFile, String hlsSegmentType){
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
		
		if (StringUtils.isNotBlank(hlsSegmentType)) {
			this.hlsSegmentType = hlsSegmentType;
		}
	}

	/**
	 * {@inheritDoc}
	 */
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

			logger.info("hls time:{}, hls list size:{} hls playlist type:{} for stream:{}", hlsTime, hlsListSize, this.hlsPlayListType, streamId);

			if (StringUtils.isNotBlank(httpEndpoint)) {
				segmentFilename = httpEndpoint;
				segmentFilename += !segmentFilename.endsWith(File.separator) ? File.separator : "";
				segmentFilename += (this.subFolder != null ? subFolder : "");
				segmentFilename += !segmentFilename.endsWith(File.separator) ? File.separator : "";
				segmentFilename += initialResourceNameWithoutExtension;
			} else {
				segmentFilename = file.getParentFile().toString();
				segmentFilename += !segmentFilename.endsWith(File.separator) ? File.separator : "";
				segmentFilename += initialResourceNameWithoutExtension;
			}
			
			segmentFileNameSuffix = getAppSettings().getHlsSegmentFileSuffixFormat();

			if(segmentFileNameSuffix.contains("%s") || segmentFileNameSuffix.contains("%Y") || segmentFileNameSuffix.contains("%m")) {
				options.put("strftime", "1");
			}
			
			segmentFilename += getAppSettings().getHlsSegmentFileSuffixFormat();

			//remove double slashes with single slash because it may cause problems
			segmentFilename = replaceDoubleSlashesWithSingleSlash(segmentFilename);

			segmentFilename += LETTER_DOT;
			options.put("hls_segment_type", hlsSegmentType);
			if (HLS_SEGMENT_TYPE_FMP4.equals(hlsSegmentType)) {

				segmentInitFilename = initialResourceNameWithoutExtension + "_" + System.currentTimeMillis() + "_init.mp4";
				options.put("hls_fmp4_init_filename", segmentInitFilename);
				segmentFilename += FMP4_EXTENSION;
			} else { //if it's mpegts
				segmentFilename += TS_EXTENSION;
			}
			

			options.put("hls_segment_filename", segmentFilename);

			if (hlsPlayListType != null && (hlsPlayListType.equals("event") || hlsPlayListType.equals("vod"))) 
			{
				options.put("hls_playlist_type", hlsPlayListType);
			}

			if (this.hlsFlags != null && !this.hlsFlags.isEmpty()) {
				options.put("hls_flags", this.hlsFlags);
			}
			
			
			tmpPacketForSEI = avcodec.av_packet_alloc();
			isInitialized = true;
		}

	}

	@Override
	public String getOutputURL() 
	{
		if (StringUtils.isNotBlank(httpEndpoint))
		{
			return replaceDoubleSlashesWithSingleSlash(httpEndpoint + File.separator + (this.subFolder != null ? subFolder : "") + File.separator + initialResourceNameWithoutExtension  + extension);
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
		return (codecId == AV_CODEC_ID_H264 
				|| codecId == AV_CODEC_ID_AAC  
				|| codecId == AV_CODEC_ID_MP3  
				|| codecId == AV_CODEC_ID_H265 
				|| codecId == AV_CODEC_ID_AC3);
	}
	
	@Override
	public synchronized void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType)
	{
			
		if (codecType == AVMEDIA_TYPE_VIDEO && pendingSEIData != null) {
			
			logger.info("sei data size:{} for streamId:{}", pendingSEIData.limit(), streamId);
				
			//inject SEI NAL Unit
			pendingSEIData.rewind();
			int newPacketSize = pkt.size() + pendingSEIData.limit();
						
			av_packet_ref(tmpPacketForSEI, pkt);
			tmpPacketForSEI.position(0);
			
			ByteBuffer packetbuffer = ByteBuffer.allocateDirect(newPacketSize);
			
			packetbuffer.put(pendingSEIData);
			packetbuffer.put(pkt.data().position(0).limit(pkt.size()).asByteBuffer());
			
			packetbuffer.position(0);
			
			tmpPacketForSEI.data(new BytePointer(packetbuffer));
			tmpPacketForSEI.data().position(0).limit(newPacketSize);
			tmpPacketForSEI.size(packetbuffer.limit());			
	
			pendingSEIData = null;
			
			super.writePacket(tmpPacketForSEI, inputTimebase, outputTimebase, codecType);
			
			av_packet_unref(tmpPacketForSEI);
			
		} 
		else {
			super.writePacket(pkt, inputTimebase, outputTimebase, codecType);
		}

	}
	
	/**
	 * We write metadata as ID3 tag for HLS Muxer
	 */
	@Override
	public synchronized void writeMetaData(String data, long dts) {
		addID3Data(data);
	}


	public static byte[] convertIntToID3v2TagSize(int size) {
		byte[] tagSizeBytes = new byte[4];
		tagSizeBytes[0] = (byte) ((size >> 21) & 0x7F);
		tagSizeBytes[1] = (byte) ((size >> 14) & 0x7F);
		tagSizeBytes[2] = (byte) ((size >> 7) & 0x7F);
		tagSizeBytes[3] = (byte) (size & 0x7F);
		return tagSizeBytes;
	}

	public synchronized void addID3Data(String data) {
		int frameSizeWithoutFrameHeader = data.length() + 3; // TXXX frame size, 3 is for encoding (1), description (1) and end of string (1) (https://id3.org/id3v2.3.0#User_defined_text_information_frame)
		int tagSize = frameSizeWithoutFrameHeader + 10; // 10 is for frame header which is "TXXX" frame id (4), frame size info(4) and frame flags (2)  (https://id3.org/id3v2.3.0#ID3v2_frame_overview)
		int id3ContentSize = tagSize + 10; // 10 is for ID3 header which is "ID3" (3), version (2), flags (1) and size info(4) (https://id3.org/id3v2.3.0#ID3v2_header)

		ByteBuffer byteBuffer = ByteBuffer.allocate(id3ContentSize);
		
		logger.debug("Adding ID3 data: {} lenght:{} to streamId:{} endpoint:{}", data, data.length(), byteBuffer.capacity(), streamId, getOutputURL());

		// ID3 header (https://id3.org/id3v2.3.0#ID3v2_header)
		byteBuffer.put("ID3".getBytes());
		byteBuffer.put(new byte[]{0x03, 0x00}); // version
		byteBuffer.put((byte) 0x00); // flags

		byteBuffer.put(convertIntToID3v2TagSize(tagSize)); // size

		// TXXX frame header (https://id3.org/id3v2.3.0#ID3v2_frame_overview)
		byteBuffer.put("TXXX".getBytes()); // frame id
		byteBuffer.putInt(frameSizeWithoutFrameHeader); // frame size without frame header
		byteBuffer.put(new byte[]{0x00, 0x00}); // frame flags

		//TXXX frame content (https://id3.org/id3v2.3.0#User_defined_text_information_frame)
		byteBuffer.put((byte) 0x03); // encoding
		byteBuffer.put((byte) 0x00); // description 00
		byteBuffer.put(data.getBytes()); // description
		byteBuffer.put((byte) 0x00); // end of string

		byteBuffer.rewind();

		writeID3Packet(byteBuffer);
	}

	public synchronized void writeID3Packet(ByteBuffer data)
	{
		if (!id3Enabled) {
			logger.info("ID3 tag is disabled for stream:{}", streamId);
			return;
		}
		//use the last send video pts as the pts of data
		long pts = getLastPts();
		id3DataPkt.pts(pts);
		id3DataPkt.dts(pts);
		id3DataPkt.stream_index(id3StreamIndex);

		id3DataPkt.data(new BytePointer(data));
		id3DataPkt.size(data.limit());
		id3DataPkt.position(0);

		writeDataFrame(id3DataPkt, getOutputFormatContext());
	}

	@Override
	public boolean writeHeader() {
		createID3StreamIfRequired();
		return super.writeHeader();
	}

	public void createID3StreamIfRequired() {
		if(id3Enabled) {
			logger.info("ID3 tag is enabled for stream:{}", streamId);
			id3DataPkt = avcodec.av_packet_alloc();
			av_init_packet(id3DataPkt);

			addID3Stream();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {
		if(!isRunning.get())
			return;

		super.writeTrailer();
		
		if (StringUtils.isBlank(this.httpEndpoint)) 
		{
			logger.info("Delete File onexit:{} upload to S3:{} stream:{} hls time:{} hlslist size:{}",
					deleteFileOnExit, uploadHLSToS3, streamId, hlsTime, hlsListSize);
			
			vertx.setTimer(Integer.parseInt(hlsTime) * Integer.parseInt(hlsListSize) * 1000l, l -> 
			{
				//final String filenameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf(extension));
	
				//SEGMENT_SUFFIX_TS is %09d.ts
				//convert segmentFileName to regular expression
				
				int indexOfSuffix = 0;
				indexOfSuffix = segmentFilename.indexOf(segmentFileNameSuffix);
				
				String segmentFileWithoutSuffix = segmentFilename.substring(segmentFilename.lastIndexOf("/")+1, indexOfSuffix);
				String regularExpression = segmentFileWithoutSuffix + ".*\\.(?:" + TS_EXTENSION +"|" + FMP4_EXTENSION +")$";
				File[] files = getHLSFilesInDirectory(regularExpression);
	
				if (files != null)
				{
	
					for (int i = 0; i < files.length; i++) 
					{
						
						handleFinalization(files[i]);
					}
				}
				
				if (segmentInitFilename != null) {
					handleFinalization(new File(file.getParentFile() + File.separator + segmentInitFilename));					
				}
				
				
				
			});
		}
		else {
			logger.info("http endpoint is {} so skipping delete or upload the m3u8 or ts files", httpEndpoint);
		}


	}

	private void handleFinalization(File file) {
		
		try {
			if (uploadHLSToS3 && storageClient.isEnabled()) 
			{
				String path = replaceDoubleSlashesWithSingleSlash(s3StreamsFolderPath + File.separator
						+ (subFolder != null ? subFolder : "") + File.separator + file.getName());
				storageClient.save(path, file, deleteFileOnExit);
			} else if (deleteFileOnExit) {
				Files.deleteIfExists(file.toPath());
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public File[] getHLSFilesInDirectory(String regularExpression) {
		return file.getParentFile().listFiles((dir, name) -> 
		
			//matches m3u8 file or ts segment file
			name.equals(file.getName()) || name.matches(regularExpression)
		);
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

	

	public synchronized void setSeiData(String data) {
		
		
		int nbStreams = getOutputFormatContext().nb_streams();
		
		boolean hevcCodec = false;
		boolean h264Codec = false;
		for (int i = 0; i < nbStreams; i++) {
			AVStream stream = getOutputFormatContext().streams(i);
			if (stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
				if (stream.codecpar().codec_id() == AV_CODEC_ID_H264) {
					h264Codec = true;
				}
				else if (stream.codecpar().codec_id() == AV_CODEC_ID_H265) {
                    hevcCodec = true;
                }
			}
		}
		
		if (!h264Codec && !hevcCodec) {
            logger.warn("There is no video stream in the muxer, so cannot add SEI data to the muxer. Stream id: {}", streamId);
            return;
		}
		
		//according to the documentation SEI data is  UUID(128bit(16 byte)) + data
		
		// nal unit becomes 00 00 01 + NAL type + SEI type + payload size + payload + align bits
		int length = data.getBytes().length;
		int payloadSize = 16 + length;
		
		int lengthByteCount = payloadSize / 0xff;

		int remaining = payloadSize % 0xff;
		if (remaining != 0) {
			lengthByteCount++;
		}
				
		int totalLength = 4 + 1 + 1 + lengthByteCount + payloadSize + 1;
		

		if (hevcCodec) {
			totalLength += 1; //because of nal unit header is 2 bytes
		}
		pendingSEIData = ByteBuffer.allocateDirect(totalLength);

		
		if (StringUtils.equals(getBitStreamFilter(), BITSTREAM_FILTER_H264_MP4TOANNEXB) || StringUtils.equals(getBitStreamFilter(), BITSTREAM_FILTER_HEVC_MP4TOANNEXB)
				|| HLS_SEGMENT_TYPE_FMP4.equals(hlsSegmentType)) 
		{
			pendingSEIData.putInt(totalLength-4); 

		}
		else {
			pendingSEIData.put((byte)0x00); //start code
			pendingSEIData.put((byte)0x00); //start code
			pendingSEIData.put((byte)0x00); //start code
			pendingSEIData.put((byte)0x01); //start code
		}
		if (h264Codec) {
			pendingSEIData.put((byte) 0x06); // NAL type
		} 
		else 
		{ //HEVC
			pendingSEIData.put((byte) 0x4E); // NAL type
			pendingSEIData.put((byte) 0x01); // NAL type
		}
		pendingSEIData.put((byte)0x05); //SEI type
		
		
		for (int i = 0; i < lengthByteCount-1; i++) {
			pendingSEIData.put((byte) 0xff);
		}
		
		pendingSEIData.put((byte)remaining);  //if payload size is bigger than 0xff, it should be 2 bytes
		
		UUID uuid = UUID.randomUUID();
		
	    pendingSEIData.putLong(uuid.getMostSignificantBits());
	    pendingSEIData.putLong(uuid.getLeastSignificantBits());
        pendingSEIData.put(data.getBytes());		
		pendingSEIData.put((byte)0x80); //RBSP to align the bits
		pendingSEIData.rewind();
		
		
	}
	
	public static void logError(int ret, String message, String streamId) {
		if (ret < 0 && logger.isErrorEnabled()) {
			logger.error(message, streamId, Muxer.getErrorDefinition(ret));
		}
	}
	

	@Override
	public synchronized boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) 
	{
		
		if (codecParameters.codec_id() == AV_CODEC_ID_H264) {
            setBitstreamFilter(BITSTREAM_FILTER_H264_MP4TOANNEXB);
        }
        else if (codecParameters.codec_id() == AV_CODEC_ID_H265){
        	setBitstreamFilter(BITSTREAM_FILTER_HEVC_MP4TOANNEXB);
        }
        else if (codecParameters.codec_id() == AV_CODEC_ID_AAC && HLS_SEGMENT_TYPE_FMP4.equals(hlsSegmentType)) {
        	//we need this conversion for fmp4
        	setAudioBitreamFilter("aac_adtstoasc");
        }
		
		return super.addStream(codecParameters, timebase, streamIndex);
	}

	public boolean addID3Stream() {
		AVCodecParameters codecParameter = new AVCodecParameters();

		codecParameter.codec_type(AVMEDIA_TYPE_DATA);
		codecParameter.codec_id(AV_CODEC_ID_TIMED_ID3);

		return super.addStream(codecParameter, MuxAdaptor.getTimeBaseForMs(), id3StreamIndex);
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

	public void setId3Enabled(boolean id3Enabled) {
		this.id3Enabled = id3Enabled;
	}

	@Override
	protected synchronized void clearResource() {
		super.clearResource();
		if (id3DataPkt != null) {
			av_packet_free(id3DataPkt);
			id3DataPkt = null;
		}
		
		if (tmpPacketForSEI != null) {
			av_packet_free(tmpPacketForSEI);
			tmpPacketForSEI = null;
		}

	}
	
	public ByteBuffer getPendingSEIData() {
		return pendingSEIData;
	}

}
