# WebRTC codecs

In this section, we're going to explain simply how to use H.264 & VP8 in Ant Media Server. VP8 and H.264 are mandatory codecs in WebRTC according to RFC 7742. On the other hand, not all browsers support VP8 and H.264 codecs at the same time.  
![](@site/static/img/image-1645442735337.png)

Ant Media Server supports H.264 and VP8 for versions 2.x and above, including transcoding and clustering. This ensures that all devices can play WebRTC streams within their browsers. If a browser does not play a stream, you may want to add H.264/VP8 support.

Below we'll show options where you enable or disable those two codecs, and how our selections affect adaptive bitrate options.

Enabling both H.264 and VP8
---------------------------

![](@site/static/img/image-1645442770087.png)

**SFU Mode (No adaptive bitrate):** Ant Media Server can ingest WebRTC stream in H.264 and VP8. If both of the codecs are supported by a browser, Ant Media Server chooses H.264 to ingest. Since there is no adaptive bitrate, incoming video stream is not transcoded to another codec. In other words, the server can ingest H.264 or VP8 and it forwards the incoming video stream to players.

**Adaptive Bitrate Mode (if you have at least one adaptive bitrate):** Ant Media Server ingests stream and transcodes it into H.264 and VP8. This way, devices supporting H264 or VP8 can play those streams.

Enabling H.264 and disabling VP8
--------------------------------

![](@site/static/img/image-1645442842621.png)

*   **SFU Mode:** Ant Media Server can only ingest H.264 streams and forwards them to the players.
*   **Adaptive Bitrate Mode:** Ant Media Server can only ingest H.264 and transcodes the stream into different H264 bitrates, and sends them the players.

You can check if your device supports H264 [at this link](https://mozilla.github.io/webrtc-landing/pc_test_no_h264.html).

Disabling H.264 and enabling VP8
--------------------------------

![](@site/static/img/image-1645442879397.png)

*   **SFU Mode:** Ant Media Server can only ingest VP8 streams and forwards them to players.
*   **Adaptive Bitrate Mode:** Ant Media Server can only ingest VP8 and transcode the stream into different VP8 bitrates and sends it to players.

_If you only have VP8 enabled, HLS Streaming & MP4 Recording will not work._