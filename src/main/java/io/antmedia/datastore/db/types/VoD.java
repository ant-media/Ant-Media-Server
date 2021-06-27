package io.antmedia.datastore.db.types;

import java.io.Serializable;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@Entity("vod")

@Indexes({ @Index(fields = @Field("vodId")), @Index(fields = @Field("vodName")), @Index(fields = @Field("streamId")), @Index(fields = @Field("streamName")) })
@ApiModel(value="VoD", description="The recorded video-on-demand object class")
public class VoD implements Serializable {

	private static final long serialVersionUID = 1L;

	
	/**
	 * The vod files that is under a folder which is set in Settings
	 */
	public static final String USER_VOD = "userVod";
	
	/**
	 * The vod files that is generated from live streams
	 */
	public static final String STREAM_VOD = "streamVod";
	
	/**
	 * The vod file user is uploaded 
	 */
	public static final String UPLOADED_VOD = "uploadedVod";
	
	@JsonIgnore
	@Id
	private ObjectId dbId;
	@ApiModelProperty(value = "the object id of the VoD")
	private String streamName;
	
	@ApiModelProperty(value = "the name of the VoD")
	private String vodName;
	
	@ApiModelProperty(value = "the stream id of the VoD")
	private String streamId;
	
	@ApiModelProperty(value = "the creation of the VoD")
	private long creationDate;
	
	@ApiModelProperty(value = "the duration of the VoD")
	private long duration;
	
	@ApiModelProperty(value = "the size of the VoD")
	private long fileSize;
	
	@ApiModelProperty(value = "the path of the VoD")
	private String filePath;
	
	@ApiModelProperty(value = "the id of the VoD")
	private String vodId;
	
	@ApiModelProperty(value = "the type of the VoD, such as userVod, streamVod, uploadedVod")
	private String type;
	

	public VoD() {
		//default constructor is used to return not found vod in rest service 
	}
	
	public VoD(String streamName, String streamId, String filePath, String vodName, long creationDate, long duration,
			long fileSize, String type, String vodId) {

		this.streamName = streamName;
		this.streamId = streamId;
		this.vodName = vodName;
		this.creationDate = creationDate;
		this.duration = duration;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.type = type;
		this.vodId = vodId;

	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public String getVodName() {
		return vodName;
	}

	public void setVodName(String vodName) {
		this.vodName = vodName;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getVodId() {
		return vodId;
	}

	public void setVodId(String vodId) {
		this.vodId = vodId;
	}

}