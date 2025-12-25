package io.antmedia.webrtc;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.IceServer.Builder;

public class WebRTCUtils {

	private static Logger logger = LoggerFactory.getLogger(WebRTCUtils.class);

	public static void parseIceServers(String iceServersConfig, List<IceServer> iceServers) {
		JSONArray iceServersArray = getIceServersJSONArray(iceServersConfig);
		if (iceServersArray != null) {
			for (Object iceServerObj : iceServersArray) {
				JSONObject iceServerJson = (JSONObject) iceServerObj;
				String urls = (String) iceServerJson.get("urls");
				String username = (String) iceServerJson.get("username");
				String credential = (String) iceServerJson.get("credential");

				if (urls != null) {
					Builder builder = IceServer.builder(urls);
					if (username != null && !username.isEmpty()) {
						builder.setUsername(username);
					}
					if (credential != null && !credential.isEmpty()) {
						builder.setPassword(credential);
					}
					iceServers.add(builder.createIceServer());
				}
			}
		}
	}

	public static JSONArray getIceServersJSONArray(String iceServersConfig) {
		if (iceServersConfig != null && !iceServersConfig.isEmpty()) {
			try {
				JSONParser parser = new JSONParser();
				return (JSONArray) parser.parse(iceServersConfig);
			} catch (Exception e) {
				logger.error("Error parsing iceServers: {}", iceServersConfig);
			}
		}
		return null;
	}

    public static boolean validateSdpMediaPayloads(String sdp) {
        String[] lines = sdp.split("\\r?\\n");
        Map<String, Set<Integer>> mediaPayloads = new HashMap<>();
        Set<Integer> allRtpMaps = new HashSet<>();

        Pattern mLinePattern = Pattern.compile("^m=(\\w+)\\s+\\d+\\s+UDP/TLS/RTP/SAVPF\\s+(.+)$");
        Pattern rtpmapPattern = Pattern.compile("^a=rtpmap:(\\d+)\\s+.+$");

        String currentMedia = null;
        for (String line : lines) {
            line = line.trim();
            if(line.contains("opus") && !line.contains("opus/48000/2")){
                System.out.println("Invalid SDP: opus should be opus/48000/2 ");
                return false;
            }

            Matcher m = mLinePattern.matcher(line);
            if (m.find()) {
                currentMedia = m.group(1); 
                String payloads = m.group(2).trim();
                Set<Integer> pts = new HashSet<>();
                for (String pt : payloads.split("\\s+")) {
                    try {
                        pts.add(Integer.parseInt(pt));
                    } catch (NumberFormatException ignored) {}
                }
                mediaPayloads.put(currentMedia, pts);
                continue;
            }
            Matcher rtp = rtpmapPattern.matcher(line);
            if (rtp.find()) {
                int pt = Integer.parseInt(rtp.group(1));
                allRtpMaps.add(pt);
            }
        }

        // Validate audio and video payloads
        for (String media : List.of("audio", "video")) {
            if (mediaPayloads.containsKey(media)) {
                for (int pt : mediaPayloads.get(media)) {
                    if (!allRtpMaps.contains(pt)) {
                        System.out.println("Invalid SDP : Missing rtpmap for " + media + " payload type: " + pt);
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
