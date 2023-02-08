# Introduction to plug-in structure

Ant Media plug-in architecture allows developers to customize the video feed while keeping the server source codes untouched. Plug-in structure have various scenarios to address the needs of the developers, for example MCU usage is developed as a plug-in by the Ant Media dev team for emphasizing the wide-range use cases for plug-in architecture.

In the most basic explanation, a plug-in is added into the regular flow of AMS in ways that we will examine.  
Here is what a plugin data flow looks like;

![Screenshot from 2022-04-04 16-19-31.png](@site/static/img/Screenshotfrom2022-04-04 16-19-31.png)

You can either get the decoded video frames( IFrameListener interface )or encoded video packets ( IPacketListener interface ) from Ant Media Server, then you can do whatever customization you require to do with them.

Custom Broadcasts

You can implement other streaming protocols for publishing to your Ant Media Server with custom broadcast plug-ins, already implemented protocols are WebRTC, RTMP, SRT and stream source pulling. [More information.](/v1/docs/custom-broadcasting)

### IFrameListener interface

Encoded packets and stream properties are sent to the plugin by this interface. In other words, you should implement this interface and register your concrete object to capture stream properties and also packets from AMS.

Methods of IPacketListener:

*   **AVPacket onPacket(String streamId, AVPacket packet)**
    
          Packets are sent to the plugin with this method. A packet may be video or audio packet.
        
    
*   **void writeTrailer()**
    
             called while stream closing
        
    
*   **void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo)**
    
          video stream properties are sent to the plugin with this method.
        
    
*   **void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo)**
    
           audio stream properties are sent to the plugin with this method.
        
    

### IPacketListener Interface

Encoded packets and stream properties are sent to the plugin by this interface. In other words, you should implement this interface and register your concrete object to capture stream properties and also packets from AMS.

Methods of IPacketListener:

*   **AVPacket onPacket(String streamId, AVPacket packet)**
    
          packets are sent to the plugin with this method. A packet may be video or audio packet.
        
    
*   **void writeTrailer()**
    
          called while stream closing
        
    
*   **void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo)**
    
          video stream properties are sent to the plugin with this method.
        
    
*   **void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo)**
    
          audio stream properties are sent to the plugin with this method.