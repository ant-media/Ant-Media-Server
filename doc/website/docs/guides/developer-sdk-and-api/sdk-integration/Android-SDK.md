# Android SDK

Ant Media's WebRTC Android SDK lets you build your own Android application that can publish and play WebRTC broadcasts with just a few lines of code.

Run the sample WebRTC Android app
---------------------------------

### Download the WebRTC Android SDK

WebRTC Android SDK is free to download. You can access them through [this link on Github.](https://github.com/ant-media/WebRTCAndroidSDK) If you're an Enterprise Edition user, it is also available for you to download on your subscription page. After you download the SDK, you can just unzip the file and open the project with Android Studio.

### Open and run the project in Android

Click "**Open an Existing Android Studio** **Project**"; a window should open as shown below for the project path.

![](@site/static/img/android-open-project-path.png)

Select your project's ```build.gradle``` file path and click the OK button. Here, you need to set ```SERVER_ADDRESS``` parameter in MainActivity.java:

![](@site/static/img/server_address.png)

### Publish stream from your Android app

*   In ```MainActivity.java```, you need to set ```webRTCMode``` parameter to ```IWebRTCClient.MODE-PUBLISH```
*   In ```MainActivity.java```, set the stream id to anything else then ```stream1``` i.e. ```streamTest1```:

![](@site/static/img/streamId.png)

*   Tap ```Start Publishing``` button on the main screen. After the clicking ```Start Publishing```, stream will be published on Ant Media Server.
*   You can go to the web panel of Ant Media Server (http://server\_ip:5080) and watch the stream there. You can also quickly play the        stream via ```https://your_domain:5443/WebRTCAppEE/player.html```

### Play stream from your Android app

First, you need to set ```webRTCMode``` parameter to ```IWebRTCClient.MODE_PLAY``` in ```MainActivity.java```.

Playing a stream on your Android is almost the same as publishing. Before playing, make sure that there is already a stream that is publishing to the server with the same stream id as your ```streamId``` parameter (you can quickly publish to the Ant Media Server via ```https://your_domain:5443/WebRTCAppEE```). Here, you just need to tap ```Start Playing``` button.

### P2P communication with your Android app

WebRTC Android SDK supports P2P communication. First, you need to set ```webRTCMode``` parameter to ```IWebRTCClient.MODE_JOIN``` in ```MainActivity.java```.

When there is another peer connected to the same stream ID via Android, iOS, or Web, then P2P communication will be established and you can talk to each other. You can quickly connect to the same stream id via ```https://your_domain:5443/WebRTCAppEE/peer.html```

### Join a conference room with your Android app

WebRTC Android SDK supports the conference room feature. First, you need to put the room Id in ConferenceActivity.java file as shown below:

    String streamId = null; //"stream1";
    String roomId = "room1";

After that, just change the launcher activity to ".ConferenceActivity" in the AndroidManifest.xml file:

    `<activity android:name=".ConferenceActivity">`
            `<intent-filter>`
                   `<action android:name="android.intent.action.MAIN" />`
    
                   `<category android:name="android.intent.category.LAUNCHER" />`
            `</intent-filter>`
    `</activity>`

When there are other streams connected to the same room Id via Android, iOS, or the Web, then a conference room will be established and you can talk to each other. You can quickly connect to the same conference room via https://your\_domain:5443/WebRTCAppEE/conference.html

### Screen share with your Android app

WebRTC Android SDK also supports screen sharing. First you need to put the streamId and your Ant Media server address in the ScreenCaptureActivity.java file as shown below:

    String streamId = "stream36";
    String tokenId = "tokenId";
    String url = "ws://domain-or-IP:5080/WebRTCAppEE/websocket"

After that, just change the launcher activity to ".ScreenCaptureActivity" in the AndroidManifest.xml file:

    `<activity android:name=".ScreenCaptureActivity">`
            `<intent-filter>`
                   `<action android:name="android.intent.action.MAIN" />`
    
                   `<category android:name="android.intent.category.LAUNCHER" />`
            `</intent-filter>`
    `</activity>`

Develop a WebRTC Android app
----------------------------

We highly recommend using the sample project to get started on your application. It's good to know the dependencies and how it works, so we're going to explain how to create a WebRTC Android app from scratch. 

### Creating an Android project

#### Open Android Studio and create a new Android Project

Just click ```**File >` New >` New Project**```. Choose "Empty Activity" in the next window:

![](@site/static/img/setup_new_1.png)

 Click Next button and a window should open as shown below for the project details. Fill out the form:

![](@site/static/img/setup_new_2.png)

Click Finish and complete creating the project.

#### Import WebRTC SDK as a module to Android project

After creating the project, let's import the WebRTC Android SDK. For this, click ```**File >` New >` Import Module**```. Choose the directory of the WebRTC Android SDK and click the Finish button.

![](@site/static/img/image-1645178806898.png)

 If the module is not included in the project, add the module name into ```settings.gradle``` file as shown in the image below.

![](@site/static/img/image-1645178854491.png)

#### Add dependency to Android Project app module

*   Right-click ```app```, choose ```Open Module Settings``` and click the ```Dependencies``` tab. Then a window should appear as below. Click the ```+``` button at the bottom and choose "Module Dependency".
*   Choose WebRTC Native Android SDK and click the OK button.

**Here, the most critical thing about this step is that you need import the module as an API. It will look like as in the image below after adding the dependency:****![](@site/static/img/android_after_adding_native_sdk.png)**

### Prepare the app for streaming

This is just a simple Android app development so we won't dive into details here. You can read lots of tutorials about this on the Internet.

First, create a MainActivity.java Class and add a Button to your activity main layout. 

Then, add permissions to the Manifest file.

Open the AndroidManifest.xml and add the below permissions between ```application``` and ```manifest``` tag:

        `<uses-feature android:name="android.hardware.camera" />`
        `<uses-feature android:name="android.hardware.camera.autofocus" />`
        `<uses-feature
            android:glEsVersion="0x00020000"
            android:required="true" />`
    
        `<uses-permission android:name="android.permission.CAMERA" />`
        `<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />`
        `<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />`
        `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
        `<uses-permission android:name="android.permission.BLUETOOTH" />`
        `<uses-permission android:name="android.permission.INTERNET" />`
        `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />`
        `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
        `<uses-permission android:name="android.permission.READ_PHONE_STATE" />`
        `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />`

#### Implement MainActivity onCreate function

Open the MainActivity.java and implement it as below. You should change ```SERVER_URL``` according to your Ant Media Server address. Secondly, the third parameter in the last line of the code below is ```IWebRTCClient.MODE_PUBLISH``` that publishes the stream to the server. You can use ```IWebRTCClient.MODE_PLAY``` for playing stream and ```IWebRTCClient.MODE_JOIN``` for P2P communication. If token control is enabled, you should define ```tokenId``` parameter.

       /**
         * Change this address with your Ant Media Server address
         */
        public static final String SERVER_ADDRESS = "serverdomain.com:5080";
    
        /**
         * Mode can Publish, Play or P2P
         */
        private String webRTCMode = IWebRTCClient.MODE_PLAY;
    
        public static final String SERVER_URL = "ws://"+ SERVER_ADDRESS +"/WebRTCAppEE/websocket";
        public static final String REST_URL = "http://"+SERVER_ADDRESS+"/WebRTCAppEE/rest/v2";
        private CallFragment callFragment;
    
        private WebRTCClient webRTCClient;
    
        private Button startStreamingButton;
        private String operationName = "";
        private Timer timer;
        private String streamId;
    
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    
            // Set window styles for fullscreen-window size. Needs to be done before
            // adding content.
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            //getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
    
            setContentView(R.layout.activity_main);
            SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.camera_view_renderer);
            SurfaceViewRenderer pipViewRenderer = findViewById(R.id.pip_view_renderer);
    
            startStreamingButton = (Button)findViewById(R.id.start_streaming_button);
    
            // Check for mandatory permissions.
            for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
                if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
    
            if (webRTCMode.equals(IWebRTCClient.MODE_PUBLISH)) {
                startStreamingButton.setText("Start Publishing");
                operationName = "Publishing";
            }
            else  if (webRTCMode.equals(IWebRTCClient.MODE_PLAY)) {
                startStreamingButton.setText("Start Playing");
                operationName = "Playing";
            }
            else if (webRTCMode.equals(IWebRTCClient.MODE_JOIN)) {
                startStreamingButton.setText("Start P2P");
                operationName = "P2P";
            }
    
            this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
            this.getIntent().putExtra(EXTRA_VIDEO_FPS, 30);
            this.getIntent().putExtra(EXTRA_VIDEO_BITRATE, 2500);
            this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
    
            webRTCClient = new WebRTCClient( this,this);
    
            //webRTCClient.setOpenFrontCamera(false);
    
            streamId = "stream1";
            String tokenId = "tokenId";
    
            webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);
    
           // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
            webRTCClient.init(SERVER_URL, streamId, webRTCMode, tokenId, this.getIntent());
        }
    
        public void startStreaming(View v) {
            if (!webRTCClient.isStreaming()) {
                ((Button)v).setText("Stop " + operationName);
                webRTCClient.startStream();
            }
            else {
                ((Button)v).setText("Start " + operationName);
                webRTCClient.stopStream();
            }
        }

