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

package org.red5.server.api.scheduling;

import java.util.Date;
import java.util.List;

import org.red5.server.api.scope.IScopeService;

/**
 * Service that supports periodic execution of jobs, adding, removing and getting their name as list.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ISchedulingService extends IScopeService {

    public static String BEAN_NAME = "schedulingService";

    /**
     * Schedule a job for periodic execution.
     * 
     * @param interval
     *            time in milliseconds between two notifications of the job
     * @param job
     *            the job to trigger periodically
     * @return the name of the scheduled job
     */
    public String addScheduledJob(int interval, IScheduledJob job);

    /**
     * Schedule a job for single execution in the future. Please note that the jobs are not saved if Red5 is restarted in the meantime.
     * 
     * @param timeDelta
     *            time delta in milliseconds from the current date
     * @param job
     *            the job to trigger
     * @return the name of the scheduled job
     */
    public String addScheduledOnceJob(long timeDelta, IScheduledJob job);

    /**
     * Schedule a job for single execution at a given date. Please note that the jobs are not saved if Red5 is restarted in the meantime.
     * 
     * @param date
     *            date when the job should be executed
     * @param job
     *            the job to trigger
     * @return the name of the scheduled job
     */
    public String addScheduledOnceJob(Date date, IScheduledJob job);

    /**
     * Schedule a job for periodic execution which will start after the specifed delay.
     * 
     * @param interval
     *            time in milliseconds between two notifications of the job
     * @param job
     *            the job to trigger periodically
     * @param delay
     *            time in milliseconds to pass before first execution.
     * @return the name of the scheduled job
     */
    public String addScheduledJobAfterDelay(int interval, IScheduledJob job, int delay);

    /**
     * Pauses the trigger which initiates job execution.
     * 
     * @param name
     *            name of the job to stop
     */
    public void pauseScheduledJob(String name);

    /**
     * Resumes the trigger which initiates job execution.
     * 
     * @param name
     *            name of the job to stop
     */
    public void resumeScheduledJob(String name);

    /**
     * Stop executing a previously scheduled job.
     * 
     * @param name
     *            name of the job to stop
     */
    public void removeScheduledJob(String name);

    /**
     * Return names of scheduled jobs.
     * 
     * @return list of job names
     */
    public List<String> getScheduledJobNames();

}
