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

package org.red5.server.stream.filter;

import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls stream bandwidth
 */
public class StreamBandwidthController implements IFilter, IPipeConnectionListener, Runnable {

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(StreamBandwidthController.class);

    /**
     * Class name
     */
    public static final String KEY = StreamBandwidthController.class.getName();

    /**
     * Stream provider pipe
     */
    private IPipe providerPipe;

    /**
     * Stream consumer pipe
     */
    private IPipe consumerPipe;

    /**
     * Daemon thread that pulls data from provider and pushes to consumer, using this controller
     */
    private Thread puller;

    /**
     * Start state
     */
    private volatile boolean isStarted;

    /** {@inheritDoc} */
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        switch (event.getType()) {
            case PROVIDER_CONNECT_PULL:
                if (event.getProvider() != this && providerPipe == null) {
                    providerPipe = (IPipe) event.getSource();
                }
                break;
            case PROVIDER_DISCONNECT:
                if (event.getSource() == providerPipe) {
                    providerPipe = null;
                }
                break;
            case CONSUMER_CONNECT_PUSH:
                if (event.getConsumer() != this && consumerPipe == null) {
                    consumerPipe = (IPipe) event.getSource();
                }
                break;
            case CONSUMER_DISCONNECT:
                if (event.getSource() == consumerPipe) {
                    consumerPipe = null;
                }
                break;
            default:
                break;
        }
    }

    /** {@inheritDoc} */
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
    }

    /** {@inheritDoc} */
    public void run() {
        while (isStarted && providerPipe != null && consumerPipe != null) {
            try {
                IMessage message = providerPipe.pullMessage();
                if (log.isDebugEnabled()) {
                    log.debug("got message: {}", message);
                }
                consumerPipe.pushMessage(message);
            } catch (Exception e) {
                log.warn("Exception in pull and push", e);
                break;
            }
        }
        isStarted = false;
    }

    /**
     * Start pulling (streaming)
     */
    public void start() {
        startThread();
    }

    /**
     * Stop pulling, close stream
     */
    public void close() {
        isStarted = false;
    }

    /**
     * Start puller thread
     */
    private void startThread() {
        if (!isStarted && providerPipe != null && consumerPipe != null) {
            puller = new Thread(this, KEY);
            puller.setDaemon(true);
            isStarted = true;
            puller.start();
        }
    }
}
