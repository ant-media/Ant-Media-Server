# WebRTC conference call

In this documentation, we're going to explain how to implement WebRTC video conference call with JavaScript SDK. Please remember that the WebRTC Video Conference feature is only available in Ant Media Server Enterprise Edition.

There is already a working demo for this. You can try it using the following URL `https://domain-name.com:5443/LiveApp/conference.html`.     

```conference.html``` is located under ```/usr/local/antmedia/webapps/LiveApp/``` directory.

Let’s proceed step by step on how to implement a simple conference call. Before starting the implementation, make sure that you've installed SSL in your Ant Media Server Enterprise Edition. 

If you haven’t got any domain for your Ant Media Server, you can get a free domain in [Freenom](https://www.freenom.com/).

> Quick Link: [Learn How to Install SSL to your Ant Media Server](/v1/docs/setting-up-ssl)

## Preparing the web page

Please go to ```LiveApp``` application folder which is under ```/usr/local/antmedia/webapps/LiveApp``` and create a file named ```conference_call.html```.

![](@site/static/img/image-1645105628540.png)

Include the following Javascript files in the head section of the HTML page.
    
```html 
<head>
...
<script src="https://webrtc.github.io/adapter/adapter-latest.js"></script>
<script src="js/webrtc_adaptor.js"></script>
...
</head>
```
    
Include the following code in your ```<body>``` of the HTML page.
    
```html 
<body>

<div class="jumbotron">
<div id="players">
<div class="col-sm-3">
<video id="localVideo" autoplay muted controls playsinline></video>
</div>
</div>

<div class="row">
<div class="col-sm-6 col-sm-offset-3">
<p>
<input type="text" class="form-control" value="room1" id="roomName" placeholder="Type room name">
</p>
<p>
<button onclick="joinRoom()" class="btn btn-info" disabled id="join_publish_button">Join Room</button>
<button onclick="leaveRoom()" class="btn btn-info" disabled id="stop_publish_button">Leave Room</button>
</p>

<div style="padding:10px">
<button id="turn_off_camera_button" onclick="turnOffLocalCamera()" class="btn btn-default"  >Turn off Camera</button>
<button id="turn_on_camera_button" disabled onclick="turnOnLocalCamera()" class="btn btn-default"  >Turn on Camera</button>

<button id="mute_mic_button" onclick="muteLocalMic()" class="btn btn-default"  >Mute Local Mic</button>
<button id="unmute_mic_button" disabled onclick="unmuteLocalMic()" class="btn btn-default"  >Unmute Local Mic</button>
</div>
<span class="label label-success" id="broadcastingInfo" style="font-size: 14px; display: none" style="display: none">Publishing</span>
</div>
</div>
</div>

</body>
```
    
Include required JavaScript code in your page. Please take a look at the full JavaScript code at [conference.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/conference.html). In the example below, we explain the callbacks.

```js  
<script>
var webRTCAdaptor = new WebRTCAdaptor(
{
        websocket_url : websocketURL,
        mediaConstraints : mediaConstraints,
        peerconnection_config : pc_config,
        sdp_constraints : sdpConstraints,
        localVideoId : "localVideo",
        isPlayMode : playOnly,
        debug : true,
        callback : function(info, obj) {
                if (info == "initialized") 
                {
                        //called by JavaScript SDK when WebSocket is connected. 
                } 
                else if (info == "joinedTheRoom") 
                {
                        //called when this client is joined the room
                        //obj contains streamId field which this client can use to publish to the room.
                        //obj contains also the active streams in the room so that you can play them directly.
                        //In order to get the streams in the room periodically call getRoominfo
                } 
                else if (info == "newStreamAvailable") 
                {
                        //called when client is ready to play WebRTC stream.
                } 
                else if (info == "publish_started") 
                {
                        //called when stream publishing is started for this client		
                } 
                else if (info == "publish_finished") 
                {
                        //called when stream publishing has finished for this client
                } 
                else if (info == "leavedFromRoom") 
                {
                        //called when this client is leaved from the room  	
                } 
                else if (info == "closed") 
                {
                        //called when websocket connection is closed	
                } 
                else if (info == "play_finished") 
                {
                        //called when a stream has finished playing	
                } 
                else if (info == "streamInformation") 
                {
                        //called when a stream information is received from the server. 
                        //This is the response of getStreamInfo method		
                } 
                else if (info == "roomInformation") 
                {
                        //Called by response of getRoomInfo when a room information is received from the server.
                        //It contains the array of the streams in the room.
                }
                else if (info == "data_channel_opened") 
                {
                        //called when data channel is opened
                } 
                else if (info == "data_channel_closed") 
                {
                        // called when data channel is closed
                } 
                else if(info == "data_received") 
                {
                        //called when data is received through data channel
                }
                },
                callbackError : function(error, message) {
        //some of the possible errors, NotFoundError, SecurityError,PermissionDeniedError
        }
});
</script>
```
    

## Joining a room

When WebRTCAdaptor is initialized successfully, it creates a websocket connection. After a successful connection, the client gets the ```initialized``` notification from the server. After receiving ```initialized``` notification, you can call ```joinRoom``` method.

```js
 webRTCAdaptor.joinRoom(roomId , streamId);
 ```

```joinRoom``` method gets two parameters:

*   ```roomId``` (mandatory): The room id. If this room is not available, it will be created.
*   ```streamId``` (optional): The stream id of the stream that this client would want to publish to.

## Publishing a stream to a room**

Just call publish method as follows.

```js
webRTCAdaptor.publish(streamId, token);
```

```publish``` method gets two parameters: \*```streamId```:(Mandatory) Stream id of the stream.

*   ```token```: It's required only when one-time token is enabled.

## Playing a stream in a room**

In order to play a stream, call the play method. In a conference call, the correct place to call method is when ```joinedTheRoom``` and ```roomInformation``` notifications are received.

```js
webRTCAdaptor.play(streamId, token);
```

```play``` method gets two parameters: 

*   ```streamId``` (mandatory): Stream id of the stream.

*   ```token```: It's required only when one-time token is enabled.

## Turning the camera on/off**

To turn off the camera, call the ```turnOffLocalCamera``` method.

```js
webRTCAdaptor.turnOffLocalCamera();
```

This method takes no parameters.

If your camera is turned off, call ```turnOnLocalCamera``` to turn it on.

```js
webRTCAdaptor.turnOffLocalCamera();
```

## Muting and unmuting the microphone

Call ```muteLocalMic``` to mute the microphone.

```js
webRTCAdaptor.muteLocalMic();
```

To unmute, call ```unmuteLocalMic```.

```js
webRTCAdaptor.unmuteLocalMic();
```

These methods don't take any parameters.

## Server notifications

Here are the conference related notifications that callback is invoked for. Please check the [conference.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/conference.html) for proper usage.

*   ```joinedTheRoom```: Called when web socket is connected. It has parameter that contains stream id for publishing and streams array in order to play the stream.
*   ```newStreamAvailable```: Called when a previously joined stream is ready to play.
*   ```roomInformation```: Called when ```getRoomInfo``` is sent by client. It returns the active streams in the room.
*   ```leavedFromRoom```: Called when this client leaves the room.