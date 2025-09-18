package io.antmedia.cluster;

import io.antmedia.AppSettings;
import io.antmedia.filter.TokenFilterManager;

/**
 * Class full of static method used in construction of {@link RequestInfo} request
 */
public final class RequestInfoUtils {

	private RequestInfoUtils() {
		// Here just to prevent accidental creation of class
	}

	public static int defineType(String requestURI) {
		if(requestURI.endsWith(IHttpLiveStreamValve.M3U8_EXTENSION)) {
			if (requestURI.contains("/ll-hls")) {
				return RequestInfo.LL_HLS_REQUEST;
			}

			return RequestInfo.HLS_REQUEST;
		}

		if(requestURI.endsWith(IHttpLiveStreamValve.TS_EXTENSION)) {
			return RequestInfo.TS_REQUEST;
		}

		if(requestURI.endsWith(IHttpLiveStreamValve.MPD_EXTENSION)) {
			return RequestInfo.DASH_REQUEST;
		}

		if(requestURI.endsWith(IHttpLiveStreamValve.M4S_EXTENSION)) {
			return RequestInfo.DASH_SEGMENT;
		}

		if (requestURI.endsWith("mp4") && requestURI.contains("__")) {
			// Request dash with DRM. Make sure to check for 'mp4' and not '.mp4',
			// since sometimes extension might be .fmp4 and not just .mp4
			return RequestInfo.DASH_SEGMENT;
		}

		if((requestURI.endsWith("."+AppSettings.PREVIEW_FORMAT_JPG)
				|| requestURI.endsWith("." + AppSettings.PREVIEW_FORMAT_PNG)
				|| requestURI.endsWith("." + AppSettings.PREVIEW_FORMAT_WEBP))
				&& requestURI.contains("/previews/")
		) {
			return RequestInfo.PREVIEW_REQUEST;
		}

		return RequestInfo.OTHER_REQUEST;
	}

	public static String defineStreamId(String requestURI, int type, String suffixFormat) {
		// For DASH streams, the stream ID can be in the path
		// especially for DRM streams with subfolders (e.g., /streams/{streamId}/).
		if (type == RequestInfo.DASH_REQUEST || type == RequestInfo.DASH_SEGMENT) {
			String pathBasedId = extractStreamIdFromPath(requestURI);
			if (pathBasedId != null) {
				return pathBasedId;
			}
		}

		if (type == RequestInfo.LL_HLS_REQUEST) {
			return extractStreamIdFromLLHls(requestURI);
		}

		// For all other cases, or as a fallback, parse the ID from the filename.
		return extractStreamIdFromFilename(requestURI, type, suffixFormat);
	}

	private static String extractStreamIdFromLLHls(String requestURI) {
		int streamsFolderIndex = findStreamsFolder(requestURI);
		if (streamsFolderIndex == -1) {
			return null;
		}

		int llhlsIndex = requestURI.indexOf("/ll-hls/", streamsFolderIndex);
		if (llhlsIndex == -1) {
			return null;
		}

		int streamIdStartIndex = llhlsIndex + "/ll-hls/".length();
		int streamIdEndIndex = requestURI.indexOf("/", streamIdStartIndex);
		if (streamIdEndIndex == -1) {
			return null;
		}

		return requestURI.substring(streamIdStartIndex, streamIdEndIndex);
	}

	/**
	 * Try to extract the stream ID from the request URI path.
	 * This method is primarily used for DASH/DRM requests, where the stream ID is part of the directory structure
	 * (e.g., /streams/{streamId}/...).
	 * @return The extracted stream ID, or null if it cannot be determined from the path.
	 */
	private static String extractStreamIdFromPath(String requestURI) {
		int streamsFolderIndex = findStreamsFolder(requestURI);
		if (streamsFolderIndex == -1) {
			return null;
		}

		String folder = requestURI.contains("/streams/") ? "/streams/" : "/previews/";
		int streamIdStartIndex = streamsFolderIndex + folder.length();
		if (streamIdStartIndex >= requestURI.length()) {
			return null;
		}

		int streamIdEndIndex = requestURI.indexOf("/", streamIdStartIndex);
		if (streamIdEndIndex != -1) {
			return requestURI.substring(streamIdStartIndex, streamIdEndIndex);
		}

		return null;
	}

