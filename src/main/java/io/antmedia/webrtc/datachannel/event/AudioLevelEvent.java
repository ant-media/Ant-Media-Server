package io.antmedia.webrtc.datachannel.event;

public class AudioLevelEvent extends ControlEvent{

	/**
	 * 0 is the max audio level and 127 is the min audio level
	 */
	private int audioLevel;
	
	public AudioLevelEvent(String streamId) {
		super(streamId);
	}

	/**
	 * @return the audioLevel
	 */
	public int getAudioLevel() {
		return audioLevel;
	}

	/**
	 * @param audioLevel the audioLevel to set
	 */
	public void setAudioLevel(int audioLevel) {
		this.audioLevel = audioLevel;
	}
}
