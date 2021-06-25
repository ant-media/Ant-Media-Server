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

package org.red5.server.net.rtmp;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains queue of tasks for processing messages in the specified channel. Ensures that all messages which has got in channel will be processed sequentially.
 *
 * @author Maria Chabanets (m.e.platova@gmail.com)
 */
public class ReceivedMessageTaskQueue {

    private final static Logger log = LoggerFactory.getLogger(ReceivedMessageTaskQueue.class);

    /**
     * Stream id.
     */
    private final int streamId;

    /**
     * Task queue.
     */
    private final Queue<ReceivedMessageTask> tasks = new ConcurrentLinkedQueue<ReceivedMessageTask>();

    /**
     * Listener which tries to process message from queue if queue has been changed.
     */
    private final IReceivedMessageTaskQueueListener listener;

    public ReceivedMessageTaskQueue(int streamId, IReceivedMessageTaskQueueListener listener) {
        this.streamId = streamId;
        this.listener = listener;
    }

    /**
     * Adds new task to the end of the queue.
     *
     * @param task
     *            received message task
     */
    public void addTask(ReceivedMessageTask task) {
        tasks.add(task);
        Packet packet = task.getPacket();
        // don't run the deadlock guard if timeout is <= 0
        if (packet.getExpirationTime() > 0L) {
            // run a deadlock guard so hanging tasks will be interrupted
            task.runDeadlockFuture(new DeadlockGuard(task));
        }
        if (listener != null) {
            listener.onTaskAdded(this);
        }
    }

    /**
     * Removes the specified task from the queue.
     *
     * @param task
     *            received message task
     */
    public void removeTask(ReceivedMessageTask task) {
        if (tasks.remove(task)) {
            task.cancelDeadlockFuture();
            if (listener != null) {
                listener.onTaskRemoved(this);
            }
        }
    }

    /**
     * Gets first task from queue if it can be processed. If first task is already in process it returns null.
     *
     * @return task that can be processed or null otherwise
     */
    public ReceivedMessageTask getTaskToProcess() {
        ReceivedMessageTask task = tasks.peek();
        if (task != null && task.setProcessing()) {
            return task;
        }
        return null;
    }

    /**
     * Removes all tasks from the queue.
     */
    public void removeAllTasks() {
        for (ReceivedMessageTask task : tasks) {
            task.cancelDeadlockFuture();
        }
        tasks.clear();
    }

    public int getStreamId() {
        return streamId;
    }

    /**
     * Prevents deadlocked message handling.
     */
    private class DeadlockGuard implements Runnable {

        private final ReceivedMessageTask task;

        /**
         * Creates the deadlock guard to prevent a message task from taking too long to setProcessing.
         *
         * @param task
         */
        private DeadlockGuard(ReceivedMessageTask task) {
            this.task = task;
            if (log.isTraceEnabled()) {
                log.trace("DeadlockGuard is created for {}", task);
            }
        }

        /**
         * Save the reference to the task, and wait until the maxHandlingTimeout has elapsed. If it elapsed, remove task and stop its thread.
         * */
        public void run() {
            Packet packet = task.getPacket();
            if (log.isTraceEnabled()) {
                log.trace("DeadlockGuard is started for {}", task);
            }
            // skip processed packet
            if (packet.isProcessed()) {
                log.debug("DeadlockGuard skipping task for processed packet {}", task);
            } else if (packet.isExpired()) {
                // try to interrupt thread
                log.debug("DeadlockGuard skipping task for expired packet {}", task);
            } else {
                // if the message task is not yet done or is not expired interrupt
                // if the task thread hasn't been interrupted check its live-ness
                // if the task thread is alive, interrupt it
                Thread taskThread = task.getTaskThread();
                if (taskThread == null) {
                    log.debug("Task has not start yet {}", task);
                } else if (!taskThread.isInterrupted() && taskThread.isAlive()) {
                    log.warn("Interrupting unfinished active task {}", task);
                    taskThread.interrupt();
                } else {
                    log.debug("Unfinished task {} already interrupted", task);
                }
            }
            // remove this task from the queue in any case
            removeTask(task);
        }
    }
}
