package io.antmedia.track;

import java.util.List;

public interface ISubtrackPoller {
	
	public interface SubtrackListener {
		void onSubTracks(List<String> subTrackStreamIds);
	}
	
	public void register(String streamId, SubtrackListener listener);
	
	public void notifySubtrackListeners(String streamId, List<String> subtracks);
	
	public void unRegister(String streamId, SubtrackListener subtrackPollerListener);

}
