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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.red5.server.net.IHandshake;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates and validates the RTMP handshake response for Flash Players.
 * Client versions equal to or greater than Flash 9,0,124,0 require a nonzero
 * value as the fifth byte of the handshake request.
 * 
 * @author Jacinto Shy II (jacinto.m.shy@ieee.org)
 * @author Steven Zimmer (stevenlzimmer@gmail.com)
 * @author Gavriloaie Eugen-Andrei
 * @author Ari-Pekka Viitanen
 * @author Paul Gregoire
 * @author Tiago Jacobs 
 */
public abstract class RTMPHandshake implements IHandshake {

	protected static Logger log = LoggerFactory.getLogger(RTMPHandshake.class);	

	//for old style handshake
	public static byte[] HANDSHAKE_PAD_BYTES;
	
	protected static final byte[] GENUINE_FMS_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x69,
		(byte) 0x61, (byte) 0x20, (byte) 0x53, (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
		(byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Media Server 001
		(byte) 0xf0, (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68, (byte) 0xbe, (byte) 0xe8,
		(byte) 0x2e, (byte) 0x00, (byte) 0xd0, (byte) 0xd1, (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57,
		(byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29, (byte) 0x80, (byte) 0x6f, (byte) 0xab,
		(byte) 0x93, (byte) 0xb8, (byte) 0xe6, (byte) 0x36, (byte) 0xcf, (byte) 0xeb, (byte) 0x31, (byte) 0xae};

	protected static final byte[] GENUINE_FP_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79,
		(byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
		(byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8,
		(byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57,
		(byte) 0x6E, (byte) 0xEC, (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB,
		(byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB, (byte) 0x31, (byte) 0xAE};	
	
	/** Modulus bytes from flazr */
	protected static final byte[] DH_MODULUS_BYTES = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2, (byte) 0x21,
			(byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80,
			(byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a,
			(byte) 0x67, (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b,
			(byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e,
			(byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3, (byte) 0xcd,
			(byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2,
			(byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d,
			(byte) 0x51, (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62,
			(byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6,
			(byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6, (byte) 0xf4,
			(byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a,
			(byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c,
			(byte) 0x4b, (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec,
			(byte) 0xe6, (byte) 0x53, (byte) 0x81, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff };

    protected static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);

    protected static final BigInteger DH_BASE = BigInteger.valueOf(2);    	

    protected static final int HANDSHAKE_SIZE_SERVER = (Constants.HANDSHAKE_SIZE * 2) + 1;
    
    protected static final int DIGEST_LENGTH = 32;

    protected static final int KEY_LENGTH = 128;
    
	protected static final Random random = new Random();
	
	protected KeyAgreement keyAgreement;
	
	protected Cipher cipherOut;
	
	protected Cipher cipherIn;
	
	protected byte handshakeType;
	
	protected byte[] handshakeBytes;
	
	// validation scheme
	protected int validationScheme = 0;

	// servers public key
	protected byte[] incomingPublicKey;

	// clients public key
	protected byte[] outgoingPublicKey;
	
	// swf verification bytes
	protected byte[] swfVerificationBytes;
	
	private Mac hmacSHA256;
	
	static {
		//get security provider
		Security.addProvider(new BouncyCastleProvider());		
	}
	
	public RTMPHandshake() {
		log.trace("Handshake ctor");
		try {
			hmacSHA256 = Mac.getInstance("HmacSHA256");
		} catch (SecurityException e) {
			log.error("Security exception when getting HMAC", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("HMAC SHA256 does not exist");
		}
		//create our handshake bytes
		createHandshakeBytes();
	}

	/**
	 * Calculates an HMAC SHA256 hash using a default key length.
	 * 
	 * @param input
	 * @param key
	 * @return hmac hashed bytes
	 */
	public byte[] calculateHMAC_SHA256(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}

	/**
	 * Calculates an HMAC SHA256 hash using a set key length.
	 * 
	 * @param input
	 * @param key
	 * @param length
	 * @return hmac hashed bytes
	 */
	public byte[] calculateHMAC_SHA256(byte[] input, byte[] key, int length) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, 0, length, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}
	
	/**
	 * Creates a Diffie-Hellman key pair.
	 * 
	 * @return dh keypair
	 */
	protected KeyPair generateKeyPair() {
		KeyPair keyPair = null;
		DHParameterSpec keySpec = new DHParameterSpec(DH_MODULUS, DH_BASE);
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
			keyGen.initialize(keySpec);
			keyPair = keyGen.generateKeyPair();
		    keyAgreement = KeyAgreement.getInstance("DH");
		    keyAgreement.init(keyPair.getPrivate());
		} catch (Exception e) {
			log.error("Error generating keypair", e);
		}
		return keyPair;
	}

	/**
	 * Returns the public key for a given key pair.
	 * 
	 * @param keyPair
	 * @return public key
	 */
	protected static byte[] getPublicKey(KeyPair keyPair) {
		 DHPublicKey incomingPublicKey = (DHPublicKey) keyPair.getPublic();
	     BigInteger	dhY = incomingPublicKey.getY();
	     log.debug("Public key: {}", dhY);
	     byte[] result = dhY.toByteArray();
	     log.debug("Public key as bytes - length [{}]: {}", result.length, Hex.encodeHexString(result));
	     byte[] temp = new byte[KEY_LENGTH];
	     if (result.length < KEY_LENGTH) {
	    	 System.arraycopy(result, 0, temp, KEY_LENGTH - result.length, result.length);
	    	 result = temp;
	    	 log.debug("Padded public key length to 128");
	     } else if(result.length > KEY_LENGTH){
	    	 System.arraycopy(result, result.length - KEY_LENGTH, temp, 0, KEY_LENGTH);
	    	 result = temp;
	    	 log.debug("Truncated public key length to 128");
	     }
	     return result;
	}
	
	/**
	 * Determines the validation scheme for given input.
	 * 
	 * @param otherPublicKeyBytes
	 * @param agreement
	 * @return shared secret bytes if client used a supported validation scheme
	 */
	protected static byte[] getSharedSecret(byte[] otherPublicKeyBytes, KeyAgreement agreement) {
		BigInteger otherPublicKeyInt = new BigInteger(1, otherPublicKeyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("DH");
			KeySpec otherPublicKeySpec = new DHPublicKeySpec(otherPublicKeyInt, RTMPHandshake.DH_MODULUS, RTMPHandshake.DH_BASE);
			PublicKey otherPublicKey = keyFactory.generatePublic(otherPublicKeySpec);
			agreement.doPhase(otherPublicKey, true);
		} catch (Exception e) {
			log.error("Exception getting the shared secret", e);
		}
		byte[] sharedSecret = agreement.generateSecret();
		log.debug("Shared secret [{}]: {}", sharedSecret.length, Hex.encodeHexString(sharedSecret));
		return sharedSecret;
	}	
	
	/**
	 * Create the initial bytes for a request / response.
	 */
	protected abstract void createHandshakeBytes();
	
	/**
	 * Determines the validation scheme for given input.
	 * 
	 * @param input
	 * @return true if its a supported validation scheme, false if unsupported
	 */
	public abstract boolean validate(IoBuffer input);
	
	/**
	 * Returns the DH offset from an array of bytes.
	 * 
	 * @param bytes
	 * @return DH offset
	 */
	protected int getDHOffset(byte[] bytes) {
		int dhOffset = -1;
		switch (validationScheme) {
			case 1:
				dhOffset = getDHOffset1(bytes);
				break;
			default:
				log.debug("Scheme 0 will be used for DH offset");
			case 0:
				dhOffset = getDHOffset0(bytes);
		}  
		return dhOffset;
	}
	
	/**
	 * Returns the DH byte offset.
	 * 
	 * @return dh offset
	 */
	protected int getDHOffset0(byte[] bytes) {
		int offset = (bytes[1532] & 0x0ff) + (bytes[1533] & 0x0ff) + (bytes[1534] & 0x0ff) + (bytes[1535] & 0x0ff);
	    offset = offset % 632;
	    offset = offset + 772;
	    if (offset + KEY_LENGTH >= 1536) {
	    	 log.error("Invalid DH offset");
	    }
	    return offset;
	}
	
	/**
	 * Returns the DH byte offset.
	 * 
	 * @return dh offset
	 */
	protected int getDHOffset1(byte[] bytes) {
		int offset = (bytes[768] & 0x0ff) + (bytes[769] & 0x0ff) + (bytes[770] & 0x0ff) + (bytes[771] & 0x0ff);
	    offset = offset % 632;
	    offset = offset + 8;
	    if (offset + KEY_LENGTH >= 1536) {
	    	 log.error("Invalid DH offset");
	    }
	    return offset;
	}	
	
	/**
	 * Returns the digest offset using current validation scheme.
	 * 
	 * @param pBuffer
	 * @return digest offset
	 */
	protected int getDigestOffset(byte[] pBuffer) {
		int serverDigestOffset = -1;
		switch (validationScheme) {
			case 1:
				serverDigestOffset = getDigestOffset1(pBuffer);
				break;
			default:
				log.debug("Scheme 0 will be used for DH offset");
			case 0:
				serverDigestOffset = getDigestOffset0(pBuffer);
		}  
		return serverDigestOffset;
	}
	
	/**
	 * Returns a digest byte offset.
	 * 
	 * @param pBuffer source for digest data
	 * @return digest offset
	 */
	protected int getDigestOffset0(byte[] pBuffer) {
		if (log.isTraceEnabled()) {
			log.trace("Scheme 0 offset bytes {},{},{},{}", new Object[]{(pBuffer[8] & 0x0ff), (pBuffer[9] & 0x0ff), (pBuffer[10] & 0x0ff), (pBuffer[11] & 0x0ff)});
		}
		int offset = (pBuffer[8] & 0x0ff) + (pBuffer[9] & 0x0ff) + (pBuffer[10] & 0x0ff) + (pBuffer[11] & 0x0ff);
	    offset = offset % 728;
	    offset = offset + 12;
	    if (offset + DIGEST_LENGTH >= 1536) {
	        log.error("Invalid digest offset");
	    }
	    return offset;
	}

	/**
	 * Returns a digest byte offset.
	 * 
	 * @param pBuffer source for digest data
	 * @return digest offset
	 */
	protected int getDigestOffset1(byte[] pBuffer) {
		if (log.isTraceEnabled()) {
			log.trace("Scheme 1 offset bytes {},{},{},{}", new Object[]{(pBuffer[772] & 0x0ff), (pBuffer[773] & 0x0ff), (pBuffer[774] & 0x0ff), (pBuffer[775] & 0x0ff)});
		}
		int offset = (pBuffer[772] & 0x0ff) + (pBuffer[773] & 0x0ff) + (pBuffer[774] & 0x0ff) + (pBuffer[775] & 0x0ff);
	    offset = offset % 728;
	    offset = offset + 776;
	    if (offset + DIGEST_LENGTH >= 1536) {
	        log.error("Invalid digest offset");
	    }
	    return offset;
	}

	/**
	 * Creates the servers handshake bytes
	 */
	public byte[] getHandshakeBytes() {
		return handshakeBytes;
	}

	/**
	 * Sets the handshake type. Currently only two types are supported, plain and encrypted.
	 * 
	 * @param handshakeType
	 */
	public void setHandshakeType(byte handshakeType) {
		log.trace("Setting handshake type: {}", handshakeType);
		this.handshakeType = handshakeType;
	}

	/**
	 * Returns the handshake type.
	 * 
	 * @return handshakeType
	 */
	public byte getHandshakeType() {
		return handshakeType;
	}
	
	/**
	 * Gets the DH offset in the handshake bytes array based on validation scheme
	 * Generates DH keypair
	 * Adds public key to handshake bytes
	 */
	public Cipher getCipherOut() {
		return cipherOut;
	}

	/**
	 * Returns the contained handshake bytes. These are just random bytes
	 * if the player is using an non-versioned player.
	 * 
	 * @return handshake bytes
	 */
	public Cipher getCipherIn() {
		return cipherIn;
	}
	
	/**
	 * Returns the SWF verification bytes.
	 * 
	 * @return swf verification bytes
	 */
	public byte[] getSwfVerificationBytes() {
		return swfVerificationBytes;
	}
	
}
