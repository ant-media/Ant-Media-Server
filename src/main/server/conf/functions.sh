#!/bin/bash


#
# Changes the configuration parameters to switching cluster or standalone mode.
# - First parameter is the mode of the cluster. It can be standalone or cluster
# if the first parameter is cluster than the following parameter should also be given
#
# - Second parameter is the url of the database. It can be mongodb:, mongodb+srv:// redis: or redis yaml configuration file
# - Third parameter is the username of the mongodb. Deprecated. Add username to the host parameter
# - Fourth parameter is the password of the mongodb. Deprecated. Add password to the host parameter
#
change_server_mode() {
  AMS_INSTALL_LOCATION=/usr/local/antmedia
  
  OS_NAME=`uname`

  if [ "$OS_NAME" = "Darwin" ]; then
    AMS_INSTALL_LOCATION=`pwd`
    SED_COMPATIBILITY='.bak'
  fi
  LIST_APPS=`ls -d $AMS_INSTALL_LOCATION/webapps/*/`

  MODE=$1
  if [ $MODE = "cluster" ]; then
    echo "Mode: cluster"
    #default db type
    DB_TYPE=mongodb
    DB_URL=$2
    
    if [ -z "$DB_URL" ]; then
      echo "No DB URL specified. Missing parameter"
      usage
      exit 1
    fi
    
    # if DB_URL is an IP address or localhost or starts with mongodb, assume that it's mongodb IP Address for backward compatibility.
    if [[ $DB_URL =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ || $DB_URL =~ ^localhost$ ||  $DB_URL =~ ^mongo.*$ ]]; then
      # it should be ^mongo.*$ not ^mongodb.*$ becaue  kubernetes deployment  give -h mongo parameter
      DB_TYPE=mongodb
      echo "DB type is mongodb"
    elif [[ $DB_URL =~ ^redis.*$ ]]; then # if DB_URL starts with redis, then it's redis URL and make DB_TYPE to redis
      DB_TYPE=redisdb
      echo "DB type is redis"
    else  # if DB_URL is something else, then assume that it's redist configuration file and make DB_TYPE to redis
      DB_TYPE=redisdb
      echo "DB type is redis"
    fi
    
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start|<!-- cluster start -->)/<!-- cluster start -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e  's/(cluster end -->|<!-- cluster end -->)/<!-- cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
        
  else
    echo "Mode: standalone"
    DB_URL=$2
    if [ -z "$DB_URL" ]; then #backward compatible if no DB_URL it's mapdb
      DB_TYPE=mapdb
      DB_URL=""
      echo "DB type is mapdb"
    elif [[ $DB_URL =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+(:[0-9]{1,5})?$ || $DB_URL =~ ^localhost(:[0-9]{1,5})?$ ||  $DB_URL =~ ^mongo.*$ ]]; then
      # it should be ^mongo.*$ not ^mongodb.*$ becaue kubernetes deployment give -h mongo parameter
      DB_TYPE=mongodb
      echo "DB type is mongodb"
    elif [[ $DB_URL =~ ^redis.*$ ]]; then # if DB_URL starts with redis, then it's redis URL and make DB_TYPE to redis
      DB_TYPE=redisdb
      echo "DB type is redis"
    else  # if DB_URL is something else, then assume that it's redist configuration file and make DB_TYPE to redis
      DB_TYPE=redisdb
      echo "DB type is redis"
    fi
    
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start -->|<!-- cluster start)/<!-- cluster start /g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e 's/(<!-- cluster end -->|cluster end -->)/cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
  fi

if [ "$OS_NAME" != "Darwin" ]; then
  #The c\ command is used to handle the & character in the mongo server url. & character mentions the matched line in s/ command
 
  USER=$3
  PASS=$4
  sed -i $SED_COMPATIBILITY "/clusterdb.host=/c\clusterdb.host=${DB_URL}" $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY "/clusterdb.user=/c\clusterdb.user=${USER}" $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY "/clusterdb.password=/c\clusterdb.password=${PASS}" $AMS_INSTALL_LOCATION/conf/red5.properties

  for i in $LIST_APPS; do
    sed -i $SED_COMPATIBILITY "/db.type=/c\db.type=$DB_TYPE" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "/db.host=/c\db.host=${DB_URL}" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "/db.user=/c\db.user=${USER}" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "/db.password=/c\db.password=${PASS}" $i/WEB-INF/red5-web.properties
  done
else
  #for darwin use s/ -> substitute
  DB_URL="${DB_URL//\//\\/}"
  USER=""
  if [ ! -z "${3}" ]; then
    USER="${3//\//\\/}"
  fi
  PASS=""
  if [ ! -z "${4}" ]; then
    PASS="${4//\//\\/}"
  fi
  
  sed -i $SED_COMPATIBILITY "s/clusterdb.host=.*/clusterdb.host=${DB_URL}/g" $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY "s/clusterdb.user=.*/clusterdb.user=${USER}/g" $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY "s/clusterdb.password=.*/clusterdb.password=${PASS}/g" $AMS_INSTALL_LOCATION/conf/red5.properties

  for i in $LIST_APPS; do
    sed -i $SED_COMPATIBILITY "s/db.type=.*/db.type=$DB_TYPE/g" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "s/db.host=.*/db.host=${DB_URL}/g" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "s/db.user=.*/db.user=${USER}/g" $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY "s/db.password=.*/db.password=${PASS}/g" $i/WEB-INF/red5-web.properties
  done
fi
  
  if [ "$OS_NAME" != "Darwin" ]; then
    LOCAL_IPv4=`ip a | awk '/inet / && !/127.0.0.1/ && !/docker0/ {print $2}' | cut -d/ -f1`
    HOST_NAME=`cat /proc/sys/kernel/hostname`
    HOST_LINE="$LOCAL_IPv4 $HOST_NAME"

    # Change /etc/hosts file
    # In docker changing /etc/hosts produces device or resource busy error.
    # Above commands takes care the changing host file

    # temp hosts file
    NEW_HOST_FILE=~/.hosts.new
    # cp hosts file
    cp /etc/hosts $NEW_HOST_FILE
    # delete hostname line from the file
    sed -i '/'$HOST_NAME'/d' $NEW_HOST_FILE
    # add host line to the file
    echo  "$HOST_LINE" | tee -a $NEW_HOST_FILE
    # change the /etc/hosts file - (mv does not work)
    cp -f $NEW_HOST_FILE /etc/hosts
    # remove temp hosts file
    rm $NEW_HOST_FILE
  fi

}


