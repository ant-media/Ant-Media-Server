package io.antmedia.storage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class GCPStorageClient extends StorageClient {

	protected static Logger logger = LoggerFactory.getLogger(GCPStorageClient.class);

	private Storage storage;

	public Storage getGCPStorage() {
		if (storage == null) {
			storage = StorageOptions.getDefaultInstance().getService();
		}
		return storage;
	}

	public void delete(String key) {
		if (isEnabled()) 
		{
			getGCPStorage().delete(getStorageName(), key);
		}
		else 
		{
			logger.debug("Storage is not enabled to delete the file: {}", key);
		}

	}

	public boolean fileExist(String key) {
		if (isEnabled()) {
			Blob blob = getGCPStorage().get(getStorageName(), key);
			return blob != null;
		}
		else {
			logger.debug("Storage is not enabled to check the file existence: {}", key);
		}
		return false;
	}

	public void save(String key, InputStream inputStream, boolean waitForCompletion) {
		if (isEnabled())
		{
			BlobInfo blobInfo = BlobInfo.newBuilder(getStorageName(), key).build();
			try {
				getGCPStorage().createFrom(blobInfo, inputStream);
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		else {
			logger.debug("Storage is not enabled to save the file: {}", key);
		}
	}

	
	public void save(String key, File file, boolean deleteLocalFile) {
		logger.info("save key:{} file:{}:", key, file.getName());
		BlobInfo blobInfo = BlobInfo.newBuilder(getStorageName(), key).build();
        try {
			getGCPStorage().create(blobInfo, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
        }

        if (deleteLocalFile)
		{
			deleteFile(file);
		}
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
		this.storage = null;
	}
}
