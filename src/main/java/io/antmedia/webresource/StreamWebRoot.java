package io.antmedia.webresource;

import org.apache.catalina.WebResource;
import org.apache.catalina.webresources.StandardRoot;

public class StreamWebRoot extends StandardRoot {

	
	 @Override
	 public WebResource getResource(String path) {
		 if (path.endsWith(".m3u8") || path.endsWith(".ts")) {
			 return getResourceInternal(path, true);
		 }
		 else {
			 return super.getResource(path);
		 }
		 
	 }
}
