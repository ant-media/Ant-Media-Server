# Javascript SDK

Ant Media Server provides a WebSocket interface in publishing and playing WebRTC streams. In this document, you will learn the basics of WebRTC JavaScript SDK and JavaScript Error Callbacks.

### WebRTCAdaptor methods

```WebRTCAdaptor``` object has the following methods:

*   ```getUserMedia(mediaConstraints, audioConstraint)```: It's called to get access to audio and video sources in the browser.
*   ```openScreen(audioConstraint, openCamera)```: It's called to access the screen sharing session.
*   ```openStream(mediaConstraints)```: It's called to open screen, camera or audio resources.
*   ```closeStream(streamId)```: It's called to close streams. If you want to stop peer connection, call stop(streamId).
*   ```checkExtension()```: It's called to check chrome screen share extension is available. If exists it call callback with "screen\_share\_extension\_available".
*   ```enableMicInMixedAudio(enable)```: It's called enable Microphone in Mixed Audio.
*   ```publish(streamId, token)```: It's called to publish stream.
*   ```joinRoom(roomName, streamId)```: It's called to Join Room function in N to N WebRTC Streaming.
*   ```play(streamId, token, roomId)```: It's called when playing the stream in N to N WebRTC.
*   ```stop(streamId)```: It's called to stop the stream.
*   ```join(streamId)```: It's called to join the stream.
*   ```leaveFromRoom(roomName)```: It's called to leave from the room.
*   ```leave(streamId)```: It's called to leave the stream.
*   ```getStreamInfo(streamId)```: It's called to return information about a stream.
*   ```gotStream(stream)```: It's called to reassign stream values.
*   ```switchVideoCapture(streamId)```: It's called for provide to switch video capture.
*   ```switchDesktopCapture(streamId)```: It's called for switch desktop capture.
*   ```switchVideoSource(streamId, mediaConstraints, onEndedCallback)```: It's called to switch video source.
*   ```onTrack(event, streamId)```: It's called to track to peer connections.
*   ```iceCandidateReceived(event, streamId)```: It's called for ice candidate messages received.
*   ```initPeerConnection(streamId)```: It's called for starting Init peer connection.
*   ```closePeerConnection(streamId)```: It's called for close peer connection.
*   ```signallingState(streamId)```: It's called to return signaling state.
*   ```iceConnectionState(streamId)```: It's called to return Ice connection state.
*   ```gotDescription(configuration, streamId)```: It's called to check peer connection descriptions.
*   ```turnOffLocalCamera()```: It's called to turn off the local camera.
*   ```turnOnLocalCamera()```: It's called to turn on the local camera.
*   ```muteLocalMic()```: It's called to mute local mic.
*   ```unmuteLocalMic()```: It's called to unmute local mic. If there is audio it calls callbackError with "AudioAlreadyActive" parameter.
*   ```takeConfiguration(idOfStream, configuration, typeOfConfiguration)```: It's called to take configuration.
*   ```takeCandidate(idOfTheStream, tmpLabel, tmpCandidate)```: It's called to take candidate.
*   ```addIceCandidate(streamId, candidate)```: It's called to add ice candidate.
*   ```startPublishing(idOfStream)```: It's called to start Publishing.
*   ```getVideoSender(streamId)```: It's called to get Video Sender. If we have multiple video tracks in coming versions, this method may cause some issues.
*   ```changeBandwidth(bandwidth, streamId)```: It's called to change bandwidth is in kbps.
*   ```getStats(streamId)```: It's called for return WebRTC stats.
*   ```enableStats(streamId)```: It's called to enable stats, setting interval 5 sec.
*   ```closeWebSocket(streamId)```: It's called to close WebSocket connection. After calling this function, create a new WebRTCAdaptor instance, don't use the same objectone. \* Because all streams are closed on the server side as well when WebSocket connection is closed.

This documentation is for developers who need to callbacks and their descriptions for WebRTC operations.

### WebRTC JavaScript Info Callbacks

