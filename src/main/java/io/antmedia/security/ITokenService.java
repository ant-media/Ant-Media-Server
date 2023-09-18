package io.antmedia.security;

import java.util.Date;
import java.util.Map;

import javax.annotation.Nonnull;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.Token;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ITokenService {
	
	public static final String HMAC_SHA1 = "HmacSHA1";

	public enum BeanName {
		TOKEN_SERVICE("token.service");
		
		
		private String originName;
		
		BeanName(String name) {
		    this.originName =  name;
		 }
		
		@Override
		public String toString() {
			return this.originName;
		}

	}

	static Logger logger = LoggerFactory.getLogger(ITokenService.class);

	/**
	 * Compare hash string with computed one which is based on streamId,type and secret
	 * @param hash - client hash
	 * @param streamId - id of the stream
	 * @param sessionId - session of the request
	 * @param type - type of the request (publish/play)
	 * @return
	 */
	boolean checkHash(String hash, String streamId, String sessionId, String type);

	/**
	 * Checks the token validity
	 * @param tokenId - requested token id
	 * @param streamId - id of the stream
	 * @param sessionId - id of the current session
	 * @param type - type of the token (play/publish)
	 * @return true or false
	 */

	boolean checkToken (String tokenId, String streamId, String sessionId, String type);

	/**
	 * Checks the time based token validity
	 * @param subscriberId - requested subscriberId
	 * @param streamId - id of the stream
	 * @param sessionId - id of the current session
	 * @param subscriberCode - with TOTP generated code 
	 * @param type - {@link Subscriber#PUBLISH_AND_PLAY_TYPE or @link Subscriber#PUBLISH_TYPE or @link Subscriber#PLAY_TYPE}
	 * @return true or false
	 */
	boolean checkTimeBasedSubscriber(String subscriberId, String streamId, String sessionId, String subscriberCode,  String type);
	
	/**
	 * Checks the JWT token validity
	 * @param jwtTokenId - requested token id
	 * @param streamId - id of the stream
	 * @param type - type of the token (play/publish)
	 * @return true or false
	 */
	boolean checkJwtToken (String jwtTokenId, String streamId, String sessionId, String type);
	
	/**
	 * Check the JWT token if it's valid. It accepts the secret key to check the validity;
	 * @param jwtTokenId
	 * @param tokenSecret
	 * @param streamId
	 * @param type
	 * @return
	 */
	boolean isJwtTokenValid(@Nonnull String jwtTokenId, @Nonnull String tokenSecret, @Nonnull String streamId, @Nonnull String type);
	
	/**
	 * creates token according to the provided parameters
	 * @param streamId - id of the requested stream for token creation
	 * @param exprireDate - expire date of the token
	 * @param type type of the token (play/publish)
	 * @param roomId- id of the room for playing streams in the room
	 * @return token
	 */

	Token createToken(String streamId, long exprireDate, String type, String roomId);
	
	/**
	 * creates token according to the provided parameters
	 * @param streamId - id of the requested stream for token creation
	 * @param exprireDate - expire date of the token (unix timestamp)
	 * @param type type of the token (play/publish)
	 * @param roomId- id of the room for playing streams in the room
	 * @return token
	 */

	Token createJwtToken(String streamId, long exprireDate, String type, String roomId);

	/**
	 * gets  map of authenticated sessions
	 * @return list
	 */

	Map<String, String>  getAuthenticatedMap();
	
	/**
	 * gets  map of authenticated subscriber sessions
	 * @return list
	 */

	Map<String, String>  getSubscriberAuthenticatedMap();

}
