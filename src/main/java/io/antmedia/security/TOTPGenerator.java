package io.antmedia.security;


import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.Base32;
import com.google.common.primitives.Longs;


 public class TOTPGenerator {
	 
	 protected static Logger logger = LoggerFactory.getLogger(TOTPGenerator.class);
	 
	 private static final long[] DIGITS_POWER
     // 0 1  2   3    4     5      6       7        8         9          10
     = {1,10,100,1000,10000,100000,1000000,10000000,100000000,1000000000,10000000000L};


     /**
      * This method uses the JCE to provide the crypto algorithm.
      * HMAC computes a Hashed Message Authentication Code with the
      * crypto hash algorithm as a parameter.
      *
      * @param crypto: the crypto algorithm (HmacSHA1, HmacSHA256,
      *                             HmacSHA512)
      * @param keyBytes: the bytes to use for the HMAC key
      * @param text: the message or text to be authenticated
      */
     private static byte[] hmac_sha(String crypto, byte[] keyBytes, byte[] text){
         try {
             Mac hmac;
             hmac = Mac.getInstance(crypto);
             SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
             hmac.init(macKey);
             return hmac.doFinal(text);
         } catch (GeneralSecurityException gse) {
             throw new UndeclaredThrowableException(gse);
         }
     }
     
     /**
      * This generated B32 Secret code on the fly for the subscriberId that is not recorded in the database
      * 
      * @param subscriberId
      * @param streamId
      * @param secretFromSettings
      * @return
      */
     public static String getSecretCodeForNotRecordedSubscriberId(String subscriberId, String streamId, String type, String secretFromSettings) {
 		
 		
 		String secretCode = null;
 		if (secretFromSettings != null) 
 		{
 			secretCode = secretFromSettings + subscriberId + streamId + type;
 			//make the subscriber id has a length of multiple of 8
 			int remainder = secretCode.length() % 8;
 			if ( remainder != 0) {
 				int paddingLength = 8 - remainder;
 				//add X to make it decodeable
 				secretCode += StringUtils.repeat('X', paddingLength) ;
 			}
 			return Base32.encodeAsString(secretCode.getBytes());
 		}
 		else 
 		{
 			//fix: Logging should not be vulnerable to injection attacks javasecurity:S5145
 			streamId = streamId.replaceAll("[\n\r]", "_");
 			subscriberId = subscriberId.replaceAll("[\n\r]", "_");
 			logger.warn("TOTP secret is not valid. It should be not null and it's length must multiple of 8 for streamId:{} and subsriberId:{}", 
 					streamId, subscriberId);
 		}
 		return null;
 	}
     

     /**
      * This method generates a TOTP value for the given
      * set of parameters.
      *
      * @param key: the shared secret, HEX encoded
      * @param time: a value that reflects a time
      * @param returnDigits: number of digits to return
      * @param crypto: the crypto function to use (HmacSHA1 | HmacSHA256 | HmacSHA512)
      *
      * @return: a numeric String in base 10 that includes
      *              {@link truncationDigits} digits
      */

     public static String generateTOTPWithTimeConstant(byte[] secretBytes,
             int codeDigits,
             String crypto, long timeConstant)
     {
         String result = null;

                
         byte[] msg = Longs.toByteArray(timeConstant);
         byte[] k = secretBytes;

         byte[] hash = hmac_sha(crypto, k, msg);

         // put selected bytes into result int
         int offset = hash[hash.length - 1] & 0xf;

         int binary =
             ((hash[offset] & 0x7f) << 24) |
             ((hash[offset + 1] & 0xff) << 16) |
             ((hash[offset + 2] & 0xff) << 8) |
             (hash[offset + 3] & 0xff);

         long otp = binary % DIGITS_POWER[codeDigits];

         result = Long.toString(otp);
         while (result.length() < codeDigits) {
             result = "0" + result;
         }
         return result;
     }
          
     public static String generateTOTP(byte[] secretBytes,
             int duration,
             int codeDigits,
             String crypto) 
     {
    	 long durationMs = duration * 1000L;
    	 double division = ((double)System.currentTimeMillis())/durationMs;
    	 long timeConstant = (long)Math.floor(division); 
    	 return generateTOTPWithTimeConstant(secretBytes, codeDigits, crypto, timeConstant);
     }

 }