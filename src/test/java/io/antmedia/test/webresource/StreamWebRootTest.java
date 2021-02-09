package io.antmedia.test.webresource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.webresource.StreamWebRoot;

public class StreamWebRootTest {
	
	@Test
	public void testGetResource() {
		
		StreamWebRoot webroot = Mockito.spy(new StreamWebRoot());
		
		//Mockito.doReturn(null).when(webroot).getResourceInternal(Mockito.anyString());
		
		webroot.getResource("test.mpd");
		assertTrue(webroot.isStreamingResource());
		
		webroot.getResource("test.m3u8");
		assertTrue(webroot.isStreamingResource());
		
		webroot.getResource("test.ts");
		assertTrue(webroot.isStreamingResource());
		
		webroot.getResource("test.m4s");
		assertTrue(webroot.isStreamingResource());
		
		
		webroot.getResource("/previews/test.png");
		assertTrue(webroot.isStreamingResource());
		
		Mockito.doReturn(null).when(webroot).getResourceDefault(Mockito.anyString());
		
		webroot.getResource("/anydir/test.png");
		assertFalse(webroot.isStreamingResource());
		
		
		webroot.getResource("/previews/test.html");
		assertFalse(webroot.isStreamingResource());
		
		webroot.getResource("test.html");
		assertFalse(webroot.isStreamingResource());
		
		
	}

}
