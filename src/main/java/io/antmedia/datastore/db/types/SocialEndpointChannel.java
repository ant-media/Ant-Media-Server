package io.antmedia.datastore.db.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * This class is for specifying different channel options in social media. 
 * It is useful for facebook so far. Type can be user, page, event, group
 * 
 * @author mekya
 *
 */
@ApiModel(value="SocialEndpointChannel", description="The SocialEndpointChannel parameter class")
public class SocialEndpointChannel {
	
	public SocialEndpointChannel(String accountId, String accountName, String type) {
		this.id = accountId;
		this.name = accountName;
		this.type = type;
	}

	public SocialEndpointChannel() {
	}

	@ApiModelProperty(value = "the type of the social end point channel")
	public String type = null;
	
	@ApiModelProperty(value = "the name of the social end point channel")
	public String name = null;
	
	@ApiModelProperty(value = "the id of the social end point channel")
	public String id = null;
	
}
