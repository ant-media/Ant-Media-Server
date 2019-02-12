package io.antmedia.rest.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Version", description="The version class")
public class Version {

	/**
	 * Gives information about the version name. 
	 */
	@ApiModelProperty(value = "Version of the software")
	public String versionName;

	/**
	 * Gives information about the version type
	 */
	@ApiModelProperty(value = "Version type of the software (Community or Enterprise)")
	public String versionType;

	@ApiModelProperty(value = "Build number(timestamp) of the software.")
	private String buildNumber;

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

	public void setBuildNumber(String value) {
		this.buildNumber = value;
	}
	
	public String getBuildNumber() {
		return buildNumber;
	}

	
}
