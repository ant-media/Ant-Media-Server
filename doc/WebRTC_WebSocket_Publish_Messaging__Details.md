# WebRTC WebSocket Messaging Details
This documentation is for developers who needs to implement signalling between Ant Media Server and clients 
for publishing & playing streams. Let's make it step by step

## Publishing WebRTC Stream

1. Client connects to Ant Media Server through WebSocket. URL of the WebSocket interface is something like

```
ws://SERVER_NAME:8081/WebRTCApp4
```
8081 is the port number of WebSocket and `WebRTCApp4` is the name of the app on the server.

2. Client sends publish JSON command to the server with stream name parameter. 

```json
{
    command : "publish",
    streamName : "stream1",
}
```

3. If Server accepts the stream, it replies back with start command
```json
{
    command : "start",
}
```

4. Client inits peer connections, creates offer sdp and send the sdp configuration 
to the server with takeConfiguration command
```json
{
   command : "takeConfiguration",
   streamName : "stream1",
   type : "offer",  
   sdp : SDP_PARAMETER
}
```

5. Server creates answer sdp and send the sdp configuration to the client with takeConfiguration command
```json
{
   command : "takeConfiguration",
   streamName : "stream1",
   type : "answer",  
   sdp : SDP_PARAMETER
}
```
6. Client and Server get ice candidates several times and sends to each other with takeCandidate command
```json
{
    command : "takeCandidate",
    streamName : "stream1",
    label : CANDIDATE.SDP_MLINE_INDEX,
    id : CANDIDATE.SDP_MID,
    candidate :CANDIDATE.CANDIDATE
}

```

7. Clients sends stop JSON command to stop publishing
```json
{
    command : "stop",
}
```
