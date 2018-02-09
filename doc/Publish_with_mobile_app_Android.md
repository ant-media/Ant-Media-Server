# Publish with a Mobile App - Android

Let’s again have a look at step by step manner to make live broadcast from Android App

### Step 1 : Clone Git Repository 

```
git clone https://github.com/ant-media/LiveVideoBroadcaster.git
```

### Step 2 : Open the Project in Android Studio

Open MainActivity.java file and assing the Server URL to `RTMP_BASE_URL`. It should be in the format like
 `rtmp://<SERVER_NAME>/LiveApp/` . `LiveApp` is one of the default apps in Ant Media Server. Change it if you have something different. 
 
### Step 3: Run the App
When you run the app on an Android device, below screen should appear. Click Live Video Broadcaster button
<br/>
![Live Video broadcaster Main Activity](https://ant-media.github.io/Ant-Media-Server/doc/images/android_live_broadcaster_main_activity.png)

You should see the below screen
<br/>
![Open Video Broadcaster](https://ant-media.github.io/Ant-Media-Server/doc/images/android_mobile_live_video_broadcaster.png)


Write a live stream name like “test” to edit text and press the button. Screen should be like below
<br/>
![Android Live Broadcasting with RTMP](https://ant-media.github.io/Ant-Media-Server/doc/images/android_mobile_broadcasting.png)


Ant Media Server accepts again that broadcast and can perform adaptive streaming, recording and publishing to any other 3rd party RTMP server as usual.





