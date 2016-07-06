/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2014 by respective authors (see below). All rights reserved.
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

package org.red5.server.so;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.scope.WebScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * This is for testing SharedObject issues.
 * 
 * http://help.adobe.com/en_US/FlashMediaServer/3.5_SS_ASD/WS5b3ccc516d4fbf351e63e3d11a11afc95e-7e63.html
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "SharedObjectTest.xml" })
public class SharedObjectTest extends AbstractJUnit4SpringContextTests {

    protected static Logger log = LoggerFactory.getLogger(SharedObjectTest.class);

    private static ExecutorService executorService;

    private static WebScope appScope;

    private static List<SOClientWorker> tasks;

    @SuppressWarnings("unused")
    private String host = "localhost";

    @SuppressWarnings("unused")
    private String appPath = "junit";

    @SuppressWarnings("unused")
    private String roomPath = "/junit/room1";

    static {
        System.setProperty("red5.deployment.type", "junit");
    }

    @Before
    public void setUp() throws Exception {
        SharedObjectTest.executorService = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {
        SharedObjectTest.executorService.shutdownNow();
    }
    
    @Test
    public void testSharedObject() {
        log.debug("testSharedObject");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        String soName = "foo";

        //Room 1
        // /default/junit/room1
        assertNotNull(appScope.getScope("room1"));
        IScope room1 = appScope.getScope("room1");
        log.debug("Room 1: {}", room1);
        assertTrue(room1.getDepth() == 2);

        // get the SO
        ISharedObject sharedObject = app.getSharedObject(room1, soName, true);
        log.debug("SO: {}", sharedObject);
        assertNotNull(sharedObject);

        log.debug("testSharedObject-end");
    }

    @Test
    public void testGetSONames() throws Exception {
        log.debug("testGetSONames");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
        log.debug("Room 1 scope: {}", room1);
        Set<String> names = room1.getScopeNames();
        log.debug("Names: {}", names);
        assertTrue(names.size() > 0);
        log.debug("testGetSONames-end");
    }

    @Test
    public void zzzRemoveSO() throws Exception {
        log.debug("testRemoveSO");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        String soName = "foo";
        IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
        room1.removeChildren();
        log.debug("Child exists: {}", room1.hasChildScope(soName));

        log.debug("testRemoveSO-end");
    }

    /**
     * Test for Issue 209 http://code.google.com/p/red5/issues/detail?id=209
     */
    @Test
    public void testPersistentCreation() throws Exception {
        log.debug("testPersistentCreation");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        String soName = "foo";
        // get our room
        IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
        // create the SO
        app.createSharedObject(room1, soName, true);
        // get the SO
        ISharedObject sharedObject = app.getSharedObject(room1, soName, true);
        assertTrue(sharedObject != null);
        log.debug("testPersistentCreation-end");
    }

    @Test
    public void testDeepDirty() throws Throwable {
        log.debug("testDeepDirty");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
        }
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        try {
            // get our room
            IScope room = ScopeUtils.resolveScope(appScope, "/junit/room99");
            if (room != null) {
                // create the SO
                app.createSharedObject(room, "dirtySO", true);
                // test runnables represent clients
                int threads = 2;
                tasks = new ArrayList<SOClientWorker>(threads);
                for (int t = 0; t < threads; t++) {
                    tasks.add(new SOClientWorker(t, app, room));
                }
                // fires off threads
                long start = System.nanoTime();
                // invokeAll() blocks until all tasks have run...
                @SuppressWarnings("unused")
                List<Future<Object>> futures = executorService.invokeAll(tasks);
                System.out.println("Runtime: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
                for (SOClientWorker r : tasks) {
                    log.debug("Worker: {} shared object: {}", r.getId(), r.getSO().getAttributes());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.debug("testDeepDirty-end");
    }

    @Test
    public void testSharedObjectWithListener() {
        log.debug("testSharedObjectWithListener");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        app.initTSOwithListener();
        // go to sleep
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        // set something on the so
        ISharedObject so = app.getSharedObject(appScope, "statusSO");
        so.setAttribute("testing", true);
        // go to sleep
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        log.debug("Attribute names: {}", so.getAttributeNames());
        // [status, testing]
        assertTrue(so.getAttributeNames().size() == 2);
    }

    @Test
    public void testSharedObjectWithGetAndClose() {
        log.debug("testSharedObjectWithGetAndClose");
        if (appScope == null) {
            appScope = (WebScope) applicationContext.getBean("web.scope");
            log.debug("Application / web scope: {}", appScope);
            assertTrue(appScope.getDepth() == 1);
        }
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        app.getAndCloseSO();
        // go to sleep
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        // set something on the so
        assertFalse(app.hasSharedObject(appScope, "issue323"));
    }

    @Test
    public void testMissingHandler() throws Throwable {
        log.debug("testMissingHandler");
        String soName = "messager";
        SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
        assertTrue(appScope.hasHandler());
        IScope top = ScopeUtils.resolveScope(appScope, "/junit");
        assertTrue(top.hasHandler());
        IScope room = ScopeUtils.resolveScope(appScope, "/junit/room13");
        if (room == null) {
            assertTrue(top.createChildScope("room13"));
            room = ScopeUtils.resolveScope(appScope, "/junit/room13");
            assertNotNull(room);
        }
        assertTrue(room.hasHandler());
        // get rooms
        IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomA");
        if (room1 == null) {
            assertTrue(room.createChildScope("subroomA"));
            room1 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomA");
            assertNotNull(room1);
        }
        IScope room2 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomB");
        if (room2 == null) {
            assertTrue(room.createChildScope("subroomB"));
            room2 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomB");
            assertNotNull(room2);
        }
        Thread.sleep(100L);
        // create the SOs
        if (!app.hasSharedObject(room1, soName)) {
            app.createSharedObject(room1, soName, false);
        }
        assertNotNull(app.getSharedObject(room1, soName, false));
        if (!app.hasSharedObject(room2, soName)) {
            app.createSharedObject(room2, soName, false);
        }
        assertNotNull(app.getSharedObject(room2, soName, false));
        // test runnables represent clients
        tasks = new ArrayList<SOClientWorker>();
        tasks.add(new SOClientWorkerA(0, app, room));
        tasks.add(new SOClientWorkerB(1, app, room2));
        // fires off threads
        long start = System.nanoTime();
        // invokeAll() blocks until all tasks have run...
        @SuppressWarnings("unused")
        List<Future<Object>> futures = executorService.invokeAll(tasks);
        System.out.println("Runtime: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
        SOClientWorkerA soa = (SOClientWorkerA) tasks.get(0);
        log.debug("Worker: {} shared object: {}", soa.getId(), soa.getSO().getAttributes());
        SOClientWorkerB sob = (SOClientWorkerB) tasks.get(1);
        log.debug("Worker: {} shared object: {}", sob.getId(), sob.getSO().getAttributes());
        Thread.sleep(300L);
        log.debug("testMissingHandler-end");
    }

    // Used to ensure all the test-runnables are in "runTest" block.
    private static boolean allThreadsRunning() {
        for (SOClientWorker worker : tasks) {
            if (!((SOClientWorker) worker).isRunning()) {
                return false;
            }
        }
        return true;
    }

    private class SOClientWorker implements Callable<Object> {

        protected int id;

        protected ISharedObject so;

        private volatile boolean running = false;

        public SOClientWorker(int id, SOApplication app, IScope room) {
            this.id = id;
            if (app != null) {
                this.so = app.getSharedObject(room, "dirtySO", true);
                ISharedObjectListener listener = new SOListener(id);
                so.addSharedObjectListener(listener);
            }
        }

        @SuppressWarnings("unchecked")
        public Object call() throws Exception {
            log.debug("runTest#{}", id);
            running = true;
            do {
                Thread.sleep(100);
            } while (!allThreadsRunning());
            // create complex type object
            Complex complex = (Complex) so.getAttribute("complex");
            if (complex == null) {
                complex = new Complex();
                complex.getMap().put("myId", id);
                so.setAttribute("complex", complex);
            }
            Thread.sleep(500);
            log.debug("runTest-end#{}", id);
            running = false;
            return id;
        }

        public int getId() {
            return id;
        }

        public ISharedObject getSO() {
            return so;
        }

        public boolean isRunning() {
            return running;
        }
    }

    /** Used for handler test */
    private class SOClientWorkerA extends SOClientWorker {

        private IScope room;

        public SOClientWorkerA(int id, SOApplication app, IScope room) {
            super(id, null, null);
            this.room = room;
            this.so = app.getSharedObject(room, "messager", false);
            ISharedObjectListener listener = new SOListener(id);
            so.addSharedObjectListener(listener);
        }

        public Object call() throws Exception {
            log.debug("runTest#{}", id);
            Thread.sleep(50);
            so.setAttribute("client-id", id);
            // sleep 100 ms for client A
            Thread.sleep(50);
            assertTrue(so.getIntAttribute("client-id") == id);
            // remove the room we used for this client; hopefully this will cause the
            // handler "missing" error to surface
            room.removeChildScope(so);
            room.getParent().removeChildScope(room);
            log.debug("runTest-end#{}", id);
            return id;
        }

    }

    /** Used for handler test */
    private class SOClientWorkerB extends SOClientWorker {

        public SOClientWorkerB(int id, SOApplication app, IScope room) {
            super(id, null, null);
            this.so = app.getSharedObject(room, "messager", false);
            ISharedObjectListener listener = new SOListener(id);
            so.addSharedObjectListener(listener);
        }

        public Object call() throws Exception {
            log.debug("runTest#{}", id);
            Thread.sleep(50);
            so.setAttribute("client-id", id);
            // sleep 200 ms for client B
            Thread.sleep(200);
            assertTrue(so.getIntAttribute("client-id") == id);
            so.sendMessage("sendMessage", null);
            Thread.sleep(50);
            log.debug("runTest-end#{}", id);
            return id;
        }

    }

}
