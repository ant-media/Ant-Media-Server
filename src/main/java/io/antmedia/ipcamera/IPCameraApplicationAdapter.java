package io.antmedia.ipcamera;

import java.util.HashMap;
import java.util.List;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.streamsource.StreamSources;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class IPCameraApplicationAdapter extends AntMediaApplicationAdapter {

	protected static Logger logger = LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap();

	StreamSources sources;

	private static Logger log = Red5LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	@Override
	public boolean appStart(IScope app) {

		this.sources = new StreamSources(app);

		List<Broadcast> streams = getDataStore().getExternalStreamsList();

		sources.startStreams(streams);

		return super.appStart(app);

	}

	public void startStreaming(Broadcast broadcast) {

		sources.startStreaming(broadcast);

	}

	public void stopStreaming(Broadcast cam) {
		sources.stopStreaming(cam);

	}

	public OnvifCamera getOnvifCamera(String id) {
		OnvifCamera onvifCamera = onvifCameraList.get(id);
		if (onvifCamera == null) {

			Broadcast camera = getDataStore().get(id);
			if (camera != null) {
				onvifCamera = new OnvifCamera();
				onvifCamera.connect(camera.getIpAddr(), camera.getUsername(), camera.getPassword());

				onvifCameraList.put(id, onvifCamera);
			}
		}
		return onvifCamera;
	}

}
