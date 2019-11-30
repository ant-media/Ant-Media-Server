usage() {
	echo ""
	echo "This script change server mode to cluster or standalone"
	echo "Please use the script as follows."
	echo ""
	echo "Usage: "
	echo "Change server mode to cluster"
	echo "$0  cluster {MONGO_DB_SERVER}"
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
    
    cp $AMS_INSTALL_LOCATION/conf/jee-container-cluster.xml $AMS_INSTALL_LOCATION/conf/jee-container.xml
    
    if [ ! -z "$3" ]; then
   	  USE_GLOBAL_IP=$3
	fi
  else
    echo "Mode: standalone"
    DB_TYPE=mapdb
    MONGO_SERVER_IP=localhost
    cp $AMS_INSTALL_LOCATION/conf/jee-container-standalone.xml $AMS_INSTALL_LOCATION/conf/jee-container.xml
fi

LIVEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/LiveApp/WEB-INF/red5-web.properties
WEBRTCAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/WebRTCAppEE/WEB-INF/red5-web.properties
CONSOLEAPP_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/webapps/root/WEB-INF/red5-web.properties
RED5_PROPERTIES_FILE=$AMS_INSTALL_LOCATION/conf/red5.properties




sed -i $SED_COMPATIBILITY 's/clusterdb.host=.*/clusterdb.host='$MONGO_SERVER_IP'/' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$USE_GLOBAL_IP'/' $RED5_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $LIVEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $LIVEAPP_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $WEBRTCAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $WEBRTCAPP_PROPERTIES_FILE

sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $CONSOLEAPP_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's/db.host=.*/db.host='$MONGO_SERVER_IP'/' $CONSOLEAPP_PROPERTIES_FILE



if [ "$OS_NAME" = "Darwin" ]; then
  echo "You can re-start Ant Media Server on your Macos"
  exit 0
fi

LOCAL_IPv4=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
HOST_NAME=`cat /proc/sys/kernel/hostname`
HOST_LINE="$LOCAL_IPv4 $HOST_NAME"

sed -i '/'$HOST_NAME'/d' /etc/hosts
echo  "$HOST_LINE" | tee -a /etc/hosts

echo "Ant Media Server will be restarted in $MODE mode."
service antmedia restart
