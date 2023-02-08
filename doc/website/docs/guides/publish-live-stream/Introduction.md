---
id: introduction
title: Introduction
sidebar_position: 1
---

# Publishing live streams

Ant Media Server can ingest/accept WebRTC and RTMP streams. It can also re-stream RTMP, HLS and RTSP streams by pulling them from another stream source (e.g from a restreaming platform).

Additionally, it can also convert RTMP/RTSP stream source inputs to WebRTC output and provide ultra-low latency streaming.

### WebRTC publishing

There are three ways for WebRTC publishing, [check out for detailed information](/v1/docs/publishing-live-streams-1#webrtc-publishing):

*   Publishing with a web browser (mobile and desktop)
*   Publishing with a mobile application (Android and iOS)
*   Publishing with an embedded SDK

### RTMP publishing

There are three ways for RTMP publishing, [check out for detailed information](/v1/docs/publishing-live-streams-1#rtmp-publishing):

*   Publishing with a broadcasting application (OBS, XSplit, Wirecast)
*   Publishing with a hardware Encoder (Teradek, Tricaster, Gosolo)
*   Publishing with a mobile application (Android, iOS)

### Re-streaming (RTMP, HLS or RTSP)

Ant Media Server can pull a stream from any source such as an IP camera or a linear stream source and can broadcast this stream to many viewers with WebRTC (Ultra low latency, with 0.5 seconds) or HLS (low latency, with 6 - 12 seconds). This workflow is called restreaming.

There are two ways for restreaming, [check out for detailed information](/v1/docs/publishing-live-streams-1#restreaming-rtmp-hls-or-rtsp):

*   ONVIF IP Camera
*   Stream source
*   Whatever method you choose, you need to ingest the stream from the source(s) to Ant Media, and Ant Media can transmit the stream to end points.