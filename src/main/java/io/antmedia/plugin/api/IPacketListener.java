package io.antmedia.plugin.api;

import org.bytedeco.ffmpeg.avcodec.AVPacket;

/*
 * Interface class to feed the plugins with encoded video/audio packets.
 * Plugins that consume the video/audio packets should implement this interface.
 */
public interface IPacketListener {
	/*
	 * AMS feeds the plugins with encoded video/audio packets with this method.
	 * @param streamId is the id of the stream
	 * @param packet is AVPacket type defined in javacpp ffmpeg wrapper
	 * @returns AVPacket this should be same with the input
	 */
	AVPacket onPacket(String streamId, AVPacket packet);
	
	/*
	 * AMS calls this method when the listening stream finishes
	 * 
	 */
	void writeTrailer();
	
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
}
