/*
 * RED5 Open Source Media Server - https://github.com/Red5/
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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;
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
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.red5.server.net.IHandshake;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates and validates the RTMP handshake response for Flash Players. Client versions equal to or greater than Flash 9,0,124,0 require
 * a nonzero value as the fifth byte of the handshake request.
 * 
 * @author Jacinto Shy II (jacinto.m.shy@ieee.org)
 * @author Steven Zimmer (stevenlzimmer@gmail.com)
 * @author Gavriloaie Eugen-Andrei
 * @author Ari-Pekka Viitanen
 * @author Paul Gregoire
 * @author Tiago Jacobs
 */
public abstract class RTMPHandshake implements IHandshake {

    protected Logger log = LoggerFactory.getLogger(RTMPHandshake.class);

    public final static String[] HANDSHAKE_TYPES = {"Undefined0", "Undefined1", "Undefined2", "RTMP", "Undefined4", "Undefined5", "RTMPE", "Undefined7", "RTMPE XTEA", "RTMPE BLOWFISH"};

    public static final byte[] GENUINE_FMS_KEY = { 
        (byte) 0x47, (byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62,
        (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c,
        (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x69, (byte) 0x61, (byte) 0x20, (byte) 0x53, (byte) 0x65,
        (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
        (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Media Server 001
        (byte) 0xf0, (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68, (byte) 0xbe, (byte) 0xe8, (byte) 0x2e, (byte) 0x00, (byte) 0xd0, (byte) 0xd1,
        (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57, (byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29, (byte) 0x80, (byte) 0x6f, (byte) 0xab,
        (byte) 0x93, (byte) 0xb8, (byte) 0xe6, (byte) 0x36,
        (byte) 0xcf, (byte) 0xeb, (byte) 0x31, (byte) 0xae }; // 68

    public static final byte[] GENUINE_FP_KEY = {
        (byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62,
        (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
        (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79, (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30,
        (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
        (byte) 0xF0, (byte) 0xEE,
        (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8, (byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E,
        (byte) 0x7E, (byte) 0x57, (byte) 0x6E, (byte) 0xEC,
        (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB, (byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB,
        (byte) 0x31, (byte) 0xAE }; // 62

    /** "Second Oakley Default Group" from RFC2409, section 6.2. */
    protected static final byte[] DH_MODULUS_BYTES = {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f,
        (byte) 0xda, (byte) 0xa2, (byte) 0x21, (byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b,
        (byte) 0x80, (byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67,
        (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22,
        (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e, (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95,
        (byte) 0x19, (byte) 0xb3, (byte) 0xcd, (byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d,
        (byte) 0xf2, (byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51,
        (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6,
        (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6, (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff,
        (byte) 0x5c, (byte) 0xb6, (byte) 0xf4, (byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb,
        (byte) 0x5a, (byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b,
        (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec, (byte) 0xe6, (byte) 0x53, (byte) 0x81,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

    /** XTEA keys for RTMPE (RTMP type 0x08) - 16 x 4 */
    protected static final int[][] XTEA_KEYS = {
        {0xbff034b2, 0x11d9081f, 0xccdfb795, 0x748de732},
        {0x086a5eb6, 0x1743090e, 0x6ef05ab8, 0xfe5a39e2},
        {0x7b10956f, 0x76ce0521, 0x2388a73a, 0x440149a1},
        {0xa943f317, 0xebf11bb2, 0xa691a5ee, 0x17f36339},
        {0x7a30e00a, 0xb529e22c, 0xa087aea5, 0xc0cb79ac},
        {0xbdce0c23, 0x2febdeff, 0x1cfaae16, 0x1123239d},
        {0x55dd3f7b, 0x77e7e62e, 0x9bb8c499, 0xc9481ee4},
        {0x407bb6b4, 0x71e89136, 0xa7aebf55, 0xca33b839},
        {0xfcf6bdc3, 0xb63c3697, 0x7ce4f825, 0x04d959b2},
        {0x28e091fd, 0x41954c4c, 0x7fb7db00, 0xe3a066f8},
        {0x57845b76, 0x4f251b03, 0x46d45bcd, 0xa2c30d29},
        {0x0acceef8, 0xda55b546, 0x03473452, 0x5863713b},
        {0xb82075dc, 0xa75f1fee, 0xd84268e8, 0xa72a44cc},
        {0x07cf6e9e, 0xa16d7b25, 0x9fa7ae6c, 0xd92f5629},
        {0xfeb1eae4, 0x8c8c3ce1, 0x4e0064a7, 0x6a387c2a},
        {0x893a9427, 0xcc3013a2, 0xf106385b, 0xa829f927}};

    /** Blowfish keys for RTMPE (RTMP type 0x09) - 16 x 24 */
    protected static final byte[][] BLOWFISH_KEYS = { 
        { (byte) 0x79, (byte) 0x34, (byte) 0x77, (byte) 0x4c, (byte) 0x67, (byte) 0xd1, (byte) 0x38, (byte) 0x3a, (byte) 0xdf, (byte) 0xb3, (byte) 0x56, (byte) 0xbe, (byte) 0x8b, (byte) 0x7b, (byte) 0xd0, (byte) 0x24, (byte) 0x38, (byte) 0xe0, (byte) 0x73, (byte) 0x58, (byte) 0x41, (byte) 0x5d, (byte) 0x69, (byte) 0x67 }, 
        { (byte) 0x46, (byte) 0xf6, (byte) 0xb4, (byte) 0xcc, (byte) 0x01, (byte) 0x93, (byte) 0xe3, (byte) 0xa1, (byte) 0x9e, (byte) 0x7d, (byte) 0x3c, (byte) 0x65, (byte) 0x55, (byte) 0x86, (byte) 0xfd, (byte) 0x09, (byte) 0x8f, (byte) 0xf7, (byte) 0xb3, (byte) 0xc4, (byte) 0x6f, (byte) 0x41, (byte) 0xca, (byte) 0x5c }, 
        { (byte) 0x1a, (byte) 0xe7, (byte) 0xe2, (byte) 0xf3, (byte) 0xf9, (byte) 0x14, (byte) 0x79, (byte) 0x94, (byte) 0xc0, (byte) 0xd3, (byte) 0x97, (byte) 0x43, (byte) 0x08, (byte) 0x7b, (byte) 0xb3, (byte) 0x84, (byte) 0x43, (byte) 0x2f, (byte) 0x9d, (byte) 0x84, (byte) 0x3f, (byte) 0x21, (byte) 0x01, (byte) 0x9b }, 
        { (byte) 0xd3, (byte) 0xe3, (byte) 0x54, (byte) 0xb0, (byte) 0xf7, (byte) 0x1d, (byte) 0xf6, (byte) 0x2b, (byte) 0x5a, (byte) 0x43, (byte) 0x4d, (byte) 0x04, (byte) 0x83, (byte) 0x64, (byte) 0x3e, (byte) 0x0d, (byte) 0x59, (byte) 0x2f, (byte) 0x61, (byte) 0xcb, (byte) 0xb1, (byte) 0x6a, (byte) 0x59, (byte) 0x0d }, 
        { (byte) 0xc8, (byte) 0xc1, (byte) 0xe9, (byte) 0xb8, (byte) 0x16, (byte) 0x56, (byte) 0x99, (byte) 0x21, (byte) 0x7b, (byte) 0x5b, (byte) 0x36, (byte) 0xb7, (byte) 0xb5, (byte) 0x9b, (byte) 0xdf, (byte) 0x06, (byte) 0x49, (byte) 0x2c, (byte) 0x97, (byte) 0xf5, (byte) 0x95, (byte) 0x48, (byte) 0x85, (byte) 0x7e }, 
        { (byte) 0xeb, (byte) 0xe5, (byte) 0xe6, (byte) 0x2e, (byte) 0xa4, (byte) 0xba, (byte) 0xd4, (byte) 0x2c, (byte) 0xf2, (byte) 0x16, (byte) 0xe0, (byte) 0x8f, (byte) 0x66, (byte) 0x23, (byte) 0xa9, (byte) 0x43, (byte) 0x41, (byte) 0xce, (byte) 0x38, (byte) 0x14, (byte) 0x84, (byte) 0x95, (byte) 0x00, (byte) 0x53 }, 
        { (byte) 0x66, (byte) 0xdb, (byte) 0x90, (byte) 0xf0, (byte) 0x3b, (byte) 0x4f, (byte) 0xf5, (byte) 0x6f, (byte) 0xe4, (byte) 0x9c, (byte) 0x20, (byte) 0x89, (byte) 0x35, (byte) 0x5e, (byte) 0xd2, (byte) 0xb2, (byte) 0xc3, (byte) 0x9e, (byte) 0x9f, (byte) 0x7f, (byte) 0x63, (byte) 0xb2, (byte) 0x28, (byte) 0x81 }, 
        { (byte) 0xbb, (byte) 0x20, (byte) 0xac, (byte) 0xed, (byte) 0x2a, (byte) 0x04, (byte) 0x6a, (byte) 0x19, (byte) 0x94, (byte) 0x98, (byte) 0x9b, (byte) 0xc8, (byte) 0xff, (byte) 0xcd, (byte) 0x93, (byte) 0xef, (byte) 0xc6, (byte) 0x0d, (byte) 0x56, (byte) 0xa7, (byte) 0xeb, (byte) 0x13, (byte) 0xd9, (byte) 0x30 }, 
        { (byte) 0xbc, (byte) 0xf2, (byte) 0x43, (byte) 0x82, (byte) 0x09, (byte) 0x40, (byte) 0x8a, (byte) 0x87, (byte) 0x25, (byte) 0x43, (byte) 0x6d, (byte) 0xe6, (byte) 0xbb, (byte) 0xa4, (byte) 0xb9, (byte) 0x44, (byte) 0x58, (byte) 0x3f, (byte) 0x21, (byte) 0x7c, (byte) 0x99, (byte) 0xbb, (byte) 0x3f, (byte) 0x24 }, 
        { (byte) 0xec, (byte) 0x1a, (byte) 0xaa, (byte) 0xcd, (byte) 0xce, (byte) 0xbd, (byte) 0x53, (byte) 0x11, (byte) 0xd2, (byte) 0xfb, (byte) 0x83, (byte) 0xb6, (byte) 0xc3, (byte) 0xba, (byte) 0xab, (byte) 0x4f, (byte) 0x62, (byte) 0x79, (byte) 0xe8, (byte) 0x65, (byte) 0xa9, (byte) 0x92, (byte) 0x28, (byte) 0x76 }, 
        { (byte) 0xc6, (byte) 0x0c, (byte) 0x30, (byte) 0x03, (byte) 0x91, (byte) 0x18, (byte) 0x2d, (byte) 0x7b, (byte) 0x79, (byte) 0xda, (byte) 0xe1, (byte) 0xd5, (byte) 0x64, (byte) 0x77, (byte) 0x9a, (byte) 0x12, (byte) 0xc5, (byte) 0xb1, (byte) 0xd7, (byte) 0x91, (byte) 0x4f, (byte) 0x96, (byte) 0x4c, (byte) 0xa3 }, 
        { (byte) 0xd7, (byte) 0x7c, (byte) 0x2a, (byte) 0xbf, (byte) 0xa6, (byte) 0xe7, (byte) 0x85, (byte) 0x7c, (byte) 0x45, (byte) 0xad, (byte) 0xff, (byte) 0x12, (byte) 0x94, (byte) 0xd8, (byte) 0xde, (byte) 0xa4, (byte) 0x5c, (byte) 0x3d, (byte) 0x79, (byte) 0xa4, (byte) 0x44, (byte) 0x02, (byte) 0x5d, (byte) 0x22 }, 
        { (byte) 0x16, (byte) 0x19, (byte) 0x0d, (byte) 0x81, (byte) 0x6a, (byte) 0x4c, (byte) 0xc7, (byte) 0xf8, (byte) 0xb8, (byte) 0xf9, (byte) 0x4e, (byte) 0xcd, (byte) 0x2c, (byte) 0x9e, (byte) 0x90, (byte) 0x84, (byte) 0xb2, (byte) 0x08, (byte) 0x25, (byte) 0x60, (byte) 0xe1, (byte) 0x1e, (byte) 0xae, (byte) 0x18 }, 
        { (byte) 0xe9, (byte) 0x7c, (byte) 0x58, (byte) 0x26, (byte) 0x1b, (byte) 0x51, (byte) 0x9e, (byte) 0x49, (byte) 0x82, (byte) 0x60, (byte) 0x61, (byte) 0xfc, (byte) 0xa0, (byte) 0xa0, (byte) 0x1b, (byte) 0xcd, (byte) 0xf5, (byte) 0x05, (byte) 0xd6, (byte) 0xa6, (byte) 0x6d, (byte) 0x07, (byte) 0x88, (byte) 0xa3 }, 
        { (byte) 0x2b, (byte) 0x97, (byte) 0x11, (byte) 0x8b, (byte) 0xd9, (byte) 0x4e, (byte) 0xd9, (byte) 0xdf, (byte) 0x20, (byte) 0xe3, (byte) 0x9c, (byte) 0x10, (byte) 0xe6, (byte) 0xa1, (byte) 0x35, (byte) 0x21, (byte) 0x11, (byte) 0xf9, (byte) 0x13, (byte) 0x0d, (byte) 0x0b, (byte) 0x24, (byte) 0x65, (byte) 0xb2 }, 
        { (byte) 0x53, (byte) 0x6a, (byte) 0x4c, (byte) 0x54, (byte) 0xac, (byte) 0x8b, (byte) 0x9b, (byte) 0xb8, (byte) 0x97, (byte) 0x29, (byte) 0xfc, (byte) 0x60, (byte) 0x2c, (byte) 0x5b, (byte) 0x3a, (byte) 0x85, (byte) 0x68, (byte) 0xb5, (byte) 0xaa, (byte) 0x6a, (byte) 0x44, (byte) 0xcd, (byte) 0x3f, (byte) 0xa7 }};
    
    protected static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);

    protected static final BigInteger DH_BASE = BigInteger.valueOf(2);

    protected static final int DIGEST_LENGTH = 32;

    protected static final int KEY_LENGTH = 128;

    protected static final Random random = new Random();

    protected KeyAgreement keyAgreement;

    protected Cipher cipherOut;

    protected Cipher cipherIn;

    protected byte handshakeType;

    protected byte[] handshakeBytes;

    // servers public key
    protected byte[] incomingPublicKey;

    // clients public key
    protected byte[] outgoingPublicKey;

    // uncompressed swf size
    protected int swfSize;

    // swf verification bytes
    protected byte[] swfVerificationBytes;

    // handshake algorithm / validation scheme
    protected int algorithm = 1;

    // start as an fp of at least version 9.0.115.0
    protected boolean fp9Handshake = true;

    // buffer for incoming data
    protected IoBuffer buffer;

    static {
        // add bouncycastle security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public RTMPHandshake() {
        this((byte) 0);
    }

    public RTMPHandshake(byte handshakeType) {
        // set the handshake type
        setHandshakeType(handshakeType);
        // whether or not to use later handshake version
        fp9Handshake = "true".equals(System.getProperty("use.fp9.handshake", "true"));
        log.trace("Use fp9 handshake? {}", fp9Handshake);
        // create our handshake bytes
        createHandshakeBytes();
        // instance a buffer to handle fragmenting
        buffer = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
        buffer.setAutoExpand(true);
    }

    /**
     * Prepare the ciphers.
     * 
     * @param sharedSecret shared secret byte sequence
     */
    protected void initRC4Encryption(byte[] sharedSecret) {
        log.debug("Shared secret: {}", Hex.encodeHexString(sharedSecret));
        // create output cipher
        log.debug("Outgoing public key [{}]: {}", outgoingPublicKey.length, Hex.encodeHexString(outgoingPublicKey));
        byte[] rc4keyOut = new byte[32];
        // digest is 32 bytes, but our key is 16
        calculateHMAC_SHA256(outgoingPublicKey, 0, outgoingPublicKey.length, sharedSecret, KEY_LENGTH, rc4keyOut, 0);
        log.debug("RC4 Out Key: {}", Hex.encodeHexString(Arrays.copyOfRange(rc4keyOut, 0, 16)));
        try {
            cipherOut = Cipher.getInstance("RC4");
            cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rc4keyOut, 0, 16, "RC4"));
        } catch (Exception e) {
            log.warn("Encryption cipher creation failed", e);
        }
        // create input cipher
        log.debug("Incoming public key [{}]: {}", incomingPublicKey.length, Hex.encodeHexString(incomingPublicKey));
        // digest is 32 bytes, but our key is 16
        byte[] rc4keyIn = new byte[32];
        calculateHMAC_SHA256(incomingPublicKey, 0, incomingPublicKey.length, sharedSecret, KEY_LENGTH, rc4keyIn, 0);
        log.debug("RC4 In Key: {}", Hex.encodeHexString(Arrays.copyOfRange(rc4keyIn, 0, 16)));
        try {
            cipherIn = Cipher.getInstance("RC4");
            cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rc4keyIn, 0, 16, "RC4"));
        } catch (Exception e) {
            log.warn("Decryption cipher creation failed", e);
        }
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
            // key agreement is initialized with "this" ends private key
            keyAgreement.init(keyPair.getPrivate());
        } catch (Exception e) {
            log.error("Error generating keypair", e);
        }
        return keyPair;
    }

    /**
     * Returns the public key for a given key pair.
     * 
     * @param keyPair key pair
     * @return public key
     */
    protected byte[] getPublicKey(KeyPair keyPair) {
        DHPublicKey incomingPublicKey = (DHPublicKey) keyPair.getPublic();
        BigInteger dhY = incomingPublicKey.getY();
        if (log.isDebugEnabled()) {
            log.debug("Public key: {}", Hex.encodeHexString(BigIntegers.asUnsignedByteArray(dhY)));
        }
        return Arrays.copyOfRange(BigIntegers.asUnsignedByteArray(dhY), 0, KEY_LENGTH);
    }

    /**
     * Determines the validation scheme for given input.
     * 
     * @param publicKeyBytes public key bytes
     * @param agreement key agreement
     * @return shared secret bytes if client used a supported validation scheme
     */
    protected byte[] getSharedSecret(byte[] publicKeyBytes, KeyAgreement agreement) {
        BigInteger otherPublicKeyInt = new BigInteger(1, publicKeyBytes);
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
     * @param handshake handshake byte sequence
     * @return true if its a supported validation scheme, false if unsupported
     */
    public abstract boolean validate(byte[] handshake);

    /**
     * Calculates the digest given the its offset in the handshake data.
     * 
     * @param digestPos digest position
     * @param handshakeMessage handshake message
     * @param handshakeOffset handshake message offset
     * @param key contains the key
     * @param keyLen the length of the key
     * @param digest contains the calculated digest
     * @param digestOffset digest offset
     */
    public void calculateDigest(int digestPos, byte[] handshakeMessage, int handshakeOffset, byte[] key, int keyLen, byte[] digest, int digestOffset) {
        if (log.isTraceEnabled()) {
            log.trace("calculateDigest - digestPos: {} handshakeOffset: {} keyLen: {} digestOffset: {}", digestPos, handshakeOffset, keyLen, digestOffset);
        }
        int messageLen = Constants.HANDSHAKE_SIZE - DIGEST_LENGTH; // 1504
        byte[] message = new byte[messageLen];
        // copy bytes from handshake message starting at handshake offset into message start at index 0 and up-to digest position length
        System.arraycopy(handshakeMessage, handshakeOffset, message, 0, digestPos);
        // copy bytes from handshake message starting at handshake offset plus digest position plus digest length
        // into message start at digest position and up-to message length minus digest position
        System.arraycopy(handshakeMessage, handshakeOffset + digestPos + DIGEST_LENGTH, message, digestPos, messageLen - digestPos);
        calculateHMAC_SHA256(message, 0, messageLen, key, keyLen, digest, digestOffset);
    }

    /**
     * Verifies the digest.
     * 
     * @param digestPos digest position
     * @param handshakeMessage handshake message
     * @param key contains the key
     * @param keyLen the length of the key
     * @return true if valid and false otherwise
     */
    public boolean verifyDigest(int digestPos, byte[] handshakeMessage, byte[] key, int keyLen) {
        if (log.isTraceEnabled()) {
            log.trace("verifyDigest - digestPos: {} keyLen: {} handshake size: {} ", digestPos, keyLen, handshakeMessage.length);
        }
        byte[] calcDigest = new byte[DIGEST_LENGTH];
        calculateDigest(digestPos, handshakeMessage, 0, key, keyLen, calcDigest, 0);
        if (!Arrays.equals(Arrays.copyOfRange(handshakeMessage, digestPos, (digestPos + DIGEST_LENGTH)), calcDigest)) {
            return false;
        }
        return true;
    }

    /**
     * Calculates an HMAC SHA256 hash into the digest at the given offset.
     * 
     * @param message incoming bytes
     * @param messageOffset message offset
     * @param messageLen message length
     * @param key incoming key bytes
     * @param keyLen the length of the key
     * @param digest contains the calculated digest
     * @param digestOffset digest offset
     */
    public void calculateHMAC_SHA256(byte[] message, int messageOffset, int messageLen, byte[] key, int keyLen, byte[] digest, int digestOffset) {
        if (log.isTraceEnabled()) {
            log.trace("calculateHMAC_SHA256 - messageOffset: {} messageLen: {}", messageOffset, messageLen);
            log.trace("calculateHMAC_SHA256 - message: {}", Hex.encodeHexString(Arrays.copyOfRange(message, messageOffset, messageOffset + messageLen)));
            log.trace("calculateHMAC_SHA256 - keyLen: {} key: {}", keyLen, Hex.encodeHexString(Arrays.copyOf(key, keyLen)));
            //log.trace("calculateHMAC_SHA256 - digestOffset: {} digest: {}", digestOffset, Hex.encodeHexString(Arrays.copyOfRange(digest, digestOffset, digestOffset + DIGEST_LENGTH)));
        }
        byte[] calcDigest;
        try {
            Mac hmac = Mac.getInstance("Hmac-SHA256", BouncyCastleProvider.PROVIDER_NAME);
            hmac.init(new SecretKeySpec(Arrays.copyOf(key, keyLen), "HmacSHA256"));
            byte[] actualMessage = Arrays.copyOfRange(message, messageOffset, messageOffset + messageLen);
            calcDigest = hmac.doFinal(actualMessage);
            //if (log.isTraceEnabled()) {
            //    log.trace("Calculated digest: {}", Hex.encodeHexString(calcDigest));
            //}
            System.arraycopy(calcDigest, 0, digest, digestOffset, DIGEST_LENGTH);
        } catch (InvalidKeyException e) {
            log.error("Invalid key", e);
        } catch (Exception e) {
            log.error("Hash calculation failed", e);
        }
    }

    /**
     * Calculates the swf verification token.
     * 
     * @param handshakeMessage servers handshake bytes
     * @param swfHash hash of swf
     * @param swfSize size of swf
     */
    public void calculateSwfVerification(byte[] handshakeMessage, byte[] swfHash, int swfSize) {
        // SHA256 HMAC hash of decompressed SWF, key are the last 32 bytes of the server handshake
        byte[] swfHashKey = new byte[DIGEST_LENGTH];
        System.arraycopy(handshakeMessage, Constants.HANDSHAKE_SIZE - DIGEST_LENGTH, swfHashKey, 0, DIGEST_LENGTH);
        byte[] bytesFromServerHash = new byte[DIGEST_LENGTH];
        calculateHMAC_SHA256(swfHash, 0, swfHash.length, swfHashKey, DIGEST_LENGTH, bytesFromServerHash, 0);
        // construct SWF verification pong payload
        ByteBuffer swfv = ByteBuffer.allocate(42);
        swfv.put((byte) 0x01);
        swfv.put((byte) 0x01);
        swfv.putInt(swfSize);
        swfv.putInt(swfSize);
        swfv.put(bytesFromServerHash);
        swfv.flip();
        swfVerificationBytes = new byte[42];
        swfv.get(swfVerificationBytes);
        log.debug("initialized swf verification response from swfSize: {} swfHash:\n{}\n{}", swfSize, Hex.encodeHexString(swfHash), Hex.encodeHexString(swfVerificationBytes));
    }
    
    /**
     * Returns the DH offset from an array of bytes.
     * 
     * @param algorithm validation algorithm
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return DH offset
     */
    public int getDHOffset(int algorithm, byte[] handshake, int bufferOffset) {
        switch (algorithm) {
            case 1:
                return getDHOffset2(handshake, bufferOffset);
            default:
            case 0:
                return getDHOffset1(handshake, bufferOffset);
        }
    }

    /**
     * Returns the DH byte offset.
     * 
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return dh offset
     */
    protected int getDHOffset1(byte[] handshake, int bufferOffset) {
        bufferOffset += 1532;
        int offset = handshake[bufferOffset] & 0xff; // & 0x0ff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 632) + 772;
        if (res + KEY_LENGTH > 1531) {
            log.error("Invalid DH offset");
        }
        return res;
    }

    /**
     * Returns the DH byte offset.
     * 
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return dh offset
     */
    protected int getDHOffset2(byte[] handshake, int bufferOffset) {
        bufferOffset += 768;
        int offset = handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 632) + 8;
        if (res + KEY_LENGTH > 767) {
            log.error("Invalid DH offset");
        }
        return res;
    }

    /**
     * Returns the digest offset using current validation scheme.
     * 
     * @param algorithm validation algorithm
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return digest offset
     */
    public int getDigestOffset(int algorithm, byte[] handshake, int bufferOffset) {
        switch (algorithm) {
            case 1:
                return getDigestOffset2(handshake, bufferOffset);
            default:
            case 0:
                return getDigestOffset1(handshake, bufferOffset);
        }
    }

    /**
     * Returns a digest byte offset.
     * 
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return digest offset
     */
    protected int getDigestOffset1(byte[] handshake, int bufferOffset) {
        bufferOffset += 8;
        int offset = handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 728) + 12;
        if (res + DIGEST_LENGTH > 771) {
            log.error("Invalid digest offset calc: {}", res);
        }
        return res;
    }

    /**
     * Returns a digest byte offset.
     * 
     * @param handshake handshake sequence
     * @param bufferOffset buffer offset
     * @return digest offset
     */
    protected int getDigestOffset2(byte[] handshake, int bufferOffset) {
        bufferOffset += 772;
        int offset = handshake[bufferOffset] & 0xff; // & 0x0ff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = Math.abs((offset % 728) + 776);
        if (res + DIGEST_LENGTH > 1535) {
            log.error("Invalid digest offset calc: {}", res);
        }
        return res;
    }

    /**
     * RTMPE type 8 uses XTEA on the regular signature http://en.wikipedia.org/wiki/XTEA
     * 
     * @param array array to get signature
     * @param offset offset to start from
     * @param keyid ID of XTEA key
     */
    public final static void getXteaSignature(byte[] array, int offset, int keyid) {
        int num_rounds = 32;
        int v0, v1, sum = 0, delta = 0x9E3779B9;
        int[] k = XTEA_KEYS[keyid];
        v0 = ByteBuffer.wrap(array, offset, 4).getInt();
        v1 = ByteBuffer.wrap(array, offset + 4, 4).getInt();
        for (int i = 0; i < num_rounds; i++) {
            v0 += (((v1 << 4) ^ (v1 >> 5)) + v1) ^ (sum + k[sum & 3]);
            sum += delta;
            v1 += (((v0 << 4) ^ (v0 >> 5)) + v0) ^ (sum + k[(sum >> 11) & 3]);
        }
        ByteBuffer tmp = ByteBuffer.allocate(4);
        tmp.putInt(v0);
        tmp.flip();
        System.arraycopy(tmp.array(), 0, array, offset, 4);
        tmp.clear();
        tmp.putInt(v1);
        tmp.flip();
        System.arraycopy(tmp.array(), 0, array, offset + 4, 4);
    }

    /**
     * RTMPE type 9 uses Blowfish on the regular signature http://en.wikipedia.org/wiki/Blowfish_(cipher)
     * 
     * @param array array to get signature
     * @param offset offset to start from
     * @param keyid ID of XTEA key
     */
    public final static void getBlowfishSignature(byte[] array, int offset, int keyid) {
        BlowfishEngine bf = new BlowfishEngine();
        // need to use little endian
        bf.init(true, new KeyParameter(BLOWFISH_KEYS[keyid]));
        byte[] output = new byte[8];
        bf.processBlock(array, offset, output, 0);
        System.arraycopy(output, 0, array, offset, 8);
    }

    /**
     * Returns whether or not a given handshake type is valid.
     * 
     * @param handshakeType the type of handshake
     * @return true if valid and supported, false otherwise
     */
    public final static boolean validHandshakeType(byte handshakeType) {
        switch (handshakeType) {
            case RTMPConnection.RTMP_NON_ENCRYPTED:
            case RTMPConnection.RTMP_ENCRYPTED:
            case RTMPConnection.RTMP_ENCRYPTED_XTEA:
            case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
            case RTMPConnection.RTMP_ENCRYPTED_UNK:
                return true;
        }
        return false;
    }

    /**
     * Whether or not encryptions is in use.
     * 
     * @return true if handshake type is an encrypted type, false otherwise
     */
    public boolean useEncryption() {
        switch (handshakeType) {
            case RTMPConnection.RTMP_ENCRYPTED:
            case RTMPConnection.RTMP_ENCRYPTED_XTEA:
            case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
                return true;
        }
        return false;
    }

    /**
     * Sets the handshake type. Currently only two types are supported, plain and encrypted.
     * 
     * @param handshakeType handshake type
     */
    public void setHandshakeType(byte handshakeType) {
        if (log.isTraceEnabled()) {
            if (handshakeType < HANDSHAKE_TYPES.length) {
                log.trace("Setting handshake type: {}", HANDSHAKE_TYPES[handshakeType]);
            } else {
                log.trace("Invalid handshake type: {}", handshakeType);
            }
        }
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
     * Gets the DH offset in the handshake bytes array based on validation scheme Generates DH keypair Adds public key to handshake bytes
     * 
     * @return cipher
     */
    public Cipher getCipherOut() {
        return cipherOut;
    }

    /**
     * Returns the contained handshake bytes. These are just random bytes if the player is using an non-versioned player.
     * 
     * @return cipher
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

    /**
     * Returns the buffer size.
     * 
     * @return buffer remaining
     */
    public int getBufferSize() {
        return buffer.limit() - buffer.remaining();
    }

    /**
     * Add a byte array to the buffer.
     * 
     * @param in incoming bytes
     */
    public void addBuffer(byte[] in) {
        buffer.put(in);
    }

    /**
     * Add a IoBuffer to the buffer.
     * 
     * @param in incoming IoBuffer
     */
    public void addBuffer(IoBuffer in) {
        byte[] tmp = new byte[in.remaining()];
        in.get(tmp);
        if (log.isDebugEnabled()) {
            log.debug("addBuffer - pos: {} limit: {} remain: {}", buffer.position(), buffer.limit(), buffer.remaining());
        }
        if (buffer.remaining() == 0) {
            buffer.clear();
        }
        buffer.put(tmp);
    }

    /**
     * Returns buffered IoBuffer itself.
     * 
     * @return IoBuffer
     */
    public IoBuffer getBufferAsIoBuffer() {
        return buffer.flip();
    }

    /**
     * Returns buffered byte array.
     * 
     * @return bytes
     */
    public byte[] getBuffer() {
        buffer.flip();
        byte[] tmp = new byte[buffer.remaining()];
        buffer.get(tmp);
        buffer.clear();
        return tmp;
    }

}
