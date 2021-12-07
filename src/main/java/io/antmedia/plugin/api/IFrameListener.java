package io.antmedia.plugin.api;

import org.bytedeco.ffmpeg.avutil.AVFrame;

/*
 * Interface class to feed the plugins with decoded video/audio frames.
 * Plugins that consume the video/audio frames should implement this interface.
 */

public interface IFrameListener {
	/*
	 * AMS feeds the plugins with decoded audio frames with this method.
	 * @param streamId is the id of the stream
	 * @param audioFrame is AVFrame type defined in javacpp ffmpeg wrapper
	 * @returns AVFrame this may have 3 different values according to the plugin
	 * 			1. Same as input: AMS streams to viewers or records this original frame
	 * 			2. Modified frame by plugin: AMS streams to viewers or records this modified frame
	 * 			3. null: AMS doesn't streams to viewers or records
	 */
	AVFrame onAudioFrame(String streamId, AVFrame audioFrame);
	
	/*
	 * AMS feeds the plugins with decoded video frames with this method.
	 * @param streamId is the id of the stream
	 * @param videooFrame is AVFrame type defined in javacpp ffmpeg wrapper
	 * @returns AVFrame this may have 3 different values according to the plugin
	 * 			1. Same as input: AMS streams to viewers or records this original frame
	 * 			2. Modified frame by plugin: AMS streams to viewers or records this modified frame
	 * 			3. null: AMS doesn't streams to viewers or records
	 */
	AVFrame onVideoFrame(String streamId, AVFrame videoFrame);
	
	/*
	 * AMS calls this method when the listening stream finishes
	 * 
	 */
	void writeTrailer(String streamId);
	
	/*
	 * AMS calls this method when plugin registers itself to a stream
	 * @param streamId is the id of the stream
	 * @param videoStreamInfo has the video properties of the listening stream
	 * 
	 */
	void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo);
	
	/*
	 * AMS calls this method when plugin registers itself to a stream
	 * @param streamId is the id of the stream
	 * @param videoStreamInfo has the audio properties of the listening stream
	 * 
	 */
	void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo);
	
	/*
	 * used by CustomBroadcast
	 * no need to implement in the plugin
	 * 
	 */
	void start();

}
