package io.antmedia.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class AmazonS3StorageClient extends StorageClient {

	private AmazonS3 amazonS3;

	private TransferManager transferManager;

	private long multipartUploadThreshold = 5L * 1024 * 1024;

	protected static Logger logger = LoggerFactory.getLogger(AmazonS3StorageClient.class);


	public AmazonS3 getAmazonS3() {
		if (amazonS3 == null) {
			amazonS3 = initAmazonS3();
		}
		return amazonS3; 
	}

	public AmazonS3 initAmazonS3() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

		// Inject endpoint if provided in the configuration file
		if (getEndpoint() != null && !getEndpoint().isEmpty() && getRegion() != null) {
			builder = builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getEndpoint(), getRegion()));
		}

		// Inject credentials if provided in the configuration file
		if (getAccessKey() != null) {
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(getAccessKey(), getSecretKey());
			builder = builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
		}

		// Inject region if provided in the configuration file
		if ((getEndpoint() == null || getEndpoint().isEmpty()) && getRegion() != null) {
			builder = builder.withRegion(Regions.fromName(getRegion()));
		}
		builder.withClientConfiguration(
				new ClientConfiguration().withMaxConnections(100)
				.withConnectionTimeout(120 * 1000)
				.withMaxErrorRetry(15));

		return builder.build();
	}

	public List<String> getObjects(String prefix) 
	{
		List<String> list = new ArrayList<>();
		if (isEnabled()) {
			AmazonS3 s3 = getAmazonS3();
			ListObjectsV2Result objects = s3.listObjectsV2(getStorageName(), prefix);

			convert2List(list, objects.getObjectSummaries());

		}
		return list;
	}

	public void convert2List(List<String> list, List<S3ObjectSummary> objectSummaries) {
		for (S3ObjectSummary s3ObjectSummary : objectSummaries) 
		{
			list.add(s3ObjectSummary.getKey());
		}
	}

	public void delete(String key) {
		if (isEnabled()) 
		{
			AmazonS3 s3 = getAmazonS3();
			s3.deleteObject(getStorageName(), key);
		}
		else 
		{
			logger.debug("S3 is not enabled to delete the file: {}", key);
		}

	}

	public boolean fileExist(String key) {
		if (isEnabled()) {
			return getAmazonS3().doesObjectExist(getStorageName(), key);
		}
		else {
			logger.debug("S3 is not enabled to check the file existence: {}", key);
		}
		return false;
	}

	public void save(final File file, String type) {
		save(type + "/" + file.getName(), file);
	}


	public void save(String key, InputStream inputStream, boolean waitForCompletion) {
		save(key, null, inputStream, false, waitForCompletion);
	}

	
	public void save(String key, File file, boolean deleteLocalFile) {
		save(key, file, null, deleteLocalFile, false);
	}
	
	public void save(String key, File file, InputStream inputStream, boolean deleteLocalFile, boolean waitForCompletion)
	{	
		if (isEnabled()) 
		{
			TransferManager tm = getTransferManager();
			PutObjectRequest putRequest;
			ObjectMetadata metadata = new ObjectMetadata();
			
			if (getCacheControl() != null) 
			{
				metadata.setCacheControl(getCacheControl());
			}
		
			
			if (file != null) 
			{
				putRequest = new PutObjectRequest(getStorageName(), key, file).withMetadata(metadata);
			}
			else 
			{
				putRequest = new PutObjectRequest(getStorageName(), key, inputStream, metadata);
			}

			putRequest.setCannedAcl(getCannedAcl());

			if(checkStorageClass(getStorageClass())){
				putRequest.withStorageClass(getStorageClass().toUpperCase());
			}

			Upload upload = tm.upload(putRequest);
			/* 
			 * TransferManager processes all transfers asynchronously, so this call returns immediately.
			 * Some blocking calls are removed. Please don't block any threads if it's really not necessary
			 */
			logger.info("File {} upload has started with key: {}", file != null ? file.getName() : "", key);

			listenUploadProgress(key, file, deleteLocalFile, upload);
			
			if (waitForCompletion) {
				try {
					upload.waitForCompletion();
				} catch (AmazonClientException e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
				catch (InterruptedException e) {
					logger.error(ExceptionUtils.getStackTrace(e));
					Thread.currentThread().interrupt();
				}
			}
		}
		else {
			logger.debug("S3 is not enabled to save the file: {}", key);
		}
	}

	private void listenUploadProgress(String key, File file, boolean deleteLocalFile, Upload upload) {
		upload.addProgressListener((ProgressListener)event -> 
		{
			if (event.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT)
			{
				logger.error("S3 - Error: Upload failed with key {}", key);
			}
			else if (event.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT)
			{	
				if (deleteLocalFile) 
				{
					deleteFile(file);
				}
				logger.info("File uploaded to S3 with key: {}", key);
			}

			if (progressListener != null) 
			{
				progressListener.progressChanged(event);
			}
		});
	}
	public boolean checkStorageClass(String s3StorageClass)
	{
		logger.debug("Requested storage class = {}" , s3StorageClass);
		//All of the inputs are upper case and case sensitive like GLACIER
		for(int i = 0; i < StorageClass.values().length; i++)
		{
			if(StorageClass.values()[i].toString().equalsIgnoreCase(s3StorageClass) ){
				return true;
			}
		}
		return false;
	}


	public TransferManager getTransferManager() {
		if (transferManager == null) {
			transferManager = TransferManagerBuilder.standard()
					.withS3Client(getAmazonS3())
					.withMultipartUploadThreshold(multipartUploadThreshold )
					.build();
		}
		return transferManager;
	}

	public void deleteFile(File file) {
		try {
			Files.delete(file.toPath());
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	@Override
	public void reset() {
		this.amazonS3 = null;
		this.transferManager = null;

	}


	public CannedAccessControlList getCannedAcl() 
	{
		switch (getPermission()) 
		{
		case "public-read":
			return CannedAccessControlList.PublicRead;
		case "private":
			return CannedAccessControlList.Private;
		case "public-read-write":
			return CannedAccessControlList.PublicReadWrite;
		case "authenticated-read":
			return CannedAccessControlList.AuthenticatedRead;
		case "log-delivery-write":
			return CannedAccessControlList.LogDeliveryWrite;
		case "bucket-owner-read":
			return CannedAccessControlList.BucketOwnerRead;
		case "bucket-owner-full-control":
			return CannedAccessControlList.BucketOwnerFullControl;
		case "aws-exec-read":
			return CannedAccessControlList.AwsExecRead;
		default: 
			break;
		}
		return CannedAccessControlList.PublicRead;
	}

	public long getMultipartUploadThreshold() {
		return multipartUploadThreshold;
	}

	public void setMultipartUploadThreshold(long multipartUploadThreshold) {
		this.multipartUploadThreshold = multipartUploadThreshold;
	}

}
