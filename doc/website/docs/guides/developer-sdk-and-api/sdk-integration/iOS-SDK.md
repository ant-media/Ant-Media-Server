# iOS SDK

Ant Media's WebRTC iOS SDK lets you build your own iOS application that can publish and play WebRTC broadcasts with just a few lines of code.

In this doc, we're going to cover the following topics.

*   Run the sample WebRTC iOS app
    *   Publish a stream from your iPhone app
    *   Play a stream on your iPhone app
    *   P2P communication with your iPhone app
*   Develop a WebRTC iOS app
    *   How to publish a stream
    *   How to play a stream
    *   How to use a data channel

Run the Sample WebRTC iOS app
-----------------------------

#### Download the WebRTC iOS SDK

WebRTC iOS SDK is free to download. You can access them through [this link on antmedia.io](https://github.com/ant-media/WebRTCiOSSDK). If you're an enterprise user, it will be also available for you to download on your subscription page. After you download the SDK, you can just unzip the file and open the project with Xcode.

#### Install Dependency

Open your terminal and go to the directory where the reference project resides and run ```pod install``` . If you are not familiar with pods, visit [cocoapods.org](https://cocoapods.org/) for documentation and installation.

Then, install the Starscream packet for WebSocket connections.

#### Open and run the project in Xcode

Open the Xcode in your MacOS and click **Open Another Project** if you don't see the Sample Project in your list.

Go to the directory where you download and unzip the iOS SDK. Open the ```AntMediaReferenceApplication.xcworkspace``` file.

After the project opens, connect your iPhone to your Mac and choose your iPhone in Xcode as shown below.

![](@site/static/img/choose_your_iphone_in_xcode.png)

Click ```Run``` button on the top left of the Xcode. Then project is going to be built and deployed to your iPhone.

![](@site/static/img/choose_your_iphone_in_xcode.png)

### Publish Stream from your iPhone

Tap ```Publish``` button and then tap ```Set Server IP``` under the connect button in your iPhone.  
![](@site/static/img/tap_publish_button.png)

Write Your Ant Media Server's full WebSocket URL and tap ```Save``` button. The format is ```ws://192.168.7.25:5080/WebRTCAppEE/websocket```. If you install SSL to Ant Media Server, you can also use ```wss://your_domain_address:5443/WebRTCAppEE/websocket```.

![](@site/static/img/set_server_ip.png)Set the stream id to anything else than 'stream1' and Tap 'Connect' button on the main screen. Then it will ask you to access the camera and mic. After you allow the camera and mc access, the stream will be published on Ant Media Server.

Now, it will start publishing to your Ant Media Server. You can go to the web panel of Ant Media Server (e.g http://server\_ip:5080) and watch the stream there. You can also quickly play the stream via https://your\_domain:5443/WebRTCAppEE/player.html

### Play a stream on your iPhone

Playing a stream on your iPhone is almost the same as Publishing. Before playing, make sure that there is a stream is already publishing to the server with the same stream id in your textbox (You can quickly publish to the Ant Media Server via https://your\_domain:5443/WebRTCAppEE). For our sample, the stream id is still "stream1" in the image below. Then you just need to tap 'Play' button and tap 'Connect' button.

![](@site/static/img/tap_play_button.png)

After clicking on the 'Connect' button, the stream will start playing.

### P2P communication with your iPhone

WebRTC iOS SDK also supports P2P communication. As you guess, just click on 'P2P' and then 'Connect' button.

When there is another peer connected to the same stream id via Android, iOS or web, then P2P communication will be established and you can talk to each other. You can quickly connect to the same stream id via https://your\_domain:5443/WebRTCAppEE/peer.html

Develop a WebRTC iOS app
------------------------

We highly recommend using the sample project to get started on your application. Nevertheless, it's good to know the dependencies and how it works. We're going to explain how to create a WebRTC iOS app from scratch. Let's get started.

#### Create Xcode Project

Open Xcode and create a project. Choose ```Single View App``` from the templates. Name your project as 'WebRTCiOSApp'.

Open your terminal and go to the directory where you create your project and make the pod installation. You can learn more about pods on [cocoapods.org](https://cocoapods.org/).

    cd /go/to/the/directory/where/you/create/the/project
    pod init

```Podfile``` should be created after running ```pod init```. Open the ```Podfile```, paste the following and save it. If you are having a problem about pod init command, just make your Xcode project Xcode 13.0 - compatible

    target 'WebRTCiOSApp' do
      # Comment the next line if you don't want to use dynamic frameworks
      use_frameworks!
    
      # Pods for WebRTCiOSApp
     pod 'Starscream', '~>` 4.0.4'
    
    
     post_install do |installer|
       installer.pods_project.targets.each do |target|
         target.build_configurations.each do |config|
           config.build_settings['BUILD_LIBRARY_FOR_DISTRIBUTION'] = 'YES'
         end
       end
     end
     
    end

Run the following command for pod installation

    pod install

Close the Xcode project and open the ```WebRTCiOSApp.xcworkspace``` in Xcode

![](@site/static/img/open_workspace_xcode.png)Make the project target iOS 10

![](@site/static/img/ios_10.png)

Disable bitcode:

![](@site/static/img/disable_bitcode.png)

*   Copy WebRTC.xcframework and WebRTCiOSSDK.xcframework folders to your projects directory.
    *   WebRTC.xcFramework is available under WebRTCiOSReferenceProject
    *   WebRTCiOSSDK.xcFramework is created by running ./export\_xc\_framework.sh in WebRTCiOSReferenceProject directory. After that, it will be ready under the Release-universal directory. Alternatively, you can import the source code of WebRTCiOSSDK to your project directly.
*   Embed WebRTC.xcframework and WebRTCiOSSDK.xcframework to your projects.

![](@site/static/img/click_add_items_to_embed.png)

*   Choose 'Add Others' at the bottom left and select Add Files. Then add WebRTC.xcframework and WebRTCiOSSDK.xcframework. After it's done, it should be similar to the below screen.

![](@site/static/img/frameworks_added.png)

Now, build and run the app. If you get errors like some methods are only available in some iOS versions, use @available annotation. You can get more info about this issue [in this post](https://fluffy.es/allow-app-created-in-xcode-11-to-run-on-ios-12-and-lower/).

### **How to publish a stream**

Create a UIView and add a button to your StoryBoard. This is just simple iOS app development, so we are not going to give details here. You can read lots of tutorials on the Internet.

Add mic and camera usage descriptions:

![](@site/static/img/camera_mic_usage.png)

Initialize ```webRTCClient``` in ```ViewController```

    let webRTCClient: AntMediaClient = AntMediaClient.init()

Add the following lines to ```viewDidLoad()```method.

     webRTCClient.delegate = self
     webRTCClient.setOptions(url: "ws://ovh36.antmedia.io:5080/WebRTCAppEE/websocket", streamId: "stream123", token: "", mode: .publish, enableDataChannel: false)
     webRTCClient.setLocalView(container: videoView, mode: .scaleAspectFit)
     webRTCClient.start()

Implement the delegate in your ```ViewController```. Xcode helps you with implementation.

ViewController should look like below. After you run the Application, it will start publishing with streamId: 'stream123' to your server.

    class ViewController: UIViewController {
    
      @IBOutlet var videoView: UIView!
    
      let webRTCClient: AntMediaClient = AntMediaClient.init()
    
      override func viewDidLoad() {
        super.viewDidLoad()
        
        webRTCClient.delegate = self
        //Don't forget to write your server url.
        webRTCClient.setOptions(url: "ws://your_server_url:5080/WebRTCAppEE/websocket", streamId: "stream123", token: "", mode: .publish, enableDataChannel: false)
        webRTCClient.setLocalView(container: videoView, mode: .scaleAspectFit)
        webRTCClient.start()
      }
    
    }

### How to play a stream

Playing a stream is simpler than publishing. We just need to change some codes in ```viewDidLoad()```. As a result, the following code snippets just play the stream on your server with streamId: 'stream123'. Before you play, you need to publish a stream to your server with having stream id 'stream123'.

      class ViewController: UIViewController {
       
        @IBOutlet var videoView: UIView!
       
        let webRTCClient: AntMediaClient = AntMediaClient.init()
       
        override func viewDidLoad() {
           super.viewDidLoad()
           
          
           webRTCClient.delegate = self
            //Don't forget to write your server url.
           webRTCClient.setOptions(url: "ws://your_server_url:5080/WebRTCAppEE/websocket", streamId: "stream123", token: "", mode: .play, enableDataChannel: false)
           webRTCClient.setRemoteView(remoteContainer: videoView, mode: .scaleAspectFit)
           webRTCClient.start()
       }
     }

How to use a data channel
-------------------------

Ant Media Server and iOS SDK can use data channels in WebRTC. In order to use data channel, make sure that it's enabled both on [server side](https://github.com/ant-media/Ant-Media-Server/wiki/Data-Channel) and mobile. In order to enable it for the server side, you can just set the ```enableDataChannel``` parameter to true in ```setOptions``` method.

    webRTCClient.setOptions(url: "ws://your_server_url:5080/WebRTCAppEE/websocket", streamId: "stream123", 
        token: "", mode: .play, enableDataChannel: true)

After that, you can send data with the following method of ```AntMediaClient```

    func sendData(data: Data, binary: Bool = false)

When a new message is received, the delegate method is called.

    func dataReceivedFromDataChannel(streamId: String, data: Data, binary: Bool)

There is also a data channel usage example in the reference project.