package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
public class MuxAdaptorTest extends UnitTestBase<MuxAdaptor> {

	private TestHLSMuxer hlsMuxer;

	@BeforeEach
	void setUp() {
		classUnderTest = new MuxAdaptor(null);
		hlsMuxer = new TestHLSMuxer();
		classUnderTest.muxerList.add(hlsMuxer);
	}

	@Test
	void testRegisterAndRouteMultipleWebVttTracks() {
		classUnderTest.registerWebVttTrack(2, "de", "DVB-TTML");
		classUnderTest.registerWebVttTrack(3, "fr", "Malentendants");
		assertThat(hlsMuxer.tracks).containsExactly(
				new WebVttTrack(2, "de", "DVB-TTML"), new WebVttTrack(3, "fr", "Malentendants"));

		WebVttCue cue = new WebVttCue(1000, 2000, "Hello");
		classUnderTest.writeWebVttCue(2, cue);
		assertThat(hlsMuxer.cue).isEqualTo(cue);
		assertThat(hlsMuxer.cueInputStreamIndex).isEqualTo(2);

		classUnderTest.writeWebVttCue(3, cue);
		assertThat(hlsMuxer.cueCallCount).isEqualTo(2);

		classUnderTest.writeWebVttCue(4, cue);
		assertThat(hlsMuxer.cueCallCount).isEqualTo(2);
	}

	private static class TestHLSMuxer extends HLSMuxer {

		private final java.util.List<WebVttTrack> tracks = new java.util.ArrayList<>();
		private WebVttCue cue;
		private int cueInputStreamIndex = -1;
		private int cueCallCount;

		TestHLSMuxer() {
			super(null, null, "streams", 0, null, false);
		}

		@Override
		public void addWebVttTrack(WebVttTrack track) {
			tracks.add(track);
		}

		@Override
		public synchronized void writeWebVttCue(int inputStreamIndex, WebVttCue cue) {
			this.cueInputStreamIndex = inputStreamIndex;
			this.cue = cue;
			cueCallCount++;
		}
	}
}
