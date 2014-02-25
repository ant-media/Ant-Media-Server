package org.red5.server.so;

import java.util.List;
import java.util.Map;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SOListener implements ISharedObjectListener {

	protected static Logger log = LoggerFactory.getLogger(SOListener.class);

	@SuppressWarnings("unused")
	private int id;

	public SOListener() {
	}

	public SOListener(int id) {
		this.id = id;
	}

	@Override
	public void onSharedObjectConnect(ISharedObjectBase so) {
		log.trace("onSharedObjectConnect");
	}

	@Override
	public void onSharedObjectDisconnect(ISharedObjectBase so) {
		log.trace("onSharedObjectDisconnect");
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value) {
		log.trace("onSharedObjectUpdate - key: {} value: {}", key, value);
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore values) {
		log.trace("onSharedObjectUpdate - values: {}", values);
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> values) {
		log.trace("onSharedObjectUpdate - values: {}", values);
	}

	@Override
	public void onSharedObjectDelete(ISharedObjectBase so, String key) {
		log.trace("onSharedObjectDelete");
	}

	@Override
	public void onSharedObjectClear(ISharedObjectBase so) {
		log.trace("onSharedObjectClear");
	}

	@Override
	public void onSharedObjectSend(ISharedObjectBase so, String method, List<?> params) {
		log.trace("onSharedObjectSend");
	}
}
