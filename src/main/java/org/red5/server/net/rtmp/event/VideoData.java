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

package org.red5.server.net.rtmp.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.VideoCodec;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.IStreamData;

/**
 * Video data event
 */
public class VideoData extends BaseEvent implements IoConstants, IStreamData<VideoData>, IStreamPacket {

	private static final long serialVersionUID = 5538859593815804830L;

	public static final String CODEC_HEVC = "hvc1";
	public static final String CODEC_AVC = "avc1";
	public static final String CODEC_AV1 = "av01";
	public static final String CODEC_VP9 = "vp09";
	public static final String CODEC_VP8 = "vp08";


	/**
	 * Videoframe type
	 */
	public static enum FrameType {
		UNKNOWN, KEYFRAME, INTERFRAME, DISPOSABLE_INTERFRAME, COMMAND
	}

	public static final int MASK_EX_VIDEO_TAG_HEADER = 0x80;

	public static final int MASK_EX_VIDEO_FRAME_TYPE = 0x70;
	
	public static final int MASK_EX_VIDEO_PACKET_TYPE = 0x0F;
	
	public enum ExVideoPacketType {
		SEQUENCE_START(0),
		CODED_FRAMES(1),
		SEQUENCE_END(2),
		CODED_FRAMESX(3),
		METADATA(4),
		MPEG2TS_SEQUENCE_START(5),
		MULTITRACK(6);
		// 7 - Reserved
		// ...
		// 14 - reserved
		// 15 - reserved

		public final int value;

		ExVideoPacketType(int value) {
			this.value = value;
		}

	
	}

	
	public enum VideoFourCC {
		//
		// Valid FOURCC values for signaling support of video codecs
		// in the enhanced FourCC pipeline. In this context, support
		// for a FourCC codec MUST be signaled via the enhanced
		// "connect" command.
		//
		VP8_FOURCC(CODEC_VP8),
		VP9_FOURCC(CODEC_VP9),
		AV1_FOURCC(CODEC_AV1),
		AVC_FOURCC(CODEC_AVC),
		HEVC_FOURCC(CODEC_HEVC);
		
		public final int value;

		VideoFourCC(String value) {
			this.value = makeFourCc(value);
		}
		
		public static int makeFourCc(String fourcc) {
			return  fourcc.charAt(0) | (fourcc.charAt(1) << 8) | (fourcc.charAt(2) << 16) | (fourcc.charAt(3) << 24); 
		}
	}
	
	

	/**
	 *	ExVideoHeader  for enchanced RTMP
	 */
	private boolean exVideoHeader = false;
		
	/**
	 * ExVideoPacketType
	 */
	private ExVideoPacketType exVideoPacketType;


    private long receivedTime;

	/**
	 * Video data
	 */
	protected IoBuffer data;

	/**
	 * Data type
	 */
	private byte dataType = TYPE_VIDEO_DATA;

	/**
	 * Frame type, unknown by default
	 */
	private FrameType frameType = FrameType.UNKNOWN;

	/**
	 * The codec id 
	 * It can be the values from legacy implementation
	 * such as 
	 * VideoCodec or VideoFourCC.HEVC_FOURCC
	 */
	private int codecId = -1;

	/**
	 * True if this is configuration data and false otherwise
	 */
	protected boolean config;

	/** Constructs a new VideoData. */
	public VideoData() {
		this(IoBuffer.allocate(0).flip());
	}

	/**
	 * Create video data event with given data buffer
	 * 
	 * @param data
	 *            Video data
	 */
	public VideoData(IoBuffer data) {
		super(Type.STREAM_DATA);
		setData(data);
	}

	/**
	 * Create video data event with given data buffer
	 * 
	 * @param data
	 *            Video data
	 * @param copy
	 *            true to use a copy of the data or false to use reference
	 */
	public VideoData(IoBuffer data, boolean copy) {
		super(Type.STREAM_DATA);
		if (copy) {
			byte[] array = new byte[data.limit()];
			data.mark();
			data.get(array);
			data.reset();
			setData(array);
		} else {
			setData(data);
		}
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return dataType;
	}

	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/** {@inheritDoc} */
	public IoBuffer getData() {
		return data;
	}

