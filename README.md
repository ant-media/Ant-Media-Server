Ant Media Server 
===========

Ant Media Server is an Open Source Media Server that supports:

 * Live Stream Publishing with RTMP, RTSP and WebRTC
 * Recording Live Streams (FLV, MP4 and HLS Containers)
 * Adaptive Bitrate Conversion for Live Streams (FLV, MP4, HLS) in Enterprise Edition
 * Streaming Video (FLV, F4V, MP4, 3GP)
 * Streaming Audio (MP3, F4A, M4A, AAC)
 * Shared Objects
 * Remoting
 * Protocols: RTMP, RTMPT, RTMPS, RTMPE, RTSP, HTTP
 
Ant Media Server is based on Red5

### Features comes with Ant Media Server

* **HLS and MP4 Muxing**: Live broadcasts can be played with HLS even in web browsers that do not support flash (Android and iOS). In addition recorded live broadcasts can be played in any web browsers without loading any add-ons with MP4 Recording. Recording operations can be easily enabled or disabled with the commands shown below which is located in the conf/red5.properties file:

   `broadcastream.auto.record.mp4=true`

   `broadcastream.auto.record.hls=true`

* **RTSP**: Publish and Play live and vod streams with RTSP (UDP). Default RTSP port number 5554.
* **WebRTC**: Publish and Play live and vod streams with WebRTC
* **Adaptive Bitrate Conversion**: Lower down the resolution of the live streams on the play to let stream play seamlessly in any internet speed.

## Releases 

### Ant Media Server 1.0RC Release (June 5, 2017)
[ZIP](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v.1.0RC/ant-media-server-1.0RC.zip)
### Red5+ 1.0.1 Release (27 March 2017)
[Tarball &amp; ZIP](https://github.com/ant-media/red5-plus-server/releases/tag/v1.0.1_red5_plus)


## Build from Source

To build the jars, execute the following on the command line:
```sh
mvn -Dmaven.test.skip=true install
```
This will create the jars in the "target" directory of the workspace; this will also skip the unit tests.

To package everything up in an assembly (tarball/zip):
```sh
mvn -Dmaven.test.skip=true clean package -P assemble
```
To build a milestone tarball:
```sh
mvn -Dmilestone.version=1.0.7-M1 clean package -Pmilestone
```

## Eclipse

1. Create the eclipse project files, execute this within ant-media-server directory.
```sh
mvn eclipse:eclipse
```
2. Import the project into Eclipse.
3. Access the right-click menu and select "Configure" and then "Convert to Maven Project".
4. Now the project will build automatically, if you have the maven plugin installed.



## Additional features supported via plugin
 
 * [WebSocket (ws and wss)](https://github.com/Red5/red5-websocket)
 * [RTSP (From Axis-type cameras)](https://github.com/Red5/red5-rtsp-restreamer)
 * [HLS](https://github.com/Red5/red5-hls-plugin)
 


