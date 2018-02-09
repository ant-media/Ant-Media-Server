
# Try Low Latency WebRTC Live Streaming

Ant Media Server 1.2.0+ Enterprise Edition supports adaptive low latency WebRTC streaming. 

In addition, Ant Media Server can
* Record WebRTC streams as MP4 and MKV
* Convert WebRTC streams to adaptive live HLS
* Create previews in PNG format from WebRTC streams

## Download

Firstly, you need to have Ant Media Server Enterprise Edition. If you are a personal user and just want to try,
contact with us at [antmedia.io](https://antmedia.io). We will reply back by providing Ant Media Server Enterprise Edition to try. 

If you are a professional user and need support, you can buy support at [antmedia.io](https://antmedia.io) as well

## Quick Start

*Ant Media Server 1.2.0+ runs on Linux and Mac not on Windows.* 

Let's start, we assume that you have got Enterprise Edition somehow and downloaded to your local computer 

1. Extract the downloaded file in to a directory.

2. Make sure you have installed Oracle Java 8

    * Installation Java 8 on Ubuntu
      ```
      $ sudo add-apt-repository ppa:webupd8team/java
      $ sudo apt-get update
      $ sudo apt-get install oracle-java8-installer
      ```
    
    * Installation Java 8 on Mac
      * Follow the guide [here](https://java.com/en/download/help/mac_install.xml)

3. Open a terminal and go to directory where you have extracted Enterprise Edition

    ```
    cd /path/to/ant-media-server
    ```
   
4. Start the server with start.sh script. 

    ```
    ./start.sh
    ```
    It starts by printing some logs to the terminal.
 
5. Open the browser(Chrome or Firefox) and go to the `http://localhost:5080/WebRTCAppEE`. 
    Let browser access your camera and mic unless it cannot send WebRTC Stream to the server
    
    ![Open WebRTCAppEE](https://ant-media.github.io/Ant-Media-Server/doc/images/1_Open_WebRTCAppEE_and_Let_Browser_Access_Cam_and_Mic.jpg)
    
    _WebRTCAppEE stands for WebRTCApp Enterprise Edition_
   
6. Write stream name or leave it as default and Press `Start Publishing` button. After you press the button, 
    "Publishing" blinking text should appear

    ![Press Start Publishing button](https://ant-media.github.io/Ant-Media-Server/doc/images/2_Press_Publish_Button.jpg)

7. Go to the `http://localhost:5080/WebRTCAppEE/player.html`

    ![Go to the player.html](https://ant-media.github.io/Ant-Media-Server/doc/images/3_Go_to_Play_Page.jpg)

8. Press `Start Play` button. After you press the button, webrtc stream should be started

    ![Press Start Playing Button](https://ant-media.github.io/Ant-Media-Server/doc/images/4_Press_Start_Play_Button.jpg)
    
9. Open `http://localhost:5080/WebRTCAppEE/player.html` in other tabs and Press `Start Playing` button again 
   to check how it plays and what the latency is. 
   
## Running on Remote Instances - Enable SSL For Ant Media Server Enterprise
If you are running the Ant Media Server Enterprise Edition in remote computer/instances, you may need SSL. If so, please follow the Enable SSL doc or [this blog post](https://antmedia.io/enable-ssl-on-ant-media-server/).

## Feedbacks

Please let us know your feedbacks about the latency and streaming or any other issue you have faced 
so that we can improve and let you try and use.

You can use contact form at [antmedia.io](https://antmedia.io) or `contact at antmedia dot io` e-mail to send your feedbacks

Thank you

[Ant Media](https://antmedia.io)

