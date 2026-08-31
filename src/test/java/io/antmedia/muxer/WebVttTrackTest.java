package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class WebVttTrackTest extends UnitTestBase<WebVttTrack> {

	@Test
	void testDefaults() {
		classUnderTest = new WebVttTrack(2, null, " ");

		assertThat(classUnderTest.inputStreamIndex()).isEqualTo(2);
		assertThat(classUnderTest.language()).isEqualTo("und");
		assertThat(classUnderTest.name()).isEqualTo("Subtitles");
	}

	@Test
	void testRejectNegativeStreamIndex() {
		assertThatIllegalArgumentException().isThrownBy(() -> new WebVttTrack(-1, "de", "DVB-TTML"));
	}
}
