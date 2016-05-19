/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

package org.red5.server.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class LocalNetworkUtil {

    /**
     * Returns a loopback IP address.
     * 
     * @return the current environment's IP address, taking into account the Internet connection to any of the available machine's Network interfaces.
     * 
     * Examples of the outputs:
     * 
     * fec0:0:0:9:213:e8ff:fef1:b717%4 siteLocal: true isLoopback: false isIPV6: true
     * ============================================
     * 130.212.150.216                 siteLocal: false isLoopback: false isIPV6: false
     * ============================================
     * 0:0:0:0:0:0:0:1%1               siteLocal: false isLoopback: true isIPV6: true
     * ============================================
     * 127.0.0.1                       siteLocal: false isLoopback: true isIPV6: false
     */
    public static String getCurrentEnvironmentLoopbackIp() {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress addr = address.nextElement();
                if (addr.isLoopbackAddress() && !addr.isSiteLocalAddress()) {
                    return addr.getHostAddress();
                }
            }
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

}
