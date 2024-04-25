package io.antmedia.storage;

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
			logger.debug("S3 is not enabled to delete the file: {}", key);
		}

	}

	public boolean fileExist(String key) {
		if (isEnabled()) {
			Blob blob = getGCPStorage().get(getStorageName(), key);
			return blob != null;
		}
		else {
			logger.debug("S3 is not enabled to check the file existence: {}", key);
		}
		return false;
	}

	public void save(String key, InputStream inputStream, boolean waitForCompletion) {
	}

	
	public void save(String key, File file, boolean deleteLocalFile) {
		BlobInfo blobInfo = BlobInfo.newBuilder(getStorageName(), key).build();
        try {
			getGCPStorage().create(blobInfo, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
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
