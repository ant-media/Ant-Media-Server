package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Set;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public abstract class RecordMuxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(RecordMuxer.class);
	protected File fileTmp;
	protected StorageClient storageClient = null;
	protected int resolution;
	
	

	protected boolean uploadMP4ToS3 = true;

	protected String previewPath;

	private String subFolder = null;

	private static final int S3_CONSTANT = 0b001;

	/**
	 * By default first video key frame should be checked
	 * and below flag should be set to true
	 * If first video key frame should not be checked,
	 * then below should be flag in advance
	 */
	protected boolean firstKeyFrameReceivedChecked = false;

	private String s3FolderPath = "streams";

	/**
	 * Millisecond timestamp with record muxer initialization.
	 * It will be define when record muxer is called by anywhere
	 */
	private long startTime = 0;


	public RecordMuxer(StorageClient storageClient, Vertx vertx, String s3FolderPath) {
		super(vertx);
		this.storageClient = storageClient;
		this.s3FolderPath = s3FolderPath;
		firstAudioDts = -1;
		firstVideoDts = -1;
	}

	protected int[] SUPPORTED_CODECS;

	public boolean isCodecSupported(int codecId) {
		for (int i=0; i< SUPPORTED_CODECS.length; i++) {
			if (codecId == SUPPORTED_CODECS[i]) {
				return true;
			}
		}
		return false;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IScope scope, final String name, int resolutionHeight, String subFolder, int bitrate) {
		super.init(scope, name, resolutionHeight, false, subFolder, bitrate);

		this.streamId = name;
		this.resolution = resolutionHeight;
		this.subFolder = subFolder;

		this.startTime = System.currentTimeMillis();

	}



	@Override
	public AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			outputFormatContext= new AVFormatContext(null);
			fileTmp = new File(file.getAbsolutePath() + TEMP_EXTENSION);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, fileTmp.getAbsolutePath());
			if (ret < 0) {
				logger.info("Could not create output context for {}", streamId);
				return null;
			}
		}
		return outputFormatContext;
	}

	protected boolean prepareAudioOutStream(AVStream inStream, AVStream outStream) {
		int ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
		if (ret < 0) {
			logger.info("Cannot get codec parameters for {}", streamId);
			return false;
		}
		return true;
	}
	
	
	@Override
	public String getOutputURL() {
		return fileTmp.getAbsolutePath();
	}
		
	public void setPreviewPath(String path){
		this.previewPath = path;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {

		super.writeTrailer();


		vertx.executeBlocking(l->{
			try {

				IContext context = RecordMuxer.this.scope.getContext();
				ApplicationContext appCtx = context.getApplicationContext();
				AntMediaApplicationAdapter adaptor = (AntMediaApplicationAdapter) appCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);

				AppSettings appSettings = (AppSettings) appCtx.getBean(AppSettings.BEAN_NAME);

				File f = getFinalFileName(appSettings.isS3RecordingEnabled());

				finalizeRecordFile(f);

				adaptor.muxingFinished(streamId, f, startTime, getDurationInMs(f,streamId), resolution, previewPath);

				logger.info("File: {} exist: {}", fileTmp.getAbsolutePath(), fileTmp.exists());


				if((appSettings.getUploadExtensionsToS3()&S3_CONSTANT) == 0){
					this.uploadMP4ToS3 = false;
				}

				if (appSettings.isS3RecordingEnabled() && this.uploadMP4ToS3 ) {
					logger.info("Storage client is available saving {} to storage", f.getName());

					saveToStorage(s3FolderPath + File.separator + (subFolder != null ? subFolder + File.separator : "" ), f, f.getName(), storageClient);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			l.complete();
		}, null);

	}


	public File getFinalFileName(boolean isS3Enabled)
	{
		String absolutePath = fileTmp.getAbsolutePath();
		String origFileName = absolutePath.replace(TEMP_EXTENSION, "");

		String prefix = s3FolderPath + File.separator + (subFolder != null ? subFolder + File.separator : "" );

		String fileName = getFile().getName();

		File f = new File(origFileName);

		if ( isS3Enabled && this.uploadMP4ToS3 && storageClient != null && doesFileExistInS3(storageClient, prefix+fileName)) 
		{
			int i = 0;

			do {
				i++;
				fileName = initialResourceNameWithoutExtension + "_" + i + extension;

				origFileName = origFileName.substring(0, origFileName.lastIndexOf("/") + 1) + fileName;
				f = new File(origFileName);
			} while (doesFileExistInS3(storageClient, prefix+fileName) || f.exists() || f.isDirectory());
		}
		return f;
	}

	private static boolean doesFileExistInS3(StorageClient storageClient, String name) {
		return storageClient.fileExist(name);
	}

	public static void saveToStorage(String prefix, File fileToUpload, String fileName, StorageClient storageClient) {
		saveToStorage(prefix, fileToUpload, fileName, storageClient, true);
	}

	public static void saveToStorage(String prefix, File fileToUpload, String fileName, StorageClient storageClient, boolean deleteLocalFile) {
		storageClient.save(prefix + fileName, fileToUpload, deleteLocalFile);
	}


	protected void finalizeRecordFile(final File file) throws IOException {
		Files.move(fileTmp.toPath(),file.toPath());
		logger.info("{} is ready", file.getName());
	}

	public static long getDurationInMs(File f, String streamId) {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		int ret;
		if (avformat_open_input(inputFormatContext, f.getAbsolutePath(), null, (AVDictionary)null) < 0) {
			logger.info("cannot open input context for duration for stream: {}", streamId);
			avformat_close_input(inputFormatContext);
			return -1L;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information for stream: {}", streamId);
			avformat_close_input(inputFormatContext);
			return -1L;
		}
		long durationInMS = -1;
		if (inputFormatContext.duration() != AV_NOPTS_VALUE)
		{
			durationInMS = inputFormatContext.duration() / 1000;
		}
		avformat_close_input(inputFormatContext);
		return durationInMS;
	}




	@Override
	public boolean checkToDropPacket(AVPacket pkt, int codecType) {
		if (!firstKeyFrameReceivedChecked && codecType == AVMEDIA_TYPE_VIDEO) 
		{
			if(firstVideoDts == -1) {
				firstVideoDts = pkt.dts();
			}

			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			//we set start time here because we start recording with key frame and drop the other
			//setting here improves synch between audio and video
			if (keyFrame == 1) {
				firstKeyFrameReceivedChecked = true;
				logger.warn("First key frame received for stream: {}", streamId);
			} else {
				logger.info("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return true;

			}
		}
		//don't drop packet because it's either audio packet or key frame is received
		return false;
	}
	

	public boolean isUploadingToS3(){return uploadMP4ToS3;}

	public void setExtradataForTest(){
		extradata = "test".getBytes();
	}
}
