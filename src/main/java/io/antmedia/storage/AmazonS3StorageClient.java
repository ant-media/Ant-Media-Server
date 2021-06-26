package io.antmedia.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
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
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class AmazonS3StorageClient extends StorageClient {

	private AmazonS3 amazonS3;

	protected static Logger logger = LoggerFactory.getLogger(AmazonS3StorageClient.class);

	public AmazonS3 getAmazonS3() {
		if (amazonS3 == null || isS3ConfChanged()) {
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
			builder.withClientConfiguration(new ClientConfiguration().withMaxConnections(100)
					.withConnectionTimeout(120 * 1000)
					.withMaxErrorRetry(15));

			setS3ConfChanged(false);
			amazonS3 = builder.build();
		}
		return amazonS3; 
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

	public void save(String key, File file)
	{	
		if (isEnabled()) {
			AmazonS3 s3 = getAmazonS3();

			TransferManager tm = TransferManagerBuilder.standard()
					.withS3Client(s3)
					.build();

			PutObjectRequest putRequest = new PutObjectRequest(getStorageName(), key, file);


			putRequest.setCannedAcl(getCannedAcl());


			Upload upload = tm.upload(putRequest);
			// TransferManager processes all transfers asynchronously,
			// so this call returns immediately.
			//Upload upload = tm.upload(getStorageName(), key, file);
			logger.info("Mp4 {} upload has started with key: {}", file.getName(), key);

			upload.addProgressListener((ProgressListener) event -> 
			{
				if (event.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT){
					logger.error("S3 - Error: Upload failed for {} with key {}", file.getName(), key);
				}
				else if (event.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT){
					try {
						Files.delete(file.toPath());
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					logger.info("File {} uploaded to S3 with key: {}", file.getName(), key);
				}
			});


			// Optionally, wait for the upload to finish before continuing.
			try {  
				upload.waitForCompletion();

				logger.info("Mp4 {} upload completed", file.getName());
			} catch (AmazonServiceException e1) {
				logger.error(ExceptionUtils.getStackTrace(e1));
			} catch (InterruptedException e1) {
				logger.error(ExceptionUtils.getStackTrace(e1));
				Thread.currentThread().interrupt();
			}
		}
		else {
			logger.debug("S3 is not enabled to save the file: {}", key);
		}

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
		}
		return CannedAccessControlList.PublicRead;
	}

}
