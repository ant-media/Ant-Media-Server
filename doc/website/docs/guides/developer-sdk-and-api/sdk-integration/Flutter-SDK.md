# Flutter SDK

Ant Media WebRTC Flutter SDK helps you in building your Flutter application that can publish and play WebRTC broadcasts with just a few lines of code. This document explains how to configure Flutter SDK and run the sample applications.


Prerequisite for Flutter development
------------------------------------

### Software requirements

*   Android Studio (For Android)
*   XCode (For iOS)

>` Please note, you can also run the projects through the terminal or command-line, however, for better debugging, consider using Android Studio or XCode. This document considers the use of Android Studio for building and running applications.

### How to set up your first application on Flutter

1.  Install Flutter, please follow the installation steps mentioned here [Install Flutter](https://docs.flutter.dev/get-started/install?gclid=Cj0KCQjwg_iTBhDrARIsAD3Ib5jaxKUnDo7Vc2XMY1sZSPRPkt1CRsb-ALyYuUMFrrnalhPkrIlTLaIaAvcbEALw_wcB&gclsrc=aw.ds).
2.  Install the Android Studio, please follow the installation steps mentioned here [Install Android Studio](https://developer.android.com/studio/install.html). 
3.  Add the Dart language plugins and Flutter extensions to Android Studio. Please follow below operating system specific installation instructions.
    1.  For macOS, use the following instructions:
        1.  Start Android Studio
        2.  Open plugin preferences (Preferences >` Plugins as of v3.6.3.0 or later)
        3.  Select the Flutter plugin and click Install
        4.  Click Yes when prompted to install the Dart plugin
        5.  Click Restart when prompted
    2.  For Linux or Windows, use the following instructions:
        1.  Start Android Studio
        2.  Open plugin preferences (File >` Settings >` Plugins)
        3.  Select Marketplace, select the Flutter plugin and click Install
        4.  Click Yes when prompted to install the Dart plugin
        5.  Click Restart when prompted
