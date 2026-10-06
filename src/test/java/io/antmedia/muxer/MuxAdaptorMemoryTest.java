package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

class MuxAdaptorMemoryTest extends UnitTestBase<MuxAdaptor> {

	MuxAdaptorMemoryTest() {
		classUnderTest = new MuxAdaptor(null);
	}

	@AfterEach
	void releaseBuffer() {
		classUnderTest.releaseReusableRtmpPacketBuffer();
	}

	@Test
	void reusesAndGrowsRtmpPacketBuffer() {
		ByteBuffer initial = classUnderTest.getReusableRtmpVideoPacketBuffer(1024);
		ByteBuffer reused = classUnderTest.getReusableRtmpVideoPacketBuffer(2048);

		assertThat(reused).isSameAs(initial);
		assertThat(reused.capacity()).isGreaterThanOrEqualTo(64 * 1024);
		assertThat(reused.limit()).isEqualTo(2048);

		ByteBuffer grown = classUnderTest.getReusableRtmpVideoPacketBuffer(128 * 1024);
		assertThat(grown).isNotSameAs(initial);
		assertThat(grown.capacity()).isGreaterThanOrEqualTo(128 * 1024);
	}

	@Test
	void keepsAudioAndVideoPacketBuffersSeparate() {
		ByteBuffer audio = classUnderTest.getReusableRtmpAudioPacketBuffer(1022);
		ByteBuffer video = classUnderTest.getReusableRtmpVideoPacketBuffer(1019);

		assertThat(audio).isNotSameAs(video);
		assertThat(audio.limit()).isEqualTo(1022);
		assertThat(video.limit()).isEqualTo(1019);
	}

	@Test
	void rejectsNegativeBufferCapacity() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> classUnderTest.getReusableRtmpVideoPacketBuffer(-1));
	}
}
