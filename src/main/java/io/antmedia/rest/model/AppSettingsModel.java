package io.antmedia.rest.model;

import java.util.List;

import io.antmedia.EncoderSettings;

public class AppSettingsModel {
	public boolean mp4MuxingEnabled;
	public boolean addDateTimeToMp4FileName;
	public boolean hlsMuxingEnabled;
	public int hlsListSize;
	public int hlsTime;
	public String hlsPlayListType;

	public String facebookClientId;
	public String facebookClientSecret;

	public String youtubeClientId;
	public String youtubeClientSecret;

	public String periscopeClientId;
	public String periscopeClientSecret;
	
	public boolean acceptOnlyStreamsInDataStore;

	public List<EncoderSettings> encoderSettings;
	
	public String vodFolder;
}
