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

package org.red5.server.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.ContextLoader;
import org.red5.server.LoaderBase;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Server/service to perform orderly and controlled shutdown and clean up of Red5.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ShutdownServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    private Logger log = Red5LoggerFactory.getLogger(ShutdownServer.class);

    /**
     * Port to which the server listens for shutdown requests. Default is 9999.
     */
    private int port = 9999;

    /**
     * Delay or wait time in seconds before exiting.
     */
    private int shutdownDelay = 30;

    /**
     * Name for the file containing the shutdown token
     */
    private String shutdownTokenFileName = "shutdown.token";

    /**
     * Spring Application context
     */
    private ApplicationContext applicationContext;

    /**
     * Red5 core context
     */
    private ApplicationContext coreContext;

    /**
     * Red5 core context
     */
    private ApplicationContext commonContext;

    /**
     * Red5 context loader
     */
    private ContextLoader contextLoader;

    // random token to verify shutdown request is genuine
    private final String token = UUID.randomUUID().toString();

    // whether the server is shutdown
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    // single thread executor for the internal startup / server
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // reference to the runnable
    private Future<?> future;
    
    // reference to the jee server
    private LoaderBase jeeServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // check for an embedded jee server
            jeeServer = applicationContext.getBean(LoaderBase.class);
            // lookup the jee container
            if (jeeServer == null) {
                log.info("JEE server was not found");
            } else {
                log.info("JEE server was found: {}", jeeServer.toString());
            }
        } catch (Exception e) {
            
        }
        // start blocks, so it must be on its own thread
        future = executor.submit(new Runnable(){
            public void run() {
                start();
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        shutdownOrderly();
        future.cancel(true);
    }

    /**
     * Starts internal server listening for shutdown requests.
     */
    public void start() {
        // dump to stdout
        System.out.printf("Token: %s%n", token);
        // write out the token to a file so that red5 may be shutdown external to this VM instance.
        try {
            // delete existing file
            Files.deleteIfExists(Paths.get(shutdownTokenFileName));
            // write to file
            Path path = Files.createFile(Paths.get(shutdownTokenFileName));
            File tokenFile = path.toFile();
            RandomAccessFile raf = new RandomAccessFile(tokenFile, "rws");
            raf.write(token.getBytes());
            raf.close();
        } catch (Exception e) {
            log.warn("Exception handling token file", e);
        }
        while (!shutdown.get()) {
            try (
                    ServerSocket serverSocket = new ServerSocket(port); 
                    Socket clientSocket = serverSocket.accept(); 
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 ) {
                log.info("Connected - local: {} remote: {}", clientSocket.getLocalSocketAddress(), clientSocket.getRemoteSocketAddress());
                String inputLine = in.readLine();
                if (inputLine != null && token.equals(inputLine)) {
                    log.info("Shutdown request validated using token");
                    out.println("Ok");
                    shutdownOrderly();
                } else {
                    out.println("Bye");
                }
            } catch (BindException be) {
                log.error("Cannot bind to port: {}, ensure no other instances are bound or choose another port", port, be);
                shutdownOrderly();
            } catch (IOException e) {
                log.warn("Exception caught when trying to listen on port {} or listening for a connection", port, e);
            }
        }
    }

    private void shutdownOrderly() {
        // shutdown internal listener
        shutdown.compareAndSet(false, true);
        // shutdown the plug-in launcher
        try {
            log.debug("Attempting to shutdown plugin registry");
            PluginRegistry.shutdown();
        } catch (Exception e) {
            log.warn("Exception shutting down plugin registry", e);
        }
        // shutdown the context loader
        if (contextLoader != null) {
            log.debug("Attempting to shutdown context loader");
            contextLoader.shutdown();
            contextLoader = null;
        }
        // shutdown the jee server
        if (jeeServer != null) {
            // destroy is a DisposibleBean method not LoaderBase
            // jeeServer.destroy();
            jeeServer = null;
        }
        // attempt to kill the contexts
        final CountDownLatch latch = new CountDownLatch(3);
        new Thread(new Runnable() {
            public void run() {
                try {
                    log.debug("Attempting to close core context");
                    ((ConfigurableApplicationContext) coreContext).close();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                try {
                    log.debug("Attempting to close common context");
                    ((ConfigurableApplicationContext) commonContext).close();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                try {
                    log.debug("Attempting to close app context");
                    ((ConfigurableApplicationContext) applicationContext).close();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        try {
            if (latch.await(shutdownDelay, TimeUnit.SECONDS)) {
                log.info("Application contexts are closed");
            } else {
                log.info("One or more contexts didn't close in the allotted time");
            }
        } catch (InterruptedException e) {
            log.error("Exception attempting to close app contexts", e);
			e.printStackTrace();
			Thread.currentThread().interrupt();
        }
        // exit
        System.exit(0);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setShutdownDelay(int shutdownDelay) {
        this.shutdownDelay = shutdownDelay;
    }

    public void setShutdownTokenFileName(String shutdownTokenFileName) {
        this.shutdownTokenFileName = shutdownTokenFileName;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setCoreContext(ApplicationContext coreContext) {
        this.coreContext = coreContext;
    }

    public void setCommonContext(ApplicationContext commonContext) {
        this.commonContext = commonContext;
    }

    public void setContextLoader(ContextLoader contextLoader) {
        this.contextLoader = contextLoader;
    }

}
