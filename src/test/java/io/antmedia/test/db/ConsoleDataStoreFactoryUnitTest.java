package io.antmedia.test.db;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        consoleDataStoreFactory = new ConsoleDataStoreFactory();
        when(applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
    }

    @Test
    public void testSetAndGetDbName() {
        String dbName = "testDbName";
        consoleDataStoreFactory.setDbName(dbName);
        assertEquals(dbName, consoleDataStoreFactory.getDbName(), "DB name should be set correctly");
    }

    @Test
    public void testSetAndGetDbType() {
        String dbType = DataStoreFactory.DB_TYPE_MONGODB;
        consoleDataStoreFactory.setDbType(dbType);
        assertEquals(dbType, consoleDataStoreFactory.getDbType(), "DB type should be set correctly");
    }

    @Test
    public void testGetDataStoreMongoDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_MONGODB);
        consoleDataStoreFactory.setDbHost("127.0.0.1");
        
        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull(dataStore, "DataStore should not be null");
        assertTrue(dataStore instanceof MongoStore, "DataStore should be of type MongoStore");
    }

    @Test
    public void testGetDataStoreMapDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_MAPDB);

        consoleDataStoreFactory.setApplicationContext(applicationContext);

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull(dataStore, "DataStore should not be null");
        assertTrue(dataStore instanceof MapDBStore, "DataStore should be of type MapDBStore");
        dataStore.clear();
        dataStore.close();
    }

    @Test
    public void testGetDataStoreRedisDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_REDISDB);
        consoleDataStoreFactory.setDbHost("redis://127.0.0.1:6379");

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull(dataStore, "DataStore should not be null");
        assertTrue(dataStore instanceof RedisStore, "DataStore should be of type RedisStore");
    }

    @Test
    public void testGetDataStoreUndefined() {
        consoleDataStoreFactory.setDbType("undefined_db_type");

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNull(dataStore, "DataStore should be null for undefined DB type");
    }
}
