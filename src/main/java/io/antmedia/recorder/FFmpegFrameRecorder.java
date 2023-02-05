/*
 * Copyright (C) 2009-2022 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Based on the output-example.c file included in FFmpeg 0.6.5
 * as well as on the decoding_encoding.c file included in FFmpeg 0.11.1,
 * and on the encode_video.c file included in FFmpeg 4.4,
 * which are covered by the following copyright notice:
 *
 * Libavformat API example: Output a media file in any supported
 * libavformat format. The default codecs are used.
 *
 * Copyright (c) 2001,2003 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.antmedia.recorder;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_CAP_EXPERIMENTAL;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_FLAG_QSCALE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_FFV1;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H263;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_HUFFYUV;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_JPEGLS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEG;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEGB;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG1VIDEO;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG2VIDEO;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_NONE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16BE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_U16BE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_U16LE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PNG;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_RAWVIDEO;
import static org.bytedeco.ffmpeg.global.avcodec.AV_INPUT_BUFFER_MIN_SIZE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_jni_set_java_vm;
import static org.bytedeco.ffmpeg.global.avcodec.av_new_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_fill_audio_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_frame;
import static org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_guess_format;
import static org.bytedeco.ffmpeg.global.avformat.av_interleaved_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_close;
import static org.bytedeco.ffmpeg.global.avformat.avio_open2;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_GRAY16BE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_GRAY16LE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_GRAY8;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NONE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NV21;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGB32;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUVJ420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_DBL;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_DBLP;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_NONE;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S32;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S32P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_U8;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_U8P;
import static org.bytedeco.ffmpeg.global.avutil.FF_QP2LAMBDA;
import static org.bytedeco.ffmpeg.global.avutil.av_d2q;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_find_nearest_q_idx;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_free;
import static org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample;
import static org.bytedeco.ffmpeg.global.avutil.av_get_default_channel_layout;
import static org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays;
import static org.bytedeco.ffmpeg.global.avutil.av_image_get_buffer_size;
import static org.bytedeco.ffmpeg.global.avutil.av_inv_q;
import static org.bytedeco.ffmpeg.global.avutil.av_log_get_level;
import static org.bytedeco.ffmpeg.global.avutil.av_malloc;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_sample_fmt_is_planar;
import static org.bytedeco.ffmpeg.global.avutil.av_samples_get_buffer_size;
import static org.bytedeco.ffmpeg.global.swresample.swr_alloc_set_opts;
import static org.bytedeco.ffmpeg.global.swresample.swr_convert;
import static org.bytedeco.ffmpeg.global.swresample.swr_free;
import static org.bytedeco.ffmpeg.global.swresample.swr_init;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR;
import static org.bytedeco.ffmpeg.global.swscale.sws_freeContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_getCachedContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;

import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacpp.ShortPointer;

/**
 *
 * @author Samuel Audet
 */
public class FFmpegFrameRecorder extends FrameRecorder {

    public static class Exception extends FrameRecorder.Exception {
        public Exception(String message) { super(message + " (For more details, make sure FFmpegLogCallback.set() has been called.)"); }
        public Exception(String message, Throwable cause) { super(message, cause); }
    }

