package org.red5.server.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.api.stream.support.StreamUtils;
import org.red5.server.scope.WebScope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "../service/testcontext.xml" })
public class ServerStreamTest extends AbstractJUnit4SpringContextTests {

	private IServerStream serverStream;

	static {
		String userDir = System.getProperty("user.dir");
		System.out.println("User dir: " + userDir);
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "file:" + userDir + "/target/classes");
		System.setProperty("red5.config_root", "file:" + userDir + "/src/main/server/conf");
	}	
	
	@Before
	public void setUp() throws Exception {
		IScope scope = (WebScope) applicationContext.getBean("web.scope");
		serverStream = StreamUtils.createServerStream(scope, "test");
	}

	@After
	public void tearDown() throws Exception {
		serverStream.removeAllItems();
	}

	@Test
	public void testAddItemIPlayItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
		assertTrue(serverStream.getItemSize() == 2);
	}

	@Test
	public void testAddItemIPlayItemInt() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		SimplePlayItem item3 = SimplePlayItem.build("f3");
		serverStream.addItem(item3, 0);
		System.out.println("Items: " + ((ServerStream) serverStream).getItems());
		assertTrue(serverStream.getItemSize() == 3);
		assertTrue("f1".equals(serverStream.getItem(1).getName()));
	}

	@Test
	public void zzremoveItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		assertTrue(serverStream.getItemSize() == 2);
		serverStream.removeItem(0);
		assertTrue(serverStream.getItemSize() == 1);
	}

	@Test
	public void zzzremoveAllItems() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue(serverStream.getItemSize() == 1);
		serverStream.removeAllItems();
		assertTrue(serverStream.getItemSize() == 0);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
	}

	@Test
	public void testGetCurrentItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		serverStream.start();
		assertEquals(item, serverStream.getCurrentItem());
	}

	@Test
	public void testGetItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue("f1".equals(serverStream.getItem(0).getName()));
	}

	@Test
	public void testNextItem() {
		SimplePlayItem item = SimplePlayItem.build("h264_aac");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("h264_speex");
		serverStream.addItem(item2);
		//serverStream.start();
		System.out.printf("Play items: #1 %s #2 %s\n", serverStream.getItem(0).getName(), serverStream.getItem(1).getName());
		serverStream.nextItem();
		System.out.printf("Play items: #1 %s #2 %s\n", serverStream.getItem(0).getName(), serverStream.getItem(1).getName());
		assertEquals(1, serverStream.getCurrentItemIndex());
		try {
			serverStream.start();
			System.out.printf("Item name: %s", serverStream.getCurrentItem().getName());
			assertEquals("h264_speex", serverStream.getCurrentItem().getName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
