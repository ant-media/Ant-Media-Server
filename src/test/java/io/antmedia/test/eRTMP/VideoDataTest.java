package io.antmedia.test.eRTMP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.codec.AVCVideo;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.ExVideoPacketType;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.event.VideoData.VideoFourCC;

public class VideoDataTest {


	@Test
	public void testVideoDataHEVC() {

		//HEVC header first byte is exVideoTagHeader - exVideoHeader(1 bit) + videoFrameType(3 bit) + videoPacketType(4 bit)
		//Next 4 bytes are fourcc


		IoBuffer buffer = HEVCVideoTest.createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_TAG_HEADER | (VideoData.FLAG_FRAMETYPE_KEYFRAME << 4) | ExVideoPacketType.SEQUENCE_START.value), 
				(byte) (VideoFourCC.HEVC_FOURCC.value), (byte)(VideoFourCC.HEVC_FOURCC.value >> 8), (byte)(VideoFourCC.HEVC_FOURCC.value >> 16), 
				(byte) (VideoFourCC.HEVC_FOURCC.value >> 24));
		VideoData videoData = new VideoData(buffer);


		assertEquals(VideoFourCC.HEVC_FOURCC.value, videoData.getCodecId());
		assertTrue(videoData.isExVideoHeader());
		assertEquals(ExVideoPacketType.SEQUENCE_START, videoData.getExVideoPacketType());
		assertEquals(FrameType.KEYFRAME, videoData.getFrameType());


		try {
			buffer = HEVCVideoTest.createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_TAG_HEADER | (VideoData.FLAG_FRAMETYPE_INTERFRAME << 4) | ExVideoPacketType.SEQUENCE_START.value), 
					(byte) (0), (byte)(0), (byte)(0) ,
					(byte) (VideoFourCC.HEVC_FOURCC.value >> 24));
			videoData = new VideoData(buffer);

			fail("It should throw exception");
		}
		catch (Exception e) {

		}

	}
	
	@Test
	public void testVideoDataAVC() {
		//AVC header first byte is  videoFrameType(4 bit) + video Codec Id(4 bit) 
		//Next 1 byte AVCPacketType
		//Next 3 bytes are composition time
		
		IoBuffer buffer = HEVCVideoTest.createIoBufferWithData((byte) ((VideoData.FLAG_FRAMETYPE_DISPOSABLE << 4) | 7 ), (byte)0, (byte)0, (byte)0); //7 is AVC codec id for FLV
		
		VideoData videoData = new VideoData(buffer);
		
		assertEquals(7, videoData.getCodecId());
		assertFalse(videoData.isExVideoHeader());
		assertNull(videoData.getExVideoPacketType());
		assertEquals(FrameType.DISPOSABLE_INTERFRAME, videoData.getFrameType());
		assertTrue(videoData.isConfig());
	}


}
