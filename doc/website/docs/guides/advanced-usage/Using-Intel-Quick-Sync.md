# Using Intel Quick Sync

In order to use QuickSync encoders, the following packages should be installed. This setup procedure is for ubuntu 18.04.

QuickSync is supported in Ant Media Server v2.4.0 and later versions.

### Basic requirements

    sudo apt install cmake build-essential pkg-config autoconf libtool libdrm-dev libva-dev libx11-dev

Make sure to enable the graphics cards in BIOS (```Chipset``` >` ```North Bridge``` >` ```Graphics Configuration):```

![](@site/static/img/quick_sync_bios_configuration.jpeg)

### Install GMMLIB

    git clone https://github.com/intel/gmmlib.git
    cd gmmlib/
    mkdir build && cd build
    cmake -DCMAKE_BUILD_TYPE=Release -DARCH=64 ..
    make 
    sudo make install
    cd ../..

### Install Libva

    git clone https://github.com/intel/libva.git
    cd libva/
     ./autogen.sh
    make 
    sudo make install
    cd ..

### Install Intel media driver

    git clone https://github.com/intel/media-driver.git
    mkdir build_media
    cd build_media
    cmake ../media-driver
    make 
    sudo make install
    cd ..

### Install Intel media SDK

    git clone https://github.com/Intel-Media-SDK/MediaSDK msdk
    cd msdk
    mkdir build && cd build
    cmake ..
    make
    sudo make install
    echo "/opt/intel/mediasdk/lib/" >` msdk.conf
    sudo mv msdk.conf /etc/ld.so.conf.d/
    sudo ldconfig

### Install and configure Ant Media Server

First, install Ant Media Server. Then, edit the properties file:

    sudo nano /usr/local/antmedia/webapps/WebRTCAppEE/WEB-INF/red5-web.properties 

 In this file, add ```settings.encoding.encoderName=h264_qsv``` save and exit from the editor.

Add ```antmedia``` user to ```video``` group

    sudo usermod -aG video antmedia

Restart the Ant Media Server

    sudo service antmedia restart

*   Go to the web panel and add adaptive bitrate.
*   Publish stream with RTMP or WebRTC.
*   Check the logs if h264\_qsv is opened. You should see something like that:

    2021-06-27 07:17:06,209 [vert.x-worker-thread-2] INFO  i.a.e.adaptive.video.H264Encoder - Video codec opened. Context gop size: 40  keyint mint 25 extradata size: 47 video codec timebase: 1/20  codecName: h264_qsv for stream: stream1