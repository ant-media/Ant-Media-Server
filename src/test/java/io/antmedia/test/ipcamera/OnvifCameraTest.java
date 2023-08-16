package io.antmedia.test.ipcamera;

import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvif.soap.OnvifDevice;
import junit.framework.TestCase;
import org.junit.Assert;

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
    
    
    
}