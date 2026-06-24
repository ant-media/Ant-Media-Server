package io.antmedia.test.ipcamera;

import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvif.soap.SOAP;
import io.antmedia.ipcamera.onvif.soap.OnvifDevice;
import io.antmedia.ipcamera.onvif.soap.devices.InitialDevices;
import io.antmedia.ipcamera.onvif.soap.devices.MediaDevices;
import io.antmedia.ipcamera.onvif.soap.devices.PtzDevices;
import jakarta.xml.soap.SOAPException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.onvif.ver10.schema.Capabilities;
import org.onvif.ver10.schema.DeviceCapabilities;
import org.onvif.ver10.schema.Profile;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("https://192.168.1.1:8080/test?profileIndex=1"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getIPAddress("192.168.1.1:8080?profileIndex=1"));
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

    @Test
    public void testConnectSelectsProfileByProfileIndex() throws Exception {
        OnvifDevice nvt = mock(OnvifDevice.class);
        TestableOnvifCamera onvifCamera = new TestableOnvifCamera(nvt);

        SOAP soap = mock(SOAP.class);
        when(nvt.getSoap()).thenReturn(soap);

        InitialDevices devices = mock(InitialDevices.class);
        when(nvt.getDevices()).thenReturn(devices);

        Capabilities capabilities = new Capabilities();
        capabilities.setDevice(new DeviceCapabilities());
        when(devices.getCapabilities()).thenReturn(capabilities);
        when(devices.getServices(false)).thenReturn(Collections.emptyList());

        PtzDevices ptzDevices = mock(PtzDevices.class);
        when(nvt.getPtz()).thenReturn(ptzDevices);

        List<Profile> profiles = new ArrayList<>();
        Profile p1 = new Profile();
        p1.setToken("token1");
        profiles.add(p1);
        Profile p2 = new Profile();
        p2.setToken("token2");
        profiles.add(p2);
        when(devices.getProfiles()).thenReturn(profiles);

        MediaDevices mediaDevices = mock(MediaDevices.class);
        when(nvt.getMedia()).thenReturn(mediaDevices);
        when(mediaDevices.getRTSPStreamUri("token2")).thenReturn("rtsp://profile2");

        int result = onvifCamera.connect("http://192.168.1.10:8080/onvif/device_service?profileIndex=1", "user", "pass");

        assertEquals(OnvifCamera.CONNECTION_SUCCESS, result);
        assertEquals("192.168.1.10:8080", onvifCamera.camIP);
        assertEquals("http", onvifCamera.protocol);
        assertEquals("rtsp://profile2", onvifCamera.getRTSPStreamURI());
        verify(mediaDevices).getRTSPStreamUri("token2");
    }

    private static class TestableOnvifCamera extends OnvifCamera {

        private final OnvifDevice nvt;
        private String camIP;
        private String protocol;

        TestableOnvifCamera(OnvifDevice nvt) {
            this.nvt = nvt;
        }

        @Override
        protected OnvifDevice createOnvifDevice(String camIP, String protocol, String username, String password) {
            this.camIP = camIP;
            this.protocol = protocol;
            return nvt;
        }
    }

    
    
    
}
