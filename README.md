[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)

![image](https://user-images.githubusercontent.com/54481799/95862105-16cb0e00-0d6b-11eb-9087-88888889825d.png)

## Basic Overview

Ant Media Server is designed to provide live video streaming technology infrastructure with ultra-low latency(WebRTC) and low-latency(HLS, CMAF available in v2.2+). It can be used to enable streaming any type of live or on demand video to any devices including mobiles, PCs or IPTV boxes.

### 2037 Running Instances in 111 Countries at 09:33 AM GMT on July 21, 2020


Ant Media Server Features
====

 * Ant Media Server is an open source media server:
 * Ant Media Server is available with Free (Community) and Paid (Enterprise) versions.
 * Ultra Low Latency Adaptive One to Many WebRTC Live Streaming in **Enterprise Edition**
 * Adaptive Bitrate for Live Streams (WebRTC, MP4, HLS) in **Enterprise Edition**
 * VP8 & H264 Support in WebRTC **Enterprise Edition**
 * Data Channel Support in WebRTC **Enterprise Edition** 
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
 * Ant Media Server is bundled with Android, iOS and JavaScript SDKs. SDKs are available for [free](https://antmedia.io/free-webrtc-android-ios-sdk/).
 
 [Comparison table for Community and Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition)
 
 ![image](https://user-images.githubusercontent.com/54481799/95864676-64954580-0d6e-11eb-94d0-d661746d49af.png)
## Usage Scenarios
### Education
Ant Media can provide virtual classrooms with teachers as many as it is required and it might be presented and viewed separately with ultra-low latency.
### IP camera streaming 
Watch and Monitor IP Cameras with Ultra Low Latency on Web Browser with Ant Media Server. You can embed ONVIF IP camera streams into your websites and mobile applications.
### Webinar
Ant Media Server supports N-N live video/audio conferencing by using WebRTC protocol that allows you to achieve ultra-low latency which is around 0.5 sec. Ant Media Server also provides scalability that can help you to scale up your solution dynamically.
### Mobile Streaming Application
Easy to Integrate and Scalable Streaming Infrastructure Solutions for your Mobile Application Projects. You can build fast ve stable streaming applications with Ant Media API’s and SDK’s. 
### Live Game Show
Scale Number of Viewers As Much As You Need. Live Video Experience has the significant role in Live Game Shows’ success. It must be scalable, low latency and adaptive.  Ant Media Server provides scalable, ultra-low latency and adaptive streaming. 
### E-sports Betting
Due to the biggest grow at esports, there is a tremendous demand for betting more   than normal sports and video streaming with ultra-low latency is a game changer for  that industry.
### Auction and Bidding Streaming 
Live Auction should be streamed with ultra sub second latency in order to get bids on 
time. Otherwise, your items get sold for a lower price. Ant Media Server provides sub-second  latency streaming. 
### Video Game Streaming
Gamers are generally complaining about interaction problems. Ant Media Server resolves this problem by ultra low latency streaming(0.5 second) via WebRTC.


Scaling WebRTC streaming is one of the powerful features of Ant Media Server and you can scale up viewers 1 to 30K easily in one minute installation with CloudFormation utility. Here is the [guide!](https://antmedia.io/scaling-webrtc-streaming-30k-cloudformation/) 
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



### [Ant Media Server Community 2.1.0 (July 20, 2020)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.1.0/ant-media-server-2.1.0-community-2.1.0-20200720_1340.zip) 

Features
* H265 Transcoding from RTMP to WebRTC [#2058](https://github.com/ant-media/Ant-Media-Server/issues/2058)
* WebM Recording [#2144](https://github.com/ant-media/Ant-Media-Server/issues/2144)
* Force WebRTC Player to play at specified resolution [#2155](https://github.com/ant-media/Ant-Media-Server/issues/2155)
* Create a websocket message that returns the available streams in the conference room [#2227](https://github.com/ant-media/Ant-Media-Server/issues/2227)
* Create a websocket message that notifies client that if bandwidth is less than the video/audio bitrate [#2103](https://github.com/ant-media/Ant-Media-Server/issues/2103)
* Check broadcast start and end time before accepting the WebRTC Stream [#2181](https://github.com/ant-media/Ant-Media-Server/issues/2181)
* Update video.js to the latest version for HLS and MP4 playback [#2231](https://github.com/ant-media/Ant-Media-Server/issues/2231)
* Create a REST method that can send message to the viewers through Data channel [#2026](https://github.com/ant-media/Ant-Media-Server/issues/2026)
* Provide the ability to choose audio input in WebRTC publishing [#2164](https://github.com/ant-media/Ant-Media-Server/issues/2164)
* Implement switch in front and back camera in JS SDK for mobile platforms [#2022](https://github.com/ant-media/Ant-Media-Server/issues/2022)
* Fetching streams in the origin cluster [#1406](https://github.com/ant-media/Ant-Media-Server/issues/1406)
* Support Unified Plan or PlanB in WebRTC [#2226](https://github.com/ant-media/Ant-Media-Server/issues/2226)
* New REST method to get VoD Id by Stream Id [#2244](https://github.com/ant-media/Ant-Media-Server/issues/2244)

Fixes and Improvements
* Upgrade Tensorflow Library to 1.15.0 [#2025](https://github.com/ant-media/Ant-Media-Server/issues/2025)
* Adding Facebook RTMP Endpoint is not working [#1981](https://github.com/ant-media/Ant-Media-Server/issues/1981)
* Fix 10 NAL Units in libx264 && freeze and quick play [#2037](https://github.com/ant-media/Ant-Media-Server/issues/2037)
* Show total available memory in the web panel [#2136](https://github.com/ant-media/Ant-Media-Server/issues/2136)
* The sound stops after 20 seconds on the edge server [#2198](https://github.com/ant-media/Ant-Media-Server/issues/2198)
* MP4 Files cannot be downloaded because of the wrong absolute path [#2070](https://github.com/ant-media/Ant-Media-Server/issues/2070)
* Unexpected number of HLS viewers increase [#2015](https://github.com/ant-media/Ant-Media-Server/issues/2015)
* Decrease number of threads in WebRTC signaling [#2265](https://github.com/ant-media/Ant-Media-Server/issues/2265)
* Fix for EncoderBlocked Warning [#2273](https://github.com/ant-media/Ant-Media-Server/issues/2273)
* Micro freeze in some RTMP streams [#2095](https://github.com/ant-media/Ant-Media-Server/issues/2095)
* Stream fetcher does not start again after restart period [#2241](https://github.com/ant-media/Ant-Media-Server/issues/2241)
* Edit stream source does not work if it's not fetching [#2251](https://github.com/ant-media/Ant-Media-Server/issues/2251)
* MP4 files uploaded in S3 have public_read permission issue [#1965](https://github.com/ant-media/Ant-Media-Server/issues/1965)
* Completing MP4 record while server is stopping [#2030](https://github.com/ant-media/Ant-Media-Server/issues/2030)
* phtread_create exception in some instances [#2254](https://github.com/ant-media/Ant-Media-Server/issues/2254)
* Add second to the date-time value in mp4 recording [#2232](https://github.com/ant-media/Ant-Media-Server/issues/2232)
* Fix external SSL certificate [#2301](https://github.com/ant-media/Ant-Media-Server/issues/2301)
* Add listenerHookURL in updateSettings [#2230](https://github.com/ant-media/Ant-Media-Server/issues/2230)
* SFU Mode sometimes does not work in H264 & VP8 Enabled [#2175](https://github.com/ant-media/Ant-Media-Server/issues/2175)


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





Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)

