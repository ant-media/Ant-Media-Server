package io.antmedia.recorder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class FFmpegFrameRecorderTest extends UnitTestBase<FFmpegFrameRecorder> {

    @TempDir
    Path directory;

    @Test
    void recordsPcmAudioWithoutAFixedCodecFrameSize() throws Exception {
        Path output = directory.resolve("audio.wav");
        classUnderTest = new FFmpegFrameRecorder(output.toString(), 0, 0, 1);
        classUnderTest.setFormat("wav");
        classUnderTest.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
        classUnderTest.setSampleRate(44100);
        try {
            classUnderTest.start();
            short[] samples = new short[8192];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) (10000 * Math.sin(i * 0.05));
            }
            classUnderTest.recordSamples(ShortBuffer.wrap(samples));
            classUnderTest.recordSamples((Buffer[]) null);
            classUnderTest.stop();
        } finally {
            classUnderTest.release();
        }

        assertRecordedStream(output, avcodec.AV_CODEC_ID_PCM_S16LE, false);
    }

    @Test
    void recordsH264WithConstrainedBaselineProfile() throws Exception {
        Path output = directory.resolve("video.mp4");
        classUnderTest = new FFmpegFrameRecorder(output.toString(), 64, 64, 0);
        classUnderTest.setFormat("mp4");
        classUnderTest.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        classUnderTest.setVideoCodecName("libopenh264");
        classUnderTest.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        try {
            classUnderTest.start();
            ByteBuffer image = ByteBuffer.allocateDirect(64 * 64 * 3 / 2);
            for (int i = 0; i < image.capacity(); i++) {
                image.put(i, (byte) (i < 64 * 64 ? 64 : 128));
            }
            for (int i = 0; i < 3; i++) {
                classUnderTest.recordImage(64, 64, Frame.DEPTH_UBYTE, 1,
                        new int[] {64, 32, 32}, avutil.AV_PIX_FMT_YUV420P, image);
            }
            classUnderTest.recordImage(64, 64, Frame.DEPTH_UBYTE, 1,
                    new int[] {64, 32, 32}, avutil.AV_PIX_FMT_YUV420P, (Buffer[]) null);
            classUnderTest.stop();
        } finally {
            classUnderTest.release();
        }

        assertRecordedStream(output, avcodec.AV_CODEC_ID_H264, true);
    }

    private void assertRecordedStream(Path output, int codecId, boolean video) {
        AVFormatContext context = avformat.avformat_alloc_context();
        try {
            assertThat(avformat.avformat_open_input(context, output.toString(), null, (AVDictionary) null))
                    .isGreaterThanOrEqualTo(0);
            assertThat(avformat.avformat_find_stream_info(context, (AVDictionary) null)).isGreaterThanOrEqualTo(0);
            assertThat(context.nb_streams()).isEqualTo(1);
            var parameters = context.streams(0).codecpar();
            assertThat(parameters.codec_id()).isEqualTo(codecId);
            if (video) {
                assertThat(parameters.width()).isEqualTo(64);
                assertThat(parameters.height()).isEqualTo(64);
                assertThat(parameters.profile()).isEqualTo(avcodec.AV_PROFILE_H264_CONSTRAINED_BASELINE);
            } else {
                assertThat(parameters.sample_rate()).isEqualTo(44100);
                assertThat(parameters.ch_layout().nb_channels()).isEqualTo(1);
            }
            AVPacket packet = avcodec.av_packet_alloc();
            try {
                assertThat(avformat.av_read_frame(context, packet)).isGreaterThanOrEqualTo(0);
                assertThat(packet.size()).isPositive();
            } finally {
                avcodec.av_packet_free(packet);
            }
        } finally {
            avformat.avformat_close_input(context);
        }
    }
}
