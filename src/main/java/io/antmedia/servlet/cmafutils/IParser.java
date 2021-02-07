package io.antmedia.servlet.cmafutils;

public interface IParser {
	
	public void parse(byte[] data, int offset, int length);

}
