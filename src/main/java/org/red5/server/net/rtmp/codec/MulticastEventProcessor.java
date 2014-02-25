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

package org.red5.server.net.rtmp.codec;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Processes multicast events.
 */
public class MulticastEventProcessor {

	/**
     * Getter for cache ID.
     *
     * @return  Cache ID
     */
    public byte getCacheId() {
		return 0;
	}

    /**
     * Disposes cached object.
	 *
     * @param obj                Cached object
     */
    public void disposeCached(Object obj) {
		if (obj == null) {
			return;
		}
		final IoBuffer[] chunks = (IoBuffer[]) obj;
		for (int c=0;c < chunks.length;c++) {
			chunks[c].free();
			chunks[c] = null;
		}
	}

    /**
     * Breaks buffer into chunks of given size.
	 *
     * @param buf                IoBuffer
     * @param size               Chunk size
     * @return                   Array of byte buffers, chunks
     */
    public static IoBuffer[] chunkBuffer(IoBuffer buf, int size) {
		final int num = (int) Math.ceil(buf.limit() / (float) size);
		final IoBuffer[] chunks = new IoBuffer[num];
		for (int i = 0; i < num; i++) {
			chunks[i] = buf.asReadOnlyBuffer();
			final IoBuffer chunk = chunks[i];
			int position = size * num;
			chunk.position(position);
			if (position + size < chunk.limit()) {
				chunk.limit(position + size);
			}
		}
		return chunks;
	}

}
