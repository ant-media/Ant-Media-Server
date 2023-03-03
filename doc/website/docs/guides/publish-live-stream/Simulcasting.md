# Simulcasting to social media channels

This guide describes how to live stream to social media and other third party RTMP end points using Ant Media Server.

<VideoPlayer youtube="true" video="https://www.youtube.com/embed/NVhYthQk_js" />

## How to Publish Live Stream on Facebook

You can publish live streams on your pages/accounts. Just click the **Live** button in the Create Post tab.

![](@site/static/img/iosmediacaptureresolutions(1).png)

After the click Live Button, you can see Facebook Live Dashboard as in the image shown below:

![](@site/static/img/facebook-live-dashboard.png)

You just need to copy the **Stream URL** and **Stream Key.**

PS: If you want to use a persistent stream key, you just need to enable **Use a Persistent Stream key** in Setup Option.

Your Facebook RTMP Endpoint URL that you will use in Ant Media Server should be like this: ```<StreamURL><StreamKey>```

For example: ```rtmps://live-api-s.facebook.com:443/rtmp/677122211923308?s_bl=1&s_psm=1&s_sc=677124129589969&s_sw=0&s_vt=api-s&a=AbxqZXR6X1VaKBzk```

![](@site/static/img/ant-media-dashboard-edit-rtmp-endpoints.png)

You just need to Add your Facebook RTMP Endpoint URL to the Ant Media Server stream RTMP Endpoint tab as the following image.

![](@site/static/img/ant-media-dashboard-add-rtmp-endpoint.png)

So, you can start broadcasting now!

## How to Publish Live Stream on Youtube

You can publish live streams on your YouTube account. Just click the **Create** button and select **Go Live.**

![](@site/static/img/image-1645118331005.png)

Just Click the **Go** button on the **Streaming Software** tab.

![](@site/static/img/youtube-studio.png)

Then copy the **Stream URL** and **Stream Key.****![](@site/static/img/youtube-studio-stream-url-stream-key.png)**

Your Youtube RTMP Endpoint URL that you will use in Ant Media Server should be like this: ```<StreamURL>`/`<StreamKey>```

For example: ```rtmp://a.rtmp.youtube.com/live2/dq1j-waph-e322-waxd-dxzd```

![](@site/static/img/ant-media-dashboard-edit-rtmp-endpoints.png)

You just need to add your Youtube RTMP Endpoint URL to the Ant Media Server stream RTMP Endpoint tab as the following image.

![](@site/static/img/ant-media-dashboard-add-rtmp-endpoint.png)

So, you can start broadcasting now!

## How to Publish Live Stream on Periscope

You can publish live streams on your periscope account. Just click the **Profile** button and select **Producer.**

![](@site/static/img/periscope-profile-producer.png)

Then copy **Stream URL** and **Stream Key.****![](@site/static/img/periscope-stream-url-stream-key.png)**  

Your Periscope RTMP Endpoint URL that you will use in Ant Media Server should be like this: `<StreamURL>`/`<StreamKey>`

For example: ,```rtmp://de.pscp.tv:80/x/baps3a3x7j32```

![](@site/static/img/ant-media-dashboard-edit-rtmp-endpoints.png)

You just need to add your Periscope RTMP Endpoint URL to the Ant Media Server stream RTMP Endpoint tab as the following image.

![](@site/static/img/ant-media-dashboard-add-rtmp-endpoint.png)

So, you can start broadcasting now!

## How to Add/Remove RTMP Endpoints?

You can Add/Remove RTMP Endpoint with 2 options.
One of them is Add/Remove RTMP Endpoint with Dashboard. It's for the general users.  Another option is Add/Remove RTMP Endpoint with REST API. 

### AMS Dashboard

This option is for general users. You just need to click the **broadcast properties** tab and click **Edit RTMP Endpoint** as the below image.

![](@site/static/img/ant-media-dashboard-edit-rtmp-endpoints.png)

### REST API

This option is for advanced users by making an API request to the rtmp-endpoint.

#### Add an RTMP endpoint

```js
var data = JSON.stringify({
"rtmpUrl":"rtmps://live-api-s.facebook.com:443/rtmp/sdasdasd=ddfsdfsdf"
});

var xhr = new XMLHttpRequest();
xhr.withCredentials = true;

xhr.addEventListener("readystatechange", function() {
  if(this.readyState === 4) {
    console.log(this.responseText);
  }
});
xhr.open("POST", "http://<server-domain>:5080/LiveApp/rest/v2/broadcasts/streamID/rtmp-endpoint");
xhr.setRequestHeader("Content-Type", "application/json");
xhr.send(data);
```

You can get more info in the [REST API](https://antmedia.io/rest/#/BroadcastRestService/addEndpointV3).

#### Remove an RTMP endpoint

```js
var data = JSON.stringify({
"rtmpUrl":"rtmps://live-api-s.facebook.com:443/rtmp/sdasdasd=ddfsdfsdf"
});

var xhr = new XMLHttpRequest();
xhr.withCredentials = true;

xhr.addEventListener("readystatechange", function() {
  if(this.readyState === 4) {
    console.log(this.responseText);
  }
});

xhr.open("DELETE", "http://`<server-domain>`:5080/LiveApp/rest/v2/broadcasts/streamID/rtmp-endpoint");
xhr.setRequestHeader("Content-Type", "application/json");
xhr.send(data);
```

You can get more info in [REST API](https://antmedia.io/rest/#/BroadcastRestService/removeEndpointV2).

Click for more detail about [REST API Guide](/v1/docs/rest-api-guide).

**PS:** Please be sure to add your IP Address to the **Use IP Filtering for RESTful API** option on Application Settings.

## Conclusion

After adding RTMP Endpoint, you need to publish a live stream. Here is our guide for [publishing live stream](/v1/docs/publishing-live-streams). Finally check the social media account to see the live stream.