package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.DBReader;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.security.ExpireStreamPublishSecurity;
import io.antmedia.security.TokenService;
import io.antmedia.statistic.HlsViewerStats;

public class DataStoreFactoryUnitTest {
	private DataStoreFactory dsf;

	@Before
	public void before() {
		deleteMapDB();
		dsf =  new DataStoreFactory();
		dsf.setAppName("myApp");
		dsf.setDbName("myDB");
		dsf.setDbHost("localhost");
		dsf.setDbUser("me");
		dsf.setDbPassword("myPass");
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
    	assertTrue(dsf.getDataStore() instanceof InMemoryDataStore);
    	
    	dsf.setDataStore(null);
    	dsf.setDbType("mapdb");
    	assertTrue(dsf.getDataStore() instanceof MapDBStore);
    	
    	dsf.setDataStore(null);
    	
    	dsf.setDbType("mongodb");
    	assertTrue(dsf.getDataStore() instanceof MongoStore);
    	
    	dsf.setDataStore(null);
    	
    	dsf.setDbType("dummy");
    	assertNull(dsf.getDataStore());
    }
    
    @Test
    public void testForUsedClases() {
    	dsf.setDbType("memorydb");
    	IDataStore datastore = dsf.getDataStore();
    	
    	AcceptOnlyStreamsInDataStore aosid = new AcceptOnlyStreamsInDataStore();
    	aosid.setDataStoreFactory(dsf);
    	assertEquals(datastore, aosid.getDatastore());
    	
    	BroadcastRestService brs = new BroadcastRestService();
    	brs.setDataStoreFactory(dsf);
    	assertEquals(datastore, brs.getDataStore());
    	
    	ExpireStreamPublishSecurity esps = new ExpireStreamPublishSecurity();
    	esps.setDataStoreFactory(dsf);
    	assertEquals(datastore, esps.getDatastore());
    	
    	StreamsSourceRestService ssrs = new StreamsSourceRestService();
    	ssrs.setDataStoreFactory(dsf);
    	assertEquals(datastore, ssrs.getStore());
    	
    	AntMediaApplicationAdapter amaa = new AntMediaApplicationAdapter();
    	amaa.setDataStoreFactory(dsf);
    	assertEquals(datastore, amaa.getDataStore());
    	
    	HlsViewerStats hvs = new HlsViewerStats();
    	hvs.setDataStoreFactory(dsf);
    	assertEquals(datastore, hvs.getDataStore());
    	
    	TokenService ts = new TokenService();
    	ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(dsf);
		ts.setApplicationContext(context);
		assertEquals(datastore, ts.getDataStore());
    }
    
    
    @Test
    public void testDBReader() {
    	dsf.setDbType("memorydb");
    	IDataStore datastore = dsf.getDataStore();
    	
    	String host = DBReader.instance.getHost("myStream", "myApp");
    	assertNull(host);
    	
    	Broadcast broadcast = new Broadcast();
    	try {
			broadcast.setStreamId("myStream");
			broadcast.setOriginAdress("1.1.1.1");
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	datastore.save(broadcast);
    	
    	host = DBReader.instance.getHost("myStream", "myApp");

    	assertTrue(host.contentEquals("1.1.1.1"));
    }
    
}
