package io.antmedia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.Test;
import org.webrtc.VideoCodecType;

public class FFmpegUtilitiesTest {

    @Test
    public void testByteArrayToString() {
        byte[] input = new byte[] { 'h', 'e', 'l', 'l', 'o', 0, '@'};
        assertEquals("hello", FFmpegUtilities.byteArrayToString(input));
    }

    @Test
    public void testZeroLengthInput() {
        assertEquals("", FFmpegUtilities.byteArrayToString(new byte[]{}));
        assertEquals("", FFmpegUtilities.byteArrayToString(null));
    }

    @Test
    public void testNonTerminatedArray() {
        assertEquals("x", FFmpegUtilities.byteArrayToString(new byte[] { 'x' }));
    }

    @Test
    public void testAvStrErrorUsage() {
        byte[] buffer = new byte[128];
        avutil.av_strerror(-22, buffer, 128);
        assertEquals("Invalid argument", FFmpegUtilities.byteArrayToString(buffer));
    }
    
    @Test
    public void testVideoCodecType() {
        VideoCodecType type = VideoCodecType.AV1;
        assertEquals(VideoCodecType.AV1.name(), type.name());
    }
    
}