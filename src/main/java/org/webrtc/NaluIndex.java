package org.webrtc;

public class NaluIndex {
		// Start index of NALU, including start sequence.
		public int startOffset;
		// Start index of NALU payload, typically type header.
		public int payloadStartOffset;
		// Length of NALU payload, in bytes, counting from payload_start_offset.
		public int payloadSize;
		
		public NaluIndex(int start_offset, int payload_start_offset, int payload_size) {
			this.startOffset = start_offset;
			this.payloadStartOffset = payload_start_offset;
			this.payloadSize = payload_size;
		}
		
		@CalledByNative
		public int getStartOffset() {
			return startOffset;
		}
		
		@CalledByNative
		public int getPayloadStartOffset() {
			return payloadStartOffset;
		}
		
		@CalledByNative
		public int getPlayloadSize() {
			return payloadSize;
		}
}
