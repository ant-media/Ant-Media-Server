# Publish Live Stream with RTMP

There are many ways to publish Live Stream with RTMP to Ant Media server. Here are the some of the ways

* Publishing with a Desktop Software like OBS, XSplit, Wirecast, etc. 
* Publishing with a Hardware Encoder (Teradek, Tricaster, Gosolo, etc)
* Publishing with a Mobile App

For each case we are going to give an example how to publish live stream with RTMP to Ant Media Server

## Publish with a Desktop Software - Open Broadcaster Software

OBS(Open Broadcaster Softeware) is Free and open source software for video recording and live streaming.
You can use either your PC’s embedded camera or externally connected one as a video source with OBS. 
Sound sources also can be configured with it. Ant Media Server is fully compatible with OBS software.

Let’s have a look at step by step how to use OBS for streaming:
### Step 1: Getting the OBS:

Download via its official [web page](https://obsproject.com/). It has Windows, Mac, and Linux releases.

### Step 2 : Provide Sources:

![OBS (Open Broadcaster Software) interface](https://ant-media.github.io/Ant-Media-Server/doc/images/obs_screenshot.jpg)

By default, OBS starts to capture from your embedded camera if exists after inilialized. 
You can add or remove video/audio source from Sources section, such as an image can be broadcasted as a video source or 
external microphone can be added as a audio source.

### Step 3: Create RTMP URL for Ant Media Server

* If your server configuration accepts any stream just use a RTMP URL like this "rtmp://<SERVER_NAME>/LiveApp/<STREAM_ID>"
Use IP address or server fqdn in the place of <SERVER_ID> and use any id/name for the <STREAM_ID>

* If your server only accepts registered live streams, you need to create live stream by Management console or rest services. 
    - To reach the Management console go to http://<SERVER_NAME>:5080 address,  
    - Click one of the apps like “LiveApp” from Applications section and click “New Live Stream”. 
    - The server creates a live stream with an unique ID in the format of “rtmp://<SERVER_NAME>/LiveApp/325859929809451108600212”.  
    - You can copy this url with clicking “Publish URL” button.

![Management Console New Live Stream](https://ant-media.github.io/Ant-Media-Server/doc/images/management_console_new_live_stream.png)

### Step 4: Configure the OBS

You need to write live stream parameters to OBS in order to start broadcasting. 
* Click “Settings” then select “Stream” tab. 
* Split the rtmp://<domain or IP of Server>/LiveApp/ as the URL <STREAM_NAME> as the stream key 
* Write URL and stream key parameters as described in the below picture. Make sure that Stream ID should be written to 
Stream Key field not to the URL.

![OBS (Open Broadcaster Software) Stream Configuration](https://ant-media.github.io/Ant-Media-Server/doc/images/OBS_Configuration.png)
 

### Step 5: Start Stream and Watch 🙂

* Close settings window and just click the “Start Streaming” button in the main window of OBS. 
You can watch stream from either Ant Media Management console or other platforms such as VLC player with same RTMP URL or with
http://<SERVER_NAME>/LiveApp/streams/<STREAM_ID>.m3u8 HLS

