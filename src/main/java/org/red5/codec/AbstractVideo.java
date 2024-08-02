package org.red5.codec;

import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;

public class AbstractVideo implements IVideoStreamCodec, IoConstants {

    /** Current timestamp for the stored keyframe */
    protected int keyframeTimestamp;

    /**
     * Storage for key frames
     */
    protected final CopyOnWriteArrayList<FrameData> keyframes = new CopyOnWriteArrayList<>();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean canDropFrames() {
        return false;
    }

    @Override
    public boolean canHandleData(IoBuffer data) {
        return false;
    }

    @Override
    public boolean addData(IoBuffer data) {
        return false;
    }

    @Override
    public boolean addData(IoBuffer data, int timestamp) {
        return false;
    }

    @Override
    public IoBuffer getDecoderConfiguration() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public IoBuffer getKeyframe() {
        if (keyframes.isEmpty()) {
            return null;
        }
        return keyframes.get(0).getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public FrameData[] getKeyframes() {
        return keyframes.toArray(new FrameData[0]);
    }

    @Override
    public int getNumInterframes() {
        return 0;
    }

    @Override
    public FrameData getInterframe(int idx) {
        return null;
    }

}
