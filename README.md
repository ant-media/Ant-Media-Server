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


 #### [Ant Media Server Community 1.5.2 (Nov 16, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.5.2/ant-media-server-community-1.5.2-181116_1126.zip)
 * Session replication and improvements in clustering
 * Improvements in Management Console
 * Publishing WebRTC streams to Social and other RTMP endpoints
 * Bug fixes


Previous releases
https://github.com/ant-media/Ant-Media-Server/releases/

## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

