package io.antmedia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class StreamIdValidatorTest extends UnitTestBase<StreamIdValidator> {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"stream1", "123", "D0827C68B9CCC702", "camera-1_main.v2", "camera..main", "..hidden"})
	void shouldPreserveExistingStreamIdForms(String streamId) {
		assertThat(StreamIdValidator.isStreamIdValid(streamId)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {".", "..", "../victim", "..//..//OtherApp/streams/victim",
			"camera/../victim", "camera\\..\\victim", "camera%2Ftoken", "/tmp/victim",
			"red5.properties|x", ".*|x"})
	void shouldRejectDirectoryMarkersPathsAndRegexOperators(String streamId) {
		assertThat(StreamIdValidator.isStreamIdValid(streamId)).isFalse();
	}
}
