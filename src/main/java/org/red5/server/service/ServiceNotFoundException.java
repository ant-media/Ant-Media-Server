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

/**
 * Thrown when service can't be found thus remote call throws an exception
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class ServiceNotFoundException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7543755414829244027L;

    /** Name of service that doesn't exist. */
    private String serviceName;

    /**
     * Creates new exception with service name
     * 
     * @param serviceName
     *            Name of service that couldn't been found
     */
    public ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName);
        this.serviceName = serviceName;
    }

    /**
     * Get the name of the service that doesn't exist.
     * 
     * @return name of the service
     */
    public String getServiceName() {
        return serviceName;
    }

}
