package io.antmedia.test.eRTMP;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.eRTMP.HEVCDecoderConfigurationParser;

public class HEVCDecoderConfigurationParserTest {

	
	private static final byte[] HEVC_DECODER_CONFIGURATION = new byte[]{1, 1, 96, 0, 0, 0, -112, 0, 0, 0, 0, 0, 123, -16, 0, -4, -3, -8, -8, 0, 0, 15, 3, 32, 0, 1, 0, 24, 64, 1, 12, 1, -1, -1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0, 123, -110, -128, -112, 33, 0, 1, 0, 45, 66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0, 123, -96, 3, -64, -128, 16, -27, -106, 74, -110, 76, -82, 106, 2, 2, 3, -62, 0, 0, 3, 0, 2, 0, 0, 3, 0, 120, 16, 34, 0, 1, 0, 8, 68, 1, -63, 114, -76, 98, 64, 0};

	@Test
	public void testParser() {
		HEVCDecoderConfigurationParser parser = new HEVCDecoderConfigurationParser(HEVC_DECODER_CONFIGURATION, 0);
		
		
		assertEquals(1920, parser.getWidth());
		assertEquals(1080, parser.getHeight());
		
		
	}

}
