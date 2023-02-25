---
id: video-effect
---
<a name="VideoEffect"></a>

## VideoEffect()
This class is used to apply a video effect to the video stream.
It's compatible with Ant Media Server JavaScript SDK v2.5.2+

**Kind**: global function  

* [123()](#VideoEffect)
    * [.init(webRTCAdaptor, streamId, virtualBackgroundImage, rawLocalVideo)](#VideoEffect+init)
    * [.createEffectCanvas()](#VideoEffect+createEffectCanvas)
    * [.initializeSelfieSegmentation()](#VideoEffect+initializeSelfieSegmentation)
    * [.enableVirtualBackground()](#VideoEffect+enableVirtualBackground)
    * [.enableBlur()](#VideoEffect+enableBlur)
    * [.removeEffect()](#VideoEffect+removeEffect)
    * [.setCanvasStreamAsCustomVideoSource()](#VideoEffect+setCanvasStreamAsCustomVideoSource)
    * [.loadSelfieSegmentation()](#VideoEffect+loadSelfieSegmentation)
    * [.playing()](#VideoEffect+playing) ⇒ <code>Promise.&lt;void&gt;</code>
    * [.drawSegmentationMask(segmentation)](#VideoEffect+drawSegmentationMask)
    * [.onResults(results)](#VideoEffect+onResults)
    * [.drawImageDirectly(image)](#VideoEffect+drawImageDirectly)
    * [.drawVirtualBackground(image, segmentation, virtualBackgroundImage)](#VideoEffect+drawVirtualBackground)
    * [.drawBlurBackground(image, segmentation, blurAmount)](#VideoEffect+drawBlurBackground)

<a name="VideoEffect+init"></a>

### videoEffect.init(webRTCAdaptor, streamId, virtualBackgroundImage, rawLocalVideo)
This method is used to initialize the video effect.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param | Type | Description |
| --- | --- | --- |
| webRTCAdaptor | <code>WebRTCAdaptor</code> | Ant Media Server JavaScript SDK instance |
| streamId | <code>string</code> | Stream ID |
| virtualBackgroundImage | <code>HTMLElement</code> | Element of virtual background image. You should set the image source before calling this method. |
| rawLocalVideo | <code>HTMLElement</code> | Element of raw local video. It's used to keep the raw video stream. |

<a name="VideoEffect+createEffectCanvas"></a>

### videoEffect.createEffectCanvas()
This method is used to create the canvas element which is used to apply the video effect.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+initializeSelfieSegmentation"></a>

### videoEffect.initializeSelfieSegmentation()
This method is used to initialize the selfie segmentation.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+enableVirtualBackground"></a>

### videoEffect.enableVirtualBackground()
This method is used to activate the virtual background effect to the video stream.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+enableBlur"></a>

### videoEffect.enableBlur()
This method is used to activate the blur effect to the video stream.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+removeEffect"></a>

### videoEffect.removeEffect()
This method is used to disable the virtual background and blur effects.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+setCanvasStreamAsCustomVideoSource"></a>

### videoEffect.setCanvasStreamAsCustomVideoSource()
This method is used to prepare canvas stream and set the custom video source on Ant Media Server SDK.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+loadSelfieSegmentation"></a>

### videoEffect.loadSelfieSegmentation()
This method is used to prepare the raw video stream.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+playing"></a>

### videoEffect.playing() ⇒ <code>Promise.&lt;void&gt;</code>
This method is used to send raw video stream to mediapipe.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  
<a name="VideoEffect+drawSegmentationMask"></a>

### videoEffect.drawSegmentationMask(segmentation)
This method is used to draw the segmentation mask.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param | Type | Description |
| --- | --- | --- |
| segmentation | <code>Uint8Array</code> | Segmentation mask |

<a name="VideoEffect+onResults"></a>

### videoEffect.onResults(results)
This method is called by mediapipe when the segmentation mask is ready.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param |
| --- |
| results | 

<a name="VideoEffect+drawImageDirectly"></a>

### videoEffect.drawImageDirectly(image)
This method is used to draw the raw frame directly to the canvas.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param |
| --- |
| image | 

<a name="VideoEffect+drawVirtualBackground"></a>

### videoEffect.drawVirtualBackground(image, segmentation, virtualBackgroundImage)
This method is used to draw the frame with virtual background effect to the canvas.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param |
| --- |
| image | 
| segmentation | 
| virtualBackgroundImage | 

<a name="VideoEffect+drawBlurBackground"></a>

### videoEffect.drawBlurBackground(image, segmentation, blurAmount)
This method is used to draw frame with background blur effect to the canvas.

**Kind**: instance method of [<code>VideoEffect</code>](#VideoEffect)  

| Param |
| --- |
| image | 
| segmentation | 
| blurAmount | 

