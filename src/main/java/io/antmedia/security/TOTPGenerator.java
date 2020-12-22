package io.antmedia.security;


import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.primitives.Longs;


 public class TOTPGenerator {
	 
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

     public static String generateTOTP(byte[] secretBytes,
             int duration,
             int codeDigits,
             String crypto){
         String result = null;

         long T = System.currentTimeMillis()/(duration*1000);
       
         byte[] msg = Longs.toByteArray(T);
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

 }