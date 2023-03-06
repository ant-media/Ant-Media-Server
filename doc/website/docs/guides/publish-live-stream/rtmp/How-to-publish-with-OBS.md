# How to publish with OBS

OBS (Open Broadcaster Software) is free and open source software for video recording and live streaming. You can use either your PC’s embedded camera or externally connected camera as a video source with OBS. Sound sources also can be configured.

Let’s have a look at step by step how to use OBS for streaming:

## Install the OBS

Download Open Broadcaster Software from [obsproject.com](https://obsproject.com/) and install it. 

## Provide sources

Open OBS. By default, OBS starts to capture from your built-in camera if it exists. You can add or remove video/audio source from Sources section.

## Configure OBS

We assume that your Ant Media Server accepts all streams (e.g there is no security option enabled.)

*   Click ```Settings``` in the OBS Window and then Select ```Stream``` on the left side menu.
*   Choose ```Custom Streaming Server``` in the ```Stream Type``` dropdown menu.
*   In the URL box, type your RTMP URL without stream id. It's like ```rtmp://your_server_domain_name/LiveApp```
*   In the Stream key, you can write any stream id because we assume that no security option is enabled.

![](@site/static/img/OBS_Configuration.png)

**When you use tokens** you need to generate a publish token and use it in this format inside the stream key : ```streamdid?token=tokenid```

![](@site/static/img/obs-stream-settings.png)

## Tune for ultra-low latency streaming

OBS by default is not optimized for ultra-low latency streaming. If you push RTMP stream with OBS and play with WebRTC, please open ```Settings > Output``` and make the rate control ```CBR (Constant Bitrate)``` and tune for ```zerolatency```. Also, you can configure the bitrate according to your quality and internet bandwidth requirements. Additionally, ```keyframe interval``` should be adjusted to 1.

![](@site/static/img/tune_for_ultra_low_latency.png)

Please keep in mind that if your network is not stable to send requested bitrate all the time, you may see freezes in playing the stream.

## Start streaming

Close ```Settings``` window and just click the “Start Streaming” button in the main window of OBS.

![](@site/static/img/obs_screenshot.jpg)

Congrats! You're publishing a live stream with OBS.

## Troubleshooting

If you have have problems with stream quality, you should check the following indicators in OBS.

### Stream health

Stream health parameters are located at the bottom right. There are 3 stream health parameters: ```Dropped Frames```, ```CPU``` and ```Stream health color.``` 

*   **Dropped frames**: This value should be 0. If it increases, you may decrease your FPS or bitrate parameters in OBS settings.
*   **CPU**: CPU value is important for streaming quality. We recommend that it never exceeds 70%.
*   **Stream health color**: This color should be green. If it has a color between yellow and red you may have problems with your stream quality.

![](@site/static/img/obs-broadcast-health.png)

### OBS cncoding overloaded warning

If your PC cannot handle the stream with the parameters you set, this warning appears. In this case, you may want to decrease the resolution, bitrate, or change the encoder preset value to one with high speed and low quality.

![](@site/static/img/obs-encoding-overloaded.png)

### Streaming recorded VoD files

If you want to stream your VoD files, please consider your VoD encoder settings as below:

*   **Profile**: ```Baseline```
*   **Tune**: ```Zerolatency```

You can change your VoD encoder settings as below:

```ffmpeg -i input.mp4 -profile baseline -tune zerolatency output.mp4```