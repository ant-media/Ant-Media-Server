package io.antmedia.datastore.db.types;

import java.io.Serializable;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;


@Entity("vod")
@Indexes({ @Index(fields = @Field("vodId")), @Index(fields = @Field("vodName")), @Index(fields = @Field("streamId")), @Index(fields = @Field("streamName")) })
@Schema(description = "The recorded video-on-demand object class")
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

    @Schema(description = "The object id of the VoD")
    private String streamName;

    @Schema(description = "The name of the VoD")
    private String vodName;

    @Schema(description = "The stream id of the VoD")
    private String streamId;

    @Schema(description = "The creation date of the VoD")
    private long creationDate;

    @Schema(description = "The start time of the VoD recording in milliseconds (UTC- Unix epoch)")
    private long startTime;

    @Schema(description = "The duration of the VoD")
    private long duration;

    @Schema(description = "The size of the VoD file in bytes")
    private long fileSize;

    @Schema(description = "The path of the VoD")
    private String filePath;

    @Schema(description = "The id of the VoD")
    private String vodId;

    @Schema(description = "The type of the VoD, such as userVod, streamVod, uploadedVod")
    private String type;

    @Schema(description = "The file path for the preview of the VoD")
    private String previewFilePath;
	

	public VoD() {
		//default constructor is used to return not found vod in rest service 
	}
	
	public VoD(String streamName, String streamId, String filePath, String vodName, long creationDate, long startTime, long duration,
			long fileSize, String type, String vodId, String previewFilePath) {

		this.streamName = streamName;
		this.streamId = streamId;
		this.vodName = vodName;
		this.creationDate = creationDate;
		this.startTime = startTime;
		this.duration = duration;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.type = type;
		this.vodId = vodId;
		this.previewFilePath = previewFilePath;

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
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
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

	public String getPreviewFilePath() {
		return previewFilePath;
	}

	public void setPreviewFilePath(String previewFilePath) {
		this.previewFilePath = previewFilePath;
	}

}