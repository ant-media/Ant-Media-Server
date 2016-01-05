/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.net.rtmp;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.red5.server.jmx.mxbeans.RTMPMinaTransportMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport setup class configures socket acceptor and thread pools for RTMP in Mina.
 * 
 * <br>
 * <i>Note: This code originates from AsyncWeb. Originally modified by Luke Hubbard.</i> <br>
 * 
 * @author Luke Hubbard
 * @author Paul Gregoire
 */
public class RTMPMinaTransport implements RTMPMinaTransportMXBean {

    private static final Logger log = LoggerFactory.getLogger(RTMPMinaTransport.class);

    // utilized when enableDefaultAcceptor is false
    private ThreadPoolExecutor executor;

    protected SocketAcceptor acceptor;

    protected Set<String> addresses = new HashSet<String>();

    protected IoHandlerAdapter ioHandler;

    protected int ioThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * MBean object name used for de/registration purposes.
     */
    protected ObjectName serviceManagerObjectName;

    protected IoServiceStatistics stats;

    protected boolean enableMinaLogFilter;

    protected boolean enableMinaMonitor;

    protected int minaPollInterval = 1000;

    protected boolean tcpNoDelay = true;

    protected boolean useHeapBuffers = true;

    protected int sendBufferSize = 65536;

    protected int receiveBufferSize = 65536;

    private int readerIdleTime = 2;

    private int trafficClass = 0x08 | 0x10;

    private int backlog = 32;

    private int thoughputCalcInterval = 1;

    private long executorKeepAliveTime = 60000;

    // use the default mina acceptor and associated options
    private boolean enableDefaultAcceptor = true;

    private int initialPoolSize = 0;

    private int maxPoolSize = Runtime.getRuntime().availableProcessors() + 1;

    private int maxProcessorPoolSize = 16;

    private boolean keepAlive;

    private void initIOHandler() {
        if (ioHandler == null) {
            log.info("No RTMP IO Handler associated - using defaults");
            ioHandler = new RTMPMinaIoHandler();
        }
    }

