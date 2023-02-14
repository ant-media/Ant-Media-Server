## Classes

<dl>
<dt><a href="#WebRTCAdaptor">WebRTCAdaptor</a></dt>
<dd><p>WebRTCAdaptor Class is interface to the JS SDK of Ant Media Server (AMS). This class manages the signalling,
keeps the states of peers.</p>
<p>This class is used for peer-to-peer signalling,
publisher and player signalling and conference.</p>
<p>Also it is responsible for some room management in conference case.</p>
<p>There are different use cases in AMS. This class is used for all of them.</p>
<p>WebRTC Publish
WebRTC Play
WebRTC Data Channel Connection
WebRTC Conference
WebRTC Multitrack Play
WebRTC Multitrack Conference
WebRTC peer-to-peer session</p>
</dd>
<dt><a href="#ReceivingMessage">ReceivingMessage</a></dt>
<dd><p>This structure is used to handle large size data channel messages (like image)
which should be splitted into chunks while sending and receiving.</p>
</dd>
</dl>

<a name="WebRTCAdaptor"></a>

## WebRTCAdaptor
WebRTCAdaptor Class is interface to the JS SDK of Ant Media Server (AMS). This class manages the signalling,
keeps the states of peers.

This class is used for peer-to-peer signalling,
publisher and player signalling and conference.

Also it is responsible for some room management in conference case.

There are different use cases in AMS. This class is used for all of them.

WebRTC Publish
WebRTC Play
WebRTC Data Channel Connection
WebRTC Conference
WebRTC Multitrack Play
WebRTC Multitrack Conference
WebRTC peer-to-peer session

**Kind**: global class  

