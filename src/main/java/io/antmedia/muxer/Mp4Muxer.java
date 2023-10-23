package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AC3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_DIRAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_DTS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_DVD_SUBTITLE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_EAC3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_HEVC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_JPEG2000;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEG;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MOV_TEXT;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP2;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP4ALS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG1VIDEO;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG2VIDEO;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG4;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG4SYSTEMS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_NONE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PNG;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_QCELP;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_TSCC2;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VC1;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VORBIS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VP9;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_get_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_init;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avformat.avio_open;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.antmedia.FFmpegUtilities;
import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVBitStreamFilter;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public class Mp4Muxer extends RecordMuxer {

	private AVBSFContext bsfContext;
	
	private boolean isAVCConversionRequired = false;
	
	private static int[] MP4_SUPPORTED_CODECS = {
			AV_CODEC_ID_MOV_TEXT     ,
			AV_CODEC_ID_MPEG4        ,
			AV_CODEC_ID_H264         ,
			AV_CODEC_ID_HEVC         ,
			AV_CODEC_ID_AAC          ,
			AV_CODEC_ID_MP4ALS       , /* 14496-3 ALS */
			AV_CODEC_ID_MPEG2VIDEO  , /* MPEG-2 Main */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Simple */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 SNR */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Spatial */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 High */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 422 */
			AV_CODEC_ID_AAC          , /* MPEG-2 AAC Main */
			AV_CODEC_ID_MP3          , /* 13818-3 */
			AV_CODEC_ID_MP2          , /* 11172-3 */
			AV_CODEC_ID_MPEG1VIDEO   , /* 11172-2 */
			AV_CODEC_ID_MP3          , /* 11172-3 */
			AV_CODEC_ID_MJPEG        , /* 10918-1 */
			AV_CODEC_ID_PNG          ,
			AV_CODEC_ID_JPEG2000     , /* 15444-1 */
			AV_CODEC_ID_VC1          ,
			AV_CODEC_ID_DIRAC        ,
			AV_CODEC_ID_AC3          ,
			AV_CODEC_ID_EAC3         ,
			AV_CODEC_ID_DTS          , /* mp4ra.org */
			AV_CODEC_ID_VP9          , /* nonstandard, update when there is a standard value */
			AV_CODEC_ID_TSCC2        , /* nonstandard, camtasia uses it */
			AV_CODEC_ID_VORBIS       , /* nonstandard, gpac uses it */
			AV_CODEC_ID_DVD_SUBTITLE , /* nonstandard, see unsupported-embedded-subs-2.mp4 */
			AV_CODEC_ID_QCELP        ,
			AV_CODEC_ID_MPEG4SYSTEMS ,
			AV_CODEC_ID_MPEG4SYSTEMS ,
			AV_CODEC_ID_NONE
	};

	public Mp4Muxer(StorageClient storageClient, Vertx vertx, String s3FolderPath) {
		super(storageClient, vertx, s3FolderPath);
		options.put("movflags", "faststart");
		extension = ".mp4";
		format = "mp4";
		SUPPORTED_CODECS = MP4_SUPPORTED_CODECS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex,
			boolean isAVC, AVCodecParameters codecpar) {
		isAVCConversionRequired = !isAVC;

		return super.addVideoStream(width, height, timebase, codecId, streamIndex, isAVC, codecpar);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean prepareAudioOutStream(AVStream inStream, AVStream outStream) {
		if (bsfVideoName != null) {
			AVBitStreamFilter adtsToAscBsf = av_bsf_get_by_name(this.bsfVideoName);
			bsfContext = new AVBSFContext(null);

			int ret = av_bsf_alloc(adtsToAscBsf, bsfContext);
			if (ret < 0) {
				logger.info("cannot allocate bsf context for {}", streamId);
				return false;
			}

			ret = avcodec_parameters_copy(bsfContext.par_in(), inStream.codecpar());
			if (ret < 0) {
				logger.info("cannot copy input codec parameters for {}", streamId);
				return false;
			}
			bsfContext.time_base_in(inStream.time_base());

			ret = av_bsf_init(bsfContext);
			if (ret < 0) {
				logger.info("cannot init bit stream filter context for {}", streamId);
				return false;
			}

			ret = avcodec_parameters_copy(outStream.codecpar(), bsfContext.par_out());
			if (ret < 0) {
				logger.info("cannot copy codec parameters to output for {}", streamId);
				return false;
			}

			outStream.time_base(bsfContext.time_base_out());
		}
		else {
			return super.prepareAudioOutStream(inStream, outStream);
		}
		return true;
	}

	/**
	 * 
	 * @param srcFile
	 * @param dstFile
	 * @param rotation clockwise rotation
	 */
	public static void remux(String srcFile, String dstFile, int rotation) {
		AVFormatContext inputContext = new AVFormatContext(null);
		int ret;
		if ((ret = avformat_open_input(inputContext,srcFile, null, null)) < 0) {
			loggerStatic.warn("cannot open input context {} errror code: {}", srcFile, ret);
			return;
		}

		ret = avformat_find_stream_info(inputContext, (AVDictionary)null);

		if (ret < 0) {
			loggerStatic.warn("Cannot find stream info {}", srcFile);
			return;
		}


		AVFormatContext outputContext = new AVFormatContext(null);
		avformat_alloc_output_context2(outputContext, null, null, dstFile);

		int streamCount = inputContext.nb_streams();
		for (int i = 0; i < streamCount; i++) {
			AVStream stream = avformat_new_stream(outputContext, null);
			ret = avcodec_parameters_copy(stream.codecpar(), inputContext.streams(i).codecpar());
			if (ret < 0) {
				loggerStatic.warn("Cannot copy codecpar parameters from {} to {} for stream index {}", srcFile, dstFile, i);
				return;
			}
			stream.codecpar().codec_tag(0);

			if (stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) 
			{	
				//display matrix is 3x3
				int size = 9 * Pointer.sizeof(IntPointer.class);

				//Let me try to explain Why we're allocation with av_malloc here.

				//if we just allocate with new Inpointer(size), the memory is released after some time by garbage collector
				//Then avformat_free_context(outputContext) also free the memory in side data and we get double-free or corrupted memory
				//Keep in mind that Memory allocated in the native side with av_malloc is not got free by gc.

				//On the other hand, if av_stream_add_side_data below would copy the side data, there would be no problem.
				//av_stream_add_side_data just sets the pointer
				//mekya Jan 29, 22
				IntPointer rotationMatrixPointer = new IntPointer(avutil.av_malloc(size)).capacity(size);
				
				avutil.av_display_rotation_set(rotationMatrixPointer, rotation);

				BytePointer bytePointer = new BytePointer(rotationMatrixPointer);
				bytePointer.limit(rotationMatrixPointer.sizeof() * rotationMatrixPointer.limit());
				
				ret = avformat.av_stream_add_side_data(stream, avcodec.AV_PKT_DATA_DISPLAYMATRIX , bytePointer, bytePointer.limit());
				if (ret < 0) {
					loggerStatic.error("Cannot add rotation matrix side data to file:{}", dstFile);
				}				
			}
		}

		AVIOContext pb = new AVIOContext(null);
		ret = avio_open(pb, dstFile, AVIO_FLAG_WRITE);
		if (ret < 0) {
			loggerStatic.warn("Cannot open io context {}", dstFile);
			return;
		}
		outputContext.pb(pb);

		ret = avformat_write_header(outputContext, (AVDictionary)null);
		if (ret < 0) {
			loggerStatic.warn("Cannot write header to {}", dstFile);
			return;
		}

		AVPacket pkt = new AVPacket();
		while (av_read_frame(inputContext, pkt) == 0) {

			AVStream inStream = inputContext.streams(pkt.stream_index());
			AVStream outStream = outputContext.streams(pkt.stream_index());

			/* copy packet */
			pkt.pts(av_rescale_q_rnd(pkt.pts(), inStream.time_base(), outStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), inStream.time_base(), outStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.duration(av_rescale_q(pkt.duration(), inStream.time_base(), outStream.time_base()));
			pkt.pos(-1);
			av_write_frame(outputContext, pkt);
			av_packet_unref(pkt);
		}

		av_write_trailer(outputContext);

		avformat_close_input(inputContext);

		avio_closep(outputContext.pb());
		avformat_free_context(outputContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finalizeRecordFile(final File file) throws IOException {
		if (isAVCConversionRequired ) {
			logger.info("AVC conversion needed for MP4 {}", fileTmp.getName());
			//TODO: There are AV_PKT_DATA_DISPLAYMATRIX and 
			remux(fileTmp.getAbsolutePath(),file.getAbsolutePath(), rotation);
			Files.delete(fileTmp.toPath());
		}
		else {
			super.finalizeRecordFile(file);
		}
	}
	


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void clearResource() {
		super.clearResource();

		if (bsfContext != null) {
			av_bsf_free(bsfContext);
			bsfContext = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeAudioFrame(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context, long dts) {
		int ret;
		if (bsfContext != null) {
			ret = av_bsf_send_packet(bsfContext, getTmpPacket());
			if (ret < 0)
				return;

			while (av_bsf_receive_packet(bsfContext, getTmpPacket()) == 0) 
			{

				ret = av_write_frame(context, getTmpPacket());
				if (ret < 0 && logger.isInfoEnabled()) {
					logger.info("cannot write audio frame to muxer({}) av_bsf_receive_packet. Error is {} ", file.getName(), getErrorDefinition(ret));
					logger.info("input timebase num/den {}/{}"
							+ "output timebase num/den {}/{}", inputTimebase.num(), inputTimebase.den(),
							outputTimebase.num(),  outputTimebase.den());

					logger.info("received dts {}", dts);
					logger.info("calculated dts {}", pkt.dts());
				}

			}
		}
		else {
			super.writeAudioFrame(pkt, inputTimebase, outputTimebase, context, dts);
		}
	}

}
