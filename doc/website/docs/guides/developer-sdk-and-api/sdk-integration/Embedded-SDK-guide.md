# Embedded SDK guide

Ant Media server can stream from IP cameras, and this solution is for IP camera manufacturers. If you just want to use peer to peer connection between an IP camera and a web browser, Ant Media can also provide a solution for this case as well.

1.  [WebRTC IP camera and browser (P2P)](#1-webrtc-ip-camera-and-browser-p2p)
2.  [How to use embedded WebRTC SDK](#2-how-to-use-embedded-webrtc-sdk)

Embedded WebRTC SDK can run on both ARM and x86 processors. IP cameras generally have a built-in RTSP URL. You can embed Native WebRTC SDK into your IP camera and the SDK fetches the RTSP stream and then forward the RTSP stream to the other peer via WebRTC.

Native SDK does not transcode the RTSP stream. It just fetches the stream and forwards it to the WebRTC stack. Hence, it does not need a lot of CPU resources. Also, there is very minimal latency.

![](@site/static/img/image-1645190541856.png)

### 1\. WebRTC IP camera and browser (P2P)

Signaling of WebRTC SDK is compatible with Ant Media Server. You need to use Ant Media Server as a signaling server in order to have peer to peer connection between a web browser and your IP camera.

### 2\. How to use the embedded SDK

There is only one method you need to call in your application. Here is the sample code.

    int main(int argc, char* argv[]) {
    	rtc::InitializeSSL();
    	signalingThread = rtc::Thread::Current();
    	startWebSocket(“ws://Your_Ant_Media_Server_Address:5080/WebRTCAppEE/websocket”,
    	“rtsp://127.0.0.1:6554/stream1”, “stream1”);
    	rtc::CleanupSSL();
    	return 0;
    }

 As you can see, the critical method is **startWebSocket** method, which has three parameters

*   The first parameter is the WebSocket URL of the Ant Media Server
*   The second parameter is the internal RTSP URL of the IP camera
*   The third parameter is the stream id that will be published. After you run this application, visit  http://Your\_Ant\_Media\_Server\_Address:5080/WebRTCAppEE/peer.html and write the stream id that you’ve used in your code (stream1 in the sample code), and click the JoinButton. Now you can watch the IP camera stream on your browser.