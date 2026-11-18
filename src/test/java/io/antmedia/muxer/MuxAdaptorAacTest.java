package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.FFmpegUtilities;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class MuxAdaptorAacTest extends UnitTestBase<MuxAdaptor> {

    @Test
    void parsesAacProfilesIntoNativeCodecParameters() {
        // MPEG-4 AudioSpecificConfig object types MAIN, LC, SSR, LTP map to FFmpeg profiles 0, 1, 2, 3.
        for (int[] profile : new int[][] {{1, 0}, {2, 1}, {3, 2}, {4, 3}}) {
            assertProfile(profile[0], profile[1]);
        }
    }

    private void assertProfile(int objectType, int expectedProfile) {
        classUnderTest = new MuxAdaptor(null);
        classUnderTest.setAudioDataConf(new byte[] {(byte) ((objectType << 3) | 2), 0x10});
        var parameters = classUnderTest.getAudioCodecParameters();
        try {
            assertThat(FFmpegUtilities.isNull(parameters)).isFalse();
            assertThat(parameters.codec_id()).isEqualTo(avcodec.AV_CODEC_ID_AAC);
            assertThat(parameters.profile()).isEqualTo(expectedProfile);
            assertThat(parameters.sample_rate()).isEqualTo(44100);
            assertThat(parameters.ch_layout().nb_channels()).isEqualTo(2);
            assertThat(parameters.extradata_size()).isEqualTo(2);
        } finally {
            if (!FFmpegUtilities.isNull(parameters)) {
                avutil.av_free(parameters.extradata());
                parameters.close();
            }
        }
    }
}
