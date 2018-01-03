package io.antmedia.ipcamera.onvifdiscovery;

import java.net.URL;
import java.util.Comparator;

public class URLComparator implements Comparator<URL> {
	public int compare(URL o1, URL o2) {
		return o1.toString().compareTo(o2.toString());
	}
}