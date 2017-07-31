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

package org.red5.server.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.service.IStreamableFileService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.api.stream.IStreamableFileFactory;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.scope.BasicScope;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.scope.Scope;
import org.red5.server.stream.provider.FileProvider;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;

public class ProviderService implements IProviderService {

	private static final Logger log = Red5LoggerFactory.getLogger(ProviderService.class);

	/** {@inheritDoc} */
	public INPUT_TYPE lookupProviderInput(IScope scope, String name, int type) {
		INPUT_TYPE result = INPUT_TYPE.NOT_FOUND;
		if (scope.getBasicScope(ScopeType.BROADCAST, name) != null) {
			// we have live input
			result = INPUT_TYPE.LIVE;
		} else {
			// "default" to VOD as a missing file will be picked up later on 
			//result = INPUT_TYPE.VOD;
			File file = getStreamFile(scope, name);
			if (file != null) {
				result = INPUT_TYPE.VOD;
			}
			else {
				
					if (type == -2) {
						result = INPUT_TYPE.LIVE_WAIT;
					}
					log.warn("Requested stream: {} does not appear to be of VOD type", name);

			}
		}
		return result;
	}

	/** {@inheritDoc} */
	public IMessageInput getProviderInput(IScope scope, String name) {
		IMessageInput msgIn = getLiveProviderInput(scope, name, false);
		if (msgIn == null) {
			return getVODProviderInput(scope, name);
		}
		return msgIn;
	}

	/** {@inheritDoc} */
	public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
		log.debug("Get live provider input for {} scope: {}", name, scope);
		//make sure the create is actually needed
		IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
		if (broadcastScope == null && needCreate) {
			synchronized (scope) {
				// re-check if another thread already created the scope
				broadcastScope = scope.getBroadcastScope(name);
				if (broadcastScope == null) {
					broadcastScope = new BroadcastScope(scope, name);
					scope.addChildScope(broadcastScope);
				}
			}
		}
		return broadcastScope;
	}

	/** {@inheritDoc} */
	public IMessageInput getVODProviderInput(IScope scope, String name) {
		log.debug("getVODProviderInput - scope: {} name: {}", scope, name);
		File file = getVODProviderFile(scope, name);
		if (file == null) {
			return null;
		}
		IPipe pipe = new InMemoryPullPullPipe();
		pipe.subscribe(new FileProvider(scope, file), null);
		return pipe;
	}

	/** {@inheritDoc} */
	public File getVODProviderFile(IScope scope, String name) {
		if (log.isDebugEnabled()) {
			log.debug("getVODProviderFile - scope: {} name: {}", scope, name);
		}
		File file = getStreamFile(scope, name);
		if (file == null || !file.exists()) {
			//if there is no file extension this is most likely a live stream
			if (name.indexOf('.') > 0) {
				log.info("File was null or did not exist: {}", name);
			} else {
				log.trace("VOD file {} was not found, may be live stream", name);
			}
		}
		return file;
	}

	/** {@inheritDoc} */
	public boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
		if (log.isDebugEnabled()) {
			log.debug("Registering - name: {} stream: {} scope: {}", new Object[] { name, bs, scope });
			((Scope) scope).dump();
		}
		IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
		if (broadcastScope == null) {
			log.debug("Creating a new scope");
			broadcastScope = new BroadcastScope(scope, name);
			if (scope.addChildScope(broadcastScope)) {
				log.debug("Broadcast scope added");
			} else {
				log.warn("Broadcast scope was not added to {}", scope);
			}
		}
		// set the client broadcast stream if we have a broadcast scope
		if (broadcastScope != null && bs instanceof IClientBroadcastStream) {
			broadcastScope.setClientBroadcastStream((IClientBroadcastStream) bs);
		}
		if (log.isDebugEnabled()) {
			log.debug("Subscribing scope {} to provider {}", broadcastScope, bs.getProvider());
		}
		return broadcastScope.subscribe(bs.getProvider(), null);
	}

	/** {@inheritDoc} */
	public Set<String> getBroadcastStreamNames(IScope scope) {
		return scope.getBasicScopeNames(ScopeType.BROADCAST);
	}

	/** {@inheritDoc} */
	public boolean unregisterBroadcastStream(IScope scope, String name) {
		return unregisterBroadcastStream(scope, name, null);
	}

	/** {@inheritDoc} */
	public boolean unregisterBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
		if (log.isDebugEnabled()) {
			log.debug("Unregistering - name: {} stream: {} scope: {}", new Object[] { name, bs, scope });
			((Scope) scope).dump();
		}
		IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
		if (bs != null) {
			log.debug("Unsubscribing scope {} from provider {}", broadcastScope, bs.getProvider());
			broadcastScope.unsubscribe(bs.getProvider());
		}
		// if the scope has no listeners try to remove it
		if (!((BasicScope) broadcastScope).hasEventListeners()) {
			if (log.isDebugEnabled()) {
				log.debug("Scope has no event listeners attempting removal");
			}
			scope.removeChildScope(broadcastScope);
		}
		// verify that scope was removed
		return scope.getBasicScope(ScopeType.BROADCAST, name) == null;
	}

	private File getStreamFile(IScope scope, String name) {
		if (log.isDebugEnabled()) {
			log.debug("getStreamFile - name: {}", name);
		}
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.class);
		String flvName = null;
		if (name.indexOf(':') == -1 && name.indexOf('.') == -1) {
			// Default to .mp4 files if no prefix and no extension is given.
			String origName = name;
			name = "mp4:" + origName;
			flvName = "flv:" + origName;
		}
		// ams sends an asterisk at the start of the name on mp4, so remove it
		if (name.charAt(0) == '*') {
			name = name.substring(1);
			if (log.isTraceEnabled()) {
				log.trace("Removed star prefix: {}", name);
			}
		}
		for (IStreamableFileService service : factory.getServices()) {
			if (name.startsWith(service.getPrefix() + ':')) {
				name = service.prepareFilename(name);
				break;
			}
		}
		// look for a custom filename gen class
		IStreamFilenameGenerator filenameGenerator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
		// get the filename
		String filename = filenameGenerator.generateFilename(scope, name, GenerationType.PLAYBACK);
		File file;
		try {
			// most likely case first
			if (!filenameGenerator.resolvesToAbsolutePath()) {
				try {
					file = scope.getContext().getResource(filename).getFile();
				} catch (FileNotFoundException e) {
					log.debug("File {} not found, nulling it", filename);
					file = null;
				}
			} else {
				file = new File(filename);
			}
			// check file existence
			if (file != null && !file.exists()) {
				// if it does not exist then null it out
				file = null;
			}
		} catch (IOException e) {
			log.info("Exception attempting to lookup file: {}", e.getMessage());
			if (log.isDebugEnabled()) {
				log.warn("Exception attempting to lookup file: {}", name, e);
			}
			// null out the file (fix for issue #238)
			file = null;
		}

		if (file == null && flvName != null) {
			//if file is null and flvName is not null, look up if flv file exists
			file = getStreamFile(scope, flvName);
		}
		return file;
	}

}
