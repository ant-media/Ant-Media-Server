package io.antmedia.ipcamera;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.red5.io.ITag;
import org.red5.io.flv.FLVHeader;
import org.red5.io.utils.IOUtils;

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
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
//import org.slf4j.Logger;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.ipcamera.utils.Camera;
import io.antmedia.ipcamera.utils.CameraStore;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter implements IStreamMuxerListener, IStreamListener {

	private static final int RECORD_INTERVAL_IN_MS =  24 * 3600000; //3600 000 -> one hour in milliseconds


	private static final long ONE_GIGABYTE = 1024 * 1024 * 1024;
	
	protected static Logger logger = LoggerFactory.getLogger(Application.class);

	private int cameraCheckerCount = 0;

	private static final String STREAMS_DIRECTORY = "webapps/IPCamera/streams/";
	private static final String PREVIEW_DIRECTORY = "webapps/IPCamera/";

	private final static int HEADER_LENGTH = 9;
	private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff), (byte) (0 & 0xff) };
	private final static int TAG_HEADER_LENGTH = 11;


	private CameraStore cameradb;


	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap();

	private static Logger log = Red5LoggerFactory.getLogger(Application.class);


	private List<CameraScheduler> camSchedulerList = new ArrayList<>();





	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		//log.info("appConnect");
		return true;
	}






	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {
		//log.info("disconnect");
		super.disconnect(conn, scope);
	}


	public void startCameraStreaming(Camera camera) {

		CameraScheduler camScheduler = new CameraScheduler(camera, this);
		camScheduler.startStream();
		camSchedulerList.add(camScheduler);
		

	}
	
	public void stopCameraStreaming(Camera camera) {
		logger.warn("inside of stopCameraStreaming");


		
		for (CameraScheduler camScheduler : camSchedulerList) {
			if (camScheduler.getCamera().ipAddr.equals(camera.ipAddr)) {
				camScheduler.stopStream();
				camSchedulerList.remove(camScheduler);
				break;
			}
			
		}

	}



	@Override
	public boolean appStart(IScope app) {
		//addScheduledJob(RECORD_INTERVAL_IN_MS, cameraScheduler);
		

		Camera[] cameras = cameradb.getCameraList();
		
	

		for (int i = 0; i < cameras.length; i++) {
			startCameraStreaming(cameras[i]);
		}
		
//		new Thread() {
//			public void run() {
//				try {
//					Thread.sleep(60000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				logger.warn("before for loop int scheduler...");
//				for (CameraScheduler camScheduler : camSchedulerList) {
//					logger.warn("stopping camera stream");
//					camScheduler.stopStream();
//					//camScheduler.startStream();
//				}
//				
//			};
//		}.start();

		
		addScheduledJobAfterDelay(60000, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				
				cameraCheckerCount ++;
				
				logger.warn("inside of cameracheckercount control...cameraCheckerCount is  :" + cameraCheckerCount);
				
				
//				for (int i = 0; i < camSchedulerList.size(); i++) {
//					CameraScheduler cameraScheduler = camSchedulerList.get(i);
//					cameraScheduler.restart();
					
//					CameraScheduler newScheduler = new CameraScheduler(cameraScheduler.getCamera(), Application.this);
//					camSchedulerList.set(i, newScheduler);
//					newScheduler.startStream();
//				}
				
				

				if (cameraCheckerCount % 60 == 0) 
				{
					
					for (CameraScheduler camScheduler : camSchedulerList) {
						if (camScheduler.isRunning()) {
							camScheduler.stopStream();
						}
						camScheduler.startStream();
					}

				}
				else {
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


	public byte[] getTag(ITag tag) throws IOException {
		/*
		 * Tag header = 11 bytes
		 * |-|---|----|---|
		 *    0 = type
		 *  1-3 = data size
		 *  4-7 = timestamp
		 * 8-10 = stream id (always 0)
		 * Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 */
		// skip tags with no data
		int bodySize = tag.getBodySize();
		// ensure that the channel is still open
		// get the data type
		byte dataType = tag.getDataType();
		// if we're writing non-meta tags do seeking and tag size update

		// set a var holding the entire tag size including the previous tag length
		int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
		// resize
		// create a buffer for this tag
		ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
		// get the timestamp
		int timestamp = tag.getTimestamp();
		// allow for empty tag bodies
		byte[] bodyBuf = null;
		if (bodySize > 0) {
			// create an array big enough
			bodyBuf = new byte[bodySize];
			// put the bytes into the array
			tag.getBody().get(bodyBuf);
			// get the audio or video codec identifier

		}
		// Data Type
		IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
		// Body Size - Length of the message. Number of bytes after StreamID to end of tag 
		// (Equal to length of the tag - 11) 
		IOUtils.writeMediumInt(tagBuffer, bodySize); //3
		// Timestamp
		IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
		// Stream id
		tagBuffer.put(DEFAULT_STREAM_ID); //3
		// get the body if we have one
		if (bodyBuf != null) {
			tagBuffer.put(bodyBuf);
		}
		// we add the tag size
		tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
		// flip so we can process from the beginning
		tagBuffer.flip();
		// write the tag

		return tagBuffer.array();

	}

	public static byte[] getFLVHeader() {
		FLVHeader flvHeader = new FLVHeader();
		flvHeader.setFlagVideo(true);
		// create a buffer
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
		flvHeader.write(header);
		return header.array();
	}




	@Override
	public void errorOccured(Exception e1, String streamUrl) {
		System.out.println("Application.errorOccured()");
	}




	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {



	}


	public CameraStore getCameradb() {
		return cameradb;
	}


	public void setCameradb(CameraStore cameradb) {
		this.cameradb = cameradb;
	}


	public OnvifCamera getOnvifCamera(String ipAddr) {
		OnvifCamera onvifCamera = onvifCameraList.get(ipAddr);
		if (onvifCamera == null) {
			Camera camera = cameradb.getCamera(ipAddr);
			if (camera != null) {
				onvifCamera = new OnvifCamera();
				onvifCamera.connect(ipAddr, camera.username, camera.password);
				onvifCameraList.put(ipAddr, onvifCamera);
			}
		}
		return onvifCamera;		
	}


	@Override
	public void muxingFinished(String streamName) {
		// TODO Auto-generated method stub

	}


}
