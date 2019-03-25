[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)

![Quality Gate](https://sonarcloud.io/api/project_badges/quality_gate?project=io.antmedia%3Aant-media-server)

Ant Media Server 
====

Ant Media Server is an open source media server that supports:

 * Ultra Low Latency Adaptive One to Many WebRTC Live Streaming in **Enterprise Edition**
 * Adaptive Bitrate for Live Streams (WebRTC, MP4, HLS) in **Enterprise Edition**
 * SFU in One to Many WebRTC Streams in **Enterprise Edition**
 * Live Stream Publishing with RTMP and WebRTC
 * WebRTC to RTMP Adapter
 * IP Camera Support
 * Recording Live Streams (MP4 and HLS)
 * Restream to Social Media Simultaneously(Facebook and Youtube in in **Enterprise Edition**)
 * One-Time Token Control in **Enterprise Edition**
 * Object Detection in **Enterprise Edition**
 
 ### Links
 
 * [Documentation](https://antmedia.io/documentation) 
 * [Web site](https://antmedia.io)
 * [Community Edition vs. Enterprise Edition](https://antmedia.io/#comparison_table)
 

## Releases 

 ### [Ant Media Server Community 1.6.2 (March 25, 2019)](https://github.com/ant-media/Ant-Media-Server/releases/download/release-1.6.2/ant-media-server-1.6.2-community-1.6.2-20190323_0616.zip)
*   Rest Filtering based on IP Address [#361](https://github.com/ant-media/Ant-Media-Server/issues/361) [#360](https://github.com/ant-media/Ant-Media-Server/issues/360)
*   Update embedded tomcat version to 8.5.38 [#520](https://github.com/ant-media/Ant-Media-Server/issues/520)
*   Configure WebRTC port-ranges for both publishing/playing [#884](https://github.com/ant-media/Ant-Media-Server/issues/884)
*   Auto renew SSL certificates before time-out [#523](https://github.com/ant-media/Ant-Media-Server/issues/523)
*   Add a rest method that accept user-assigned stream id [#1007](https://github.com/ant-media/Ant-Media-Server/issues/1007)
*   Make encoder settings configurable [#826](https://github.com/ant-media/Ant-Media-Server/issues/826)
*   Receive build number from Root app, instead of LiveApp/WebRTCAppEE [#957](https://github.com/ant-media/Ant-Media-Server/issues/957)
*   Vod section add download button [#776](https://github.com/ant-media/Ant-Media-Server/issues/776)
*   Secret based streams authorization without rest api calls [#717](https://github.com/ant-media/Ant-Media-Server/issues/717)
*   Websocket signaling ping/pong request [#718](https://github.com/ant-media/Ant-Media-Server/issues/718)
*   Update ffmpeg, make it compatible with cuda javacpp version [#769](https://github.com/ant-media/Ant-Media-Server/issues/752)
*   Collect exceptions in the instances in order to have better quality # [#909](https://github.com/ant-media/Ant-Media-Server/issues/909)

#### Fixes and Refactors
*   HLS delete files in the edge nodes [#853](https://github.com/ant-media/Ant-Media-Server/issues/853)
*   Collect previews from origin in the cluster [#726](https://github.com/ant-media/Ant-Media-Server/issues/726)
*   WebRTC number of viewers is seen even if stream is not alive [#824](https://github.com/ant-media/Ant-Media-Server/issues/824)
*   WebRTC embed code sometimes does not play automatically [#799](https://github.com/ant-media/Ant-Media-Server/issues/799)
*   Some cases ICE Connection Fails [#869](https://github.com/ant-media/Ant-Media-Server/issues/869)
*   WebRTC playing from edge with the same Id does not start after origin restart during the streaming [#877](https://github.com/ant-media/Ant-Media-Server/issues/877)
*   High Idle load time after some rtsp operations [#855](https://github.com/ant-media/Ant-Media-Server/issues/855)
*   Refactor on webrtc client stats graph [#702](https://github.com/ant-media/Ant-Media-Server/issues/702)
*   Change Rest Service method(GET->POST) of Mp4Muxing for streams [#798](https://github.com/ant-media/Ant-Media-Server/issues/798)
*   Inconsistency in stream number in dashboard [#547](https://github.com/ant-media/Ant-Media-Server/issues/547)
*   WebRTCApp HLS preview doesn't work in Community Edition [#1014](https://github.com/ant-media/Ant-Media-Server/issues/1014)  [#965](https://github.com/ant-media/Ant-Media-Server/issues/965)
*   Unique play.html [#690](https://github.com/ant-media/Ant-Media-Server/issues/690)
*   Simultaneous Periscope accounts issue [#802](https://github.com/ant-media/Ant-Media-Server/issues/802)

 ### [Ant Media Server Community 1.6.1 (Jan 8, 2019)](https://github.com/ant-media/Ant-Media-Server/releases/download/release-1.6.1/ant-media-server-1.6.1-community-1.6.1-190108_1656.zip)
*   Use GPU resources respectively in the system [#663](https://github.com/ant-media/Ant-Media-Server/issues/663)
*   Transcoding is not enabled by default [#734](https://github.com/ant-media/Ant-Media-Server/issues/734)
*   Inject app properties to the AppSettings bean in run time(No injection through xml is used anymore) [#727](https://github.com/ant-media/Ant-Media-Server/issues/727)
*   Make Transcoder frame rate parametric through app properties file [#664](https://github.com/ant-media/Ant-Media-Server/issues/664)
*   Unnecessary edge connection is starting up in some cases [#662](https://github.com/ant-media/Ant-Media-Server/issues/662)
*   Forward video without transcoding for SFU [#436](https://github.com/ant-media/Ant-Media-Server/issues/436)
*   Start to use auto -generated documentation for Rest Services [#187](https://github.com/ant-media/Ant-Media-Server/issues/187)
*   Periscope title does not support utf-8 characters [#769](https://github.com/ant-media/Ant-Media-Server/issues/769)
*   Frame rotation issue in SFU mode [#680](https://github.com/ant-media/Ant-Media-Server/issues/680)
*   Stop broadcast method does not support stopping streams in pulling mode [#661](https://github.com/ant-media/Ant-Media-Server/issues/661)
*   HLS does not updated properly in SFU mode [#701](https://github.com/ant-media/Ant-Media-Server/issues/701)
*   "Undefined" JS app request settings which causes exception [#698](https://github.com/ant-media/Ant-Media-Server/issues/698)



 #### [Ant Media Server Community 1.6.0 (Dec 17, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/release-1.6.0/ant-media-server-community-1.6.0-181216_1551.zip)
  * Update native codes to WebRTC v69
  * Optimize native byte usage in Java side
  * SFU Support (EE) in WebRTC (Forwarding stream without transcoding with low latency)
  * MP4/HLS Recording in SFU
  * RTMP Pushing in SFU
  * Show WebRTC Viewers Stats on Dashboard
  * Rest API Documentation
  * Improved Webhook support
  * Enable/Disable MP4 Recording on Stream basis via Rest API
  * AVC compatibility
  * Bug fixes


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

