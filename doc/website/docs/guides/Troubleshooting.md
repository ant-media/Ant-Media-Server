---
id: troubleshooting
title: Troubleshooting Guide
sidebar_position: 17
---

# Troubleshooting

### How can I fix pixelating?

Pixelating generally occurs if the bitrate value is not as much as the video resolution or video content requires. For example, if the scene has rapid motions and the bitrate is low, then you can see that pixelating occurs. Similarly, if you try to play a high resolution video with a low bitrate, again you will experience pixelating.

So if you meet such a pixelating issue on your stream, check the following.

*   First, make sure that bitrate values are high enough. You may increase the bitrate on the publisher side (AMS built-in WebRTC publish page, OBS or any other 3rd party RTMP encoders). However, don't forget that a higher bitrate value requires higher bandwidth. Hence, if your network bandwidth is not enough to handle the desired bitrate, you may consider decreasing resolution.

*   If you are using Adaptive Bitrate (ABR) in Ant Media Server, check the bitrate values of ABR settings. Normally default values are set so that values for each resolution can prevent pixelating.

### How can I fix choppy streams?

Choppy streams are caused by different reasons like internet connection quality and video encoding.

If you experience a choppy stream, check the following:

*   Make sure that your connection quality is good enough. To do that, Ant Media Server has a built-in test page. You can access it and start testing from ```https://{YOUR_DOMAIN_NAME}:5443/WebRTCAppEE/webrtc-test-tool.html```
*   If your use case is RTMP to WebRTC streaming, check if your stream has B-Frames. WebRTC doesn't accept B-Frames and if your video has them the stream will be choppy. To avoid B-Frames h264 baseline should be main.

### I see a high CPU usage

Please get a thread dump by using the following REST methods: ```GET http://AMS_URL:5080/rest/threads-info``` ```GET http://AMS_URL:5080/rest/thread-dump-json``` ```GET http://AMS_URL:5080/rest/thread-dump-raw```. You can easily call these methods via the browser address bar, since all of them are GET methods.

Check whether there exists a **dead-locked-thread** by the ```threads-info``` method.

Check **blocked time** of the threads got by ```thread-dump-json``` method.

For more details, analyze threads by loading the raw dump file using the VisualVM tool.

### I see high memory usage

Please get the memory dump by using the following REST method: ```GET http://AMS_URL:5080/rest/heap-dump```. You can easily call this method via the browser address bar since all of it is a simple GET method.

Load the created **heapdump.hprof** file using the VisualVM tool and analyse the memory. Check for any leaks. You can also use [Eclipse Memory Analyzer Tool](https://www.eclipse.org/mat/) tool to find leaks automatically.

### I get notSetRemoteDescription error

Your device may not have the necessary h264 codec. Check your device codec compatibility from: [https://mozilla.github.io/webrtc-landing/pc\_test\_no\_h264.html](https://mozilla.github.io/webrtc-landing/pc_test_no_h264.html)

### I cannot play the stream with WebRTC player

This issue ( "NoStreamExist") happens when the sending stream resolution is not enough for configured adaptive bitrate values.

On the WebRTC side, you can check your camera resolution capacity at the following links:

*   [https://webrtchacks.github.io/WebRTC-Camera-Resolution/](https://webrtchacks.github.io/WebRTC-Camera-Resolution/)
*   [https://webrtc.github.io/samples/src/content/getusermedia/resolution/](https://webrtc.github.io/samples/src/content/getusermedia/resolution/)

If you want to stream at 720HD, you need to provide the requirements at least 720p.

Make sure that video resolution in your adaptive settings is equal to or less than the stream you send.

### I can't start autoplay in Chrome or Firefox.

This is due to those browsers' policy rules:

*   [https://developers.google.com/web/updates/2017/09/autoplay-policy-changes](https://developers.google.com/web/updates/2017/09/autoplay-policy-changes)
*   [https://developer.mozilla.org/en-US/docs/Web/Media/Autoplay\_guide#The\_autoplay\_feature\_policy](https://developer.mozilla.org/en-US/docs/Web/Media/Autoplay_guide#The_autoplay_feature_policy)

### I get "Caused by: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"

The reason for this error is that your CA certificate is not available on your server. To fix this issue, you need to download the root and intermediate certificates of the SSL provider (SHA-1, SHA-2). Then, you need to import Java.

As an example, see the commands below:

```keytool -import -trustcacerts -alias AddTrustExternalCARoot -file comodorsaaddtrustca.crt -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts```

```keytool -import -trustcacerts -alias comodointermediate -file addtrustexternalcaroot.crt -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts```

```keytool -import -trustcacerts -alias comodointermediate2 -file comodorsadomainvalidationsecureserverca.crt -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts```