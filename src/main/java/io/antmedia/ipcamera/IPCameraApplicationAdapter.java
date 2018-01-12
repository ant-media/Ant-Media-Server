package io.antmedia.ipcamera;

import java.util.ArrayList;
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
import org.red5.server.api.IConnection;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class IPCameraApplicationAdapter extends AntMediaApplicationAdapter {

	private static final int RECORD_INTERVAL_IN_MS = 24 * 3600000; // 3600 000
																	// -> one
																	// hour in
																	// milliseconds

	private static final long ONE_GIGABYTE = 1024 * 1024 * 1024;

	protected static Logger logger = LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	private int cameraCheckerCount = 0;

	private static final String STREAMS_DIRECTORY = "webapps/IPCamera/streams/";
	private static final String PREVIEW_DIRECTORY = "webapps/IPCamera/";

	private final static int HEADER_LENGTH = 9;
	private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff),
			(byte) (0 & 0xff) };
	private final static int TAG_HEADER_LENGTH = 11;

	private MapDBStore cameradb;

	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap();

	private static Logger log = Red5LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	private List<CameraScheduler> camSchedulerList = new ArrayList<>();

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		// log.info("appConnect");
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {
		// log.info("disconnect");
		super.disconnect(conn, scope);
	}

	public void startCameraStreaming(Broadcast broadcast) {

		CameraScheduler camScheduler = new CameraScheduler(broadcast, this);
		camScheduler.startStream();
		camSchedulerList.add(camScheduler);

	}

	public void stopCameraStreaming(Broadcast cam) {
		logger.warn("inside of stopCameraStreaming");

		for (CameraScheduler camScheduler : camSchedulerList) {
			if (camScheduler.getCamera().getIpAddr().equals(cam.getIpAddr())) {
				camScheduler.stopStream();
				camSchedulerList.remove(camScheduler);
				break;
			}

		}

	}

	@Override
	public boolean appStart(IScope app) {
		// addScheduledJob(RECORD_INTERVAL_IN_MS, cameraScheduler);

		List<Broadcast> cameras = getDataStore().getCameraList();

		for (int i = 0; i < cameras.size(); i++) {
			startCameraStreaming(cameras.get(i));
		}

		addScheduledJobAfterDelay(60000, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {

				cameraCheckerCount++;

				logger.warn("inside of cameracheckercount control...cameraCheckerCount is  :" + cameraCheckerCount);

				if (cameraCheckerCount % 60 == 0) {

					for (CameraScheduler camScheduler : camSchedulerList) {
						if (camScheduler.isRunning()) {
							camScheduler.stopStream();
						}
						camScheduler.startStream();
					}

				} else {
					for (CameraScheduler camScheduler : camSchedulerList) {
						if (!camScheduler.isRunning()) {
							camScheduler.startStream();
						}
					}
				}

			}
		}, 65000);

		return super.appStart(app);
	}

	@Override
	public void appStop(IScope app) {
		super.appStop(app);
	}

	public MapDBStore getCameradb() {
		return cameradb;
	}

	public void setCameradb(MapDBStore cameradb) {
		this.cameradb = cameradb;
	}

	public OnvifCamera getOnvifCamera(String ipAddr) {
		OnvifCamera onvifCamera = onvifCameraList.get(ipAddr);
		if (onvifCamera == null) {
			Broadcast camera = cameradb.getCamera(ipAddr);
			if (camera != null) {
				onvifCamera = new OnvifCamera();
				onvifCamera.connect(ipAddr, camera.getUsername(), camera.getPassword());
				onvifCameraList.put(ipAddr, onvifCamera);
			}
		}
		return onvifCamera;
	}

}
