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

import java.util.Arrays;

/**
 * Thrown if service method is not found so call throws exception
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class MethodNotFoundException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7559230924102506068L;

    /**
     * Creates exception with given method name
     * 
     * @param methodName
     *            Service method name that can't be found
     */
    public MethodNotFoundException(String methodName) {
        super("Method " + methodName + " without arguments not found");
    }

    /**
     * Creates exception with given method name and arguments
     * 
     * @param methodName
     *            Service method name that can't be found
     * @param args
     *            Arguments given
     */
    public MethodNotFoundException(String methodName, Object[] args) {
        super("Method " + methodName + " with arguments " + Arrays.asList(args) + " not found");
    }

}
