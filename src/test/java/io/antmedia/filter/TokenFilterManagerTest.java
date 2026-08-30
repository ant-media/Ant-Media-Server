package io.antmedia.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.antmedia.test.UnitTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fast")
class TokenFilterManagerTest extends UnitTestBase<TokenFilterManager> {

	@Test
	void doesNotInterpretNdiStreamsEndpointAsMediaStreamPath() {
		assertThat(TokenFilterManager.getStreamId("/live/rest/v2/broadcasts/ndi-streams")).isNull();
		assertThat(TokenFilterManager.getStreamId("/live/streams/sample.m3u8")).isEqualTo("sample");
	}
}
