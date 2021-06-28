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
package org.red5.server.util;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

/**
 * Converter for properties originating from properties files. Predetermined string formats are converted into other usable types such as timestamps.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class PropertyConverter {

    /**
     * Converts a string denoting an amount of time into milliseconds and adds it to the current date. Strings are expected to follow this form where # equals a digit: #M The following are permitted for denoting time: H = hours, M = minutes, S = seconds
     * 
     * @param time
     *            time
     * @return time in milliseconds
     */
    public static long convertStringToFutureTimeMillis(String time) {
        Calendar exp = Calendar.getInstance();
        if (time.endsWith("H")) {
            exp.add(Calendar.HOUR, Integer.valueOf(StringUtils.remove(time, 'H')));
        } else if (time.endsWith("M")) {
            exp.add(Calendar.MINUTE, Integer.valueOf(StringUtils.remove(time, 'M')));
        } else if (time.endsWith("S")) {
            exp.add(Calendar.MILLISECOND, Integer.valueOf(StringUtils.remove(time, 'S')) * 1000);
        }
        return exp.getTimeInMillis();
    }

    /**
     * Converts a string denoting an amount of time into seconds. Strings are expected to follow this form where # equals a digit: #M The following are permitted for denoting time: H = hours, M = minutes, S = seconds
     * 
     * @param time
     *            time
     * @return time in seconds
     */
    public static int convertStringToTimeSeconds(String time) {
        int result = 0;
        if (time.endsWith("H")) {
            int hoursToAdd = Integer.valueOf(StringUtils.remove(time, 'H'));
            result = (60 * 60) * hoursToAdd;
        } else if (time.endsWith("M")) {
            int minsToAdd = Integer.valueOf(StringUtils.remove(time, 'M'));
            result = 60 * minsToAdd;
        } else if (time.endsWith("S")) {
            int secsToAdd = Integer.valueOf(StringUtils.remove(time, 'S'));
            result = secsToAdd;
        }
        return result;
    }

    /**
     * Converts a string denoting an amount of time into milliseconds. Strings are expected to follow this form where # equals a digit: #M The following are permitted for denoting time: H = hours, M = minutes, S = seconds
     * 
     * @param time
     *            time
     * @return time in milliseconds
     */
    public static long convertStringToTimeMillis(String time) {
        long result = 0;
        if (time.endsWith("H")) {
            long hoursToAdd = Integer.valueOf(StringUtils.remove(time, 'H'));
            result = ((1000 * 60) * 60) * hoursToAdd;
        } else if (time.endsWith("M")) {
            long minsToAdd = Integer.valueOf(StringUtils.remove(time, 'M'));
            result = (1000 * 60) * minsToAdd;
        } else if (time.endsWith("S")) {
            long secsToAdd = Integer.valueOf(StringUtils.remove(time, 'S'));
            result = 1000 * secsToAdd;
        }
        return result;
    }

    /**
     * Converts a string denoting an amount of bytes into an integer value. Strings are expected to follow this form where # equals a digit: #M The following are permitted for denoting binary size: K = kilobytes, M = megabytes, G = gigabytes
     * 
     * @param memSize
     *            memory
     * @return size as an integer
     */
    public static int convertStringToMemorySizeInt(String memSize) {
        int result = 0;
        if (memSize.endsWith("K")) {
            result = Integer.valueOf(StringUtils.remove(memSize, 'K')) * 1000;
        } else if (memSize.endsWith("M")) {
            result = Integer.valueOf(StringUtils.remove(memSize, 'M')) * 1000 * 1000;
        } else if (memSize.endsWith("G")) {
            result = Integer.valueOf(StringUtils.remove(memSize, 'G')) * 1000 * 1000 * 1000;
        }
        return result;
    }

    /**
     * Converts a string denoting an amount of bytes into an long value. Strings are expected to follow this form where # equals a digit: #M The following are permitted for denoting binary size: K = kilobytes, M = megabytes, G = gigabytes
     * 
     * @param memSize
     *            memory size
     * @return size as an long
     */
    public static long convertStringToMemorySizeLong(String memSize) {
        long result = 0;
        if (memSize.endsWith("K")) {
            result = Long.valueOf(StringUtils.remove(memSize, 'K')) * 1000;
        } else if (memSize.endsWith("M")) {
            result = Long.valueOf(StringUtils.remove(memSize, 'M')) * 1000 * 1000;
        } else if (memSize.endsWith("G")) {
            result = Long.valueOf(StringUtils.remove(memSize, 'G')) * 1000 * 1000 * 1000;
        }
        return result;
    }

    /**
     * Quick time converter to keep our timestamps compatible with PHP's time() (seconds)
     * 
     * @return time in seconds
     */
    public static Integer getCurrentTimeSeconds() {
        return convertMillisToSeconds(System.currentTimeMillis());
    }

    /**
     * Quick time converter to keep our timestamps compatible with PHP's time() (seconds)
     * 
     * @param millis
     *            milliseconds
     * @return seconds
     */
    public static Integer convertMillisToSeconds(Long millis) {
        return Long.valueOf(millis / 1000).intValue();
    }

}
