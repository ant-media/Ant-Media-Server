# React Native SDK

Ant Media's WebRTC React Native SDK lets you build your own React Native application that can publish and play WebRTC broadcasts with just a few lines of code.

In this doc, we're going to cover the following topics.

*   [Pre-Requisite For React Native Development](/v1/docs/react-native-sdk#prerequisite-for-react-native-development)
    
    *   [Software Requirements](/v1/docs/react-native-sdk#software-requirements)
    *   [Verify React Native Installation By running a sample app](/v1/docs/react-native-sdk#how-to-setup-your-first-application)
*   [Download and ready WebRTC sample apps](/v1/docs/react-native-sdk#download-and-ready-webrtc-sample-apps)
    
    *   [Download the Sample React Native projects](/v1/docs/react-native-sdk#download-the-sample-react-native-projects)
    *   [Install dependencies and run sample projects](/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects)
*   [Run the sample WebRTC React Native apps](/v1/docs/react-native-sdk#run-the-sample-react-native-app)
    
    *   [Publish stream from your React Native sample app](/v1/docs/react-native-sdk#publish-stream-from-your-sample-react-native-app)
    *   [Play stream on your React Native sample app](/v1/docs/react-native-sdk#play-stream-from-your-sample-react-native-app)
    *   [P2P communication with your React Native sample app](/v1/docs/react-native-sdk#p2p-communication-with-sample-react-native-app)
    *   [Conference with your React Native sample app](/v1/docs/react-native-sdk#conference-with-your-react-native-sample-app)
    *   [Data Channel with your React Native Sample app](/v1/docs/react-native-sdk#data-channel-with-your-react-native-sample-app)
*   [Using WebRTC React Native SDK](/v1/docs/react-native-sdk#using-webrtc-react-native-sdk)
    
    *   [Install @antmedia/react-native-ant-media Package](/v1/docs/react-native-sdk#install-antmediareactnativeantmedia-package)
    *   [How to publish](/v1/docs/react-native-sdk#how-to-publish-a-stream)
    *   [How to play](/v1/docs/react-native-sdk#how-to-play-a-stream)
    *   [How to use peer 2 peer](/v1/docs/react-native-sdk#how-to-use-peer-2-peer)
    *   [How to use conference](/v1/docs/react-native-sdk#how-to-use-conference)
    *   [How to use the data channel](/v1/docs/react-native-sdk#how-to-use-the-data-channel)

Pre-Requisite for React Native development
------------------------------------------

### Software requirements

*   Android Studio (IDE)
*   Android SDK
*   Java
*   NodeJs
*   NPM
*   React Native CLI

First of all, you’re required to install the node on your system. If your machine already has Node.js installed, then skip this step.

**Node.js Installation**

![](@site/static/img/image(83).png)

Download the latest Node.js from here [nodejs.org/en](http://nodejs.org/en)

Once the setup is downloaded on your system, run the .msi downloaded file and follow the prompt instructions to install the application.

Furthermore, make ascertain that the Node and NPM have been installed.

Use the Below Commands-

Text

Text

    node -v
    

Text

Text

    npm –v
    

**React Native**

Use the command

Text

Text

    npm install -g react-native-cli
    

in the command terminal to install React Native.

**Android Development Environment**

Download & Install the Android Studio [https://developer.android.com/studio/install.html](https://developer.android.com/studio/install.html)

![](@site/static/img/image(84).png)

The Android Studio lets you run the Reactive Native application in an emulator and test the application.

### Verify React Native Installation By running a sample app

We’ll be building a project using React Native by running the following command:

Text

Text

    react-native init MySampleApp
    

in the terminal from the folder where you will be creating the application.

Now, run commands below:

Text

Text

    react-native start
    

Text

Text

    react-native run-android
    

 Make sure you’ve started the emulator on your machine.

This Is how the sample project will look like in the Emulator:

![](@site/static/img/image(85).png)

Download and install WebRTC sample apps
---------------------------------------

### Download sample React Native projects

WebRTC React Native samples are free to download. You can access them through this [link on Github](https://github.com/ant-media/WebRTC-React-Native-SDK).

After downloading the whole project, you can see all samples React Native projects in the ```**samples**``` folder.

  

![](data:image/png;base64,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)

  

### Install dependencies and run sample projects

For installing dependencies you can use **npm** or as an alternative method, you can also use **yarn**.

Install **yarn** by [following this link.](https://classic.yarnpkg.com/lang/en/docs/install/#windows-stable)

First, you need to open the terminal on the root directory of the sample project you want to install and then run commands below.

To install dependencies by npm run ```npm install```

or

To install dependencies by yarn run ```yarn install```

After dependencies are installed , you need to run commands below in order to run the project itself.

Run ```npm run android``` if using npm

or

Run ```yarn android``` if using yarn

>` Note: If you want to use **npm**, then  follow only npm commands and if you want to use **yarn** then follow only yarn commands.

After the project starts successfully, sample app will appear on the device/emulator.

Run the sample React Native app
-------------------------------

### Publish a stream from your sample React Native app

*   Open **/samples/publish/src/App.tsx** file and update **defaultStreamName** variable for stream name and update **webSocketUrl** variable with server name.

![](@site/static/img/image-1654599250441.png)

*   Move to **/samples/publish** folder and follow the [Install dependencies and run sample projects](/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects) steps to run the **Publish** sample React native app.

![](@site/static/img/image-1654599372613.png)

*   Tap ```Start Publishing``` button on the screen. After the clicking ```Start Publishing```, the stream will be published on Ant Media Server.
*   You can now go to the web panel of Ant Media Server (e.g http://server\_ip:5080) and watch the stream there. You can also quickly play the stream via ```https://your_domain:5443/WebRTCAppEE/player.html```

### Play stream from your sample React Native app

*   Open **/samples/play/src/Play.tsx** file and update **defaultStreamName** variable for stream name and update **webSocketUrl** variable with server name.![](@site/static/img/image-1654599250441.png)

*   Go to **/samples/play** folder and follow the [Install dependencies and run sample projects](/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects) steps to run the **Play** sample React native app.

*   Before playing, make sure that there is a stream that is already publishing to the server with the same stream id in your **defaultStreamName** variable(You can quickly publish to the Ant Media Server via ```https://your_domain:5443/WebRTCAppEE```).

![](@site/static/img/image-1654599731503.png)

*   Tap ```Start Playing``` button on the screen. After clicking Start Playing , the stream will start playing.

![](@site/static/img/image-1654600749349.png)

### P2P communication with sample React Native app

*   Open **/samples/peer/src/Peer.tsx** file and update **defaultStreamName** variable for stream name and update **webSocketUrl** variable with server name.![](@site/static/img/image-1654599250441.png)
*   Go to **/samples/peer** folder and follow the [Install dependencies and run sample projects](https://portal.document360.io/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects) steps to run the **Peer** sample React native app.

![](@site/static/img/image-1654601111460.png)

*   When there is another peer connected to the same stream ID via Android, iOS or web, the P2P communication will be established and you can talk to each other. You can quickly connect to the same stream id via ```https://your_domain:5443/WebRTCAppEE/peer.html```

### ![](@site/static/img/image-1654601140029.png)

  

### Conference with your React Native sample app

*   Open **/samples/conference/src/conference.tsx** file and update **defaultRoomName** variable for stream name and update **webSocketUrl** variable with server name.

![](@site/static/img/image-1655196972089.png)

*   Go to **/samples/conference** folder and follow the [Install dependencies and run sample projects](https://portal.document360.io/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects) steps to run the C**onference** sample React native app.

![](@site/static/img/image-1655197421323.png)

*   When there are other streams connected to the same room id via Android, iOS or Web, then a conference room will be established and you can talk to each other. You can quickly connect to the same stream id via
    
    https://your\_domain:5443/WebRTCAppEE/conference.html![](@site/static/img/image-1655197140679.png)  
    

  

### Data Channel with your React Native Sample app

*   Open **/samples/DataChannel/src/[](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx "Chat.tsx")**[](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx "Chat.tsx")[**Chat**.](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx "Chat.tsx")[](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx "Chat.tsx")**[](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx "Chat.tsx").tsx** file and update **defaultStreamName** variable for stream name and update **webSocketUrl** variable with server name.![](@site/static/img/image-1654599250441.png)

*   Go to **/samples/DataChannel** folder and follow the [Install dependencies and run sample projects](/v1/docs/react-native-sdk#install-dependencies-and-run-sample-projects) steps to run the **Play** sample React native app.
*   Tap **Publish** button to start publishing in data channel.

![](@site/static/img/image-1656673042845.png)

*   After that you can start sending messages using send button and also can see the received button, You can also quickly play the stream via https://your\_domain:5443/WebRTCAppEE/player.html and send and receive the data channel messages.

![](@site/static/img/image-1656673300268.png)

Using WebRTC React Native SDK
-----------------------------

Before moving forward with using WebRTC React Native SDK, we highly recommend using the sample project to get started with your application. It's good to know the dependencies and how it works in general.

### **Install @antmedia/react-native-ant-media Package**

**```npm```**

Text

Text

    npm i @antmedia/react-native-ant-media react-native-webrtc

**```yarn```**

Text

Text

    yarn add @antmedia/react-native-ant-media react-native-webrtc
    

### **initialize useAntMedia adaptor**

TypeScript

TypeScript

    // . . .
    
    import { useAntMedia, rtc_view } from "@antmedia/react-native-ant-media";
    
    const adaptor = useAntMedia({
      url: webSocketUrl, // your web socket server URL
      mediaConstraints: {
        // mediaConstraints
        audio: true,
        video: {
          width: 640,
          height: 480,
          frameRate: 30,
          facingMode: "front",
        },
      },
      onlyDataChannel: false, // for using only data channel not audio and video
      callback(command, data) {}, // check info callbacks
      callbackError: (err, data) =>` {}, // // check error callbacks
      peer_connection_config: {
        // peerconnection_config
        iceServers: [{ urls: "stun:stun1.l.google.com:19302" }],
      },
      debug: true, // debug true / false
    });
    
    // Then, in another part of your script, you can start streaming by calling the publish method
    adaptor.publish(streamName);
    
    // . . .
    

The example above is taken from [ant-media / WebRTC-React-Native-SDK](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/main/samples/publish/src/App.tsx)

### **How to publish a stream**

The method below is used to publish a stream.

TypeScript

TypeScript

    // . . .
    
    adaptor.publish(streamName);
    
    // . . .
    
    

The method below is used to stop publish

TypeScript

TypeScript

    // . . .
    
    adaptor.stop(streamName);
    
    // . . .
    
    

Detailed code can be viewed in [ant-media / WebRTC-React-Native-SDK Publish](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/main/samples/publish/src/App.tsx)

### **How to play a stream**

The method below is used to play a stream.

TypeScript

TypeScript

    // . . .
    
    adaptor.play(streamName);
    
    // . . .
    
    

Detailed code can be viewed in [ant-media / WebRTC-React-Native-SDK Play](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/main/samples/play/src/Play.tsx)

### How to use peer 2 peer

The method method is used to join a room.

TypeScript

TypeScript

    // . . .
    
    adaptor.join(streamName);
    
    // . . .
    
    

The method below is used to leave a room.

TypeScript

TypeScript

    // . . .
    
    adaptor.leave(streamName);
    
    // . . .
    
    

Detailed code can be viewed in [ant-media / WebRTC-React-Native-SDK p2p](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/main/samples/peer/src/Peer.tsx)

### How to use conference

The method below is used to join a room.

TypeScript

TypeScript

    // . . .
    
    adaptor.joinRoom(room);
    
    // . . .
    
    

The method below is used to leave a room.

TypeScript

TypeScript

    // . . .
    
    adaptor.leaveFromRoom(room);
    
    // . . .
    
    

Detailed code can be viewed in [ant-media / WebRTC-React-Native-SDK Conference](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/main/samples/conference/src/conference.tsx)

### How to use the data channel

The method below is used to send messages.

TypeScript

TypeScript

    // . . .
    
    adaptor.sendData(streamId, message);
    
    // . . .
    
    

Detailed code can be viewed in [ant-media / WebRTC-React-Native-SDK Data Channel](https://github.com/ant-media/WebRTC-React-Native-SDK/blob/develop/samples/DataChannel/src/Chat.tsx)

>` Check Also: [WebRTC-React-Native-SDK](https://github.com/ant-media/WebRTC-React-Native-SDK)