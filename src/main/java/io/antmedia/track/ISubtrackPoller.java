package io.antmedia.track;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;

public interface ISubtrackPoller {
	
	public interface SubtrackListener {
		/**
		 * This method is used to notify the listeners for the all subtracks
		 * @param subTrackStreamIds the streamIds of the subtracks
		 */
		void onSubTracks(List<Broadcast> subTrackStreamIds);
		
		/**
		 * This method is used to notify the listeners that a new subtrack has been added
		 * @param subTrackStreamId the streamId of the subtrack
		 * @param role the role of the subtrack
		 */
		void onNewSubTrack(String subTrackStreamId, String role);
		
		/**
		 * This method is used to notify the listeners that a subtrack has been removed
		 * @param subTrackStreamId the streamId of the subtrack
		 * @param role the role of the subtrack
		 */
		void onSubTrackRemoved(String subTrackStreamId, String role);
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

	/**
	 * This method is used to notify the listeners that a subtrack has been removed
	 * @param mainTrackId the streamId of the main track
	 * @param role the role of the subtrack
	 * @param streamId the streamId of the subtrack
	 */
	public void notifySubTrackRemoved(String mainTrackId, String role, String streamId);

}
