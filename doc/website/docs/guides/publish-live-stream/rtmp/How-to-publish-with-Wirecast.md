# How to publish with Wirecast

Wirecast is a live video streaming production tool by Telestream. It allows users to create live or on-demand broadcasts for the web. Wirecast supports various sources for capturing such as webcams, IP cameras, NDIs and capture cards. 

## Create a live stream in AMS

To publish with WireCast, we need to create a live stream in Ant Media Server, because we will use this live stream id for publishing the stream in Wirecast. 

In Ant Media Server, create a live stream with the name ```WireCast1``` as in the screen:

![](@site/static/img/image6.png)

Live stream will be added to the table. Note the stream id of ```WireCast1``` stream as we will use this later.

## Create a live stream in Wirecast

Now we are going to create a live stream in Wirecast and publish it to an output destination which is Ant Media Server in our case.

In Wirecast click the + button in Wirecast as in the screenshot:

![](@site/static/img/image4.png)

Chose FaceTime as video capture source which is the webcam of our computer as in the screenshot:

![](@site/static/img/image7.png)

We are going to publish stream to an RTMP URL in Ant Media Server. Click **Output Settings** in the upper menu and choose RTMP Server and click OK, as in the screenshot below.

![](@site/static/img/image8.png)

Fill the settings using the Stream Id that you noted in previous steps as in the screenshot:

![](@site/static/img/image1.png)

## Tune for ultra-low latency streaming

Wirecast by default is not optimized for ultra low latency streaming. If you push RTMP stream with Wirecast and play with WebRTC, please open Output Settings >` Edit Encoding configuration and make **Baseline** for profile. Also you can configure the bitrate according to your quality and internet bandwidth requirements.

![](@site/static/img/wirecast-encoding-settings.png) Click the right arrow to select the source of the video stream as in the screenshot:

![](@site/static/img/image11.png)

Start broadcasting the live stream by clicking the Start/Stop Broadcasting in the upper menu as in the screenshot below.

![](@site/static/img/image2.png)

Now the live stream is published to Ant Media Server. You will see the status of live stream in Ant Media Server is changed to Broadcasting status.

You can now click the Play button and watch the live stream.