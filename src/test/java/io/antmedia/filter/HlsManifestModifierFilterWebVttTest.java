package io.antmedia.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class HlsManifestModifierFilterWebVttTest extends UnitTestBase<HlsManifestModifierFilter> {

	@Test
	void testSecurityParametersAreAddedToWebVttSegmentUrls() {
		classUnderTest = new HlsManifestModifierFilter();
		String playlist = """
				#EXTM3U
				#EXT-X-TARGETDURATION:2
				#EXTINF:2.0,
				stream_subtitles_3_10.vtt
				#EXTINF:2.0,
				stream_subtitles_3_11.vtt?existing=value
				""";

		String modified = classUnderTest.modifyManifestFileContent(playlist, "testToken",
				"testSubscriber", "testCode", HlsManifestModifierFilter.SEGMENT_FILE_REGEX);

		assertThat(modified).isEqualTo("""
				#EXTM3U
				#EXT-X-TARGETDURATION:2
				#EXTINF:2.0,
				stream_subtitles_3_10.vtt?subscriberCode=testCode&subscriberId=testSubscriber&token=testToken
				#EXTINF:2.0,
				stream_subtitles_3_11.vtt?existing=value&subscriberCode=testCode&subscriberId=testSubscriber&token=testToken
				""");
	}
}
