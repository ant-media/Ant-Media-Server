# Multitrack publish and play with AMS

Webrtc multitrack lets us stream multiple video/audio tracks through a single WebRTC connection. In order to use Multitrack feature, the SDP semantic should be set as **Unified Plan** which is also set by defalut in AMS v2.4.3 and above.

With multitrack streams, you can play different groups of streams with a single broadcast ID. Then, you can start playing those groups of streams with one play request and most importantly through a single WebRTC connection and this decreases resource usage as well.

### Terminologies related to multitrack

**Main track:** StreamId of a group is referred as main track.  
**Sub track:** The streams in a group with different streamIds are referred as sub tracks.

In order to group the broadcasts into a single broadcast (main track), you need to do one of the items below.

*   For WebRTC streams, you need to pass group ID as main track while calling publish method of webrtcAdaptor in the SDK.

`https://{AMS_URL}:5443/AppName/?mainTrack={GROUP_ID}`

*   For RTMP streams, you need to add multiTrack parameter to the stream ID as:

`rtmp://{AMS_URL}:1935/WebRTCAppEE/{STREAM_ID}?mainTrack={GROUP_ID}`

### Publishing multitrack streams

Let's publish a stream with streamid=video to the sample WebRTCAppEE application with group streamId (main track) as main.

`rtmp://{AMS_URL}:1935/WebRTCAppEE/video?mainTrack=main`

Now publish streams with different audio subtracks as required.

Let's say there are two addio tracks with streamId as audio1 and audio2.

![multitrack-streams.png](@site/static/img/multitrack-streams.png)

### Playing multitrack streams

Multitrack streams can be played with the sample page, **multitrackplayer.html**

`https://{AMS_URL}:5443/WebRTCAppEE/multitrackplayer.html`

![sample.png](@site/static/img/sample(1).png)

1.  Write the group id in the text box
2.  Request the sub-tracks by clicking the Tracks button
3.  Select the tracks you want to play and click the Play button
4.  If a new subtrack is added to the group, it will be played automatically

You can enable or disable video/audio feed for a sub-track with enableTrack(mainTrackId, trackId, enabled) methods in the webrtc-adaptor SDK.

![multitrack-player-1.png](@site/static/img/multitrack-player-1(1).png)

### Multitrack conference

We have implemented a new sample conference page, **conference-room.html** and this page is compatible with multitrack playback.

The main-track (group) id will be the same as the room id.

`https://{AMS_URL}:5443/WebRTCAppEE/conference-room.html?roomName=main`

In Multitrack conference the play request is called once for the roomId.