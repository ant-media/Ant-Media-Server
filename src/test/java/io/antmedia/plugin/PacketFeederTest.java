package io.antmedia.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.StreamParametersInfo;
import io.antmedia.test.UnitTestBase;

class PacketFeederTest extends UnitTestBase<PacketFeeder> {

	PacketFeederTest() {
		classUnderTest = new PacketFeeder("stream");
	}

	@AfterEach
	void closePacketFeeder() {
		classUnderTest.close();
	}

	@Test
	void closesNativePacketsAndListenersIdempotently() {
		int[] trailerCalls = {0};
		IPacketListener listener = new IPacketListener() {
			@Override
			public AVPacket onVideoPacket(String streamId, AVPacket packet) {
				return packet;
			}

			@Override
			public AVPacket onAudioPacket(String streamId, AVPacket packet) {
				return packet;
			}

			@Override
			public AVPacket onDataPacket(String streamId, AVPacket packet) {
				return packet;
			}

			@Override
			public void writeTrailer(String streamId) {
				trailerCalls[0]++;
			}

			@Override
			public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {
				// No-op.
			}

			@Override
			public void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo) {
				// No-op.
			}
		};
		assertThat(classUnderTest.addListener(listener)).isTrue();

		classUnderTest.close();
		classUnderTest.close();

		assertThat(classUnderTest.isClosed()).isTrue();
		assertThat(classUnderTest.addListener(listener)).isFalse();
		classUnderTest.writeTrailer();
		assertThat(trailerCalls[0]).isZero();
	}
}
