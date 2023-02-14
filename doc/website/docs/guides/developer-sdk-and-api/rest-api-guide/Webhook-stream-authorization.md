# Webhook stream authorization

There are plenty of [Security options for Publishing and Playing Streams](https://resources.antmedia.io/docs/stream-security) available in Ant Media Server, however, if these are not suitable for your use case and you want to control which stream to be published directly from your own end, then you can use your own webhook structure for stream authentication.

If you enable this feature, whenever a stream is initiated to publish, the server will send an HTTP request to your given webhook address. This request has information regarding the stream like stream name, app name, streamId etc.

Based on this request, you can parse and process that information on your end and send a response. If the response code is 200, the server will authorize the stream and allow it to begin publishing. If the response code is different from 200, the server will refuse the stream to be published.

To enable this feature, first, you need to add the setting below in `<AMS\_DIR>`/webapps/`<AppName>`/WEB-INF/red5-web.properties.

    settings.webhookAuthenticateURL=your-webhook-URL

After inserting this setting, restart the server.

You can use this [webhook site](https://webhook.site/) to test this feature and get your own webhook URL. However, when you send a request to that site correctly, the response code will always be 200 by default. Let's test this with an example to publish a stream.

Sample webhook URL from webhook site is added in settings file as follows.

    settings.webhookAuthenticateURL=https://webhook.site/23fd2ec9-8ecc-46fb-8144-65009c3aacbc

After this, when an RTMP/WebRTC stream initiates to publish, it will trigger the webhook URL and sends the request like below.

![Webhook-request1](@site/static/img/Webhook-request1.png)

If the response is 200 then it allows the stream to be published with logs as follows.

    INFO  i.a.s.AcceptOnlyStreamsWithWebhook - Response from webhook is: 200 for stream:stream1
    INFO  i.a.e.w.WebSocketEnterpriseHandler - Is publish allowed through Webhook Authentication: true

Now you can change the response code on the webhook site by clicking the **Edit** option. As an example, change it to 300.  
Please try to publish the stream again but this time as the response will be 300, hence it will not authorize to publish the stream on the server with the logs below.

    INFO  i.a.s.AcceptOnlyStreamsWithWebhook - Response from webhook is: 300 for stream:stream1
    WARN  i.a.s.AcceptOnlyStreamsWithWebhook - Connection object is null for stream1
    INFO  i.a.e.w.WebSocketEnterpriseHandler - Is publish allowed through Webhook Authentication: false

For more details about using webhook in Ant Media Server, please check [webhook documentation](https://resources.antmedia.io/docs/using-webhooks).