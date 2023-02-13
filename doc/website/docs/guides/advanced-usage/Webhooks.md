---
title: Webhooks
sidebar_position: 16
---

# Using webhooks

Ant Media Server provides webhooks for making your system/app know when certain events occurs on the server. Therefore, you can register your URL to Ant Media Server which makes POST request when a live stream is started, finished or recorded. Firstly,  let's look at how to register your backend URL to Ant Media Server as a webhook. After that, we'll provide reference for webhooks.  
![](@site/static/img/68747470733a2f2f616e746d656469612e696f2f77702d636f6e74656e742f75706c6f6164732f323031382f31312f776562686f6f6b732d333030783237332e706e67.png)

### Register Your Webhook URL

You can add default webhook URL to your streaming app on Ant Media Server. In addition, it lets you add custom specific webhook URL's in creating broadcast.

#### Add default Webhook URL

In order to add default Webhook URL, you just need to add/change Webhook URL your app settings as below:  
![](@site/static/img/ant-media-server-webhook-configuration.png)

Your Ant Media Server now has a default hook which is called when certain events happen (see below)

#### Add Custom Webhook for Streams

Ant Media Server provides creating streams through rest service. Therefore, If you want to specify the webhook URL for each stream, you can use _createBroadcast_ method in the [rest service.](https://github.com/ant-media/Ant-Media-Server/blob/master/src/main/java/io/antmedia/rest/BroadcastRestService.java)  _createBroadcast_ method has Broadcast object parameter which has _listenerHookURL_ field_._

As a result,  you can set _listenerHookURL_ for creating a stream at Ant Media Server.

Here is a sample JSON for using _createBroadcast_ method with [Postman](https://www.getpostman.com/)

    {
    	"variables": [],
    	"info": {
    		"name": "samples",
    		"_postman_id": "cbef37ab-d4ae-c349-4845-b4a91d1ab201",
    		"description": "",
    		"schema": "https://schema.getpostman.com/json/collection/v2.0.0/collection.json"
    	},
    	"item": [
    		{
    			"name": "http://localhost:5080/LiveApp/rest/broadcast/create",
    			"request": {
    				"url": "http://localhost:5080/LiveApp/rest/broadcast/create",
    				"method": "POST",
    				"header": [
    					{
    						"key": "Content-Type",
    						"value": "application/json",
    						"description": ""
    					}
    				],
    				"body": {
    					"mode": "raw",
    					"raw": "{\"name\":\"test_video\", \"listenerHookURL\":\"http://www.example.com/webhook\"}"
    				},
    				"description": "ListenerHookURL sample"
    			},
    			"response": []
    		}
    	]
    }

### Webhooks List

Ant Media Server will hook to your website using a POST request with "application/x-www-form-urlencoded" as the body. Some example responses are:

*   **liveStreamStarted:** Ant Media server calls this hook when a new live stream is started. It sends **POST (application/x-www-form-urlencoded)**request to URL with following variables
    *   **id**:  stream id of the broadcast
    *   **action**: "liveStreamStarted"
    *   **streamName**: stream name of the broadcast. It can be null.
    *   **category**:  stream category of the broadcast. It can be null.
*   **liveStreamEnded:** Ant Media Server calls this hook when a live stream is ended. It sends **POST(application/x-www-form-urlencoded)**request to URL with following variables.
    *   **id:** stream id of the broadcast
    *   **action:** "liveStreamEnded"
    *   **streamName:** stream name of the broadcast. It can be null.
    *   **category:** stream category of the broadcast. It can be null.
*   **vodReady:** Ant Media Server calls this hook when the recording of the live stream is ended. It sends **POST(application/x-www-form-urlencoded)**request to URL with following variables.
    *   **id**: stream id of the broadcast
    *   **action:** "vodReady"
    *   **vodName:**  vod file name
    *   **vodId:**  vod id in the datastore
*   **endpointFailed:** Ant Media server calls this hook when the RTMP endpoint broadcast went into the failed status. It sends **POST (application/x-www-form-urlencoded)**request to URL with following variables
    *   **id**:  stream id of the broadcast
    *   **action**: "endpointFailed"
    *   **streamName**: stream name of the broadcast. It can be null.
    *   **category**:  stream category of the broadcast. It can be null.
    *   **metadata**:  RTMP URL of the endpoint.
*   **publishTimeoutError:** Ant Media server calls this hook when there is a publish time out error, it generally means that the server is not getting any frames. It sends **POST (application/x-www-form-urlencoded)**request to URL with following variables
    *   **id**:  stream id of the broadcast
    *   **action**: "endpointFailed"
    *   **streamName**: stream name of the broadcast. It can be null.
    *   **category**:  stream category of the broadcast. It can be null.
*   **encoderNotOpenedError:** Ant Media server calls this hook when the encoder can't be opened. It sends **POST (application/x-www-form-urlencoded)**request to URL with following variables
    *   **id**:  stream id of the broadcast
    *   **action**: "encoderNotOpenedError"
    *   **streamName**: stream name of the broadcast. It can be null.
    *   **category**:  stream category of the broadcast. It can be null.

That's all. As a result, you can now determine the type of the request by using the _action_ parameter within the POST request.

We hope this post will help you to make automation in your project.  Please [keep in touch](https://antmedia.io/#contact) if you have any question. We will be happy if we can help you.

>` **Attention:** Please process the POST request within your application as quick as possible as the hooks are called within the event loop thread which will not wait for your application to complete complex tasks.