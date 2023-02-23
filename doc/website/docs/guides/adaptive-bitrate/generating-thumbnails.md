---
title: Generating Thumbnails
---

# Generating thumbnails from streams

Ant Media Server can generate periodic previews (snapshots) of the incoming streams on the fly. This guide will help you learn configuration parameters for generating and using previews.

In order to activate preview generation, you just need to add at least one adaptive bitrate. You can do that in the dashboard using ```Application >` Your App >` Settings >` Add New Bitrate```

![](@site/static/img/preview_1.png)

Generated preview images will be available in this URL template:

```
http://<SERVER_NAME>:5080/<APP_NAME>/previews/<STREAM_ID>.png
```

**With v2.4.3** and later, "\_finished" suffix is added to PNG file after streaming has finished. So that it will be in the following template

```
http://<SERVER_NAME>:5080/<APP_NAME>/previews/<STREAM_ID>_finished.png
```

The absolute path of the preview image is as follows:

```
<ANT_MEDIA_SERVER_DIR>/webapps/<APP_NAME>/previews/<STREAM_ID>.png
```

In addition to this, you can also upload preview images to Amazon S3. Please [check out the instructions for S3 Integration](/v1/docs/amazon-aws-s3-integration).

## Configuration parameters

You can add/change following properties to the ```<ANT_MEDIA_SERVER_DIR>`/webapps/`<APP_NAME>`/WEB-INF/red5-web.properties```

```settings.previewHeight```: Preview image is saved as 480p default. If you want to increase the resolution, add the following parameter into red5-web.properties file.

```js
settings.previewHeight=360
```

```settings.createPreviewPeriod```: Preview image creation period in milliseconds. The default value is 5000 ms. As an example, if you change it as follows, it will create a preview every second.

```js
settings.createPreviewPeriod=1000
```

```settings.previewOverwrite```: Default value is false. If it is false, when a new stream is received with the same stream id, \_N (increasing number) suffix is added to the preview file name. If it is true, a new preview file will overwrite the old one.

```js
settings.previewOverwrite=false
```

```settings.addDateTimeToMp4FileName```: Default value is false. If true, adds date-time value to file names. If false, it does not add date-time values to file names.

```js
settings.addDateTimeToMp4FileName=false
```

As an alternative, you can also enable this feature on the web panel by enabling the tick box under ```Application >` Your App >` Settings >` Add Date-Time to Record File names``` and saving the settings.

![](@site/static/img/preview_2.png)

```settings.previewGenerate```: Default value is true. If false, preview images will not be generated.

```js
settings.previewGenerate=true
```

Keep in mind that if you change the configuration files, you also need to restart Ant Media Server for changes to take effect.

```js
systemctl restart antmedia
```

You can also enable thumbnail generating options from web panel after version 2.4.3:  
  
![](@site/static/img/Screenshot from 2022-03-30 16-40-21.png)