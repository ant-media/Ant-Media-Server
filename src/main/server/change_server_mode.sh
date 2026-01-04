#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' 

usage() {
  echo ""
  echo "This script change server mode to cluster or standalone"
  echo "Please use the script as follows."
  echo ""
  echo "Usage: "
  echo "Change server mode to cluster"
  echo "$0  cluster {DB_CONNECTION_URL}"
  echo ""
  echo "You can use connection string directly as {DB_CONNECTION_URL} that starts with mongodb:// or mongodb+srv:// or give the ip address"
  echo "You can also use REDIS url or redis configuration file as {DB_CONNECTION_URL}"
  echo "Please don't forget quote your connection string. Take a look at the sample below."
  echo "Usage:"
  echo "$0 cluster \"mongodb://[username:password@]host1[:port1][,...hostN[:portN]][/[defaultauthdb][?options]]\" "
  echo "$0 cluster \"redis://[username:password@]host1:port1]\" "
  echo ""
  echo "Change server mode to standalone that uses mapdb"
  echo "$0  standalone"
  echo "Change server mode to standalone that uses {DB_CONNECTION_URL} for mongo or redis"
  echo "$0  standalone {DB_CONNECTION_URL}"
  echo ""
  echo ""
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

validate_parameters() {
  local mode=$1
  local db_connection=$2
  
  if [[ "$mode" != "cluster" && "$mode" != "standalone" ]]; then
    echo -e "${RED}ERROR: Invalid mode '$mode'. Only 'cluster' or 'standalone' are allowed.${NC}"
    usage
    exit 1
  fi
  
  if [[ "$mode" == "cluster" && -z "$db_connection" ]]; then
    echo -e "${RED}ERROR: Database connection URL is required for cluster mode.${NC}"
    usage
    exit 1
  fi
  
  if [[ -n "$db_connection" ]]; then
    if [[ ! "$db_connection" =~ ^(mongodb://|mongodb\+srv://|redis://) ]] && [[ ! -f "$db_connection" ]]; then
      echo -e "${YELLOW}WARNING: DB connection doesn't start with mongodb://, mongodb+srv://, redis:// and is not a file.${NC}"
      echo "Are you sure this is correct? (y/n)"
      read -r confirmation
      if [[ "$confirmation" != "y" && "$confirmation" != "Y" ]]; then
        echo -e "${RED}Operation cancelled.${NC}"
        exit 1
      fi
    fi
  fi
}
 
BASEDIR=$(dirname "$0")
cd $BASEDIR
AMS_INSTALL_LOCATION=`pwd`

source $AMS_INSTALL_LOCATION/conf/functions.sh

USE_GLOBAL_IP="false"
MODE=$1

if [ -z "$MODE" ]; then
  echo -e "${RED}ERROR: No server mode specified. Missing parameter${NC}"
  usage
  exit 1
fi

validate_parameters "$1" "$2"

OS_NAME=`uname`
if [ "$OS_NAME" = "Darwin" ]; then
    SED_COMPATIBILITY='.bak'
fi

if ! change_server_mode $1 $2 $3 $4; then
  echo -e "${RED}ERROR: Failed to change server mode.${NC}"
  exit 1
fi

sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$USE_GLOBAL_IP'/' $AMS_INSTALL_LOCATION/conf/red5.properties 

if [ "$OS_NAME" = "Darwin" ]; then
  echo -e "${GREEN}You can re-start Ant Media Server on your Macos${NC}"
  exit 0
fi

echo -e "${GREEN}Ant Media Server is restarting in $MODE mode.${NC}"
#service antmedia restart does not work if daemon is not running so that stop and start
service antmedia stop
service antmedia start
