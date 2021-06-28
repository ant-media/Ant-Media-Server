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

package org.red5.server.scheduling;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Scheduled job that is registered in the Quartz scheduler.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class QuartzSchedulingServiceJob extends QuartzJobBean {

    private Logger log = LoggerFactory.getLogger(QuartzSchedulingServiceJob.class);

    /**
     * Scheduling service constant
     */
    protected static final String SCHEDULING_SERVICE = "scheduling_service";

    /**
     * Scheduled job constant
     */
    protected static final String SCHEDULED_JOB = "scheduled_job";

    /**
     * Job data map
     */
    private JobDataMap jobDataMap;

    public void setJobDataMap(JobDataMap jobDataMap) {
        log.debug("Set job data map: {}", jobDataMap);
        this.jobDataMap = jobDataMap;
    }

    public void execute() {
        log.debug("execute");
        ISchedulingService service = null;
        IScheduledJob job = null;
        try {
            service = (ISchedulingService) jobDataMap.get(SCHEDULING_SERVICE);
            job = (IScheduledJob) jobDataMap.get(SCHEDULED_JOB);
            job.execute(service);
        } catch (Throwable e) {
            if (job == null) {
                log.error("Job not found");
            } else {
                log.error("Job {} execution failed", job.toString(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        log.debug("execute: {}", executionContext);
        ISchedulingService service = null;
        IScheduledJob job = null;
        try {
            JobDetail jobDetail = executionContext.getJobDetail();
            JobDataMap dataMap = jobDetail.getJobDataMap();
            service = (ISchedulingService) dataMap.get(SCHEDULING_SERVICE);
            job = (IScheduledJob) dataMap.get(SCHEDULED_JOB);
            job.execute(service);
        } catch (Throwable e) {
            if (job == null) {
                log.error("Job not found");
            } else {
                log.error("Job {} execution failed", job.toString(), e);
            }
        }
    }

}
