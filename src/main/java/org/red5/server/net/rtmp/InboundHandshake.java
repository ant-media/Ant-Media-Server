/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

import java.security.KeyPair;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.message.Constants;

/**
 * Performs handshaking for server connections.
 * 
 * @author Paul Gregoire
 */
public class InboundHandshake extends RTMPHandshake {

	public InboundHandshake() {
		super();
	}
	
	/**
	 * Generates response for versioned connections.
	 * 
	 * @param input incoming RTMP bytes
	 * @return outgoing handshake
	 */
	public IoBuffer doHandshake(IoBuffer input) {
		log.trace("doHandshake: {}", input);
		if (log.isDebugEnabled()) {
			log.debug("Player encryption byte: {}", handshakeType);
			byte[] bIn = input.array();
			log.debug("Detecting flash player version {},{},{},{}", new Object[]{(bIn[4] & 0x0ff), (bIn[5] & 0x0ff), (bIn[6] & 0x0ff), (bIn[7] & 0x0ff)});
    		//if the 5th byte is 0 then dont generate new-style handshake
    		if (log.isTraceEnabled()) {
    			log.trace("First few bytes (in): {},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}", new Object[] { 
    					bIn[0], bIn[1],	bIn[2], bIn[3], bIn[4], 
    					bIn[5], bIn[6], bIn[7], bIn[8], bIn[9], 
    					bIn[10], bIn[11], bIn[12], bIn[13], bIn[14],
    					bIn[15] });
    			//client version hex
    			byte[] ver = new byte[4];
    			System.arraycopy(bIn, 4, ver, 0, 4);    			
    			log.trace("Version string: {}", Hex.encodeHexString(ver));
    			//dump
    			byte[] buf = new byte[KEY_LENGTH];
    			System.arraycopy(bIn, 0, buf, 0, KEY_LENGTH);
    			log.trace("Hex: {}", Hex.encodeHexString(buf));
    		}		
		}
		input.mark();
		byte versionByte = input.get(4);
		log.debug("Player version byte: {}", (versionByte & 0x0ff));
		input.reset();
		if (versionByte == 0) {
			return generateUnversionedHandshake(input);
		}
		//create output buffer
		IoBuffer output = IoBuffer.allocate(HANDSHAKE_SIZE_SERVER);
		input.mark();
		//make sure this is a client we can communicate with
		if (validate(input)) {
			log.debug("Valid RTMP client detected");
		} else {
			log.info("Invalid RTMP connection data detected, you may experience errors");
		}
		input.reset();
		log.debug("Using new style handshake");
		input.mark();	
		//create all the dh stuff and add to handshake bytes
		prepareResponse(input);
		input.reset();
		if (handshakeType == RTMPConnection.RTMP_ENCRYPTED) {
    		log.debug("Incoming public key [{}]: {}", incomingPublicKey.length, Hex.encodeHexString(incomingPublicKey));
    		log.debug("Outgoing public key [{}]: {}", outgoingPublicKey.length, Hex.encodeHexString(outgoingPublicKey));
    		byte[] sharedSecret = getSharedSecret(outgoingPublicKey, keyAgreement);
    		// create output cipher
    		byte[] digestOut = calculateHMAC_SHA256(outgoingPublicKey, sharedSecret);
    		try {
    			cipherOut = Cipher.getInstance("RC4");
    			cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(digestOut, 0, 16, "RC4"));
    		} catch (Exception e) {
    			log.warn("Encryption cipher creation failed", e);
    		}
    		// create input cipher
    		byte[] digestIn = calculateHMAC_SHA256(incomingPublicKey, sharedSecret);
    		try {
    			cipherIn = Cipher.getInstance("RC4");
    			cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(digestIn, 0, 16, "RC4"));
    		} catch (Exception e) {
    			log.warn("Decryption cipher creation failed", e);
    		}
            // update 'encoder / decoder state' for the RC4 keys
            // both parties *pretend* as if handshake part 2 (1536 bytes) was encrypted
            // effectively this hides / discards the first few bytes of encrypted session
            // which is known to increase the secure-ness of RC4
            // RC4 state is just a function of number of bytes processed so far
            // that's why we just run 1536 arbitrary bytes through the keys below
            byte[] dummyBytes = new byte[Constants.HANDSHAKE_SIZE];
            cipherIn.update(dummyBytes);
            cipherOut.update(dummyBytes);
		}						
		input.mark();
		//create the server digest
		int serverDigestOffset = getDigestOffset(handshakeBytes);
		byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
	    System.arraycopy(handshakeBytes, 0, tempBuffer, 0, serverDigestOffset);
	    System.arraycopy(handshakeBytes, serverDigestOffset + DIGEST_LENGTH, tempBuffer, serverDigestOffset, Constants.HANDSHAKE_SIZE - serverDigestOffset - DIGEST_LENGTH);			
	    //calculate the hash
		byte[] tempHash = calculateHMAC_SHA256(tempBuffer, GENUINE_FMS_KEY, 36);
		//add the digest 
		System.arraycopy(tempHash, 0, handshakeBytes, serverDigestOffset, DIGEST_LENGTH);
		//compute the challenge digest
		byte[] inputBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
		//log.debug("Before get: {}", input.position());
		input.get(inputBuffer);
		//log.debug("After get: {}", input.position());
		int keyChallengeIndex = getDigestOffset(inputBuffer);
		byte[] challengeKey = new byte[DIGEST_LENGTH];
		input.position(keyChallengeIndex);
		input.get(challengeKey, 0, DIGEST_LENGTH);			
		input.reset();
		//compute key
		tempHash = calculateHMAC_SHA256(challengeKey, GENUINE_FMS_KEY, 68);
		//generate hash
		byte[] randBytes = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
		random.nextBytes(randBytes);
		byte[] lastHash = calculateHMAC_SHA256(randBytes, tempHash, DIGEST_LENGTH);
		//set handshake with encryption type 
		output.put(handshakeType);			
		output.put(handshakeBytes);
		output.put(randBytes);
		output.put(lastHash);
		output.flip();			
		if (log.isTraceEnabled()) {
			byte[] bOut = output.array();
			log.trace("First few bytes (out): {},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}", new Object[] { 
					bOut[0], bOut[1], bOut[2], bOut[3], bOut[4], 
					bOut[5], bOut[6], bOut[7], bOut[8], bOut[9], 
					bOut[10], bOut[11], bOut[12], bOut[13], bOut[14],
					bOut[15]});
			byte[] buf = new byte[KEY_LENGTH];
			System.arraycopy(bOut, 0, buf, 0, KEY_LENGTH);
			log.trace("Hex: {}", Hex.encodeHexString(buf));
		}		
		return output;
	}	
	
	/**
	 * Generates response for non-versioned connections, such as those before FP9.
	 * 
	 * @param input incoming RTMP bytes
	 * @return outgoing handshake
	 */
	private IoBuffer generateUnversionedHandshake(IoBuffer input) {
		log.debug("Using old style (un-versioned) handshake");
		//save resource by only doing this after the first request
		if (HANDSHAKE_PAD_BYTES == null) {
    		HANDSHAKE_PAD_BYTES = new byte[Constants.HANDSHAKE_SIZE - 4];
    		//fill pad bytes
    		Arrays.fill(HANDSHAKE_PAD_BYTES, (byte) 0x00);
		}
		IoBuffer output = IoBuffer.allocate(HANDSHAKE_SIZE_SERVER);
		//non-encrypted
		output.put(RTMPConnection.RTMP_NON_ENCRYPTED);
		//set server uptime in seconds
		output.putInt((int) Red5.getUpTime() / 1000); //0x01
		output.put(RTMPHandshake.HANDSHAKE_PAD_BYTES);
		output.put(input);
		output.flip();
		return output;
	}
	
	/**
	 * Creates the servers handshake bytes
	 */
	@Override
	protected void createHandshakeBytes() {
		handshakeBytes = new byte[Constants.HANDSHAKE_SIZE];
		//timestamp
		handshakeBytes[0] = 0;
		handshakeBytes[1] = 0;
		handshakeBytes[2] = 0;
		handshakeBytes[3] = 0;
		//version (0x01020304)
		handshakeBytes[4] = 1;
		handshakeBytes[5] = 2;
		handshakeBytes[6] = 3;
		handshakeBytes[7] = 4;
		//fill the rest with random bytes
		byte[] rndBytes = new byte[Constants.HANDSHAKE_SIZE - 8];
		random.nextBytes(rndBytes);		
		//copy random bytes into our handshake array
		System.arraycopy(rndBytes, 0, handshakeBytes, 8, (Constants.HANDSHAKE_SIZE - 8));	
	}	
	
	/**
	 * Gets the DH offset in the handshake bytes array based on validation scheme
	 * Generates DH keypair
	 * Adds public key to handshake bytes
	 * @param input 
	 */
	private void prepareResponse(IoBuffer input) {
		//put the clients input into a byte array
		byte[] inputBuffer = new byte[input.limit()];
		input.get(inputBuffer);
		//get the clients dh offset
		int clientDHOffset = getDHOffset(inputBuffer);
		log.trace("Incoming DH offset: {}", clientDHOffset);
		//get the clients public key
		outgoingPublicKey = new byte[KEY_LENGTH];
		System.arraycopy(inputBuffer, clientDHOffset, outgoingPublicKey, 0, KEY_LENGTH);		
		//get the servers dh offset
		int serverDHOffset = getDHOffset(handshakeBytes);
		log.trace("Outgoing DH offset: {}", serverDHOffset);
		//create keypair
		KeyPair keys = generateKeyPair();
		//get public key
		incomingPublicKey = getPublicKey(keys);
		//add to handshake bytes
		System.arraycopy(incomingPublicKey, 0, handshakeBytes, serverDHOffset, KEY_LENGTH);
	}	
	
	/**
	 * Determines the validation scheme for given input.
	 * 
	 * @param input
	 * @return true if client used a supported validation scheme, false if unsupported
	 */
	@Override
	public boolean validate(IoBuffer input) {
		byte[] pBuffer = new byte[input.remaining()];
		//put all the input bytes into our buffer
		input.get(pBuffer, 0, input.remaining());		
	    if (validateScheme(pBuffer, 0)) {
	        validationScheme = 0;
			log.debug("Selected scheme: 0");
	        return true;
	    }
	    if (validateScheme(pBuffer, 1)) {
	        validationScheme = 1;
			log.debug("Selected scheme: 1");
	        return true;
	    }
	    log.error("Unable to validate client");
	    return false;
	}
	
	private boolean validateScheme(byte[] pBuffer, int scheme) {
		int digestOffset = -1;
		switch (scheme) {
			case 0:
				digestOffset = getDigestOffset0(pBuffer);
				break;
			case 1:
				digestOffset = getDigestOffset1(pBuffer);
				break;
			default:
				log.error("Unknown scheme: {}", scheme);
		}   
		log.debug("Scheme: {} client digest offset: {}", scheme, digestOffset);

	    byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
	    System.arraycopy(pBuffer, 0, tempBuffer, 0, digestOffset);
	    System.arraycopy(pBuffer, digestOffset + DIGEST_LENGTH, tempBuffer, digestOffset, Constants.HANDSHAKE_SIZE - digestOffset - DIGEST_LENGTH);	    

	    byte[] tempHash = calculateHMAC_SHA256(tempBuffer, GENUINE_FP_KEY, 30);
	    log.debug("Temp: {}", Hex.encodeHexString(tempHash));

	    boolean result = true;
	    for (int i = 0; i < DIGEST_LENGTH; i++) {
	    	//log.trace("Digest: {} Temp: {}", (pBuffer[digestOffset + i] & 0x0ff), (tempHash[i] & 0x0ff));
	        if (pBuffer[digestOffset + i] != tempHash[i]) {
	            result = false;
	            break;
	        }
	    }

	    return result;	
	}
	
}
