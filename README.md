[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)

![image](https://user-images.githubusercontent.com/54481799/95862105-16cb0e00-0d6b-11eb-9087-88888889825d.png)

## Basic Overview

Ant Media Server is designed to provide live video streaming technology infrastructure with ultra-low latency(WebRTC) and low-latency(HLS, CMAF available in v2.2+). It can be used to enable streaming any type of live or on demand video to any devices including mobiles, PCs or IPTV boxes.

### 3867 Running Instances in 129 Countries 


## Ant Media Server Features
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
 
## Quick Launch

<b>Launch in [Amazon Web Services](https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box)</b>

 <a href="https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/06/1200px-Amazon_Web_Services_Logo.svg-300x180.png" width=90/></a>

<b>Launch in [Microsoft Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1)</b> (Wait a few seconds for listings appear)

 <a href="https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1"><img src="https://i1.wp.com/antmedia.io/wp-content/uploads/2019/01/azure-e1548153434609.png" width=130/></a>

 
### Links

 * [Documentation](http://docs.antmedia.io/)
 * [Web site](https://antmedia.io)
 * [Community Edition vs. Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition) 
 * [Your Scalable, Real-Time Video Streaming Platform Ready in 5 Minutes](https://www.youtube.com/watch?v=y7bP0u0jQRQ)
 
  [![Your Scalable, Real-Time Video Streaming Platform Ready in 5 Minutes](https://img.youtube.com/vi/y7bP0u0jQRQ/0.jpg)](https://www.youtube.com/watch?v=y7bP0u0jQRQ)

 
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

## Releases

### [Ant Media Server Community 2.4.0.2 (Sep 5, 2021)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.4.0.2/ant-media-server-community-2.4.0.2-20210905_1340.zip)

- Plugin Architecture 
- Implementing MCU Plugin(audio-only supported) and providing it as a  built-in plugin -> Quick test: https://SERVER:5443/WebRTCAppEE/mcu.html
- H265 Support in Ingesting WebRTC(H265 encoder is available in some Android devices)
- Support HLS AES-Encryption
- Support QuickSync in Hardware Transcoding
- Configure `X-Forwarded-For` support as built-in for REST API.
- Send specific resolutions to the RTMP endpoints via REST API
- Add JWKS support for JWT Filter
- Provide option to force adaptive bitrate all time or use whenever the source resolution is higher
- Fix random bad name issue in RTMP Ingest (This was a common issue. Thank you God, it seems it's fixed :))
- Upgrade Tomcat to 8.5.69 and don't use native Tomcat libraries(Because it causes crash in some cases)
- Support re-connecting to the same session in a specific timeout after the publisher is disconnected. 

[Full ChangeLog](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v2.4.0)



### [Ant Media Server Community 2.3.3 (June 6, 2021)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.3.2/ant-media-server-2.3.2-community-2.3.2-20210422_0754.zip)

- Check existence of audio sync issue for RTMP to WebRTC case #3229
- Let web panel block the user logging in for 5 minutes if username and password is wrong for 3 times #3246
- H265 Support in WebRTC Ingesting for the supported devices #1817
- Audio & Video Sync in RTMP re-publish & Increased Audio RTT #3231
- Audio distortion/buzzing noises in 2.3.2 EE #3196
- Hash-Based token doesn't work properly in HLS Stream with Adaptive Streaming #2259
- Make Tensorflow solution downloadable #2965
- Support ARM architecture #2607
- Some threads may get blocked infinitely when an unexpected event occurs #3202
- Stream audio-only to RTMP #3132
- Add S3 configuration to Dashboard #3087

[Full ChangeLog](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v2.3.3) 


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


[Full ChangeLog](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v2.3.0) 


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact

 For more information and blog posts visit [antmedia.io](https://antmedia.io)

 [contact@antmedia.io](mailto:contact@antmedia.io)