4.  To verify the Flutter installation, please create and sample an app and build it by following the instructions provided here [Create a Flutter project](https://docs.flutter.dev/get-started/codelab). 

Download and run the WebRTC sample projects
-------------------------------------------

### Download the sample Flutter projects

*   Clone or Download the SDK code from here [WebRTC Flutter SDK](https://github.com/ant-media/WebRTC-Flutter-SDK/).

### Configuration of the sample project

Open SDK in Android studio, and make sure you have installed the Flutter and Dart plugins. 

Make sure that the paths of Flutter and Dart SDK are correctly configured in Android Studio.

**File >` Settings >` Languages & Frameworks**

  

![](@site/static/img/image-1654690015352.png)

If these plugins have been installed and the locations of Flutter and Dart SDK are configured correctly, then the options to run the samples will appear automatically after source code indexing. Please refer to the below screenshot.

  

![](https://lh3.googleusercontent.com/XaiEOZCJbTupBrZ_cBmzZibQJMus7XpNi9bUInVftH2jPmTcuL5TYUTtJSr_RQmbCftmm_xCPSU1Rr1wvE642Oa8ltaj7I-X-luNEeY0nAFzRy-HFLwb1koi_25I7YPCv8ei8VFqJaBmNk0noA)

### Install dependencies and run sample project

In the project navigator, you will find two folders named samples and examples. In the example folder, there is an example project which uses the ant\_media\_flutter dependency with all options (Publish, Play, P2P and Conference) to test. 

In the samples folder, there are 4 separate projects to test publish, play, peer and conference individually.

All projects use [Ant Media Flutter](https://pub.dev/packages/ant_media_flutter ) dependency which is added in **pubspec.yaml** file. 

Text

    ant_media_flutter: ^0.0.8

Click on the **pub get** button to install the dependency in the project. Pub get button appears only when **pubspec.yaml** file is opened in the editor.

Run the sample WebRTC Flutter apps
----------------------------------

To run the sample apps on Android, you need to connect the Android device with your workstation. For that, make sure you have enabled the developer options and USB debugging on your device. On Android 4.1 and lower, the Developer options screen is available by default. To get the developer options screen on Android 4.2 and higher, follow the below steps:

*   Open the Settings app
*   Select System
*   Select About phone
*   Scroll to the build number and tap it 7 times
*   Return to the previous screen to find Developer options near the bottom
*   Scroll down and enable USB debugging

If USB debugging is enabled on your device, then your device name will automatically be available in the list of devices.

Just select the device and select the sample project from the target list and click on the run button. The Gradle task will start and wait for some time until the app builds. After the building is complete, a confirmation popup will come to your device for installation.

![](https://lh6.googleusercontent.com/p6us2BbRBh1Qq4PbqM6_GQ4HblTx3DOBI4Kgp9ssLrdEBNBGJbuegyqvr-PfbcLrI3xB1wbxYqVbsc4q78adKO_hQxLgrAchh0MVoHmlvH_d_ZYy15kjBhUuJkhWHI-PlYabSnXqts2D7Lau2Q )

Please follow the below instructions for running specific sample apps

### Running Publish Sample App

Select the Publish app from the target list and click the Run button. Once the app is running, enter the server IP address. For entering the IP address please follow the below steps.

*   Tap on the Setting icon in the top right corner of the application.
*   Enter the Server IP as ws://ant\_media\_server\_address:ip/WebRTCAppEE/websocket
*   Tap the 'Set Server Ip' button.

![](@site/static/img/IMG_F1372DF3182B-1(2).jpeg)

Select Publish list item.

![](@site/static/img/image-1654687508699.png)  

Enter the stream Id which you want to publish.

![](@site/static/img/image-1654687625473.png) Choose the publishing source. Please note, for the iOS app screen recording option, records the app's UI only, while the Android app records the whole complete device screen.

![](@site/static/img/image-1654687706637.png)

To verify whether the stream is published successfully or not,  please open the web panel of Ant Media Server (e.g http://server\_ip:5080) and check for the newly created stream. You can also quickly play the stream via web player [https://your\_domain:5443/WebRTCAppEE/player.html](https://your_domain:5443/WebRTCAppEE/player.html )

### Running Play Sample App

Select the Play app from the target list and click the Run button. Once the app is running, enter the server IP address. For entering the IP address please follow the below steps.

*   Tap on the Setting icon in the top right corner of the application.
*   Enter the Server IP as ws://ant\_media\_server\_address:ip/WebRTCAppEE/websocket
*   Tap the 'Set Server Ip' button.

![](@site/static/img/IMG_02A254033728-1(1).jpeg)

Before playing, make sure that there is a stream that is already publishing to the server with the same stream id in your streamId parameter (You can quickly publish to the Ant Media Server via https://your\_domain:5443/WebRTCAppEE).

Select Play from the list item.

![](@site/static/img/image-1654688010762.png)Enter the stream Id which you want to play.

![](@site/static/img/image-1654688079112.png)

### Running Peer Sample App

Select the Peer app from the target list and click the Run button. Once the app is running, enter the server IP address. For entering the IP address please follow the below steps.

*   Tap on the Setting icon in the top right corner of the application.
*   Enter the Server IP as ws://ant\_media\_server\_address:ip/WebRTCAppEE/websocket
*   Tap the 'Set Server Ip' button.

![](@site/static/img/IMG_61C65FD7D641-1.jpeg)

Select P2P from the list item.

![](https://lh5.googleusercontent.com/n47OOeKbuiLx-xrAObZYkT1B0lx-2-Dkcxwgqri9pr9zfKK4u1RfeADusJwxR11MOcyly-pwiIxF8dBWmqY1I_QNcokHiazCgphUQxyW015Vi6OYT6Qpf6ONjsV3hdP0FZ2RuN0rZYy7XS4b4w )

Enter the stream Id which you want to join in p2p.

![](https://lh3.googleusercontent.com/v7SfoBnIbnS-mexwFN6NbHapQQGeEWFYkJGkAL24ww6vi9iJ4SbTdIwcmKUxeXpLjkY2xuwlwe5A5y_T6oWqci1pAZVIgnVQUPm59TYV_HCwro6LVFgZSrGorQI3UyxILwpIPXX1YYY1wnMAVg )

When there is another peer connected to the same stream ID via Android, iOS, or the web, the P2P communication will be established and you can talk to each other. You can quickly connect to the same stream id via [https://your\_domain:5443/WebRTCAppEE/peer.html](https://your_domain:5443/WebRTCAppEE/peer.html )

### Running Conference Sample App

Select the Conference app from the target list and click the Run button. Once the app is running, enter the server IP address. For entering the IP address please follow the below steps.

*   Tap on the Setting icon in the top right corner of the application.
*   Enter the Server IP as ws://ant\_media\_server\_address:ip/WebRTCAppEE/websocket
*   Tap the 'Set Server Ip' button.

![](@site/static/img/IMG_7404B1521BFA-1.jpeg)

Select Conference from list item.

![](https://lh3.googleusercontent.com/UV-_SAwEqBhU6IvWj3yWLd9rqAyNbVVlVktP-609CbjxtSjg4-ssoAK8Qvom8HLTOoRovoMIbl_Ae-HH7mdb30B_3tideWT-d6fx2nl7IB5LsX3oSbTFhOPIYLAIck0aLOvyqoLWObNrDilOkA )

Enter the stream Id and room id which you want to join for the conference.

![stream and room id.jpeg ](https://lh6.googleusercontent.com/AGwDbjsjQmCX9BNcKNGVHSliJ6V0IFxTyhihca7xK0M7uyllrLuT0Frglzfp1l6v1OZIZeMsHSi7Fh4FNKiT-eyCST5nI3YLJuQzQi4a-X_b1W96LNJRPCR3q_VihaAOePu3dGFwePLKeyW5-A )Now, you can quickly connect to the same stream id via [https://your\_domain:5443/WebRTCAppEE/conference.html](https://your_domain:5443/WebRTCAppEE/conference.html)

  

### Running DataChannel Sample App

Select the DataChannel app from the target list and click the Run button. Once the app is running, enter the server IP address. For entering the IP address please follow the below steps.

*   Tap on the Setting icon in the top right corner of the application.
*   Enter the Server IP as ws://ant\_media\_server\_address:ip/WebRTCAppEE/websocket
*   Tap the 'Set Server Ip' button.

![](@site/static/img/IMG_A2C7D0611FF7-1.jpeg)

Select DataChannel from list item.

       ![](@site/static/img/IMG_634E4AB6B820-1.jpeg)

  

Enter the stream Id which you want to publish for the data channel.

  

![](@site/static/img/IMG_E70F9A26E9DD-1.jpeg)

  

After entering the streamId following type of chat screen will appear.

  

![](@site/static/img/IMG_2600ABC725B1-1.jpeg)

Now, you can quickly connect to the same stream id via [https://your\_domain:5443/WebRTCAppEE/player.html](https://your_domain:5443/WebRTCAppEE/conference.html) and click on options.

You can see the data channel chat UI. You can enter the message and click send. The message will update on your device screen. You can also send the message through your device.

**Using WebRTC Flutter SDK**
----------------------------

Before moving forward with using WebRTC Flutter SDK, we highly recommend using the sample projects to get started with your application. It's good to know the dependencies and how it works in general.

### Install ant\_media\_flutter package from **[pub.dev](https://pub.dev/)**

    ant_media_flutter: ^*.*.*   # latest version

**Initialise** **imports and request permission from Flutter-SDK**

    import 'package:ant_media_flutter/ant_media_flutter.dart';

    AntMediaFlutter.requestPermissions();
      
    if (Platform.isAndroid) {
        AntMediaFlutter.startForegroundService();
      }

**Set stream Id and server URL**

The method below is used to publish a stream.

     // Replace your own domain name  and port number with this domain name and port. 
    String serverUrl = “wss://domain:port/WebRTCAppEE/websocket”;
    
    // Set stream id 
    String _streamId = 'testStream';

#### **How to use the SDK.**

There is a common function used in ant\_media\_flutter.dart to achieve the functionalities of the Publish, Play,  Peer, Conference and DataChannel module. In this function we can pass  streamId,  server address ,  roomId ,  type of calling and all the callback functions which are described below  as parameters.  The method below is used as the common function.

    static void connect (
    String ip, 
    String streamId, 
    String roomId, 
    AntMediaType type, 
    bool userScreen, 
    bool forDataChannel,
    HelperStateCallback onStateChange, 
    StreamStateCallback onLocalStream, 
    StreamStateCallback onAddRemoteStream, 
    DataChannelCallback onDataChannel, 
    DataChannelMessageCallback onDataChannelMessage, 
    ConferenceUpdateCallback onupdateConferencePerson,
    StreamStateCallback onRemoveRemoteStream,)  async  {
       anthelper = null;
       anthelper ??= AntHelper(
           ip,
           streamId,
           roomId,
           onStateChange,
           onAddRemoteStream,
           onDataChannel,
           onDataChannelMessage,
           onLocalStream,
           onRemoveRemoteStream,
           userScreen,
           onupdateConferencePerson,
           forDataChannel )  
          ..connect(type);
    }

Here are the small description of all parameters

1\. **Ip** :- ip is the WebRTC server address which we want to use in our SDK, the format of this server is as follows. 

    ws://domain:port/WebRTCAppEE/websocket

2\. **streamId**:-  Stream id is the id of the stream which we want to use.

3\. **roomId** :- roomId is the id of the room in which we want to join our stream. In case of publishing roomId should be passed as a token.

4\. **type**:- The type is AntMediaType enum, there are 6 cases in this type. 

*   **Undefined** :- This is a default type.
*   **Publish** :- When we want to publish a stream or want to test data channel examples, we have to pass this type as        AntMediaType.Publish.
*   **Play** :- When we want to play a stream we have to pass this type as AntMediaType.Play.
*   **Peer**:- When we want to start a peer to peer connection we have to pass this type as AntMediaType.Peer.
*   **Conference**:- When we want to start a conference we have to pass this type as AntMediaType.Conference.

5\. **UserScreen**:- this function is used to change the type of publishing source when it is true screen recording will be published and  when it is false camera recording will be published.

6\. **forDataChannel** :- This is a bool type and this is specifically used in the case of publishing the WebRTC stream without using any recording option to achieve the data channel  functionality.  if we keep this value true so SDK will not publish any type of recording. Initialisation of data channel will not be affected by this property. data channel will be initialised in all cases (Publish , Play, Peer , Conference). Only the publishing of recordings will be affected by this value.

7\. **OnStateChange**:-  this  is a function which uses one parameter of HelperState type and returns nothing. HelperState type has these subtypes

*   **CallStateNew** :- This type of HelperState function is called when the call is started. We can write the change in UI code when a call has been started.
*   **CallStateBye**:- This HelperState function is called when the peer connection has been closed or we can say when the call has been finished.
*   **ConnectionOpen**:- This HelperState function is called when the web socket has been connected and opened.
*   **ConnectionClosed**:- This HelperState function is called when the web socket has been disconnected and closed.
*   **ConnectionError**:- This HelperState function is called when there is an error in making a peer connection.

8\. **onLocalStream**:- This function is a non return type function which has a MediaStream type parameter. The parameter is a local stream occurred by our device’s camera or screen recording. We can use this stream to see what we are publishing to the server. 

9\. **onAddRemoteStream**:- This function is a non return type function which has a MediaStream type parameter. The parameter is a remote stream occurred by a connected peer’s device’s camera or screen recording. We can use this stream to see what we are getting from the server.

10\. **onDataChannel**:- This function is a no return type function and uses the RTCDataChannel type parameter which refers to the data channel initialised.

11\. **onDataChannelMessage**:- This function is a no return type which uses three parameters 

*   dc :- this is the data channel by which the message is sent or received.
*   message:- this is the RTCDataChannelMessage type which is the message which we have received or sent.
*   isReceived:- This is a bool type. If this is true, a message is received. If false then a message is sent.

12\. **onUpdateConferencePerson**:- This is a no return type function and it is used in case of AntMediaType.Conference type connection. When the user joins the room and in that room other streams are added or removed so this function will be called. It uses a dynamic type function which is an array of stream Id which are joined in the room.

13. **onRemoveRemoteStream:-** This is a no return function which is called when any remote streams have been removed. It uses a MediaStream type parameter which is the stream which has been removed. This function is basically used to add the code to go back . when the call has been disconnected.