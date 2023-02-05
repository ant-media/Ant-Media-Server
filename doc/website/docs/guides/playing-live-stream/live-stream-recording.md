# Live Stream Recording

Ant Media Server can create an MP4 asset when you finish broadcasting to your live stream. 

## Playing VOD streams with MP4

First, confirm that your application has MP4 muxing for live streams enabled. This can be enabled in the app's settings on the Web panel.

Assume that there is a live stream that is publishing to the Ant Media Server. After publishing is finished, the MP4 file will be created.

*   In Community Edition, an MP4 URL will be available at this URL: http://`<SERVER\_NAME>`:5080/LiveApp/streams/Stream\_Id.mp4
*   In Enterprise Edition, MP4 for different bitrates is also created if adaptive bitrate is enabled. Assuming you have 480p and 240p resolution enabled in adaptive bitrate settings, you will have two more MP4 files with the following format:
    *   ```http://<SERVER_NAME>:5080/LiveApp/streams/<STREAM_ID>_480p.mp4```
    *   ```http://<SERVER_NAME>:5080/LiveApp/streams/<STREAM_ID>_240p.mp4```


## Playing live and VoD streams with an embedded player

There are embedded players in both the Community and Enterprise Editions. Both live and on-demand streams can be played by this player. If Ant Media Server has a live stream available, you can view it and embed it using the following URL format:

```http://<SERVER_NAME>:5080/LiveApp/play.html?name=<STREAM_ID>```

When the live stream is over, the recorded MP4 file with an embedded player can be viewed at the URL below.

```http://<SERVER_NAME>:5080/LiveApp/play.html?name=<STREAM_ID>&playOrder=vod```