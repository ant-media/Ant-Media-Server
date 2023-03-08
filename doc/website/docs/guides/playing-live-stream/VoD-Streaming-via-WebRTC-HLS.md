# VoD Streaming via WebRTC/HLS

You can play VoDs as WebRTC and HLS in Ant Media Server.  Let's show you how to do it.

## Create a playlist in the dashboard

To do this, first log in to the AMS Dashboard, select your **Application**, and click **New** **Live Stream > Playlist**.

![](@site/static/img/vod-hls-webrtc-1.png)

After naming the Playlist, add the URL address of mp4 files you want to add under the Playlist URL and click Create.

![](@site/static/img/vod-hls-webrtc-2.png)

After your playlist is created, click "Start Broadcast" to start the broadcast.

![](@site/static/img/vod-hls-webrtc-3.png)

## How to create Playlist using Rest API?

You just need to put playlist name and URLs in playlistItemList while creating broadcast.

Here is the sample create Playlist CURL command:

```shell
curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/create" -d '{ "name":"streamName", "playListItemList":[ { "streamUrl": "http://SAMPLE_STREAM_URL.com/sample.mp4", "type": "VoD" } ], "type":"playlist" }'
```
    

## How to playback with WebRTC?

Navigate to the following URL in the browser: `https://ams_url:5443/your_app/play.html?name=stream_name`. 

For example, using one of the default applications `WebRTCAppEE` the playback URL will be as below:

`http://ams-domain:5080/WebRTCAppEE/play.html?name=streamid`

![](@site/static/img/vod-hls-webrtc-4.png)

## How to playback with HLS?

If you want to playback your playlist using HLS, then its necessary to explicitly pass HLS as a value for the `playOrder` parametre.  `https://ams_url:5443/your_app/play.html?name=your_stream_id&playOrder=hls`.

![](@site/static/img/vod-hls-webrtc-5.png)