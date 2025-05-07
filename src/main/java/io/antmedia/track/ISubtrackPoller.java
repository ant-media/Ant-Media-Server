package io.antmedia.track;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;

public interface ISubtrackPoller {
	
	public interface SubtrackListener {
		void onSubTracks(List<Broadcast> subTrackStreamIds);
		
		void onNewSubTrack(String subTrackStreamId, String role);
	}
	
	public void register(String mainTrackId, SubtrackListener listener);
	
	public void notifySubtrackListeners(String mainTrackId, List<Broadcast> subtracks);
	
	
	public void notifyNewSubTrack(String mainTrackId, String role, String subTrackStreamId);
	
	public void unRegister(String mainTrackId, SubtrackListener subtrackPollerListener);
	
	/**
	 * This method is used to check if a listener is registered for a given streamId
	 * @param mainTrackId the streamId of the main track
	 * @return true if there is a listener registered for the given streamId, false otherwise
	 */
	public boolean hasListener(String mainTrackId);

}
