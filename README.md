[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server)
<a href="https://twitter.com/antmedia_io" ><img src="https://img.shields.io/twitter/follow/antmedia_io.svg?style=social" /> </a>

![image](https://user-images.githubusercontent.com/54481799/95862105-16cb0e00-0d6b-11eb-9087-88888889825d.png)

## Basic Overview

Ant Media Server is a streaming engine software that provides adaptive, ultra low latency streaming by using WebRTC technology with ~0.5 seconds latency or low latency by using HLS or CMAF. Ant Media Server is highly scalable both horizontally and vertically, running on-premises or on any cloud provider of your choice.

We help create high-performing organizations by supporting them with their low latency streaming infrastructure. This way, we're solving the video streaming and delivery headache, a huge problem for companies seeking low latency, adaptive video streaming, by making it simple and on-demand.

Ant Media’s main product is real-time streaming engine, called Ant Media Server. Currently, 6000+ Ant Media Server instances run in over 120 countries. Apart from the flagship product, Ant Media Enterprise Edition, startups can also enjoy the open source Community Edition, downloadable from this repo. 

## Ant Media Server Features

**Community Edition**

 * RTMP ingesting
 * RTMP, RTSP, MP4 and HLS support	
 * WebRTC to RTMP adapter
 * IP camera support
 * 360 Degree Live & VoD streams
 * Intuitive web management dashboard
 * Recording live streams (MP4 and HLS)
 * iOS & Android RTMP SDKs
 * Javascript SDK

**Enterprise Edition**

In addition to what Community Edition has to offer, the Enterprise Edition has the following features: 

 * Ultra low latency adaptive one to many WebRTC live streaming 
 * Scalable to [hundreds of thousands of simultaneous viewers](https://antmedia.io/ultimate-guide-for-webrtc-hero-with-ant-media-server/)
 * [Adaptive bitrate](https://antmedia.io/adaptive-bitrate-streaming/) for live streams (WebRTC, MP4, HLS)
 * [VP8 & H264 support in WebRTC](https://antmedia.io/webrtc-vp8-data-channel-and-4k-60fps-support/)
 * [Data channel support](https://antmedia.io/exchange-data-easily-using-webrtc-data-channels/) in WebRTC
 * [CMAF streaming support](https://antmedia.io/cmaf-streaming/)
 * Horizontal clustering and vertical scaling 
 * SFU in one to many WebRTC Streams
 * [Live stream publishing from RTMP to WebRTC](https://antmedia.io/start-your-live-video-stream-with-webrtc-and-make-it-available-to-watch-with-dash-and-hls/)
 * [Restream to social media simultaneously](https://antmedia.io/how-to-publish-live-stream-in-social-media/) (e.g Facebook and Youtube)
 * [Hardware encoding](https://antmedia.io/unknown-5-webrtc-stream-features/) (Nvidia GPU, QuickSync)
 * [Secure video streaming](https://antmedia.io/secure-video-streaming/) using one-time token control
 * Object detection via [Tensorflow](https://antmedia.io/object-detection-with-tensorflow/)
 
Ant Media Server is bundled with Android, iOS and JavaScript SDKs. SDKs are available for [free](https://antmedia.io/free-webrtc-android-ios-sdk/).

Please see a [comparison table for Community and Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition) here.

## Quick launch

Ant Media server is available in your favorite cloud platform:

* [AWS](https://aws.amazon.com/marketplace/pp/prodview-464ritgzkzod6?sr=0-1&ref_=beagle&applicationId=AWSMPContessa)
* [Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1)
* [Alibaba](https://marketplace.alibabacloud.com/products/56712002/Ant_Media_Server_Enterprise_2_2_1-sgcmjj00025347.html)
* [Digital Ocean](https://marketplace.digitalocean.com/apps/antmedia-server-enterprise-edition-3) 
* [Linode](https://www.linode.com/marketplace/apps/ant-media/ant-media-community-edition/)

 
## Usage scenarios
### 👨🏽‍💻 Education
Ant Media can provide virtual classrooms to teachers, using ultra-low latency technology, enabling teachers connect with the audience using 1-1 or 1-many connection types.

### 🤖 IP camera streaming
Watch and Monitor IP cameras with ultra low latency on a web browser with Ant Media Server. You can embed ONVIF IP camera streams into your websites and mobile applications.

### 🙇 Webinars
Ant Media Server supports N-N live video/audio conferencing by using WebRTC, allowing you to achieve ultra-low latency (~ 0.5 sec). Ant Media Server also provides scalability that can help you to scale up your solution dynamically.

### 👾 Mobile streaming application
Using our SDKs, you can integrate your mobile application solutions with Ant Media Server, and build a fast, reliable and stable streaming platform with AMS APIs and SDKs.

### 📺 Live game shows
Live video experience has a significant role in live game show success, with a strong requirement of being scalable, low latency and adaptive for which Ant Media provides.

### 🎯 E-sports & betting streaming
Due to the evergrowing esports domain, there is a tremendous demand for video streaming with ultra-low latency.

### 🎭 Auction and bidding streaming
Live auctions should be streamed with ultra sub second latency in order to get bids ontime.

### ✨ Video game streaming
Ant Media Server resolves interactivity and scalability issues by providing ultra low latency streaming via WebRTC.


## ⚡ Releases

Ant Media Server Community Edition Releases are [automatically deployed here](https://github.com/ant-media/Ant-Media-Server/releases), with auto-generated changelog. 

Ant Media Server Enterprise Editions are available through several cloud marketplaces ([AWS](https://aws.amazon.com/marketplace/pp/prodview-464ritgzkzod6?sr=0-1&ref_=beagle&applicationId=AWSMPContessa), [Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps?search=Ant%20Media%20Server&page=1), [Alibaba](https://marketplace.alibabacloud.com/products/56712002/Ant_Media_Server_Enterprise_2_2_1-sgcmjj00025347.html), [Digital Ocean](https://marketplace.digitalocean.com/apps/antmedia-server-enterprise-edition-3) and [Linode](https://www.linode.com/marketplace/apps/ant-media/ant-media-community-edition/)). You can also download Enterprise Edition via subscription on [antmedia.io](https://antmedia.io)

## 🌱 Links

* [Web site](https://antmedia.io)
* [Documentation](https://github.com/ant-media/Ant-Media-Server/wiki)
* [How do companies use Ant Media?](https://antmedia.io/case-studies/)
* [We're hiring](https://angel.co/company/ant-media-1)
* [Community Edition vs. Enterprise Edition](https://github.com/ant-media/Ant-Media-Server/wiki#community-edition--enterprise-edition) 
* [Your Scalable, Real-Time Video Streaming Platform Ready in 5 Minutes](https://www.youtube.com/watch?v=y7bP0u0jQRQ)
 
## 📫 Contact

* For more information and blog posts visit [antmedia.io](https://antmedia.io/blog/)
* For general inquiries, [contact@antmedia.io](mailto:contact@antmedia.io)

## ⭐️ Supporting Ant Media Server

If you would like to say thank you or/and support active development of Ant Media Server: 

* Add a [GitHub Star](https://github.com/ant-media/Ant-Media-Server/) to the project.
* Tweet about project on your Twitter.
* Write interesting articles about project on [Dev.to](https://dev.to/), [Medium](https://medium.com/) or personal blog.

Together, we can make this project **better** every day! 😘
