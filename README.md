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
 
## Quick Launch
 Launch pre-installed Ant Media Server instantly. 
 
### Amazon Web Services
 Launch Ant Media Server instantly in [Amazon Web Services](https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box)
 
 <a href="https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/06/1200px-Amazon_Web_Services_Logo.svg-300x180.png" width=90/></a>
 
### Microsoft Azure
  Launch Ant Media Server instantly in [Microsoft Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1) (Wait a few seconds for listings appear)
  
 <a href="https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/01/azure-e1548153434609.png" width=130/></a>
 
 
 
 ### Links
 
 * [Documentation](https://antmedia.io/documentation) 
 * [Web site](https://antmedia.io)
 * [Community Edition vs. Enterprise Edition](https://antmedia.io/#comparison_table)
 
 ### Quick Launch 
 * Launch Ant Media Server in Amazon Web Services(AWS)
 * Launch Ant Media Server in Azure
 

## Releases 

### [Ant Media Server Community 1.7.2 (June 2, 2019)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.7.2/ant-media-server-community-1.7.2-20190602_1447.zip)
 * %40 Performance Improvement in WebRTC Streams [#1130](https://github.com/ant-media/Ant-Media-Server/issues/1130), [#1147](https://github.com/ant-media/Ant-Media-Server/issues/1147), [#1068](https://github.com/ant-media/Ant-Media-Server/issues/1068)
 * View Logs on Web Panel [#100](https://github.com/ant-media/Ant-Media-Server/issues/100)
 * Licence Control for Enterprise Edition [#762](https://github.com/ant-media/Ant-Media-Server/issues/762)
 * Web Panel  Improvements [#1057](https://github.com/ant-media/Ant-Media-Server/issues/1057), [#1059](https://github.com/ant-media/Ant-Media-Server/issues/1059), [#1055](https://github.com/ant-media/Ant-Media-Server/issues/1055), [#437](https://github.com/ant-media/Ant-Media-Server/issues/437)
 * REST API method to get all system stats [#907](https://github.com/ant-media/Ant-Media-Server/issues/907)
 * Optimize Memory Operations in WebRTC native side [#971](https://github.com/ant-media/Ant-Media-Server/issues/971)
 * Better error handling for stream fetching [#955](https://github.com/ant-media/Ant-Media-Server/issues/955)
 * Enable/Disable stats update to data stores [#1131](https://github.com/ant-media/Ant-Media-Server/issues/1131)
 * Built-in Apache Kafka Producer for monitoring nodes [#1164](https://github.com/ant-media/Ant-Media-Server/issues/1164)
 * Reliability: Create unique host and port for cluster nodes [#1215](https://github.com/ant-media/Ant-Media-Server/issues/1215), [#1214](https://github.com/ant-media/Ant-Media-Server/issues/1214)
 * Bug fix: MP4 rotation in SFU mode [#825](https://github.com/ant-media/Ant-Media-Server/issues/825)
 * Bug fix: Sudden Connection Drop [#978](https://github.com/ant-media/Ant-Media-Server/issues/978)
 * Bug fix: Cluster Edge-Origin Reconnect [#1106](https://github.com/ant-media/Ant-Media-Server/issues/1106)
 * Bug fix: Stop WebRTC streams through REST Stop Method [#1160](https://github.com/ant-media/Ant-Media-Server/issues/1160)
 * Bug fix: Stream Sources Deleting [#1272](https://github.com/ant-media/Ant-Media-Server/issues/1272)
 * Bug fix: Stream stop does not stop immediately in edge [#1257](https://github.com/ant-media/Ant-Media-Server/issues/1257)
 * Bug fix: Web panel IP Camera adding failure
 


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


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

