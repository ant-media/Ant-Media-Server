package io.antmedia.plugin;

import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;

import io.antmedia.plugin.api.IPacketListener;

public class PacketFeeder{

	private Queue<IPacketListener> listeners = new ConcurrentLinkedQueue<>();
	private String streamId;
	private AVPacket videoPkt;
	private AVPacket audioPkt;


	public PacketFeeder(String streamId) {
		this.streamId = streamId;
		audioPkt = avcodec.av_packet_alloc();
		av_init_packet(audioPkt);

		videoPkt = avcodec.av_packet_alloc();
		av_init_packet(videoPkt);
	}

	public void writeTrailer() {
		for (IPacketListener listener : listeners) {
			listener.writeTrailer(streamId);
		}
	}

	public void writePacket(AVPacket packet, int type) {
		for (IPacketListener listener : listeners) {
			if(type == AVMEDIA_TYPE_VIDEO) {
				listener.onVideoPacket(streamId, packet);
			}
			else if(type == AVMEDIA_TYPE_AUDIO) {
				listener.onAudioPacket(streamId, packet);
			}
		}
	}

	public boolean addListener(IPacketListener listener) {
		return listeners.add(listener);
	}

	public boolean removeListener(IPacketListener listener) {
		return listeners.remove(listener);
	}


	public void writeAudioBuffer(ByteBuffer audioFrame, int streamIndex, long timestamp) {
		if(!listeners.isEmpty()) {
			audioPkt.stream_index(streamIndex);
			audioPkt.pts(timestamp);
			audioPkt.dts(timestamp);
			audioFrame.rewind();
			audioPkt.flags(audioPkt.flags() | AV_PKT_FLAG_KEY);
			audioPkt.data(new BytePointer(audioFrame));
			audioPkt.size(audioFrame.limit());
			audioPkt.position(0);

			writePacket(audioPkt, AVMEDIA_TYPE_AUDIO);

			av_packet_unref(audioPkt);
		}
	}

	public void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,
			boolean isKeyFrame,long firstFrameTimeStamp, long pts) {
		if(!listeners.isEmpty()) {
			videoPkt.stream_index(streamIndex);
			videoPkt.pts(pts);
			videoPkt.dts(dts);

			encodedVideoFrame.rewind();
			if (isKeyFrame) {
				videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
			}

			BytePointer bytePointer = new BytePointer(encodedVideoFrame);
			videoPkt.data(bytePointer);
			videoPkt.size(encodedVideoFrame.limit());
			videoPkt.position(0);


			writePacket(videoPkt, AVMEDIA_TYPE_VIDEO);

			av_packet_unref(videoPkt);
		}
	}
}
