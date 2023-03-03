# How to publish with Teradek

Teradek Vidiu Pro is fully compatible with Ant Media Server. Before starting, you need to create a live stream in Ant Media Server web panel. You can create a stream in LiveApp or WebRTCAppEE, as there are differences between them. However, your RTMP URL changes depending on which one to choose. When you create a live stream, your RTMP URL will be in the following format:

```rtmp://YOUR_SERVER_IP_OR_URL/LiveApp/YOUR_STREAM_ID``` or ```rtmp://YOUR_SERVER_IP_OR_URL/WebRTCAppEE/YOUR_STREAM_ID```

depending on which app you use. You will use parts of these URLs in Teradek's settings.

Let’s have a look at step by step how to use Teradek Vidiu Pro for streaming, firstly start by powering the Teradek Vidiu Pro.

## Connect to Teradek Vidiu Pro WiFi Network.

It creates a WiFi network with the name ```VidiU−XXXXX```. Connect that network with your computer and go to the ```172.16.1.1``` on your browser.

![](@site/static/img/vidiu_pro_console.png)

Click “Settings” button on the top right and then click “Network” item in the screen.

![](@site/static/img/configure_vidiu_network_button.png)

## Configure Wireless Network Connection of Teradek Vidiu Pro

*   Click the WiFi item. Choose “Client” mode and Click the “Browse” button to look up the WiFi networks around.  
    ![](@site/static/img/set_vidiu_pro_wifi_connectivity.png)Choose the WiFi you would like Teradek Vidiu Pro to connect. Enter the password of the WiFi network and click “**Apply**” button on the top right. Then Restart Teradek Vidiu Pro. Connect to the same wireless network on your computer and check the IP address of the Teradek Vidiu Pro from device’s LED screen. Press the Menu button on the device. The menu button is a joystick button as well. Now go to the Network Settings >` WiFi >` Info to see the IP address of the device

Sometimes you may need to power off / on the Teradek Vidiu Pro.

## Configure broadcasting settings

*   Click the broadcast item on the Settings screen.  
    ![](@site/static/img/configure_broadcasting_settings_vidiu_pro.png)
*   Choose Manual from Mode then Enter Ant Media Server address to RTMP Server URL and write the name of the stream id to Stream box  
    ![](@site/static/img/write_ant_media_server_url_to_vidiu_pro.png)

Click ```Apply``` button on the top right again.

![](@site/static/img/apply_settings_vidiu_pro.png)  

Your device configuration is OK now.

## Start broadcasting

*   Connect the HDMI cable between the HDMI source (camera, television or computer) and Teradek Vidiu Pro HDMI.
*   Wait until to see the ```Ready``` text appears on the right top of Teradek Vidiu Pro’s LED screen. Then press ```Start/Stop``` button on the device, a message appears on the device asking for confirmation about starting the broadcast. Press ```Yes``` and then broadcasting starts.

Congrats! You're publishing with Teradek Vidiu Pro to Ant Media Server.