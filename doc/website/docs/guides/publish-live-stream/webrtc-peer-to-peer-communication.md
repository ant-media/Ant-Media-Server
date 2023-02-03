---
title: WebRTC Peer to Peer Communication
---
# WebRTC peer to peer communication

In this documentation, we're going to explain how to implement WebRTC peer to peer communication with JavaScript SDK. There is already a working demo for this in peer.html file under your AMS installation URL:

    https://domain-name.com:5443/LiveApp/peer.html

File is located under ```/usr/local/antmedia/webapps/LiveApp/peer.html```

> WebRTC video Conference is available in the Enterprise Edition.

## Implementation of Peer to Peer Communication

 Let’s proceed step by step for implementing a peer to peer call. Before starting the implementation, make sure that you've installed SSL to your Ant Media Server Enterprise Edition. If you haven’t got any domain for your Ant Media Server, you can get a free domain in [Freenom](https://www.freenom.com/).

> Quick Link: [Learn How to Install SSL to your Ant Media Server](/v1/docs/setting-up-ssl)

### Preparing the web page

Please go to ```LiveApp``` application folder which is under ```/usr/local/antmedia/webapps/LiveApp``` and create a file named ```peer.html```.

![](@site/static/img/image-1645111368769.png)

Include JavaScript files to your page in the header as follows.

```html
<head>
...
<script src="https://webrtc.github.io/adapter/adapter-latest.js">``</script>`
<script src="js/webrtc_adaptor.js">``</script>`
...
</head>
```

Include the following lines in your page body as follows.

```html
<body>
...
<video id="localVideo" autoplay muted width="480"></video>
<video id="remoteVideo" autoplay controls width="480"></video>
<br /> <br />
<div class="input-group col-sm-offset-2 col-sm-8">
<input type="text" class="form-control" value="stream1" id="streamName" placeholder="Type stream name"> <span class="input-group-btn">
<button onclick="join()" class="btn btn-default" disabled id="join_button">Join</button>
<button onclick="leave()" class="btn btn-default" disabled id="leave_button">Leave</button>
</span>
</div>
<div style="padding:10px">
<button onclick="turnOffLocalCamera()" class="btn btn-default"  >Turn off Camera</button>
<button onclick="turnOnLocalCamera()" class="btn btn-default"  >Turn on Camera</button>

<button onclick="muteLocalMic()" class="btn btn-default"  >Mute Local Mic</button>
<button onclick="unmuteLocalMic()" class="btn btn-default"  >Unmute Local Mic</button>
</div>
...
</body>
```

Include the following JavaScript code in your page. Please take a look at the full JS code at [peer.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/peer.html).

```js
 <script>
...
Define Media Source variable, SDP variable and etc.

Define websocketURL your URL.
var websocketURL = "wss://domain-name.com:5443/WebRTCAppEE/websocket";

var webRTCAdaptor = new WebRTCAdaptor({
      websocket_url: websocketURL,
      mediaConstraints: mediaConstraints,
      peerconnection_config: pc_config,
      sdp_constraints: sdpConstraints,
      localVideoId: "localVideo",
      remoteVideoId: "remoteVideo",
      callback: function(info) {
        if (info == "initialized") {
          console.log("initialized");
        }
        else if (info == "joined") {
          //joined the stream
          console.log("joined");
        }
        else if (info == "leaved") {
          //leaved the stream
          console.log("leaved");
        }
      },
      callbackError: function(error) {
        //some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError
        
        console.log("error callback: " + error);
        alert(error);
      }
    });
  
...
</script>
```

### Joining a peer to peer communication

When WebRTCAdaptor is initialized successfully, it creates a web socket connection. After a successful connection, the client gets the initialized notification from the server. After receiving ```initialized``` notification, you can call ```join``` method.

    webRTCAdaptor.join(streamId);

```join``` method gets one parameter:

*   ```streamId``` (mandatory): The id of the peer to peer connection that this client would join.

If ```join``` method returns successful, the server responds with ```joined``` notification. As a result ```callback``` method is called with joined notification.

### Leaving from a peer to peer communication**

When you want to leave from a peer to peer connection, just call the ```leave``` method.

    webRTCAdaptor.leave(streamId);

 ```leave``` method gets one parameter:

```streamId``` (mandatory): The id of the peer to peer connection that this client would leave from.

### Auxiliary methods

JavaScript SDK provides several auxiliary methods to provide enough flexibility in your application.

```turnOffLocalCamera```: Turn off the local camera in WebRTC peer to peer communication.

    webRTCAdaptor.turnOffLocalCamera();

```turnOnLocalCamera```: Turn on the local camera in WebRTC peer to peer communication.

    webRTCAdaptor.turnOnLocalCamera();

```muteLocalMic```: Mutes the local microphone in WebRTC peer to peer communication.

    webRTCAdaptor.muteLocalMic();

```unmuteLocalMic```: Unmute the local microphone in WebRTC peer to peer communication.

    webRTCAdaptor.unmuteLocalMic();

### TURN server

In some cases, peer to peer communication cannot be established and a relay server is required for video/audio transmission. For this requirement, TURN servers are needed to relay the video/audio.

![](@site/static/img/dataPathways.png)

[Coturn](https://github.com/coturn/coturn) can be used as a TURN server. You can enter TURN server credentials in [peer.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/peer.html) as follows.

```js
var pc_config =
{
  'iceServers' : 
      [ {
        'urls' : 'turnServerURL',
        'username' : 'turnServerUsername',
        'credential' : 'turnServerCredential'
      } ]
};
```