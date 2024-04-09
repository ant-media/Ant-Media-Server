package io.antmedia.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class LoggerUtils {
	
	public static final String EVENT_PUBLISH_ENDED = "publishEnded";
	
	
	public static final String STREAM_ID_FIELD = "streamId";

    private LoggerUtils() {
        //Hide public constructor
    }

    private static final Logger analyticsLogger = LoggerFactory.getLogger("analytics");


	public static final String EVENT_PUBLISH_STARTED = "publishStarted";


	public static final String PROTOCOL_FIELD = "protocol";


	public static final String STREAM_NAME_FIELD = "streamName";


	public static final String BITRATE_FIELD = "bitrate";


	public static final String HEIGHT_FIELD = "height";


	public static final String WIDTH_FIELD = "width";


	public static final String VIDEO_CODEC_FIELD = "videoCodec";


	public static final String AUDIO_CODEC_FIELD = "audioCodec";


	public static final String SUBSCRIBER_ID_FIELD = "subscriberId";


	public static final String EVENT_PLAY_STARTED = "playStarted";


	public static final String TOTAL_BYTES_FIELD = "totalBytes";


	public static final String EVENT_VIEWER_STATS = "viewerStats";


	public static final String CLIENT_IP_FIELD = "clientIP";


	public static final String USER_AGENT_FIELD = "userAgent";


	public static final String DURATION_MS_FIELD = "durationMs";


	public static final String EVENT_PUBLISH_STATS = "publishStats";


	public static final String EVENT_PLAY_ENDED = "playEnded";


	public static final String EVENT_KEY_FRAME_STATS = "keyFrameStats";


	public static final String KEY_FRAME_DIFFERENCE = "keyFrameDiffMs";


	public static final String KEY_FRAME_IN_LAST_MINUTE = "keyFrameInLastMinute";

   
    static Gson gson = new Gson();
    
    public static void logAnalyticsFromServer(String appName, String eventName, String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be in pairs");
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", eventName);
        jsonObject.addProperty("app", appName);

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            jsonObject.addProperty(keyValuePairs[i], keyValuePairs[i + 1]);
        }

        jsonObject.addProperty("time",System.currentTimeMillis());
    	jsonObject.addProperty("logSource", "server");

        var log = jsonObject.toString();
        analyticsLogger.info(log);
    }
    
    public static void logAnalyticsFromClient(String appName, Object logObject, String clientIP) {
    	
    	 
    	JsonObject jsonObject = (JsonObject) gson.toJsonTree(logObject);
    	jsonObject.addProperty("app", appName);
    	jsonObject.addProperty("time", System.currentTimeMillis());
    	jsonObject.addProperty("clientIP", clientIP); 
    	jsonObject.addProperty("logSource", "client");
    	var log = jsonObject.toString();
        analyticsLogger.info(log);
    	
    }
    
    
}
