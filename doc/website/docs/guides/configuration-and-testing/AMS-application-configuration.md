# AMS application configuration

The Ant Media Server configurations can be made directly from the files as well as through the management console. The documentation of all the application settings are provided below.

[App Settings Javadoc](https://antmedia.io/javadoc/io/antmedia/AppSettings.html)

### How to use App Settings Javadoc

App Settings are set for each applications and stored in the file ```<AMS_DIR>`/webapps/`<AppName>`/WEB-INF/red5-web.properties```. Management panel allows some changes however, the file is much more extensive, see the [App Settings Javadoc](https://antmedia.io/javadoc/io/antmedia/AppSettings.html) to reach all the settings.

#### Use case example:

*   Open the link [App Settings Javadoc](https://antmedia.io/javadoc/io/antmedia/AppSettings.html)
*   Find the setting that you need on the Field Summary Section. Check the description if needed. Most of the settings are self explanatory. i.e.
    *   Click on the name of the field ```aacEncodingEnabled```
    *   You'll see the following description
        
            @Value("${settings.aacEncodingEnabled:true}") 
            private boolean aacEncodingEnabled 
        
    *   It is a boolean type and its default value is true as shown in the @Value section. The comment for the field as follows
        
             If `aacEncodingEnabled` is true, aac encoding will be active even if mp4 or hls muxing is not enabled, If aacEncodingEnabled is false,
             aac encoding is only activated if mp4 or hls muxing is enabled in the settings, This value should be true if you're sending stream to
             RTMP endpoints or enable/disable mp4 recording on the fly
        
    *   To change it, open the file `<AMS\_DIR>`/webapps/`<AppName>`/WEB-INF/red5-web.properties
    *   If the setting is not already in `<AMS\_DIR>`/webapps/`<AppName>`/WEB-INF/red5-web.properties you can add a new line to change it.
        
            settings.aacEncodingEnabled=false