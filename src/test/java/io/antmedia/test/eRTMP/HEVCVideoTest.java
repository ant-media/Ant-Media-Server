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
import org.red5.codec.HEVCVideo;
import org.red5.server.net.rtmp.event.VideoData;


public class HEVCVideoTest  {

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
		data.put((byte) 0x1C); // first byte

		data.flip();
		result = hevcVideo.canHandleData(data);
		assertTrue(result);


		data = IoBuffer.allocate(10);
		data.put((byte) 0x0C); // first byte

		data.flip();
		result = hevcVideo.canHandleData(data);
		assertTrue(result);

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
	public void testAddData() {
		IoBuffer buffer = createIoBufferWithData((byte) 0x1C, (byte)0x00, (byte)0x00);
		int timestamp = 1000;

		
		assertNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getDecoderConfiguration());
		boolean result = hevcVideo.addData(buffer, timestamp);

		assertTrue(result);
		
		IoBuffer actualConfiguration = hevcVideo.getDecoderConfiguration();
        assertNotNull("DecoderConfiguration should not be null", actualConfiguration);
		assertNull(hevcVideo.getKeyframe());

		
		buffer = createIoBufferWithData((byte) 0x1C, (byte)0x01);
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		
		assertNull(hevcVideo.getInterframe(0));
		buffer = createIoBufferWithData((byte) (VideoData.FLAG_FRAMETYPE_INTERFRAME << 4), (byte)0x01);
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getInterframe(0));
		
		
		assertNull(hevcVideo.getInterframe(0));
		buffer = createIoBufferWithData((byte) (VideoData.FLAG_FRAMETYPE_INTERFRAME << 4 | (byte)12), (byte)0x01);
		//hevcVideo.setBufferInterframes(true);
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getInterframe(0));
		
		buffer = createIoBufferWithData((byte) (VideoData.FLAG_FRAMETYPE_INTERFRAME << 4 | (byte)12), (byte)0x01);
		hevcVideo.setBufferInterframes(true);
		result = hevcVideo.addData(buffer, timestamp);
		assertNotNull(hevcVideo.getKeyframe());
		assertNotNull(hevcVideo.getInterframe(0));
		
		
		buffer = createIoBufferWithData((byte) (VideoData.FLAG_FRAMETYPE_KEYFRAME << 4 | (byte)12), (byte)0x01);
		result = hevcVideo.addData(buffer, timestamp+1);
		assertNotNull(hevcVideo.getKeyframe());
		assertNull(hevcVideo.getInterframe(0));
		
		result = hevcVideo.addData(IoBuffer.allocate(10).position(10));	
		

        
	}

	public static IoBuffer createIoBufferWithData(byte... data) {
		return IoBuffer.wrap(data);
		
	}







}
