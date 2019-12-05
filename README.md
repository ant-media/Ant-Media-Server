[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)

###### 1083 Active Instances in 103 Countries at 09:08 AM GMT on Nov 28, 2019


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
 * Restream to Social Media Simultaneously(Facebook and Youtube in **Enterprise Edition**)
 * One-Time Token Control in **Enterprise Edition**
 * Object Detection in **Enterprise Edition**
 
## Quick Launch
 
<b>Launch in [Amazon Web Services](https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box)</b>

 <a href="https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/06/1200px-Amazon_Web_Services_Logo.svg-300x180.png" width=90/></a>
 
<b>Launch in [Microsoft Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1)</b> (Wait a few seconds for listings appear)
  
 <a href="https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/01/azure-e1548153434609.png" width=130/></a>
 
 
 ### Links
 
 * [Documentation](https://antmedia.io/documentation) 
 * [Web site](https://antmedia.io)
 * [Community Edition vs. Enterprise Edition](https://antmedia.io/#comparison_table)
 

## Releases 

### [Ant Media Server Community 1.9.0 (Nov 27, 2019)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.9.0/ant-media-server-community-1.9.0-20191127_1738.zip)
* Excessive Bandwidth Algorithm [#1516](https://github.com/ant-media/Ant-Media-Server/issues/1516)
* Built-in HTTP Forwarding for services like S3 [#1615](https://github.com/ant-media/Ant-Media-Server/issues/1615
* S3 Multipart upload support [#1663](https://github.com/ant-media/Ant-Media-Server/issues/1663)
* More control of STUN, UDP candidates [#1514](https://github.com/ant-media/Ant-Media-Server/issues/1514)
* Start/Stop MP4 Recording while stream is broadcasting [#1270](https://github.com/ant-media/Ant-Media-Server/issues/1270) [#1310](https://github.com/ant-media/Ant-Media-Server/issues/1310)
* Add/Remove RTMP end points while stream is broadcasting [#1510](https://github.com/ant-media/Ant-Media-Server/issues/1510)
* IP Filter(CIDR) for RTMP publishing [#1462](https://github.com/ant-media/Ant-Media-Server/issues/1462)
* Generic App Structure [#686](https://github.com/ant-media/Ant-Media-Server/issues/686)
* Support VP8 in Publishing in Community and Adaptive Enable in Enterprise [#1529](https://github.com/ant-media/Ant-Media-Server/issues/1529)
* Support form in the web panel [#1543](https://github.com/ant-media/Ant-Media-Server/issues/1543)
* Refactor in cluster mode that is cleaner and faster [#1517](https://github.com/ant-media/Ant-Media-Server/issues/1517)
* Faster response for single track (audio or video only) streams [#1502](https://github.com/ant-media/Ant-Media-Server/issues/1502)
* Improvements and fixes: [#1597](https://github.com/ant-media/Ant-Media-Server/issues/1597) - [#1581](https://github.com/ant-media/Ant-Media-Server/issues/1581) - [#1567](https://github.com/ant-media/Ant-Media-Server/issues/1567) - [#1557](https://github.com/ant-media/Ant-Media-Server/issues/1557) - [#1556](https://github.com/ant-media/Ant-Media-Server/issues/1556) - [#1555](https://github.com/ant-media/Ant-Media-Server/issues/1555) - [#1515](https://github.com/ant-media/Ant-Media-Server/issues/) - [#1513](https://github.com/ant-media/Ant-Media-Server/issues/) - [#1512](https://github.com/ant-media/Ant-Media-Server/issues/1515) - [#1490](https://github.com/ant-media/Ant-Media-Server/issues/1490) - [#1486](https://github.com/ant-media/Ant-Media-Server/issues/1486) - [#1116](https://github.com/ant-media/Ant-Media-Server/issues/1116) - [#1617](https://github.com/ant-media/Ant-Media-Server/issues/1617) - [#1577](https://github.com/ant-media/Ant-Media-Server/issues/1577) - [#1558](https://github.com/ant-media/Ant-Media-Server/issues/1558) - [#1537](https://github.com/ant-media/Ant-Media-Server/issues/1537) - [#1536](https://github.com/ant-media/Ant-Media-Server/issues/1536)

### [Ant Media Server Community 1.8.1 (Aug 28, 2019)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.8.1/ant-media-server-community-1.8.1-20190828_0800.zip)
* Make WebRTC Publishing/Playing Load Balancer friendly [#1340](https://github.com/ant-media/Ant-Media-Server/issues/1340)
* Handover of streams between nodes in auto-scale mode [#585](https://github.com/ant-media/Ant-Media-Server/issues/585)
* Support Screen + webcam(PIP) broadcasting [#1247](https://github.com/ant-media/Ant-Media-Server/issues/1247)
* Support ONVIF Zoom-in/Zoom-out methods in REST [#1339](https://github.com/ant-media/Ant-Media-Server/issues/1339)
* Support Room Token in Conference calls [#1337](https://github.com/ant-media/Ant-Media-Server/issues/1337) [#1268](https://github.com/ant-media/Ant-Media-Server/issues/1268)
* Create REST Service v2 [#1317](https://github.com/ant-media/Ant-Media-Server/issues/1317)
* Support Chat Solution [#431](https://github.com/ant-media/Ant-Media-Server/issues/431)
* Remove RTSP support completely for playing/ingesting [#1423](https://github.com/ant-media/Ant-Media-Server/issues/1423)
* Make CPU limit configurable [#1143](https://github.com/ant-media/Ant-Media-Server/issues/1143)
* Upgrade Spring Framework [#1212](https://github.com/ant-media/Ant-Media-Server/issues/1212)
* Improvements and fixes [#1408](https://github.com/ant-media/Ant-Media-Server/issues/1408) [#1249](https://github.com/ant-media/Ant-Media-Server/issues/1249) [#1343](https://github.com/ant-media/Ant-Media-Server/issues/1343) [#934](https://github.com/ant-media/Ant-Media-Server/issues/934) [#1395](https://github.com/ant-media/Ant-Media-Server/issues/1395) [#1204](https://github.com/ant-media/Ant-Media-Server/issues/1204) [#1336](https://github.com/ant-media/Ant-Media-Server/issues/1336) [#307](https://github.com/ant-media/Ant-Media-Server/issues/307) [#1257](https://github.com/ant-media/Ant-Media-Server/issues/1257) [#1254](https://github.com/ant-media/Ant-Media-Server/issues/1254) [#1258](https://github.com/ant-media/Ant-Media-Server/issues/1258) [#1266](https://github.com/ant-media/Ant-Media-Server/issues/1266) [#1319](https://github.com/ant-media/Ant-Media-Server/issues/1319) [#1425](https://github.com/ant-media/Ant-Media-Server/issues/1425) [#1409](https://github.com/ant-media/Ant-Media-Server/issues/1409) [#1477](https://github.com/ant-media/Ant-Media-Server/issues/1477)


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


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

