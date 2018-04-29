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

package org.red5.server.adapter;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;

/**
 * Base class for applications, takes care that callbacks are executed single-threaded. If you want to have maximum performance, use
 * {@link MultiThreadedApplicationAdapter} instead.
 * 
 * Using this class may lead to problems if accepting a client in the <code>Connect</code> or <code>Join</code> methods takes too
 * long, so using the multi-threaded version is preferred.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ApplicationAdapter extends MultiThreadedApplicationAdapter {

    private Semaphore lock;

    /** {@inheritDoc} */
    @Override
    public boolean start(IScope scope) {
        if (lock == null) {
            lock = new Semaphore(1, true);
        }
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            return super.start(scope);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void stop(IScope scope) {
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            super.stop(scope);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean connect(IConnection conn, IScope scope, Object[] params) {
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            return super.connect(conn, scope, params);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(IConnection conn, IScope scope) {
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            super.disconnect(conn, scope);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean join(IClient client, IScope scope) {
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            return super.join(client, scope);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void leave(IClient client, IScope scope) {
        try {
            lock.tryAcquire(1, TimeUnit.SECONDS);
            super.leave(client, scope);
        } catch (InterruptedException e) {
            e.printStackTrace();
			Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
    }

}
