/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

/**
 * Video codecs that Red5 supports.
 * 
 * @author Art Clarke
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum VideoCodec {

    JPEG((byte) 0x01), H263((byte) 0x02), SCREEN_VIDEO((byte) 0x03), VP6((byte) 0x04), VP6a((byte) 0x05), SCREEN_VIDEO2((byte) 0x06), AVC((byte) 0x07);

    private byte id;

    private VideoCodec(byte id) {
        this.id = id;
    }

    /**
     * Returns back a numeric id for this codec, that happens to correspond to the numeric identifier that FLV will use for this codec.
     * 
     * @return the codec id
     */
    public byte getId() {
        return id;
    }

}