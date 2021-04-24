[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)

![image](https://user-images.githubusercontent.com/54481799/95862105-16cb0e00-0d6b-11eb-9087-88888889825d.png)

## Basic Overview

Ant Media Server is designed to provide live video streaming technology infrastructure with ultra-low latency(WebRTC) and low-latency(HLS, CMAF available in v2.2+). It can be used to enable streaming any type of live or on demand video to any devices including mobiles, PCs or IPTV boxes.

### 3300+ Running Instances in 120+ Countries 


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

<br/>
<br/>

<b>Scaling WebRTC</b> streaming is one of the powerful features of Ant Media Server and you can scale up viewers 1 to 30K easily in one minute installation with CloudFormation utility. Here is the [guide!](https://antmedia.io/scaling-webrtc-streaming-30k-cloudformation/)

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
### [Ant Media Server Community 2.3.2 (April 22, 2021)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.3.2/ant-media-server-2.3.2-community-2.3.2-20210422_0754.zip)
 - Increase buffer size - fixes Chrome 90 issue #3164 
 - Synch video according to audio in SFU mode
 - Fix during audio stream adding RTMP Endpoint 
 - Stop timers(native stats, measuring bitrate) in signaling thread 
 - Fix pts and dts base timing in the incoming packets
 - Remove jsvc 

### [Ant Media Server Community 2.3.1 (April 14, 2021)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.3.1/ant-media-server-2.3.1-community-2.3.1-20210414_1212.zip)
- Problem in fragment.size() Check #3053
- Audio intermittently cuts out. #3035
- Webm start - stop recording does not work #3096
- Fix analyze time in EncoderAdaptor #2939
- P2P NoSpaceForNewPeer error  #2954
- Create app is not working on the fly in standalone mode #3033
- Sorted Application List Would Be Good #2783
- Web Panel does not show vod ID #3006
- Import external js files into the package #2943
- Get MD5 of password in Angular app #3075
- Provide configuration option to push CMAF and HLS to any Http Endpoint  #3071
- Create and delete apps via web panel #3064
- Update FFmpeg(4.3.2) and CUDA(11.2) #3047
- Audio/Video sync issue in 2.3 with ABR #3088

### [Ant Media Server Community 2.3.0 (March 3, 2021)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.3.0/ant-media-server-2.3.0-community-2.3.0-20210301_0825.zip)
- Improvements in Low latency DASH
- Low latency HLS (experimental)
- Stereo support for WebRTC streaming
- Audio forwarding in SFU mode
- White Board implementation on top of WebRTC data channel
- WebRTC Data Channel support without video & audio
- Application deploy/undeploy on the fly in cluster mode
- Time-based Tokens (TOTP) for stream security
- JWT tokens for stream and REST security
- Support Multi-Level Cluster

[Full ChangeLog](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v2.3.0) 


### [Ant Media Server Community 2.2.1 (Oct 29, 2020)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.2.1/ant-media-server-2.2.1-community-2.2.1-20201029_0748.1.zip)
- Support CMAF in DASH [2471](https://github.com/ant-media/Ant-Media-Server/issues/2471), [2440](https://github.com/ant-media/Ant-Media-Server/issues/2440)
- Update from Java 8 to Java 11 [2394](https://github.com/ant-media/Ant-Media-Server/issues/2394), [2458](https://github.com/ant-media/Ant-Media-Server/issues/2458)
- Tomcat performance improvement with APR and SSL [2525](https://github.com/ant-media/Ant-Media-Server/issues/2525)
- Support Kubernetes [2390](https://github.com/ant-media/Ant-Media-Server/issues/2390), [2464](https://github.com/ant-media/Ant-Media-Server/pull/2464)
-  Create REST method equivalent of WebSocket's getRoomInfo [2463](https://github.com/ant-media/Ant-Media-Server/issues/2463), [2596](https://github.com/ant-media/Ant-Media-Server/issues/2596)
-  Update default STUN server [2472](https://github.com/ant-media/Ant-Media-Server/issues/2472)
-  Tomcat version is updated to 8.5.58 [2447](https://github.com/ant-media/Ant-Media-Server/issues/2447)
-  Support custom resolutions from WebRTC to RTMP in Community Edition [2485](https://github.com/ant-media/Ant-Media-Server/issues/2485)
- Fix number of Viewers decrease less than zero [2438](https://github.com/ant-media/Ant-Media-Server/pull/2438)
- HLS token problem in Cluster [2432](https://github.com/ant-media/Ant-Media-Server/pull/2432)
- Support WebRTC and HLS Viewer Limit in Broadcasts [2489](https://github.com/ant-media/Ant-Media-Server/issues/2389)
[Full ChangeLog](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v2.2.0)


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact

 For more information and blog posts visit [antmedia.io](https://antmedia.io)

 [contact@antmedia.io](mailto:contact@antmedia.io)
