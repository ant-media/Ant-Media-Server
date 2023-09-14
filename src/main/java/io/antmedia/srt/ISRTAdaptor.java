package io.antmedia.srt;

public interface ISRTAdaptor {

    String PUBLISH_TYPE = "SRT";

    public enum BeanName {
        SRT_ADAPTOR("srtAdaptor");


        private String originName;

        BeanName(String name) {
            this.originName =  name;
        }

        @Override
        public String toString() {
            return this.originName;
        }

    }
    public boolean stopSRTStream(String streamId);



}
