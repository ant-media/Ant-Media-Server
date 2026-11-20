package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.StreamCodecInfo;

import io.antmedia.eRTMP.HEVCVideoEnhancedRTMP;
import io.antmedia.test.eRTMP.HEVCDecoderConfigurationParserTest;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class MuxAdaptorVideoTest extends UnitTestBase<MuxAdaptor> {

    @Test
    void leavesUnspecifiedColorMetadataForTheDecoderToDetermine() {
        classUnderTest = new MuxAdaptor(null);
        classUnderTest.setEnableVideo(true);
        HEVCVideoEnhancedRTMP codec = new HEVCVideoEnhancedRTMP();
        byte[] configuration = HEVCDecoderConfigurationParserTest.HEVC_DECODER_CONFIGURATION;
        IoBuffer header = IoBuffer.allocate(5 + configuration.length);
        header.put(new byte[] {(byte) 0x80, 'h', 'v', 'c', '1'}).put(configuration).flip();
        codec.addData(header);
        StreamCodecInfo info = new StreamCodecInfo();
        info.setVideoCodec(codec);
        classUnderTest.getVideoDataConf(info);
        var parameters = classUnderTest.getVideoCodecParameters();
        try {
            assertThat(parameters.width()).isEqualTo(1920);
            assertThat(parameters.height()).isEqualTo(1080);
            assertThat(parameters.color_space()).isEqualTo(avutil.AVCOL_SPC_UNSPECIFIED);
            assertThat(parameters.color_primaries()).isEqualTo(avutil.AVCOL_PRI_UNSPECIFIED);
            assertThat(parameters.color_trc()).isEqualTo(avutil.AVCOL_TRC_UNSPECIFIED);
            var context = avcodec.avcodec_alloc_context3(null);
            try {
                assertThat(avcodec.avcodec_parameters_to_context(context, parameters)).isZero();
                assertThat(context.colorspace()).isEqualTo(avutil.AVCOL_SPC_UNSPECIFIED);
            } finally {
                avcodec.avcodec_free_context(context);
            }
        } finally {
            avutil.av_free(parameters.extradata());
            parameters.close();
        }
    }
}
