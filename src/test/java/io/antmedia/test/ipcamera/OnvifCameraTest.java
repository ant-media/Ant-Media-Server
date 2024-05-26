package io.antmedia.test.ipcamera;

import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvif.soap.OnvifDevice;
import io.antmedia.ipcamera.onvif.soap.devices.InitialDevices;
import io.antmedia.ipcamera.onvif.soap.devices.MediaDevices;
import jakarta.xml.soap.SOAPException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.onvif.ver10.schema.Profile;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;


public class OnvifCameraTest extends TestCase {

    public void testGetIPAddress() {
        
        OnvifCamera onvifCamera = new OnvifCamera();

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("http://192.168.1.1:8080"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("192.168.1.1:8080/test/test2"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("rtmp://192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("rtmps://192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("http://192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("https://192.168.1.1:8080/test"));
    }
    
    
    public void testGetPortAddress() {
        
        OnvifCamera onvifCamera = new OnvifCamera();

        Assert.assertEquals("http", onvifCamera.getProtocol("http://192.168.1.1:8080"));

        Assert.assertEquals("https", onvifCamera.getProtocol("https://192.168.1.1:8080/test"));

        Assert.assertNull(onvifCamera.getProtocol("192.168.1.1:8080/test/test2"));

    }

    @Test
    public void testGetProfiles() throws SOAPException, ConnectException {

        OnvifCamera onvifCamera = new OnvifCamera();
        OnvifDevice nvt = mock(OnvifDevice.class);
        onvifCamera.setNvtForTest(nvt);

        InitialDevices devices = mock(InitialDevices.class);
        when(nvt.getDevices()).thenReturn(devices);

        List<Profile> profiles = new ArrayList<>();
        when(devices.getProfiles()).thenReturn(profiles);

        String token1 = "token1";
        String token2 = "token2";

        Profile p1 = new Profile();
        p1.setToken(token1);
        profiles.add(p1);

        Profile p2 = new Profile();
        p2.setToken(token2);
        profiles.add(p2);

        MediaDevices mediaDevices = mock(MediaDevices.class);
        when(nvt.getMedia()).thenReturn(mediaDevices);

        String uri1 = "uri1";
        String uri2 = "uri2";

        when(mediaDevices.getRTSPStreamUri(token1)).thenReturn(uri1);
        when(mediaDevices.getRTSPStreamUri(token2)).thenReturn(uri2);

        String[] profilesUris = onvifCamera.getProfiles();

        assertEquals(2, profilesUris.length);
        assertEquals(uri1, profilesUris[0]);
        assertEquals(uri2, profilesUris[1]);


    }

    
    
    
}