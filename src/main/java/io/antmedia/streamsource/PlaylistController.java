package io.antmedia.streamsource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Plays the items of a playlist one after another. Every item is an ordinary {@link StreamFetcher}
 * registered under the playlist's own stream id, so the rest of the server keeps seeing one stream
 * with one status. This class only decides which item comes next and when the playlist is over.
 *
 * The next item starts from the previous one's STOPPED transition, never alongside it.
 *
 * Everything runs on {@link StreamFetcherManager}'s shared context.
 */
public class PlaylistController {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);

	private static final String NOT_RUNNING_MESSAGE = "Playlist is not running for stream:";

	private static final int URL_CHECK_TIMEOUT_MS = 5000;

	/** One entry per playing playlist. Its presence is what "this playlist is running" means. */
	private final Map<String, PlaylistSession> sessions = new ConcurrentHashMap<>();

	private final StreamFetcherManager manager;
	private final Vertx vertx;
	private final Context context;
	private final AppSettings appSettings;

	public PlaylistController(StreamFetcherManager manager, Vertx vertx, Context context, AppSettings appSettings) {
		this.manager = manager;
		this.vertx = vertx;
		this.context = context;
		this.appSettings = appSettings;
	}

	private static class PlaylistSession {

		/** Index an explicit skip asked for, waiting for the current item to stop. */
		Integer pendingIndex;

		/** Items that ended without playing anything since the last one that did. */
		int failuresInPass;

		/** Full passes over the list that played nothing at all. */
		int failedPasses;

		/** True while an item is being started, which includes waiting for its url check. */
		boolean starting;
	}

	/**
	 * Starts the playlist at its current index. Returns as soon as the start is accepted, the first
	 * item is opened on the shared context.
	 */
	public Result startPlaylist(Broadcast playlist) {
		String streamId = playlist.getStreamId();
		List<PlayListItem> items = playlist.getPlayListItemList();

		if (items == null || items.isEmpty()) {
			logger.warn("There is no item to play in playlist:{}", streamId);
			return new Result(false, streamId, "There is no item to play in the playlist:" + streamId);
		}

		//isStreamRunning covers the other nodes of a cluster, the session is the local mutual exclusion:
		//whoever creates it owns the playlist until it ends
		if (manager.isStreamRunning(playlist) || sessions.putIfAbsent(streamId, new PlaylistSession()) != null) {
			logger.warn("Playlist is already running for stream:{}", streamId);
			return new Result(false, streamId, "Playlist is already running for stream:" + streamId);
		}

		int startIndex = playlist.getCurrentPlayIndex();
		if (startIndex < 0 || startIndex >= items.size()) {
			logger.warn("Resetting current play index to 0 because it is out of range for playlist:{}", streamId);
			startIndex = 0;
		}

		int index = startIndex;
		context.runOnContext(v -> playIndex(streamId, index));

		return new Result(true, streamId, "Playlist is started");
	}

	/** Stops the item that is playing and marks the playlist finished, keeping its current index. */
	public Result stopPlaylist(String streamId) {
		if (StringUtils.isBlank(streamId)) {
			return new Result(false, "Stream id is not defined");
		}

		logger.info("Stopping playlist for stream:{}", streamId);

		boolean running = isRunning(streamId);
		endPlaylist(streamId, false);

		return new Result(running, streamId, running ? "Playlist is stopped" : NOT_RUNNING_MESSAGE + streamId);
	}

	/**
	 * Plays the item at index, or the one after the current item when index is negative.
	 * Asking for the next item while the last one of non-looping playlist plays, will finish playlist!
	 */
	public Result playItem(String streamId, int index) {
		Broadcast playlist = manager.getDatastore().get(streamId);
		if (playlist == null || !AntMediaApplicationAdapter.PLAY_LIST.equals(playlist.getType())) {
			return new Result(false, streamId, "There is no playlist for stream id:" + streamId);
		}

		List<PlayListItem> items = playlist.getPlayListItemList();
		if (items == null || items.isEmpty()) {
			return new Result(false, streamId, "There is no item to play in the playlist:" + streamId);
		}

		if (!isRunning(streamId)) {
			return new Result(false, streamId, NOT_RUNNING_MESSAGE + streamId);
		}

		if (index >= items.size()) {
			return new Result(false, streamId, "Index " + index + " is out of the playlist:" + streamId);
		}

		int target = index >= 0 ? index : nextIndex(playlist);
		if (target < 0) {
			endPlaylist(streamId, true);
			return new Result(true, streamId, "Playlist is finished, there was no next item to play");
		}

		context.runOnContext(v -> skipTo(streamId, target));
		return new Result(true, streamId, "Playing item:" + target);
	}

	public boolean isRunning(String streamId) {
		return sessions.containsKey(streamId);
	}

	/**
	 * Marks every playing playlist finished before the application goes down.
	 */
	void shutdown() {
		for (String streamId : sessions.keySet()) {
			endPlaylist(streamId, false);
		}
	}

	/**
	 * Callback when playlist item reached it's end (terminal state).
	 * <p>
	 * Called form shared context in {@link StreamFetcherManager}
	 *
	 * Ids this controller does not own are ignored.
	 */
	void onItemStopped(StreamFetcher item) {
		String streamId = item.getStreamId();
		PlaylistSession session = sessions.get(streamId);
		if (session == null) {
			return;
		}

		try {
			advance(streamId, session, item.isPublished());
		}
		catch (Exception e) {
			//a database blip here would otherwise leave a playlist that nothing plays and nothing ever
			//restarts, which is the failure this whole rewrite exists to remove. Ending it is honest
			logger.error("Playlist:{} could not pick its next item so it is finished. {}", streamId, ExceptionUtils.getStackTrace(e));
			endPlaylist(streamId, false);
		}
	}

	private void advance(String streamId, PlaylistSession session, boolean itemPlayed) {
		if (manager.isServerShuttingDown()) {
			//the shutdown flag can be set before shutdown() gets here, so finish the playlist properly
			//instead of dropping the session and leaving the database saying it is still playing
			logger.info("Playlist will not play the next item because the server is shutting down, streamId:{}", streamId);
			endPlaylist(streamId, false);
			return;
		}

		Integer pending = session.pendingIndex;
		session.pendingIndex = null;
		if (pending != null) {
			//somebody asked for this item explicitly, so whatever the previous one did is history
			session.failuresInPass = 0;
			session.failedPasses = 0;
			playIndex(streamId, pending);
			return;
		}

		if (itemPlayed) {
			session.failuresInPass = 0;
			session.failedPasses = 0;
		}
		else {
			session.failuresInPass++;
		}

		playNext(streamId, session);
	}

	/**
	 * Moves on from the item the playlist is sitting on, or ends the playlist when there is nowhere
	 * left to go. Both a finished item and one whose url could not be reached come through here.
	 */
	private void playNext(String streamId, PlaylistSession session) {
		Broadcast playlist = manager.getDatastore().get(streamId);
		if (playlist == null) {
			logger.info("Playlist is deleted so the next item is not played, streamId:{}", streamId);
			sessions.remove(streamId);
			return;
		}

		List<PlayListItem> items = playlist.getPlayListItemList();
		if (items == null || items.isEmpty()) {
			logger.info("Playlist has no items left to play so it is finished, streamId:{}", streamId);
			endPlaylist(streamId, true);
			return;
		}

		int next = nextIndex(playlist);
		if (next < 0) {
			logger.info("Playlist reached the end of its items and looping is disabled, streamId:{}", streamId);
			endPlaylist(streamId, true);
			return;
		}

		if (session.failuresInPass < items.size()) {
			playIndex(streamId, next);
		}
		else {
			retryAfterFailedPass(streamId, session, next);
		}
	}

	/**
	 * A whole pass over the list played nothing. Without this a playlist of dead urls would cycle as
	 * fast as ffmpeg can refuse them, so the next pass waits and an operator can bound the retries.
	 */
	private void retryAfterFailedPass(String streamId, PlaylistSession session, int next) {
		session.failuresInPass = 0;
		session.failedPasses++;

		int maxPasses = appSettings.getStreamFetcherMaxRetryAttempts();
		if (maxPasses >= 0 && session.failedPasses > maxPasses) {
			logger.error("Giving up on playlist:{}, no item could be played in {} passes over the list", streamId, session.failedPasses);
			endPlaylist(streamId, true);
			return;
		}

		long delayMs = Math.max(1, appSettings.getStreamFetcherRetryDelayMs());
		logger.warn("No item of playlist:{} could be played in the last pass, trying again in {}ms", streamId, delayMs);

		//nothing is on air during the wait, so say it is still trying rather than still playing
		writePlaylistStatus(streamId, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, null);

		vertx.setTimer(delayMs, t -> {
			//the playlist may have been stopped or restarted while this timer was armed
			if (sessions.get(streamId) == session) {
				playIndex(streamId, next);
			}
		});
	}

	/**
	 * Starts the item at index. The list is read fresh, so edits made while playing take effect here.
	 */
	private void playIndex(String streamId, int index) {
		PlaylistSession session = sessions.get(streamId);
		if (session == null) {
			return;
		}

		if (session.starting || manager.getStreamFetcher(streamId) != null) {
			//a skip can beat a pending retry to it. Two items under one id is the one thing this class
			//must never do, so it is worth refusing outright rather than unpicking how they got here
			logger.warn("Not starting item:{} of playlist:{} because one is already starting or playing", index, streamId);
			return;
		}

		try {
			Broadcast playlist = manager.getDatastore().get(streamId);
			List<PlayListItem> items = playlist != null ? playlist.getPlayListItemList() : null;

			if (items == null || index < 0 || index >= items.size()) {
				logger.warn("Playlist item:{} does not exist anymore so the playlist is finished, streamId:{}", index, streamId);
				endPlaylist(streamId, true);
				return;
			}

			//move the stored index before the check, it is what a failed check advances from
			PlayListItem item = items.get(index);
			writePlaylistStatus(streamId, null, index);

			session.starting = true;
			vertx.executeBlocking(() -> isWorthTrying(item.getStreamUrl()), false)
					.onComplete(ar -> onUrlChecked(streamId, session, index, item, Boolean.TRUE.equals(ar.result())));
		}
		catch (Exception e) {
			logger.error("Playlist:{} could not start item:{} so it is finished. {}", streamId, index, ExceptionUtils.getStackTrace(e));
			endPlaylist(streamId, false);
		}
	}

	/**
	 * False only when an http item answers badly, so a file that has been deleted is glided past
	 * instead of waiting for ffmpeg to work it out. Anything else cannot be asked, rtsp and srt are
	 * not urls {@link URI} understands, and is worth trying. Blocks, so keep it off the context.
	 */
	private static boolean isWorthTrying(String url) {
		if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
			return true;
		}

		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
			//without these a host that accepts and then stalls blocks the calling thread forever
			connection.setConnectTimeout(URL_CHECK_TIMEOUT_MS);
			connection.setReadTimeout(URL_CHECK_TIMEOUT_MS);

			int responseCode = connection.getResponseCode();
			if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MOVED_PERM) {
				return true;
			}

			logger.warn("Playlist item url {} responded:{}", url, responseCode);
			return false;
		}
		catch (IOException | IllegalArgumentException e) {
			//IllegalArgumentException is URI.create on a url somebody typed wrong
			logger.warn("Playlist item url {} cannot be reached. {}", url, e.getMessage());
			return false;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/** Back on the shared context once the url check answered. */
	private void onUrlChecked(String streamId, PlaylistSession session, int index, PlayListItem item, boolean worthTrying) {
		session.starting = false;

		if (sessions.get(streamId) != session) {
			logger.info("Playlist was stopped while item:{} was being checked, streamId:{}", index, streamId);
			return;
		}

		Integer pending = session.pendingIndex;
		if (pending != null) {
			session.pendingIndex = null;
			logger.info("Item:{} was skipped while its url was being checked, playing item:{} instead, streamId:{}", index, pending, streamId);
			playIndex(streamId, pending);
			return;
		}

		try {
			if (worthTrying) {
				startItem(streamId, index, item);
			}
			else {
				session.failuresInPass++;
				playNext(streamId, session);
			}
		}
		catch (Exception e) {
			logger.error("Playlist:{} could not move on from item:{} so it is finished. {}", streamId, index, ExceptionUtils.getStackTrace(e));
			endPlaylist(streamId, false);
		}
	}

	private void startItem(String streamId, int index, PlayListItem item) {
		writePlaylistStatus(streamId, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, index);

		Result result = manager.startPlaylistItem(streamId, item);
		if (!result.isSuccess()) {
			logger.error("Playlist item:{} could not be started so the playlist is finished, streamId:{} reason:{}", index, streamId, result.getMessage());
			endPlaylist(streamId, true);
		}
	}

	/** Index of the item to play after the current one, or -1 when the playlist is over. */
	private int nextIndex(Broadcast playlist) {
		int next = playlist.getCurrentPlayIndex() + 1;

		if (next < playlist.getPlayListItemList().size()) {
			return next;
		}
		return playlist.isPlaylistLoopEnabled() ? 0 : -1;
	}

	/**
	 * Nothing is playing and nothing will start until the playlist is started again.
	 * @param rewind true when the playlist ran to its end, so the next start begins from the top
	 */
	private void endPlaylist(String streamId, boolean rewind) {
		//dropping the session first is what keeps the stop below from starting the next item
		sessions.remove(streamId);

		StreamFetcher item = manager.getStreamFetcher(streamId);
		if (item != null) {
			item.stopStream();
		}

		writePlaylistStatus(streamId, IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, rewind ? 0 : null);
	}

	/** Either argument may be null, which leaves that stored field alone. */
	private void writePlaylistStatus(String streamId, String status, Integer currentPlayIndex) {
		BroadcastUpdate update = new BroadcastUpdate();
		update.setPlayListStatus(status);
		update.setCurrentPlayIndex(currentPlayIndex);
		manager.getDatastore().updateBroadcastFields(streamId, update);
	}

	private void skipTo(String streamId, int index) {
		PlaylistSession session = sessions.get(streamId);
		if (session == null) {
			logger.info("Playlist is not running so item:{} is not played, streamId:{}", index, streamId);
			return;
		}

		StreamFetcher current = manager.getStreamFetcher(streamId);
		if (current != null) {
			session.pendingIndex = index;
			current.stopStream();
			return;
		}

		if (session.starting) {
			//an item is having its url checked right now, it picks this up instead of starting itself
			session.pendingIndex = index;
			return;
		}

		playIndex(streamId, index);
	}
}
