package io.antmedia.test.webrtc.adaptor;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.webrtc.audio.WebRtcAudioRecord;

public class WebRTCNativeInterfaceTest {
	
	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@Before
	public void setup() {

	}


	@Test
	public void testNotifyEncodedData() {
		WebRtcAudioRecord audioRecord = spy(new WebRtcAudioRecord(null, null, 0, 0,null, null, null, false, false, null));
		doNothing().when(audioRecord).nativeEncodedDataIsReady(anyLong(), anyString(), anyInt());
		String trackId = "track1";
		audioRecord.getEncodedByteBuffers().put(trackId, ByteBuffer.allocate(1000));
		ByteBuffer audio = ByteBuffer.allocate(100);
		audioRecord.notifyEncodedData(trackId, audio);
	}
}
