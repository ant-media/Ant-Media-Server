package io.antmedia;

public enum RecordType {
	MP4("mp4"),
	WEBM("webm");
	
	private String name;
	RecordType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
