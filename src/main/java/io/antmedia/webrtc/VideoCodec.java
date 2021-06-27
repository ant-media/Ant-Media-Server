package io.antmedia.webrtc;

public enum VideoCodec {
	NOVIDEO("NoCodec"),
	VP8("VP8"),
	H264("H264"),
	PNG("PNG"),
	H265("H265");
	
	private String name;
	VideoCodec(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
