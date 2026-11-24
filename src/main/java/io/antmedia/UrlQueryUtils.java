package io.antmedia;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Utilities for reading and editing URL query strings.
 */
public final class UrlQueryUtils {

	private UrlQueryUtils() {
	}

	/**
	 * @return raw values of every {@code key} param in the query, in order. A param without '=' has "" as value
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

	public static List<String> getDecodedParamValues(String query, String key) {
		List<String> values = new ArrayList<>();
		if (query == null) {
			return values;
		}
		int fragment = query.indexOf('#');
		String queryString = fragment < 0 ? query : query.substring(0, fragment);
		// Match URITemplateProxyServlet, including its semicolon separator.
		for (NameValuePair param : URLEncodedUtils.parse(URI.create("?" + queryString), StandardCharsets.UTF_8)) {
			if (key.equals(param.getName())) {
				values.add(param.getValue());
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
