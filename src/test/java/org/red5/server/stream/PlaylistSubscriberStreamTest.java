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

package org.red5.server.stream;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.server.Context;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.scope.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "PlaylistSubscriberStreamTest.xml" })
public class PlaylistSubscriberStreamTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(PlaylistSubscriberStreamTest.class);

	private static PlaylistSubscriberStream pss;

	@Before
	public void setUp() throws Exception {
		System.out.println("setUp");
		//
		System.setProperty("red5.deployment.type", "junit");
		if (pss == null) {
			pss = (PlaylistSubscriberStream) applicationContext.getBean("playlistSubscriberStream");
			Context ctx = new Context();
			ctx.setApplicationContext(applicationContext);
			Scope scope = new DummyScope();
			scope.setName("");
			scope.setContext(ctx);
			pss.setScope(scope);
			//
			ISchedulingService schedulingService = (ISchedulingService) applicationContext.getBean(ISchedulingService.BEAN_NAME);
			IConsumerService consumerService = (IConsumerService) applicationContext.getBean(IConsumerService.KEY);
			IProviderService providerService = (IProviderService) applicationContext.getBean(IProviderService.BEAN_NAME);
			//create and get the engine
			PlayEngine engine = pss.createEngine(schedulingService, consumerService, providerService);
			//mock the message output
			engine.setMessageOut(new DummyMessageOut());
			// add an item
			SimplePlayItem item = SimplePlayItem.build("h264_mp3.flv");
			pss.addItem(item);
		}
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("tearDown");
		pss = null;
	}

	@Test
	public void testStart() {
		System.out.println("testStart");
		pss.nextItem();
		try {
			pss.start();
		} catch (IllegalStateException ex) {
		}
	}

	@Test
	public void testPlay() throws Exception {
		System.out.println("testPlay");
		pss.play();
	}

	@Test
	public void testQPause() throws IOException {
		System.out.println("testPause - items: " + pss.getItemSize());
		pss.setItem(0);
		//pss.start();
		pss.play();
		long sent = pss.getBytesSent();
		pss.getCurrentItem().getName();
		pss.pause((int) sent);
	}

	@Test
	public void testResume() {
		System.out.println("testResume");
		long sent = pss.getBytesSent();
		pss.resume((int) sent);
	}

	@Test
	public void testSeek() {
		System.out.println("testSeek");
		long sent = pss.getBytesSent();
		try {
			pss.seek((int) (sent * 2));
		} catch (OperationNotSupportedException e) {
			log.warn("Exception {}", e);
		}
	}

	@Test
	public void testAddItemIPlayItem() {
		System.out.println("testAddItemIPlayItem");
		SimplePlayItem item = SimplePlayItem.build("h264_speex.flv");
		pss.addItem(item);
	}

	@Test
	public void testSetItem() {
		pss.setItem(0);
	}

	@Test
	public void testNextItem() {
		pss.nextItem();
	}

	@Test
	public void testPreviousItem() {
		pss.previousItem();
	}

	@Test
	public void zzStop() {
		System.out.println("testStop");
		pss.stop();
	}

	@Test
	public void zzzClose() {
		System.out.println("testClose");
		pss.close();
	}

	private class DummyScope extends Scope {
		
		DummyScope() {
			super(new Scope.Builder(null, ScopeType.ROOM, "dummy", false));
		}
		
	}

	private class DummyMessageOut implements IMessageOutput {

		public List<IProvider> getProviders() {
			System.out.println("getProviders");
			return null;
		}

		public void pushMessage(IMessage message) throws IOException {
			System.out.println("pushMessage: " + message);
		}

		public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
			System.out.println("sendOOBControlMessage");
		}

		public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
			System.out.println("subscribe");
			return true;
		}

		public boolean unsubscribe(IProvider provider) {
			System.out.println("unsubscribe");
			return true;
		}
	}

}
