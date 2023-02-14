# WebRTC Playback

WebRTC playback is only available in Ant Media Server Enterprise Edition (EE). 

Before playing a stream with WebRTC, make sure that stream is broadcasting on the server.

> Quick Link: [Learn How to Publish with WebRTC](/guides/publish-live-stream/WebRTC/)

1.  Visit ```https://your_domain_name:5443/WebRTCAppEE/player.html```. If you're running Ant Media Server in your local computer, you can also visit ```http://localhost:5080/WebRTCAppEE/player.html```

2.  Write the stream id in text box( ```stream1``` by default)  

    ![](@site/static/img/3_Go_to_Play_Page.jpg)

3.  Press ```Start Play``` button. After you press the button, WebRTC stream starts playing  

    ![](@site/static/img/4_Press_Start_Play_Button.jpg)
    
      
    
    You can also use the URL format listed below to play the WebRTC stream using the Embed player (play.html):  
      
    `https://your_domain_name:5443/WebRTCAppEE/play.html?name=streamId`
      
    Congrats. You're playing with WebRTC. Please check the latency.