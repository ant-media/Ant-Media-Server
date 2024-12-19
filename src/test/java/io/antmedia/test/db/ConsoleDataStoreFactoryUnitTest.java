package io.antmedia.test.db;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.datastore.MongoStore;
import io.antmedia.console.datastore.RedisStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;

public class ConsoleDataStoreFactoryUnitTest {

    private ConsoleDataStoreFactory consoleDataStoreFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Vertx vertx;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        consoleDataStoreFactory = new ConsoleDataStoreFactory();
        when(applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
    }

    @Test
    public void testSetAndGetDbName() {
        String dbName = "testDbName";
        consoleDataStoreFactory.setDbName(dbName);
        assertEquals("DB name should be set correctly", dbName, consoleDataStoreFactory.getDbName());
    }

    @Test
    public void testSetAndGetDbType() {
        String dbType = DataStoreFactory.DB_TYPE_MONGODB;
        consoleDataStoreFactory.setDbType(dbType);
        assertEquals("DB type should be set correctly", dbType, consoleDataStoreFactory.getDbType());
    }

    @Test
    public void testGetDataStoreMongoDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_MONGODB);
        consoleDataStoreFactory.setDbHost("127.0.0.1");
        consoleDataStoreFactory.setDbUser(null);
        consoleDataStoreFactory.setDbPassword("password");
        
        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull("DataStore should not be null", dataStore);
        assertTrue("DataStore should be of type MongoStore", dataStore instanceof MongoStore);
    }

    @Test
    public void testGetDataStoreMapDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_MAPDB);

        consoleDataStoreFactory.setApplicationContext(applicationContext);

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull("DataStore should not be null", dataStore);
        assertTrue("DataStore should be of type MapDBStore", dataStore instanceof MapDBStore);
        dataStore.clear();
        dataStore.close();
    }

    @Test
    public void testGetDataStoreRedisDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_REDISDB);
        consoleDataStoreFactory.setDbHost("redis://127.0.0.1:6379");

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull("DataStore should not be null", dataStore);
        assertTrue("DataStore should be of type RedisStore", dataStore instanceof RedisStore);
    }

    @Test
    public void testGetDataStoreUndefined() {
        consoleDataStoreFactory.setDbType("undefined_db_type");

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNull("DataStore should be null for undefined DB type", dataStore);
    }
}
