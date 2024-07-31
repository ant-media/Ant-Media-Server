package io.antmedia.eRTMP;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AbstractVideo;
import org.red5.codec.VideoCodec;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.ExVideoPacketType;
import org.red5.server.net.rtmp.event.VideoData.VideoFourCC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HEVCVideo extends AbstractVideo {

	static final String CODEC_NAME = "HEVC";

    private FrameData decoderConfiguration;
    
    private static Logger log = LoggerFactory.getLogger(HEVCVideo.class);
    
    
    private final CopyOnWriteArrayList<FrameData> interframes = new CopyOnWriteArrayList<>();

    private final AtomicInteger numInterframes = new AtomicInteger(0);
    
    private boolean bufferInterframes = false;


	public HEVCVideo() {
		this.reset();
	}

	@Override
	public String getName() {
		return CODEC_NAME;
	}

	@Override
	public boolean canDropFrames() {
		return true;
	}

	@Override
	public void reset() {
		decoderConfiguration = new FrameData();
		softReset();
	}
	
	 // reset all except decoder configuration
    private void softReset() {
        keyframes.clear();
        interframes.clear();
        numInterframes.set(0);
    }
    
    public boolean canHandleData(IoBuffer data) {
        boolean result = false;
        if (data.limit() > 0) {

        	int firstByte = data.get() & 0xff;
        	
        	boolean exVideoHeader = ((firstByte & VideoData.MASK_EX_VIDEO_TAG_HEADER) >> 7) == 1;
        	int frameTypeLocal = 0;
        	if (exVideoHeader) {
        		frameTypeLocal = (firstByte & VideoData.MASK_EX_VIDEO_FRAME_TYPE) >> 4;
        	}
        	
        	int videoPacketType = (firstByte & VideoData.MASK_EX_VIDEO_PACKET_TYPE);
			ExVideoPacketType[] values = ExVideoPacketType.values();
			ExVideoPacketType exVideoPacketType = values[videoPacketType];
        	
            result = ((data.get() & 0x0f) == VideoCodec.AVC.getId());
        	byte[] fourcc = new byte[4];
			data.get(fourcc);
			
			
			VideoFourCC videoFourCc = VideoData.findFourCcByValue(VideoFourCC.makeFourCc(new String(fourcc)));
			
		
			if (videoFourCc != null && videoFourCc == VideoFourCC.HEVC_FOURCC) {
				result = true;
			}
			
			
            data.rewind();
        }
        return result;
    }
    
    @Override
    public boolean addData(IoBuffer data) {
        return addData(data, (keyframeTimestamp + 1));
    }
    
    @Override
    public boolean addData(IoBuffer data, int timestamp) 
    {
        if (data.hasRemaining()) {
            // mark
            int start = data.position();
            // get frame type
            
            int firstByte = data.get() & 0xff;
        	// Check if this is an extended video tag header
        	int frameTypeLocal = (firstByte & VideoData.MASK_EX_VIDEO_FRAME_TYPE) >> 4;
        	int videoPacketType = (firstByte & VideoData.MASK_EX_VIDEO_PACKET_TYPE);
			
			ExVideoPacketType exVideoPacketType =  ExVideoPacketType.values()[videoPacketType];
			
			if (exVideoPacketType == ExVideoPacketType.SEQUENCE_START) {
				// body contains a configuration record to start the sequence. See ISO/IEC
				// 14496-15, 8.3.3.1.2 for the description of HEVCDecoderConfigurationRecord
				//hevcHeader = [HEVCDecoderConfigurationRecord]
				data.rewind();
				decoderConfiguration.setData(data);
				softReset();
			}
			else if (exVideoPacketType == ExVideoPacketType.CODED_FRAMES) {
				// See ISO/IEC 14496-12, 8.15.3 for an explanation of composition times.
				// The offset in an FLV file is always in milliseconds.
				
				//compositionTimeOffset = SI24
				// Body contains one or more NALUs; full frames are required
				//hevcData = [HevcCodedData]
				
				//TODO: implement this compositionTimeOffset
				
				
			}
			else if (exVideoPacketType == ExVideoPacketType.CODED_FRAMESX) {
				// Body contains one or more NALUs; full frames are required
				// hevcData = [HevcCodedData]
				
				
			}
			
			if (frameTypeLocal == FLAG_FRAMETYPE_KEYFRAME) {
				  data.rewind();
				  if (timestamp != keyframeTimestamp) {
                      //log.trace("New keyframe");
                      // new keyframe
                      keyframeTimestamp = timestamp;
                      // if its a new keyframe, clear keyframe and interframe collections
                      softReset();
                  }
                  // store keyframe
                  keyframes.add(new FrameData(data));
			}
			else
			{
                // rewind
                data.rewind();
                try {
                    int lastInterframe = numInterframes.getAndIncrement();
                    if (lastInterframe < interframes.size()) {
                        interframes.get(lastInterframe).setData(data);
                    } else {
                        interframes.add(new FrameData(data));
                    }
                } catch (Throwable e) {
                    log.error("Failed to buffer interframe", e);
                }
			}
            // go back to where we started
            data.position(start);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public IoBuffer getDecoderConfiguration() {
        return decoderConfiguration.getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumInterframes() {
        return numInterframes.get();
    }

    /** {@inheritDoc} */
    @Override
    public FrameData getInterframe(int index) {
        if (index < numInterframes.get()) {
            return interframes.get(index);
        }
        return null;
    }

    public boolean isBufferInterframes() {
        return bufferInterframes;
    }

    public void setBufferInterframes(boolean bufferInterframes) {
        this.bufferInterframes = bufferInterframes;
    }

}
