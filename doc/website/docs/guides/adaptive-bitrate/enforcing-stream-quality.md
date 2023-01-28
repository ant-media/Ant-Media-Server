---
title: Enforcing Stream Quality
---
# Enforcing the stream quality

Ant Media Server (AMS) has the ability to force stream quality. In this guide, you'll learn what it is, how it works, and how to benefit from stream quality feature.

## How does the adaptive bitrate work?

AMS measures the viewers' internet speed and sends the best quality according to the internet speed of the viewer.

Example:

*   Assume that there are two bitrates on the server.
    *   The first one is 360p and 800kbps.
    *   The second one is 480p and 1500kbps.
*   Assume that the viewer's internet speends are as follows:
    *   Above 1500kbps: In this case, a resolution of 480p is sent.
    *   Less than 800kbps: In this case, a resolution of 360p is sent.

The adaptive bitrate feature makes sure that the end user only gets what the server is sending out.

## Enforcing stream quality

The client side viewer can enforce a resolution it would like to get. Keep in mind that if you request a quality with a bitrate higher than the client's bitrate, you may see some packet drops or pixelations.

While you play the stream, after you receive ```play_started``` notification in ```WebRTCAdaptor```, Call ```getStreamInfo``` with ```webRTCAdaptor.getStreamInfo({your_stream_Id})```

```js
else if (info == "play_started") 
{
    console.log("play started");
    webRTCAdaptor.getStreamInfo(streamId);
} 
else if (info == "play_finished") 
{
```

### How to get enforced resolution from getStreamInfo

Calling getStreamInfo method makes the server send stream information callback which returns stream information such as adaptive resolutions, audio bitrate and video bitrate.

When server sends the ```streamInformation```, you can get stream details like following:

```js
else if (info == "streamInformation") {

        var streamResolutions = new Array();

        obj["streamInfo"].forEach(function(entry) {
        //It's needs to both of VP8 and H264. So it can be duplicate
        if(!streamResolutions.includes(entry["streamHeight"])){
            streamResolutions.push(entry["streamHeight"]);	

        }// Got resolutions from server response and added to an array.

        });
        
}// After getting stream information, forceStreamQuality can be used with the information we got.
else if (info == "ice_connection_state_changed"){
```

After getting stream info, you can call the following function to force the video quality you want to watch:

```js
webRTCAdaptor.forceStreamQuality("{your_stream_Id}",  {the_resolution_to_be_forced});
```

There is a working sample in player.html as shown below. When you choose a resolution, it'll force the quality. You can select the resolution, as you can see from the screenshot below.

![](@site/static/img/92497488-14bcdf00-f202-11ea-9790-b9afcbe0f456.png)In the example above, ```240p``` is selected, and the bitrate is ```500000```.