#!/bin/bash

usage() {
  echo ""
  echo "This script change server mode to cluster or standalone"
  echo "Please use the script as follows."
  echo ""
  echo "Usage: "
  echo "Change server mode to cluster"
  echo "$0  cluster {MONGO_DB_SERVER}"
  echo "$0  cluster {MONGO_DB_SERVER} {MONGO_DB_USERNAME} {MONGO_DB_PASSWORD}"
  echo ""
  echo "Change server mode to standalone"
  echo "$0  standalone"
  echo ""
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

MODE=$1
if [ -z "$MODE" ]; then
  echo "No server mode specified. Missing parameter"
  usage
  exit 1
fi

AMS_INSTALL_LOCATION=/usr/local/antmedia
OS_NAME=`uname`

if [ "$OS_NAME" = "Darwin" ]; then
  AMS_INSTALL_LOCATION=`pwd`
  SED_COMPATIBILITY='.bak'
fi

USE_GLOBAL_IP="false"

if [ $MODE = "cluster" ]
  then
    echo "Mode: cluster"
    DB_TYPE=mongodb
    MONGO_SERVER_IP=$2
      if [ -z "$MONGO_SERVER_IP" ]; then
        echo "No Mongo DB Server specified. Missing parameter"
        usage
        exit 1
      fi
    
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start|<!-- cluster start -->)/<!-- cluster start -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e  's/(cluster end -->|<!-- cluster end -->)/<!-- cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
        
  else
    echo "Mode: standalone"
    DB_TYPE=mapdb
    MONGO_SERVER_IP=localhost
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start -->|<!-- cluster start)/<!-- cluster start /g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e 's/(<!-- cluster end -->|cluster end -->)/cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
fi

LIVEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/LiveApp/WEB-INF/red5-web.properties
WEBRTCAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/WebRTCAppEE/WEB-INF/red5-web.properties
CONSOLEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/root/WEB-INF/red5-web.properties
RED5_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/conf/red5.properties


sed -i $SED_COMPATIBILITY 's/clusterdb.host=.*/clusterdb.host='$MONGO_SERVER_IP'/' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$USE_GLOBAL_IP'/' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/clusterdb.user=.*/clusterdb.user='$3'/' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/clusterdb.password=.*/clusterdb.password='$4'/' $RED5_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $LIVEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $LIVEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$3'/' $LIVEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$4'/' $LIVEAPP_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $WEBRTCAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $WEBRTCAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$3'/' $WEBRTCAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$4'/' $WEBRTCAPP_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $CONSOLEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $CONSOLEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$3'/' $CONSOLEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$4'/' $CONSOLEAPP_PROPERTIES_FILE


if [ "$OS_NAME" = "Darwin" ]; then
  echo "You can re-start Ant Media Server on your Macos"
  exit 0
fi

LOCAL_IPv4=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
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

echo "Ant Media Server is restarting in $MODE mode."
#service antmedia restart does not work if daemon is not running so that stop and start
service antmedia stop
service antmedia start

