package io.antmedia.track;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;

public interface ISubtrackPoller {
	
	public interface SubtrackListener {
		void onSubTracks(List<Broadcast> subTrackStreamIds);
	}
	
	public void register(String streamId, SubtrackListener listener);
	
	public void notifySubtrackListeners(String streamId, List<Broadcast> subtracks);
	
	public void unRegister(String streamId, SubtrackListener subtrackPollerListener);

}
