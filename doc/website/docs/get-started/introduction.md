---
title: Introduction
sidebar_position: 1
---

Ant Media provides ready-to-use, highly scalable real-time video streaming solutions for live video streaming needs. Based on customer requirements and preferences, it enables a live video streaming solution to be deployed easily and quickly on-premise or on public cloud networks such as AWS, Azure, Digital Ocean, Linode, and Alibaba Cloud.

Ant Media’s well-known product, called Ant Media Server, is a video streaming platform and technology enabler, providing highly scalable, Ultra-Low Latency (WebRTC) and Low Latency (CMAF & HLS) video streaming solutions supported with operational management utilities. There are two versions of Ant Media Server: Community Edition and Enterprise Edition. You can find a comparison table below.  

Ant Media Server in a cluster mode dynamically scales up and down to enable our customers to serve from tens to hundreds of thousands of viewers in an automated and controlled way.

Ant Media Server provides compatibility to be played in any web browser. In addition to this, SDKs for iOS, Android, and JS are provided freely to enable customers to expand their reach to a broader audience. Thanks to the adaptive bitrate streaming feature that allows any video to be played at any bandwidth on mobile devices.

Ant Media has been serving more than 1150 customers in more than 120 countries. Customers from different segments such as education, live auctions, radio and TV broadcasts, enterprises, service providers, government agencies, gaming companies, and mobile / web app developers enhance their business by using Ant Media Server and benefit from its capabilities.

## Usage scenarios  
  
### 👨🏽‍💻 Education

Ant Media can provide virtual classrooms to teachers, using ultra-low latency technology, enabling teachers to connect with the audience using 1-1 or 1-many connection types.

### 🤖 IP camera streaming

