# Publish with a Hardware Encoder - Teradek Vidiu Pro

Teradek Vidiu Pro is fully compatible with Ant Media Server 1.2.0+ Let’s have a look at step by step how to use Teradek Vidiu Pro for streaming, firstly start by powering the Teradek Vidiu Pro

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
