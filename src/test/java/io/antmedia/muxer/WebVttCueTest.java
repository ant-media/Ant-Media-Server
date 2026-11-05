package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class WebVttCueTest extends UnitTestBase<WebVttCue> {

	@Test
	void testValidCue() {
		classUnderTest = new WebVttCue(1200, 3400, "Hello");

		assertThat(classUnderTest.startTimeMs()).isEqualTo(1200);
		assertThat(classUnderTest.endTimeMs()).isEqualTo(3400);
		assertThat(classUnderTest.text()).isEqualTo("Hello");
	}

	@Test
	void testRejectInvalidCue() {
		assertThatIllegalArgumentException().isThrownBy(() -> new WebVttCue(-1, 1000, "Hello"));
		assertThatIllegalArgumentException().isThrownBy(() -> new WebVttCue(1000, 1000, "Hello"));
		assertThatIllegalArgumentException().isThrownBy(() -> new WebVttCue(1000, 2000, " "));
	}
}
