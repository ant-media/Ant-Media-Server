/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.logging;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerbyLogInterceptor {

    protected static Logger log = LoggerFactory.getLogger(DerbyLogInterceptor.class);

    private static ThreadLocal<StringBuilder> local = new ThreadLocal<>();

    public static OutputStream handleDerbyLogFile() {
        return new OutputStream() {

            @Override
            public void write(byte[] b) throws IOException {
                log.info("Derby log: {}", new String(b));
            }

            @Override
            public void write(int i) throws IOException {
                StringBuilder sb = local.get();
                if (sb == null) {
                    sb = new StringBuilder();
                }
                //look for LF
                if (i == 10) {
                    log.info("Derby log: {}", sb.toString());
                    sb.delete(0, sb.length() - 1);
                } else {
                    log.trace("Derby log: {}", i);
                    sb.append(new String(intToDWord(i)));
                }
                local.set(sb);
            }
        };
    }

    private static byte[] intToDWord(int i) {
        byte[] dword = new byte[4];
        dword[0] = (byte) (i & 0x00FF);
        dword[1] = (byte) ((i >> 8) & 0x000000FF);
        dword[2] = (byte) ((i >> 16) & 0x000000FF);
        dword[3] = (byte) ((i >> 24) & 0x000000FF);
        return dword;
    }

}
