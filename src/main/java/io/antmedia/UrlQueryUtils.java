package io.antmedia;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Works on a url query, the part after '?', as raw text. Nothing is decoded or re-encoded.
 */
public final class UrlQueryUtils {

	private UrlQueryUtils() {
	}

	/**
	 * @return values of every {@code key} param in the query, in order. A param without '=' has "" as value
	 */
	public static List<String> getParamValues(String query, String key) {
		List<String> values = new ArrayList<>();
		for (String param : query.split("&")) {
			String[] keyValue = param.split("=", 2);
			if (key.equals(keyValue[0])) {
				values.add(keyValue.length > 1 ? keyValue[1] : "");
			}
		}
		return values;
	}

	/**
	 * @return the query without any {@code key} param, the other params stay exactly as written and in order
	 */
	public static String removeParam(String query, String key) {
		StringJoiner newQuery = new StringJoiner("&");
		for (String param : query.split("&")) {
			if (!key.equals(param.split("=", 2)[0])) {
				newQuery.add(param);
			}
		}
		return newQuery.toString();
	}
}
