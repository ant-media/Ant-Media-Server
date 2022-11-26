package io.antmedia.plugin.api;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;

import org.bytedeco.ffmpeg.avutil.AVRational;

/*
 * This class is used to send video or audio stream informations to plugin from AMS or vice versa.
 */

public class StreamParametersInfo {
	/*
	 * Codec parameters for the video/audio stream
	 */
	private AVCodecParameters codecParameters;
	/*
	 * Time base for the pts values for the packets or frames will be sent to plugins
	 */
	private AVRational timeBase;
	/*
	 * Determines if this stream is available in the source. If it is false, that means
	 * video or audio is not available in the source stream.
	 */
	private boolean enabled = false;
	/*
	 * This shows if the stream is hosted in node on which plugin works or not.
	 * It is set true if the stream is hosted by another node in cluster mode
	 */
	private boolean hostedInOtherNode = false; 
	
	public AVCodecParameters getCodecParameters() {
		return codecParameters;
	}
	public void setCodecParameters(AVCodecParameters codecParameters) {
		this.codecParameters = codecParameters;
	}
	public AVRational getTimeBase() {
		return timeBase;
	}
	public void setTimeBase(AVRational timeBase) {
		this.timeBase = timeBase;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isHostedInOtherNode() {
		return hostedInOtherNode;
	}
	public void setHostedInOtherNode(boolean hostedInOtherNode) {
		this.hostedInOtherNode = hostedInOtherNode;
	}
}
