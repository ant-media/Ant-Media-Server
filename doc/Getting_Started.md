# Getting Started

Ant Media Server is a software that can stream live and vod videos. It supports adaptive streaming on the fly and 
records live videos in several formats like HLS, MP4, etc. 

### Features
* Receive live streams in RTMP, RTSP and WebRTC
* Records live streams in MP4, FLV, HLS and Dash Formats
* Transcodes live streams into lower resolutions on the fly for adaptive streaming
* Play live streams with RTMP, RTSP, WebRTC, HLS and Dash Formats


## Installation

#### Linux (Ubuntu)

Firstly, start installing Oracle Java 8

Run the below commands to install Oracle's PPA
```
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
```

It is time to install Java 8
```
$ sudo apt-get install oracle-java8-installer
```

Next step is installing Jsvc in order to run media server as a service

```
$ sudo apt-get install jsvc
```

Download the Ant Media Server Community Edition from http://antmedia.io 

Extract the zip folder. In order to do that first install unzip
```
$ sudo apt-get install unzip
$ unzip ant-media-server.zip     
```

Move the ant-media-server directory to /usr/local/antmedia 
```
$ sudo mv ant-media-server /usr/local/antmedia
```

Go to /usr/local/antmedia 
```
$ cd /usr/local/antmedia
```
Edit the antmedia file by a text editor like nano, vim, etc.
and change the line from

```JAVA_HOME="/usr/lib/jvm/default-java"```
to ```JAVA_HOME="/usr/lib/jvm/java-8-oracle/"```


Copy antmedia folder to /etc/init.d and install the service
```
$ sudo cp antmedia /etc/init.d/
$ sudo update-rc.d antmedia defaults
$ sudo update-rc.d antmedia enable
```

It is time to start the server
```
$ sudo service antmedia start
```

To stop the service run the command below
```
$ sudo service antmedia stop
```

To see the status of the service, run
```
$ sudo service antmedia status
```

### Server Ports
In order to server run properly you need to open some network ports. 
Here are the ports server uses

* TCP:1935 (RTMP)
* TCP:5080 (HTTP)
* TCP:5443 (RTSP)
* UDP:5000-65000 (RTP in RTSP)
* TCP: 8081-8082 (WebSocket)






