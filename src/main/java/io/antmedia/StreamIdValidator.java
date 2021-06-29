package io.antmedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamIdValidator {
	private static Pattern namePattern = Pattern.compile("[^a-z0-9-_]", Pattern.CASE_INSENSITIVE);
	
	private StreamIdValidator() {
		
	}
	public static boolean isStreamIdValid(String id) {
		if (id != null) {
			Matcher m = namePattern.matcher(id);
			return !m.find();
		}
		//return true if name is null, it means that stream id will be generated
		return true;
	}
}
