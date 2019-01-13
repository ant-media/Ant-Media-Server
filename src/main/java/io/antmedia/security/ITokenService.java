package io.antmedia.security;

import java.util.Map;

import io.antmedia.datastore.db.types.Token;

public interface ITokenService {

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

	/**
	 * checks the token validity
	 * @param tokenId - requested token id
	 * @param streamId - id of the stream
	 * @param sessionId - id of the current session
	 * @param type - type of the token (play/publish)
	 * @return true or false
	 */

	boolean checkToken (String tokenId, String streamId, String sessionId, String type);

	/**
	 * creates token according to the provided parameters
	 * @param streamId - id of the requested stream for token creation
	 * @param exprireDate - expire date of the token
	 * @param type type of the token (play/publish)
	 * @return token
	 */

	Token createToken(String streamId, long exprireDate, String type);

	/**
	 * gets  map of authenticated sessions
	 * @return list
	 */

	Map<String, String>  getAuthenticatedMap();

}