	/**
	 * Extracts the stream ID from the request URI's filename.
	 * @param suffixFormat The suffix format for adaptive HLS streams.
	 * @return The extracted stream ID or NULL if not found
	 */
	private static String extractStreamIdFromFilename(String requestURI, int type, String suffixFormat) {
		if (type == RequestInfo.HLS_REQUEST || type == RequestInfo.TS_REQUEST) {
			return TokenFilterManager.getStreamId(requestURI, suffixFormat);
		}

		int startIndex = requestURI.lastIndexOf("/");
		int endIndex = -1;

		switch (type) {
			case RequestInfo.PREVIEW_REQUEST:
				endIndex = requestURI.lastIndexOf("." + AppSettings.PREVIEW_FORMAT_PNG);
				if (endIndex == -1) {
					endIndex = requestURI.lastIndexOf("." + AppSettings.PREVIEW_FORMAT_JPG);
				}
				if (endIndex == -1) {
					endIndex = requestURI.lastIndexOf("." + AppSettings.PREVIEW_FORMAT_WEBP);
				}
				break;
			case RequestInfo.DASH_REQUEST:
				endIndex = requestURI.lastIndexOf(".mpd");
				break;
			case RequestInfo.DASH_SEGMENT:
				endIndex = requestURI.lastIndexOf("__");
				if (endIndex == -1) {
					endIndex = requestURI.lastIndexOf("_");
				}
				break;
			default:
				// No filename parsing for other types
				break;
		}

		if (endIndex != -1 && startIndex != -1) {
			return requestURI.substring(startIndex + 1, endIndex);
		}

		return null;
	}

	public static String defineScopeName(String requestURI, int type) {
		int streamsFolderIndex = findStreamsFolder(requestURI);
		if (streamsFolderIndex > 0) {
			if (type == RequestInfo.LL_HLS_REQUEST) {
				int webappsStart = requestURI.indexOf("webapps/");
				webappsStart = webappsStart < 0 ? 1 : webappsStart + 8;

				return requestURI.substring(webappsStart, streamsFolderIndex);
			}

			return requestURI.substring(1, streamsFolderIndex);
		}

		//scope name is between first slash and second slash
		int startIndex = requestURI.indexOf("/");
		if (startIndex == 0) {
			//start index should 0
			int endIndex = requestURI.indexOf("/", startIndex+1);
			if (endIndex != -1) {
				return requestURI.substring(startIndex+1, endIndex);
			}
		}
		return null;
	}

	private static int findStreamsFolder(String requestURI) {
		int streamsFolderIndex = requestURI.indexOf("/streams/");
		if (streamsFolderIndex != -1) {
			return streamsFolderIndex;
		}

		return requestURI.indexOf("/previews/");
	}

	/**
	 * Will basically remove '/drm/' from path,
	 * and convert it to path as if it was normal dash stream, and not DRM one.
	 * This method is used so that other methods that extract streamId, appName, etc... would work properly
	 */
	public static String preProcessDRMRequestURI(String reqURI) {
		int drm = reqURI.indexOf("/drm/");
		if (drm == -1) {
			return reqURI;
		}

		// remove '/drm/' part so that other processing work properly
		String finalUrl = reqURI.substring(0, drm) + reqURI.substring(drm + 4);
		int lastSlash = finalUrl.lastIndexOf('/');
		if (finalUrl.endsWith("master.mpd")) {
			int streamStart = finalUrl.lastIndexOf('/', lastSlash - 1) + 1;
			String streamName = finalUrl.substring(streamStart, lastSlash);
			finalUrl = finalUrl.substring(0, lastSlash + 1) + streamName + ".mpd";
		} else if (finalUrl.endsWith("master.m3u8")) {
			finalUrl = finalUrl.substring(0, lastSlash) + ".m3u8";
		}

		return finalUrl;
	}
} 