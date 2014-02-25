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

package org.red5.server.api.stream;

import org.red5.server.api.scope.IScope;

/**
 * Interface for handlers that control access to stream publishing.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IStreamPublishSecurity {

	/**
	 * Check if publishing a stream with the given name is allowed.
	 * 
	 * @param scope Scope the stream is about to be published in.
	 * @param name Name of the stream to publish.
	 * @param mode Publishing mode.
	 * @return <code>True</code> if publishing is allowed, otherwise <code>False</code>
	 */
	public boolean isPublishAllowed(IScope scope, String name, String mode);

}
