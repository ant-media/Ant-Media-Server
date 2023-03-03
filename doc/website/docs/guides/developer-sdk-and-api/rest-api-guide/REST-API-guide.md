---
title: Introduction
sidebar_position: 1
---
# REST API guide

When designing the Ant Media Server, we made sure everything is accessible through REST API. By using the REST API, you can almost do anything on Ant Media Server. Here is an abstract list of available methods in REST API:

* CRUD (Create/Read/Update/Delete) operations on
  * Streams
  * Stream sources
  * IP camera
* CRUD operations on VoD streams
* Add/remove RTMP endpoints to the streams
* Authorize/revoke social endpoints
* Change settings (bitrates, recording, enable/disable object detection, VoD folder path) via root app

![](@site/static/img/what_is_rest_api-768x309.png)

For the full REST API reference, please visit the [https://antmedia.io/rest](https://antmedia.io/rest)

For the rest of this guide, we will explain how to call REST methods, give examples, and discuss security using IP filtering.

## Calling REST API methods

All REST methods are bound to the ```rest``` path of the app. For example, the Community Edition has ```LiveApp``` and ```WebRTCApp``` by default. On the other hand, Enterprise Edition has ```LiveApp``` and ```WebRTCAppEE``` by default. 

The ```LiveApp``` REST methods (for instance broadcast’s get method) are available as follows:

```js
http://SERVER_ADDRESS:5080/LiveApp/rest/v2/broadcasts/{STREAM_ID}
```

**Important Note:**

Please keep in mind that the REST interface only responds to the calls that are made from 127.0.0.1 by default. If you call from any other IP address, it does not return anything. In order to add more trusted IP addresses, please check the security section document.

We can provide a ```Broadcast``` object as a parameter in JSON format. Ant Media Server returns created broadcast object in JSON format. The most critical field in the returned response is ```streamId``` field in JSON. We use streamId in getting broadcast.

## Getting a broadcast

Getting a broadcast is easy. You just need to add the ```streamId``` as a query parameter to ```streamId``` variable as follows.

```
curl -X GET
‘http://localhost:5080/LiveApp/rest/v2/broadcasts/650320906975923279669775’
```

```get``` method returns the broadcast object as `create` method. Below is a sample JSON response that **get** method returns.

```
{
  "streamId":"650320906975923279669775",
  "status":"created",
  "type":"liveStream",
  "name":"test_video",
  "description":null,
  "publish":true,
  "date":1555431732095,
  "plannedStartDate":null,
  "duration":null,
  "endPointList":null,
  "publicStream":true,
  "is360":false,
  "listenerHookURL":null,
  "category":null,
  "ipAddr":null,
  "username":null,
  "password":null,
  "quality":null,
  "speed":0.0,
  "streamUrl":null,
  "originAdress":null,
  "mp4Enabled":0,
  "expireDurationMS":0,
  "rtmpURL":"rtmp://10.2.42.53/LiveApp/650320906975923279669775",
  "zombi":false,
  "pendingPacketSize":0,
  "hlsViewerCount":0,
  "webRTCViewerCount":0,
  "rtmpViewerCount":0
}
```

## REST API reference

The samples below show how to call REST methods. In order to have a look at all methods and their parameters, you can check the REST API reference at [https://antmedia.io/rest](https://antmedia.io/rest) built with Swagger.

![](@site/static/img/rest.png)

## REST API security with IP filtering

You may want Ant Media Server to respond to the calls that are made from specific IP ranges. By default, Ant Media Server only responds to the calls from 127.0.0.1.

In order to add IP ranges, you need to the Settings of the app in the management panel and add IP ranges in [CIDR notation](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_notation). You can add multiple comma-separated IP address ranges. Please take note that the IP filter settings of **LiveApp** and **WebRTCAppEE** are separate. This way, you can have different IP filter settings for each web application you create.

![](@site/static/img/ipfiltering.png)
