[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)

### 1551 Running Instances in 110 Countries at 07:11 AM GMT on May 5, 2020


Ant Media Server 
====

Ant Media Server is an open source media server that supports:

 * Ultra Low Latency Adaptive One to Many WebRTC Live Streaming in **Enterprise Edition**
 * Adaptive Bitrate for Live Streams (WebRTC, MP4, HLS) in **Enterprise Edition**
 * VP8 & H264 Support in WebRTC **Enterprise Edition**
 * Data Channel Support in WebRTC **Enterprise Editio** 
 * Horizontal(Clustering) and Vertical Scaling **Enterprise Edition**
 * SFU in One to Many WebRTC Streams in **Enterprise Edition**
 * Live Stream Publishing from RTMP to WebRTC **Enterprise Edition**
 * RTMP Ingesting
 * WebRTC to RTMP Adapter
 * IP Camera Support
 * Recording Live Streams (MP4 and HLS)
 * Restream to Social Media Simultaneously(Facebook and Youtube in **Enterprise Edition**)
 * One-Time Token Control in **Enterprise Edition**
 * Object Detection in **Enterprise Edition**
 
 [Comparison table for Community and Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition)
 
## Quick Launch
 
<b>Launch in [Amazon Web Services](https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box)</b>

 <a href="https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/06/1200px-Amazon_Web_Services_Logo.svg-300x180.png" width=90/></a>
 
<b>Launch in [Microsoft Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1)</b> (Wait a few seconds for listings appear)
  
 <a href="https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/01/azure-e1548153434609.png" width=130/></a>
 
 
 ### Links
 
 * [Documentation](http://docs.antmedia.io/) 
 * [Web site](https://antmedia.io)
 * [Community Edition vs. Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition)
 

## Releases

### [Ant Media Server Community 2.0.0 (May 4, 2020)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.0.0/ant-media-server-2.0.0-community-2.0.0-20200504_1842.zip)
* VP8 Support in Playing, Ingesting, Transcoding and Clustering [#1816](https://github.com/ant-media/Ant-Media-Server/issues/1816) [#1962](https://github.com/ant-media/Ant-Media-Server/issues/1962) [#2013](https://github.com/ant-media/Ant-Media-Server/issues/2013) [#1994](https://github.com/ant-media/Ant-Media-Server/issues/1994) 
* Data Channel Support in Playing, Ingesting, Clustering [#1737](https://github.com/ant-media/Ant-Media-Server/issues/1737) [#2004](https://github.com/ant-media/Ant-Media-Server/issues/2004) [#2011](https://github.com/ant-media/Ant-Media-Server/issues/2001) [#2045](https://github.com/ant-media/Ant-Media-Server/issues/2045) [#1866](https://github.com/ant-media/Ant-Media-Server/issues/1866) 
* 4K 60 FPS RTMP -> WebRTC Streaming Support [#1854](https://github.com/ant-media/Ant-Media-Server/issues/1854) [#1867](https://github.com/ant-media/Ant-Media-Server/issues/1867) [#1759](https://github.com/ant-media/Ant-Media-Server/issues/1759) [#1775](https://github.com/ant-media/Ant-Media-Server/issues/1775)
* WebRTC Stack is updated to WebRTC M79 [#1818](https://github.com/ant-media/Ant-Media-Server/issues/1818) [#1838](https://github.com/ant-media/Ant-Media-Server/issues/1838) [#1827](https://github.com/ant-media/Ant-Media-Server/issues/1827) 
* Official Ubuntu 18.04 support [#1655](https://github.com/ant-media/Ant-Media-Server/issues/1655)
* Cluster Monitoring Support [#1897](https://github.com/ant-media/Ant-Media-Server/issues/1897) 
* Playlist Support [#199](https://github.com/ant-media/Ant-Media-Server/issues/199)
* Native Screen Share Support without extension [#1662](https://github.com/ant-media/Ant-Media-Server/issues/1662)
* Add RTMP Buffering for Smooth WebRTC Play [#1975](https://github.com/ant-media/Ant-Media-Server/issues/1975)
* Remote JMX Connection Support [#1595](https://github.com/ant-media/Ant-Media-Server/issues/1595)
* Make Admin Panel accesible via IP Filter [#1891](https://github.com/ant-media/Ant-Media-Server/issues/1891)  
* Fixes and Improvements [#1845](https://github.com/ant-media/Ant-Media-Server/issues/1845) [#1913](https://github.com/ant-media/Ant-Media-Server/issues/1913) [#1954](https://github.com/ant-media/Ant-Media-Server/issues/1954) [#1967](https://github.com/ant-media/Ant-Media-Server/issues/1967) [#1960](https://github.com/ant-media/Ant-Media-Server/issues/1960) [#1940](https://github.com/ant-media/Ant-Media-Server/issues/1940) [#2045](https://github.com/ant-media/Ant-Media-Server/issues/2045) [#1991](https://github.com/ant-media/Ant-Media-Server/issues/1991) [#1923](https://github.com/ant-media/Ant-Media-Server/issues/1923) [#2043](https://github.com/ant-media/Ant-Media-Server/issues/2043) [#1992](https://github.com/ant-media/Ant-Media-Server/issues/1992) [#1907](https://github.com/ant-media/Ant-Media-Server/issues/1907) [#1932](https://github.com/ant-media/Ant-Media-Server/issues/1932)  

### [Ant Media Server Community 1.9.1 (Jan 12, 2020)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.9.1/ant-media-server-1.9.1-community-1.9.1-20200112_1622.zip)
* Thread blocking issues [#1757](https://github.com/ant-media/Ant-Media-Server/issues/1757) [#1781](https://github.com/ant-media/Ant-Media-Server/issues/1781) [#1784](https://github.com/ant-media/Ant-Media-Server/issues/1784)
* Support IPv6 for ICE Candidates [#1714](https://github.com/ant-media/Ant-Media-Server/issues/1714)
* Update Youtube App secrets [#1688](https://github.com/ant-media/Ant-Media-Server/issues/1688)
* Sort VoD files in Panel [#640](https://github.com/ant-media/Ant-Media-Server/issues/640)
* Increase Cluster compatibility with Vidiu Encoder [#1716](https://github.com/ant-media/Ant-Media-Server/issues/1716)
* Edit Access log name [#1633](https://github.com/ant-media/Ant-Media-Server/issues/1633)
* Add start/stop button to Web Panel for Stream Sources [#1697](https://github.com/ant-media/Ant-Media-Server/issues/1697)
* Show native memory usage in Web Panel [#1704](https://github.com/ant-media/Ant-Media-Server/issues/1704)

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




Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