    public void start() throws Exception {
        initIOHandler();
        IoBuffer.setUseDirectBuffer(!useHeapBuffers); // this is global, oh well
        if (useHeapBuffers) {
            // dont pool for heap buffers
            IoBuffer.setAllocator(new SimpleBufferAllocator());
        }
        log.info("RTMP Mina Transport Settings\nAcceptor style: {} I/O threads: {}\nTCP no-delay: {} keep-alive: {}", new Object[] { (enableDefaultAcceptor ? "default" : "blocking-queue"), ioThreads, tcpNoDelay, keepAlive });
        // use the defaults
        if (enableDefaultAcceptor) {
            //constructs an acceptor using default parameters, and given number of NioProcessor for multithreading I/O operations.
            acceptor = new NioSocketAcceptor(ioThreads);
        } else {
            // simple pool for i/o processors
            SimpleIoProcessorPool<NioSession> pool = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, maxProcessorPoolSize);
            // executor for acceptors, defaults to 32k for work queue
            executor = new ThreadPoolExecutor(initialPoolSize, maxPoolSize, executorKeepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(Short.MAX_VALUE));
            // our adjusted socket acceptor with tweaked executor and pool
            acceptor = new NioSocketAcceptor(executor, pool);
        }
        // use only for low level debugging
        if (enableMinaLogFilter) {
            DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
            LoggingFilter logFilter = new LoggingFilter(RTMPMinaTransport.class);
            //logFilter.setExceptionCaughtLogLevel(LogLevel.TRACE);
            //logFilter.setMessageReceivedLogLevel(LogLevel.TRACE);
            //logFilter.setMessageSentLogLevel(LogLevel.TRACE);
            //logFilter.setSessionClosedLogLevel(LogLevel.TRACE);
            //logFilter.setSessionCreatedLogLevel(LogLevel.TRACE);
            //logFilter.setSessionIdleLogLevel(LogLevel.TRACE);
            //logFilter.setSessionOpenedLogLevel(LogLevel.TRACE);
            chain.addLast("logger", logFilter);
        }
        // close sessions when the acceptor is stopped
        acceptor.setCloseOnDeactivation(true);
        // set acceptor props
        acceptor.setHandler(ioHandler);
        // requested maximum length of the queue of incoming connections
        acceptor.setBacklog(backlog);
        // get the current session config that would be used during create
        SocketSessionConfig sessionConf = acceptor.getSessionConfig();
        // reuse the addresses
        sessionConf.setReuseAddress(true);
        sessionConf.setTcpNoDelay(tcpNoDelay);
        sessionConf.setSendBufferSize(sendBufferSize);
        // 
        sessionConf.setReceiveBufferSize(receiveBufferSize);
        sessionConf.setMaxReadBufferSize(receiveBufferSize);
        // sets the interval (seconds) between each throughput calculation, the default value is 3 seconds
        sessionConf.setThroughputCalculationInterval(thoughputCalcInterval);
        // set the reader idle time (seconds)
        sessionConf.setReaderIdleTime(readerIdleTime);
        sessionConf.setKeepAlive(keepAlive);
        // to prevent setting of the traffic class we expect a value of -1
        if (trafficClass == -1) {
            log.info("Traffic class modification is disabled");
        } else {
            // set the traffic class - http://docs.oracle.com/javase/6/docs/api/java/net/Socket.html#setTrafficClass(int)
            // IPTOS_LOWCOST (0x02)
            // IPTOS_RELIABILITY (0x04)
            // IPTOS_THROUGHPUT (0x08) *
            // IPTOS_LOWDELAY (0x10) *
            sessionConf.setTrafficClass(trafficClass);
        }
        // get info
        log.info("Send buffer size: {} recv buffer size: {} so linger: {} traffic class: {}", new Object[] { sessionConf.getSendBufferSize(), sessionConf.getReceiveBufferSize(), sessionConf.getSoLinger(), sessionConf.getTrafficClass() });
        // set reuse address on the socket acceptor as well
        acceptor.setReuseAddress(true);
        try {
            // loop through the addresses and bind
            Set<InetSocketAddress> socketAddresses = new HashSet<InetSocketAddress>();
            for (String addr : addresses) {
                if (addr.indexOf(':') != -1) {
                    String[] parts = addr.split(":");
                    socketAddresses.add(new InetSocketAddress(parts[0], Integer.valueOf(parts[1])));
                } else {
                    socketAddresses.add(new InetSocketAddress(addr, 1935));
                }
            }
            log.debug("Binding to {}", socketAddresses.toString());
            acceptor.bind(socketAddresses);
            // create a new mbean for this instance RTMPMinaTransport
            String cName = this.getClass().getName();
            if (cName.indexOf('.') != -1) {
                cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
            }
            //enable only if user wants it
            if (enableMinaMonitor) {
                //add a stats to allow for more introspection into the workings of mina
                stats = new IoServiceStatistics((AbstractIoService) acceptor);
                //poll every second
                stats.setThroughputCalculationInterval(minaPollInterval);
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                try {
                    serviceManagerObjectName = new ObjectName("org.red5.server:type=RTMPMinaTransport");
                    mbs.registerMBean(new StandardMBean(this, RTMPMinaTransportMXBean.class, true), serviceManagerObjectName);
                } catch (Exception e) {
                    log.warn("Error on jmx registration", e);
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred during resolve / bind", e);
        }
    }

    public void stop() {
        log.info("RTMP Mina Transport stop");
        // first we unbind to prevent new connections
        acceptor.unbind();
        // second we shutdown the customized executor, if we used it
        if (!enableDefaultAcceptor) {
            executor.shutdownNow();
        }
        // lastly dispose the acceptor without allowing for deadlocks
        acceptor.dispose(false);
        // deregister with jmx
        if (serviceManagerObjectName != null) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.unregisterMBean(serviceManagerObjectName);
            } catch (Exception e) {
                log.warn("Error on jmx unregistration", e);
            }
        }
    }

    public void setAddress(String address) {
        addresses.add(address);
        log.info("RTMP will be bound to {}", address);
    }

    public void setAddresses(List<String> addrs) {
        for (String addr : addrs) {
            addresses.add(addr);
        }
        log.info("RTMP will be bound to {}", addresses);
    }

    public void setIoHandler(IoHandlerAdapter rtmpIOHandler) {
        this.ioHandler = rtmpIOHandler;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    /**
     * @param sendBufferSize
     *            the sendBufferSize to set
     */
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @param receiveBufferSize
     *            the receiveBufferSize to set
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @param trafficClass
     *            the trafficClass to set
     */
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    /**
     * @param backlog
     *            the backlog to set
     */
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    /**
     * @param thoughputCalcInterval
     *            the thoughputCalcInterval to set
     */
    public void setThoughputCalcInterval(int thoughputCalcInterval) {
        this.thoughputCalcInterval = thoughputCalcInterval;
    }

    /**
     * @param executorKeepAliveTime
     *            the executorKeepAliveTime to set
     */
    public void setExecutorKeepAliveTime(long executorKeepAliveTime) {
        this.executorKeepAliveTime = executorKeepAliveTime;
    }

    public void setEnableDefaultAcceptor(boolean enableDefaultAcceptor) {
        this.enableDefaultAcceptor = enableDefaultAcceptor;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * @param maxProcessorPoolSize
     *            the maxProcessorPoolSize to set
     */
    public void setMaxProcessorPoolSize(int maxProcessorPoolSize) {
        this.maxProcessorPoolSize = maxProcessorPoolSize;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * @param keepAlive
     *            the keepAlive to set
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setUseHeapBuffers(boolean useHeapBuffers) {
        this.useHeapBuffers = useHeapBuffers;
    }

    /**
     * @return the enableMinaLogFilter
     */
    public boolean isEnableMinaLogFilter() {
        return enableMinaLogFilter;
    }

    /**
     * @param enableMinaLogFilter
     *            the enableMinaLogFilter to set
     */
    public void setEnableMinaLogFilter(boolean enableMinaLogFilter) {
        this.enableMinaLogFilter = enableMinaLogFilter;
    }

    /**
     * @param enableMinaMonitor
     *            the enableMinaMonitor to set
     */
    public void setEnableMinaMonitor(boolean enableMinaMonitor) {
        this.enableMinaMonitor = enableMinaMonitor;
    }

    public void setMinaPollInterval(int minaPollInterval) {
        this.minaPollInterval = minaPollInterval;
    }

    /**
     * @param readerIdleTime
     *            the readerIdleTime to set
     */
    public void setReaderIdleTime(int readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
    }

    /**
     * Returns all the bound addresses and ports as string.
     * 
     * @return addresses
     */
    public String getAddress() {
        return addresses.toString();
    }

    /**
     * Returns the current statistics as a json formatted string.
     * 
     * @return json
     */
    public String getStatistics() {
        StringBuilder json = new StringBuilder("[Statistics{");
        if (stats != null) {
            // returns the cumulative number of sessions which were managed (or are being managed) by this service, which means 'currently managed session count + closed session count'
            json.append("cumulativeManagedSessionCount=");
            json.append(stats.getCumulativeManagedSessionCount());
            json.append(',');
            // returns the maximum number of sessions which were being managed at the same time
            json.append("largestManagedSessionCount=");
            json.append(stats.getLargestManagedSessionCount());
            json.append(',');
            // returns the maximum of the readBytesThroughput
            json.append("largestReadBytesThroughput=");
            json.append(stats.getLargestReadBytesThroughput());
            json.append(',');
            // returns the maximum of the readMessagesThroughput
            json.append("largestReadMessagesThroughput=");
            json.append(stats.getLargestReadMessagesThroughput());
            json.append(',');
            // returns the maximum of the writtenBytesThroughput
            json.append("largestWrittenBytesThroughput=");
            json.append(stats.getLargestWrittenBytesThroughput());
            json.append(',');
            // returns the maximum of the writtenMessagesThroughput
            json.append("largestWrittenMessagesThroughput=");
            json.append(stats.getLargestWrittenMessagesThroughput());
            json.append(',');
            // returns the time in millis when I/O occurred lastly
            json.append("lastIoTime=");
            json.append(stats.getLastIoTime());
            json.append(',');
            // returns the time in millis when read operation occurred lastly
            json.append("lastReadTime=");
            json.append(stats.getLastReadTime());
            json.append(',');
            // returns the time in millis when write operation occurred lastly
            json.append("lastWriteTime=");
            json.append(stats.getLastWriteTime());
            json.append(',');
            // returns the number of bytes read by this service
            json.append("readBytes=");
            json.append(stats.getReadBytes());
            json.append(',');
            // returns the number of read bytes per second
            json.append("readBytesThroughput=");
            json.append(stats.getReadBytesThroughput());
            json.append(',');
            // returns the number of messages this services has read
            json.append("readMessages=");
            json.append(stats.getReadMessages());
            json.append(',');
            // returns the number of read messages per second
            json.append("readMessagesThroughput=");
            json.append(stats.getReadMessagesThroughput());
            json.append(',');
            // returns the count of bytes scheduled for write
            json.append("scheduledWriteBytes=");
            json.append(stats.getScheduledWriteBytes());
            json.append(',');
            // returns the count of messages scheduled for write
            json.append("scheduledWriteMessages=");
            json.append(stats.getScheduledWriteMessages());
            json.append(',');
            // returns the interval (seconds) between each throughput calculation
            json.append("throughputCalculationInterval=");
            json.append(stats.getThroughputCalculationInterval());
            json.append(',');
            // returns the interval (milliseconds) between each throughput calculation
            json.append("throughputCalculationIntervalInMillis=");
            json.append(stats.getThroughputCalculationIntervalInMillis());
            json.append(',');
            // returns the number of bytes written out by this service
            json.append("writtenBytes=");
            json.append(stats.getWrittenBytes());
            json.append(',');
            // returns the number of written bytes per second
            json.append("writtenBytesThroughput=");
            json.append(stats.getWrittenBytesThroughput());
            json.append(',');
            // returns the number of messages this service has written
            json.append("writtenMessages=");
            json.append(stats.getWrittenMessages());
            json.append(',');
            // returns the number of written messages per second
            json.append("writtenMessagesThroughput=");
            json.append(stats.getWrittenMessagesThroughput());
            json.append("}]");
        }
        return json.toString();
    }

    public String toString() {
        return String.format("RTMP Mina Transport %s", addresses.toString());
    }

}
