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

package org.red5.server.messaging;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple in-memory version of pull-pull pipe. It is triggered by an active consumer that pulls messages through it from a pullable provider.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class InMemoryPullPullPipe extends AbstractPipe {
    private static final Logger log = LoggerFactory.getLogger(InMemoryPullPullPipe.class);

    /** {@inheritDoc} */
    @Override
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
        boolean success = super.subscribe(consumer, paramMap);
        if (success) {
            fireConsumerConnectionEvent(consumer, PipeConnectionEvent.EventType.CONSUMER_CONNECT_PULL, paramMap);
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        if (provider instanceof IPullableProvider) {
            boolean success = super.subscribe(provider, paramMap);
            if (success) {
                fireProviderConnectionEvent(provider, PipeConnectionEvent.EventType.PROVIDER_CONNECT_PULL, paramMap);
            }
            return success;
        } else {
            throw new IllegalArgumentException("Non-pullable provider not supported by PullPullPipe");
        }
    }

    /** {@inheritDoc} */
    public IMessage pullMessage() throws IOException {
        IMessage message = null;
        for (IProvider provider : providers) {
            if (provider instanceof IPullableProvider) {
                // choose the first available provider
                try {
                    message = ((IPullableProvider) provider).pullMessage(this);
                    if (message != null) {
                        break;
                    }
                } catch (Throwable t) {
                    if (t instanceof IOException) {
                        // Pass this along
                        throw (IOException) t;
                    }
                    log.error("exception when pulling message from provider", t);
                }
            }
        }
        return message;
    }

    /** {@inheritDoc} */
    public IMessage pullMessage(long wait) {
        IMessage message = null;
        // divided evenly
        int size = providers.size();
        long averageWait = size > 0 ? wait / size : 0;
        // choose the first available provider
        for (IProvider provider : providers) {
            if (provider instanceof IPullableProvider) {
                try {
                    message = ((IPullableProvider) provider).pullMessage(this, averageWait);
                    if (message != null) {
                        break;
                    }
                } catch (Throwable t) {
                    log.error("exception when pulling message from provider", t);
                }
            }
        }
        return message;
    }

    /** {@inheritDoc} */
    public void pushMessage(IMessage message) {
        // push mode ignored
    }

}
