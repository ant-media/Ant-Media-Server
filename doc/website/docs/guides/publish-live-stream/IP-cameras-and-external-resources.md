---
title: IP Cameras
---

# Streaming IP cameras and external resources(HLS, RTMP, RTSP)

Ant Media Server Users can pull IP camera streams easily on the management panel. In other words, you don’t need to write any commands or use a terminal to be able to restream sources.

In order for IP camera restreaming, the camera should support the ONVIF standard. ONVIF makes it easy to manage IP cameras. All CRUD and PTZ operations are based on well-defined SOAP messages.

![](@site/static/img/onvif_conformance.gif)

Let’s have a look at how to pull a stream from an IP camera.

## Adding an IP camera

*   Go to the management panel, Select **LiveApp** from applications, then click on **New Live Stream** and select **IP Camera**.  
    ![](@site/static/img/re-stream-add-ip-camera-1.png)
*   Fill in the **camera name**, **camera** **username**, and **camera password**. You should add the ONVIF URL of the IP camera. Generally, it is in the following format: ```IP-ADDRESS-OF-IPCAMERA:8080```. If you don't know the ONVIF URL, you can use “**auto-discovery**” feature. If the IP camera and the server are in the same subnet, Ant Media server automatically can discover them. The screenshot of auto-discovery result is shown below.  
    ![](@site/static/img/image-1645088850464.png)

## Watching IP cameras

If IP cameras are reachable and configured correctly, Ant Media Server adds their streams as a live stream and starts to pull streams from them. You can see its status on the management panel. To watch the stream, click the **Play button** under **Actions**.

![](@site/static/img/restream-ip-camera.png)

## Recording IP camera streams

The Ant Media Server can save IP camera streams in MP4 format. It record streams with defined periods such as one hour or ten hours interval. You can see these recorded files on **VOD** tab in the management panel.

![](@site/static/img/image-1645088966529.png)

## Restreaming external sources

Ant Media Server (AMS) can operate with different streaming flows. As well as accepting and creating streaming media, it also has the capability to pull live streams from external sources, such as live TV streams, IP camera streams or other forms of live streams (RTSP, HLS, TS, FLV etc.).

![](@site/static/img/restream-add-streamsource.png)

In order to restream from an external source, follow those steps:

*   First, log in to the management panel. Click on **New Live Stream** >` **Stream Source**. Define stream name and URL.
*   AMS starts to pull streams.
*   As the stream starts to pull, you can watch it from AMS panel.