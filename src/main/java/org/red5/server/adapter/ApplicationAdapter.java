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
 * Base class for applications, takes care that callbacks are executed single-threaded.
 * If you want to have maximum performance, use {@link MultiThreadedApplicationAdapter}
 * instead.
 * 
 * Using this class may lead to problems if accepting a client in the <code>*Connect</code>
 * or <code>*Join</code> methods takes too long, so using the multi-threaded version is
 * preferred.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ApplicationAdapter extends MultiThreadedApplicationAdapter {

	/** {@inheritDoc} */
	@Override
	public synchronized boolean connect(IConnection conn, IScope scope, Object[] params) {
		return super.connect(conn, scope, params);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void disconnect(IConnection conn, IScope scope) {
		super.disconnect(conn, scope);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized boolean start(IScope scope) {
		return super.start(scope);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void stop(IScope scope) {
		super.stop(scope);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized boolean join(IClient client, IScope scope) {
		return super.join(client, scope);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void leave(IClient client, IScope scope) {
		super.leave(client, scope);
	}

}
