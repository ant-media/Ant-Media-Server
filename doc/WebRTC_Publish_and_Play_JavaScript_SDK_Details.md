# WebRTC Publish & Play JavaScript SDK

Ant Media Server provides WebSocket interface in publishing and playing WebRTC streams. In this document, 
we will show both how to publish and play WebRTC streams by using JavaScript SDK. 

## How to Publish WebRTC stream with JavaScript SDK
Let's see how to make it step by step

1. Load the below scripts in head element of the html file
```html
<head>
...
<script src="https://webrtc.github.io/adapter/adapter-latest.js"></script>
<script src="js/webrtc_adaptor.js" ></script>
...
</head>
```

2. Create local video element somewhere in the body tag
```html
<video id="localVideo" autoplay muted width="480"></video>
```


3. Initialize the `WebRTCAdaptor` object in script tag

```javascript
	var pc_config = null;

	var sdpConstraints = {
		OfferToReceiveAudio : false,
		OfferToReceiveVideo : false

	};
	var mediaConstraints = {
		video : true,
		audio : true
	};

	var webRTCAdaptor = new WebRTCAdaptor({
		websocket_url : "ws://" + location.hostname + ":8081/WebRTCApp4",
		mediaConstraints : mediaConstraints,
		peerconnection_config : pc_config,
		sdp_constraints : sdpConstraints,
		localVideoId : "localVideo",
		callback : function(info) {
			if (info == "initialized") {
				console.log("initialized");
				
			} else if (info == "publish_started") {
				//stream is being published 
				console.log("publish started");
				
				
			} else if (info == "publish_finished") {
				//stream is finished
				console.log("publish finished");
				
			}
		},
		callbackError : function(error) {
			//some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError

			console.log("error callback: " + error);
			alert(error);
		}
	});
```

4. Call `publish(streamName)` to Start Publishing


In order to publish WebRTC stream to Ant Media Server, WebRTCAdaptor's `publish(streamName)` function should be called. 
You can choose the call this function in *success callback function* when the info parameter is having value "initialized" 

```javascript
 if (info == "initialized")  
 {  
  // it is called with this parameter when it connects to                            
  // Ant Media Server and everything is ok 
  console.log("initialized");
  webRTCAdaptor.publish("stream1");
 }
```
5. Call `stop()` to Stop Publishing

You may want to stop publishing anytime by calling stop function of WebRTCAdaptor

```javascript
webRTCAdaptor.stop()
```

#### Sample
Please take a look at the WebRTCApp4/index.html file in order to see How JavaScript SDK can be used for publishing a stream

## How to Play WebRTC stream with JavaScript SDK

1. Load the below scripts in head element of the html file
```html
<head>
...
<script src="https://webrtc.github.io/adapter/adapter-latest.js"></script>
<script src="js/webrtc_adaptor.js" ></script>
...
</head>
```

2. Create remote video element somewhere in the body tag
```html
<video id="remoteVideo" autoplay controls></video>
```


3. Initialize the `WebRTCAdaptor` object like below in script tag
```javascript
  var pc_config = null;

	var sdpConstraints = {
		OfferToReceiveAudio : true,
		OfferToReceiveVideo : true

	};
	var mediaConstraints = {
		video : true,
		audio : true
	};
	
	var webRTCAdaptor = new WebRTCAdaptor({
		websocket_url : "ws://" + location.hostname + ":8081/WebRTCApp4",
		mediaConstraints : mediaConstraints,
		peerconnection_config : pc_config,
		sdp_constraints : sdpConstraints,
		remoteVideoId : "remoteVideo",
		isPlayMode: true,
		callback : function(info) {
			if (info == "initialized") {
				console.log("initialized");
			
			} else if (info == "play_started") {
				//play_started
				console.log("play started");
			
			} else if (info == "play_finished") {
				// play finishedthe stream
				console.log("play finished");
				
			}
		},
		callbackError : function(error) {
			//some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError

			console.log("error callback: " + error);
			alert(error);
		}
	});
```  
4. Call `play(streamName)` to Start Playing


In order to play WebRTC stream to Ant Media Server, WebRTCAdaptor's `play(streamName)` function should be called. 

You can choose the call this function in *success callback function* when the info parameter is having value "initialized" 

```javascript
 if (info == "initialized")  
 {  
  // it is called with this parameter when it connects to                            
  // Ant Media Server and everything is ok 
  console.log("initialized");
  webRTCAdaptor.play("stream1");
 }
```

5. Call `stop()` to Stop Playing

You may want to stop play anytime by calling stop function of WebRTCAdaptor

```javascript
webRTCAdaptor.stop()
```

#### Sample
Please take a look at the WebRTCApp4/player.html file in order to see How JavaScript SDK can be used for playing a stream
