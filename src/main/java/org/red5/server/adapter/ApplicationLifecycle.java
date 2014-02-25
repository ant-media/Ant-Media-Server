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

package org.red5.server.adapter;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;

/**
 *
 * @author Dominick Accattato (daccattato@gmail.com) 
 */
public class ApplicationLifecycle implements IApplication {

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.red5.server.adapter.IApplication#appConnect(org.red5.server.api.
	 * IConnection, java.lang.Object[])
	 */
	public boolean appConnect(IConnection conn, Object[] params) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#appDisconnect(org.red5.server.api
	 * .IConnection)
	 */
	public void appDisconnect(IConnection conn) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#appJoin(org.red5.server.api.IClient,
	 * org.red5.server.api.IScope)
	 */
	public boolean appJoin(IClient client, IScope app) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#appLeave(org.red5.server.api.IClient
	 * , org.red5.server.api.IScope)
	 */
	public void appLeave(IClient client, IScope app) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#appStart(org.red5.server.api.IScope)
	 */
	public boolean appStart(IScope app) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#appStop(org.red5.server.api.IScope)
	 */
	public void appStop(IScope app) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomConnect(org.red5.server.api.
	 * IConnection, java.lang.Object[])
	 */
	public boolean roomConnect(IConnection conn, Object[] params) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomDisconnect(org.red5.server.api
	 * .IConnection)
	 */
	public void roomDisconnect(IConnection conn) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomJoin(org.red5.server.api.IClient
	 * , org.red5.server.api.IScope)
	 */
	public boolean roomJoin(IClient client, IScope room) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomLeave(org.red5.server.api.IClient
	 * , org.red5.server.api.IScope)
	 */
	public void roomLeave(IClient client, IScope room) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomStart(org.red5.server.api.IScope
	 * )
	 */
	public boolean roomStart(IScope room) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.red5.server.adapter.IApplication#roomStop(org.red5.server.api.IScope)
	 */
	public void roomStop(IScope room) {
	}

}
