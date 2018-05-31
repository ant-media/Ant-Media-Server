# Play Live Stream with HLS

Now, we will talk about converting the RTMP URL to the ant media server into the HLS. 

We have RTMP URL like this: “rtmp://<SERVER_NAME>/LiveApp/<STREAM_ID>”.

* If you have a Community Edition, you can watch stream with http://<SERVER_NAME>/LiveApp/streams/<STREAM_ID>.m3u8 HLS. 

* If you have a Enterprise Edition, you can watch stream with http://<SERVER_NAME>/LiveApp/streams/<STREAM_ID>-adaptive.m3u8 HLS.

* If you want to record live stream, you can use http://<SERVER_NAME>/LiveApp/streams/<STREAM_ID>.mp4 HLS.

* If you want to embed it on any site, you can use http://<SERVER_NAME>/LiveApp/play.html?name=<STREAM_ID>.m3u8 HLS.