*   ```initialized```: WebSocket connection is initialized successfully in this state.
*   ```publish_started```: WebRTC stream publishing has been started in this state.
*   ```publish_finished```: WebRTC stream publishing has been finished in this state.
*   ```screen_share_extension_available```: Screen Share extension is available in this state.
*   ```screen_share_stopped```: Screen Share is stopped in this state.
*   ```closed```: The webSocket connection is closed in this state.
*   ```pong```: When the client sends a ping message, the server answers a pong message.
*   ```refreshConnection```: When WebSocket connection is refreshed and the stream is published in this state.
*   ```ice_connection_state_changed```: If Ice Connection is changed, the server sends changed info.
*   ```updated_stats```: When Peer stats are changed, the server sends updated values.

### WebRTC JavaScript Error Callbacks

*   ```WebSocketNotSupported```: WebSocket connection is not supported for environment or connection is not in the correct state.
*   ```AbortError```: Although the user and operating system both granted access to the hardware device, and no hardware issues occurred that would cause a NotReadableError, some problems occurred which prevented the device from being used.
*   ```NotAllowedError```: The user has specified that the current browsing instance is not permitted access to the device, or the user has denied access for the current session, or the user has denied all access to user media devices globally.
*   ```NotFoundError```: No media tracks of the type specified were found that satisfy the given constraints.
*   ```OverconstrainedError```: The specified constraints resulted in no candidate devices which met the criteria requested. The error is an object of type OverconstrainedError and has a constraint property whose string value is the name of a constraint that was impossible to meet, and a message property containing a human-readable string explaining the problem.
*   ```SecurityError```: User media support is disabled on the Document on which getUserMedia() was called. The mechanism by which user media support is enabled and disabled is left up to the individual user agent.
*   ```AudioAlreadyActive```: If there is audio it calls callbackError with "AudioAlreadyActive.
*   ```Camera or Mic is being used by some other process that does not let read the devices```: Error definition it is sent when media devices are used by another application.
*   ```VideoAlreadyActive```: If there is a video, it calls callbackError with "VideoAlreadyActive".
*   ```NotSupportedError```: Error definition is sent when SSL is needed.
*   ```noStreamNameSpecified```: Error definition it is sent when stream id is not specified in the message.
*   ```not_allowed_unregistered_streams```: This is sent back to the user if the publisher wants to send a stream with an unregistered id and the server is configured not to allow this kind of stream.
*   ```no_room_specified```: This is sent back to the user when there is no room specified for joining the video conference.
*   ```unauthorized_access```: This is sent back to the user when the token is not validated.
*   ```no_encoder_settings```: This is sent back to the user when there are no encoder settings available in publishing the stream.
*   ```no_peer_associated_before```: This is peer to peer connection error definition. It is sent back to the user when there is no peer associated with the stream.
*   ```notSetLocalDescription```: It is sent when the local description is not sent successfully.
*   ```screen_share_permission_denied```: It is sent when the user does not allow screen share

Using the WebRTCAdaptor in your projects as a Module
----------------------------------------------------

In your project, run:

    npm i @antmedia/webrtc_adaptor --save-dev

Then inside your javascript file initialize the WebRTCAdaptor.

    // ...
    import { WebRTCAdaptor } from '@ant-media/webrtc_adaptor';
    
    const webRTCAdaptor = new WebRTCAdaptor({
        websocket_url: "wss://your-domain.tld:5443/WebRTCAppEE/websocket",
        mediaConstraints: {
            video: true,
            audio: true,
        },
        peerconnection_config: {
            'iceServers': [{'urls': 'stun:stun1.l.google.com:19302'}]
        },
        sdp_constraints: {
            OfferToReceiveAudio : false,
            OfferToReceiveVideo : false,
        },
        localVideoId: "id-of-video-element", // `<video id="id-of-video-element" autoplay muted>``</video>`
        bandwidth: int|string, // default is 900 kbps, string can be 'unlimited'
        dataChannelEnabled: true|false, // enable or disable data channel
        callback: (info, obj) =>` {}, // check info callbacks bellow
        callbackError: function(error, message) {}, // check error callbacks bellow
    });
    
    // Then, in another part of your script, you can start streaming by calling the publish method
    webRTCAdaptor.publish(streamId, token, subscriberId, subscriberCode, streamName);
    
    //...

Above example taken from the [StreamApp repository](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/index.html#L511).