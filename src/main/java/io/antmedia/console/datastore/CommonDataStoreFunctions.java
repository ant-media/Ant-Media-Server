package io.antmedia.console.datastore;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonDataStoreFunctions {

    protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

    public void dropNonUniqueBroadcasts(MongoClient mongoClient, String dbName){

        /* Drop Index of streamId if the database is already created in a mongodb instance.
         * We are adding unique index to streamId and this method is just to ensure backward compatibility for now.
         * It can be deleted in later versions, the last version that requires this is 2.4.2
         */
        for( String db : mongoClient.listDatabaseNames()){
            if(db.equals(dbName) || db.equals(dbName+"VoD") || db.equals(dbName + "_token") || db.equals(dbName + "_subscriber") || db.equals(dbName + "detection") || db.equals(dbName + "room")){
                MongoDatabase database = mongoClient.getDatabase(db);
                MongoCollection broadcastCollection = database.getCollection("broadcast");
                boolean isUnique= false;
                for(Object index : broadcastCollection.listIndexes()){
                    if(index.toString().contains("unique=true")){
                        isUnique = true;
                    }
                }
                if(isUnique != true){
                    logger.info("Dropping stream ID index because it does not have unique label for db: {}", dbName);
                    broadcastCollection.dropIndex("streamId_1");
                }
            }
        }
    }
}
