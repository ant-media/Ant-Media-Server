/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

package org.red5.server.session;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.session.ISession;
import org.red5.server.util.PropertyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages sessions.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static ConcurrentMap<String, ISession> sessions = new ConcurrentHashMap<String, ISession>();

    private static String destinationDirectory;

    private static Long maxLifetime;

    private static ISchedulingService schedulingService;

    // Create a random generator
    public static final Random rnd = new Random();

    public void init() {
        if (schedulingService != null) {
            // set to run once per hour
            schedulingService.addScheduledJob(3600000, new ReaperJob());
        } else {
            log.warn("Session reaper job was not scheduled");
        }
    }

    public static String getSessionId() {
        //random int from 1 - 100000
        int part1 = rnd.nextInt(99999) + 1;
        //thread-safe "long" part
        long part2 = ThreadLocalRandom.current().nextLong();
        //current time in millis
        long part3 = System.currentTimeMillis();
        //generate uuid-type id
        String sessionId = createHash(part1 + "-" + part2 + "-" + part3);
        log.debug("Session id created: {}", sessionId);
        return sessionId;
    }

    public static ISession createSession() {
        // create a new session
        return createSession(getSessionId());
    }

    public static ISession createSession(String sessionId) {
        // create a new session
        Session session = new Session(sessionId);
        session.setDestinationDirectory(destinationDirectory);
        // add to list
        sessions.put(sessionId, session);
        return session;
    }

    public static ISession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public static ISession removeSession(String sessionId) {
        return sessions.remove(sessionId);
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    public void setDestinationDirectory(String destinationDir) {
        log.debug("Setting session destination directory {}", destinationDir);
        SessionManager.destinationDirectory = destinationDir;
    }

    public void setMaxLifetime(String maxLifetime) {
        if (StringUtils.isNumeric(maxLifetime)) {
            SessionManager.maxLifetime = Long.valueOf(maxLifetime);
        } else {
            SessionManager.maxLifetime = PropertyConverter.convertStringToTimeMillis(maxLifetime);
        }
        log.debug("Max lifetime set to {} ms", SessionManager.maxLifetime);
    }

    public void setSchedulingService(ISchedulingService schedulingService) {
        SessionManager.schedulingService = schedulingService;
    }

    public static String createHash(String str) {
        return DigestUtils.md5Hex(str.getBytes());
    }

    /**
     * Quartz job to kill off old sessions
     */
    private final static class ReaperJob implements IScheduledJob {

        public ReaperJob() {
            log.debug("Creating job to remove stale sessions");
        }

        public void execute(ISchedulingService service) {
            log.debug("Reaper running...");
            if (sessions != null) {
                if (!sessions.isEmpty()) {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, ISession> entry : sessions.entrySet()) {
                        ISession session = entry.getValue();
                        long creationTime = session.getCreated();
                        // check if session life exceeds max lifetime
                        if (now - creationTime > SessionManager.maxLifetime) {
                            String key = session.getSessionId();
                            log.info("Reaper killing stale session: {}", key);
                            sessions.remove(key);
                            session.reset();
                            session = null;
                        }
                    }
                }
            }
        }

    }

}
