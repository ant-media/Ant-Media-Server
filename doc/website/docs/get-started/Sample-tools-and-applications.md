---
sidebar_position: 2
title: Sample tools and Apps
---

Ant Media Server comes with some sample applications to demonstrate different use-cases for ease of use. You can review and also execute these samples to see Ant Media Server in action.

Default applications in Enterprise Edition are `LiveApp` and `WebRTCAppEE` and in Community Edition are `LiveApp` and `WebRTCApp`. Note that those are just app names and naming scheme doesn't denote or mean anything, hence both sample app names have exactly the same features.

## 1. WebRTC video publishing

Ant Media Server can ingest WebRTC video of users from web browsers. 

After installing the server and SSL, you can publish video using webRTC from this file 

```
/usr/local/antmedia/webapps/LiveApp/index.html
``` 

at this URL 

```
https://domain-name.com:5443/LiveApp/index.html
``` 

For installation, please check out [quick start installation guide](/).



![](@site/static/img/138691164-04d3f2c7-f37a-4acd-a590-2af05477e256.png)

#### WebRTC video publishing without SSL installed

If you are running the server on the localhost, there won't be a problem. However, for security reasons web browsers does not allow reaching out to media devices without the SSL. In order to bypass that in the Chrome for development, you can add your server's IP to the following Chrome property and it will not ask for SSL.

`chrome://flags/#unsafely-treat-insecure-origin-as-secure`

## 2. WebRTC video playing

You can play the streams published to the server with WebRTC video player. WebRTC video playback is available in **Enterprise Edition**.

You can navigate to the following URL to playback your webRTC stream. 
```
https://domain-name.com:5443/LiveApp/player.html
```

File is located at `/usr/local/antmedia/webapps/LiveApp/player.html`

![](@site/static/img/138707768-2fd87a70-9ccd-4c66-9c96-fa33ef5fe781.png)

## 3. WebRTC audio publishing

WebRTC Audio publishing is available in **Enterprise Edition**

```
https://domain-name.com:5443/LiveApp/audio_publish.html
```

File is located at `/usr/local/antmedia/webapps/LiveApp/audio_publish.html`

![](@site/static/img/138696358-9d967d80-343b-4717-a587-1e934e63d5e3.png )

## 4. WebRTC audio playing

WebRTC Audio playing is available in **Enterprise Edition**

 File is located at `/usr/local/antmedia/webapps/LiveApp/audio_player.html`

![](@site/static/img/138696730-9acb0de6-0c8d-42c4-9303-fd7f8a388aaf.png )

## 5. Conference calls

WebRTC Conference is available in **Enterprise Edition**. This feature allows one or more publishers to send video streams to a group of audiences. Latency in the conference is around 0.5 seconds.

File is located at `/usr/local/antmedia/webapps/LiveApp/conference.html`.

For technical and detailed information about conference alls, please follow the [link](https://portal.document360.io/v1/docs/webrtc-conference-call).

![](@site/static/img/image-1645105628540.png )

## 6. Peer to peer live streaming

WebRTC live streaming (peer to peer) is also available in **Enterprise Edition**.

```
https://domain-name.com:5443/LiveApp/peer.html
```

File is located at `/usr/local/antmedia/webapps/LiveApp/peer.html`

If you would like to dive in further regarding the technology behind live streaming, you can also analyze the WebRTC video/audio publish example. For technical details, please follow the [link](https://portal.document360.io/v1/docs/webrtc-peer-to-peer-communication).

## 7. WebRTC multitrack player

WebRTC Multitrack Player is available in **Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/multitrackplayer.html`

## 8. WebRTC multipeer player

WebRTC Multitrack Player is available in **Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/multipeerplay.html`

## 9. Live publishing with whiteboard

You can broadcast live streams to Ant Media Server with a whiteboard, where the whiteboard is synchronized with all players. Please make sure that data channel is enabled on the server side as it's disabled by default. WebRTC whiteboard publisher is available in **Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/canvas-publish.html`

![](@site/static/img/138704308-6dccbd55-1bff-40e3-9c67-44fc23fc2b50.png )

## 10. Live playing with whiteboard

You can play live streams with a synchronized whiteboard. Please make sure that data channel is enabled on the server side as it's disabled by default. WebRTC whiteboard player is available in **Enterprise Edition**.

```
https://domain-name.com:5443/LiveApp/canvas-player.html
```

File is located at `/usr/local/antmedia/webapps/LiveApp/canvas-player.html`

## 11. WebRTC DataChannel

You can send only data through WebRTC via Ant Media Server without video or audio. It works the same way with WebRTC Publish and Play which means all data is delivered to subscribers. WebRTC DataChannel is available in **Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/datachannel.html`

![](@site/static/img/138705765-b0a913c8-25c2-4ce7-89d5-68a664694532.png )

## 12. Live streaming test tool

With this tool, you can measure E2E (End to end) bitrate, RTT, packet lost and other connection parameters that may affect the quality of the stream with test tool automatically. Live streaming test tool is available in **Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/webrtc-test-tool.html`.

![](@site/static/img/138707266-883326ee-a3b6-4e58-a265-3e2844c97ef0.png )

## 13. Live streaming with timestamp

You can broadcast WebRTC camera stream by drawing current timestamp of the frame. It's used for measuring the E2E (End to End) latency. Live streaming with timestamp feature is available in **Enterprise Edition**.

```
https://domain-name.com:5443/LiveApp/publish_with_timestamp.html
```

File is located at `/usr/local/antmedia/webapps/LiveApp/publish_with_timestamp.html

## 14. WebRTC Player With Timestamp

You can broadcast WebRTC camera stream by drawing current timestamp of the frame. It's used for measuring the E2E(End to End) latency. It has a built in function to send the frames to either Amazon Recognition or Google Vision API to get the latency measurement. For technical details, please read [e2e latency measurement documentation](https://portal.document360.io/v1/docs/measuring-end-to-end-latency). WebRTC Player with Timestamp is available in **Enterprise Edition**

File is located at `/usr/local/antmedia/webapps/LiveApp/player_with_timestamp.html

![](@site/static/img/138709249-aa18beda-a575-42d0-981a-b62758b8df10.png )

## 15. WebRTC stream merger

You can connect a conference room, get all streams & merge them into a canvas & republish to Ant Media Server. This solution is useful if you want to publish conference room to large number of audience in a single stream. Please check this [blogpost for more information](https://antmedia.io/merge-webrtc-conferences-as-a-single-video-stream/) for details. WebRTC stream merger is available **in Enterprise Edition**.

File is located at `/usr/local/antmedia/webapps/LiveApp/merge_streams.html`
