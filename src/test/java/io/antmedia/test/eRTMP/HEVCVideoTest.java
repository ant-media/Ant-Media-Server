package io.antmedia.test.eRTMP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.codec.AVCVideo;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.ExVideoPacketType;
import org.red5.server.net.rtmp.event.VideoData.VideoFourCC;

import io.antmedia.eRTMP.HEVCVideo;

public class HEVCVideoTest extends AVCVideo {

	private HEVCVideo hevcVideo;

	@Before
	public void setUp() {
		hevcVideo = new HEVCVideo();
		// Any additional setup needed
	}

	@Test
	public void testCanHandleData() {
		HEVCVideo hevcVideo = new HEVCVideo();
		IoBuffer data = IoBuffer.allocate(10);
		data.put((byte) 0x00); // first byte
		data.put((byte) 0x01); // second byte
		data.flip();

		boolean result = hevcVideo.canHandleData(data);
		assertFalse(result);

		hevcVideo = new HEVCVideo();
		data = IoBuffer.allocate(10);
		data.put((byte) 0x00); // first byte
		data.put((byte) 0x02); // second byte
		data.flip();

		result = hevcVideo.canHandleData(data);
		assertFalse(result);


		hevcVideo = new HEVCVideo();
		data = IoBuffer.allocate(10);
		data.put((byte) 0x80); // first byte
		data.put((byte) 0x02); // second byte
		data.flip();
		result = hevcVideo.canHandleData(data);

		assertFalse(result);

		hevcVideo = new HEVCVideo();

		data = IoBuffer.allocate(10);
		data.put((byte) 0x80); // first byte

		data.put((byte)(VideoFourCC.HEVC_FOURCC.value)); 
		data.put((byte)(VideoFourCC.HEVC_FOURCC.value >> 8)); 
		data.put((byte)(VideoFourCC.HEVC_FOURCC.value >> 16)); 
		data.put((byte)(VideoFourCC.HEVC_FOURCC.value >> 24)); 

		data.flip();
		result = hevcVideo.canHandleData(data);
		assertTrue(result);


		data = IoBuffer.allocate(0);
		data.flip();
		result = hevcVideo.canHandleData(data);
		assertFalse(result);

		data = IoBuffer.allocate(10);
		data.put((byte) 0x80); // first byte

		data.put((byte)0); 
		data.put((byte)0); 
		data.put((byte)0); 
		data.put((byte)0); 

		data.flip();
		result = hevcVideo.canHandleData(data);
		assertFalse(result);


		data = IoBuffer.allocate(10);
		data.put((byte) 0x80); // first byte

		data.put((byte)(VideoFourCC.AV1_FOURCC.value)); 
		data.put((byte)(VideoFourCC.AV1_FOURCC.value >> 8)); 
		data.put((byte)(VideoFourCC.AV1_FOURCC.value >> 16)); 
		data.put((byte)(VideoFourCC.AV1_FOURCC.value >> 24)); 

		data.flip();
		result = hevcVideo.canHandleData(data);
		assertFalse(result);


	}


	@Test
	public void testGetName() {
		hevcVideo = new HEVCVideo();
		assertEquals("HEVC", hevcVideo.getName());
	}

	@Test
	public void testCanDropFrames() {
		hevcVideo = new HEVCVideo();
		assertTrue(hevcVideo.canDropFrames());
	}




	@Test
	public void testAddData_SequenceStart() {
		IoBuffer buffer = createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_PACKET_TYPE & ExVideoPacketType.SEQUENCE_START.value));
		int timestamp = 1000;

		
		assertNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getDecoderConfiguration());
		boolean result = hevcVideo.addData(buffer, timestamp);

		assertTrue(result);
		
		IoBuffer actualConfiguration = hevcVideo.getDecoderConfiguration();
        assertNotNull("DecoderConfiguration should not be null", actualConfiguration);
		assertNull(hevcVideo.getKeyframe());

		
		buffer = createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_TAG_HEADER | (VideoData.FLAG_FRAMETYPE_KEYFRAME << 4)  | ExVideoPacketType.SEQUENCE_START.value));
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		
		
		assertNull(hevcVideo.getInterframe(0));
		buffer = createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_TAG_HEADER | (VideoData.FLAG_FRAMETYPE_INTERFRAME << 4)  | ExVideoPacketType.CODED_FRAMESX.value));
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		assertNotNull(hevcVideo.getInterframe(0));
		
		
		buffer = createIoBufferWithData((byte) (VideoData.MASK_EX_VIDEO_TAG_HEADER | (VideoData.FLAG_FRAMETYPE_KEYFRAME << 4)  | ExVideoPacketType.SEQUENCE_START.value));
		result = hevcVideo.addData(buffer, timestamp+1);
		assertNotNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getInterframe(0));

        
	}

	public static IoBuffer createIoBufferWithData(byte... data) {
		return IoBuffer.wrap(data);
		
	}







}
