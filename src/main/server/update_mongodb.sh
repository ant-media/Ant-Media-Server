#!/bin/bash

# You should run this script if you use MongoDB database and upgrade your server v2.4.1 or below versions to the latest version.
# Here is the script usage:
# ./update_mongodb.sh MONGODB_IP MONGODB_USERNAME MONGODB_PASSWORD
  
MONGODB_IP=$1
MONGODB_USERNAME=$2
MONGODB_PASSWORD=$3
TODAY=$(date +"%Y-%m-%d-%H-%M")

run_mongo() {
    db=$1
    shift
    mongo "$MONGODB_IP/$db" --quiet --eval "$@"
}

run_mongo_auth() {
        db=$1
        shift
        mongo --host $MONGODB_IP --authenticationDatabase admin $db --quiet --username $MONGODB_USERNAME --password $MONGODB_PASSWORD  --eval "$@"
}

mongo_backup(){
        if [ -z "$MONGODB_USERNAME" ] && [ -z "$MONGODB_PASSWORD" ]; then
                mongodump --host $MONGODB_IP --quiet --gzip -o /usr/local/antmedia/mongo_dump_$TODAY
        elif [ ! -z "$MONGODB_USERNAME" ] && [ ! -z "$MONGODB_PASSWORD" ]; then
                mongodump --host $MONGODB_IP --quiet --username $MONGODB_USERNAME --password $MONGODB_PASSWORD --gzip -o /usr/local/antmedia/mongo_dump_$TODAY
        fi

        if [ $? != 0 ]; then
           echo "The backup is not completed."
        else
           echo "The backup is completed. Backup Folder: /usr/local/antmedia/mongo_dump_$TODAY"
        fi
}

mongo_backup


mongocon() {
        if [ -z "$MONGODB_USERNAME" ] && [ -z "$MONGODB_PASSWORD" ]; then
                for appsettings in $(seq 0 $(run_mongo "clusterdb" 'db.AppSettings.count()'-1)); do
                                appName=`run_mongo "clusterdb" 'db.AppSettings.find()['$appsettings'].appName'|tr '[:upper:]' '[:lower:]'`
                                for broadcast in $(seq 0 $(run_mongo "$appName" 'db.broadcast.count()'-1)); do
                                        broadcastObjectId=`run_mongo "$appName" 'db.broadcast.find()['$broadcast']._id'`
                                        q=$(run_mongo "$appName" 'db.broadcast.find({"_t" : {$ne : "Broadcast"}})')
                                        if [ "$q" ]; then
                                                run_mongo $appName "db.broadcast.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.datastore.db.types.Broadcast\": \"\"}});"
                                                run_mongo "$appName" "db.broadcast.updateMany({\"_id\" :$broadcastObjectId}, {\$set : {\"_t\" : \"Broadcast\", \"metaData\" : \"\"}})"
                                        fi
                                done

                                for vod in $(seq 0 $(run_mongo $appName"VoD" 'db.vod.count()'-1)); do
                                        vodObjectId=`run_mongo $appName"VoD" "db.vod.find()['$vod']._id"`
                                        q=$(run_mongo $appName"VoD" 'db.vod.find({"_t" : {$ne : "VoD"}})')
                                        if [ "$q" ]; then
                                                run_mongo $appName"VoD" "db.vod.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.datastore.db.types.VoD\": \"\"}});"
                                                run_mongo $appName"VoD" "db.vod.updateMany({\"_id\" :$vodObjectId}, {\$set : {\"_t\" : \"VoD\"}})"
                                        fi
                                done
                done

                if [ `run_mongo "serverdb" 'db.user.count()'` = "0" ]; then
                        run_mongo "serverdb" "db.user.drop()"
                        run_mongo "serverdb" "db.User.renameCollection('user')"
                        for x in $(seq 0 $(run_mongo "serverdb" 'db.user.count()'-1)); do
                                users=`run_mongo "serverdb" 'db.user.find()['$x']._id'`
                                run_mongo "serverdb" "db.user.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.rest.model.User\": \"\"}});"
                                run_mongo "serverdb" "db.user.updateMany({\"_id\" : $users}, {\$set : {\"_t\" : \"User\", \"scope\" : \"system\", \"fullName\" : \"JamesBond\"}})"
                        done
                fi

        fi

        if [ ! -z "$MONGODB_USERNAME" ] && [ ! -z "$MONGODB_PASSWORD" ]; then
                for appsettings in $(seq 0 $(run_mongo_auth "clusterdb" 'db.AppSettings.count()'-1)); do
                                appName=`run_mongo_auth "clusterdb" 'db.AppSettings.find()['$appsettings'].appName'|tr '[:upper:]' '[:lower:]'`
                                for broadcast in $(seq 0 $(run_mongo_auth "$appName" 'db.broadcast.count()'-1)); do
                                        broadcastObjectId=`run_mongo_auth "$appName" 'db.broadcast.find()['$broadcast']._id'`
                                        q=$(run_mongo_auth "$appName" 'db.broadcast.find({"_t" : {$ne : "Broadcast"}})')
                                        if [ "$q" ]; then
                                                run_mongo_auth $appName "db.broadcast.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.datastore.db.types.Broadcast\": \"\"}});"
                                                run_mongo_auth "$appName" "db.broadcast.updateMany({\"_id\" :$broadcastObjectId}, {\$set : {\"_t\" : \"Broadcast\", \"metaData\" : \"\"}})"
                                        fi
                                done

                                for vod in $(seq 0 $(run_mongo_auth $appName"VoD" 'db.vod.count()'-1)); do
                                        vodObjectId=`run_mongo_auth $appName"VoD" "db.vod.find()['$vod']._id"`
                                        q=$(run_mongo_auth $appName"VoD" 'db.vod.find({"_t" : {$ne : "VoD"}})')
                                        if [ "$q" ]; then
                                                run_mongo_auth $appName"VoD" "db.vod.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.datastore.db.types.VoD\": \"\"}});"
                                                run_mongo_auth $appName"VoD" "db.vod.updateMany({\"_id\" :$vodObjectId}, {\$set : {\"_t\" : \"VoD\"}})"
                                        fi
                                done
                done

                if [ `run_mongo_auth "serverdb" 'db.user.count()'` = "0" ]; then
                        run_mongo_auth "serverdb" "db.user.drop()"
                        run_mongo_auth "serverdb" "db.User.renameCollection('user')"
                        for x in $(seq 0 $(run_mongo_auth "serverdb" 'db.user.count()'-1)); do
                                users=`run_mongo_auth "serverdb" 'db.user.find()['$x']._id'`
                                run_mongo_auth "serverdb" "db.user.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.rest.model.User\": \"\"}});"
                                run_mongo_auth "serverdb" "db.user.updateMany({\"_id\" : $users}, {\$set : {\"_t\" : \"User\", \"scope\" : \"system\", \"fullName\" : \"JamesBond\"}})"
                        done
                fi

        fi

}

mongocon