Watch and Monitor IP cameras with ultra-low latency on a web browser with Ant Media Server. You can embed ONVIF IP camera streams into your websites and mobile applications. [Read more](https://github.com/ant-media/Ant-Media-Server/#-ip-camera-streaming)

### 🙇 Webinars  

Ant Media Server supports N-N live video/audio conferencing by using WebRTC, allowing you to achieve ultra-low latency (~ 0.5 sec). Ant Media Server also provides scalability that can help you to scale up your solution dynamically. [Read more](https://github.com/ant-media/Ant-Media-Server/#-webinars) 

### 👾 Mobile streaming application 

Using our SDKs, you can integrate your mobile application solutions with Ant Media Server, and build a fast, reliable and stable streaming platform with AMS APIs and SDKs. [Read more](https://github.com/ant-media/Ant-Media-Server/#-mobile-streaming-application) 

### 📺 Live game shows  

Live video experience has a significant role in live game show success, with a strong requirement of being scalable and having low latency. [Read more](https://github.com/ant-media/Ant-Media-Server/#-live-game-shows) 

### 🎯 E-sports & betting streaming   

Due to the ever-growing esports domain, there is a tremendous demand for video streaming with ultra-low latency. [Read more](https://github.com/ant-media/Ant-Media-Server/#-e-sports--betting-streaming)

### 🎭 Auctions and bidding 

Live auctions should be streamed with ultra sub-second latency in order to get bids on time. [Read more](https://github.com/ant-media/Ant-Media-Server/#-auction-and-bidding-streaming)

### ✨ Video game streaming 

Ant Media Server resolves interactivity and scalability issues by providing ultra-low latency streaming via WebRTC. [Read more](https://github.com/ant-media/Ant-Media-Server/#-video-game-streaming)

### Community & Enterprise Edition comparison 

Ant Media Server has two versions. One of them is the Community Edition (freely downloadable) and the other one is Enterprise Edition, which is a paid and supported version. Community Edition is available to [download on Github.](https://github.com/ant-media/Ant-Media-Server) Enterprise Edition can be purchased [on antmedia.io](https://antmedia.io/). [Read more](https://github.com/ant-media/Ant-Media-Server/#-releases) 
  
**Feature**|**Community Edition**|**Enterprise Edition**
:-----:|:-----:|:-----:
One-to-Many WebRTC Streaming| ![false](@site/static/img/cross.png )   | ![false](@site/static/img/tick.png )
End-to-End Latency|8-12 Seconds|0.5 Seconds (500ms)
CMAF|![false](@site/static/img/cross.png )  | ![false](@site/static/img/tick.png )
Scaling|![false](@site/static/img/cross.png )  | ![false](@site/static/img/tick.png )
RTMP(Ingesting) to WebRTC (Playing)| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
Hardware Encoding(Nvidia GPU, QuickSync)| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
WebRTC Data Channel|![false](@site/static/img/cross.png )  | ![false](@site/static/img/tick.png )
Adaptive Bitrate| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
Secure Streaming| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
SRT ingest support| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
iOS & Android WebRTC SDK| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
VP8 and H.265 Support| ![false](@site/static/img/cross.png ) | ![false](@site/static/img/tick.png )
iOS & Android RTMP SDK| ![false](@site/static/img/tick.png ) | ![false](@site/static/img/tick.png )
JavaScript SDK| ![false](@site/static/img/tick.png )| ![false](@site/static/img/tick.png )
RTMP, RTSP, MP4 and HLS Support| ![false](@site/static/img/tick.png )| ![false](@site/static/img/tick.png )
WebRTC to RTMP Adapter|![false](@site/static/img/tick.png ) | ![false](@site/static/img/tick.png )
360 Degree Live & VoD Streams|![false](@site/static/img/tick.png ) | ![false](@site/static/img/tick.png )
Web Management Dashboard|![false](@site/static/img/tick.png ) | ![false](@site/static/img/tick.png )
IP Camera Support| ![false](@site/static/img/tick.png )| ![false](@site/static/img/tick.png )
Re-stream Remote Streams| ![false](@site/static/img/tick.png )| ![false](@site/static/img/tick.png )
Open Source| ![false](@site/static/img/tick.png )| ![false](@site/static/img/tick.png )
Simulcast to all Social Media via RTMP|![false](@site/static/img/tick.png ) | ![false](@site/static/img/tick.png )
Support|Community|E-mail, On-site
Price|Free|Paid


## Community Edition Releases

You can directly download AMS Community Edition from** [**Github.**](https://github.com/ant-media/Ant-Media-Server/releases/)

## Licensing

Ant Media Server has basically three types of licenses.

1.  Ant Media Server Community Edition is free to use, with Apache license.
2.  You can opt in with a pay-as-you-go plan where usage is calculated every streaming minute.  
    
3.  Ant Media Server Enterprise Edition has a paid license per instance/server. Paid license options are hourly, monthly, annually and perpetual. You can ask for a license from us.

### Enterprise Edition cluster license

Enterprise Edition cluster license is similar features to the Enterprise Edition license. The only difference is that Enterprise Cluster License supports many instances run simultaneously with the same license key. On the other hand, the Enterprise Edition license key only supports one instance at a time.

If you're planning to have a large deployment for your Enterprise Cluster, please contact Sales at [contact@antmedia.io](mailto:contact@antmedia.io) in order to discuss discount options.

### Free Enterprise Edition license for educational and tech communities

Ant Media provides _free Enterprise Edition licenses_ for students, academics, and communities. To get the advantage of this opportunity, just send an email (from your institution or community e-mail address) to [contact@antmedia.io](mailto:contact@antmedia.io)

## Functional Architecture

**![](@site/static/img/image-1648754379709.png)**  

## Supported Environments

Ant Media Server runs on **Linux (Ubuntu and CentOS)**. it supports only x64 architecture, and deployment scripts are provided for Ubuntu (starting from 18.04) and CentOS 8. In addition, It's known that Ant Media Server is used on MacOS, SuSE, Debian, and Red Hat Enterprise Linux distributions as well.

## Extensions

#### Object recognition with TensorFlow

Ant Media Server can use trained deep learning model to recognize objects in live streams. This is a CPU-intensive process so if you enable this feature, the server's CPU consumption will increase.

Meanwhile, users can use any deep models to execute live streams on the fly.

## Ant Media Community discussion

There is a user community available. You can ask or answer questions by joining the community on [https://community.antmedia.io/](https://community.antmedia.io/?forumId=4)

You can also ask your questions at StackOverflow with the [ant-media-server] tag.

## Contact

For more information and blog posts visit [antmedia.io](https://antmedia.io/). If you have any questions, please send an email to [contact@antmedia.io](mailto:contact@antmedia.io). Support inquiries should go to [support@antmedia.io](mailto:support@antmedia.io).
