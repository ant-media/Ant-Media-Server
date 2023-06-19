package io.antmedia.storage;

import java.io.File;
import java.io.InputStream;

import com.amazonaws.event.ProgressListener;

public abstract class StorageClient {
	
	
	public static final String BEAN_NAME = "app.storageClient";

	/**
	 * Endpoint for the storage, it's optional and it's used in some platforms like Digital Ocean, Wasabi, OVH
	 */
	private String endpoint;
	
	/**
	 * Access key to access the storage
	 */
	private String accessKey;
	/**
	 * Secret key for the storage
	 */
	private String secretKey;
	
	/**
	 * The region of the storage. It's like us-west-1 in S3
	 */
	private String region;
	
	/**
	 * Name of the storage. It's bucketname for S3
	 */
	private String storageName;
	
	/**
	 * Permission of the file that is uploaded to the Storage. 
	 * It may differ according to the implemenation. Default value is public-read
	 */
	private String permission = "public-read";

	/**
	 * Cache control policy for the file that is uploaded to the Storage.
	 * It may differ according to the implemenation. Default value is no-store, no-cache, must-revalidate, max-age=0
	 */
	private String cacheControl = "no-store, no-cache, must-revalidate, max-age=0";

	private boolean enabled;

	/**
	 * Storage type. In S3 there is standard, glacier, etc. 
	 */
	private String storageClass;
	
	protected ProgressListener progressListener;

	
	/**
	 * Delete file from storage
	 * 
	 * @param fileName
	 * @param type
	 */
	public abstract void delete(String key);
	
	/**
	 * Save input stream to the storage with key parameter
	 * 
	 * @param key
	 * @param inputStream
	 */
	public abstract void save(String key, InputStream inputStream, boolean waitForCompletion);
	
	/**
	 * Save file to storage and delete the local file according to the parameter
	 * 
	 * @param key
	 * @param file
	 * @param deleteLocalFile
	 */
	public abstract void save(String key, File file, boolean deleteLocalFile);
	
	
	public void setProgressListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}
	
	/**
	 * Save file to storage and delete the local file 
	 * 
	 * @param key
	 * @param file
	 */
	public void save(String key, File file) {
		save(key, file, true);
	}

	/**
	 * Check if the key exists in the bucket
	 * 
	 * @param key
	 * @return
	 */
	public abstract boolean fileExist(String key);
	
	/**
	 * Re-initialize the storage client with the current parameters
	 */
	public abstract void reset();

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getStorageName() {
		return storageName;
	}

	public void setStorageName(String storageName) {
		this.storageName = storageName;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}
	
	public String getPermission() {
		return permission;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;		
	}
	
	public String getStorageClass() {
		return storageClass;
	}

	public String getCacheControl() {
		return cacheControl;
	}

	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}
}
