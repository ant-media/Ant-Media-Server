package io.antmedia;

public class Version {

	public static void main(String[] args) {
		System.out.println("Version: " + Version.class.getPackage().getImplementationVersion());
	}
}