#### Create activity\_main.xml layout

Create an activity\_main.xml layout file and add the lines below. 

        `<?xml version="1.0" encoding="utf-8"?>`
        `<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            xmlns:tools="http://schemas.android.com/tools"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:context=".MainActivity">`
    
        `<org.webrtc.SurfaceViewRenderer
            android:id="@+id/camera_view_renderer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center" />`
    
        `<org.webrtc.SurfaceViewRenderer
            android:id="@+id/pip_view_renderer"
            android:layout_height="144dp"
            android:layout_width="wrap_content"
            android:layout_gravity="bottom|end"
            android:layout_margin="16dp"/>`
    
        `<Button
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Start"
            android:id="@+id/start_streaming_button"
            android:onClick="startStreaming"
            android:layout_gravity="bottom|center"/>`
    
        `<FrameLayout
            android:id="@+id/call_fragment_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />`
    
        `</FrameLayout>`

### **How to publish a stream**

We need to change a line in **onCreate**. After this, the following code snippet will publish the stream on your server with ```streamId```: 'stream1'.

You need to set ```webRTCMode``` to ```IWebRTCClient.MODE_PUBLISH```.

    private String webRTCMode = IWebRTCClient.MODE_PUBLISH;

### **How to play a stream**

Playing a stream is almost the same with publishing. We just need to change a few lines in onCreate. As a result, the following code snippets will play the stream on your server with ```streamId```: 'stream1'. Note that before you try to play, you need to publish a stream to your server with having stream id 'stream1'

