package io.antmedia.ipcamera.onvifdiscovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProbeSenderThread extends Thread {
	
	private Thread probeReceiverThread;
	private DatagramSocket socket;
	private CountDownLatch serverStarted;
	private CountDownLatch serverFinished;
	private InetAddress address;
	private String probeMsgTemplate;

	public ProbeSenderThread(InetAddress address, DatagramSocket socket, String probeMsgTemplate,
			CountDownLatch serverStarted, CountDownLatch serverFinished, Thread probeReceiverThread) {
		this.probeReceiverThread = probeReceiverThread;
		this.serverStarted = serverStarted;
		this.serverFinished = serverFinished;
		this.socket = socket;
		this.address = address;
		this.probeMsgTemplate = probeMsgTemplate;
	}

	public void run() {
		try {
			final String uuid = UUID.randomUUID().toString();
			final String probe = probeMsgTemplate.replaceAll(
					"<wsa:MessageID>urn:uuid:.*</wsa:MessageID>",
					"<wsa:MessageID>urn:uuid:" + uuid + "</wsa:MessageID>");
			probeReceiverThread.start();
			try {
				serverStarted.await(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			socket.send(new DatagramPacket(probe.getBytes(), probe.length(), address, DeviceDiscovery.WS_DISCOVERY_PORT));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			serverFinished.await(DeviceDiscovery.WS_DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

}