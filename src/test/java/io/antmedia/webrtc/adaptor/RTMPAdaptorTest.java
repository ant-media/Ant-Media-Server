package io.antmedia.webrtc.adaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class RTMPAdaptorTest extends UnitTestBase<RTMPAdaptor> {

    @TempDir
    Path directory;

    @Test
    void waitsForAudioFormatBeforeStartingAudioOnlyRecorder() {
        classUnderTest = spy(new RTMPAdaptor("audio.flv", null, 0));
        classUnderTest.setEnableVideo(false);
        FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);
        doReturn(recorder).when(classUnderTest).getNewRecorder("audio.flv", 0, 0, "flv");

        classUnderTest.encodeAudio();
        verify(classUnderTest, never()).getNewRecorder("audio.flv", 0, 0, "flv");
        assertThat(classUnderTest.recorder).isNull();

        RTMPAdaptor.AudioFrame frame = new RTMPAdaptor.AudioFrame(ByteBuffer.allocateDirect(1920), 2, 48000);
        classUnderTest.getAudioFrameQueue().offer(frame);
        classUnderTest.encodeAudio();
        verify(classUnderTest).getNewRecorder("audio.flv", 0, 0, "flv");
        assertThat(classUnderTest.recorder).isSameAs(recorder);

        classUnderTest.encodeAudio();
        verify(classUnderTest).recordSamples(frame);
        assertThat(classUnderTest.getAudioFrameQueue()).isEmpty();
    }

    @Test
    void startsRecorderWithValidChannelsBeforeAndAfterAudioTrackInitialization() throws Exception {
        for (int channels : new int[] {-1, 0, 2}) {
            classUnderTest = new RTMPAdaptor(directory.resolve("audio-" + channels + ".flv").toString(), null, 0);
            if (channels >= 0) {
                WebRtcAudioTrack track = mock(WebRtcAudioTrack.class);
                when(track.getChannels()).thenReturn(channels);
                classUnderTest.setWebRtcAudioTrack(track);
            }
            FFmpegFrameRecorder recorder = classUnderTest.getNewRecorder(
                    directory.resolve("audio-" + channels + ".flv").toString(), 0, 0, "flv");
            try {
                assertThat(recorder.getAudioChannels()).isEqualTo(channels > 0 ? channels : 1);
            } finally {
                try {
                    recorder.stop();
                } finally {
                    recorder.release();
                }
            }
        }
    }
}
