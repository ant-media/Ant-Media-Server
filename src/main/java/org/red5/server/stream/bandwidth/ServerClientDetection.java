/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.stream.bandwidth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates the bandwidth between the client and server. The checks originate from the server.
 * 
 * @see <a href='http://help.adobe.com/en_US/FlashMediaServer/3.5_Deving/WS5b3ccc516d4fbf351e63e3d11a0773d56e-7ffa.html'>FMS 3.5 Bandwidth Doc</a>
 * 
 * @author The Red5 Project
 * @author Dan Rossi
 * @author Paul Gregoire
 */
public class ServerClientDetection implements IPendingServiceCallback, IBandwidthDetection {

	private static Logger log = LoggerFactory.getLogger(ServerClientDetection.class);

	// maximum latency alloted for in milliseconds
	private static final double LATENCY_MAX = 1000d;

	// minimum latency alloted for in milliseconds
	private static final double LATENCY_MIN = 10d;

	private IConnection conn;

	private volatile double latency;

	private volatile double cumLatency = 1;

	private double kbitDown;

	private double deltaDown;

	private double deltaTime;

	// current bytes written on the connection
	private long startBytesWritten;

	// start time using nanos
	private long startTime;

	// time passed overall 
	private long timePassed;

	private AtomicInteger packetsSent = new AtomicInteger(0);

	private AtomicInteger packetsReceived = new AtomicInteger(0);

	private byte[] payload = new byte[1024];

	private byte[] payload1 = new byte[1024 * 32];

	public void checkBandwidth(IConnection conn) {
		calculateClientBw(conn);
	}

	public void calculateClientBw(IConnection conn) {
		log.debug("calculateClientBw: {} ", conn);
		// set local connection ref
		this.conn = conn;
		// get random generator
		Random rnd = new Random();
		rnd.nextBytes(payload);
		rnd.nextBytes(payload1);
		// get the current bytes written on the connection
		startBytesWritten = conn.getWrittenBytes();
		// start time using nanos
		startTime = System.nanoTime();
		log.debug("Starting bandwidth check at {} ns", startTime);
		callBWCheck("");
	}

	/**
	 * Handle callback from service call.
	 */
	public void resultReceived(IPendingServiceCall call) {
		// if we aren't connection, skip any further testing
		if (Call.STATUS_NOT_CONNECTED != call.getStatus()) {
			// receive time using nanos
			long now = System.nanoTime();
			// increment received
			int received = packetsReceived.incrementAndGet();
			log.debug("Call time stamps - write: {} read: {}", call.getWriteTime(), call.getReadTime());
			// time passed is in milliseconds
			timePassed = (now - startTime) / 1000000;
			log.debug("Received count: {} sent: {} timePassed: {} ms", new Object[] { received, packetsSent.get(), timePassed });
			switch (received) {
				case 1:
					// first packet is used to test latency
					latency = Math.max(Math.min(timePassed, LATENCY_MAX), LATENCY_MIN);
					log.debug("Receive latency: {}", latency);
					// We now have a latency figure so can start sending test data.
					// Second call. 1st packet sent
					log.debug("Sending first payload at {} ns", now);
					callBWCheck(payload); // 1k	
					break;
				case 2:
					log.debug("Sending second payload at {} ns", now);
					// increment cumulative latency
					cumLatency++;
					callBWCheck(payload1); // 32k
					break;
				default:
					log.debug("Doing calculations at {} ns", now);
					// increment cumulative latency
					cumLatency++;
					// bytes to kbits
					deltaDown = ((conn.getWrittenBytes() - startBytesWritten) * 8) / 1000d;
					log.debug("Delta kbits: {}", deltaDown);
					// total dl time - latency for each packet sent in secs
					deltaTime = (timePassed - (latency * cumLatency));
					if (deltaTime <= 0) {
						deltaTime = (timePassed + latency);
					}
					log.debug("Delta time: {} ms", deltaTime);
					// calculate kbit/s
					kbitDown = Math.round(deltaDown / (deltaTime / 1000d));
					log.debug("onBWDone: kbitDown: {} deltaDown: {} deltaTime: {} latency: {} ", new Object[] { kbitDown, deltaDown, deltaTime, latency });
					callBWDone();
			}
		} else {
			log.debug("Pending call skipped due to being no longer connected");
		}
	}

	private void callBWCheck(Object payload) {
		if (log.isTraceEnabled()) {
			log.trace("callBWCheck: {}", payload);
		} else {
			log.debug("callBWCheck");
		}
		IConnection conn = Red5.getConnectionLocal();
		Map<String, Object> statsValues = new HashMap<String, Object>();
		statsValues.put("count", packetsReceived.get());
		statsValues.put("sent", packetsSent.get());
		statsValues.put("timePassed", timePassed);
		statsValues.put("latency", latency);
		statsValues.put("cumLatency", cumLatency);
		statsValues.put("payload", payload);
		if (conn instanceof IServiceCapableConnection) {
			log.debug("Invoking onBWCheck on the client");
			// increment sent counter
			packetsSent.incrementAndGet();
			// invoke on the client
			((IServiceCapableConnection) conn).invoke("onBWCheck", new Object[] { statsValues }, this);
		}
	}

	private void callBWDone() {
		log.debug("callBWDone");
		IConnection conn = Red5.getConnectionLocal();
		Map<String, Object> statsValues = new HashMap<String, Object>();
		statsValues.put("kbitDown", kbitDown);
		statsValues.put("deltaDown", deltaDown);
		statsValues.put("deltaTime", deltaTime);
		statsValues.put("latency", latency);
		if (conn instanceof IServiceCapableConnection) {
			log.debug("Invoking onBWDone on the client");
			// invoke on the client
			((IServiceCapableConnection) conn).invoke("onBWDone", new Object[] { statsValues });
			// adjust bandwidth to mbit/s
			int mbits = (int) ((kbitDown / 1000d) * 1000000);
			log.debug("Setting bandwidth to {} mbit/s", mbits);
			// tell the flash player how fast we want data and how fast we shall send it
			conn.setBandwidth(mbits);
		}
	}

	public void onServerClientBWCheck() {
		log.debug("onServerClientBWCheck");
		calculateClientBw(Red5.getConnectionLocal());
	}

}
