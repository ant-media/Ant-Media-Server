# REST API examples

Here are some handy examples that you can consume on REST API. Please replace curly braces with your own configuration. Typical values for ```{domain}``` is either your server's accessible IP address or fully qualified domain name, ```port``` is 5080 for HTTP and 5443 for HTTPS. ```{application}``` can be either ```LiveApp``` or any other one.

**Note:**

This guide assumes that you included your IP in the IP-Filter as [mentioned here](/v1/docs/rest-api-guide#security-%E2%80%93-ip-filtering) so that you can do REST API calls.

Create
------

Following command creates a stream on Ant Media Server

    curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/create"

For quick testing when we run the following command in terminal ```curl -X POST -H "Content-Type: application/json" "http://localhost:5080/LiveApp/rest/v2/broadcasts/create"``` It returns a Broadcast Object in the response like this

    {"streamId":"247807894779015096249123","status":"created","type":"liveStream","name":null,"description":null,"publish":true,"date":1605776243606,"plannedStartDate":0,"plannedEndDate":0,"duration":0,"endPointList":null,"publicStream":true,"is360":false,"listenerHookURL":null,"category":null,"ipAddr":null,"username":null,"password":null,"quality":null,"speed":0.0,"streamUrl":null,"originAdress":"127.0.0.1","mp4Enabled":0,"webMEnabled":0,"expireDurationMS":0,"rtmpURL":"rtmp://127.0.0.1/LiveApp/247807894779015096249123","zombi":false,"pendingPacketSize":0,"hlsViewerCount":0,"webRTCViewerCount":0,"rtmpViewerCount":0,"startTime":0,"receivedBytes":0,"bitrate":0,"userAgent":"N/A","latitude":null,"longitude":null,"altitude":null,"mainTrackStreamId":null,"subTrackStreamIds":null,"absoluteStartTimeMs":0,"webRTCViewerLimit":-1,"hlsViewerLimit":-1}

You can take a look at the [Broadcast object in the REST Reference](https://antmedia.io/rest/)

### Create Broadcast with pre-defined ```streamId```

You can determine your ```streamId``` in advance and make request with that ```streamId``` as follows

    curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/create" -d '{"streamId":"{YOUR_STREAM_ID}"}'

Running sample command in my localhost ```curl -X POST -H "Content-Type: application/json" "http://localhost:5080/LiveApp/rest/v2/broadcasts/create" -d '{"streamId":"1234567", "name":"Test Stream"}'```. It returns following response.

    {"streamId":"1234567","status":"created","type":"liveStream","name":"Test Stream","description":null,"publish":true,"date":1605776884508,"plannedStartDate":0,"plannedEndDate":0,"duration":0,"endPointList":null,"publicStream":true,"is360":false,"listenerHookURL":null,"category":null,"ipAddr":null,"username":null,"password":null,"quality":null,"speed":0.0,"streamUrl":null,"originAdress":"127.0.0.1","mp4Enabled":0,"webMEnabled":0,"expireDurationMS":0,"rtmpURL":"rtmp://127.0.0.1/LiveApp/1234567","zombi":false,"pendingPacketSize":0,"hlsViewerCount":0,"webRTCViewerCount":0,"rtmpViewerCount":0,"startTime":0,"receivedBytes":0,"bitrate":0,"userAgent":"N/A","latitude":null,"longitude":null,"altitude":null,"mainTrackStreamId":null,"subTrackStreamIds":null,"absoluteStartTimeMs":0,"webRTCViewerLimit":-1,"hlsViewerLimit":-1}

### Create Stream Source Broadcasts

Stream Sources are external and you can make Ant Media Server pull RTSP, IP Camera or any other Stream Sources. Here is the sample command for that.

    curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/create?autoStart=false" -d '{ "type":"streamSource", "streamUrl":"YOUR_STREAM_SOURCE_URL"}'

If you want it to start pulling stream immediately, you can set ```autoStart``` value to ```true``` in the query above. You can also start/stop stream sources with following commands.

### Starting Stream Source

    curl -X POST -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/{streamId}/start"

Read
----

You can query Broadcasts with GET methods. Check this out

    curl -X GET "https://{domain:port}/{application}/rest/v2/broadcasts/{streamid}"

It returns the Broadcast object or 404 if there is no streamId specified in the URL.

### Read Broadcast Statistics

Following methods return Broadcast Statistics for the specified stream Id

    curl -X GET "https://{domain:port}/{application}/rest/v2/broadcasts/{streamid}/broadcast-statistics"

Broadcast Statistics contains the number of viewers for the broadcast.

Update
------

Following method change the name of the Broadcast.

    curl -X PUT -H "Content-Type: application/json" "https://{domain:port}/{application}/rest/v2/broadcasts/{streamid}" -d '{"name":"
    {streamname}"}'

The method above returns Result object that shows if operation is successful or not

Delete
------

Delete requests are straight-forward which means these methods are aim to delete from database 

    curl -X DELETE https://{domain:port}/{application}/rest/v2/broadcasts/{streamId}

The command above deletes the broadcast with the specified streamId

### REST API Reference

In this doc, we just provide some sample commands for consuming REST methods. For the all methods in the REST API, please visit [https://antmedia.io/rest/](https://antmedia.io/rest/)

Note:

On Windows Command Prompt, body part of the requests should be like the following : ```-d "{""name"":""{streamname}""}"```.