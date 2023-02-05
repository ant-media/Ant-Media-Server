# Dash playing (CMAF)

### What Is CMAF (Common Media Application Format)?

The Common Media Application Format (CMAF) is essentially a new format to reduce HTTP delivery latency. It is a new standard that aims to reduce the cost, complexity, and latency of streaming (typically 3-5 seconds). CMAF can be used in DASH or HLS. Both LL-DASH (Low Latency Dash) and LL-HLS (Low Latency HLS) are supported by Ant Media Server in the CMAF format.

![](@site/static/img/126611-CMAF-Fig1-ORG.jpg)

How to use CMAF
---------------

In Ant Media Server **v2.4.3** and earlier, you must enable it in your application's configuration file, as shown below. It is now possible to enable it directly from the Ant Media dashboard starting with version **2.5.0**.

**1.** Open the following file with your favorite editor

    /usr/local/antmedia/webapps/WebRTCAppEE/WEB-INF/red5-web.properties

**2.** Enable DASH by adding the following property to the file above.

    settings.dashMuxingEnabled=true

If you want to enable LL-HLS (an experimental feature), you also need to set the following properties:

    settings.dash.llHlsEnabled=true

**3.** Restart the Ant Media Server

    sudo service antmedia restart

**4.** Send the WebRTC stream to Ant Media Server via the WebRTC publisher page. Let’s assume your stream id is ```“stream1”```

    https://YOUR_DOMAIN:5443/WebRTCAppEE

**5.** Play the stream with CMAF using the following URL:

    https://YOUR_DOMAIN:5443/WebRTCAppEE/play.html?id=stream1&playOrder=dash

There are a few more options for CMAF and their default values. You can assume that the following values are in use if they are not specified in the properties file:

    #Duration of segments in mpd files.
    settings.dashSegDuration=6
    #Fragments are a property of fragmented MP4 files. Typically a fragment consists of moof + mdat.
    settings.dashFragmentDuration=0.5
    
    #Target latency
    settings.dashTargetLatency=3.5
    
    #DASH window size. Number of files in manifest
    settings.dashWindowSize=5
    
    #DASH extra window size. Number of segments kept outside of the manifest before removing from disk
    settings.dashExtraWindowSize=5
    
    #Enable low latency dash. This settings is effective if dash is enabled
    settings.dash.llEnabled=true
