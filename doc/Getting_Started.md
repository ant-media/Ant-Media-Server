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

## Build from Source

#### Linux (Ubuntu)
A couple of repos should be clone and build with maven so that git and maven should be installed in advance

* Go to a directory where you clone repos
* Clone and build Ant-Media-Server-Common

```
$ git clone https://github.com/ant-media/Ant-Media-Server-Common.git
$ cd Ant-Media-Server-Common
$ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
$ cd ..
```

* Clone and build the Ant-Media-Server-Service 

```
$ git clone https://github.com/ant-media/Ant-Media-Server-Service.git
$ cd Ant-Media-Server-Service
$ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
$ cd ..
```

* Clone and build the Periscope-Producer-API 

```
$ git clone https://github.com/ant-media/Periscope-Producer-API.git
$ cd Periscope-Producer-API/
$ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
$ cd ..
```

* Clone and build the Tomcat Plugin
```
$ git clone https://github.com/ant-media/red5-plugins.git
$ cd red5-plugins/tomcat/
$ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
$ cd ../..
```



* Clone, build and package Ant-Media-Server
```
$ git clone https://github.com/ant-media/Ant-Media-Server.git
$ cd Ant-Media-Server
$ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
$ ./repackage.sh
```

If everthing goes well, a new packaged Ant Media Server(ant-media-server-x.x.x.zip) file will be created 
in Ant-Media-Server/target directory


### Server Ports
In order to server run properly you need to open some network ports. 
Here are the ports server uses

* TCP:1935 (RTMP)
* TCP:5080 (HTTP)
* TCP:5443 (RTSP)
* UDP:5000-65000 (RTP in RTSP)
* TCP: 8081-8082 (WebSocket)






