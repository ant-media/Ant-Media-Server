package io.antmedia.test.rtmp;

import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.IEndpointStatusListener;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.RtmpMuxer;
import io.vertx.core.Vertx;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RtmpMuxerUnitTests {

	@Test
	public void testOutputFormatCtx(){
		Vertx vertx = mock(Vertx.class);
		RtmpMuxer muxer = new RtmpMuxer("rtmp://test-link/test-app/", vertx);

		AVFormatContext ctx = muxer.getOutputFormatContext();
		assertEquals("flv", ctx.oformat().name().getString());

		muxer = spy(new RtmpMuxer("rtmp://test-link/test-app/", vertx));
		doReturn(-1).when(muxer).avFormatAllocOutputContext2Wrapper();
		ctx = muxer.getOutputFormatContext();
		assertNull(ctx);
	}

	@Test
	public void testSetStatus() {
		Vertx vertx = mock(Vertx.class);
		String url = "rtmp://test-link/test-app/streamId";
		RtmpMuxer muxer = new RtmpMuxer(url, vertx);

		IEndpointStatusListener listener = mock(IEndpointStatusListener.class);
		muxer.setStatusListener(listener);

		muxer.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		verify(listener).endpointStatusUpdated(url, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, muxer.getStatus());

		muxer.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		verify(listener, times(1)).endpointStatusUpdated(anyString(), anyString());

		muxer.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		verify(listener).endpointStatusUpdated(url, IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
	}

	@Test
	public void testPrepareIO() {
		Vertx realVertx = Vertx.vertx();
		try {
			RtmpMuxer muxer = spy(new RtmpMuxer("rtmp://test-link/test-app/streamId", realVertx));

			doReturn(true).when(muxer).openIO();
			doAnswer(invocation -> {
				muxer.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				return true;
			}).when(muxer).writeHeader();

			doReturn(1).when(muxer).getStreamCount();

			muxer.prepareIO();

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
				IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(muxer.getStatus())
			);
			verify(muxer).writeHeader();

			RtmpMuxer muxerFail = spy(new RtmpMuxer("rtmp://test-link/test-app/streamId", realVertx));
			doReturn(false).when(muxerFail).openIO();
			doReturn(1).when(muxerFail).getStreamCount();

			muxerFail.prepareIO();

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
				IAntMediaStreamHandler.BROADCAST_STATUS_FAILED.equals(muxerFail.getStatus())
			);
			verify(muxerFail).clearResource();
			
		} finally {
			realVertx.close();
		}
	}

	@Test
	public void testWriteHeaderAfterTrailer() {
		Vertx vertx = mock(Vertx.class);
		TestableRtmpMuxer muxer = new TestableRtmpMuxer("rtmp://test-link/test-app/streamId", vertx);

		muxer.setTrailerWritten(true);
		boolean result = muxer.writeHeader();
		assertFalse(result);
	}

	@Test
	public void testWriteTrailer() {
		Vertx vertx = mock(Vertx.class);
		TestableRtmpMuxer muxer = new TestableRtmpMuxer("rtmp://test-link/test-app/streamId", vertx);
		
		muxer.writeTrailer();
		// Header not written, so it should return early and not set status to FINISHED
		assertNotEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
		
		// Simulate header written
		muxer.setHeaderWritten(true);
		muxer.writeTrailer();
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
		
		// Reset for worker thread test
		muxer = new TestableRtmpMuxer("rtmp://test-link/test-app/streamId", vertx);
		muxer.setHeaderWritten(true);
		muxer.startWorkerThread();
		
		muxer.writeTrailer();
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
	}

	@Test
	public void testWriteVideoBuffer() {
		Vertx vertx = mock(Vertx.class);
		TestableRtmpMuxer muxer = spy(new TestableRtmpMuxer("rtmp://test-link/test-app/streamId", vertx));
		
		// Prevent calling the base class method that might do complex logic or native calls
		// We only want to verify that RtmpMuxer logic passes control to it.
		doNothing().when(muxer).writeVideoBuffer(any(Muxer.VideoBuffer.class));

		int streamIndex = 0;
		// Case 1: Muxer not running - should return early
		muxer.setIsRunning(false);
		muxer.writeVideoBuffer(null, 0, 0, streamIndex, true, 0, 0);
		verify(muxer, never()).writeVideoBuffer(any(Muxer.VideoBuffer.class));

		// Case 2: Stream not registered - should return early
		muxer.setIsRunning(true);
		// registeredStreamIndexList is empty by default
		muxer.writeVideoBuffer(null, 0, 0, streamIndex, true, 0, 0);
		verify(muxer, never()).writeVideoBuffer(any(Muxer.VideoBuffer.class));

		// Register stream
		muxer.getRegisteredStreamIndexList().add(streamIndex);

		// Case 3: Keyframe not yet received, incoming frame is NOT keyframe - should return early (keyFrameReceived flag logic)
		// keyFrameReceived is false by default
		muxer.writeVideoBuffer(null, 0, 0, streamIndex, false, 0, 0);
		verify(muxer, never()).writeVideoBuffer(any(Muxer.VideoBuffer.class));

		// Case 4: Keyframe not yet received, incoming frame IS keyframe - should set flag and call super
		muxer.writeVideoBuffer(null, 0, 0, streamIndex, true, 0, 0);
	}

	static class TestableRtmpMuxer extends RtmpMuxer {
		public TestableRtmpMuxer(String url, Vertx vertx) {
			super(url, vertx);
		}
		
		public void setHeaderWritten(boolean v) {
			this.headerWritten = v;
		}
		
		public void setIsRunning(boolean v) {
			this.isRunning.set(v);
		}

		public void setTrailerWritten(boolean v) {
			this.trailerWritten = v;
		}

		@Override
		public boolean writeSuperHeader() {
			// Override to prevent native calls
			return true;
		}
		
		@Override
		public void superWriteTrailer() {
			// Override to prevent native calls
		}
	}
}
