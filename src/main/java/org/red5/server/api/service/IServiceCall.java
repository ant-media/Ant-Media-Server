/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

package org.red5.server.api.service;

/**
 * Container for a Service Call 
 */
public interface IServiceCall {

	/**
	 * Whether call was successful or not
	 * 
	 * @return	<code>true</code> on success, <code>false</code> otherwise
	 */
	public abstract boolean isSuccess();

	/**
	 * Returns service method name 
	 * 
	 * @return	Service method name as string
	 */
	public abstract String getServiceMethodName();

	/**
	 * Returns service name
	 * 
	 * @return	Service name
	 */
	public abstract String getServiceName();

	/**
	 * Returns array of service method arguments
	 * 
	 * @return	array of service method arguments
	 */
	public abstract Object[] getArguments();

	/**
	 * Get service call status
	 * 
	 * @return	service call status
	 */
	public abstract byte getStatus();

	/**
	 * Returns the time stamp at which this object was deserialized.
	 * 
	 * @return the readTime
	 */
	public long getReadTime();

	/**
	 * Returns the time stamp at which this object was serialized.
	 * 
	 * @return the writeTime
	 */
	public long getWriteTime();
	
	/**
	 * Get service call exception
	 * 
	 * @return	service call exception
	 */
	public abstract Exception getException();

	/**
	 * Sets status
	 * 
	 * @param status Status as byte
	 */
	public abstract void setStatus(byte status);

	/**
	 * Sets exception
	 * 
	 * @param exception Call exception
	 */
	public abstract void setException(Exception exception);

}