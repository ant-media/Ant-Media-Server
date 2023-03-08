# Recording live streams

Ant Media Server supports various types of live stream recording. Recording features are controlled by the REST API for which some of the APIs are available from the web panel. There are two options for recording: you can either enable recording for all of incoming streams or enable recording for a specific stream ID. In this document, we will go through mp4 and WebM recording and REST calls to enable them.

Enabling recording
------------------

## mp4 recording

To be able to record live streams as mp4, we first need to have the right codecs which are supported by MP4 container. The most famous codec for this purpose is H.264, which is enabled as the default codec in Ant Media Server. If h.264 is disabled, mp4 recording will not be available, you can either set the h264 enabled setting from `/usr/local/antmedia/webapps/<your_app_name>/WEB-INF/red5-web.properties` and changing the below setting: 

```settings.h264Enabled=true```

Or from the dashboard, under Applications settings.

![Screenshot from 2021-12-06 17-03-19](https://user-images.githubusercontent.com/32591015/144859658-8a1887e2-3e3a-4247-948f-6c35e611684a.png)

## Enabling mp4 recording for every stream in application

You can enable mp4 recording from either the dashboard or settings under /usr/local/antmedia/webapps/`<your\_app\_name>`/WEB-INF/red5-web.properties file. To update from the settings file, add:

```settings.mp4MuxingEnabled=true```

From the dashboard, click on the checkbox for **H264** and **Record live streams as mp4**;

![Screenshot from 2021-12-06 16-20-31](https://user-images.githubusercontent.com/32591015/144853316-ca9ef1ce-9bfd-428d-b396-3e2f935f56d0.png)

## Enabling Mp4 recording for a specific stream

You can set each streams recording individually with a REST call. It allows users to start and stop recording a live stream when it is necessary and discard the rest to protect resources. The required API is: https://antmedia.io/rest/#/BroadcastRestService/enableRecording

An example curl command to start recording a particular stream;

```curl -X PUT "http://localhost:5080/LiveApp/rest/v2/broadcasts/{stream1}/recording/{true}?recordType=mp4"```

After enabling the mp4 recording, you can publish with webRTC, RTMP or you can pull the stream source. Server will record incoming streams as Mp4 files as long as they have h.264 codec.

WebM
----

To record webm formatted files, we need to have VP8 codec enabled at the Ant Media Server application. WebM recording is supported by webRTC since VP8 is supported. However, by adding adaptive bitrate, you can record e.g RTMP ingested streams, which do not support VP8, just like WebM. You can either set the h264 enabled setting from /usr/local/antmedia/webapps/`<your\_app\_name>`/WEB-INF/red5-web.properties file as;

```settings.vp8Enabled=true```

Or from the dashboard, under Applications settings;

![Screenshot from 2021-12-06 17-00-45](https://user-images.githubusercontent.com/32591015/144859285-9dedac37-f0a7-4f0d-94d5-de97f393d194.png)

## Enabling WebM recording for every stream in application

You can enable WebM recording from either the dashboard or settings under /usr/local/antmedia/webapps/`<your\_app\_name>`/WEB-INF/red5-web.properties file. To update from the settings file, add:

```settings.webMMuxingEnabled=true```

From the dashboard, click on the checkbox for **VP8** and **Record Live Streams as WebM**;

![Screenshot from 2021-12-06 17-09-46](https://user-images.githubusercontent.com/32591015/144860705-981906aa-1f14-41fb-a39b-f67e2c4ecda9.png)

## Enabling WebM recording for a specific stream

With the same REST call as in Mp4, you can set individual recording for each stream by adding recordType=webm at the parameters: [Rest call for set recording](https://antmedia.io/rest/#/BroadcastRestService/enableRecording)

Example;

```curl -X PUT "http://localhost:5080/LiveApp/rest/v2/broadcasts/{stream1}/recording/{true}?recordType=webm"```

After enabling WebM recording, you can record the live streams that has VP8 codec as WebM files.

Additional entities of recording
--------------------------------

## Recording with different resolutions and bitrates

One of the main features of Ant Media Server is adaptive bitrate, which makes a difference also when it comes to recording. If you enable any kind of recording with adaptive bitrate settings, server will record each resolution in a format like;

```stream1_240p500kbps.webm```

Enabling adaptive bitrate means, server is transcoding the video inside itself. This can extend the ability to record incoming streams.

## Recording a stream which has different codec

Containers do not support every codec. Assume you are publishing with RTMP but you also want to record in WebM format. In this case, Ant Media server can transcode video and audio codec to required format with adaptive bitrate. Let's say a 240p bitrate is added and RTMP publishing is ongoing. If you enable webM recording, 240p stream will be recorded like ```stream1_240p500kbps.webm```

Storing recordings to another directory
---------------------------------------

AMS stores the recordings to streams directory by default.

The streams directory is located under ```usr/local/antmedia/webapps/(LiveApp/WebRTCAppEE)/streams```

For example, if you are using the LiveApp application, streams directory will be ```usr/local/antmedia/webapps/LiveApp/streams```

If you would like to store the recordings (VoDs) to another directory/location, it is recommended to create a symbolic link.

For live streams, to create symbolic link:

    cp -p -r /usr/local/antmedia/webapps/appname/streams/ /backup/
    
    rm -rf /usr/local/antmedia/webapps/appname/streams/
    
    ln -s /mnt/vod_storage/folder /usr/local/antmedia/webapps/appname/streams
    

After creating the symbolic link, you need to change the persmission of both base directory and target dirctory using below commands.

    sudo chown -R antmedia:antmedia /usr/local/antmedia
    
    sudo chown -R antmedia:antmedia /mnt/vod_storage/folder
    

In order to link another directory containing MP4 files as VoD directory on Web Panel:

1.  Login to Ant Media Server panel
2.  Go to Applications (LiveApp/WebRTCAppEE) settings
3.  Set VoD streaming folder (i.e., add directory where MP4 files are located).

## Uploading records to S3

Please check [S3 documentation for Ant Media Server](/v1/docs/integrating-with-s3).

To configure the HLS, record (MP4 or WebM) or output PNG format, you can use the following setting;

```settings.uploadExtensionsToS3```

Add this setting under /usr/local/antmedia/webapps/app-name/WEB-INF/red5-web.properties file.

After making the changes just restart the server with **sudo service antmedia restart**.

This setting is a number where the digits represent whether an upload will be done or. The least significant digit switches record files, the second switches HLS and the third for PNG.

Example: settings.uploadExtensionsToS3=5 ( 101 in binary ) means upload mp4 and PNG but not HLS

Possible values are as follows:

Don't upload anything:

```settings.uploadExtensionsToS3=0```

Only record file (mp4 or webm) upload:

```settings.uploadExtensionsToS3=1```

HLS upload only:

```settings.uploadExtensionsToS3=2```

HLS + record upload:

```settings.uploadExtensionsToS3=3```

PNG upload only:

```settings.uploadExtensionsToS3=4```

PNG and record upload:

```settings.uploadExtensionsToS3=5```

PNG + HLS upload:

```settings.uploadExtensionsToS3=6```

Upload everything (default behaviour):

```settings.uploadExtensionsToS3=7```