    private static Exception loadingException = null;
    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        } else {
            try {
                Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
                Loader.load(org.bytedeco.ffmpeg.global.swresample.class);
                Loader.load(org.bytedeco.ffmpeg.global.avcodec.class);
                Loader.load(org.bytedeco.ffmpeg.global.avformat.class);
                Loader.load(org.bytedeco.ffmpeg.global.swscale.class);

                /* initialize libavcodec, and register all codecs and formats */
                av_jni_set_java_vm(Loader.getJavaVM(), null);
                avformat_network_init();

                Loader.load(org.bytedeco.ffmpeg.global.avdevice.class);
                avdevice_register_all();
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw loadingException = (Exception)t;
                } else {
                    throw loadingException = new Exception("Failed to load " + FFmpegFrameRecorder.class, t);
                }
            }
        }
    }

    static {
        try {
            tryLoad();
        } catch (Exception ex) { }
    }

   
    public FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight, int audioChannels) {
        this.filename      = filename;
        this.imageWidth    = imageWidth;
        this.imageHeight   = imageHeight;
        this.audioChannels = audioChannels;

        this.pixelFormat   = AV_PIX_FMT_NONE;
        this.videoCodec    = AV_CODEC_ID_NONE;
        this.videoBitrate  = 400000;
        this.frameRate     = 30;

        this.sampleFormat  = AV_SAMPLE_FMT_NONE;
        this.audioCodec    = AV_CODEC_ID_NONE;
        this.audioBitrate  = 64000;
        this.sampleRate    = 44100;

        this.interleaved = true;
    }

    public void release() throws Exception {
        synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
            releaseUnsafe();
        }
    }
    public synchronized void releaseUnsafe() throws Exception {
        started = false;

        if (planePtr != null && planePtr2 != null) {
            planePtr.releaseReference();
            planePtr2.releaseReference();
            planePtr = planePtr2 = null;
        }

        if (video_pkt != null && audio_pkt != null) {
            video_pkt.releaseReference();
            audio_pkt.releaseReference();
            video_pkt = audio_pkt = null;
        }

        /* close each codec */
        if (video_c != null) {
            avcodec_free_context(video_c);
            video_c = null;
        }
        if (audio_c != null) {
            avcodec_free_context(audio_c);
            audio_c = null;
        }
        if (picture_buf != null) {
            av_free(picture_buf);
            picture_buf = null;
        }
        if (picture != null) {
            av_frame_free(picture);
            picture = null;
        }
        if (tmp_picture != null) {
            av_frame_free(tmp_picture);
            tmp_picture = null;
        }
        if (video_outbuf != null) {
            av_free(video_outbuf);
            video_outbuf = null;
        }
        if (frame != null) {
            av_frame_free(frame);
            frame = null;
        }
        if (samples_in != null) {
            for (int i = 0; i < samples_in.length; i++) {
                if (samples_in[i] != null) {
                    samples_in[i].releaseReference();
                }
            }
            samples_in = null;
        }
        if (samples_out != null) {
            for (int i = 0; i < samples_out.length; i++) {
                av_free(samples_out[i].position(0));
            }
            samples_out = null;
        }
        if (audio_outbuf != null) {
            av_free(audio_outbuf);
            audio_outbuf = null;
        }
        if (video_st != null && video_st.metadata() != null) {
            av_dict_free(video_st.metadata());
            video_st.metadata(null);
        }
        if (audio_st != null && audio_st.metadata() != null) {
            av_dict_free(audio_st.metadata());
            audio_st.metadata(null);
        }
        video_st = null;
        audio_st = null;
        filename = null;

        if (oc != null && !oc.isNull()) {
            if ((oformat.flags() & AVFMT_NOFILE) == 0) {
                /* close the output file */
                avio_close(oc.pb());
            }

            /* free the streams */
            avformat_free_context(oc);
            oc = null;
        }

        if (img_convert_ctx != null) {
            sws_freeContext(img_convert_ctx);
            img_convert_ctx = null;
        }

        if (samples_convert_ctx != null) {
            swr_free(samples_convert_ctx);
            samples_convert_ctx = null;
        }

    }
    @Override protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    static Map<Pointer,OutputStream> outputStreams = Collections.synchronizedMap(new HashMap<Pointer,OutputStream>());

    private String filename;
    private AVFrame picture, tmp_picture;
    private BytePointer picture_buf;
    private BytePointer video_outbuf;
    private int video_outbuf_size;
    private AVFrame frame;
    private Pointer[] samples_in;
    private BytePointer[] samples_out;
    private BytePointer audio_outbuf;
    private int audio_outbuf_size;
    private int audio_input_frame_size;
    private AVOutputFormat oformat;
    private AVFormatContext oc;
    private AVCodec video_codec, audio_codec;
    private AVCodecContext video_c, audio_c;
    private AVStream video_st, audio_st;
    private SwsContext img_convert_ctx;
    private SwrContext samples_convert_ctx;
    private int samples_channels, samples_format, samples_rate;
    private PointerPointer planePtr;
    private PointerPointer planePtr2;
    private AVPacket video_pkt, audio_pkt;
    private int[] got_video_packet, got_audio_packet;
    private AVFormatContext ifmt_ctx;

    private volatile boolean started = false;

    @Override public int getFrameNumber() {
        return picture == null ? super.getFrameNumber() : (int)picture.pts();
    }
    @Override public void setFrameNumber(int frameNumber) {
        if (picture == null) { super.setFrameNumber(frameNumber); } else { picture.pts(frameNumber); }
    }

    /** Returns best guess for timestamp in microseconds... */
    @Override public long getTimestamp() {
        return Math.round(getFrameNumber() * 1000000L / getFrameRate());
    }
    @Override public void setTimestamp(long timestamp)  {
        setFrameNumber((int)Math.round(timestamp * getFrameRate() / 1000000L));
    }

    @Override public void start() throws Exception {
        synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
            startUnsafe();
        }
    }

    /*
     * Resources are closed in releaseUnsafe
     */
    @SuppressWarnings("resource")
	public synchronized void startUnsafe() throws Exception {
        try (PointerScope scope = new PointerScope()) {

        if (oc != null && !oc.isNull()) {
            throw new Exception("start() has already been called: Call stop() before calling start() again.");
        }

        int ret;
        picture = null;
        tmp_picture = null;
        picture_buf = null;
        frame = null;
        video_outbuf = null;
        audio_outbuf = null;
        oc = new AVFormatContext(null);
        video_c = null;
        audio_c = null;
        video_st = null;
        audio_st = null;
        planePtr  = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS).retainReference();
        planePtr2 = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS).retainReference();
        video_pkt = new AVPacket().retainReference();
        audio_pkt = new AVPacket().retainReference();
        got_video_packet = new int[1];
        got_audio_packet = new int[1];

        /* auto detect the output format from the name. */
        String format_name = format == null || format.length() == 0 ? null : format;
        if ((oformat = av_guess_format(format_name, filename, null)) == null) {
            int proto = filename.indexOf("://");
            if (proto > 0) {
                format_name = filename.substring(0, proto);
            }
            if ((oformat = av_guess_format(format_name, filename, null)) == null) {
                throw new Exception("av_guess_format() error: Could not guess output format for \"" + filename + "\" and " + format + " format.");
            }
        }
        format_name = oformat.name().getString();

        /* allocate the output media context */
        if (avformat_alloc_output_context2(oc, null, format_name, filename) < 0) {
            throw new Exception("avformat_alloc_context2() error:\tCould not allocate format context");
        }

        oc.oformat(oformat);
        oc.url(new BytePointer(av_malloc(filename.getBytes().length + (long)1)).putString(filename));
        oc.max_delay(maxDelay);

        /* add the audio and video streams using the format codecs
           and initialize the codecs */
        AVStream inpVideoStream = null, inpAudioStream = null;
     

        if (imageWidth > 0 && imageHeight > 0) {

            /* find the video encoder */
            if ((video_codec = avcodec_find_encoder_by_name(videoCodecName)) == null &&
                (video_codec = avcodec_find_encoder(videoCodec)) == null) {
                releaseUnsafe();
                throw new Exception("avcodec_find_encoder() error: Video codec not found.");
            }

            AVRational frame_rate = av_d2q(frameRate, 1001000);
            AVRational supported_framerates = video_codec.supported_framerates();
            if (supported_framerates != null) {
                int idx = av_find_nearest_q_idx(frame_rate, supported_framerates);
                frame_rate = supported_framerates.position(idx);
            }

            /* add a video output stream */
            if ((video_st = avformat_new_stream(oc, null)) == null) {
                releaseUnsafe();
                throw new Exception("avformat_new_stream() error: Could not allocate video stream.");
            }

            if ((video_c = avcodec_alloc_context3(video_codec)) == null) {
                releaseUnsafe();
                throw new Exception("avcodec_alloc_context3() error: Could not allocate video encoding context.");
            }

            video_c.codec_id(video_codec.id());
            video_c.codec_type(AVMEDIA_TYPE_VIDEO);


            /* put sample parameters */
            video_c.bit_rate(videoBitrate);
            /* resolution must be a multiple of two. Scale height to maintain the aspect ratio. */
            if (imageWidth % 2 == 1) {
                int roundedWidth = imageWidth + 1;
                imageHeight = (roundedWidth * imageHeight + imageWidth / 2) / imageWidth;
                imageWidth = roundedWidth;
            }
            video_c.width(imageWidth);
            video_c.height(imageHeight);
            if (aspectRatio > 0) {
                AVRational r = av_d2q(aspectRatio, 255);
                video_c.sample_aspect_ratio(r);
                video_st.sample_aspect_ratio(r);
            }
            /* time base: this is the fundamental unit of time (in seconds) in terms
               of which frame timestamps are represented. for fixed-fps content,
               timebase should be 1/framerate and timestamp increments should be
               identically 1. */
            AVRational time_base = av_inv_q(frame_rate);
            video_c.time_base(time_base);
            video_st.time_base(time_base);
            video_st.avg_frame_rate(frame_rate);
            if (gopSize >= 0) {
                video_c.gop_size(gopSize); /* emit one intra frame every gopSize frames at most */
            }
            if (videoQuality >= 0) {
                video_c.flags(video_c.flags() | AV_CODEC_FLAG_QSCALE);
                video_c.global_quality((int)Math.round(FF_QP2LAMBDA * videoQuality));
            }

            if (pixelFormat != AV_PIX_FMT_NONE) {
                video_c.pix_fmt(pixelFormat);
            } else if (video_c.codec_id() == AV_CODEC_ID_RAWVIDEO || video_c.codec_id() == AV_CODEC_ID_PNG ||
                       video_c.codec_id() == AV_CODEC_ID_HUFFYUV  || video_c.codec_id() == AV_CODEC_ID_FFV1) {
                video_c.pix_fmt(AV_PIX_FMT_RGB32);   // appropriate for common lossless formats
            } else if (video_c.codec_id() == AV_CODEC_ID_JPEGLS) {
                video_c.pix_fmt(AV_PIX_FMT_BGR24);
            } else if (video_c.codec_id() == AV_CODEC_ID_MJPEG || video_c.codec_id() == AV_CODEC_ID_MJPEGB) {
                video_c.pix_fmt(AV_PIX_FMT_YUVJ420P);
            } else {
                video_c.pix_fmt(AV_PIX_FMT_YUV420P); // lossy, but works with about everything
            }

            if (video_c.codec_id() == AV_CODEC_ID_MPEG2VIDEO) {
                /* just for testing, we also add B frames */
                video_c.max_b_frames(2);
            } else if (video_c.codec_id() == AV_CODEC_ID_MPEG1VIDEO) {
                /* Needed to avoid using macroblocks in which some coeffs overflow.
                   This does not happen with normal video, it just happens here as
                   the motion of the chroma plane does not match the luma plane. */
                video_c.mb_decision(2);
            } else if (video_c.codec_id() == AV_CODEC_ID_H263) {
                // H.263 does not support any other resolution than the following
                if (imageWidth <= 128 && imageHeight <= 96) {
                    video_c.width(128).height(96);
                } else if (imageWidth <= 176 && imageHeight <= 144) {
                    video_c.width(176).height(144);
                } else if (imageWidth <= 352 && imageHeight <= 288) {
                    video_c.width(352).height(288);
                } else if (imageWidth <= 704 && imageHeight <= 576) {
                    video_c.width(704).height(576);
                } else {
                    video_c.width(1408).height(1152);
                }
            } else if (video_c.codec_id() == AV_CODEC_ID_H264) {
                // default to constrained baseline to produce content that plays back on anything,
                // without any significant tradeoffs for most use cases
                video_c.profile(AVCodecContext.FF_PROFILE_H264_CONSTRAINED_BASELINE);
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                video_c.flags(video_c.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((video_codec.capabilities() & AV_CODEC_CAP_EXPERIMENTAL) != 0) {
                video_c.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }

            if (maxBFrames >= 0) {
                video_c.max_b_frames(maxBFrames);
                video_c.has_b_frames(maxBFrames == 0 ? 0 : 1);
            }

            if (trellis >= 0) {
                video_c.trellis(trellis);
            }
        }

        /*
         * add an audio output stream
         */
        if (audioChannels > 0 && audioBitrate > 0 && sampleRate > 0) {

            /* find the audio encoder */
            if ((audio_codec = avcodec_find_encoder_by_name(audioCodecName)) == null &&
                (audio_codec = avcodec_find_encoder(audioCodec)) == null) {
                releaseUnsafe();
                throw new Exception("avcodec_find_encoder() error: Audio codec not found.");
            }

            AVRational sampleRateLocal = av_d2q(sampleRate, 1001000);

            if ((audio_st = avformat_new_stream(oc, null)) == null) {
                releaseUnsafe();
                throw new Exception("avformat_new_stream() error: Could not allocate audio stream.");
            }

            if ((audio_c = avcodec_alloc_context3(audio_codec)) == null) {
                releaseUnsafe();
                throw new Exception("avcodec_alloc_context3() error: Could not allocate audio encoding context.");
            }

            audio_c.codec_id(audio_codec.id());
            audio_c.codec_type(AVMEDIA_TYPE_AUDIO);


            /* put sample parameters */
            audio_c.bit_rate(audioBitrate);
            audio_c.sample_rate(sampleRate);
            audio_c.channels(audioChannels);
            audio_c.channel_layout(av_get_default_channel_layout(audioChannels));
            if (sampleFormat != AV_SAMPLE_FMT_NONE) {
                audio_c.sample_fmt(sampleFormat);
            } else {
                // use AV_SAMPLE_FMT_S16 by default, if available
                audio_c.sample_fmt(AV_SAMPLE_FMT_FLTP);
                IntPointer formats = audio_c.codec().sample_fmts();
                for (int i = 0; formats.get(i) != -1; i++) {
                    if (formats.get(i) == AV_SAMPLE_FMT_S16) {
                        audio_c.sample_fmt(AV_SAMPLE_FMT_S16);
                        break;
                    }
                }
            }
            AVRational timeBaseLocal = av_inv_q(sampleRateLocal);
            audio_c.time_base(timeBaseLocal);
            audio_st.time_base(timeBaseLocal);
            switch (audio_c.sample_fmt()) {
                case AV_SAMPLE_FMT_U8:
                case AV_SAMPLE_FMT_U8P:  audio_c.bits_per_raw_sample(8);  break;
                case AV_SAMPLE_FMT_S16:
                case AV_SAMPLE_FMT_S16P: audio_c.bits_per_raw_sample(16); break;
                case AV_SAMPLE_FMT_S32:
                case AV_SAMPLE_FMT_S32P: audio_c.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_FLT:
                case AV_SAMPLE_FMT_FLTP: audio_c.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_DBL:
                case AV_SAMPLE_FMT_DBLP: audio_c.bits_per_raw_sample(64); break;
                default: assert false;
            }
            if (audioQuality >= 0) {
                audio_c.flags(audio_c.flags() | AV_CODEC_FLAG_QSCALE);
                audio_c.global_quality((int)Math.round(FF_QP2LAMBDA * audioQuality));
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                audio_c.flags(audio_c.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((audio_codec.capabilities() & AV_CODEC_CAP_EXPERIMENTAL) != 0) {
                audio_c.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }
        }

        /* now that all the parameters are set, we can open the audio and
           video codecs and allocate the necessary encode buffers */
        if (video_st != null && inpVideoStream == null) {
            AVDictionary options = new AVDictionary(null);
            if (videoQuality >= 0) {
                av_dict_set(options, "crf", "" + videoQuality, 0);
            }
            for (Entry<String, String> e : videoOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }

            // Enable multithreading when available
            video_c.thread_count(0);

            /* open the codec */
            if ((ret = avcodec_open2(video_c, video_codec, options)) < 0) {
                releaseUnsafe();
                av_dict_free(options);
                throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
            }
            av_dict_free(options);

            video_outbuf = null;


            /* allocate the encoded raw picture */
            if ((picture = av_frame_alloc()) == null) {
                releaseUnsafe();
                throw new Exception("av_frame_alloc() error: Could not allocate picture.");
            }
            picture.pts(0); // magic required by libx264

            int size = av_image_get_buffer_size(video_c.pix_fmt(), video_c.width(), video_c.height(), 1);
            if ((picture_buf = new BytePointer(av_malloc(size))).isNull()) {
                releaseUnsafe();
                throw new Exception("av_malloc() error: Could not allocate picture buffer.");
            }

            /* if the output format is not equal to the image format, then a temporary
               picture is needed too. It is then converted to the required output format */
            if ((tmp_picture = av_frame_alloc()) == null) {
                releaseUnsafe();
                throw new Exception("av_frame_alloc() error: Could not allocate temporary picture.");
            }

            /* copy the stream parameters to the muxer */
            if ((ret = avcodec_parameters_from_context(video_st.codecpar(), video_c)) < 0) {
                releaseUnsafe();
                throw new Exception("avcodec_parameters_from_context() error " + ret + ": Could not copy the video stream parameters.");
            }

            AVDictionary metadata = new AVDictionary(null);
            for (Entry<String, String> e : videoMetadata.entrySet()) {
                av_dict_set(metadata, new BytePointer(e.getKey(), charset), new BytePointer(e.getValue(), charset), 0);
            }
            video_st.metadata(metadata);
        }

        if (audio_st != null && inpAudioStream == null) {
            AVDictionary options = new AVDictionary(null);
            if (audioQuality >= 0) {
                av_dict_set(options, "crf", "" + audioQuality, 0);
            }
            for (Entry<String, String> e : audioOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }

            // Enable multithreading when available
            audio_c.thread_count(0);

            /* open the codec */
            if ((ret = avcodec_open2(audio_c, audio_codec, options)) < 0) {
                releaseUnsafe();
                av_dict_free(options);
                throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
            }
            av_dict_free(options);

            audio_outbuf_size = 256 * 1024;
            audio_outbuf = new BytePointer(av_malloc(audio_outbuf_size));

            /* ugly hack for PCM codecs (will be removed ASAP with new PCM
               support to compute the input frame size in samples */
            if (audio_c.frame_size() <= 1) {
                audio_outbuf_size = AV_INPUT_BUFFER_MIN_SIZE;
                audio_input_frame_size = audio_outbuf_size / audio_c.channels();
                switch (audio_c.codec_id()) {
                    case AV_CODEC_ID_PCM_S16LE:
                    case AV_CODEC_ID_PCM_S16BE:
                    case AV_CODEC_ID_PCM_U16LE:
                    case AV_CODEC_ID_PCM_U16BE:
                        audio_input_frame_size >>= 1;
                        break;
                    default:
                        break;
                }
            } else {
                audio_input_frame_size = audio_c.frame_size();
            }
            int planes = av_sample_fmt_is_planar(audio_c.sample_fmt()) != 0 ? (int)audio_c.channels() : 1;
            int data_size = av_samples_get_buffer_size((IntPointer)null, audio_c.channels(),
                    audio_input_frame_size, audio_c.sample_fmt(), 1) / planes;
            samples_out = new BytePointer[planes];
            for (int i = 0; i < samples_out.length; i++) {
                samples_out[i] = new BytePointer(av_malloc(data_size)).capacity(data_size);
            }
            samples_in = new Pointer[AVFrame.AV_NUM_DATA_POINTERS];

            /* allocate the audio frame */
            if ((frame = av_frame_alloc()) == null) {
                releaseUnsafe();
                throw new Exception("av_frame_alloc() error: Could not allocate audio frame.");
            }
            frame.pts(0); // magic required by libvorbis and webm

            /* copy the stream parameters to the muxer */
            if ((ret = avcodec_parameters_from_context(audio_st.codecpar(), audio_c)) < 0) {
                releaseUnsafe();
                throw new Exception("avcodec_parameters_from_context() error " + ret + ": Could not copy the audio stream parameters.");
            }

            AVDictionary metadata = new AVDictionary(null);
            for (Entry<String, String> e : audioMetadata.entrySet()) {
                av_dict_set(metadata, new BytePointer(e.getKey(), charset), new BytePointer(e.getValue(), charset), 0);
            }
            audio_st.metadata(metadata);
        }

        AVDictionary options = new AVDictionary(null);
        for (Entry<String, String> e : this.options.entrySet()) {
            av_dict_set(options, e.getKey(), e.getValue(), 0);
        }

        /* open the output file, if needed */
        if ((oformat.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            if ((ret = avio_open2(pb, filename, AVIO_FLAG_WRITE, null, options)) < 0) {
                String errorMsg = "avio_open2 error() error " + ret + ": Could not open '" + filename + "'";
                releaseUnsafe();
                av_dict_free(options);
                throw new Exception(errorMsg);
            }
            oc.pb(pb);
        }

        AVDictionary metadata = new AVDictionary(null);
        for (Entry<String, String> e : this.metadata.entrySet()) {
            av_dict_set(metadata, new BytePointer(e.getKey(), charset), new BytePointer(e.getValue(), charset), 0);
        }
        /* write the stream header, if any */
        if ((ret = avformat_write_header(oc.metadata(metadata), options)) < 0) {
            String errorMsg = "avformat_write_header error() error " + ret + ": Could not write header to '" + filename + "'";
            releaseUnsafe();
            av_dict_free(options);
            throw new Exception(errorMsg);
        }
        av_dict_free(options);

        if (av_log_get_level() >= AV_LOG_INFO) {
            av_dump_format(oc, 0, filename, 1);
        }

        started = true;

        }
    }

    public synchronized void flush() throws Exception {
        synchronized (oc) {
            /* flush all the buffers */
            while (video_st != null && ifmt_ctx == null && recordImage(0, 0, 0, 0, null, AV_PIX_FMT_NONE, (Buffer[])null));
            while (audio_st != null && ifmt_ctx == null && recordSamples(0, 0, (Buffer[])null));

            if (interleaved && (video_st != null || audio_st != null)) {
                av_interleaved_write_frame(oc, null);
            } else {
                av_write_frame(oc, null);
            }
        }
    }

    public void stop() throws Exception {
        if (oc != null) {
            try {
                flush();

                /* write the trailer, if any */
                av_write_trailer(oc);
            } finally {
                release();
            }
        }
    }

    @Override 
    public void record(Frame frame) throws Exception {
    	//No need to implement
    }
  

    public synchronized boolean recordImage(int width, int height, int depth, int channels, int[] stride, int pixelFormat, Buffer ... image) throws Exception {
        try (PointerScope scope = new PointerScope()) {

        if (video_st == null) {
            throw new Exception("No video output stream (Is imageWidth > 0 && imageHeight > 0 and has start() been called?)");
        }
        if (!started) {
            throw new Exception("start() was not called successfully!");
        }
        int ret;

        if (image == null || image.length == 0) {
            /* no more frame to compress. The codec has a latency of a few
               frames if using B frames, so we get the last frames by
               passing the same picture again */
        } else {
            BytePointer data = image[0] instanceof ByteBuffer
                    ? new BytePointer((ByteBuffer)image[0]).position(0)
                    : new BytePointer(new Pointer(image[0]).position(0));

            if (pixelFormat == AV_PIX_FMT_NONE) {
                if ((depth == Frame.DEPTH_UBYTE || depth == Frame.DEPTH_BYTE) && channels == 3) {
                    pixelFormat = AV_PIX_FMT_BGR24;
                } else if ((depth == Frame.DEPTH_UBYTE || depth == Frame.DEPTH_BYTE) && channels == 1) {
                    pixelFormat = AV_PIX_FMT_GRAY8;
                } else if ((depth == Frame.DEPTH_USHORT || depth == Frame.DEPTH_SHORT) && channels == 1) {
                    pixelFormat = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ?
                            AV_PIX_FMT_GRAY16BE : AV_PIX_FMT_GRAY16LE;
                } else if ((depth == Frame.DEPTH_UBYTE || depth == Frame.DEPTH_BYTE) && channels == 4) {
                    pixelFormat = AV_PIX_FMT_RGBA;
                } else if ((depth == Frame.DEPTH_UBYTE || depth == Frame.DEPTH_BYTE) && channels == 2) {
                    pixelFormat = AV_PIX_FMT_NV21; // Android's camera capture format
                } else {
                    throw new Exception("Could not guess pixel format of image: depth=" + depth + ", channels=" + channels);
                }
            }



            if (video_c.pix_fmt() != pixelFormat || video_c.width() != width || video_c.height() != height) {
                /* convert to the codec pixel format if needed */
                img_convert_ctx = sws_getCachedContext(img_convert_ctx, width, height, pixelFormat,
                        video_c.width(), video_c.height(), video_c.pix_fmt(),
                        imageScalingFlags != 0 ? imageScalingFlags : SWS_BILINEAR,
                        null, null, (DoublePointer)null);
                if (img_convert_ctx == null) {
                    throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
                }
                av_image_fill_arrays(new PointerPointer(tmp_picture), tmp_picture.linesize(), data, pixelFormat, width, height, 1);
                av_image_fill_arrays(new PointerPointer(picture), picture.linesize(), picture_buf, video_c.pix_fmt(), video_c.width(), video_c.height(), 1);
                tmp_picture.linesize(0, stride[0]);
                tmp_picture.linesize(1, stride[1]);
                tmp_picture.linesize(2, stride[2]);
                tmp_picture.format(pixelFormat);
                tmp_picture.width(width);
                tmp_picture.height(height);
                picture.format(video_c.pix_fmt());
                picture.width(video_c.width());
                picture.height(video_c.height());
                sws_scale(img_convert_ctx, new PointerPointer(tmp_picture), tmp_picture.linesize(),
                          0, height, new PointerPointer(picture), picture.linesize());
            } else {
                av_image_fill_arrays(new PointerPointer(picture), picture.linesize(), data, pixelFormat, width, height, 1);
                picture.linesize(0, stride[0]);
                picture.linesize(1, stride[1]);
                picture.linesize(2, stride[2]);
                picture.format(pixelFormat);
                picture.width(width);
                picture.height(height);
            }
        }


            /* encode the image */
            picture.quality(video_c.global_quality());
            if ((ret = avcodec_send_frame(video_c, image == null || image.length == 0 ? null : picture)) < 0
                    && image != null && image.length != 0) {
                throw new Exception("avcodec_send_frame() error " + ret + ": Error sending a video frame for encoding.");
            }
            picture.pts(picture.pts() + 1); // magic required by libx264

            /* if zero size, it means the image was buffered */
            got_video_packet[0] = 0;
            while (ret >= 0) {
                av_new_packet(video_pkt, video_outbuf_size);
                ret = avcodec_receive_packet(video_c, video_pkt);
                if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
                    av_packet_unref(video_pkt);
                    break;
                } else if (ret < 0) {
                    av_packet_unref(video_pkt);
                    throw new Exception("avcodec_receive_packet() error " + ret + ": Error during video encoding.");
                }
                got_video_packet[0] = 1;

                if (video_pkt.pts() != AV_NOPTS_VALUE) {
                    video_pkt.pts(av_rescale_q(video_pkt.pts(), video_c.time_base(), video_st.time_base()));
                }
                if (video_pkt.dts() != AV_NOPTS_VALUE) {
                    video_pkt.dts(av_rescale_q(video_pkt.dts(), video_c.time_base(), video_st.time_base()));
                }
                video_pkt.stream_index(video_st.index());

                /* write the compressed frame in the media file */
                writePacket(AVMEDIA_TYPE_VIDEO, video_pkt);
            }
        return image != null ? (video_pkt.flags() & AV_PKT_FLAG_KEY) != 0 : got_video_packet[0] != 0;

        }
    }

    public boolean recordSamples(Buffer ... samples) throws Exception {
        return recordSamples(0, 0, samples);
    }
    public synchronized boolean recordSamples(int sampleRate, int audioChannels, Buffer ... samples) throws Exception {
        try (PointerScope scope = new PointerScope()) {

        if (audio_st == null) {
            throw new Exception("No audio output stream (Is audioChannels > 0 and has start() been called?)");
        }
        if (!started) {
            throw new Exception("start() was not called successfully!");
        }

        if (samples == null && samples_out[0].position() > 0) {
            // Typically samples_out[0].limit() is double the audio_input_frame_size --> sampleDivisor = 2
            double sampleDivisor = Math.floor((int)Math.min(samples_out[0].limit(), Integer.MAX_VALUE) / audio_input_frame_size);
            writeSamples((int)Math.floor((int)samples_out[0].position() / sampleDivisor));
            return writeFrame((AVFrame)null);
        }

        int ret;

        if (sampleRate <= 0) {
            sampleRate = audio_c.sample_rate();
        }
        if (audioChannels <= 0) {
            audioChannels = audio_c.channels();
        }
        int inputSize = samples != null ? samples[0].limit() - samples[0].position() : 0;
        int inputFormat = samples_format;
        int inputChannels = samples != null && samples.length > 1 ? 1 : audioChannels;
        int inputDepth = 0;
        int outputFormat = audio_c.sample_fmt();
        int outputChannels = samples_out.length > 1 ? 1 : audio_c.channels();
        int outputDepth = av_get_bytes_per_sample(outputFormat);
        if (samples != null && samples[0] instanceof ShortBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_S16P : AV_SAMPLE_FMT_S16;
            inputDepth = 2;
            for (int i = 0; i < samples.length; i++) {
                ShortBuffer b = (ShortBuffer)samples[i];
                if (samples_in[i] instanceof ShortPointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((ShortPointer)samples_in[i]).position(0).put(b.array(), samples[i].position(), inputSize);
                } else {
                    if (samples_in[i] != null) {
                        samples_in[i].releaseReference();
                    }
                    samples_in[i] = new ShortPointer(b).retainReference();
                }
            }
        } else if (samples != null) {
            throw new Exception("Audio samples Buffer has unsupported type: " + samples);
        }

        if (samples_convert_ctx == null || samples_channels != audioChannels || samples_format != inputFormat || samples_rate != sampleRate) {
            samples_convert_ctx = swr_alloc_set_opts(samples_convert_ctx, audio_c.channel_layout(), outputFormat, audio_c.sample_rate(),
                    av_get_default_channel_layout(audioChannels), inputFormat, sampleRate, 0, null);
            if (samples_convert_ctx == null) {
                throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
            } else if ((ret = swr_init(samples_convert_ctx)) < 0) {
                throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
            }
            samples_channels = audioChannels;
            samples_format = inputFormat;
            samples_rate = sampleRate;
        }

        for (int i = 0; samples != null && i < samples.length; i++) {
            samples_in[i].position(samples_in[i].position() * inputDepth).
                    limit((samples_in[i].position() + inputSize) * inputDepth);
        }
        while (true) {
            int inputCount = (int)Math.min(samples != null ? (samples_in[0].limit() - samples_in[0].position()) / (inputChannels * inputDepth) : 0, Integer.MAX_VALUE);
            int outputCount = (int)Math.min((samples_out[0].limit() - samples_out[0].position()) / (outputChannels * outputDepth), Integer.MAX_VALUE);
            inputCount = Math.min(inputCount, (outputCount * sampleRate + audio_c.sample_rate() - 1) / audio_c.sample_rate());
            for (int i = 0; samples != null && i < samples.length; i++) {
                planePtr.put(i, samples_in[i]);
            }
            for (int i = 0; i < samples_out.length; i++) {
                planePtr2.put(i, samples_out[i]);
            }
            if ((ret = swr_convert(samples_convert_ctx, planePtr2, outputCount, planePtr, inputCount)) < 0) {
                throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
            } else if (ret == 0) {
                break;
            }
            for (int i = 0; samples != null && i < samples.length; i++) {
                samples_in[i].position(samples_in[i].position() + inputCount * inputChannels * inputDepth);
            }
            for (int i = 0; i < samples_out.length; i++) {
                samples_out[i].position(samples_out[i].position() + ret * outputChannels * outputDepth);
            }

            if (samples == null || samples_out[0].position() >= samples_out[0].limit()) {
                writeSamples(audio_input_frame_size);
            }
        }
        return samples != null ? frame.key_frame() != 0 : writeFrame((AVFrame)null);

        }
    }

    private void writeSamples(int nb_samples) throws Exception {
        if (samples_out == null || samples_out.length == 0) {
            return;
        }

        frame.nb_samples(nb_samples);
        avcodec_fill_audio_frame(frame, audio_c.channels(), audio_c.sample_fmt(), samples_out[0], (int)samples_out[0].position(), 0);
        for (int i = 0; i < samples_out.length; i++) {
            int linesize = 0;
            if (samples_out[0].position() > 0 && samples_out[0].position() < samples_out[0].limit()) {
                // align the end of the buffer to a 32-byte boundary as sometimes required by FFmpeg
                linesize = ((int)samples_out[i].position() + 31) & ~31;
            } else {
                linesize = (int)Math.min(samples_out[i].limit(), Integer.MAX_VALUE);
            }

            frame.data(i, samples_out[i].position(0));
            frame.linesize(i, linesize);
        }
        frame.format(audio_c.sample_fmt());
        frame.quality(audio_c.global_quality());
        frame.sample_rate(audio_c.sample_rate());
        frame.channels(audio_c.channels());
        frame.channel_layout(av_get_default_channel_layout(audio_c.channels()));
        writeFrame(frame);
    }


    private boolean writeFrame(AVFrame frame) throws Exception {
        int ret;

        if ((ret = avcodec_send_frame(audio_c, frame)) < 0 && frame != null) {
            throw new Exception("avcodec_send_frame() error " + ret + ": Error sending an audio frame for encoding.");
        }
        if (frame != null) {
            frame.pts(frame.pts() + frame.nb_samples()); // magic required by libvorbis and webm
        }

        /* if zero size, it means the image was buffered */
        got_audio_packet[0] = 0;
        while (ret >= 0) {
            av_new_packet(audio_pkt, audio_outbuf_size);
            ret = avcodec_receive_packet(audio_c, audio_pkt);
            if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
                av_packet_unref(audio_pkt);
                break;
            } else if (ret < 0) {
                av_packet_unref(audio_pkt);
                throw new Exception("avcodec_receive_packet() error " + ret + ": Error during audio encoding.");
            }
            got_audio_packet[0] = 1;

            if (audio_pkt.pts() != AV_NOPTS_VALUE) {
                audio_pkt.pts(av_rescale_q(audio_pkt.pts(), audio_c.time_base(), audio_st.time_base()));
            }
            if (audio_pkt.dts() != AV_NOPTS_VALUE) {
                audio_pkt.dts(av_rescale_q(audio_pkt.dts(), audio_c.time_base(), audio_st.time_base()));
            }
            audio_pkt.flags(audio_pkt.flags() | AV_PKT_FLAG_KEY);
            audio_pkt.stream_index(audio_st.index());

            /* write the compressed frame in the media file */
            writePacket(AVMEDIA_TYPE_AUDIO, audio_pkt);

            if (frame == null) {
                // avoid infinite loop with buggy codecs on flush
                break;
            }
        }

        return got_audio_packet[0] != 0;
    }

    private void writePacket(int mediaType, AVPacket avPacket) throws Exception {
    	AVStream avStream = null;
    	String mediaTypeStr = "unsupported media stream type";
    	if (mediaType == AVMEDIA_TYPE_VIDEO) {
    		avStream = video_st;
    		mediaTypeStr = "video";
    	}
    	else if (mediaType == AVMEDIA_TYPE_AUDIO) {
    		avStream = audio_st;
    		mediaTypeStr = "audio";
    	}

        synchronized (oc) {
            int ret;
            if (interleaved && avStream != null) {
                if ((ret = av_interleaved_write_frame(oc, avPacket)) < 0) {
                    av_packet_unref(avPacket);
                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved " + mediaTypeStr + " packet.");
                }
            } else {
                if ((ret = av_write_frame(oc, avPacket)) < 0) {
                    av_packet_unref(avPacket);
                    throw new Exception("av_write_frame() error " + ret + " while writing " + mediaTypeStr + " packet.");
                }
            }
        }
        av_packet_unref(avPacket);
    }

	public String getFilename() {
		return filename;
	}
	public AVFrame getPicture() {
		return picture;
	}

	public void debugSetStarted(boolean started) {
		this.started = started;
	}
}