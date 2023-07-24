package io.antmedia.test.ipcamera;

import io.antmedia.ipcamera.OnvifCamera;
import junit.framework.TestCase;
import org.junit.Assert;

public class OnvifCameraTest extends TestCase {

    public void testGetURL() {
        
        OnvifCamera onvifCamera = new OnvifCamera();

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("http://192.168.1.1:8080"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("192.168.1.1:8080/test/test2"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("rtmp://192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("rtmps://192.168.1.1:8080/test"));

        Assert.assertEquals("192.168.1.1:8080", onvifCamera.getURL("http://192.168.1.1:8080/test"));

        Assert.assertEquals("https://192.168.1.1:8080", onvifCamera.getURL("https://192.168.1.1:8080/test"));
    }
}