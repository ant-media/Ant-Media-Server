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
 
BASEDIR=$(dirname "$0")
cd $BASEDIR
AMS_INSTALL_LOCATION=`pwd`


source $AMS_INSTALL_LOCATION/conf/functions.sh

USE_GLOBAL_IP="false"
MODE=$1
if [ -z "$MODE" ]; then
  echo "No server mode specified. Missing parameter"
  usage
  exit 1
fi

OS_NAME=`uname`
if [ "$OS_NAME" = "Darwin" ]; then
    SED_COMPATIBILITY='.bak'
fi

change_server_mode $1 $2 $3 $4

sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$USE_GLOBAL_IP'/' $AMS_INSTALL_LOCATION/conf/red5.properties 

if [ "$OS_NAME" = "Darwin" ]; then
  echo "You can re-start Ant Media Server on your Macos"
  exit 0
fi



echo "Ant Media Server is restarting in $MODE mode."
#service antmedia restart does not work if daemon is not running so that stop and start
service antmedia stop
service antmedia start