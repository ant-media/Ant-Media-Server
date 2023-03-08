# Stream security

This guide explains stream security options in Ant Media Server. Briefly, Stream Security options are;

### **1\. Enable/Disable Accepting Undefined Streams**

This setting shortly is checking if live stream is registered in Ant Media Server.

For example: If Ant Media Server accepts undefined streams, it accepts any incoming streams. If accepting undefined Streams is disabled, only streams with their stream id in the database are being accepted by Ant Media Server.

You can find in more detail in [here](/v1/docs/ams-application-configuration)

After modifying the configuration, please add the streamId, stream name in "broadcast" collections of your App.

![](@site/static/img/image-1645191373332.png)

### 2\. One Time Token Control

One Time Token Control feature usage is in Dashboard / Application(LiveApp or etc.) / Publish/Play with One-time Tokens section.

![](@site/static/img/image-1645191401746.png)

By enabling this option, one-time tokens are required for publishing and playing. Publish/Play requests without tokens will not be streamed.

If One-Time Token control is enabled, then all publish and play requests should be sent with a token parameter.

After version 2.2 one time token security option in the Dashboard will be divided into two parts. There will be seperate options for enabling/disabling one time tokens for publishing and for playing. This will allow for example using one time tokens for only players and hash-based tokens (or no security) for publishers or vice-versa.

**Create a Token in Publish&Play Scenario**

