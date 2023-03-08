# Using the data channel

Data channel is another channel in WebRTC other than video and audio. In the data channel, you can send any kind of information to the other clients. Data channels can be utilized in various use cases including chat, control messages or file sharing. Ant Media Server provides a generic data channel infrastructure that can be used in all use cases.

## Enabling the data channel

In order to use data channel functionality, you first should enable data channel in the dashboard, in order to send/receive anything through data channel with SDKs.

![](@site/static/img/image-1645113575789.png)

There are some data delivery options for data channels you can choose:

*   **Nobody**: Only the publisher can send messages to the players and players cannot send messages.
*   **Only publisher**: Player messages are only delivered to the publisher. Publisher messages are delivered to all players.
*   **Publisher and all players:** Players' and publisher's messages are delivered to the publisher and all other players who are watching the stream and publisher.

## Sending and receiving messages with JavaScript

Sending and receiving messages via data channels can be implemented using Ant Media Server Javascript SDK with less than 10 lines of code.

### Sending a message

```sendData = function(streamId, message)``` function in ```webrtc_adaptor.js``` is used to send messages as shown in the following code: 

    webRTCAdaptor.sendData("stream1", "Hi!");

Here is a screenshot of the sample page that you can try in WebRTC publishing:

![](@site/static/img/image-1645113728234.png)

### Receiving a message

In order to receive a message, just add the following callback for WebRTCAdaptor.

```js
  callback : function(info, description) {
  
    if (info == "data_channel_opened") {
  
        console.log("data channel is open");
  
    }
    else if (info == "data_received") {
  
        console.log("Message received ", description.data );
  
        handleData(description);
    }
  
    else if (info == "data_channel_error") {
  
        handleError(description);
  
    } else if (info == "data_channel_closed") {
  
        console.log("Data channel closed " );
    }
```
![](@site/static/img/image-1645113777763.png)

  

Basic sample for sending and receiving messages through data channel is available in WebRTC Publishing and Playing pages as shown above.

## Sending & receiving data with Android SDK

Exchanging data through WebRTC data channels is also straightforward with AMS Android WebRTC SDK. Your activity should implement ```IDataChannelObserver``` interface as shown below:

```java
  public interface IDataChannelObserver {
  
      /**
        * Called by SDK when the buffered amount has been changed
        */
      void onBufferedAmountChange(long previousAmount, String dataChannelLabel);
  
      /**
        * Called when the state of the data channel has been changed
        */
      void onStateChange(DataChannel.State state, String dataChannelLabel);
  
      /**
        * Called by SDK when a new message is received
        */ 
      void onMessage(DataChannel.Buffer buffer, String dataChannelLabel);
  
      /**
        * Called by SDK when the message is sent successfully
        */ 
      void onMessageSent(DataChannel.Buffer buffer, boolean successful);
  }
```

### Initialization

```MainActivity.java``` sample code in Android SDK implements the ```IDataChannelObserver``` and has the required initialization code. The base data channel functionality is in the ```WebRTCClient.java```, ```MainActivity``` file. The sample below shows how to init, send and receive data messages in Android SDK. In order to init the WebRTCClient Before initialization of WebRTCClient you also need to add the following lines in ```onCreate``` method of the Activity.

```java
//Enable data channel communication by putting following key-value pair to your Intent before initialization of WebRTCClient
this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);

//Set your Data Channel observer in the WebRTCClient 
webRTCClient.setDataChannelObserver(this);

//Init the WebRTCClient
webRTCClient.init(SERVER_URL, streamId, webRTCMode, tokenId, this.getIntent());
```

### Sending data

WebRTClient has ```sendMessageViaDataChannel(DataChannel.Buffer)``` method to send messages. It has also been called in MainActivity as follows.

```java
public void sendTextMessage(String messageToSend) 
{
  final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
  DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
  webRTCClient.sendMessageViaDataChannel(buf);
} 
```

### Receiving data

When a data channel message is received, ```onMessage``` method of the ```IDataChannelObserver``` is called. You can handle the received data in ```onMessage``` method as shown below.

```java
public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) 
{
  ByteBuffer data = buffer.data;
  String messageText = new String(data.array(), StandardCharsets.UTF_8);
  Toast.makeText(this, "New Message: " + messageText, Toast.LENGTH_LONG).show();
}
```

In this example, we show the incoming text in a toast message.

## Sending & receiving data with iOS SDK

### Initialization

Ant Media Server and WebRTC iOS SDK can use data channels in WebRTC. In order to use the data channel, make sure that it’s enabled both server-side and mobile. In order to enable it for the server-side, you can just set the ```enableDataChannel``` parameter to true in ```setOptions``` method.

```swift
webRTCClient.setOptions(url: "ws://your_server_url:5080/WebRTCAppEE/websocket", streamId: "stream123", token: "", mode: .play, enableDataChannel: true)
```

WebRTC iOS SDK also provides sample code for sending and receiving messages via a data channel.

### Sending data

You can send data with the ```sendData``` method of ```AntMediaClient``` as follows.

```swift
if let data = textValue.data(using: .utf8) {
    /*
      Send data through data channel
      */
    self.client.sendData(data: data, binary: false)       
}
```

### Receiving data

When a new message is received, the delegate’s ```dataReceivedFromDataChannel``` method is called:

```swift
func dataReceivedFromDataChannel(streamId: String, data: Data, binary: Bool) {      
    Run.onMainThread {
      self.showToast(controller: self, message:  String(decoding: data, as: UTF8.self), seconds: 1.0)
    }       
}
```

Take a look at the ```VideoViewController.swift``` in order to see how to use data channel.

## Sending data with REST method

You can also programmatically send a data channel message with REST API. Here is an example cURL usage:

```http
curl -X POST
http://localhost:5080/WebRTCAppEE/rest/v2/broadcasts/{STREAM_ID}/data
-H 'content-type: application/json'
-d '{message: "test"}'
```

You can send any text with this method. The sample command above just sends ```{message: "test"}``` to the publisher or players of the ```{STREAM_ID}```

## Receiving channel messages with webhook

You can programmatically collect all data channel messages for any stream with a web hook. All data channel messages are delivered to these hooks as well. Here is the step by step guide to add web hook for data channel messages.

*   Open your app's ```red5-web.properties``` which is under ```webapps/`<app_name>`/WEB-INF```
*   Add ```settings.dataChannelWebHook``` property and assign your webhook URL. Start with ```http``` or ```https```
*   Save the file and restart the server.

```
sudo service antmedia restart
```

After restarting, your webhook URL is called with data channel messages by Ant Media Server. POST method is used for sending data channel messages with "**multipart/form-data**" encoding. The name of the variable is the data that contains the data channel message.