	public void setData(IoBuffer data) {
		this.data = data;
		if (data != null && data.limit() > 0) 
		{
			data.mark();
			int firstByte = data.get() & 0xff;

			// Check if this is an extended video tag header
			//isExVideoHeader = UB[1]   Extended video flag
			exVideoHeader = ((firstByte & MASK_EX_VIDEO_TAG_HEADER) >> 7) == 1;
			int frameTypeLocal = 0;

			if (exVideoHeader) 
			{
				//videoFrameType = UB[3] as VideoFrameType 
				frameTypeLocal = (firstByte & MASK_EX_VIDEO_FRAME_TYPE) >> 4;
		
		
				// The UB[4] bits are interpreted as VideoPacketType
				// instead of VideoCodecId
				int videoPacketType = (firstByte & MASK_EX_VIDEO_PACKET_TYPE);
				ExVideoPacketType[] values = ExVideoPacketType.values();
				exVideoPacketType = values[videoPacketType];
				log.debug("Extended video tag header - exVideoPacketType: {} frameType value:{}", exVideoPacketType.name(), frameTypeLocal);

			}
			else 
			{     
				codecId = firstByte & ITag.MASK_VIDEO_CODEC;
				if (codecId == VideoCodec.AVC.getId()) 
				{
					config = (data.get() == 0);
				}

				frameTypeLocal = (firstByte & MASK_VIDEO_FRAMETYPE) >> 4;

			}
		

			if (frameTypeLocal == FLAG_FRAMETYPE_KEYFRAME) 
			{
				this.frameType = FrameType.KEYFRAME;
			} 
			else if (frameTypeLocal == FLAG_FRAMETYPE_INTERFRAME) 
			{
				this.frameType = FrameType.INTERFRAME;
			} 
			else if (frameTypeLocal == FLAG_FRAMETYPE_DISPOSABLE) 
			{
				this.frameType = FrameType.DISPOSABLE_INTERFRAME;
			} 
			else if (frameTypeLocal == FLAG_FRAMETYPE_INFO) 
			{
				this.frameType = FrameType.COMMAND;
			}
			else {
				this.frameType = FrameType.UNKNOWN;
			}
			
			
			if (exVideoHeader) {
				
				if (exVideoPacketType != ExVideoPacketType.METADATA &&
						this.frameType == FrameType.COMMAND) 
				{
					log.info("Command is not yet supported in video data, incoming command:{}", data.get() & 0xFF);

				}
				else if (exVideoPacketType == ExVideoPacketType.MULTITRACK) {
					log.info("Multitrack is not yet supported in video data");
				}
				else {
					//we already read the first byte and the next bytes are the fourCC
					//get the fourCC
					byte[] fourcc = new byte[4];
					data.get(fourcc);
					
					
					VideoFourCC videoFourCc = findFourCcByValue(VideoFourCC.makeFourCc(new String(fourcc)));
					
					if (videoFourCc == null) {
						throw new IllegalArgumentException("Video fourcc cannot be found");
					}
					log.debug("Incoming video fourcc:{} videoPacketType:{} frameType:{}", videoFourCc.name(),  exVideoPacketType.name(), frameType.name());
					
					codecId = videoFourCc.value;
				
				}
				
			}
			
			//don't forget to reset the buffer
			data.reset();
			
		}
	}
	
	 public static VideoFourCC findFourCcByValue(int value) {
        for (VideoFourCC fourCc : VideoFourCC.values()) {
            if (fourCc.value == value) {
                return fourCc;
            }
        }
        return null; // or throw an exception if not found
    }

	



	public void setData(byte[] data) {
		this.data = IoBuffer.allocate(data.length);
		this.data.put(data).flip();
	}

	/**
	 * Getter for frame type
	 *
	 * @return Type of video frame
	 */
	public FrameType getFrameType() {
		return frameType;
	}

	public int getCodecId() {
		return codecId;
	}

	public boolean isConfig() {
		return config;
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {
		if (data != null) {
			final IoBuffer localData = data;
			// null out the data first so we don't accidentally
			// return a valid reference first
			data = null;
			localData.clear();
			localData.free();
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		frameType = (FrameType) in.readObject();
		byte[] byteBuf = (byte[]) in.readObject();
		if (byteBuf != null) {
			setData(IoBuffer.wrap(byteBuf));
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(frameType);
		if (data != null) {
			out.writeObject(data.array());
		} else {
			out.writeObject(null);
		}
	}

	/**
	 * Duplicate this message / event.
	 * 
	 * @return duplicated event
	 */
	public VideoData duplicate() throws IOException, ClassNotFoundException {
		VideoData result = new VideoData();
		// serialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		writeExternal(oos);
		oos.close();
		// convert to byte array
		byte[] buf = baos.toByteArray();
		baos.close();
		// create input streams
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		ObjectInputStream ois = new ObjectInputStream(bais);
		// deserialize
		result.readExternal(ois);
		ois.close();
		bais.close();
		// clone the header if there is one
		if (header != null) {
			result.setHeader(header.clone());
		}
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return String.format("Video - ts: %s length: %s", getTimestamp(), (data != null ? data.limit() : '0'));
	}

	public boolean isExVideoHeader() {
		return exVideoHeader;
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public ExVideoPacketType getExVideoPacketType() {
		return exVideoPacketType;
	}

	public void setConfig(boolean config) {
		this.config = config;
	}

	public void setReceivedTime(long timeMillis) {
		this.receivedTime = timeMillis;
	}

}
