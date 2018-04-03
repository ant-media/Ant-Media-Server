package com.antstreaming.rtsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class RtspConnectionManager implements IConnectionManager<RtspConnection>, ApplicationContextAware {

	private static Logger logger = LoggerFactory.getLogger(RtspConnectionManager.class);

	protected ConcurrentMap<String, RtspConnection> connMap = new ConcurrentHashMap<String, RtspConnection>();

	private static RtspConnectionManager instance;

	private static ApplicationContext applicationContext;

	protected AtomicInteger conns = new AtomicInteger();

	private ThreadPoolTaskScheduler taskScheduler;

	public static RtspConnectionManager getInstance() {
		if (instance == null) {
			logger.trace("Connection manager instance does not exist");
			if (applicationContext != null && applicationContext.containsBean("rtspConnectionManager")) {
				logger.trace("Connection manager bean exists");
				instance = (RtspConnectionManager) applicationContext.getBean("rtspConnectionManager");
			} else {
				logger.trace("Connection manager bean doesnt exist, creating new instance");
				instance = new RtspConnectionManager();
			}
		}
		return instance;
	}

	@Override
	public Collection<RtspConnection> getAllConnections() {
		ArrayList<RtspConnection> list = new ArrayList<RtspConnection>(connMap.size());
		list.addAll(connMap.values());
		return list;
	}

	@Override
	public Collection<RtspConnection> removeConnections() {
		ArrayList<RtspConnection> list = new ArrayList<>(connMap.size());
		list.addAll(connMap.values());
		connMap.clear();
		conns.set(0);
		return list;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		RtspConnectionManager.applicationContext = applicationContext;
		logger.debug("setting application context " + applicationContext.getApplicationName());
	}

	public ThreadPoolTaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		logger.debug("setting task scheduler");
	}

	@Override
	public RtspConnection getConnection(int clientId) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void setConnection(RtspConnection conn) {
		throw new UnsupportedOperationException("Not implemented");

	}

	@Override
	public RtspConnection getConnectionBySessionId(String sessionId) {
		logger.trace("Getting connection by session id: {}", sessionId);
		RtspConnection conn = connMap.get(sessionId);
		if (conn == null && logger.isDebugEnabled()) {
			logger.debug("Connection not found for {}", sessionId);
			if (logger.isTraceEnabled()) {
				logger.trace("Connections ({}) {}", connMap.size(), connMap.values());
			}
		}
		return conn;
	}

	@Override
	public RtspConnection createConnection(Class<?> connCls) {
		RtspConnection conn = null;
		try {
			// create connection
			conn = (RtspConnection) connCls.newInstance();
			connMap.put(conn.getSessionId(), conn);
			// add to local map
			logger.trace("Connections: {}", conns.incrementAndGet());
			// set the scheduler
			logger.debug("applicationContext.containsBean(rtmpScheduler) : " + applicationContext.containsBean("rtmpScheduler"));
			logger.debug("connection scheduler : "  + conn.getScheduler() + " manager scheduler:" + getTaskScheduler());
			conn.setScheduler(getTaskScheduler());
			logger.trace("Connection created: {}", conn);
		} catch (Exception ex) {
			logger.warn("Exception creating connection", ex);
		}
		return conn;
	}

	@Override
	public RtspConnection createConnection(Class<?> connCls, String sessionId) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public RtspConnection removeConnection(int clientId) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public RtspConnection removeConnection(String sessionId) {
		logger.trace("Removing connection with session id: {}", sessionId);
		if (logger.isTraceEnabled()) {
			logger.trace("Connections ({}) at pre-remove: {}", connMap.size(), connMap.values());
		}
		// remove from map
		RtspConnection conn = connMap.remove(sessionId);
		if (conn != null) {
			logger.trace("Connections: {}", conns.decrementAndGet());
			Red5.setConnectionLocal(null);
		}
		return conn;
	}

}
