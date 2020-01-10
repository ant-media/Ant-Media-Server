#!/bin/bash

AMS_INSTALL_LOCATION="/usr/local/antmedia"
RED5_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/conf/red5.properties

declare -A files
files=(
   [liveapp]=$AMS_INSTALL_LOCATION/webapps/LiveApp/WEB-INF/red5-web.properties
   [webrtc]=$AMS_INSTALL_LOCATION/webapps/WebRTCAppEE/WEB-INF/red5-web.properties
   [console]=$AMS_INSTALL_LOCATION/webapps/root/WEB-INF/red5-web.properties

)

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


if [ "$#" -eq 0 ]; then
  usage
  exit 1
fi

if [ "$OS_NAME" = "Darwin" ]; then
  AMS_INSTALL_LOCATION=`pwd`
  SED_COMPATIBILITY='.bak'
fi

conf ()
{
    sed -i $SED_COMPATIBILITY 's/clusterdb.host=.*/clusterdb.host='$1'/' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$2'/' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/clusterdb.user=.*/clusterdb.user='$4'/' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/clusterdb.password=.*/clusterdb.password='$5'/' $RED5_PROPERTIES_FILE

    for conf_files in ${files[@]}; do
      sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$3'/' $conf_files
      sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$1'/' $conf_files
      sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$4'/' $conf_files
      sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$5'/' $conf_files
    done
}

conf_cluster ()
{
  sed -i -E -e $SED_COMPATIBILITY 's/(<!-- cluster start|<!-- cluster start -->)/<!-- cluster start -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
  sed -i -E -e $SED_COMPATIBILITY 's/(cluster end -->|<!-- cluster end -->)/<!-- cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
}

hostfile ()
{
    LOCAL_IPv4=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
    HOST_NAME=`cat /proc/sys/kernel/hostname`
    HOST_LINE="$LOCAL_IPv4 $HOST_NAME"
    sed -i '/'$HOST_NAME'/d' /etc/hosts
    echo  "$HOST_LINE" | tee -a /etc/hosts
}

if [[ ! -z "$1" && ! -z "$2"  && ! -z "$3" && ! -z "$4" ]]; then
   DB_TYPE=mongodb
   conf_cluster
   conf $2 false $DB_TYPE $3 $4
   hostfile
elif [ "$1" == "cluster" ]; then
   echo "Mode: cluster"
    DB_TYPE=mongodb
    MONGO_SERVER_IP=$2
      if [ -z "$MONGO_SERVER_IP" ]; then
        echo "No Mongo DB Server specified. Missing parameter"
        usage
        exit 1
      fi
    conf_cluster
    conf $2 false $DB_TYPE
    hostfile  
elif [ "$1" == "standalone" ]; then
    echo "Mode: standalone"
    DB_TYPE=mapdb
    MONGO_SERVER_IP=localhost
    sed -i -E -e $SED_COMPATIBILITY 's/(<!-- cluster start -->|<!-- cluster start)/<!-- cluster start /g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i -E -e $SED_COMPATIBILITY 's/(<!-- cluster end -->|cluster end -->)/cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    conf $MONGO_SERVER_IP false $DB_TYPE
else
    usage
fi

echo "Ant Media Server will be restarted in $1 mode."
service antmedia restart
