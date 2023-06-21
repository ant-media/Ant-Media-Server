package io.antmedia.datastore.db.types;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;


import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity("ConferenceRoom")
@Indexes({ @Index(fields = @Field("roomId")) })
@ApiModel(value="ConferenceRoom", description="The Conference Room class")
public class ConferenceRoom {
	
	
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "The db id of the Conference Room")
	private ObjectId dbId;
	
	@ApiModelProperty(value = "The id of the Conference Room")
	private String roomId; 
	
	@ApiModelProperty(value = "The start date of the Conference Room. It's unix timestamp in seconds.")
	private long startDate;
	
	@ApiModelProperty(value = "The end date of the Conference Room. It's unix timestamp in seconds")
	private long endDate;
	
	@ApiModelProperty(value = "The list of streams in the Conference Room")
	private List<String> roomStreamList = new ArrayList<>();
	
	@ApiModelProperty(value = "Conference Room Mode: legacy | mcu | multi-track")
	private String mode = "legacy";
	
	@JsonIgnore
	private boolean zombi;
	
	/**
	 * This is the origin address of the node hosting the room.
	 */
	@ApiModelProperty(value = "the origin address of the node hosting the room")
	private String originAdress;

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}
	
	public List<String> getRoomStreamList() {
		return roomStreamList;
	}

	public void setRoomStreamList(List<String> roomStreamList) {
		this.roomStreamList = roomStreamList;
	}

	public boolean isZombi() {
		return zombi;
	}

	public void setZombi(boolean zombi) {
		this.zombi = zombi;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getOriginAdress() {
		return originAdress;
	}

	public void setOriginAdress(String originAdress) {
		this.originAdress = originAdress;
	}

}

