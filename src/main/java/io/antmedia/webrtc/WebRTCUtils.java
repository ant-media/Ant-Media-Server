package io.antmedia.webrtc;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebRTCUtils {
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
