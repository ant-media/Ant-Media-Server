package org.red5.server.util;

import java.util.HashMap;

/**
 * Simple query string to map converter.
 *
 * @param <K>
 *            key
 * @param <V>
 *            value
 * 
 * @author Paul Gregoire
 */
@SuppressWarnings("serial")
public final class UrlQueryStringMap<K, V> extends HashMap<K, V> {

    public UrlQueryStringMap() {
        super();
    }

    /**
     * Parse a given query string and return an instance of this class.
     * 
     * @param queryString
     *            query string
     * @return query string items as map entries
     */
    public static UrlQueryStringMap<String, String> parse(String queryString) {
        UrlQueryStringMap<String, String> map = new UrlQueryStringMap<String, String>();
        //
        String tmp = queryString;
        //check if we start with '?' or not
        if (queryString.charAt(0) != '?') {
            tmp = queryString.split("\\?")[1];
        } else if (queryString.charAt(0) == '?') {
            tmp = queryString.substring(1);
        }
        //now break up into key/value blocks
        String[] kvs = tmp.split("&");
        //take each key/value block and break into its key value parts
        for (String kv : kvs) {
            String[] split = kv.split("=");
            map.put(split[0], split[1]);
        }
        return map;
    }

}
