# Sharing screen with WebRTC

Seamless switch between WebRTC screen sharing and streaming using a camera is available on both Community Edition and and Enterprise Edition. This means that you can switch between screen and camera within the same session.

Note that this process doesn't require any extra plugins or additional software to be installed on the browser side.

## Try WebRTC screen sharing without plugin

First of all, you should have a ```getDisplayMedia``` supported browser. You can see a list of ```getDisplayMedia``` supported browsers in [this link](https://caniuse.com/#search=getDisplayMedia).

Visit the WebRTC publishing web page which is ```https://domainAddress:5443/WebRTCApp``` (Community Edition) or ```https://domainAddress:5443/WebRTCAppEE``` (Enterprise Edition). Before this, you need to assign a domain name to your server and [install SSL](/v1/docs/ssl-setup). Otherwise, your browser won't let you access the camera or screen.

> Quick Link: [Learn How to Install SSL to your Ant Media Server](/v1/docs/setting-up-ssl)

## Using WebRTC Screen Sharing 

You can see simple functions in [js/webrtc\_adaptor.js](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/js/webrtc_adaptor.js) file for a seamless switch between screen sharing and camera. You can take a look at the source code of [WebRTCApp/index.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/index.html)  to see a full implementation.

First of all, there is a new callback with ```browser_screen_share_supported```. If the callback method is called with this parameter, your browser screen share functionality is ready to use.

```js
var webRTCAdaptor = new WebRTCAdaptor({
	websocket_url : websocketURL,
	mediaConstraints : mediaConstraints,
	peerconnection_config : pc_config,
	sdp_constraints : sdpConstraints,
	localVideoId : "localVideo",
	debug:true,
	callback : function(info, obj) {
		if (info == "initialized") {
			console.log("initialized");
			start_publish_button.disabled = false;
			stop_publish_button.disabled = true;
		} else if (info == "publish_started") {
			//stream is being published
			console.log("publish started");
			start_publish_button.disabled = true;
			stop_publish_button.disabled = false;
			startAnimation();
		} else if (info == "publish_finished") {
			//stream is being finished
			console.log("publish finished");
			start_publish_button.disabled = false;
			stop_publish_button.disabled = true;
		}
		else if (info == "browser_screen_share_supported") {
			screen_share_checkbox.disabled = false;
			console.log("browser screen share supported");
			browser_screen_share_doesnt_support.style.display = "none";
		}
		else if (info == "screen_share_stopped") {
			console.log("screen share stopped");
		}
		else if (info == "closed") {
			//console.log("Connection closed");
			if (typeof obj != "undefined") {
				console.log("Connecton closed: " + JSON.stringify(obj));
			}
		}
		else if (info == "pong") {
			//ping/pong message are sent to and received from server to make the connection alive all the time
			//It's especially useful when load balancer or firewalls close the websocket connection due to inactivity
		}
		else if (info == "refreshConnection") {
			startPublishing();
		}
		else if (info == "ice_connection_state_changed") {
			console.log("iceConnectionState Changed: ",JSON.stringify(obj));
		}
		else if (info == "updated_stats") {
			//obj is the PeerStats which has fields
				//averageOutgoingBitrate - kbits/sec
			//currentOutgoingBitrate - kbits/sec
			console.log("Average outgoing bitrate " + obj.averageOutgoingBitrate + " kbits/sec"
					+ " Current outgoing bitrate: " + obj.currentOutgoingBitrate + " kbits/sec");
				
		}
	},
	callbackError : function(error, message) {
		//some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError
		
		console.log("error callback: " +  JSON.stringify(error));
		var errorMessage = JSON.stringify(error);
		if (typeof message != "undefined") {
			errorMessage = message;
		}
		var errorMessage = JSON.stringify(error);
		if (error.indexOf("NotFoundError") != -1) {
			errorMessage = "Camera or Mic are not found or not allowed in your device";
		}
		else if (error.indexOf("NotReadableError") != -1 || error.indexOf("TrackStartError") != -1) {
			errorMessage = "Camera or Mic is being used by some other process that does not let read the devices";
		}
		else if(error.indexOf("OverconstrainedError") != -1 || error.indexOf("ConstraintNotSatisfiedError") != -1) {
			errorMessage = "There is no device found that fits your video and audio constraints. You may change video and audio constraints"
		}
		else if (error.indexOf("NotAllowedError") != -1 || error.indexOf("PermissionDeniedError") != -1) {
			errorMessage = "You are not allowed to access camera and mic.";
		}
		else if (error.indexOf("TypeError") != -1) {
			errorMessage = "Video/Audio is required";
		}
		else if (error.indexOf("ScreenSharePermissionDenied") != -1) {
			errorMessage = "You are not allowed to access screen share";
			screen_share_checkbox.checked = false;
		}
		else if (error.indexOf("WebSocketNotConnected") != -1) {
			errorMessage = "WebSocket Connection is disconnected.";
		}
		alert(errorMessage);
	}
});
```

## Switching to desktop screen share

If your browser supports ```getDisplayMedia```, you only need to call ```webRTCAdaptor.switchDesktopCapture(streamId)``` function to switch to screen sharing

```js
webRTCAdaptor.switchDesktopCapture(streamId);
```

Please take a look at the sample page code at [index.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/index.html) file on Github.

## Switching to screen share with camera

You only need to call ```webRTCAdaptor.switchDesktopCaptureWithCamera(streamId)``` function to switch to screen sharing with camera.

```js
webRTCAdaptor.switchDesktopCaptureWithCamera(streamId);
```

## Switching back to the camera

To switch back to the camera, just call ```webRTCAdaptor.switchVideoCapture(streamId)```.

```js
webRTCAdaptor.switchVideoCapture(streamId);
```

## Share desktop audio with Chrome's screen share with audio option

Click on Chrome's share audio option while sharing screen. This way, the adaptor will start sharing the audio muting the microphone. If you want to mix the desktop and microphone voice, follow the instructions:

*   Go to webrtc\_adaptor.js , find ```captureScreenSound(stream, micStream,streamId)```
*   Under the method adjust the gains which are;

```desktopSoundGainNode.gain.value = (Some value between 0 and 1);```

```micGainNode.gain.value = (Some value between 0 and 1);```

Please take a look at sample code at [index.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/index.html) in Github.