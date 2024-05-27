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
import io.swagger.v3.oas.annotations.media.Schema;

@Entity("ConferenceRoom")
@Indexes({ @Index(fields = @Field("roomId")) })
@Schema(description = "The Conference Room class")
public class ConferenceRoom {
	public static final String MULTI_TRACK_MODE = "multitrack";

	public static final String LEGACY_MODE = "legacy";

	@JsonIgnore
    @Id
    @Schema(description = "The db id of the Conference Room")
    private ObjectId dbId;

    @Schema(description = "The id of the Conference Room")
    private String roomId;

    @Schema(description = "The start date of the Conference Room. It's unix timestamp in seconds.")
    private long startDate;

    @Schema(description = "The end date of the Conference Room. It's unix timestamp in seconds")
    private long endDate;

    @Schema(description = "The list of streams in the Conference Room")
    private List<String> roomStreamList = new ArrayList<>();

    @Schema(description = "Conference Room Mode: legacy | mcu | multi-track")
    private String mode = "legacy";

    @JsonIgnore
    private boolean zombi;

    /**
     * This is the origin address of the node hosting the room.
     */
    @Schema(description = "the origin address of the node hosting the room")
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

