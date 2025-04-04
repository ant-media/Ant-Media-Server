package io.antmedia.eRTMP;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AVCVideo;
import org.red5.codec.VideoCodec;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.ExVideoPacketType;
import org.red5.server.net.rtmp.event.VideoData.VideoFourCC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HEVC Video codec complaint with Enhanced RTMP
 * There is another HEVCVideo that uses codec id as 12. This one use fourcc for codec id
 */
public class HEVCVideoEnhancedRTMP extends AVCVideo {

	static final String CODEC_NAME = "HEVC";

	private static Logger log = LoggerFactory.getLogger(HEVCVideoEnhancedRTMP.class);

	public HEVCVideoEnhancedRTMP() {
		this.reset();
	}

	@Override
	public String getName() {
		return CODEC_NAME;
	}

	@Override
	public boolean canDropFrames() {
		return true;
	}

	public boolean canHandleData(IoBuffer data) {
		boolean result = false;
		if (data.limit() > 0) 
		{
			int firstByte = data.get() & 0xff;

			boolean exVideoHeader = ((firstByte & VideoData.MASK_EX_VIDEO_TAG_HEADER) >> 7) == 1;
			if (exVideoHeader) {


				int videoPacketType = (firstByte & VideoData.MASK_EX_VIDEO_PACKET_TYPE);
				ExVideoPacketType[] values = ExVideoPacketType.values();
				ExVideoPacketType exVideoPacketType = values[videoPacketType];

				if (data.remaining() >= 4) 
				{ 
					byte[] fourcc = new byte[4];
					
					data.get(fourcc);
					VideoFourCC videoFourCc = VideoData.findFourCcByValue(VideoFourCC.makeFourCc(new String(fourcc)));
	
					if (videoFourCc != null && videoFourCc == VideoFourCC.HEVC_FOURCC) {
						result = true;
				}
				}
			}


			data.rewind();
		}
		return result;
	}
	
	@Override
	public boolean addData(IoBuffer data, int timestamp) 
	{
		if (data.hasRemaining()) {
			// mark
			int start = data.position();
			// get frame type

			int firstByte = data.get() & 0xff;
			// Check if this is an extended video tag header
			int frameTypeLocal = (firstByte & VideoData.MASK_EX_VIDEO_FRAME_TYPE) >> 4;
			int videoPacketType = (firstByte & VideoData.MASK_EX_VIDEO_PACKET_TYPE);

			ExVideoPacketType exVideoPacketType =  ExVideoPacketType.values()[videoPacketType];

			if (exVideoPacketType == ExVideoPacketType.SEQUENCE_START) {
				// body contains a configuration record to start the sequence. See ISO/IEC
				// 14496-15, 8.3.3.1.2 for the description of HEVCDecoderConfigurationRecord
				//hevcHeader = [HEVCDecoderConfigurationRecord]
				data.rewind();
				decoderConfiguration.setData(data);
				softReset();
			}
			else if (exVideoPacketType == ExVideoPacketType.CODED_FRAMES) {
				// See ISO/IEC 14496-12, 8.15.3 for an explanation of composition times.
				// The offset in an FLV file is always in milliseconds.

				//compositionTimeOffset = SI24
				// Body contains one or more NALUs; full frames are required
				//hevcData = [HevcCodedData]

				//we've implemented coded frames composition tmie offset in MuxAdaptor. 


			}
			else if (exVideoPacketType == ExVideoPacketType.CODED_FRAMESX) {
				// Body contains one or more NALUs; full frames are required
				// hevcData = [HevcCodedData]


			}

			if (frameTypeLocal == FLAG_FRAMETYPE_KEYFRAME) {
				data.rewind();
				if (timestamp != keyframeTimestamp) {
					//log.trace("New keyframe");
					// new keyframe
					keyframeTimestamp = timestamp;
					// if its a new keyframe, clear keyframe and interframe collections
					softReset();
				}
				// store keyframe
				keyframes.add(new FrameData(data));
			}
			else
			{
				// rewind
				data.rewind();
				try {
					int lastInterframe = numInterframes.getAndIncrement();
					if (lastInterframe < interframes.size()) {
						interframes.get(lastInterframe).setData(data);
					} else {
						interframes.add(new FrameData(data));
					}
				} catch (Throwable e) {
					log.error("Failed to buffer interframe", e);
				}
			}
			// go back to where we started
			data.position(start);
		}
		return true;
	}



}
