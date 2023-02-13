package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.junit.Test;

import io.antmedia.recorder.Frame;
import io.antmedia.recorder.Frame.Type;

public class FrameTest {
	
	@Test
	public void testFrameClone() {
		Frame frame = new Frame(480, 360, Frame.DEPTH_UBYTE, 2);
		
		Frame clone = frame.clone();
		
		assertNotEquals(frame, clone);
		
		assertEquals(480, clone.getImageWidth());
		assertEquals(360, clone.getImageHeight());
		assertEquals(Frame.DEPTH_UBYTE, clone.getImageDepth());
		assertEquals(2, clone.getImageChannels());
		assertEquals(960, clone.getImageStride());
		
		EnumSet<Type> types = clone.getTypes();
		
		assertTrue(types.contains(Type.VIDEO));
		
		assertNotNull(clone.getOpaque());
		
		clone.close();
		
		assertNull(clone.getOpaque());
	}
	
	@Test
	public void testFrameCloneTypes() {
		Frame frame = new Frame(480, 360, Frame.DEPTH_SHORT, 2);
		Frame clone = frame.clone();
		assertNotEquals(frame, clone);
		
		frame = new Frame(480, 360, Frame.DEPTH_INT, 2);
		clone = frame.clone();
		assertNotEquals(frame, clone);
		
		frame = new Frame(480, 360, Frame.DEPTH_LONG, 2);
		clone = frame.clone();
		assertNotEquals(frame, clone);
		
		frame = new Frame(480, 360, Frame.DEPTH_FLOAT, 2);
		clone = frame.clone();
		assertNotEquals(frame, clone);
		
		frame = new Frame(480, 360, Frame.DEPTH_DOUBLE, 2);
		clone = frame.clone();
		assertNotEquals(frame, clone);
	}
	
	@Test
	public void testSamples() {
		
		Frame frame = new Frame();
		
		frame.setSamples(new Buffer[1]);
		frame.getSamples()[0] = ByteBuffer.allocate(200);
		
		frame.setSampleRate(48000);
		Frame clone = frame.clone();
		
		assertEquals(48000, clone.getSampleRate());
		assertEquals(1, clone.getSamples().length);
		assertEquals(200, clone.getSamples()[0].capacity());
		
		EnumSet<Type> types = clone.getTypes();
		assertTrue(types.contains(Type.AUDIO));
	}
	
	@Test
	public void testData() {
		
		Frame frame = new Frame();
		
		frame.setData(ByteBuffer.allocate(100));
	
		Frame clone = frame.clone();
		assertEquals(100, clone.getData().capacity());
		
		EnumSet<Type> types = clone.getTypes();
		assertTrue(types.contains(Type.DATA));
	}

}
