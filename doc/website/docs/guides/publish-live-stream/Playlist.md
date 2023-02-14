# Playlist

This guide describes how to use Playlist feature on Ant Media Server. You can use this feature in both Community Edition and Enterprise Edition.

![image.png](@site/static/img/image(2).png)

## What is Linear Live Streaming?

Linear Live Streaming is basically about scheduling your streams for a 7/24 live streaming and can be delivered with different methods. Live Streams and VoD streams can be used in your scheduled live streams which means Linear as well.

So, Linear Live Streams have some programs which have a start and end date streams in the program. Furthermore, Linear Live Streaming is a live event in which all viewers are watching the same live event at the same time. This means you don’t get any spoiler before viewing.

Live linear streaming is a “passive” video viewing experience, meaning viewers don’t “search and click” (except to change the program). The experience of linear streaming is that video content comes to you and while you can change the channel, you don’t have to select an entire collection of videos to watch like you do with a playlist.

## How to create a Playlist?

You can create a playlist in Ant Media Server Dashboard. Your playlist is ready in 2 steps. Here are the steps 🙂

Click `New Live Stream > Playlist` as shown above.

![image.png](@site/static/img/image(3).png)

Just type the Playlist name and Playlist URL into the fields and click “Create” button.

![image.png](@site/static/img/image(4).png)

**Note: Please make sure your VoD files can be accessible with AMS.**

## How to Build your online TV channel in AMS?

You can build your online TV channel with Ant Media Server. You just need mp4 files for the streams. Furthermore, there is no need to store those mp4 files on your server. Ant Media Server can pull the mp4 files from any place that is stored.

## How can I use Playlist API?

You just need to playlist and playListItemList in Broadcast.

Here is the sample create Playlist CURL command:

    curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/create" -d '{ "name":"streamName", "playListItemList":[ { "streamUrl": "http://SAMPLE_STREAM_URL.com/sample.mp4", "type": "VoD" } ], "type":"playlist" }'
    

## How to play Linear Live Streaming?

You can play the playlist in HLS and WebRTC.

Here is HLS player documentation ->` [https://resources.antmedia.io/docs/hls-playing](https://resources.antmedia.io/docs/hls-playing)

Here is WebRTC play documentation ->` [https://resources.antmedia.io/docs/webrtc-playing](https://resources.antmedia.io/docs/webrtc-playing)