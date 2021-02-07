package io.antmedia.webresource;

import org.apache.catalina.WebResource;
import org.apache.catalina.webresources.StandardRoot;

public class StreamWebRoot extends StandardRoot {

	 boolean streamingResource = false;
	
	 @Override
	 public WebResource getResource(String path) {
		 streamingResource = false;
		 if (path.endsWith(".m3u8") || path.endsWith(".ts") || path.endsWith(".mpd") || path.endsWith(".m4s") || (path.endsWith(".png") && path.contains("/previews/"))) {
			 streamingResource = true;
			 return getResourceInternal(path, true);
		 }
		 else {
			 return getResourceDefault(path);
		 }
		 
	 }
	 
	 
	 public WebResource getResourceDefault(String path) {
		 return super.getResource(path);
	 }
	 
	 public boolean isStreamingResource() {
		return streamingResource;
	}
}
