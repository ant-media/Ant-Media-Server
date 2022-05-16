#!/bin/bash

# In version 2.4.2, the MongoDB driver is updated. 
# If you use MongoDB database and upgrade your server from v2.4.1 or below versions to the latest version,
# you should run this script
# Here is the script usage:
# ./fix_mongodb_for_upgrading_from_2.4.1_and_earlier.sh MONGODB_IP MONGODB_USERNAME MONGODB_PASSWORD
  
MONGODB_IP=$1
MONGODB_USERNAME=$2
MONGODB_PASSWORD=$3
TODAY=$(date +"%Y-%m-%d-%H-%M")


usage() {
  #show usage 
  echo "In version 2.4.2, the MongoDB driver is updated."
  echo "If you use MongoDB database and upgrade your server "
  echo "from v2.4.1 or below versions to the latest version, you should run this script"
  #install mongodb-clients 
  echo "Usage:"
  echo "$0 MONGODB_IP MONGODB_USERNAME MONGODB_PASSWORD\n"
  
}


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
                mongodump --host $MONGODB_IP --gzip -o mongo_dump_$TODAY
        elif [ ! -z "$MONGODB_USERNAME" ] && [ ! -z "$MONGODB_PASSWORD" ]; then
                mongodump --host $MONGODB_IP --username $MONGODB_USERNAME --password $MONGODB_PASSWORD --gzip -o mongo_dump_$TODAY
        fi

        if [ $? != 0 ]; then
           echo "The backup is not completed so it does not continue."
           echo "Please check credentials are correct and you've write permission to current directory: `pwd` "
           echo "If it's a permission issue, you may need to run this command with sudo"
           exit 1;
        else
           echo "The backup is completed. Backup Folder: /usr/local/antmedia/mongo_dump_$TODAY"
        fi
}




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

                                #Old AppSettings changes
                                appObjectId=`run_mongo "clusterdb" 'db.AppSettings.find()['$appsettings']._id'`
                                q=$(run_mongo "clusterdb" 'db.AppSettings.find({"_t" : {$ne : "AppSettings"}})')
                                if [ "$q" ]; then
                                        run_mongo "clusterdb" "db.AppSettings.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.AppSettings\": \"\"}});"
                                        run_mongo "clusterdb" "db.AppSettings.updateMany({\"_id\" :$appObjectId}, {\$set : {\"_t\" : \"AppSettings\"}})"
                                fi

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

                                #Old AppSettings changes
                                appObjectId=`run_mongo_auth "clusterdb" 'db.AppSettings.find()['$appsettings']._id'`
                                q=$(run_mongo_auth "clusterdb" 'db.AppSettings.find({"_t" : {$ne : "AppSettings"}})')
                                if [ "$q" ]; then
                                        run_mongo_auth "clusterdb" "db.AppSettings.updateMany({}, {\$unset: {\"className\" : \"\", \"io.antmedia.AppSettings\": \"\"}});"
                                        run_mongo_auth "clusterdb" "db.AppSettings.updateMany({\"_id\" :$appObjectId}, {\$set : {\"_t\" : \"AppSettings\"}})"
                                fi
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

#check if mongodb IP address is set
if [ -z "$MONGODB_IP" ]; then
  echo "Missing parameter. MongoDB IP is not set"
  usage
  exit 1
fi

#check if mongodb client tools is available
if ! [ -x "$(command -v mongo)" ]; then
  echo "mongo command is not found. Please install the mongodb-clients. ";
  echo "sudo apt-get install mongodb-clients"
  exit 1;
fi


mongo_backup

mongocon