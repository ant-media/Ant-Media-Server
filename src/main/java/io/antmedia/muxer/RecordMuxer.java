package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public abstract class RecordMuxer extends Muxer {

	protected File fileTmp;
	protected StorageClient storageClient = null;
	protected int resolution;
	
	protected boolean uploadMP4ToS3 = true;

	protected String previewPath;

	private String subFolder = null;

	private static final int S3_CONSTANT = 0b001;

	private String s3FolderPath = "streams";

	/**
	 * Millisecond timestamp with record muxer initialization.
	 * It will be define when record muxer is called by anywhere
	 */
	private long startTime = 0;
	
	private String vodId;


	protected RecordMuxer(StorageClient storageClient, Vertx vertx, String s3FolderPath) {
		super(vertx);
		this.storageClient = storageClient;
		this.s3FolderPath = s3FolderPath;
		firstAudioDts = -1;
		firstVideoDts = -1;
		firstKeyFrameReceived = false;
		
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
		if (outputFormatContext == null) 
		{
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


		vertx.executeBlocking(()->{
			try {

				AntMediaApplicationAdapter adaptor = getAppAdaptor();

				AppSettings appSettings = getAppSettings();

				File f = getFinalFileName(appSettings.isS3RecordingEnabled());

				finalizeRecordFile(f);

				adaptor.muxingFinished(streamId, f, startTime, getDurationInMs(f,streamId), resolution, previewPath, vodId);

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
			return null;
		});

	}

	public AntMediaApplicationAdapter getAppAdaptor() {
		IContext context = RecordMuxer.this.scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		AntMediaApplicationAdapter adaptor = (AntMediaApplicationAdapter) appCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		return adaptor;
	}

	
	public static String getS3Prefix(String s3FolderPath, String subFolder) {
		return replaceDoubleSlashesWithSingleSlash(s3FolderPath + File.separator + (subFolder != null ? subFolder : "" ) + File.separator);
	}

	public File getFinalFileName(boolean isS3Enabled)
	{
		String absolutePath = fileTmp.getAbsolutePath();
		String origFileName = absolutePath.replace(TEMP_EXTENSION, "");

		String prefix = getS3Prefix(s3FolderPath, subFolder);
		
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


	

	public boolean isUploadingToS3(){return uploadMP4ToS3;}

	public String getVodId() {
		return vodId;
	}

	public void setVodId(String vodId) {
		this.vodId = vodId;
	}


}
