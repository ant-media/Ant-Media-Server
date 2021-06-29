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

package org.red5.server.jmx.mxbeans;

import java.util.List;

import javax.management.MXBean;

@MXBean
public interface RTMPConnectionMXBean extends AttributeStoreMXBean {

    public String getType();

    public String getHost();

    public String getRemoteAddress();

    public List<String> getRemoteAddresses();

    public int getRemotePort();

    public String getPath();

    public String getSessionId();

    public boolean isConnected();

    public void close();

    public long getReadBytes();

    public long getWrittenBytes();

    public long getReadMessages();

    public long getWrittenMessages();

    public long getDroppedMessages();

    public long getPendingMessages();

}
