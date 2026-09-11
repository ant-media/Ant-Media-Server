package io.antmedia.test.db;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.datastore.MongoStore;
import io.antmedia.console.datastore.RedisStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(MockitoExtension.class)
@Testcontainers
public class ConsoleDataStoreFactoryUnitTest {

    private ConsoleDataStoreFactory consoleDataStoreFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Vertx vertx;

    @Container
    public static GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(6379);

    @Container
    public static GenericContainer mongo = new GenericContainer(DockerImageName.parse("mongo:7"))
            .withExposedPorts(27017);

    @BeforeEach
    public void setUp() {
        consoleDataStoreFactory = new ConsoleDataStoreFactory();
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
        consoleDataStoreFactory.setDbHost("mongodb://" + mongo.getHost() + ":" + mongo.getFirstMappedPort());
        
        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull(dataStore, "DataStore should not be null");
        assertTrue(dataStore instanceof MongoStore, "DataStore should be of type MongoStore");
        dataStore.close();
    }

    @Test
    public void testGetDataStoreMapDB() {
        consoleDataStoreFactory.setDbType(DataStoreFactory.DB_TYPE_MAPDB);

        when(applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
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
        consoleDataStoreFactory.setDbHost("redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNotNull(dataStore, "DataStore should not be null");
        assertTrue(dataStore instanceof RedisStore, "DataStore should be of type RedisStore");
        dataStore.close();
    }

    @Test
    public void testGetDataStoreUndefined() {
        consoleDataStoreFactory.setDbType("undefined_db_type");

        AbstractConsoleDataStore dataStore = consoleDataStoreFactory.getDataStore();
        assertNull(dataStore, "DataStore should be null for undefined DB type");
    }
}
