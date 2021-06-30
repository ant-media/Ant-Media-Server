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

package org.red5.server.net.rtmp.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.so.ISharedObjectMessage;

/**
 * Encodes events to byte buffer.
 */
public interface IEventEncoder {
    /**
     * Encodes Notify event to byte buffer.
     *
     * @param notify
     *            Notify event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeNotify(Notify notify);

    /**
     * Encodes Invoke event to byte buffer.
     *
     * @param invoke
     *            Invoke event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeInvoke(Invoke invoke);

    /**
     * Encodes Ping event to byte buffer.
     *
     * @param ping
     *            Ping event
     * @return Byte buffer
     */
    public abstract IoBuffer encodePing(Ping ping);

    /**
     * Encodes BytesRead event to byte buffer.
     *
     * @param streamBytesRead
     *            BytesRead event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeBytesRead(BytesRead streamBytesRead);

    /**
     * Encodes Aggregate event to byte buffer.
     *
     * @param aggregate
     *            Aggregate event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeAggregate(Aggregate aggregate);

    /**
     * Encodes AudioData event to byte buffer.
     *
     * @param audioData
     *            AudioData event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeAudioData(AudioData audioData);

    /**
     * Encodes VideoData event to byte buffer.
     *
     * @param videoData
     *            VideoData event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeVideoData(VideoData videoData);

    /**
     * Encodes Unknown event to byte buffer.
     *
     * @param unknown
     *            Unknown event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeUnknown(Unknown unknown);

    /**
     * Encodes ChunkSize event to byte buffer.
     *
     * @param chunkSize
     *            ChunkSize event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeChunkSize(ChunkSize chunkSize);

    /**
     * Encodes SharedObjectMessage event to byte buffer.
     *
     * @param so
     *            ISharedObjectMessage event
     * @return Byte buffer
     */
    public abstract IoBuffer encodeSharedObject(ISharedObjectMessage so);

    /**
     * Encodes SharedObjectMessage event to byte buffer using AMF3 encoding.
     *
     * @param so
     *            ISharedObjectMessage event
     * @return Byte buffer
     */
    public IoBuffer encodeFlexSharedObject(ISharedObjectMessage so);
}
