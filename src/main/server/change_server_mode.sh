#!/bin/bash

AMS_INSTALL_LOCATION="/usr/local/antmedia"
LIVEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/LiveApp/WEB-INF/red5-web.properties
WEBRTCAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/WebRTCAppEE/WEB-INF/red5-web.properties
CONSOLEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/root/WEB-INF/red5-web.properties
RED5_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/conf/red5.properties

usage ()
{
    echo "Usage:"
      echo -e "    $0 -m standalone"
      echo -e "    $0 -m cluster -s mongodb_ip"
      echo -e "    $0 -m cluster -s mongodb_ip -u username -p password"
      echo -e "    $0 -h"
      exit 0
}

if [ "$#" -eq 0 ]; then
  usage
fi

if [ "$OS_NAME" = "Darwin" ]; then
  AMS_INSTALL_LOCATION=`pwd`
  SED_COMPATIBILITY='.bak'
fi


conf ()
{
    sed -i $SED_COMPATIBILITY 's/clusterdb.host=.*/clusterdb.host='$1'/' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$2'/' $RED5_PROPERTIES_FILE

    sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$3'/' $LIVEAPP_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$1'/' $LIVEAPP_PROPERTIES_FILE

    sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$3'/' $WEBRTCAPP_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$1'/' $WEBRTCAPP_PROPERTIES_FILE

    sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$3'/' $CONSOLEAPP_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$1'/' $CONSOLEAPP_PROPERTIES_FILE

    sed -i $SED_COMPATIBILITY 's/clusterdb.user=.*/clusterdb.user='$4'/' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's/clusterdb.password=.*/clusterdb.password='$5'/' $RED5_PROPERTIES_FILE
}

hostfile ()
{

    LOCAL_IPv4=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
    HOST_NAME=`cat /proc/sys/kernel/hostname`
    HOST_LINE="$LOCAL_IPv4 $HOST_NAME"
    sed -i '/'$HOST_NAME'/d' /etc/hosts
    echo  "$HOST_LINE" | tee -a /etc/hosts

}

while getopts "m:s:u:p:h" opt; do
  case ${opt} in
    h )
      usage
      ;;
    m )
      m=${OPTARG}
      ;;
    s )
      s=${OPTARG}
      ;;
    u )
      u=${OPTARG}
      ;;
    p )
      p=${OPTARG}
      ;;

    \? )
      echo "Invalid Option: -$OPTARG" 1>&2
      usage
      exit 1
      ;;
  esac
done
shift $((OPTIND -1))

if [[ "$m" == "cluster" && ! -z $s  &&  ! -z $u && ! -z $p ]];then
    
    echo "Mode: cluster"
    DB_TYPE=mongodb
    sed -i $SED_COMPATIBILITY 's^<!-- cluster start^<!-- cluster start -->^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY 's^cluster end -->^<!-- cluster end -->^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    conf $s false $DB_TYPE $u $p
    cp -p $AMS_INSTALL_LOCATION/conf/jee-container-cluster.xml $AMS_INSTALL_LOCATION/conf/jee-container.xml
    hostfile

elif [ "$m" == "standalone" ]; then
    
    echo "Mode: standalone"
    DB_TYPE=mapdb
    MONGO_SERVER_IP=localhost
    sed -i $SED_COMPATIBILITY 's^<!-- cluster start -->^<!-- cluster start^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY 's^<!-- cluster end -->^cluster end -->^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    conf $MONGO_SERVER_IP false $DB_TYPE
    cp -p $AMS_INSTALL_LOCATION/conf/jee-container-standalone.xml $AMS_INSTALL_LOCATION/conf/jee-container.xml

elif [[ "$m" == "cluster" && ! -z "$s" ]]; then
    
    echo "Mode: cluster"
    DB_TYPE=mongodb
    sed -i $SED_COMPATIBILITY 's^<!-- cluster start^<!-- cluster start -->^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY 's^cluster end -->^<!-- cluster end -->^' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    conf $s false $DB_TYPE 
    cp -p $AMS_INSTALL_LOCATION/conf/jee-container-cluster.xml $AMS_INSTALL_LOCATION/conf/jee-container.xml
    hostfile

else

    usage

fi

echo "Ant Media Server will be restarted in $m mode."
service antmedia restart

