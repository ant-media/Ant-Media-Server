package io.antmedia.rest.model;

public class Version {

	/**
	 * Gives information about the version name. 
	 */
	public String versionName;

	/**
	 * Gives information about the version type
	 */
	public String versionType;

	public String getVersionName() {
		return versionName;
	}

	public String getVersionType() {
		return versionType;
	}
	
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public void setVersionType(String versionType) {
		this.versionType = versionType;
	}

	
}
