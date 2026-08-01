package io.antmedia.whip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * Inspects the media sections of a WHIP offer to find out which tracks the publisher really sends.
 *
 * WHIP clients have no way to declare their tracks other than the SDP itself, so the flags cannot be
 * trusted to come from the query parameters. If the server assumes audio while the publisher sends
 * video only, the ingest waits for an audio clock that never advances and the video is not playable.
 */
public class SdpMediaInspector {

	public static final String AUDIO = "audio";
	public static final String VIDEO = "video";

	private static final String MEDIA_PREFIX = "m=";
	private static final String ATTRIBUTE_PREFIX = "a=";

	private static final String SENDRECV = "sendrecv";
	private static final String SENDONLY = "sendonly";
	private static final String RECVONLY = "recvonly";
	private static final String INACTIVE = "inactive";

	protected static Logger logger = LoggerFactory.getLogger(SdpMediaInspector.class);

	private SdpMediaInspector() {
		//utility class
	}

	/**
	 * Tells if the given media type is received from the publisher according to the SDP.
	 *
	 * It's true if there is at least one media section of this type that is not rejected(port 0) and
	 * that sends media. Direction is read from the media section, it falls back to the session level
	 * attribute and then to sendrecv which is the default in RFC 4566.
	 *
	 * @param sdp the offer, it may be null or blank because the caller does not validate it
	 * @param mediaType {@link #AUDIO} or {@link #VIDEO}
	 * @return true if the type is sent by the publisher, true as well if the SDP is not available to not change the flag
	 */
	public static boolean isMediaEnabled(String sdp, String mediaType) {
		if (StringUtils.isBlank(sdp)) {
			//nothing to inspect, leave the decision to the caller
			return true;
		}

		String sessionDirection = null;
		String currentDirection = null;
		boolean inRequestedSection = false;
		boolean sectionAccepted = false;
		boolean beforeFirstSection = true;

		for (String line : sdp.split("\\r?\\n")) {
			line = line.trim();

			if (line.startsWith(MEDIA_PREFIX)) {
				//a new media section starts, so the previous one can be evaluated
				if (inRequestedSection && sectionAccepted && isSending(direction(currentDirection, sessionDirection))) {
					return true;
				}

				beforeFirstSection = false;
				inRequestedSection = isMediaType(line, mediaType);
				sectionAccepted = inRequestedSection && !isRejected(line);
				currentDirection = null;
			}
			else if (line.startsWith(ATTRIBUTE_PREFIX)) {
				String attribute = line.substring(ATTRIBUTE_PREFIX.length());
				if (isDirection(attribute)) {
					if (beforeFirstSection) {
						sessionDirection = attribute;
					}
					else {
						currentDirection = attribute;
					}
				}
			}
		}
		//evaluate the last section because there is no media line after it
		boolean isEnabled = inRequestedSection && sectionAccepted && isSending(direction(currentDirection, sessionDirection));
		logger.info("Checking if media is enabled for {} in sdp, isEnabled: {}", mediaType, isEnabled);
		return isEnabled;
	}

	/**
	 * m=&lt;media&gt; &lt;port&gt; &lt;proto&gt; &lt;fmt&gt; ...
	 */
	private static boolean isMediaType(String mediaLine, String mediaType) {
		String media = mediaLine.substring(MEDIA_PREFIX.length()).trim();
		return media.equals(mediaType) || media.startsWith(mediaType + " ");
	}

	/**
	 * Port 0 means the media section is rejected/disabled.
	 */
	private static boolean isRejected(String mediaLine) {
		String[] fields = mediaLine.substring(MEDIA_PREFIX.length()).trim().split("\\s+");
		if (fields.length < 2) {
			//malformed media line, don't disable the track because of it
			return false;
		}

		//port may be written as <port>/<number of ports>
		String port = StringUtils.substringBefore(fields[1], "/");
		try {
			return Integer.parseInt(port) == 0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isDirection(String attribute) {
		return SENDRECV.equals(attribute) || SENDONLY.equals(attribute)
				|| RECVONLY.equals(attribute) || INACTIVE.equals(attribute);
	}

	private static String direction(String mediaDirection, String sessionDirection) {
		if (mediaDirection != null) {
			return mediaDirection;
		}
		return sessionDirection != null ? sessionDirection : SENDRECV;
	}

	/**
	 * Server receives media only if the publisher sends it.
	 */
	private static boolean isSending(String direction) {
		return SENDRECV.equals(direction) || SENDONLY.equals(direction);
	}

}
