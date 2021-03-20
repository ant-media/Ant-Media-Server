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

package org.red5.daemon;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.red5.server.Bootstrap;
import org.red5.server.Shutdown;

/**
 * Launch the Engine from a variety of sources, either through a main() or invoked through Apache Daemon.
 */
public class EngineLauncher implements Daemon {

    private static EngineLauncher engineLauncherInstance = new EngineLauncher();

    private static AtomicBoolean stopped = new AtomicBoolean(false);

    private static String[] commandLineArgs;

    /**
     * The Java entry point.
     * 
     * @param args
     *            Command line arguments
     */
    public static void main(String[] args) {
        System.out.printf("Args: %s%n", Arrays.toString(args));
        // store the args
        commandLineArgs = args;
        // the main routine is only here so I can also run the app from the command line
        engineLauncherInstance.initialize();
        Scanner sc = new Scanner(System.in);
        // wait until receive stop command from keyboard
        System.out.printf("Enter 'stop' to halt: ");
        while (!sc.nextLine().toLowerCase().equals("stop"))
            ;
        if (!stopped.get()) {
            engineLauncherInstance.terminate();
        }
        sc.close();
    }

    /**
     * Static methods called by prunsrv to start/stop the Windows service. Pass the argument "start" to start the service, and pass "stop"
     * to stop the service.
     *
     * Taken lock, stock and barrel from Christopher Pierce's blog at http://blog.platinumsolutions.com/node/234
     *
     * @param args
     *            Arguments from prunsrv command line
     */
    public static void windowsService(String args[]) {
        String cmd = "start";
        if (args.length > 0) {
            cmd = args[0];
        }
        if ("start".equals(cmd)) {
            engineLauncherInstance.windowsStart();
        } else {
            commandLineArgs = args;
            engineLauncherInstance.windowsStop();
        }
    }

    public void windowsStart() {
        System.out.println("windowsStart called");
        initialize();
        while (!stopped.get()) {
            // don't return until stopped
            synchronized (this) {
                try {
                    this.wait(60000); // wait 1 minute and check if stopped
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public void windowsStop() {
        System.out.println("windowsStop called");
        terminate();
        synchronized (this) {
            // stop the start loop
            this.notify();
        }
    }

    // Implementing the Daemon interface is not required for Windows but is for Linux
    @Override
    public void init(DaemonContext ctx) throws Exception {
        System.out.println("Daemon init");
        // store the args
        commandLineArgs = ctx.getArguments();
        System.out.printf("Args: %s%n", Arrays.toString(commandLineArgs));
    }

    @Override
    public void start() {
        System.out.println("Daemon start");
        initialize();
    }

    @Override
    public void stop() {
        System.out.println("Daemon stop");
        terminate();
    }

    @Override
    public void destroy() {
        System.out.println("Daemon destroy");
    }

    /**
     * Do the work of starting the engine
     */
    private void initialize() {
        if (!stopped.get()) {
            System.out.printf("Starting Red5 with args: %s%n", Arrays.toString(commandLineArgs));
            // start
            try {
                Bootstrap.main(commandLineArgs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cleanly stop the engine.
     */
    public void terminate() {
        if (stopped.compareAndSet(false, true)) {
            System.out.printf("Stopping Red5 with args: %s%n", Arrays.toString(commandLineArgs));
            if (commandLineArgs == null || commandLineArgs.length < 2) {
                // there should be 40+ characters worth of args (port + token)
                // use the default port of 9999
                String port = "9999";
                if (commandLineArgs.length >= 1) {
                    port = commandLineArgs[0];
                }
                // read the token from the file
                String token = "cafebeef";
                try {
                    File tokenFile = Paths.get("shutdown.token").toFile();
                    RandomAccessFile raf = new RandomAccessFile(tokenFile, "r");
                    byte[] buf = new byte[36];
                    raf.readFully(buf);
                    token = new String(buf);
                    System.out.printf("Token loaded: %s%n", token);
                    raf.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // set the args
                commandLineArgs = new String[] { port, token };
            }
            // shutdown
            Shutdown.main(commandLineArgs);
            System.out.println("Red5 stopped");
        }
    }
}