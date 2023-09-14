package io.antmedia.srt;


/**
 * 
 * This is a mock SRT Adaptor.
 * Real implementation is in Enterprise project
 *
 */
public class SRTAdaptor implements ISRTAdaptor{
    @Override
    public boolean stopSRTStream(String streamId) {
        return false;
    }
}
