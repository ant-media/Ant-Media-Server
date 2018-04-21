package com.antstreaming.rtsp.protocol;

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

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   Copyright (C) 2005 - Matteo Merli - matteo.merli@gmail.com            *
 *                                                                         *
 ***************************************************************************/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse the RTSP Transport header field. Reference Grammar:
 * 
 * <pre>
 *         Transport           =    &quot;Transport&quot; &quot;:&quot;
 *                                  1\#transport-spec
 *         transport-spec      =    transport-protocol/profile[/lower-transport]
 *                                  *parameter
 *         transport-protocol  =    &quot;RTP&quot;
 *         profile             =    &quot;AVP&quot;
 *         lower-transport     =    &quot;TCP&quot; | &quot;UDP&quot;
 *         parameter           =    ( &quot;unicast&quot; | &quot;multicast&quot; )
 *                             |    &quot;;&quot; &quot;destination&quot; [ &quot;=&quot; address ]
 *                             |    &quot;;&quot; &quot;interleaved&quot; &quot;=&quot; channel [ &quot;-&quot; channel ]
 *                             |    &quot;;&quot; &quot;append&quot;
 *                             |    &quot;;&quot; &quot;ttl&quot; &quot;=&quot; ttl
 *                             |    &quot;;&quot; &quot;layers&quot; &quot;=&quot; 1*DIGIT
 *                             |    &quot;;&quot; &quot;port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;client_port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;server_port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;ssrc&quot; &quot;=&quot; ssrc
 *                             |    &quot;;&quot; &quot;mode&quot; = &lt;&quot;&gt; 1\#mode &lt;&quot;&gt;
 *         ttl                 =    1*3(DIGIT)
 *         port                =    1*5(DIGIT)
 *         ssrc                =    8*8(HEX)
 *         channel             =    1*3(DIGIT)
 *         address             =    host
 *         mode                =    &lt;&quot;&gt; *Method &lt;&quot;&gt; | Method
 *      
 *      
 *         Example:
 *           Transport: RTP/AVP;multicast;ttl=127;mode=&quot;PLAY&quot;,
 *                      RTP/AVP;unicast;client_port=3456-3457;mode=&quot;PLAY&quot;
 * </pre>
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public class RTSPTransport {

	private static Logger log = LoggerFactory.getLogger(RTSPTransport.class);

	public enum TransportProtocol {
		NONE, RTP, RDT, RAW
	}

	public enum Profile {
		NONE, AVP
	}

	public enum LowerTransport {
		NONE, TCP, UDP
	}

	public enum DeliveryType {
		NONE, UNICAST, MULTICAST
	}

	TransportProtocol transportProtocol;

	Profile profile;

	LowerTransport lowerTransport;

	DeliveryType deliveryType;

	String destination;

	String interleaved;

	int layers;

	boolean append;

	int ttl;

	int[] port = new int[2];

	int[] clientPort = new int[2];

	int[] serverPort = new int[2];

	String ssrc;

	String mode;

	String source;


	/**
	 * Constructor. Creates a RTSPTransport object from a transport header
	 * string.
	 * @param transport transport type
	 */
	public RTSPTransport(String transport) {
		transportProtocol = TransportProtocol.NONE;
		profile = Profile.NONE;
		lowerTransport = LowerTransport.NONE;
		deliveryType = DeliveryType.NONE;
		destination = null;
		interleaved = null;
		layers = 0;
		append = false;
		ttl = 0;
		port[0] = 0;
		port[1] = 0;
		clientPort[0] = 0;
		clientPort[1] = 0;
		serverPort[0] = 0;
		serverPort[1] = 0;
		ssrc = null;
		mode = null;
		source = null;

		parseTransport(transport);
		if (transport.compareToIgnoreCase(this.toString()) != 0) {
			log.warn("Transport header incorrectly parsed.");
		}
	}

	private void parseTransport(String transport) {
		for (String tok : transport.split(";")) {

			// First check for the transport protocol
			if (tok.startsWith("RTP") || tok.startsWith("RDT")) {
				String[] tpl = tok.split("/");
				transportProtocol = TransportProtocol.valueOf(tpl[0]);
				if (tpl.length > 1)
					profile = Profile.valueOf(tpl[1]);
				if (tpl.length > 2)
					lowerTransport = LowerTransport.valueOf(tpl[2]);
				continue;
			}

			if (tok.compareToIgnoreCase("unicast") == 0)
				deliveryType = DeliveryType.UNICAST;
			else if (tok.compareToIgnoreCase("multicast") == 0)
				deliveryType = DeliveryType.MULTICAST;
			else if (tok.startsWith("destination"))
				setDestination(_getStrValue(tok));
			else if (tok.startsWith("interleaved"))
				setInterleaved(_getStrValue(tok));
			else if (tok.startsWith("append"))
				setAppend(true);
			else if (tok.startsWith("layers"))
				setLayers(Integer.valueOf(_getStrValue(tok)));
			else if (tok.startsWith("ttl"))
				setTTL(Integer.valueOf(_getStrValue(tok)));
			else if (tok.startsWith("port"))
				setPort(_getPairValue(tok));
			else if (tok.startsWith("client_port"))
				setClientPort(_getPairValue(tok));
			else if (tok.startsWith("server_port"))
				setServerPort(_getPairValue(tok));
			else if (tok.startsWith("ssrc"))
				setSSRC(_getStrValue(tok));
			else if (tok.startsWith("mode"))
				setMode(_getStrValue(tok));
			else if (tok.startsWith("source"))
				setSource(_getStrValue(tok));
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(transportProtocol);
		if (profile != Profile.NONE) {
			sb.append("/").append(profile);
			if (lowerTransport != LowerTransport.NONE)
				sb.append("/").append(lowerTransport);
		}
		if (deliveryType != DeliveryType.NONE)
			sb.append(";").append(deliveryType);
		if (destination != null)
			sb.append(";destination=").append(destination);
		if (interleaved != null)
			sb.append(";interleaved=").append(interleaved);
		if (append)
			sb.append(";append");
		if (layers > 0)
			sb.append(";layers=").append(layers);
		if (ttl > 0)
			sb.append(";ttl=").append(ttl);
		if (port[0] > 0)
			sb.append(";port=").append(port[0]).append("-").append(port[1]);
		if (clientPort[0] > 0)
			sb.append(";clientPort=").append(clientPort[0]).append("-")
					.append(clientPort[1]);
		if (serverPort[0] > 0)
			sb.append(";serverPort=").append(serverPort[0]).append("-")
					.append(serverPort[1]);
		if (ssrc != null)
			sb.append(";ssrc=").append(ssrc);
		if (source != null)
			sb.append(";source=").append(source);
		if (mode != null)
			sb.append(";mode=").append(mode);
		return sb.toString();
	}

	/**
	 * @return Returns the append.
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * @param append
	 *        The append to set.
	 */
	public void setAppend(boolean append) {
		this.append = append;
	}

	/**
	 * @return Returns the clientPort.
	 */
	public int[] getClientPort() {
		return clientPort;
	}

	/**
	 * @param clientPort
	 *        The clientPort to set.
	 */
	public void setClientPort(int[] clientPort) {
		this.clientPort = clientPort;
	}

	/**
	 * @return Returns the deliveryType.
	 */
	public DeliveryType getDeliveryType() {
		return deliveryType;
	}

	/**
	 * @param deliveryType
	 *        The deliveryType to set.
	 */
	public void setDeliveryType(DeliveryType deliveryType) {
		this.deliveryType = deliveryType;
	}

	/**
	 * @return Returns the destination.
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @param destination
	 *        The destination to set.
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * @return Returns the interleaved.
	 */
	public String getInterleaved() {
		return interleaved;
	}

	/**
	 * @param interleaved
	 *        The interleaved to set.
	 */
	public void setInterleaved(String interleaved) {
		this.interleaved = interleaved;
	}

	/**
	 * @return Returns the layers.
	 */
	public int getLayers() {
		return layers;
	}

	/**
	 * @param layers
	 *        The layers to set.
	 */
	public void setLayers(int layers) {
		this.layers = layers;
	}

	/**
	 * @return Returns the lowerTransport.
	 */
	public LowerTransport getLowerTransport() {
		return lowerTransport;
	}

	/**
	 * @param lowerTransport
	 *        The lowerTransport to set.
	 */
	public void setLowerTransport(LowerTransport lowerTransport) {
		this.lowerTransport = lowerTransport;
	}

	/**
	 * @return Returns the mode.
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode
	 *        The mode to set.
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * @return Returns the port.
	 */
	public int[] getPort() {
		return port;
	}

	/**
	 * @param port
	 *        The port to set.
	 */
	public void setPort(int[] port) {
		this.port = port;
	}

	/**
	 * @return Returns the profile.
	 */
	public Profile getProfile() {
		return profile;
	}

	/**
	 * @param profile
	 *        The profile to set.
	 */
	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	/**
	 * @return Returns the serverPort.
	 */
	public int[] getServerPort() {
		return serverPort;
	}

	/**
	 * @param serverPort
	 *        The serverPort to set.
	 */
	public void setServerPort(int[] serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * @return Returns the ssrc.
	 */
	public String getSSRC() {
		return ssrc;
	}

	/**
	 * @param ssrc
	 *        The ssrc to set.
	 */
	public void setSSRC(String ssrc) {
		this.ssrc = ssrc;
	}

	/**
	 * @param ssrc
	 *        The ssrc to set.
	 */
	public void setSSRC(long ssrc) {
		this.ssrc = Long.toHexString(ssrc & 0xFFFFFFFFL).toUpperCase();
	}

	/**
	 * @return Returns the transportProtocol.
	 */
	public TransportProtocol getTransportProtocol() {
		return transportProtocol;
	}

	/**
	 * @param transportProtocol
	 *        The transportProtocol to set.
	 */
	public void setTransportProtocol(TransportProtocol transportProtocol) {
		this.transportProtocol = transportProtocol;
	}

	/**
	 * @return Returns the ttl.
	 */
	public int getTTL() {
		return ttl;
	}

	/**
	 * @param ttl
	 *        The ttl to set.
	 */
	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	/**
	 * Get the value part in a string like:
	 * 
	 * <pre>
	 * key = value
	 * </pre>
	 * 
	 * @param str
	 *        the content string
	 * @return a String containing only the value
	 */
	private static String _getStrValue(String str) {
		String[] list = str.split("=");
		if (list.length != 2)
			return null;

		return list[1];
	}

	/**
	 * Get the value part in a string like:
	 * 
	 * <pre>
	 * key = 6344 - 6345
	 * </pre>
	 * 
	 * @param str
	 *        the content string
	 * @return a int[2] containing only the value
	 */
	private static int[] _getPairValue(String str) {
		int[] pair = { 0, 0 };
		String[] list = str.split("=");
		if (list.length != 2)
			return pair;

		try {
			pair[0] = Integer.parseInt(list[1].split("-")[0]);
			pair[1] = Integer.parseInt(list[1].split("-")[1]);

			// log.debug("Client ports: " + 1);
			// Integers.parse();

		} catch (Exception e) {
			return pair;
		}
		return pair;
	}
}
