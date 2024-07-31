/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AudioCodec;
import org.red5.codec.VideoCodec;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.flv.FLVHeader;
import org.red5.io.flv.IFLV;
import org.red5.io.object.Deserializer;
import org.red5.io.utils.IOUtils;
import org.red5.media.processor.IPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Writer is used to write the contents of a FLV file
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FLVWriter implements ITagWriter {

    private static Logger log = LoggerFactory.getLogger(FLVWriter.class);

    /**
     * Length of the flv header in bytes
     */
    private final static int HEADER_LENGTH = 9;

    /**
     * Length of the flv tag in bytes
     */
    private final static int TAG_HEADER_LENGTH = 11;

    /**
     * For now all recorded streams carry a stream id of 0.
     */
    private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff), (byte) (0 & 0xff) };

    /**
     * Executor for tasks within this instance
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * FLV object
     */
    private static IFLV flv;

    /**
     * Number of bytes written
     */
    private volatile long bytesWritten;

    /**
     * Position in file
     */
    private int offset;

    /**
     * Position in file
     */
    private int timeOffset;

    /**
     * Id of the audio codec used.
     */
    private volatile int audioCodecId = -1;

    /**
     * Id of the video codec used.
     */
    private volatile int videoCodecId = -1;

    /**
     * If audio configuration data has been written
     */
    private AtomicBoolean audioConfigWritten = new AtomicBoolean(false);

    /**
     * If video configuration data has been written
     */
    private AtomicBoolean videoConfigWritten = new AtomicBoolean(false);

    /**
     * Sampling rate
     */
    private volatile int soundRate;

    /**
     * Size of each audio sample
     */
    private volatile int soundSize;

    /**
     * Mono (0) or stereo (1) sound
     */
    private volatile boolean soundType;

    /**
     * Are we appending to an existing file?
     */
    private boolean append;

    /**
     * Duration of the file.
     */
    private int duration;

    /**
     * Size of video data
     */
    private int videoDataSize = 0;

    /**
     * Size of audio data
     */
    private int audioDataSize = 0;

    /**
     * Flv output destination.
     */
    private SeekableByteChannel fileChannel;

    /**
     * Destination to which stream data is stored without an flv header.
     */
    private SeekableByteChannel dataChannel;

    // path to the original file passed to the writer
    private String filePath;

    private final Semaphore lock = new Semaphore(1, true);

    // the size of the last tag written, which includes the tag header length
    private volatile int lastTagSize;

    // to be executed after flv is finalized
    private LinkedList<IPostProcessor> postProcessors;

    // state of flv finalization
    private AtomicBoolean finalized = new AtomicBoolean(false);

    // iso 8601 date of recording
    private String recordedDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);

    // metadata
    private Map<String, ?> meta;

    // offset in previous flv to skip when appending
    private long appendOffset = HEADER_LENGTH + 4L;

    /**
     * Creates writer implementation with for a given file
     * 
     * @param filePath
     *            path to existing file
     */
    public FLVWriter(String filePath) {
        this.filePath = filePath;
        log.debug("Writing to: {}", filePath);
        try {
            createDataFile();
        } catch (Exception e) {
            log.error("Failed to create FLV writer", e);
        }
    }

    /**
     * Creates writer implementation with for a given file
     *
     * @param repair
     *        repair the .ser file
     *
     * @param filePath
     *            path to existing file
     */
    public FLVWriter(boolean repair, String filePath) {
        this.filePath = filePath;
        try {
            createRepairDataFile();
        } catch (Exception e) {
            log.error("Failed to create FLV writer", e);
        }
    }

    /**
     * Creates writer implementation with given file and flag indicating whether or not to append.
     *
     * FLV.java uses this constructor so we have access to the file object
     *
     * @param file
     *            File output stream
     * @param append
     *            true if append to existing file
     */
    public FLVWriter(File file, boolean append) {
        this(file.toPath(), append);
    }

    /**
     * Creates writer implementation with given file and flag indicating whether or not to append.
     *
     * FLV.java uses this constructor so we have access to the file object
     *
     * @param path
     *            File output path
     * @param append
     *            true if append to existing file
     */
    public FLVWriter(Path path, boolean append) {
        filePath = path.toFile().getAbsolutePath();
        this.append = append;
        log.debug("Writing to: {} {}", filePath, flv);
        try {
            if (append) {
                // get previous metadata
                meta = getMetaData(path, 5);
                if (meta != null) {
                    for (Entry<String, ?> entry : meta.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if ("duration".equals(key)) {
                            if (value instanceof Double) {
                                Double d = (((Double) value) * 1000d);
                                duration = d.intValue();
                            } else {
                                duration = Integer.valueOf((String) value) * 1000;
                            }
                        } else if ("recordeddate".equals(key)) {
                            recordedDate = String.valueOf(value);
                        }
                    }
                    // if we are appending get the duration as offset
                    timeOffset = duration;
                    log.debug("Duration: {}", duration);
                }
                // move / rename previous flv
                Files.move(path, path.resolveSibling(path.toFile().getName().replace(".flv", ".old")));
                log.debug("Previous flv renamed");
            }
            createDataFile();
        } catch (Exception e) {
            log.error("Failed to create FLV writer", e);
        }
    }

    private Map<String, ?> getMetaData(Path path, int maxTags) throws IOException {
        Map<String, ?> meta = null;
        // attempt to read the metadata
        SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
        long size = channel.size();
        log.debug("Channel open: {} size: {} position: {}", channel.isOpen(), size, channel.position());
        if (size > 0L) {
            // skip flv signature 4b, flags 1b, data offset 4b (9b), prev tag size (4b)
            channel.position(appendOffset);
            // flv tag header size 11b
            ByteBuffer dst = ByteBuffer.allocate(11);
            do {
                int read = channel.read(dst);
                if (read > 0) {
                    dst.flip();
                    byte tagType = (byte) (dst.get() & 31); // 1
                    int bodySize = IOUtils.readUnsignedMediumInt(dst); // 3
                    int timestamp = IOUtils.readExtendedMediumInt(dst); // 4
                    int streamId = IOUtils.readUnsignedMediumInt(dst); // 3
                    log.debug("Data type: {} timestamp: {} stream id: {} body size: {}", new Object[] { tagType, timestamp, streamId, bodySize });
                    if (tagType == ITag.TYPE_METADATA) {
                        ByteBuffer buf = ByteBuffer.allocate(bodySize);
                        read = channel.read(buf);
                        if (read > 0) {
                            buf.flip();
                            // construct the meta
                            IoBuffer ioBuf = IoBuffer.wrap(buf);
                            Input input = new Input(ioBuf);
                            String metaType = Deserializer.deserialize(input, String.class);
                            log.debug("Metadata type: {}", metaType);
                            meta = Deserializer.deserialize(input, Map.class);
                            input = null;
                            ioBuf.clear();
                            ioBuf.free();
                            if (meta.containsKey("duration")) {
                                appendOffset = channel.position() + 4L;
                                break;
                            }
                        }
                        buf.compact();
                    }
                    // advance beyond prev tag size
                    channel.position(channel.position() + 4L);
                    //int prevTagSize = dst.getInt(); // 4
                    //log.debug("Previous tag size: {} {}", prevTagSize, (bodySize - 11));
                    dst.compact();
                }
            } while (--maxTags > 0); // read up-to "max" tags looking for duration
            channel.close();
        }
        return meta;
    }

    /**
     * Writes the header bytes
     *
     * @throws IOException
     *             Any I/O exception
     */
    @Override
    public void writeHeader() throws IOException {
        // create a buffer
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
        // instance an flv header
        FLVHeader flvHeader = new FLVHeader();
        flvHeader.setFlagAudio(audioCodecId != -1 ? true : false);
        flvHeader.setFlagVideo(videoCodecId != -1 ? true : false);
        // write the flv header in the buffer
        flvHeader.write(buf);
        // the final version of the file will go here
        createOutputFile();
        // write header to output channel
        bytesWritten = fileChannel.write(buf);
        assert ((HEADER_LENGTH + 4) - bytesWritten == 0);
        log.debug("Header size: {} bytes written: {}", (HEADER_LENGTH + 4), bytesWritten);
        buf.clear();
        buf = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeTag(ITag tag) throws IOException {
        // a/v config written flags
        boolean onWrittenSetVideoFlag = false, onWrittenSetAudioFlag = false;
        try {
            lock.acquire();
            /*
             * Tag header = 11 bytes |-|---|----|---| 0 = type 1-3 = data size 4-7 = timestamp 8-10 = stream id (always 0) Tag data = variable bytes Previous tag = 4 bytes (tag header size +
             * tag data size)
             */
            log.trace("writeTag: {}", tag);
            long prevBytesWritten = bytesWritten;
            log.trace("Previous bytes written: {}", prevBytesWritten);
            // skip tags with no data
            int bodySize = tag.getBodySize();
            log.trace("Tag body size: {}", bodySize);
            // verify previous tag size stored in incoming tag
            int previousTagSize = tag.getPreviousTagSize();
            if (previousTagSize != lastTagSize) {
                // use the last tag size
                log.trace("Incoming previous tag size: {} does not match current value for last tag size: {}", previousTagSize, lastTagSize);
            }
            // ensure that the channel is still open
            if (dataChannel != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Current file position: {}", dataChannel.position());
                }
                // get the data type
                byte dataType = tag.getDataType();
                // when tag is ImmutableTag which is in red5-server-common.jar, tag.getBody().reset() will throw InvalidMarkException because 
                // ImmutableTag.getBody() returns a new IoBuffer instance everytime.
                IoBuffer tagBody = tag.getBody();
                // set a var holding the entire tag size including the previous tag length
                int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
                // create a buffer for this tag
                ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
                // get the timestamp
                int timestamp = tag.getTimestamp() + timeOffset;
                // allow for empty tag bodies
                byte[] bodyBuf = null;
                if (bodySize > 0) {
                    // create an array big enough
                    bodyBuf = new byte[bodySize];
                    // put the bytes into the array
                    tagBody.get(bodyBuf);
                    // get the audio or video codec identifier
                    if (dataType == ITag.TYPE_AUDIO) {
                        audioDataSize += bodySize;
                        if (audioCodecId == -1) {
                            int id = bodyBuf[0] & 0xff; // must be unsigned
                            audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
                            log.debug("Audio codec id: {}", audioCodecId);
                            // if aac use defaults
                            if (audioCodecId == AudioCodec.AAC.getId()) {
                                log.trace("AAC audio type");
                                // Flash Player ignores	these values and extracts the channel and sample rate data encoded in the AAC bit stream
                                soundRate = 44100;
                                soundSize = 16;
                                soundType = true;
                                // this is aac data, so a config chunk should be written before any media data
                                if (bodyBuf[1] == 0) {
                                    // when this config is written set the flag
                                    onWrittenSetAudioFlag = true;
                                } else {
                                    // reject packet since config hasnt been written yet
                                    log.debug("Rejecting AAC data since config has not yet been written");
                                    return false;
                                }
                            } else if (audioCodecId == AudioCodec.SPEEX.getId()) {
                                log.trace("Speex audio type");
                                soundRate = 5500; // actually 16kHz
                                soundSize = 16;
                                soundType = false; // mono
                            } else {
                                switch ((id & ITag.MASK_SOUND_RATE) >> 2) {
                                    case ITag.FLAG_RATE_5_5_KHZ:
                                        soundRate = 5500;
                                        break;
                                    case ITag.FLAG_RATE_11_KHZ:
                                        soundRate = 11000;
                                        break;
                                    case ITag.FLAG_RATE_22_KHZ:
                                        soundRate = 22000;
                                        break;
                                    case ITag.FLAG_RATE_44_KHZ:
                                        soundRate = 44100;
                                        break;
                                }
                                log.debug("Sound rate: {}", soundRate);
                                switch ((id & ITag.MASK_SOUND_SIZE) >> 1) {
                                    case ITag.FLAG_SIZE_8_BIT:
                                        soundSize = 8;
                                        break;
                                    case ITag.FLAG_SIZE_16_BIT:
                                        soundSize = 16;
                                        break;
                                }
                                log.debug("Sound size: {}", soundSize);
                                // mono == 0 // stereo == 1
                                soundType = (id & ITag.MASK_SOUND_TYPE) > 0;
                                log.debug("Sound type: {}", soundType);
                            }
                        } else if (!audioConfigWritten.get() && audioCodecId == AudioCodec.AAC.getId()) {
                            // this is aac data, so a config chunk should be written before any media data
                            if (bodyBuf[1] == 0) {
                                // when this config is written set the flag
                                onWrittenSetAudioFlag = true;
                            } else {
                                // reject packet since config hasnt been written yet
                                return false;
                            }
                        }
                    } else if (dataType == ITag.TYPE_VIDEO) {
                        videoDataSize += bodySize;
                        if (videoCodecId == -1) {
                            int id = bodyBuf[0] & 0xff; // must be unsigned
                            videoCodecId = id & ITag.MASK_VIDEO_CODEC;
                            log.debug("Video codec id: {}", videoCodecId);
                            if (videoCodecId == VideoCodec.AVC.getId()) {
                                // this is avc/h264 data, so a config chunk should be written before any media data
                                if (bodyBuf[1] == 0) {
                                    // when this config is written set the flag
                                    onWrittenSetVideoFlag = true;
                                } else {
                                    // reject packet since config hasnt been written yet
                                    log.debug("Rejecting AVC data since config has not yet been written");
                                    return false;
                                }
                            }
                        } else if (!videoConfigWritten.get() && videoCodecId == VideoCodec.AVC.getId()) {
                            // this is avc/h264 data, so a config chunk should be written before any media data
                            if (bodyBuf[1] == 0) {
                                // when this config is written set the flag
                                onWrittenSetVideoFlag = true;
                            } else {
                                // reject packet since config hasnt been written yet
                                log.debug("Rejecting AVC data since config has not yet been written");
                                return false;
                            }
                        }
                    }
                }
                // Data Type
                IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
                // Body Size - Length of the message. Number of bytes after StreamID to end of tag 
                // (Equal to length of the tag - 11) 
                IOUtils.writeMediumInt(tagBuffer, bodySize); //3
                // Timestamp
                IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
                // Stream id
                tagBuffer.put(DEFAULT_STREAM_ID); //3
                // get the body if we have one
                if (bodyBuf != null) {
                    tagBuffer.put(bodyBuf);
                }
                // store new previous tag size
                lastTagSize = TAG_HEADER_LENGTH + bodySize;
                // we add the tag size
                tagBuffer.putInt(lastTagSize);
                // flip so we can process from the beginning
                tagBuffer.flip();
                // write the tag
                dataChannel.write(tagBuffer);
                bytesWritten = dataChannel.position();
                if (log.isTraceEnabled()) {
                    log.trace("Tag written, check value: {} (should be 0)", (bytesWritten - prevBytesWritten) - totalTagSize);
                }
                tagBuffer.clear();
                // update the duration
                log.debug("Current duration: {} timestamp: {}", duration, timestamp);
                duration = Math.max(duration, timestamp);
                // validate written amount
                if ((bytesWritten - prevBytesWritten) != totalTagSize) {
                    log.debug("Not all of the bytes appear to have been written, prev-current: {}", (bytesWritten - prevBytesWritten));
                }
                return true;
            } else {
                // throw an exception and let them know the cause
                throw new IOException("FLV write channel has been closed", new ClosedChannelException());
            }
        } catch (InterruptedException e) {
            log.warn("Exception acquiring lock", e);
        } finally {
            // update the file information
            updateInfoFile();
            // mark config written flags
            if (onWrittenSetAudioFlag && audioConfigWritten.compareAndSet(false, true)) {
                log.trace("Audio configuration written");
            } else if (onWrittenSetVideoFlag && videoConfigWritten.compareAndSet(false, true)) {
                log.trace("Video configuration written");
            }
            // release lock
            lock.release();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeTag(byte dataType, IoBuffer data) throws IOException {
        if (timeOffset == 0) {
            timeOffset = (int) System.currentTimeMillis();
        }
        try {
            lock.acquire();
            /*
             * Tag header = 11 bytes |-|---|----|---| 0 = type 1-3 = data size 4-7 = timestamp 8-10 = stream id (always 0) Tag data = variable bytes Previous tag = 4 bytes (tag header size +
             * tag data size)
             */
            if (log.isTraceEnabled()) {
                log.trace("writeTag - type: {} data: {}", dataType, data);
            }
            long prevBytesWritten = bytesWritten;
            log.trace("Previous bytes written: {}", prevBytesWritten);
            // skip tags with no data
            int bodySize = data.limit();
            log.debug("Tag body size: {}", bodySize);
            // ensure that the channel is still open
            if (dataChannel != null) {
                log.debug("Current file position: {}", dataChannel.position());
                // set a var holding the entire tag size including the previous tag length
                int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
                // create a buffer for this tag
                ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
                // Data Type
                IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
                // Body Size - Length of the message. Number of bytes after StreamID to end of tag 
                // (Equal to length of the tag - 11) 
                IOUtils.writeMediumInt(tagBuffer, bodySize); //3
                // Timestamp
                int timestamp = (int) (System.currentTimeMillis() - timeOffset);
                IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
                // Stream id
                tagBuffer.put(DEFAULT_STREAM_ID); //3
                log.trace("Tag buffer (after tag header) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
                // get the body if we have one
                if (data.hasArray()) {
                    tagBuffer.put(data.array());
                    log.trace("Tag buffer (after body) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
                }
                // store new previous tag size
                lastTagSize = TAG_HEADER_LENGTH + bodySize;
                // we add the tag size
                tagBuffer.putInt(lastTagSize);
                log.trace("Tag buffer (after prev tag size) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
                // flip so we can process from the beginning
                tagBuffer.flip();
                if (log.isDebugEnabled()) {
                    //StringBuilder sb = new StringBuilder();
                    //HexDump.dumpHex(sb, tagBuffer.array());
                    //log.debug("\n{}", sb);
                }
                // write the tag
                dataChannel.write(tagBuffer);
                bytesWritten = dataChannel.position();
                if (log.isTraceEnabled()) {
                    log.trace("Tag written, check value: {} (should be 0)", (bytesWritten - prevBytesWritten) - totalTagSize);
                }
                tagBuffer.clear();
                // update the duration
                duration = Math.max(duration, timestamp);
                log.debug("Writer duration: {}", duration);
                // validate written amount
                if ((bytesWritten - prevBytesWritten) != totalTagSize) {
                    log.debug("Not all of the bytes appear to have been written, prev-current: {}", (bytesWritten - prevBytesWritten));
                }
                return true;
            } else {
                // throw an exception and let them know the cause
                throw new IOException("FLV write channel has been closed", new ClosedChannelException());
            }
        } catch (InterruptedException e) {
            log.warn("Exception acquiring lock", e);
        } finally {
            // update the file information
            updateInfoFile();
            // release lock
            lock.release();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean writeStream(byte[] b) {
        try {
            dataChannel.write(ByteBuffer.wrap(b));
            return true;
        } catch (IOException e) {
            log.error("", e);
        }
        return false;
    }

    /**
     * Create the stream output file; the flv itself.
     * 
     * @throws IOException
     */
    private void createOutputFile() throws IOException {
        this.fileChannel = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Create the stream data file.
     * 
     * @throws IOException
     */
    private void createDataFile() throws IOException {
        // temporary data file for storage of stream data
        Path path = Paths.get(filePath + ".ser");
        if (Files.deleteIfExists(path)) {
            log.debug("Previous flv data file existed and was removed");
        }
        this.dataChannel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ);
    }

    /**
     * Create the stream data file for repair.
     *
     * @throws IOException
     */
    private void createRepairDataFile() throws IOException {
        // temporary data file for storage of stream data
        Path path = Paths.get(filePath + ".ser");

        // Create a data channel that is read-only
        this.dataChannel = Files.newByteChannel(path, StandardOpenOption.READ);
    }

    /**
     * Write "onMetaData" tag to the file.
     *
     * @param duration
     *            Duration to write in milliseconds.
     * @param videoCodecId
     *            Id of the video codec used while recording.
     * @param audioCodecId
     *            Id of the audio codec used while recording.
     * @throws IOException
     *             if the tag could not be written
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void writeMetadataTag(double duration, int videoCodecId, int audioCodecId) throws IOException, InterruptedException, ExecutionException {
        log.debug("writeMetadataTag - duration: {} video codec: {} audio codec: {}", new Object[] { duration, videoCodecId, audioCodecId });
        IoBuffer buf = IoBuffer.allocate(256);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> params = new HashMap<>();
        if (meta != null) {
            params.putAll(meta);
        }
        params.putIfAbsent("server", "Red5");
        params.putIfAbsent("recordeddate", recordedDate);
        params.put("duration", (Number) duration);
        if (log.isDebugEnabled()) {
            log.debug("Stored duration: {}", params.get("duration"));
        }
        if (videoCodecId != -1) {
            params.put("videocodecid", (videoCodecId == 7 ? "avc1" : videoCodecId));
            if (videoDataSize > 0) {
                params.put("videodatarate", 8 * videoDataSize / 1024 / duration); //from bytes to kilobits
            }
        } else {
            // place holder
            params.put("novideocodec", 0);
        }
        if (audioCodecId != -1) {
            params.put("audiocodecid", (audioCodecId == 10 ? "mp4a" : audioCodecId));
            if (audioCodecId == AudioCodec.AAC.getId()) {
                params.put("audiosamplerate", 44100);
                params.put("audiosamplesize", 16);
            } else if (audioCodecId == AudioCodec.SPEEX.getId()) {
                params.put("audiosamplerate", 16000);
                params.put("audiosamplesize", 16);
            } else {
                params.put("audiosamplerate", soundRate);
                params.put("audiosamplesize", soundSize);
            }
            params.put("stereo", soundType);
            if (audioDataSize > 0) {
                params.put("audiodatarate", 8 * audioDataSize / 1024 / duration); //from bytes to kilobits		
            }
        } else {
            // place holder
            params.put("noaudiocodec", 0);
        }
        // this is actual only supposed to be true if the last video frame is a keyframe
        params.put("canSeekToEnd", true);
        out.writeMap(params);
        buf.flip();
        int bodySize = buf.limit();
        log.debug("Metadata size: {}", bodySize);
        // set a var holding the entire tag size including the previous tag length
        int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
        // create a buffer for this tag
        ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
        // get the timestamp
        int timestamp = 0;
        // create an array big enough
        byte[] bodyBuf = new byte[bodySize];
        // put the bytes into the array
        buf.get(bodyBuf);
        // Data Type
        IOUtils.writeUnsignedByte(tagBuffer, ITag.TYPE_METADATA); //1
        // Body Size - Length of the message. Number of bytes after StreamID to end of tag 
        // (Equal to length of the tag - 11) 
        IOUtils.writeMediumInt(tagBuffer, bodySize); //3
        // Timestamp
        IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
        // Stream id
        tagBuffer.put(DEFAULT_STREAM_ID); //3
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after tag header) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // get the body
        tagBuffer.put(bodyBuf);
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after body) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // we add the tag size
        tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after prev tag size) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // flip so we can process from the beginning
        tagBuffer.flip();
        // write the tag
        if (log.isTraceEnabled()) {
            log.trace("Writing metadata starting at position: {}", bytesWritten);
        }
        // add to the total bytes written
        bytesWritten += fileChannel.write(tagBuffer);
        if (log.isTraceEnabled()) {
            log.trace("Updated position: {}", bytesWritten);
        }
        tagBuffer.clear();
        buf.clear();
    }

    /**
     * Finalizes the FLV file.
     * 
     * @return bytes transferred
     */
    private long finalizeFlv() {
        long bytesTransferred = 0L;
        if (!finalized.get()) {
            log.debug("Finalizing {}", filePath);
            try {
                // read file info if it exists
                File tmpFile = new File(filePath + ".info");
                if (tmpFile.exists()) {
                    int[] info = readInfoFile(tmpFile);
                    if (audioCodecId == -1 && info[0] > 0) {
                        audioCodecId = info[0];
                    }
                    if (videoCodecId == -1 && info[1] > 0) {
                        videoCodecId = info[1];
                    }
                    if (duration == 0 && info[2] > 0) {
                        duration = info[2];
                    }
                    if (audioDataSize == 0 && info[3] > 0) {
                        audioDataSize = info[3];
                    }
                    if (soundRate == 0 && info[4] > 0) {
                        soundRate = info[4];
                    }
                    if (soundSize == 0 && info[5] > 0) {
                        soundSize = info[5];
                    }
                    if (!soundType && info[6] > 0) {
                        soundType = true;
                    }
                    if (videoDataSize == 0 && info[7] > 0) {
                        videoDataSize = info[7];
                    }
                } else {
                    log.debug("Flv info file not found");
                }
                tmpFile = null;
                // write the file header
                writeHeader();
                log.debug("Pos post header: {}", fileChannel.position());
                // write the metadata with the final duration
                writeMetadataTag(duration * 0.001d, videoCodecId, audioCodecId);
                log.debug("Pos post meta: {}", fileChannel.position());
                // create a transfer buffer
                ByteBuffer dst = ByteBuffer.allocate(1024);
                // when appending, read original stream data first and put it at the front
                if (append) {
                    Path prevFlv = Paths.get(filePath.replace(".flv", ".old"));
                    if (Files.exists(prevFlv)) {
                        log.debug("Found previous flv: {} offset: {}", prevFlv, appendOffset);
                        SeekableByteChannel prevChannel = Files.newByteChannel(prevFlv, StandardOpenOption.READ);
                        // skip the flv header, prev tag size, and possibly metadata
                        prevChannel.position(appendOffset);
                        int read = -1, wrote;
                        boolean showfirsttag = true;
                        do {
                            read = prevChannel.read(dst);
                            log.trace("Read: {} bytes", read);
                            if (read > 0) {
                                dst.flip();
                                // inspect the byte to make sure its a valid type
                                if (showfirsttag) {
                                    showfirsttag = false;
                                    dst.mark();
                                    log.debug("Tag type: {}", (dst.get() & 31));
                                    dst.reset();
                                }
                                wrote = fileChannel.write(dst);
                                log.trace("Wrote: {} bytes", wrote);
                                bytesTransferred += wrote;
                            }
                            dst.compact();
                        } while (read > 0);
                        dst.clear();
                        prevChannel.close();
                        // remove the previous flv
                        Files.deleteIfExists(prevFlv);
                        log.debug("Previous FLV bytes written: {} final position: {}", (bytesWritten + bytesTransferred), fileChannel.position());
                    } else {
                        log.warn("Previous flv to be appended was not found: {}", prevFlv);
                    }
                }
                // get starting position of the channel where latest stream data was written
                long pos = dataChannel.position();
                log.trace("Data available: {} bytes", pos);
                // set the data file the beginning 
                dataChannel.position(0L);
                // transfer / write data file into final flv
                int read = -1, wrote;
                do {
                    read = dataChannel.read(dst);
                    log.trace("Read: {} bytes", read);
                    if (read > 0) {
                        dst.flip();
                        wrote = fileChannel.write(dst);
                        log.trace("Wrote: {} bytes", wrote);
                        bytesTransferred += wrote;
                    }
                    dst.compact();
                } while (read > 0);
                dst.clear();
                dataChannel.close();
                // get final position
                long length = fileChannel.position();
                // close the file
                fileChannel.close();
                // close and remove the ser file if write was successful
                if (bytesTransferred > 0) {
                    if (!Files.deleteIfExists(Paths.get(filePath + ".info"))) {
                        log.warn("FLV info file not deleted");
                    }
                    if (!Files.deleteIfExists(Paths.get(filePath + ".ser"))) {
                        log.warn("FLV serial file not deleted");
                    }
                }
                log.debug("FLV bytes written: {} final position: {}", (bytesWritten + bytesTransferred), length);
            } catch (Exception e) {
                log.warn("Finalization of flv file failed; new finalize job will be spawned", e);
            } finally {
                finalized.compareAndSet(false, true);
                // check for post processors that may be available
                if (FLVWriter.flv != null) {
                    LinkedList<Class<IPostProcessor>> writePostProcessors = ((FLV) FLVWriter.flv).getWritePostProcessors();
                    if (writePostProcessors != null) {
                        for (Class<IPostProcessor> postProcessor : writePostProcessors) {
                            try {
                                addPostProcessor(postProcessor.getDeclaredConstructor().newInstance());
                            } catch (Exception e) {
                                log.warn("Post processor: {} instance creation failed", postProcessor, e);
                            }
                        }
                    }
                }
                // run post process
                if (postProcessors != null) {
                    for (IPostProcessor postProcessor : postProcessors) {
                        log.debug("Execute: {}", postProcessor);
                        try {
                            // set properties that the post processor requires or may require
                            postProcessor.init(filePath);
                            // execute and block
                            executor.submit(postProcessor).get();
                        } catch (Throwable t) {
                            log.warn("Exception during post process on: {}", filePath, t);
                        }
                    }
                    postProcessors.clear();
                } else {
                    log.debug("No post processors configured");
                }
            }
        } else {
            log.trace("Finalization already completed");
        }
        return bytesTransferred;
    }

    /**
     * Read flv file information from pre-finalization file.
     * 
     * @param tmpFile
     * @return array containing audio codec id, video codec id, and duration
     */
    private static int[] readInfoFile(File tmpFile) {
        int[] info = new int[8];
        try (RandomAccessFile infoFile = new RandomAccessFile(tmpFile, "r")) {
            // audio codec id
            info[0] = infoFile.readInt();
            // video codec id
            info[1] = infoFile.readInt();
            // duration
            info[2] = infoFile.readInt();
            // audio data size
            info[3] = infoFile.readInt();
            // audio sound rate
            info[4] = infoFile.readInt();
            // audio sound size
            info[5] = infoFile.readInt();
            // audio type
            info[6] = infoFile.readInt();
            // video data size
            info[7] = infoFile.readInt();
        } catch (Exception e) {
            log.warn("Exception reading flv file information data", e);
        }
        return info;
    }

    /**
     * Write or update flv file information into the pre-finalization file.
     */
    private void updateInfoFile() {
        try (RandomAccessFile infoFile = new RandomAccessFile(filePath + ".info", "rw")) {
            infoFile.writeInt(audioCodecId);
            infoFile.writeInt(videoCodecId);
            infoFile.writeInt(duration);
            // additional props
            infoFile.writeInt(audioDataSize);
            infoFile.writeInt(soundRate);
            infoFile.writeInt(soundSize);
            infoFile.writeInt(soundType ? 1 : 0);
            infoFile.writeInt(videoDataSize);
        } catch (Exception e) {
            log.warn("Exception writing flv file information data", e);
        }
    }

    /**
     * Ends the writing process, then merges the data file with the flv file header and metadata.
     */
    @Override
    public void close() {
        log.debug("close");
        // spawn a thread to finish up our flv writer work
        boolean locked = false;
        try {
            // try to get a lock within x time
            locked = lock.tryAcquire(500L, TimeUnit.MILLISECONDS);
            if (locked) {
                finalizeFlv();
            }
        } catch (InterruptedException e) {
            log.warn("Exception acquiring lock", e);
        } finally {
            if (locked) {
                lock.release();
            }
            if (executor != null && !executor.isTerminated()) {
                executor.shutdown();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPostProcessor(IPostProcessor postProcessor) {
        if (postProcessors == null) {
            postProcessors = new LinkedList<>();
        }
        postProcessors.add(postProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public IStreamableFile getFile() {
        return flv;
    }

    /**
     * Setter for FLV object
     *
     * @param flv
     *            FLV source
     *
     */
    public static void setFLV(IFLV flv) {
        FLVWriter.flv = flv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOffset() {
        return offset;
    }

    /**
     * Setter for offset
     *
     * @param offset
     *            Value to set for offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }

    public void setVideoCodecId(int videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    public void setAudioCodecId(int audioCodecId) {
        this.audioCodecId = audioCodecId;
    }

    public void setSoundRate(int soundRate) {
        this.soundRate = soundRate;
    }

    public void setSoundSize(int soundSize) {
        this.soundSize = soundSize;
    }

    public void setSoundType(boolean soundType) {
        this.soundType = soundType;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setVideoDataSize(int videoDataSize) {
        this.videoDataSize = videoDataSize;
    }

    public void setAudioDataSize(int audioDataSize) {
        this.audioDataSize = audioDataSize;
    }

    private final class FLVFinalizer implements Runnable {

        @Override
        public void run() {
            log.debug("Finalizer run");
            try {
                // delete the incomplete file if it exists
                boolean deleted = Files.deleteIfExists(Paths.get(filePath));
                log.info("Deleted ({}) incomplete file: {}", deleted, filePath);
                // quick sleep, cheap delay
                Thread.sleep(1000L);
            } catch (Exception e) {
                log.error("Error on cleanup of flv", e);
            }
            // attempt to finalize the flv
            finalizeFlv();
            log.debug("Finalizer exit");
        }

    }

    /**
     * Allows repair of flv files if .info and .ser files still exist.
     * 
     * @param path
     *            path to .ser file
     * @param audioId
     *            audio codec id
     * @param videoId
     *            video codec id
     * @return true if conversion was successful
     * @throws InterruptedException
     *             Exception on interruption
     */
    public static boolean repair(String path, Integer audioId, Integer videoId) throws InterruptedException {
        boolean result = false;
        FLVWriter writer = null;
        log.debug("Serial file path: " + path);
        System.out.println("Serial file path: " + path);
        if (path.endsWith(".ser")) {
            File ser = new File(path);
            if (ser.exists() && ser.canRead()) {
                ser = null;
                String flvPath = path.substring(0, path.lastIndexOf('.'));
                log.debug("Flv file path: " + flvPath);
                System.out.println("Flv file path: " + flvPath);
                // check for .info and if it does not exist set dummy data
                File inf = new File(flvPath + ".info");
                if (inf.exists() && inf.canRead()) {
                    inf = null;
                    // create a writer
                    writer = new FLVWriter(true, flvPath);
                } else {
                    log.debug("Info file was not found or could not be read, using dummy data");
                    System.err.println("Info file was not found or could not be read, using dummy data");
                    // create a writer
                    writer = new FLVWriter(true, flvPath);
                    int acid = audioId == null ? 11 : audioId, vcid = videoId == null ? 7 : videoId;
                    writer.setAudioCodecId(acid); // default: speex
                    writer.setVideoCodecId(vcid); // default: h.264
                    writer.setDuration(Integer.MAX_VALUE);
                    writer.setSoundRate(16000);
                    writer.setSoundSize(16);
                }
            } else {
                log.error("Serial file was not found or could not be read");
                System.err.println("Serial file was not found or could not be read");
            }
        } else {
            log.error("Provide the path to your .ser file");
            System.err.println("Serial file was not found or could not be read");
        }
        if (writer != null) {
            // spawn a flv finalizer
            Future<?> future = writer.submit(writer.new FLVFinalizer());
            try {
                // get result / blocking
                future.get();
                log.debug("File repair completed");
                System.out.println("File repair completed");
                result = true;
            } catch (Exception e) {
                log.warn("Exception while finalizing: {}", path, e);
            }
        }
        return result;
    }

    /**
     * Submits a finalizer internally.
     * 
     * @param flvFinalizer
     * @return Future representing task
     */
    private Future<?> submit(FLVFinalizer flvFinalizer) {
        if (executor != null && !executor.isTerminated()) {
            return executor.submit(flvFinalizer);
        }
        return null;
    }

    /**
     * Exposed to allow repair of flv files if .info and .ser files still exist.
     * 
     * @param args
     *            0: path to .ser file 1: audio codec id 2: video codec id
     * @throws InterruptedException
     *             Exception on interruption
     */
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args[0] == null) {
            System.err.println("Provide the path to your .ser file");
        } else {
            repair(args[0], args.length > 1 && args[1] != null ? Integer.valueOf(args[1]) : null, args.length > 2 && args[2] != null ? Integer.valueOf(args[2]) : null);
        }
        System.exit(0);
    }

}
