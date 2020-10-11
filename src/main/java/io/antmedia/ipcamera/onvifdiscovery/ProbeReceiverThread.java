package io.antmedia.ipcamera.onvifdiscovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProbeReceiverThread extends Thread {
	private final Collection<String> addresses;
	private final CountDownLatch serverStarted;
	private final DatagramSocket socket;
	private final CountDownLatch serverFinished;

	public ProbeReceiverThread(Collection<String> addresses, CountDownLatch serverStarted, DatagramSocket socket,
			CountDownLatch serverFinished) {
		this.addresses = addresses;
		this.serverStarted = serverStarted;
		this.socket = socket;
		this.serverFinished = serverFinished;
	}

	@Override
	public void run() {
	    try {
	       final DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
	       socket.setSoTimeout(DeviceDiscovery.WS_DISCOVERY_TIMEOUT);
	       long timerStarted = System.currentTimeMillis();
	       while (System.currentTimeMillis() - timerStarted < DeviceDiscovery.WS_DISCOVERY_TIMEOUT) {
	          serverStarted.countDown();
	          socket.receive(packet);
	          final Collection<String> collection = parseSoapResponseForUrls(Arrays.copyOf(packet.getData(), packet.getLength()));
	          for (String key : collection) {
	             addresses.add(key);
	          }
	       }
	    } catch (SocketTimeoutException | SocketException ignored) {
	    	//ignore this exception
	    } catch (Exception e) {
	       e.printStackTrace();
	    } finally {
	       serverFinished.countDown();
	       socket.close();
	    }
	 }
	private static Collection<Node> getNodeMatching(Node body, String regexp) {
	      final Collection<Node> nodes = new ArrayList<>();
	      if (body.getNodeName().matches(regexp)) nodes.add(body);
	      if (body.getChildNodes().getLength() == 0) return nodes;
	      NodeList returnList = body.getChildNodes();
	      for (int k = 0; k < returnList.getLength(); k++) {
	         final Node node = returnList.item(k);
	         nodes.addAll(getNodeMatching(node, regexp));
	      }
	      return nodes;
	   }

	   private static Collection<String> parseSoapResponseForUrls(byte[] data) throws SOAPException, IOException {
	      //System.out.println(new String(data));
	      final Collection<String> urls = new ArrayList<>();
	      MessageFactory factory = MessageFactory.newInstance(DeviceDiscovery.WS_DISCOVERY_SOAP_VERSION);
	      final MimeHeaders headers = new MimeHeaders();
	      headers.addHeader("Content-type", DeviceDiscovery.WS_DISCOVERY_CONTENT_TYPE);
	      SOAPMessage message = factory.createMessage(headers, new ByteArrayInputStream(data));
	      SOAPBody body = message.getSOAPBody();
	      for (Node node : getNodeMatching(body, ".*:XAddrs")) {
	         if (node.getTextContent().length() > 0) {
	            urls.addAll(Arrays.asList(node.getTextContent().split(" ")));
	         }
	      }
	      return urls;
	   }
}