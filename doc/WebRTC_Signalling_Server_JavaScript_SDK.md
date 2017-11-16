
# WebRTC Signalling Server 
Ant Media Server can also be used as a signalling server for peer to peer connections. WebSocket is used for connection
between peers and Ant Media Server. 

In order to use Ant Media Server as a WebSocket you just need to use an app that provides this feature. If you do not know, 
how to do that drop an email to contact at antmedia dot io

## JavaScript SDK
Of course there is a JavaScript SDK in order to make using signalling server straight forward. 
There is a sample peer.html file in the sample app, you can also try it to understand how to use JavaScript SDK 

### How to use JavaScript SDK
JavaScript SDK is so easy, just create `WebRTCAdaptor` object and call `join(roomName)` function. 
Let's see how to make it step by step

#### Load the below scripts in head element of the html file. 

```
<head>
...
<script src="https://webrtc.github.io/adapter/adapter-latest.js"></script>
<script src="js/webrtc_adaptor.js" ></script>
...
</head>
```

#### Create video elements somewhere in the body tag
```
<video id="localVideo" autoplay muted width="480"></video>
<video id="remoteVideo" autoplay controls width="480"></video>
```

First video tag is for local video and the second video tag for remote video.

#### Initialize the `WebRTCAdaptor object in script tag
```
<script>
    var pc_config = null;
	
	  var sdpConstraints = 
	  {
		  OfferToReceiveAudio : true,
		  OfferToReceiveVideo : true	
	  };
	  var mediaConstraints = {
	          video: true,
	          audio: true
	        };
	
	  var webRTCAdaptor = new WebRTCAdaptor({
		  websocket_url:"ws://" + location.hostname + ":8081/WebRTCApp4",  // url of the WebSocket Signalling Server
		  mediaConstraints: mediaConstraints, 
		  peerconnection_config: pc_config,
		  sdp_constraints: sdpConstraints,
		  localVideoId: "localVideo",   // id of the local video tag
		  remoteVideoId: "remoteVideo",  // id of the remote video tag
		  
		  callback: function(info) {     // *success callback function*
			  
                    if (info == "initialized")  
                    {  
                        // it is called with this parameter when it connects to                            
                        // signalling server and everything is ok 
                        console.log("initialized");
                    }
                    else if (info == "joined")
                    { 
                       // it is called with this parameter when it joins a room
                       console.log("joined");
                    }
                    else if (info == "leaved")
                    {
                        // it is called with this parameter when it leaves from room
                        console.log("leaved");
                    }
                  },
		  callbackError: function(error) {  
                    // error callback function it is called when an error occurs
                    //some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError
                    console.log("error callback: " + error);
                    alert(error);
		  }
	  });
</script>
```

#### Call `join` function
In order to create a connection between peers, each peer should join the same room by calling `join(roomName)` function of
the WebRTCAdaptor. When there are two peers in the same room, signalling starts automatically and peer to peer connection
is established.

For instance, you can call join function in *success callback function* when the info parameter is having value "initialized" 

```
 if (info == "initialized")  
 {  
  // it is called with this parameter when it connects to                            
  // signalling server and everything is ok 
  console.log("initialized");
  webRTCAdaptor.join("room1");
 }
```

According to code above, when peer.html file is opened by two peers, they will joined the "room1" and peer to peer connection will be established. 


#### Functions 
As shown above, main object is WebRTCAdaptor so that let's look at its functions

* `join(roomName)` :

    Lets peer join to a room specified in the parameter, if operation is successfull then callback function is called with
info parameter having "joined" value. When there are two people in the same room, signalling starts automatically 
and peer to peer connection is established
* `leave()`: 

    Lets peer leave the room it joined previously. If operation is successfull then callback function is called
with info parameter having "leaved" value
* `turnOnLocalCamera()`: 

    Lets the local camera turn on and add the video stream to peer connection
* `turnOffLocalCamera()`: 

    Lets the local camera turn off and remove the video stream from peer connection
* `unmuteLocalMic()`: 

    Lets the local mic unmute and add the audio stream to peer connection
* `muteLocalMic()`: 

   Lets the local mic mute and remove the audio stream from peer connection



#### Sample
Please take a look at the WebRTCApp4/peer.html file in order to see How JavaScript SDK can be used



