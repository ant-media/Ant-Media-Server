# How to measure end to end latency

Sometimes you may want to calculate the time it takes for a stream to be transmitted from one point to another. In this guide, we'll guide you on how to calculate the duration between the publisher and the player this step by step.

This is the methodology used for this purpose:

1.  Draw the timestamp (publish time) onto the stream canvas while broadcasting the stream.
2.  Draw the timestamp (play time) onto the stream canvas while playing the stream.
3.  Extract publish and play time text using OCR (Optical Character Recognition).
4.  Calculate the E2E latency by subtracting the publish time from play time.

### Guide to measuring end to end latency

We'll use **Amazon Rekognition** or **Google's Vision API** to calculate the E2E latency. 

Below you can find the required pages source code for web SDK.

*   [publish\_with\_timestamp.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/publish_with_timestamp.html): This is the page that draws the timestamp (publish time) on the stream canvas while broadcasting the stream. This page is already available in Ant Media Server v2.3.0+ with other samples.
*   [player\_with\_timestamp.html](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/player_with_timestamp.html): This is the page that draws the timestamp (play time) on the stream canvas while playing the stream. It also calls OCR API and calculates the latency. This page is already available in Ant Media Server v2.3.0+ with other samples.

#### 1\. Sync device with a TimeServer

It is required that both the publisher and the player devices be in sync in terms of time to calculate the time difference. We use the NTP time provider for the tests. If the time servers can't be used - mostly the case in mobile devices, you can manually synchronize the devices via player\_with\_timestamp.html. This is explained below.

#### Manual sync

Find the offset in publisher and player devices. We've used [AtomicClock](https://play.google.com/store/apps/details?id=partl.atomicclock&hl=en_US&gl=US) to find the offset.

![](@site/static/img/image-1645445267761.png)

Here the local device is beyond NTP by 290 milliseconds, which is time difference between 11:09:25.060 and 11:09:25.351(system clock - bottom of the image). Assume that the device is the publisher. In this case, publisher offset is -291. If the device was behind NTP by 290 milliseconds, the offset would be 290 without a negative sign.

After you check the time difference manually from a time server, you can enter the offset of the publisher and player devices on the player page. If the device's time is beyond the NTP time, the offset value will be negative. Otherwise, it will be positive. On this page you will see that there is a publisher and player offset.

![](@site/static/img/image-1645445342702.png)

### 2\. Setup OCR: Choose AWS Rekognition or Google's Vision API

**AWS Rekognition:** To enable the AWS SDK for using Rekognition, you need to get your AWS Access Key ID and AWS secret key. [Check this link](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys) for AWS authentication.

Enter your credentials and region:

![](@site/static/img/image-1645445405563.png)

**Google Vision API:** To get the token from vision API, you should download and enable **gcloud** from the terminal. Read [Google's documentation](https://cloud.google.com/vision/docs/setup) on how to download and enable **gcloud.**

After the authentication is done, enter the following command on the terminal:

```gcloud auth application-default print-access-token```

The response will be the token that should be given to the player\_with\_timetamp.html in the Vision Token box. Enter your token.

![](@site/static/img/image-1645445480526.png)

If **gcloud** can't be run from the terminal, you can set it to the path by downloading the SDK [manually to your home directory](https://cloud.google.com/sdk/docs/install).

     Run the following commands on Ubuntu:

    $~/google-cloud-sdk/bin$ source '/home/karinca/google-cloud-sdk/path.bash.inc'
    $~/google-cloud-sdk/bin$ source '/home/karinca/google-cloud-sdk/completion.bash.inc'
    $~/google-cloud-sdk/bin$ gcloud

### 3\. Measure latency

After you give the required parameters, latency will be measured every second programmatically.

![](@site/static/img/image-1645445619577.png)

### Accuracy of the end to end measurement

There are a few things that affect the accuracy when measuring the time.

1.  **Canvas rendering:** Since we draw the current time on top of a canvas with the stream, we have a delay from canvas rendering time of javaScript. It adds 10 milliseconds of more latency to the calculation, which can be ignored.
2.  **Canvas FPS:** Canvas FPS adds 30 milliseconds of delay to measured delay.
3.  **Time Offset:** Even if the device is synced with a time server automatically, there will be tens of milliseconds of error rate for each device, which makes a total of around 20 seconds of milliseconds of error when we measure latency.
