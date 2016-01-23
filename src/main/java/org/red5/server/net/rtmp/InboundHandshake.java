/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.LoggerFactory;

/**
 * Performs handshaking for server connections.
 * 
 * @author Paul Gregoire
 */
public class InboundHandshake extends RTMPHandshake {

    /** Client initial request C1 */
    private byte[] c1 = null;

    public InboundHandshake() {
        super(RTMPConnection.RTMP_NON_ENCRYPTED);
        log = LoggerFactory.getLogger(InboundHandshake.class);
    }

    public InboundHandshake(byte handshakeType) {
        super(handshakeType);
        log = LoggerFactory.getLogger(InboundHandshake.class);
    }

    public InboundHandshake(byte handshakeType, int algorithm) {
        this(handshakeType);
        this.algorithm = algorithm;
    }

    /**
     * Generates response for versioned connections.
     * 
     * @param input incoming RTMP handshake bytes
     * @return outgoing handshake
     */
    public IoBuffer doHandshake(IoBuffer in) {
        if (log.isTraceEnabled()) {
            log.trace("doHandshake: {}", in);
        }
        return decodeClientRequest1(in);
    }

    /**
     * Decodes the first client request (C1) and returns a server response (S0S1).
     * <pre>
     * C1 = 1536 bytes from the client
     * S0 = 0x03 (server handshake type - 0x03, 0x06, 0x08, or 0x09)
     * S1 = 1536 bytes from server
     * </pre>
     * @param in incoming handshake C1
     * @return server response S0+S1
     */
    public IoBuffer decodeClientRequest1(IoBuffer in) {
        if (log.isTraceEnabled()) {
            log.debug("decodeClientRequest1: {}", Hex.encodeHexString(in.array()));
        }
        if (in.hasArray()) {
            c1 = in.array();
        } else {
            c1 = new byte[Constants.HANDSHAKE_SIZE];
            in.get(c1);
        }
        //if (log.isTraceEnabled()) {
        //    log.trace("C1: {}", Hex.encodeHexString(c1));
        //}
        if (log.isDebugEnabled()) {
            log.debug("Flash player version {}", Hex.encodeHexString(Arrays.copyOfRange(c1, 4, 8)));
        }
        // check for un-versioned handshake
        int clientVersionByte = (c1[4] & 0xff);
        if (clientVersionByte == 0) {
            return generateUnversionedHandshake(c1);
        }
        // make sure this is a client we can communicate with
        //if (validate(c1)) {
        //    log.debug("Valid RTMP client detected, algorithm: {}", algorithm);
        //} else {
        //    log.info("Invalid RTMP connection data detected, you may experience errors");
        //}
        // handle encryption setup
        if (useEncryption()) {
            // configure based on type and fp version
            if (handshakeType == 6 || handshakeType == 8) {
                // start off with algorithm 1 if we're type 6 or 8
                algorithm = 1;
                // set to xtea type 8 if client is fp10 capable
                if (clientVersionByte == 128) {
                    handshakeType = 8;
                }
            }
            // get the DH offset in the handshake bytes, generates DH keypair, and adds the public key to handshake bytes
            int clientDHOffset = getDHOffset(algorithm, c1, 0);
            log.trace("Incoming DH offset: {}", clientDHOffset);
            // get the clients public key
            outgoingPublicKey = new byte[KEY_LENGTH];
            System.arraycopy(c1, clientDHOffset, outgoingPublicKey, 0, KEY_LENGTH);
            log.debug("Client public key: {}", Hex.encodeHexString(outgoingPublicKey));
            // get the servers dh offset
            int serverDHOffset = getDHOffset(algorithm, handshakeBytes, 0);
            log.trace("Outgoing DH offset: {}", serverDHOffset);
            // create keypair
            KeyPair keys = generateKeyPair();
            // get public key
            incomingPublicKey = getPublicKey(keys);
            log.debug("Server public key: {}", Hex.encodeHexString(incomingPublicKey));
            // add to handshake bytes
            System.arraycopy(incomingPublicKey, 0, handshakeBytes, serverDHOffset, KEY_LENGTH);
            // create the RC4 ciphers
            initRC4Encryption(getSharedSecret(outgoingPublicKey, keyAgreement));
            switch (handshakeType) {
                case RTMPConnection.RTMP_ENCRYPTED:
                    
                    break;
                case RTMPConnection.RTMP_ENCRYPTED_XTEA:
                    
                    break;
                case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
                    
                    break;
            }
        }
        // create the server digest
        int digestPosServer = getDigestOffset(algorithm, handshakeBytes, 0);
        log.debug("Server digest position offset: {} algorithm: {}", digestPosServer, algorithm);
        // calculate the server hash and add to the handshake bytes (S1)
        calculateDigest(digestPosServer, handshakeBytes, 0, GENUINE_FMS_KEY, 36, handshakeBytes, digestPosServer);
        log.debug("Server digest: {}", Hex.encodeHexString(Arrays.copyOfRange(handshakeBytes, digestPosServer, digestPosServer + DIGEST_LENGTH)));
        // S1 is ready to be sent to the client, copy it before we proceed, since swfhash generation may overwrite the server digest
        byte[] s1 = new byte[Constants.HANDSHAKE_SIZE];
        System.arraycopy(handshakeBytes, 0, s1, 0, Constants.HANDSHAKE_SIZE);
        // get the client digest
        log.trace("Trying algorithm: {}", algorithm);
        int digestPosClient = getDigestOffset(algorithm, c1, 0);
        log.debug("Client digest position offset: {}", digestPosClient);
        if (!verifyDigest(digestPosClient, c1, GENUINE_FP_KEY, 30)) {
            // try a different position
            algorithm ^= 1;
            log.trace("Trying algorithm: {}", algorithm);
            digestPosClient = getDigestOffset(algorithm, c1, 0);
            log.debug("Client digest position offset: {}", digestPosClient);
            if (!verifyDigest(digestPosClient, c1, GENUINE_FP_KEY, 30)) {
                log.warn("Client digest verification failed");
                return null;
            }
        }
        // how in the heck do we generate a hash for a swf when we dont know which one is requested
        byte[] swfHash = new byte[DIGEST_LENGTH];
        // calculate the client hash
        byte[] clientDigestHash = new byte[DIGEST_LENGTH];
        calculateDigest(digestPosClient, c1, 0, GENUINE_FP_KEY, 30, clientDigestHash, 0);
        log.debug("Client digest: {}", Hex.encodeHexString(clientDigestHash));
        // compute key
        calculateHMAC_SHA256(clientDigestHash, 0, DIGEST_LENGTH, GENUINE_FMS_KEY, 68, swfHash, 0);
        log.debug("Key: {}", Hex.encodeHexString(swfHash));
        // calculate swf hash. what should swf size be?
        calculateSwfVerification(handshakeBytes, swfHash, DIGEST_LENGTH);
        log.debug("swfVerification: {}", Hex.encodeHexString(swfVerificationBytes));
        // create output buffer
        IoBuffer s0s1 = IoBuffer.allocate(Constants.HANDSHAKE_SIZE + 1); // 1537
        // set handshake with encryption type 
        s0s1.put(handshakeType); // 1
        s0s1.put(s1); // 1536
        s0s1.flip();
        if (log.isTraceEnabled()) {
            log.trace("S0+S1 size: {}", s0s1.limit());
        }
        return s0s1;
    }

