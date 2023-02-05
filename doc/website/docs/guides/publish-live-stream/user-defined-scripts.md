---
title: User Defined Scripts
---
# User defined Scripts

User defined scripts are run automatically by the Ant Media Server after the MP4 Muxing process (recording) finishes or VoD upload process finishes. It enables users to make some changes on the mp4 file. A few examples:

1) Creating different resolutions for VoD serving ( Using adaptive bitrate on the fly will spend more resources, you can just transcode once for each VoD with your own user defined script after every muxing operations )

2) Merging VoDs with ffmpeg

3) Adding some watermark to VoDs after the stream is saved.

You can get creative with user-defined scripts, there are no limits. They are called after each streams recording process is finished or each VoD upload process is finished.

MP4 muxing(recording) finish process
------------------------------------

 It will work after the MP4 Muxing(recording) process finishes. Let’s have a look at that step by step.

1.  [Define run script location in App Settings](#define-mp4-muxing)
2.  [Script running instructions](#mp4-muxing-script)

### Define MP4 muxing run script location in App Settings

Add script setting in ```[AMS-DIR]/webapps/applications(LiveApp or etc.)/WEB-INF/red5-web.properties```

Usage:
```shell
settings.muxerFinishScript
```

Example Usage:
```shell
settings.muxerFinishScript=/Script-DIR/scriptFile.sh
```

Save the file and restart the server

```shell
sudo service antmedia restart
```

The script should be able to executable permission

Mark the file as executable with below code:

```shell
chmod +x scriptFile.sh
```

Setting References: [settings.muxerFinishScript Setting](/v1/docs/ams-application-configuration)

### MP4 Muxing script usage instructions

After the muxing process is finished, the AMS runs the following code snippets.

```shell
scriptFilePath fullPathOfMP4File
```

Example:

```
~/test_script.sh /usr/local/antmedia/webapps/LiveApp/streams/test_stream.mp4
```

When script is finished successfully, AMS writes in INFO log as a below:

```
running muxer finish script: ~/test_script.sh /usr/local/antmedia/webapps/LiveApp/streams/test_stream.mp4
```
## VoD upload finish process

It will work after the VoD upload process finishes. Let’s have a look at that step by step.

1.  [Define run script location in App Settings](#define-vod-upload)
2.  [Script running instructions](#vod-upload-script)

### Define VoD upload run script location in App Settings

Add script setting in ```[AMS-DIR]``` / ```webapps``` / ```applications(LiveApp or etc.)``` / ```WEB-INF``` / ```red5-web.properties```

Usage:

```
settings.vodUploadFinishScript
```

Example Usage:
```
settings.vodUploadFinishScript=/Script-DIR/scriptFile.sh
```

Save the file and restart the server
```
sudo service antmedia restart
```

**The script should be able to executable permission**

Mark the file as executable with below code:
```
chmod +x scriptFile.sh
```

Setting References: [settings.vodUploadFinishScript Setting](/v1/docs/ams-application-configuration)

### VoD Upload script usage instructions

After the VoD upload process is finished, the AMS runs the following code snippets.

```
scriptFilePath fullPathOfMP4File
```

Example:
```
~/test_script.sh /usr/local/antmedia/webapps/LiveApp/streams/test_stream.mp4
```

When script finished successfully, AMS writes in INFO log as a below:
```
running muxer finish script: ~/test_script.sh /usr/local/antmedia/webapps/LiveApp/streams/test_stream.mp4
```