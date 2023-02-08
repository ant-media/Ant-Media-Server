# WebRTC WebSocket Messaging Reference

This documentation is for developers who needs to implement signalling between Ant Media Server and clients for publishing & playing streams. Let's make it step by step

##  Publishing WebRTC Stream

Client connects to Ant Media Server through WebSocket. URL of the WebSocket interface is something like
    
`ws://SERVER_NAME:5080/WebRTCAppEE/websocket`
    

Client sends publish JSON command to the server with stream name parameter. (Remove token parameter if token control is not enabled) Only ```Command``` and ```streamId``` is mandatory. Audio and video is enabled default, it can be overwritten. ```Token```, ```subscriberId``` and ```subscriberCode``` is for stream security and optional like setting audio and video. You can take a look at this [guide](https://antmedia.io/time-based-one-time-password-totp-makes-streams-secure/) to learn more about ```subscriberId``` and ```subscriberCode``` and how to use them. For general ```stream security``` information take a look at [here](/v1/docs/stream-security).

If Server accepts the stream, it replies back with start command

```json
{
  command : "start",
  streamId : "stream1",
}
```  



4.  Client inits peer connections, creates offer sdp and send the sdp configuration to the server with takeConfiguration command
```json
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "offer",  
  sdp : "${SDP_PARAMETER}"
}
```    

5.  Server creates answer sdp and send the sdp configuration to the client with takeConfiguration command

```json
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "answer",  
  sdp : "${SDP_PARAMETER}"
}
```

6.  Client and Server get ice candidates several times and sends to each other with takeCandidate command

```json 
{
  command : "takeCandidate",
  streamId : "stream1",
  label : "${CANDIDATE.SDP_MLINE_INDEX}",
  id : "${CANDIDATE.SDP_MID}",
  candidate : "${CANDIDATE.CANDIDATE}"
}
```        
    

7.  Clients sends stop JSON command to stop publishing

```json
{
  command : "stop",
  streamId: "stream1"
}
```    

## Playing WebRTC Stream

1.  Client connects to Ant Media Server through WebSocket.
    
        ws://SERVER_NAME:5080/WebRTCAppEE/websocket
    

2.  Client sends play JSON command to the server with stream name parameter. (Remove token parameter if token control is not enabled) Only ```command``` and ```streamId``` is mandatory, rest is situational, such as ```subscriber id```, ```code```, ```token``` and ```enabled tracks```. You can take a look at this [guide](https://antmedia.io/time-based-one-time-password-totp-makes-streams-secure/) to learn more about ```subscriberId``` and ```subscriberCode``` and how to use them. For general ```stream security``` information take a look at [here](/v1/docs/stream-security). When there are multitracks on the webrtc stream user can speficy the tracks that needs to be played. If there are 2 tracks on the stream, user can specify the both and both track will be played. To get all tracks in a stream you can take a look in ```getTrackList``` command that is in the [miscellaneous](#miscellaneous-websocket-methods) part.
    
```json
{
  command : "play",
  streamId : "stream1",
  token : "tokenId",
  trackList: [enabledtracksarray],
  subscriberId: "subscriberId",
  subscriberCode: "subscriberCode"
}
```   

3.  If Server accepts the stream, it replies back with offer command

```json 
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "offer",  
  sdp : "${SDP_PARAMETER}"
}
```    

5.  Client creates answer sdp and send the sdp configuration to the server with takeConfiguration command

```json
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "answer",  
  sdp : "${SDP_PARAMETER}"
}
```    

6.  Client and Server get ice candidates several times and sends to each other with takeCandidate command

```json 
{
  command : "takeCandidate",
  streamId : "stream1",
  label : "${CANDIDATE.SDP_MLINE_INDEX}",
  id : "${CANDIDATE.SDP_MID}",
  candidate : "${CANDIDATE.CANDIDATE}"
}
```        
    

7.  Clients sends stop JSON command to stop playing

```json
{
  command : "stop",
  streamId: "stream1",
}
```    

8.  Client sends toggle video to stop/start incoming video packets from a video track ( streamId = trackId for single track use ) Enabled is ```true``` as default. ```trackId``` and ```streamId``` is mandatory.

```json 
{
  command : "toggleVideo",
  streamId: "stream1",
  trackId: "track1",
  enabled: boolean
}
```    

9.  Client sends toggle audio to stop/start incoming audio packets from an audio track ( streamId = trackId for single track use ) Enabled is ```true``` as default. ```trackId``` and ```streamId``` is mandatory.

```json 
{
  command : "toggleAudio",
  streamId: "stream1",
  trackId: "track1",
  enabled: boolean
}
```    

## Peer to Peer WebRTC Stream

1.  Peers connects to Ant Media Server through WebSocket.
    
`ws://SERVER_NAME:5080/WebRTCAppEE/websocket`
    

2.  Client sends join JSON command to the server with stream name parameter. If only want to play, mode can be set to play, if user wants to publish and play at the same time, both can be set. Only ```command``` and ```streamId``` is mandatory. Mode and multiPeer is only for embedded SDK and should not be used unless you are using embedded SDK. As default, ```mode``` is set to ```both``` and multiPeer is ```false```.

```json
{
  command : "join",
  streamId : "stream1",
  multiPeer: boolean,
  mode: "play or both"
}
```    

If there is only one peer in the stream1, server waits for the other peer to join the room.
    

3.  When second peer joins the stream, server sends start JSON command to the first peer
    
```json 
{
  command : "start",
  streamId : "stream1",
}
```    

4.  First peer create offer sdp and send to the server with takeConfiguration command,
    
```json 
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "offer",  
  sdp : "${SDP_PARAMETER}"
}
 ```   
    Server relays the offer sdp to the second peer
    

5.  Second peer creates answer sdp and sends to the server with takeConfiguration command
```json     
{
  command : "takeConfiguration",
  streamId : "stream1",
  type : "answer",  
  sdp : "${SDP_PARAMETER}"
}
 ```   
    Server relays the answer sdp to the first peer
    

6.  Each peers get ice candidates several times and sends to each other with takeCandidate command through server
```json 
{
  command : "takeCandidate",
  streamId : "stream1",
  label : "${CANDIDATE.SDP_MLINE_INDEX}",
  id : "${CANDIDATE.SDP_MID}",
  candidate : "${CANDIDATE.CANDIDATE}"
}
 ```       
    

7.  Clients sends leave JSON command to leave the room
```json     
{
  command : "leave",
  streamId: "stream1"
}
```    

## Conference WebRTC Stream

Peers connects to Ant Media Server through WebSocket.
    
`ws://SERVER_NAME:5080/WebRTCAppEE/websocket`
    

Client sends join JSON command to the server with room name parameter. 

```streamId``` field is optional in case ```streamId``` should be specified in advance. If ```streamId``` is not sent, server returns with a random ```streamId``` in the second message.

 ```json    
{
  command : "joinRoom",
  room : "room_id_for_your_conference",
  streamId: "stream_id_you_want_to_use"
}
```    

Server notifies the client with available streams in the room

 ```json    
{
  command : "notification",
  definition : "joinedTheRoom",
  streamId: "unique_stream_id_returned_by_the_server"
  streams: [
      "stream1_in_the_room",
      "stream2_in_the_room",
      ...
  ]
}
 ```   
```streamId``` returned by the server is the stream id client uses to publish stream to the room. ```streams``` is the json array which client can play via WebRTC. Client can play each stream by play method above. This streams array can be empty if there is no stream in the room.
    

Web app should pull the server periodically for the room info as follows,

```json 
{
  command : "getRoomInfo",
  room : "room_id_for_your_conference",
  streamId: "unique_stream_id_returned_by_the_server"
}
 ```   

Server returns the active streams in the room as follows. Application should synchronize the players in their side.

 ```json    
{
  command:"roomInformation",
  room: "room_id_for_your_conference",
  streams: [
    "stream1_in_the_room",
    "stream2_in_the_room",
    ...
  ]
}
```    

Any user can leave the room by sending below message

```json 
{
  command : "leaveFromRoom",
  room: "roomName"
}
```    

## WebSocket Error Callbacks

```noStreamNameSpecified```: it is sent when stream id is not specified in the message.

 ```json    
{
  command : "error",
  definition : "noStreamNameSpecified",
}
 ```  
 
```not_allowed_unregistered_streams```: This is sent back to the user if the publisher wants to send a stream with an unregistered id and server is configured not to allow this kind of streams.

```json     
{
  command : "error",
  definition: "not_allowed_unregistered_streams",
}
 ```   

```no_room_specified```: This is sent back to the user when there is no room specified in joining the video conference.

```json     
{
  command : "error",
  definition : "no_room_specified",
}
 ```   
```unauthorized_access```: This is sent back to the user when the token is not validated.

```json     
{
  command : "error",
  definition : "unauthorized_access",
}
 ```   

```no_encoder_settings```: This is sent back to the user when there are no encoder settings available in publishing the stream.

 ```json    
{
  command : "error",
  definition : "no_encoder_settings",
}
 ```   

```no_peer_associated_before```: This is peer to peer connection error definition.It is sent back to the user when there is no peer associated with the stream.

```json     
{
  command : "error",
  definition : "no_peer_associated_before",
}
```  

```notSetLocalDescription```: It is sent when local description is not set successfully
```json  

{
  command : "error",
  definition : "notSetLocalDescription",
}
```   

```highResourceUsage```: It is sent when server is overloaded. Server overload means over CPU usage or over RAM usage. Over CPU usage means CPU load is more than the ```server.cpu_limit``` value in ```conf/red5.properties```. Its default value is %85. Over RAM usage means available memory in the system is less than ```server.min_free_ram``` value in conf/red5.properties. Its unit is MB and default value is 10.

 ```json    
{
  command : "error",
  definition : "highResourceUsage",
}
```   

```streamIdInUse```: The server sends this message if it detects that there is already an active stream(preparing or publishing state) with that same stream id when a user tries to publish a stream. One may get this error if s/he tries to re-publish a stream with the same stream id without closing the previous WebRTC connection.

```json     
{
  command : "error",
  definition : "streamIdInUse",
}
```    

```publishTimeoutError```: The server sends this message if WebRTC publishing is not started in a specified time period. This value is configurable via ```settings.webrtc.client.start.timeoutMs``` property in App Configuration. Its default value is 5000 milliseconds.

```json     
{
  command : "error",
  definition : "publishTimeoutError",
}
 ```
   
```invalidStreamName```: it is send when stream name contains special characters.

```json     
{
  command : "error",
  definition : "invalidStreamName",
}
```    

```data_store_not_available```: It's sent when data store is not available. It's not available if it's not initialized or closed.
```json     
{
  command : "error",
  definition : "data_store_not_available"
}
 ```   
```license_suspended_please_renew_license```: It's send when license is suspended
 ```json    
{
  command : "error",
  definition : "license_suspended_please_renew_license"
}
```    
```already_playing```: This is sent back to the user when a new play message received while it is playing or it is about to play
```json     
{
  command : "error",
  definition : "already_playing",
  streamId: "stream1"
}
 ```   
```already_publishing```: This is sent back to the user when a new publish message received while there it is publishing or it about to publish
 ```json    
{
  command : "error",
  definition : "already_publishing",
  streamId: "stream1"
}
```    
```no_codec_enabled_in_the_server```: This is sent back to the user when there is no codec enabled in the server and someone try to make a publish
```json     
{
  command : "error",
  definition : "no_codec_enabled_in_the_server",
  streamId: "stream1"
}
 ```   
```stream_not_active_or_expired```: This is sent back to the user when stream plannedStartDate and plannedEndDate values are in interval or not.
 ```json    
{
  command : "error",
  definition : "stream_not_active_or_expired",
}
```    
```viewerLimitReached```: This is send when viewer limit reached. when user is trying to watch a broadcast that already hit the limit, this error is send to client.
 ```json    
{
  command : "error",
  definition : "viewerLimitReached",
}
```    

## Miscellaneous WebSocket Methods

```ping``` & ```pong``` Whenever you send a ping command to the server, it will respond you with pong command. Ping Command
 ```json    
{
command : "ping",
}
```    
    Pong Response from Server
 ```json    
{
command : "pong",
}
```    
```getStreamInfo```: Get Stream Information from Server. You may use this method to learn more about stream status and bitrates. Client should send the following message.
```json     
{
 command: "getStreamInfo",
 streamId: "stream_id_that_you_want_to_get_info"
} 
```    
    Server returns in two ways. It may return stream information as follows
```json     
{
 command: "streamInformation",
 streamId: "stream_id_of_the_stream_information",
 streamInfo: [{
     streamWidth: resolution_width,
     streamHeight: resolution_height,
     videoBitrate: video_bitrate,
     audioBitrate: audio_bitrate,
     videoCodec: codec_of_the_video 
     },
   ...
   ]
}
```    
    If stream is not active, it will return no\_stream\_exist
```json     
{
command : "error",
definition : "no_stream_exist",
streamId: "id_of_the_stream"
}
```    
```getRoomInfo```: Get Room Information from server that returns the whole active streams in the room. Client should send the following message to get the response from the server.
```json     
{
 command: "getRoomInfo",
 room: "room_id_that_you_want_to_get_info",
 streamId: "server_returns_while_you_join_the_room"
} 
 ```   
    Server responds in following format
 ```json    
{
  command: "roomInformation",
  room: "room_id_that_this_information_belongs_to",
  streams: [ stream_id_1, stream_id_2, ...]
}
```    
```bitrateMeasurement```: Server periodically sends this information to the WebRTC viewers. It lets develop show a message to the user if it's internet bandwidth is not good enough. If the ```targetBitrate``` is bigger than the sum of ```videoBitrate``` and ```audioBitrate```, it means internet bandwidth is good enough to play the video. If the ```targetBitrate``` is less than the sum of ```videoBitrate``` and ```audioBitrate```, it means some playback issues(pixelating, packet drop, etc.) may happen and it disturbs the user experience.
```json     
{
  command : "notification",
  definition : "bitrateMeasurement",
  streamId: "unique_stream_id_returned_by_the_server"
  targetBitrate: measured_bandwidth_of_the_client,
  videoBitrate: video_bitrate_of_the_current_playing_video,
  audioBitrate: "audio_bitrate_of_the_current_playing_audio
}
```
    
```forceStreamQuality```: If there are adaptive-bitrates(multi-bitrate) for that stream, you can get bitrates with ```getStreamInfo``` method and then you can make the Ant Media Server force to send you a specific resolution. If you want to switch back to auto stream quality, you can use give ```0``` for ```streamHeight``` and send the message below.
    
{
command: "forceStreamQuality",
streamId: "write_the_stream_id",
streamHeight: write_the_height_of_the_resolution_you_want_to_force
}
    
```server_will_stop```: It's sent when server is in shutdown process.
    
{
        
command : "notification",
definition : "server_will_stop"
}
    
```leavedFromRoom```: It's sent after stop command received or if client sents leaveFromRoom command.
    
{
        
command : "notification",
definition : "leavedFromRoom",
ATTR_ROOM_NAME: "roomName"
}
    
```getTrackList```: Sends a request to server to get track list in specified stream. Token is not mandatory. If stream has token, token needs to be used, otherwise not needed.
    
{
        
command : "getTrackList",
streamId: "stream1",
token: "token"
}
    
```trackList```: Server returns track list of the specified stream after receiving getTrackList command.
    
{
        
command : "trackList",
streamId: "stream1",
trackList: [tracks]
}
    
```enableTrack```: Publisher can enable or disable the tracks in the broadcast.
    
{
command : "enableTrack",
streamId: "stream1",
trackId: "id of track",
enabled: boolean
}