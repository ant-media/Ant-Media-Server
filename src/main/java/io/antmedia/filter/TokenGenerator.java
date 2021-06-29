package io.antmedia.filter;

import org.apache.commons.lang3.RandomStringUtils;

public class TokenGenerator {
	
	public static final String INTERNAL_COMMUNICATION_TOKEN_NAME = "ClusterToken";
	public static final String BEAN_NAME = "tokenGenerator";
	
	private String genetaredToken;

	public String getGenetaredToken() {
		if(genetaredToken == null) {
			genetaredToken = RandomStringUtils.random(16);
		}
		return genetaredToken;
	}
}
