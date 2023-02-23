# Embedded Web Player

You can use ```play.html``` page on your website with IFrame. ```play.html``` page is in the Application folder on Ant Media Server.

```play.html``` page accepts the below arguments

*   **```id```** : The stream id to play. It is ***mandatory***.
*   **```token```** : The token to play the stream. It's mandatory if token security is enabled on the server-side.
*   **```autoplay```** : To start playing automatically if streams are available. Optional. The default value is true.
*   **```mute```** : To start playing with mute if stream is available. Optional. Default value is true.
*   **```playOrder```** : The order which technologies is used in playing. Optional. Default value is ```webrtc,hls```. Possible values are ```hls,webrtc,webrtc,hls,vod,dash```
*   **```playType```** : The order which play type is used in playing. Optional. Default value is ```mp4,webm```. Possible values are ```webm,mp4,mp4,webm```.
*   **```targetLatency```** : To define target latency for the DASH player. Optional. Default value is ```3```.
*   **```is360```** : To play the stream in 360. Default value is false.

**You can use** **play.html** **in IFrame as below:**

```html
<iframe width="560" height="315" src="https://your_domain_name:5443/LiveApp/play.html?name=125214322064017559554903" frameborder="0" allowfullscreen>``</iframe>
```

#### ![](@site/static/img/ant-media-server-iframe-player.png)

#### Change width/height resolution in ```play.html``` page

If you want to change width/height parameters, you just need to change your IFrame code snippets. If you don't enter any parameter, play.html covers all screen.