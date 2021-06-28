package io.antmedia.datastore.db.types;

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

@Entity("detection")

@Indexes({ @Index(fields = @Field("dbId")) })

@ApiModel(value="TensorFlowObject", description="The TensorFlow detected object class")
public class TensorFlowObject {
	
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the id of the detected object")
	private ObjectId dbId;
	
	/**
	 * Name of the object
	 */
	@ApiModelProperty(value = "the name of the detected object")
	public String objectName;
	
	/**
	 * % percent of the recognition probability
	 */
	@ApiModelProperty(value = "the probablity of the detected object")
	public float probability;
	
	/**
	 * Detection time
	 */
	@ApiModelProperty(value = "the time of the detected object")
	public long detectionTime;
	
	@ApiModelProperty(value = "the id of the detected image")
	private String imageId;
	
	@ApiModelProperty(value = "the x coordinate of upper-left corner of detected object frame")
	private double minX;
	
	@ApiModelProperty(value = "the y coordinate of upper-left corner of detected object frame")
	private double minY;
	
	@ApiModelProperty(value = "the x coordinate of lower-right corner of detected object frame")
	private double maxX;
	
	@ApiModelProperty(value = "the y coordinate of lower-right corner of detected object frame")
	private double maxY;
	
	
	public TensorFlowObject(String name, float probability, String imageId) {
		this.objectName = name;
		this.probability = probability;
		this.imageId = imageId;
	}

	public TensorFlowObject(String objectName, float probability, long detectionTime) {
		this.objectName = objectName;
		this.probability = probability;
		this.detectionTime = detectionTime;
	}
	
	public TensorFlowObject(){
		
	}

	public long getDetectionTime() {
		return detectionTime;
	}

	public void setDetectionTime(long detectionTime) {
		this.detectionTime = detectionTime;
	}



	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public double getMinX() {
		return minX;
	}

	public void setMinX(double minX) {
		this.minX = minX;
	}

	public double getMinY() {
		return minY;
	}

	public void setMinY(double minY) {
		this.minY = minY;
	}

	public double getMaxX() {
		return maxX;
	}

	public void setMaxX(double maxX) {
		this.maxX = maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	public void setMaxY(double maxY) {
		this.maxY = maxY;
	}
}