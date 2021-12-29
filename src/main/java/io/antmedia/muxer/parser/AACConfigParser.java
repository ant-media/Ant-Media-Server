package io.antmedia.muxer.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AACConfigParser extends Parser {
	
	protected static Logger logger = LoggerFactory.getLogger(AACConfigParser.class);
	
	
	/*
	    0: 96000 Hz
		1: 88200 Hz
		2: 64000 Hz
		3: 48000 Hz
		4: 44100 Hz
		5: 32000 Hz
		6: 24000 Hz
		7: 22050 Hz
		8: 16000 Hz
		9: 12000 Hz
		10: 11025 Hz
		11: 8000 Hz
		12: 7350 Hz
		13: Reserved
		14: Reserved
		15: frequency is written explictly
	 */
	protected static final int SAMPLE_RATE_96000 = 0; 
	protected static final int SAMPLE_RATE_88200 = 1; 
	protected static final int SAMPLE_RATE_64000 = 2; 
	protected static final int SAMPLE_RATE_48000 = 3; 
	protected static final int SAMPLE_RATE_44100 = 4; 
	protected static final int SAMPLE_RATE_32000 = 5; 
	protected static final int SAMPLE_RATE_24000 = 6; 
	protected static final int SAMPLE_RATE_22050 = 7; 
	protected static final int SAMPLE_RATE_16000 = 8; 
	protected static final int SAMPLE_RATE_12000 = 9; 
	protected static final int SAMPLE_RATE_11025 = 10; 
	protected static final int SAMPLE_RATE_8000 = 11; 
	protected static final int SAMPLE_RATE_7350 = 12; 
	
	
	private int sampleRate;
	
	/*
		0: Defined in AOT Specifc Config
		1: 1 channel: front-center
		2: 2 channels: front-left, front-right
		3: 3 channels: front-center, front-left, front-right
		4: 4 channels: front-center, front-left, front-right, back-center
		5: 5 channels: front-center, front-left, front-right, back-left, back-right
		6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
		7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
		8-15: Reserved
	*/
	private int channelCount;
	
	
	/*	
		0: Null
		1: AAC Main
		2: AAC LC (Low Complexity)
		3: AAC SSR (Scalable Sample Rate)
		4: AAC LTP (Long Term Prediction)
		5: SBR (Spectral Band Replication)
		6: AAC Scalable
		7: TwinVQ
		8: CELP (Code Excited Linear Prediction)
		9: HXVC (Harmonic Vector eXcitation Coding)
		10: Reserved
		11: Reserved
		12: TTSI (Text-To-Speech Interface)
		13: Main Synthesis
		14: Wavetable Synthesis
		15: General MIDI
		16: Algorithmic Synthesis and Audio Effects
		17: ER (Error Resilient) AAC LC
		18: Reserved
		19: ER AAC LTP
		20: ER AAC Scalable
		21: ER TwinVQ
		22: ER BSAC (Bit-Sliced Arithmetic Coding)
		23: ER AAC LD (Low Delay)
		24: ER CELP
		25: ER HVXC
		26: ER HILN (Harmonic and Individual Lines plus Noise)
		27: ER Parametric
		28: SSC (SinuSoidal Coding)
		29: PS (Parametric Stereo)
		30: MPEG Surround
		31: (Escape value)
		32: Layer-1
		33: Layer-2
		34: Layer-3
		35: DST (Direct Stream Transfer)
		36: ALS (Audio Lossless)
		37: SLS (Scalable LosslesS)
		38: SLS non-core
		39: ER AAC ELD (Enhanced Low Delay)
		40: SMR (Symbolic Music Representation) Simple
		41: SMR Main
		42: USAC (Unified Speech and Audio Coding) (no SBR)
		43: SAOC (Spatial Audio Object Coding)
		44: LD MPEG Surround
		45: USAC
	*/
	public enum AudioObjectTypes {
		NULL(0),
		AAC_MAIN(1),
		AAC_LC(2),
		AAC_SSR(3),
		AAC_LTP(4),
		SBR(5),
		AAC_SCALABLE(6);
		
		public final  int value;
		AudioObjectTypes(int value) {
			this.value = value;
		}
	}
	
	private AudioObjectTypes objectType;

	private int frameSize;

	public AACConfigParser(byte[] data, int offset) 
	{
		super(data, offset);
	}

	protected void parse() {
		/**
		5 bits: object type
		if (object type == 31)
		    6 bits + 32: object type
		4 bits: frequency index
		if (frequency index == 15)
		    24 bits: frequency
		4 bits: channel configuration
		var bits: AOT Specific Config
		*/
		
		
		int objectTypeIndex = readBits(5);
		
		if (objectTypeIndex == AudioObjectTypes.NULL.value) {
			objectType = AudioObjectTypes.NULL;
		}
		else if (objectTypeIndex == AudioObjectTypes.AAC_MAIN.value) {
			objectType = AudioObjectTypes.AAC_MAIN;
		}
		else if (objectTypeIndex == AudioObjectTypes.AAC_LC.value) {
			objectType = AudioObjectTypes.AAC_LC;
		}
		else if (objectTypeIndex == AudioObjectTypes.AAC_SSR.value) {
			objectType = AudioObjectTypes.AAC_SSR;
		}
		else if (objectTypeIndex == AudioObjectTypes.AAC_LTP.value) {
			objectType = AudioObjectTypes.AAC_LTP;
		}
		else if (objectTypeIndex == AudioObjectTypes.SBR.value) {
			objectType = AudioObjectTypes.SBR;
		}
		else if (objectTypeIndex == AudioObjectTypes.AAC_SCALABLE.value) {
			objectType = AudioObjectTypes.AAC_SCALABLE;
		}
		else {
			logger.error("Cannot determine the AAC object type:{} ", objectTypeIndex);
			errorOccured = true;
			return;
		}
		
		if (objectType == AudioObjectTypes.NULL) {
			errorOccured = true;
			logger.error("Cannot determine the AAC object type it's null ");
			return;
		}
		
		logger.info("AAC object type:{} ", objectType);
		
		
		int sampleRateIndex = readBits(4);
		if (sampleRateIndex == SAMPLE_RATE_96000) {
			sampleRate = 96000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_88200) {
			sampleRate = 88200;
		}
		else if (sampleRateIndex == SAMPLE_RATE_64000) {
			sampleRate = 64000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_48000) {
			sampleRate = 48000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_44100) {
			sampleRate = 44100;
		}
		else if (sampleRateIndex == SAMPLE_RATE_32000) {
			sampleRate = 32000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_24000) {
			sampleRate = 24000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_22050) {
			sampleRate = 22050;
		}
		else if (sampleRateIndex == SAMPLE_RATE_16000) {
			sampleRate = 16000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_12000) {
			sampleRate = 12000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_11025) {
			sampleRate = 11025;
		}
		else if (sampleRateIndex == SAMPLE_RATE_8000) {
			sampleRate = 8000;
		}
		else if (sampleRateIndex == SAMPLE_RATE_7350) {
			sampleRate = 7350;
		}
		else {
			logger.error("Cannot determine the AAC Sample Rate:{} ", sampleRateIndex);
			errorOccured = true;
			return;
		}
				
		logger.info("AAC Sample rate:{} ", sampleRate);
		
		channelCount = readBits(4);
		
		if (channelCount == 0 || channelCount > 7) {
			logger.error("Cannot determine the channel count: {}", channelCount);
			errorOccured = true;
			return;
		}
		
		if (channelCount == 7) {
			channelCount = 8;
		}
		
		frameSize = readBit() == 0x00 ? 1024 : 960;
	}

	public AudioObjectTypes getObjectType() {
		return objectType;
	}
	
	public int getChannelCount() {
		return channelCount;
	}
	
	public int getSampleRate() {
		return sampleRate;
	}
	
	public int getFrameSize() {
		return frameSize;
	}

}
