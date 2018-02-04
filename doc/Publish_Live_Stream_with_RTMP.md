# Publish Live Stream with RTMP

There are many ways to publish Live Stream with RTMP to Ant Media server. Here are the some of the ways

* <a href="#publish_with_desktop_software">Publish with a Desktop Software like OBS, XSplit, Wirecast, etc.</a> 
* <a href="#publish_with_hardware_encoder">Publish with a Hardware Encoder (Teradek, Tricaster, Gosolo, etc)</a>
* Publish with a Mobile App

For each case we are going to give an example how to publish live stream with RTMP to Ant Media Server

## <div id="publish_with_desktop_software">Publish with a Desktop Software - Open Broadcaster Software</div>

OBS(Open Broadcaster Softeware) is Free and open source software for video recording and live streaming.
You can use either your PC’s embedded camera or externally connected one as a video source with OBS. 
Sound sources also can be configured with it. Ant Media Server is fully compatible with OBS software.

Let’s have a look at step by step how to use OBS for streaming:
### Step 1 : Getting the OBS:

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
* Split the rtmp://<SERVER_NAME>/LiveApp/ as the URL <STREAM_NAME> as the stream key 
* Write URL and stream key parameters as described in the below picture. Make sure that Stream ID should be written to 
Stream Key field not to the URL.

![OBS (Open Broadcaster Software) Stream Configuration](https://ant-media.github.io/Ant-Media-Server/doc/images/OBS_Configuration.png)
 

### Step 5: Start Stream and Watch 🙂

* Close settings window and just click the “Start Streaming” button in the main window of OBS. 
You can watch stream from either Ant Media Management console or other platforms such as VLC player with same RTMP URL or with
http://<SERVER_NAME>/LiveApp/streams/<STREAM_ID>.m3u8 HLS

## <div id="publish_with_hardware_encoder">Publish with a Hardware Encoder - Teradek Vidiu Pro</div>

Teradek Vidiu Pro is fully compatible with Ant Media Server 1.2.0+ 

Let’s have a look at step by step how to use Teradek Vidiu Pro for streaming, firstly start by powering the Teradek Vidiu Pro

### Step 1 : Connect to Teradek Vidiu Pro WiFi Network. 
It creates a WiFi network with name VidiU−XXXXX. Connect that network with your computer and go to the 172.16.1.1 on your browser.

![Connect Teradek's WiFi Network](https://ant-media.github.io/Ant-Media-Server/doc/images/vidiu_pro_console.png)

Click “Settings” button on the top right and then click “Network” item in the screen.

![Configure Network Settings of Teradek Vidiu](https://ant-media.github.io/Ant-Media-Server/doc/images/configure_vidiu_network_button.png)

## Step 2 :Configure Wireless Network Connection of Teradek Vidiu Pro
Click the WiFi item. Choose “Client”  mode and Click “Browse” button to lookup the WiFi networks around.

![Configure WiFi of Teradek Vidiu](https://ant-media.github.io/Ant-Media-Server/doc/images/set_vidiu_pro_wifi_connectivity.png)

Choose the WiFi you would like Teradek Vidiu Pro to connect from the list.

![Choose WiFi from list on Teradek Vidiu](https://ant-media.github.io/Ant-Media-Server/doc/images/choose_wifi_for_vidiu_pro.png)

Enter the password of the WiFi network and click “Apply” button on the top right

Restart Teradek Vidiu Pro

Connect to the same wireless network on your computer and check the IP address of the Teradek Vidiu Pro from device’s LED screen.  Press Menu button on the device. Menu button is a joystick button as well. So go to the Network Settings > WiFi > Info to see the IP address of the device

Sometimes you may need to power off / on the Teradek Vidiu Pro.

### Step 3 :Configure Broadcasting Settings
Click the “Broadcast” item on the Settings screen

![Configure broadcast settings of Teradek Vidiu](https://ant-media.github.io/Ant-Media-Server/doc/images/configure_broadcasting_settings_vidiu_pro.png)

Choose “Manual” from Mode then Enter Ant Media Server address to “RTMP Server URL” and write name of the stream to “Stream” box

![Write Ant Media Server URL on Teradek Vidiu Pro broadcast configuration](https://ant-media.github.io/Ant-Media-Server/doc/images/write_ant_media_server_url_to_vidiu_pro.png)

Click “Apply” button on the top right again

![Save broadcast settings of Teradek Vidiu Pro](https://ant-media.github.io/Ant-Media-Server/doc/images/apply_settings_vidiu_pro.png)

Your device configuration is OK now.

### Step 4 : Start Broadcasting
Connect the HDMI cable between HDMI source (camera, television or computer) and Teradek Vidiu Pro HDMI.

Wait until to see the “Ready” text appears on the right top of Teradek Vidiu Pro’s LED screen. Then press “Start/Stop” button on the device, a message appears on the device asking confirmation about starting the broadcast. Press “Yes” and  then broadcasting starts.

Ant Media Server accepts that broadcast and can perform adaptive streaming, recording and publishing to any other 3rd party RTMP server as usual.

When you are done about broadcasting, you can press “Start/Stop” button again to stop the broadcasting


