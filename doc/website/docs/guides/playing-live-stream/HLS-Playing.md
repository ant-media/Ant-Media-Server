# HLS Playing

HLS Playing is available in both the Community and Enterprise Editions. Before playing a stream, make sure that stream is broadcasting on the server.

> Quick Link: [Learn How to Publish Live Streams](/v1/docs/publishing-live-streams)

## 1. Navigate to the video player  

You can use play.html under the Application. Visit ```https://your_domain_name:5443/WebRTCAppEE/play.html```. If you're running Ant Media Server on your local computer, you can also visit ```http://localhost:5080/WebRTCAppEE/play.html```
    
You will encounter a Stream ID doesn't exist popup error.
    
![](@site/static/img/image-1645523240043.png)
    

## 2. Add the necessary URL parametres

Pass the stream ID & HLS play order parameters in URL. 
    
```https://your_domain_name:5443/WebRTCAppEE/play.html?name=stream1&playOrder=hls```
    
![](@site/static/img/image-1645523879129.png)
    

## 3. Playback starts automatically

The HLS stream will start to play automatically when it becomes live.
    
![](@site/static/img/image-1645523922843.png)
    

Autoplay may not be activated for some policies in Chrome and Firefox. So you may need to click the player button to get it started. Look at the following links:

[https://developers.google.com/web/updates/2017/09/autoplay-policy-changes](https://developers.google.com/web/updates/2017/09/autoplay-policy-changes) [https://hacks.mozilla.org/2019/02/firefox-66-to-block-automatically-playing-audible-video-and-audio/](https://hacks.mozilla.org/2019/02/firefox-66-to-block-automatically-playing-audible-video-and-audio/)

Congrats. You're playing with HLS.

## More Details About HLS

Make sure that HLS muxing is enabled in your application. By checking the HLS Muxing checkbox in the app's settings on the web management panel, you can verify it.

Assume that HLS muxing is enabled and there is a stream publishing to Ant Media Server with an RTMP URL in this format: ```rtmp://`<SERVER_NAME>`/LiveApp/`<STREAM_ID>```

*   HLS URL is in this format: ```http://<SERVER_NAME>:5080/LiveApp/streams/<STREAM_ID>.m3u8```
*   If there are adaptive streams enabled in Enterprise Edition, HLS Master URL is in this format: ```http://<SERVER_NAME>:5080/LiveApp/streams/<STREAM_ID>_adaptive.m3u8```

## Save HLS Records

HLS streaming is a more cost-effective and secure method than VOD streaming. You can record your HLS streams. You just need to change your application's HLS settings as below:

*   Open your apps ```red5-web.properties``` and change the below mentioned settings. The file is located under `/usr/local/antmedia/webapps/<App-name>/WEB-INF` folder.
    
```js 
settings.hlsPlayListType=event
```
    
To store HLS files permanently after the stream is ended.

```js 
settings.deleteHLSFilesOnEnded=false
```
    
To prevent overwriting of old HLS files in case same stream Id is published again, use the below property.

```js 
settings.hlsflags=+append_list
```

Restart the server on the command line.
    
```shell
sudo service antmedia restart
```
    
Now, your HLS streams will record.

## Announcement for v2.4.1:
After version 2.4.1, the filename structure has added bitrate to the name. So for previous versions, the filename was "stream1_240p0001.ts", now it is "stream1_240p_300kbps0001.ts" , this change was needed because we enabled the same resolution with different bitrates
    

> Quick Link: [App Configurations](/v1/docs/ams-application-configuration) Quick Link: [FFmpeg Configurations](https://ffmpeg.org/ffmpeg-formats.html#toc-Options-6)