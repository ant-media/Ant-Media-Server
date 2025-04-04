package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity("detection")

@Indexes({ @Index(fields = @Field("dbId")) })

@Schema(description="The TensorFlow detected object class")
public class TensorFlowObject {
	
	 /**
     * The id of the detected object.
     */
    @Schema(description = "The id of the detected object")
    @Id
    private ObjectId dbId;

    /**
     * The name of the object.
     */
    @Schema(description = "The name of the detected object")
    private String objectName;

    /**
     * The percent of the recognition probability.
     */
    @Schema(description = "The probability of the detected object")
    private float probability;

    /**
     * The detection time.
     */
    @Schema(description = "The time of the detected object")
    private long detectionTime;

    /**
     * The id of the detected image.
     */
    @Schema(description = "The id of the detected image")
    private String imageId;

    /**
     * The x coordinate of the upper-left corner of the detected object frame.
     */
    @Schema(description = "The x coordinate of the upper-left corner of detected object frame")
    private double minX;

    /**
     * The y coordinate of the upper-left corner of the detected object frame.
     */
    @Schema(description = "The y coordinate of the upper-left corner of detected object frame")
    private double minY;

    /**
     * The x coordinate of the lower-right corner of the detected object frame.
     */
    @Schema(description = "The x coordinate of the lower-right corner of detected object frame")
    private double maxX;

    /**
     * The y coordinate of the lower-right corner of the detected object frame.
     */
    @Schema(description = "The y coordinate of the lower-right corner of detected object frame")
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