Below, you need to set ```webRTCMode``` to ```IWebRTCClient.MODE_PLAY```.

    private String webRTCMode = IWebRTCClient.MODE_PLAY;

### **How to use a data channel**

Ant Media Server and Android SDK can use data channels in WebRTC. In order to use a data channel, make sure that it's enabled both [server-side](https://github.com/ant-media/Ant-Media-Server/wiki/Data-Channel) and mobile.

Before initialization of webRTCClient you need to do the following: 

*   Set your data channel observer in the WebRTCClient object like this:

    webRTCClient.setDataChannelObserver(this);

*   Enable data channel communication by adding the following key-value pair to your **Intent** before initialization of WebRTCClient with it:

    this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);

Then your Activity is ready to send and receive data.

*   To send data, call ```sendMessageViaDataChannel``` method of WebRTCClient and pass the raw data like this on click of a button:

    webRTCClient.sendMessageViaDataChannel(buf);

![](@site/static/img/android_data_channel.png)

### How to use conference room

Ant Media Server also supports the ConferenceRoom feature. For this, you need to initialize ```ConferenceManager```.

        private ConferenceManager conferenceManager;
    
        conferenceManager = new ConferenceManager(
                    this,
                    this,
                    getIntent(),
                    MainActivity.SERVER_URL,
                    roomId,
                    publishViewRenderer,
                    playViewRenderers,
                    streamId,
                    this
         );

Also check conference document for more details: [https://resources.antmedia.io/docs/webrtc-conference-call](https://resources.antmedia.io/docs/webrtc-conference-call)