package io.antmedia.datastore.db.types;

import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import io.swagger.annotations.ApiModelProperty;

@Entity
@Indexes({ @Index(fields = @Field("subscriberId")) })
public class SubscriberMetadata {
	

	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the SubscriberMetadata")
	private ObjectId dbId;
	
	/**
	 * Subscriber id. It can be username, email or any random number
	 */
	@ApiModelProperty(value = "the subscriber id")
	private String subscriberId;
	
	@ApiModelProperty(value = "Push notification tokens provided by FCM and APN")
	private List<PushNotificationToken> pushNotificationTokens;

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

	public List<PushNotificationToken> getPushNotificationTokens() {
		return pushNotificationTokens;
	}

	public void setPushNotificationTokens(List<PushNotificationToken> pushNotificationTokens) {
		this.pushNotificationTokens = pushNotificationTokens;
	}
	
	
}
