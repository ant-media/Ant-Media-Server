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

package org.red5.server.jmx.mxbeans;

import javax.management.MXBean;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;

/**
 * JMX mbean for Application.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
@MXBean
public interface ApplicationMXBean {

	public boolean appStart(IScope app);

	public boolean appConnect(IConnection conn, Object[] params);

	public boolean appJoin(IClient client, IScope app);

	public void appDisconnect(IConnection conn);

	public void appLeave(IClient client, IScope app);

	public void appStop(IScope app);

	public boolean roomStart(IScope room);

	public boolean roomConnect(IConnection conn, Object[] params);

	public boolean roomJoin(IClient client, IScope room);

	public void roomDisconnect(IConnection conn);

	public void roomLeave(IClient client, IScope room);

	public void roomStop(IScope room);

}
