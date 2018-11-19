[![Build Status](https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master)](https://travis-ci.org/ant-media/Ant-Media-Server) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server)

![Quality Gate](https://sonarcloud.io/api/project_badges/quality_gate?project=io.antmedia%3Aant-media-server)


Ant Media Server 
====

Ant Media Server is an Open Source Media Server that supports:

 * Live Stream Publishing with RTMP, RTSP and WebRTC
 * WebRTC to RTMP Adapter
 * IP Camera Support
 * Recording Live Streams (FLV, MP4 and HLS Containers)
 * Restream to Social Media Simultaneously(Facebook and Youtube in in **Enterprise Edition**)
 * Low Latency 1:N WebRTC Live Streaming in **Enterprise Edition**
 * Adaptive Bitrate Conversion for Live Streams (FLV, MP4, HLS) in **Enterprise Edition**
 * One-Time Token Control in **Enterprise Edition**
 * Object Detection in **Enterprise Edition**
 [Documentation](https://antmedia.io/documentation) & [Enterprise Edition](https://antmedia.io)

## Releases 

 #### [Ant Media Server Community 1.5.2 (Nov 16, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.5.2/ant-media-server-community-1.5.2-181116_1126.zip)
 * Session replication and improvements in clustering
 * Improvements in Management Console
 * Publishing WebRTC streams to Social and other RTMP endpoints
 * Bug fixes

 #### [Ant Media Server Community 1.5.1.1 (October 11, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.5.1.1/ant-media-server-community-1.5.1.1-181011_1410.zip)
* DB Based Clustering
* Autoscaling with DB Based Clustering
* Fetching audio streams from Shoutcast
* Bug fixes

 #### [Ant Media Server Community 1.5.0 (September 21, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.5.0/ant-media-server-community-1.5.0-180921_0855.zip)
* One-Time Token Control Services Added
* Object Detection Features Added
* Improvements & Compatibility on Web Panel
* Bug Fixes


#### [Ant Media Server Community 1.4.0 (August 2, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.4.0/ant-media-server-community-1.4.0-180802_1155.zip)
* Improvement WebRTCApp compatibility for browsers
* Fix the audio & video synch issue in some cases in WebRTCApp
* **Scalable WebRTC streaming** for Enterprise Edition in cluster mode
* Performance improvement in WebRTC stream playing
* Improvements & Compatibility on Web Panel
* Bug-fixes

#### [Ant Media Server Community 1.3.6 (June 21, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.3.6.2/ant-media-server-community-1.3.6.2-180621_2202.zip)
* HLS Viewer Count can be available via web panel and rest service
* Limit the log file size. Old log files are deleted automatically if log files size exceeding 1.5GB
* Change Stream Source URL without changing the URL in the server
* Define period parameter to let Stream Sources or IP Camera Streams restart periodically to create VoD files
* Improve code quality and test coverage. It passes credentials in SonarCloud 
* Bug fixes

#### [Ant Media Server Community 1.3.5.1 (May 25, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.3.5.1/ant-media-server-community-1.3.5.1-180525_1137.zip)

* Restream RTSP, RTMP, HLS and MPEG-TS stream URLs as Stream Source
* Improvement: Start StreamFetcher thread immediately after old one is finished to provide continuous stream
* Bug fix: New Stream Source form does not clear values after a stream source added
* Bug fix: In some streams, adaptive bitrate does not work properly. It creates longer video than expected and
  this prevents web player to play properly
* Bug fix: Login request delay in management panel application

#### [Ant Media Server Community 1.3.4 (Apr 30, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.3.4)

* Enhanced UI Elements,
* Performance Improvements,
* Bug Fixes.

#### [Ant Media Server Community 1.3.2 (Apr 12, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.3.2)

* IP Camera Streaming Support
* ONVIF Standard Features (Ptz etc.).
* Auto Discovery of IP Cameras.
* Creating and Saving VoDs from IP Cameras.
* Upload VoD Feature, Dynamic VoD Folder Selection,
* Enhanced UI Elements.(Stream source quality, stream source speed, etc.)
* Performance Improvements,
* Bug Fixes.

#### [Ant Media Server 1.2.6 Release (Feb 23, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.2.6)
* Security Improvement on accessing Rest Services
* Static Cluster Support
* Load Balancer Documentation
* No need to change XML files for enabling Facebook and Youtube in Enterprise
* Edit Live Streams
* Bug fixes

#### [Ant Media Server 1.2.3 Release (Feb 16, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.2.3)
* Video is enabled by default for WebRTC apps
* Bug fix: While server running on a small VPS, it could not update broadcast status everytime.

#### [Ant Media Server 1.2.2 Release (Feb 13, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.2.2)
* Add Publish Security Handler:
  "Allow Only Streams in Datastore" or "Allow all" can be configured via Management Console UI
* Show non-registered live streams on Management Console when "Allow all" is active
* Fix the problem when there is no audio in WebRTC stream while publishing
* Add google analytic for just measure how many instances are alive
* Publish Live Stream to Facebook Pages
* Add new rest service endpoints that returns viewer count
* Minor bug fixes & improvement on Management Console UI & Refactor codes

#### [Ant Media Server 1.2.1 SNAPSHOT Release (Feb 3, 2018)](https://oss.sonatype.org/service/local/repositories/snapshots/content/io/antmedia/ant-media-server/1.2.1-SNAPSHOT/ant-media-server-1.2.1-20180203.094349-1-community-1.2.1-SNAPSHOT-180203_0943.zip)
* Enable SSL script [Blog post](https://antmedia.io/enable-ssl-on-ant-media-server/)
* Fix No Audio or No Video issue in WebRTC Streams
* Use specific threads for audio, video and signalling in WebRTC Community Edition

#### [Ant Media Server 1.2.0 Release (Jan 27, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/ams-v1.2.0)
* Web Management Interface
* Infrastructure for WebRTC low latency
* Bug fixes
* Documentation

#### [Ant Media Server 1.2.0 SNAPSHOT Release (January 14, 2018)](https://github.com/ant-media/Ant-Media-Server/releases/tag/untagged-e09c2795e299b44bcb86)

#### [Ant Media Server 1.1.1 Release (August 3, 2017)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v1.1.1/ant-media-server-1.1.1.zip)

#### [Ant Media Server 1.0RC Release (June 5, 2017)](https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v.1.0RC/ant-media-server-1.0RC.zip)

#### [Red5+ 1.0.1 Release (27 March 2017)](https://github.com/ant-media/red5-plus-server/releases/tag/v1.0.1_red5_plus)


## Contact 

 For more information and blog posts visit [antmedia.io](https://antmedia.io)
 
 [contact@antmedia.io](mailto:contact@antmedia.io)
 

