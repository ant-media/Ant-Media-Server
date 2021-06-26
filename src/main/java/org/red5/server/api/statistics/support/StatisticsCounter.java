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

package org.red5.server.api.statistics.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts numbers used by the statistics. Keeps track of current, maximum and total numbers.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class StatisticsCounter {

    /** Current number. */
    private AtomicInteger current = new AtomicInteger();

    /** Total number. */
    private AtomicInteger total = new AtomicInteger();

    /** Maximum number. */
    private AtomicInteger max = new AtomicInteger();

    /**
     * Increment statistics by one.
     */
    public void increment() {
        total.incrementAndGet();
        max.compareAndSet(current.intValue(), current.incrementAndGet());
    }

    /**
     * Decrement statistics by one.
     */
    public void decrement() {
        current.decrementAndGet();
    }

    /**
     * Get current number.
     * 
     * @return current number
     */
    public int getCurrent() {
        return current.intValue();
    }

    /**
     * Get total number.
     * 
     * @return total
     */
    public int getTotal() {
        return total.intValue();
    }

    /**
     * Get maximum number.
     * 
     * @return max
     */
    public int getMax() {
        return max.intValue();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "StatisticsCounter [current=" + current + ", total=" + total + ", max=" + max + "]";
    }

}