    /**
     * Decodes the second client request (C2) and returns a server response (S2).
     * <pre>
     * C2 = Copy of S1 bytes
     * S2 = Copy of C1 bytes
     * </pre>
     * @param in incoming handshake C2
     * @return server response S2
     */
    public IoBuffer decodeClientRequest2(IoBuffer in) {
        // just send back c1
        IoBuffer s2 = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
        // set handshake with encryption type 
        s2.put(c1); // 1536
        s2.flip();
        if (log.isTraceEnabled()) {
            log.trace("S2 size: {}", s2.limit());
        }
        return s2;
    }

    /**
     * Generates response for non-versioned connections, such as those before FP9.
     * 
     * @param input incoming RTMP bytes
     * @return outgoing handshake
     */
    private IoBuffer generateUnversionedHandshake(byte[] input) {
        log.debug("Using old style (un-versioned) handshake");
        // save resource by only doing this after the first request
        if (HANDSHAKE_PAD_BYTES == null) {
            HANDSHAKE_PAD_BYTES = new byte[Constants.HANDSHAKE_SIZE - 4];
            // fill pad bytes
            Arrays.fill(HANDSHAKE_PAD_BYTES, (byte) 0x00);
        }
        IoBuffer output = IoBuffer.allocate(HANDSHAKE_SIZE_SERVER);
        // non-encrypted
        output.put(RTMPConnection.RTMP_NON_ENCRYPTED);
        // set server uptime in seconds
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
        // timestamp
        int time = (int) (Red5.getUpTime() / 1000);
        handshakeBytes[0] = (byte) (time >>> 24);
        handshakeBytes[1] = (byte) (time >>> 16);
        handshakeBytes[2] = (byte) (time >>> 8);
        handshakeBytes[3] = (byte) time;
        // version 4
        handshakeBytes[4] = 4;
        handshakeBytes[5] = 0;
        handshakeBytes[6] = 0;
        handshakeBytes[7] = 1;
        // fill the rest with random bytes
        byte[] rndBytes = new byte[Constants.HANDSHAKE_SIZE - 8];
        random.nextBytes(rndBytes);
        // copy random bytes into our handshake array
        System.arraycopy(rndBytes, 0, handshakeBytes, 8, (Constants.HANDSHAKE_SIZE - 8));
    }

    /**
     * Determines the validation scheme for given input.
     * 
     * @param input handshake bytes from the client
     * @return true if client used a supported validation scheme, false if unsupported
     */
    @Override
    public boolean validate(byte[] handshake) {
        if (validateScheme(handshake, 0)) {
            algorithm = 0;
            return true;
        }
        if (validateScheme(handshake, 1)) {
            algorithm = 1;
            return true;
        }
        log.error("Unable to validate client");
        return false;
    }

    private boolean validateScheme(byte[] handshake, int scheme) {
        int digestOffset = -1;
        switch (scheme) {
            case 0:
                digestOffset = getDigestOffset1(handshake, 0);
                break;
            case 1:
                digestOffset = getDigestOffset2(handshake, 0);
                break;
            default:
                log.error("Unknown algorithm: {}", scheme);
        }
        log.debug("Algorithm: {} digest offset: {}", scheme, digestOffset);
        byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
        System.arraycopy(handshake, 0, tempBuffer, 0, digestOffset);
        System.arraycopy(handshake, digestOffset + DIGEST_LENGTH, tempBuffer, digestOffset, Constants.HANDSHAKE_SIZE - digestOffset - DIGEST_LENGTH);
        byte[] tempHash = new byte[DIGEST_LENGTH];
        calculateHMAC_SHA256(tempBuffer, 0, tempBuffer.length, GENUINE_FP_KEY, 30, tempHash, 0);
        log.debug("Hash: {}", Hex.encodeHexString(tempHash));
        boolean result = true;
        for (int i = 0; i < DIGEST_LENGTH; i++) {
            if (handshake[digestOffset + i] != tempHash[i]) {
                result = false;
                break;
            }
        }
        return result;
    }

}