* [WebRTCAdaptor](#WebRTCAdaptor)
    * [.peerconnection_config](#WebRTCAdaptor+peerconnection_config)
    * [.sdp_constraints](#WebRTCAdaptor+sdp_constraints)
    * [.remotePeerConnection](#WebRTCAdaptor+remotePeerConnection)
    * [.remotePeerConnectionStats](#WebRTCAdaptor+remotePeerConnectionStats)
    * [.remoteDescriptionSet](#WebRTCAdaptor+remoteDescriptionSet)
    * [.iceCandidateList](#WebRTCAdaptor+iceCandidateList)
    * [.roomName](#WebRTCAdaptor+roomName)
    * [.playStreamId](#WebRTCAdaptor+playStreamId)
    * [.audioContext](#WebRTCAdaptor+audioContext)
    * [.isMultiPeer](#WebRTCAdaptor+isMultiPeer)
    * [.multiPeerStreamId](#WebRTCAdaptor+multiPeerStreamId)
    * [.webSocketAdaptor](#WebRTCAdaptor+webSocketAdaptor)
    * [.isPlayMode](#WebRTCAdaptor+isPlayMode)
    * [.debug](#WebRTCAdaptor+debug)
    * [.publishStreamId](#WebRTCAdaptor+publishStreamId)
    * [.idMapping](#WebRTCAdaptor+idMapping)
    * [.onlyDataChannel](#WebRTCAdaptor+onlyDataChannel)
    * [.dataChannelEnabled](#WebRTCAdaptor+dataChannelEnabled)
    * [.receivingMessages](#WebRTCAdaptor+receivingMessages)
    * [.candidateTypes](#WebRTCAdaptor+candidateTypes)
    * [.remoteVideo](#WebRTCAdaptor+remoteVideo)
    * [.soundMeters](#WebRTCAdaptor+soundMeters)
    * [.soundLevelList](#WebRTCAdaptor+soundLevelList)
    * [.mediaManager](#WebRTCAdaptor+mediaManager)
    * [.initialize()](#WebRTCAdaptor+initialize)
    * [.publish()](#WebRTCAdaptor+publish)
    * [.joinRoom()](#WebRTCAdaptor+joinRoom)
    * [.play()](#WebRTCAdaptor+play)
    * [.stop()](#WebRTCAdaptor+stop)
    * [.join()](#WebRTCAdaptor+join)
    * [.leaveFromRoom()](#WebRTCAdaptor+leaveFromRoom)
    * [.leave()](#WebRTCAdaptor+leave)
    * [.getStreamInfo()](#WebRTCAdaptor+getStreamInfo)
    * [.upateStreamMetaData()](#WebRTCAdaptor+upateStreamMetaData)
    * [.getRoomInfo()](#WebRTCAdaptor+getRoomInfo)
    * [.enableTrack()](#WebRTCAdaptor+enableTrack)
    * [.getTracks()](#WebRTCAdaptor+getTracks)
    * [.onTrack()](#WebRTCAdaptor+onTrack)
    * [.iceCandidateReceived()](#WebRTCAdaptor+iceCandidateReceived)
    * [.initDataChannel()](#WebRTCAdaptor+initDataChannel)
    * [.initPeerConnection()](#WebRTCAdaptor+initPeerConnection)
    * [.closePeerConnection()](#WebRTCAdaptor+closePeerConnection)
    * [.signallingState()](#WebRTCAdaptor+signallingState)
    * [.iceConnectionState()](#WebRTCAdaptor+iceConnectionState)
    * [.gotDescription()](#WebRTCAdaptor+gotDescription)
    * [.takeConfiguration()](#WebRTCAdaptor+takeConfiguration)
    * [.takeCandidate()](#WebRTCAdaptor+takeCandidate)
    * [.addIceCandidate()](#WebRTCAdaptor+addIceCandidate)
    * [.startPublishing()](#WebRTCAdaptor+startPublishing)
    * [.toggleVideo()](#WebRTCAdaptor+toggleVideo)
    * [.toggleAudio()](#WebRTCAdaptor+toggleAudio)
    * [.getStats()](#WebRTCAdaptor+getStats)
    * [.enableStats()](#WebRTCAdaptor+enableStats)
    * [.disableStats()](#WebRTCAdaptor+disableStats)
    * [.checkWebSocketConnection()](#WebRTCAdaptor+checkWebSocketConnection)
    * [.closeWebSocket()](#WebRTCAdaptor+closeWebSocket)
    * [.peerMessage()](#WebRTCAdaptor+peerMessage)
    * [.forceStreamQuality()](#WebRTCAdaptor+forceStreamQuality)
    * [.sendData()](#WebRTCAdaptor+sendData)
    * [.enableAudioLevel(stream, streamId)](#WebRTCAdaptor+enableAudioLevel)
    * [.getSoundLevelList(streamsList)](#WebRTCAdaptor+getSoundLevelList)
    * [.getSender(streamId, type)](#WebRTCAdaptor+getSender) ⇒
    * [.assignVideoTrack(videoTrackId, streamId, enabled)](#WebRTCAdaptor+assignVideoTrack) ⇒
    * [.updateVideoTrackAssignments(offset, size)](#WebRTCAdaptor+updateVideoTrackAssignments) ⇒
    * [.setMaxVideoTrackCount(maxTrackCount)](#WebRTCAdaptor+setMaxVideoTrackCount) ⇒
    * [.updateAudioLevel(value)](#WebRTCAdaptor+updateAudioLevel) ⇒
    * [.getDebugInfo()](#WebRTCAdaptor+getDebugInfo) ⇒
    * [.turnOffLocalCamera()](#WebRTCAdaptor+turnOffLocalCamera)
    * [.switchVideoCameraFacingMode(streamId, facingMode)](#WebRTCAdaptor+switchVideoCameraFacingMode)

<a name="WebRTCAdaptor+peerconnection_config"></a>

### webRTCAdaptor.peerconnection\_config
Used while initializing the PeerConnection
https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection#parameters

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+sdp_constraints"></a>

### webRTCAdaptor.sdp\_constraints
Used while creating SDP (answer or offer)
https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/createOffer#parameters

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+remotePeerConnection"></a>

### webRTCAdaptor.remotePeerConnection
This keeps the PeerConnections for each stream id.
It is an array because one @WebRTCAdaptor instance can manage multiple WebRTC connections as in the conference.
Its indices are the Stream Ids of each stream

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+remotePeerConnectionStats"></a>

### webRTCAdaptor.remotePeerConnectionStats
This keeps statistics for the each PeerConnection.
It is an array because one @WebRTCAdaptor instance can manage multiple WebRTC connections as in the conference.
Its indices are the Stream Ids of each stream

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+remoteDescriptionSet"></a>

### webRTCAdaptor.remoteDescriptionSet
This keeps the Remote Description (SDP) set status for each PeerConnection.
We need to keep this status because sometimes ice candidates from the remote peer
may come before the Remote Description (SDP). So we need to store those ice candidates
in @iceCandidateList field until we get and set the Remote Description.
Otherwise setting ice candidates before Remote description may cause problem.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+iceCandidateList"></a>

### webRTCAdaptor.iceCandidateList
This keeps the Ice Candidates which are received before the Remote Description (SDP) received.
For details please check @remoteDescriptionSet field.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+roomName"></a>

### webRTCAdaptor.roomName
This is the name for the room that is desired to join in conference mode.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+playStreamId"></a>

### webRTCAdaptor.playStreamId
This keeps StreamIds for the each playing session.
It is an array because one @WebRTCAdaptor instance can manage multiple playing sessions.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+audioContext"></a>

### webRTCAdaptor.audioContext
Audio context to use

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+isMultiPeer"></a>

### webRTCAdaptor.isMultiPeer
This is the flag indicates if multiple peers will join a peer in the peer to peer mode.
This is used only with Embedded SDk

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+multiPeerStreamId"></a>

### webRTCAdaptor.multiPeerStreamId
This is the stream id that multiple peers can join a peer in the peer to peer mode.
This is used only with Embedded SDk

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+webSocketAdaptor"></a>

### webRTCAdaptor.webSocketAdaptor
This is instance of @WebSocketAdaptor and manages to websocket connection.
All signalling messages are sent to/recived from
the Ant Media Server over this web socket connection

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+isPlayMode"></a>

### webRTCAdaptor.isPlayMode
This flags indicates if this @WebRTCAdaptor instance is used only for playing session(s)
You don't need camera/mic access in play mode

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+debug"></a>

### webRTCAdaptor.debug
This flags enables/disables debug logging

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+publishStreamId"></a>

### webRTCAdaptor.publishStreamId
This is the Stream Id for the publisher. One @WebRCTCAdaptor supports only one publishing
session for now (23.02.2022).
In conference mode you can join a room with null stream id. In that case
Ant Media Server generates a stream id and provides it JoinedTheRoom callback and it is set to this field.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+idMapping"></a>

### webRTCAdaptor.idMapping
This is used to keep stream id and track id (which is provided in SDP) mapping
in MultiTrack Playback and conference.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+onlyDataChannel"></a>

### webRTCAdaptor.onlyDataChannel
This is used when only data is brodcasted with the same way video and/or audio.
The difference is that no video or audio is sent when this field is true

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+dataChannelEnabled"></a>

### webRTCAdaptor.dataChannelEnabled
While publishing and playing streams data channel is enabled by default

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+receivingMessages"></a>

### webRTCAdaptor.receivingMessages
This is array of @ReceivingMessage
When you receive multiple large size messages @ReceivingMessage simultaneously
this map is used to indicate them with its index tokens.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+candidateTypes"></a>

### webRTCAdaptor.candidateTypes
Supported candidate types. Below types are for both sending and receiving candidates.
It means if when client receives candidate from STUN server, it sends to the server if candidate's protocol
is in the list. Likely, when client receives remote candidate from server, it adds as ice candidate
if candidate protocol is in the list below.

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+remoteVideo"></a>

### webRTCAdaptor.remoteVideo
The html video tag for receiver is got here

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+soundMeters"></a>

### webRTCAdaptor.soundMeters
Keeps the sound meters for each connection. Its index is stream id

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+soundLevelList"></a>

### webRTCAdaptor.soundLevelList
Keeps the current audio level for each playing streams in conference mode

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+mediaManager"></a>

### webRTCAdaptor.mediaManager
All media management works for teh local stream are made by @MediaManager class.
for details please check @MediaManager

**Kind**: instance property of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+initialize"></a>

### webRTCAdaptor.initialize()
Called by constuctor to
	-check local stream unless it is in play mode
	-start websocket connection

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+publish"></a>

### webRTCAdaptor.publish()
Called to start a new WebRTC stream. AMS responds with start message.
Parameters:
	 streamId: unique id for the stream
	 token: required if any stream security (token control) enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Stream-Security-Documentation
	 subscriberId: required if TOTP enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Time-based-One-Time-Password-(TOTP)
	 subscriberCode: required if TOTP enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Time-based-One-Time-Password-(TOTP)
  streamName: required if you want to set a name for the stream
  mainTrack: required if you want to start the stream as a subtrack for a main streamwhich has id of this parameter.
				Check:https://antmedia.io/antmediaserver-webrtc-multitrack-playing-feature/
				!!! for multitrack conference set this value with roomName
  metaData: a free text information for the stream to AMS. It is provided to Rest methods by the AMS

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+joinRoom"></a>

### webRTCAdaptor.joinRoom()
Called to join a room. AMS responds with joinedTheRoom message.
Parameters:
	 roomName: unique id of the room
	 stream: unique id of the stream belogns to this participant
	 mode: 	legacy for older implementation (default value)
			mcu for merging streams
			amcu: audio only conferences with mixed audio

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+play"></a>

### webRTCAdaptor.play()
Called to start a playing session for a stream. AMS responds with start message.
Parameters:
	 streamId: unique id for the stream that you want to play
	 token: required if any stream security (token control) enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Stream-Security-Documentation
  roomId: required if this stream is belonging to a room participant
  enableTracks: required if the stream is a main stream of multitrack playing. You can pass the the subtrack id list that you want to play.
					you can also provide a track id that you don't want to play by adding ! before the id.
	 subscriberId: required if TOTP enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Time-based-One-Time-Password-(TOTP)
	 subscriberCode: required if TOTP enabled. Check https://github.com/ant-media/Ant-Media-Server/wiki/Time-based-One-Time-Password-(TOTP)
  metaData: a free text information for the stream to AMS. It is provided to Rest methods by the AMS

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+stop"></a>

### webRTCAdaptor.stop()
Called to stop a publishing/playing session for a stream. AMS responds with publishFinished or playFinished message.
Parameters:
	 streamId: unique id for the stream that you want to stop publishing or playing

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+join"></a>

### webRTCAdaptor.join()
Called to join a peer-to-peer mode session as peer. AMS responds with joined message.
Parameters:
	 streamId: unique id for the peer-to-peer session

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+leaveFromRoom"></a>

### webRTCAdaptor.leaveFromRoom()
Called to leave from a conference room. AMS responds with leavedTheRoom message.
Parameters:
	 roomName: unique id for the conference room

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+leave"></a>

### webRTCAdaptor.leave()
Called to leave from a peer-to-peer mode session. AMS responds with leaved message.
Parameters:
	 streamId: unique id for the peer-to-peer session

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+getStreamInfo"></a>

### webRTCAdaptor.getStreamInfo()
Called to get a stream information for a specific stream. AMS responds with streamInformation message.
Parameters:
	 streamId: unique id for the stream that you want to get info about

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+upateStreamMetaData"></a>

### webRTCAdaptor.upateStreamMetaData()
Called to update the meta information for a specific stream.
Parameters:
	 streamId: unique id for the stream that you want to update MetaData
  metaData: new free text information for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+getRoomInfo"></a>

### webRTCAdaptor.getRoomInfo()
Called to get the room information for a specific room. AMS responds with roomInformation message
which includes the ids and names of the streams in that room.
Parameters:
	 roomName: unique id for the room that you want to get info about
	 streamId: unique id for the stream that is streamed by this @WebRTCAdaptor

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+enableTrack"></a>

### webRTCAdaptor.enableTrack()
Called to enable/disable data flow from the AMS for a specific track under a main track.
Parameters:
	 mainTrackId: unique id for the main stream
	 trackId: unique id for the track that you want to enable/disable data flow for
	 enabled: true or false

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+getTracks"></a>

### webRTCAdaptor.getTracks()
Called to get the track ids under a main stream. AMS responds with trackList message.
Parameters:
	 streamId: unique id for the main stream
	 token: not used
TODO: check this function

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+onTrack"></a>

### webRTCAdaptor.onTrack()
Called by browser when a new track is added to WebRTC connetion. This is used to infor html pages with newStreamAvailable callback.
Parameters:
	 event: TODO
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+iceCandidateReceived"></a>

### webRTCAdaptor.iceCandidateReceived()
Called by WebSocketAdaptor when a new ice candidate is received from AMS.
Parameters:
	 event: TODO
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+initDataChannel"></a>

### webRTCAdaptor.initDataChannel()
Called internally to initiate Data Channel.
Note that Data Channel should be enabled fromAMS settings.
	 streamId: unique id for the stream
  dataChannel: provided by PeerConnection

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+initPeerConnection"></a>

### webRTCAdaptor.initPeerConnection()
Called internally to initiate PeerConnection.
	 streamId: unique id for the stream
  dataChannelMode: can be "publish" , "play" or "peer" based on this it is decided which way data channel is created

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+closePeerConnection"></a>

### webRTCAdaptor.closePeerConnection()
Called internally to close PeerConnection.
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+signallingState"></a>

### webRTCAdaptor.signallingState()
Called to get the signalling state for a stream.
This information can be used for error handling.
Check: https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/connectionState
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+iceConnectionState"></a>

### webRTCAdaptor.iceConnectionState()
Called to get the ice connection state for a stream.
This information can be used for error handling.
Check: https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/iceConnectionState
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+gotDescription"></a>

### webRTCAdaptor.gotDescription()
Called by browser when Local Configuration (SDP) is created successfully.
It is set as LocalDescription first then sent to AMS.
	 configuration: created Local Configuration (SDP)
	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+takeConfiguration"></a>

### webRTCAdaptor.takeConfiguration()
Called by WebSocketAdaptor when Remote Configuration (SDP) is received from AMS.
It is set as RemoteDescription first then if @iceCandidateList has candidate that
is received bfore this message, it is added as ice candidate.
	 configuration: received Remote Configuration (SDP)
	 idOfStream: unique id for the stream
	 typeOfConfiguration: unique id for the stream
	 idMapping: stream id and track id (which is provided in SDP) mapping in MultiTrack Playback and conference.
				It is recorded to match stream id as new tracks are added with @onTrack

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+takeCandidate"></a>

### webRTCAdaptor.takeCandidate()
Called by WebSocketAdaptor when new ice candidate is received from AMS.
If Remote Description (SDP) is already set, the candidate is added immediately,
otherwise stored in @iceCandidateList to add after Remote Description (SDP) set.
	 idOfTheStream: unique id for the stream
	 tmpLabel: sdpMLineIndex
	 tmpCandidate: ice candidate

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+addIceCandidate"></a>

### webRTCAdaptor.addIceCandidate()
Called internally to add the Ice Candidate to PeerConnection
	 streamId: unique id for the stream
	 tmpCandidate: ice candidate

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+startPublishing"></a>

### webRTCAdaptor.startPublishing()
Called by WebSocketAdaptor when start message is received //TODO: may be changed. this logic shouldn't be in WebSocketAdaptor
	 idOfStream: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+toggleVideo"></a>

### webRTCAdaptor.toggleVideo()
Toggle video track on the server side.

  streamId: is the id of the stream
  trackId: is the id of the track. streamId is also one of the trackId of the stream. If you are having just a single track on your
        stream, you need to give streamId as trackId parameter as well.
  enabled: is the enable/disable video track. If it's true, server sends video track. If it's false, server does not send video

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+toggleAudio"></a>

### webRTCAdaptor.toggleAudio()
Toggle audio track on the server side.

  streamId: is the id of the stream
  trackId: is the id of the track. streamId is also one of the trackId of the stream. If you are having just a single track on your
        	stream, you need to give streamId as trackId parameter as well.
  enabled: is the enable/disable video track. If it's true, server sends audio track. If it's false, server does not send audio

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+getStats"></a>

### webRTCAdaptor.getStats()
Called to get statistics for a PeerConnection. It can be publisher or player.

	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+enableStats"></a>

### webRTCAdaptor.enableStats()
Called to start a periodic timer to get statistics periodically (5 seconds) for a specific stream.

	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+disableStats"></a>

### webRTCAdaptor.disableStats()
Called to stop the periodic timer which is set by @enableStats

	 streamId: unique id for the stream

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+checkWebSocketConnection"></a>

### webRTCAdaptor.checkWebSocketConnection()
Called to check and start Web Socket connection if it is not started

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+closeWebSocket"></a>

### webRTCAdaptor.closeWebSocket()
Called to stop Web Socket connection
After calling this function, create new WebRTCAdaptor instance, don't use the the same object
Because all streams are closed on server side as well when websocket connection is closed.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+peerMessage"></a>

### webRTCAdaptor.peerMessage()
Called to send a text message to other peer in the peer-to-peer sessionnnection is closed.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+forceStreamQuality"></a>

### webRTCAdaptor.forceStreamQuality()
Called to force AMS to send the video with the specified resolution in case of Adaptive Streaming (ABR) enabled.
Normally the resolution is automatically determined by AMS according to the network condition.
	 streamId: unique id for the stream
  resolution: default is auto. You can specify any height value from the ABR list.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+sendData"></a>

### webRTCAdaptor.sendData()
Called to send data via DataChannel. DataChannel should be enabled on AMS settings.
	 streamId: unique id for the stream
  data: data that you want to send. It may be a text (may in Json format or not) or binary

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+enableAudioLevel"></a>

### webRTCAdaptor.enableAudioLevel(stream, streamId)
Called by user
to add SoundMeter to a stream (remote stream)
to measure audio level. This sound Meters are added to a map with the key of StreamId.
When user called @getSoundLevelList, the instant levels are provided.

This list can be used to add a sign to talking participant
in conference room. And also to determine the dominant audio to focus that player.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  

| Param | Type |
| --- | --- |
| stream | <code>\*</code> | 
| streamId | <code>\*</code> | 

<a name="WebRTCAdaptor+getSoundLevelList"></a>

### webRTCAdaptor.getSoundLevelList(streamsList)
Called by the user
to get the audio levels for the streams for the provided StreamIds

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  

| Param | Type |
| --- | --- |
| streamsList | <code>\*</code> | 

<a name="WebRTCAdaptor+getSender"></a>

### webRTCAdaptor.getSender(streamId, type) ⇒
Called media manaher to get video/audio sender for the local peer connection

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: boolean  

| Param | Type | Description |
| --- | --- | --- |
| streamId | <code>\*</code> | : |
| type | <code>\*</code> | : "video" or "audio" |

<a name="WebRTCAdaptor+assignVideoTrack"></a>

### webRTCAdaptor.assignVideoTrack(videoTrackId, streamId, enabled) ⇒
Called by user

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: void  

| Param | Type | Description |
| --- | --- | --- |
| videoTrackId | <code>\*</code> | : track id associated with pinned video |
| streamId | <code>\*</code> | : streamId of the pinned video |
| enabled | <code>\*</code> | : true | false |

<a name="WebRTCAdaptor+updateVideoTrackAssignments"></a>

### webRTCAdaptor.updateVideoTrackAssignments(offset, size) ⇒
Called by user
video tracks may be less than the participants count
so these parameters are used for assigning video tracks to participants.
This message is used to make pagination in conference.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: void  

| Param | Type | Description |
| --- | --- | --- |
| offset | <code>\*</code> | : start index for participant list to play |
| size | <code>\*</code> | : number of the participants to play |

<a name="WebRTCAdaptor+setMaxVideoTrackCount"></a>

### webRTCAdaptor.setMaxVideoTrackCount(maxTrackCount) ⇒
Called by user
This message is used to set max video track count in a conference.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: void  

| Param | Type | Description |
| --- | --- | --- |
| maxTrackCount | <code>\*</code> | : maximum video track count |

<a name="WebRTCAdaptor+updateAudioLevel"></a>

### webRTCAdaptor.updateAudioLevel(value) ⇒
Called by user
This message is used to send audio level in a conference.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: void  

| Param | Type | Description |
| --- | --- | --- |
| value | <code>\*</code> | : audio lavel |

<a name="WebRTCAdaptor+getDebugInfo"></a>

### webRTCAdaptor.getDebugInfo() ⇒
Called by user
This message is used to get debug data from server for debugging purposes in conference.

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
**Returns**: void  
<a name="WebRTCAdaptor+turnOffLocalCamera"></a>

### webRTCAdaptor.turnOffLocalCamera()
The following messages are forwarded to MediaManager. They are also kept here because of backward compatibility.
You can find the details about them in media_manager.js

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  
<a name="WebRTCAdaptor+switchVideoCameraFacingMode"></a>

### webRTCAdaptor.switchVideoCameraFacingMode(streamId, facingMode)
Called by User
to switch between front and back camera on mobile devices

**Kind**: instance method of [<code>WebRTCAdaptor</code>](#WebRTCAdaptor)  

| Param | Type | Description |
| --- | --- | --- |
| streamId | <code>\*</code> | Id of the stream to be changed. |
| facingMode | <code>\*</code> | it can be "user" or "environment" This method is used to switch front and back camera. |

<a name="ReceivingMessage"></a>

## ReceivingMessage
This structure is used to handle large size data channel messages (like image)
which should be splitted into chunks while sending and receiving.

**Kind**: global class  
