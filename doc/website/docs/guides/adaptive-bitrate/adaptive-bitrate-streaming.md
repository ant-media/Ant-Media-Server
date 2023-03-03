---
title: Adaptive Bitrate Streaming
---
# Adaptive Bitrate Streaming

Adaptive bitrate streaming (also known as "dynamic adaptive streaming" or "multi-bitrate streaming") allows you to deliver the optimum video quality according to the network bandwidth between you and the media server. This enables users to play videos smoothly, regardless of their internet connection speed or device.

## Why use adaptive bitrate

More people are getting connected to the internet, and more videos are viewed each day. However, there may be issues, as internet connection speeds may not let you watch videos in high quality. In this case, the player needs buffering, which makes viewers wait while watching the video.

However, there might be a problem when watching videos online because slow internet connections typically prevent high-quality video playback, which makes you wait while watching videos.

![](@site/static/img/buffering.jpg)

In order to provide a better user experience, service providers create lower resolutions of the videos to make people watch them seamlessly even if their network condition is not good enough to watch HD videos. This way, you do not have to wait for the player to buffer, thanks to adaptive streaming.

![](@site/static/img/AP658325161480_131.jpg)

## Adaptive bitrate on the fly

Lowering the resolutions of videos for recorded streams is not a big deal. However, doing the same job for live streams on the fly is not as easy as for recorded streams. Thankfully, Ant Media Server supports adaptive bitrate streaming in Enterprise Edition, and live streams can be played with WebRTC and HLS (HTTP Live Streaming).

![](@site/static/img/HLSsegmentedvideodelivery.png)

## How WebRTC & HLS adaptive streaming works

AMS supports adaptive streaming in both WebRTC and HLS formats. On the other hand, there is a slight difference between WebRTC and HLS adaptive streaming. Ant Media Server measures the player's bandwidth in WebRTC and chooses the best quality in accordance with that measurement. The player in HLS determines its bandwidth and requests the best quality from the server.

## How to enable adaptive bitrate

### From the dashboard

Enable adaptive streaming under App >` Settings >` Adaptive Bitrate and add new streams.  
![](@site/static/img/abs.png)

Note:

Adaptive streaming detects the device's bandwidth and CPU capacity and adjusts the streaming rate and video quality accordingly. Therefore, you'll see a higher CPU load, and we recommend enabling the GPU for AMS.

> Quick Link: [Learn How to Enable GPU for Ant Media Server](https://resources.antmedia.io/docs/using-nvidia-gpus)

The configuration above will create videos at 1080p, 720p, and 360p resolutions if the incoming stream resolution is higher than 1080p. If the incoming stream resolution is 480p, then 480p and 360p versions of the stream will be created on the fly.

![](@site/static/img/iosmediacaptureresolutions.png)

### Using configuration file

Open the configuration file `{INSTALL\_DIR}/webapps/{APP\_NAME}/WEB-INF/red5-web.properties`**` with your favorite editor.

Now, add this line to the file: 

```js
settings.encoderSettingsString=[
  {
    "videoBitrate":800000,
    "forceEncode":true,
    "audioBitrate":64000,
    "height":360},
    {
      "videoBitrate":500000,
      "forceEncode":true,
      "audioBitrate":32000,
      "height":240
    }
]
```

The format of the file is as follows: resolution height, video bitrate per second, and audio bitrate per second. In the example above, we are adding two adaptive bitrates:

1.  360p, 800Kbps video bitrate, 64Kbps audio bitrate
2.  240p, 500Kbps video bitrate, 32Kbps audio bitrate

Save and close the file. After entering adaptive bitrate settings manually, you need to restart the Ant Media Server.
```shell
sudo service antmedia restart
```