package io.antmedia.datastore.db.types;

import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Indexes({ @Index(fields = @Field("subscriberId")) })
public class SubscriberMetadata {
	

	@JsonIgnore
    @Schema(hidden = true)
	@Id
    private ObjectId dbId;

    /**
     * The subscriber id. It can be username, email or any random number
     */
    @Schema(description = "The subscriber id")
    private String subscriberId;

    @Schema(description = "Push notification tokens provided by FCM and APN")
    private Map<String, PushNotificationToken> pushNotificationTokens;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public Map<String, PushNotificationToken> getPushNotificationTokens() {
		return pushNotificationTokens;
	}

	public void setPushNotificationTokens(Map<String, PushNotificationToken> pushNotificationTokens) {
		this.pushNotificationTokens = pushNotificationTokens;
	}
	
	
}