The Server creates tokens with [getTokenV2](https://github.com/ant-media/Ant-Media-Server/blob/master/src/main/java/io/antmedia/rest/BroadcastRestService.java) Rest Service getting ```streamId, expireDate``` and ```type``` parameters with query parameters. Service returns tokenId and other parameters. It is important that ```streamId``` and type parameters should be defined properly. Because ```tokenId``` needs to match with both ```streamId``` and type.

The sample token creation service URL in Publish Scenario:

    http://[IP_Address]:5080/`<Application_Name>`/rest/v2/broadcasts/`<Stream_Id>`/token?expireDate=`<Expire_Date>`&type=publish

The sample token creation service URL in Play Scenario:

    http://[IP_Address]:5080/`<Application_Name>`/rest/v2/broadcasts/`<Stream_Id>`/token?expireDate=`<Expire_Date>`&type=play

Expire Date format is Unix Timestamp. Check also ->` [https://www.epochconverter.com/](https://www.epochconverter.com/)

**RTMP & SRT URL usage:**

    rtmp://IP_Address/Application_Name/StreamId?token=tokenId
    
    srt://IP_Address:4200?Application_Name/streamid,token=tokenId

Here is OBS settings for the One Time Token.

![](@site/static/img/ant-media-server-one-time-token.png)

**Live Stream / VoD URL usage:**

    http://[IP_Address]/`<Application_Name>`/streams/streamID.mp4?token=tokenId
    http://[IP_Address]/`<Application_Name>`/streams/streamID.m3u8?token=tokenId
    http://[IP_Address]/`<Application_Name>`/play.html?name=streamID&playOrder=hls&token=tokenId

**WebRTC usage:**

**\-Playing usage:** Again the token parameter should be inserted to play WebSocket message. Also please have a look at the principles described in the [WebRTC playing wiki page](/v1/docs/webrtc-websocket-messaging-reference#playing-webrtc-stream).

TODO: Please tell or give link how to get token from Ant Media Server -->`

    Secure WebSocket: wss://SERVER_NAME:5443/WebRTCAppEE/websocket
    WebSocket without Secure: ws://SERVER_NAME:5080/WebRTCAppEE/websocket
    
    {
    command : "play",
    streamId : "stream1",
    token : "tokenId",
    }

**\-Publishing usage:** Again the token parameter should be inserted to WebSocket message. Also please have a look at the principles described in the [WebRTC publishing wiki page](/v1/docs/webrtc-websocket-messaging-reference#publishing-webrtc-stream).

    Secure WebSocket: wss://SERVER_NAME:5443/WebRTCAppEE/websocket
    WebSocket without Secure: ws://SERVER_NAME:5080/WebRTCAppEE/websocket
    
    {
    command : "publish",
    streamId : "stream1",
    token : "tokenId",
    }

### 3\. CORS Filter

CORS(Cross-Origin Resource Sharing) Filter is enabled and accepts requests from everywhere by default.

If you want to customize by yourself CORS Filters in Application, you can access in ```SERVER_FOLDER``` / ```webapps``` / ```{Application}``` / ```WEB-INF``` / web.xml

    `<filter>`
          `<filter-name>`CorsFilter`</filter-name>`
          `<filter-class>`io.antmedia.filter.CorsHeaderFilter`</filter-class>`
          `<init-param>`
             `<param-name>`cors.allowed.origins`</param-name>`
             `<param-value>`*`</param-value>`
           `</init-param>`
           `<init-param>`
                `<param-name>`cors.allowed.methods`</param-name>`
                `<param-value>`GET,POST,HEAD,OPTIONS,PUT,DELETE`</param-value>`
           `</init-param>`
    
            `<!-- cors.allowed.origins ->` * and credentials are not supported at the same time.
            If you set to cors.allowed.origins to specific domains and support credentials open the below lines
            `<init-param>`
                `<param-name>`cors.support.credentials`</param-name>`
                `<param-value>`true`</param-value>`
            `</init-param>`
             -->`
            `<init-param>`
                `<param-name>`cors.allowed.headers`</param-name>`
                `<param-value>`Accept, Origin, X-Requested-With, Access-Control-Request-Headers, Content-Type, Access-Control-Request-Method, Authorization`</param-value>`
             `</init-param>`
             `<async-supported>`true`</async-supported>`
    `</filter>`
    `<filter-mapping>`
         `<filter-name>`CorsFilter`</filter-name>`
         `<url-pattern>`/*`</url-pattern>`
    `</filter-mapping>`

If you want to customize by yourself CORS Filters in Root, you can access in ```SERVER_FOLDER``` / ```webapps``` / ```root``` / ```WEB-INF``` / web.xml

    `<filter>`
    	`<filter-name>`CorsFilter`</filter-name>`
    	`<filter-class>`io.antmedia.filter.CorsHeaderFilter`</filter-class>`
    	`<init-param>`
    	  `<param-name>`cors.allowed.origins`</param-name>`
    	  `<param-value>`*`</param-value>`
    	`</init-param>`
    	`<init-param>`
    	  `<param-name>`cors.allowed.methods`</param-name>`
    	  `<param-value>`GET,POST,HEAD,OPTIONS,PUT,DELETE`</param-value>`
    	`</init-param>`
    `</filter>`
    `<filter-mapping>`
    	`<filter-name>`CorsFilter`</filter-name>`
    	`<url-pattern>`/*`</url-pattern>`
    `</filter-mapping>`

>` Quick Learn: [Tomcat CORS Filter](https://tomcat.apache.org/tomcat-8.0-doc/api/index.html?org/apache/catalina/filters/CorsFilter.html)

### 4\. Hash-Based Token

Firstly, settings should be enabled from the settings file of the application in ```SERVER_FOLDER``` / ```webapp``` / ```{Application}``` / ```WEB-INF``` / ```red5-web.properties```

    settings.hashControlPublishEnabled=true
    settings.hashControlPlayEnabled=true
    tokenHashSecret=PLEASE_WRITE_YOUR_SECRET_KEY

Set true ```settings.hashControlPublishEnabled``` to enable secret based hash control for publishing operations, and ```settings.hashControlPlayEnabled``` for playing operations.

>` Also, do not forget to define a secret key for generating a hash value.

#### Publishing Scenario

**Step 1. Generate a Hash**

You need to generate a hash value using the formula ```sha256(STREAM_ID+ROLE+SECRET)``` for your application and send to your clients. The values used for hash generation are:

    STREAM_ID: The id of stream, generated in Ant Media Server.
    ROLE: It is either "play or "publish"
    SECRET: Shared secret key (should be defined in the setting file)

**Step 2. Request with Hash**

The system controls hash validity during publishing or playing. **Keep in mind that there is NO '+' in calculating the hash in this formula** ```**sha256(STREAM_ID+ROLE+SECRET)**``` Here is an example for that.

Let's say ```STREAM_ID: stream1```, ```ROLE: publish```, ```SECRET: this_is_secret``` Your hash is the result of this calculation: ```sha256(stream1publishthis_is_secret)```

Go to [JavaScript SHA-256](https://geraintluff.github.io/sha256/) for online demo

**RTMP Publishing:** You need to add a hash parameter to RTMP URL before publishing. Sample URL:

    rtmp://IP_Address/Application_Name/StreamId?token=hash
    
    srt://IP_Address:4200?Application_Name/streamid,token=hash

Here is OBS settings for the Hash-Based Token

![](@site/static/img/ant-media-server-one-time-token(1).png)

**WebRTC Publishing:** Hash parameter should be inserted to publish WebSocket messages.

    {
    command : "publish",
    streamId : "stream1",
    token : "hash",
    }

#### Playing Scenario

**Step 1. Generate a Hash**

You need to generate a hash value using the formula sha256(STREAM\_ID + ROLE + SECRET) for your application and send to your clients. The values used for hash generation are:

    STREAM_ID: The id of stream, generated in Ant Media Server.
    ROLE: It is either "play or "publish"
    SECRET: Shared secret key (should be defined in the setting file)

**Step 2. Request with Hash**

**Live Stream/VoD Playing:** Same as publishing, the hash parameter is added to the URL. Sample URL:

    http://[IP_Address]/`<Application_Name>`/streams/`<Stream_Id_or_Source_Name>`?token=hash

**WebRTC Playing:** Again the hash parameter should be inserted to play WebSocket message.

    {
    command : "play",
    streamId : "stream1",
    token : "hash",
    }

>` Please have a look at the principles described in the [WebRTC WebSocket wiki page](/v1/docs/webrtc-websocket-messaging-reference).

#### Evaluation of the Hash

If related settings are enabled, Ant Media Server first generates hash values based on the formula sha256(STREAM\_ID + ROLE + SECRET) using streamId, role parameters and secret string which is defined in the settings file.

Then compare this generated hash value with the client's hash value during authentication.

Once the hash is successfully validated by Ant Media Server, the client is granted either to publish or play according to application setting and user request.

### 5\. Publisher IP Filter

>` Publisher IP Filter feature is available for later versions of the 1.9.0+ version.

Publisher IP filter feature allows you to specify the IP addresses allowed for publishing. You can define multiple allowed IPs in CIDR format as comma (,) separated.

To enable publisher IP filtering you must set ```settings.allowedPublisherCIDR``` in ```AMS_DIR/webapps/`<App_Name>`/WEB_INF/red5.properties``` file with the allowed IP addresses.

Example: ```settings.allowedPublisherCIDR=10.20.30.40/24,127.0.0.1/32``` allows IPs 10.20.30.\[0-255\] and 127.0.0.1.

You can [read more](https://whatismyipaddress.com/cidr/) about CIDR notation.

### 6\. JWT Stream Security Filter

JWT Stream Security feature is enabled/disabled in ```Dashboard/LiveApp( or any other)/ Settings/Publish/Play with JWT Filter```. Just take a look at the image for the related part. You can use JWT Stream Security Filter for Stream Publishing and Playing. Publish/Play requests without JWT tokens will not be streamed if you enable the JWT Stream Security Filter as shown below by also adding Secret Key on web panel.

![](@site/static/img/ant-media-server-jwt-stream-security-filter-dashboard.png)

After version 2.3.3 JWT Stream Security filter option in the Dashboard will be divided into two parts. There will be separate options for enabling/disabling JWT Stream Security for publishing and for playing. This will allow for example using JWT Stream Security for only players and other security (or no security) for publishers or vice-versa.

### Generate JWT Token

Let's assume that our secret key is ```zautXStXM9iW3aD3FuyPH0TdK4GHPmHq``` so that we just need to create a JWT token. Luckily, there are plenty of [libraries available for JWT](https://jwt.io/#libraries-io) for your development. For our case, we will just use [Debugger at JWT](https://jwt.io/#debugger-io).

![](@site/static/img/generate-jwt-stream-token.png)

As shown above, we use HS256 as algorithm and use our secret key ```zautXStXM9iW3aD3FuyPH0TdK4GHPmHq``` to generate the token. The payload is not critical for this authorization. You can use any payload to create the token. On the server side, it just checks the token is signed with secret key. So that our JWT token to publish/play the stream is:

![](@site/static/img/generate-jwt-stream-token-with-expiration.png)  

As shown above, the expiration time of the token is Mar 08, 2021 02:14:08 GMT+3. It means that you can use the generated token until the expiration time. The unit of expiration time is [unix timestamp](https://www.unixtimestamp.com/). When it expires, the JWT token becomes invalid.

#### Generate JWT Token with REST API

You can also generate Publish/Play JWT Token with REST API. The Server creates JWT tokens with [getJwtTokenV2](https://antmedia.io/rest/#/BroadcastRestService/getJwtTokenV2) Rest Service getting ```streamId```, ```expireDate``` and ```type``` parameters with query parameters. Service returns ```tokenId``` and other parameters. It is important that ```streamId``` and ```type``` parameters should be defined properly. Because ```tokenId``` needs to match with both ```streamId``` and ```type```.

**The sample JWT token creation service URL in Publish Scenario:**

    http://[IP_Address]:5080/`<Application_Name>`/rest/v2/broadcasts/`<Stream_Id>`/jwt-token?expireDate=`<Expire_Date>`&type=publish

**The sample JWT token creation service URL in Play Scenario:**

    http://[IP_Address]:5080/`<Application_Name>`/rest/v2/broadcasts/`<Stream_Id>`/jwt-token?expireDate=`<Expire_Date>`&type=play

Expire Date format is Unix Timestamp. Check also ->` [https://www.epochconverter.com/](https://www.epochconverter.com/)

### How to use JWT Token

#### RTMP URL usage

    rtmp://IP_Address/Application_Name/StreamId?token=tokenId
    
    srt://IP_Address:4200?Application_Name/streamid,token=tokenId

 Here is OBS setting for the JWT Token:

![](@site/static/img/ant-media-server-one-time-token.png)

#### HLS/ VoD & Embedded Player Usage

    http://[IP_Address]/`<Application_Name>`/streams/streamID.mp4?token=tokenId
    http://[IP_Address]/`<Application_Name>`/streams/streamID.m3u8?token=tokenId
    http://[IP_Address]/`<Application_Name>`/play.html?name=streamID&playOrder=hls&token=tokenId

#### WebRTC Publish/Play Usage

*   Play: Again the JWT token parameter should be inserted to play WebSocket message. Also please have a look at the principles described in the [WebRTC playing wiki page](/v1/docs/webrtc-websocket-messaging-reference#playing-webrtc-stream).

    {
    command : "play",
    streamId : "stream1",
    token : "tokenId",
    }

*   Publish: Again the JWT token parameter should be inserted to publish WebSocket message. Also please have a look at the principles described in the [WebRTC publishing wiki page](/v1/docs/webrtc-websocket-messaging-reference#publishing-webrtc-stream).

    {
    command : "publish",
    streamId : "stream1",
    token : "tokenId",
    }

**This feature is available in Ant Media Server 2.3.3+ versions.**

### 7\. Time based One Time Password

The Time-based One-time Password algorithm (TOTP) is an extension of the HMAC-based One-time Password algorithm (HOTP) that generates a one-time password (OTP) by instead taking uniqueness from the current time.

We define a publisher or player as a subscriber. If time based token enabled, a subscriber should be created for the stream to able to publish or play. Each subscriber has an ID and a code. When a subscriber requests to publish or play a stream, he should provide his ID and time based token generated for his code. Otherwise server doesn't accept the publish or play request.

Enabling and Setting
--------------------

You can enable TOTP using Management Panel or in configuration file as ```settings.timeTokenSubscriberOnly=true``` You can also set TOTP period in seconds in configuration file as ```settings.timeTokenPeriod=60```

![](@site/static/img/image-1671624507845.png)
-----------------------------------------------------------------------------------------------------------------

Subscriber Operations
---------------------

After enabling TOP in the server the following operations should be performed to publish or play by using TOTP.

*   Admin creates a new subscriber (publisher or player) by using this rest method. You should assign a base 32 secret to each subscriber at the creation. A secret should be in length of multiple of 8 characters .

_Curl example for publisher type subscriber creation._

    curl -X POST -H "Accept: Application/json" -H "Content-Type: application/json" http://localhost:5080/WebRTCAppEE/rest/v2/broadcasts/stream1/subscribers -d '{"subscriberId":"publisherA", "b32Secret":"mysecret", "type":"publish"}'
    

_Curl example for player type subscriber creation._

    curl -X POST -H "Accept: Application/json" -H "Content-Type: application/json" http://localhost:5080/WebRTCAppEE/rest/v2/broadcasts/stream1/subscribers -d '{"subscriberId":"playerB", "b32Secret":"mysecret", "type":"play"}'
    

*   Subscriber (Publisher or Player) needs to have a TOTP token to publish or play the stream. This token should be created using subscriber secret key. [Here](https://totp.danhersam.com/) is an example page that creates TOTP.
*   Subscriber (Publisher or Player) can request publish or play using the created TOTP.

_Example of a publish request:_

    http://localhost:5080/WebRTCAppEE/?subscriberId=publisherA&subscriberCode=440456
    

_Example of a play request:_

    http://localhost:5080/WebRTCAppEE/play.html?subscriberId=playerB&subscriberCode=438610

![](https://github.com/ant-media/Ant-Media-Server/wiki/images/totp_messages.png)

You can find create, delete, list REST Methods references from [REST API Reference](https://antmedia.io/rest)

Subscriber Statistics
---------------------

You can also get the some statistics like connection events, average bitrate for each subscriber with the following REST method.

    curl -i -H "Accept: Application/json" -X GET "http://localhost:5080/WebRTCAppEE/rest/v2/broadcasts/stream1/subscriber-stats/list/0/5"