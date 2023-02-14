---
title: SRT Ingest Guide
sidebar_position: 1
---

# SRT Ingest Guide

Using SRT, you can push streams to Ant Media Server with SRT and can play not only with WebRTC but also with all other formats (e.g HLS, CMAF), and record it as MP4. Adaptive streaming is also supported for SRT ingest. This feature is available starting from 2.4.3.

For this feature, we’ve used the official [SRT library by Haivision](https://github.com/Haivision/srt). We’ve created an SRT preset for the [JavaCPP-Presets](https://github.com/bytedeco/javacpp-presets) and we plan to send a PR to the JavaCPP-Presets.

You can read about how to play SRT streams below.

## Pushing SRT stream with FFmpeg

We assume that you’ve installed and run Ant Media Server v2.4.3 and later. 

Just enter the following command with FFmpeg:

    ffmpeg -re -i {INPUT} -vcodec libx264 -profile:v baseline -g 60 -acodec aac -f mpegts srt://ant.media.server.address:4200?streamid=WebRTCAppEE/stream1

After you run the command, the stream is going to be available in “**WebRTCAppEE**” with a stream id “**stream1**”.

If you see a "**Protocol not found**" error,  FFmpeg needs to be [compiled with **\--enable-libsrt**](https://srtlab.github.io/srt-cookbook/apps/ffmpeg/) to support the SRT protocol.

**srt://ant.media.server.address:4200?streamid\=WebRTCAppEE/stream1: Protocol not found**

You can check as follows if FFmpeg is compiled with SRT protocol.

    ffmpeg -protocols

## Pushing SRT stream with OBS

If you don’t have command-line tools experience, you can use OBS to push an SRT stream to the Ant Media Server. If you are not familiar with OBS, you can take a look [at this blog post](https://antmedia.io/how-to-use-obs-with-ant-media-server/). Just enter the SRT URL to the stream window as shown in the image below.

![](@site/static/img/Screen-Shot-2022-04-20-at-14.48.30-1024x811.png)

## Playing SRT ingested stream with WebRTC

After you publish the stream to the Ant Media Server either from OBS or FFmpeg, it becomes available in the web panel to watch as any other ingested stream.

SRT is enabled by default in Ant Media Server and it uses the 4200 (UDP) port by default. If you need to change, just open the following file:

```
conf/red5.properties
```

and add/replace the following property:

```js
server.srt_port={WRITE_YOUR_PORT_NUMBER}
```

After this, restart the server and it will use the new configured port number for SRT ingest.

Lastly, SRT support is available in **X86\_64 architectures**. It’s not currently available in ARM architectures and will be added in one of the later releases.