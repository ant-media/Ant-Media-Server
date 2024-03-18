package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.VoDRestService;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.security.ExpireStreamPublishSecurity;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.vertx.core.Vertx;

public class DataStoreFactoryUnitTest {
	private DataStoreFactory dsf;
	Vertx vertx = Vertx.vertx();

	@Before
	public void before() 
	{
		deleteMapDB();
		dsf =  new DataStoreFactory();
		dsf.setDbName("myDB");
		dsf.setDbHost("127.0.0.1");
		dsf.setDbUser(null);
		dsf.setDbPassword("myPass");
		dsf.setDbType("memorydb");
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		Mockito.when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());		
		Mockito.when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		dsf.setApplicationContext(context);
		dsf.setDataStore(null);
	}

	@After
	public void after() {
		deleteMapDB();
	}
	private void deleteMapDB() {
		File f = new File("myDB.db");
		if (f.exists()) {
			f.delete();
		}

	}


	@Test
	public void testDBCreation() {
		dsf.setDbType("memorydb");
		try {
			dsf.init();

			assertTrue(dsf.getDataStore() instanceof InMemoryDataStore);

			dsf.setDataStore(null);
			dsf.setDbType("mapdb");
			
			dsf.init();
			assertTrue(dsf.getDataStore() instanceof MapDBStore);

			dsf.setDataStore(null);

			dsf.setDbType("mongodb");
			dsf.init();
			assertTrue(dsf.getDataStore() instanceof MongoStore);

			dsf.setDataStore(null);

			dsf.setDbType("dummy");
			dsf.init();
			assertNull(dsf.getDataStore());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testForUsedClases() {
		dsf.setDbType("memorydb");
		dsf.init();
		DataStore datastore = dsf.getDataStore();

		AcceptOnlyStreamsInDataStore aosid = new AcceptOnlyStreamsInDataStore();
		aosid.setDataStoreFactory(dsf);
		assertEquals(datastore, aosid.getDatastore());

		BroadcastRestService brs = new BroadcastRestService();
		brs.setDataStoreFactory(dsf);
		assertEquals(datastore, brs.getDataStore());

		ExpireStreamPublishSecurity esps = new ExpireStreamPublishSecurity();
		esps.setDataStoreFactory(dsf);
		assertEquals(datastore, esps.getDatastore());

		VoDRestService ssrs = new VoDRestService();
		ssrs.setDataStoreFactory(dsf);
		assertEquals(datastore, ssrs.getDataStore());

		AntMediaApplicationAdapter amaa = new AntMediaApplicationAdapter();
		amaa.setDataStoreFactory(dsf);
		assertEquals(datastore, amaa.getDataStore());

		HlsViewerStats hvs = new HlsViewerStats();
		hvs.setDataStoreFactory(dsf);
		assertEquals(datastore, hvs.getDataStore());
		
		DashViewerStats dvs = new DashViewerStats();
		dvs.setDataStoreFactory(dsf);
		assertEquals(datastore, dvs.getDataStore());

	}

}
