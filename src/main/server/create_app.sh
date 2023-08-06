#!/bin/bash

echo "Please use the POST REST method (/applications/{appName}) to create an application."

usage() {
  echo "Usage:"
  echo "$0 -n APPLICATION_NAME [-p INSTALLATION_PATH] [-w true|false] [-c true|false]"
  echo "Options:"
  echo "-n:  Name of the application that you want to have. It's mandatory"
  echo "-p: (Optional) Path is the install location of Ant Media Server which is /usr/local/antmedia by default."
  echo "-w: (Optional) The flag to deploy application as war file. Default value is false"
  echo "-c: (Optional) The flag to deploy application in cluster mode. Default value is false"
  echo "-m:  Mongo DB URI (including username and password). If it's a cluster, it's mandatory for both cluster and standalone modes."
  echo "-r:  Redis URI (including username and password). If it's a cluster, it's mandatory for both cluster and standalone modes."
  echo "-t:  Redis username (required for standalone mode)."
  echo "-q:  Redis password (required for standalone mode)."
  echo "-h: print this usage"
  echo "-f: war file path for custom app deployment"
  echo " "
  echo "Example: "
  echo "$0 -n live -c true -m mongo://username:password@127.0.0.1:27017 -r redis://username:password@127.0.0.1:6379"
  echo " "
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

echo "all parameters"
echo $@
echo "--> \n"

ERROR_MESSAGE="Error: App is not created. Please check the error in the terminal and take a look at the instructions below"

AMS_DIR=/usr/local/antmedia
AS_WAR=false
IS_CLUSTER=false

while getopts 'n:p:w:h:c:m:r:t:q:f:' option
do
  case "${option}" in
    n) APP_NAME=${OPTARG};;
    p) AMS_DIR=${OPTARG};;
    w) AS_WAR=${OPTARG};;
    c) IS_CLUSTER=${OPTARG};;
    m) MONGO_URI=${OPTARG};;
    r) REDIS_URI=${OPTARG};;
    t) REDIS_USER=${OPTARG};;
    q) REDIS_PASS=${OPTARG};;
    f) WAR_FILE=${OPTARG};;
    h) usage 
       exit 1;;
   esac
done

check_result() {
  OUT=$?
  if [ $OUT -ne 0 ]; then
    echo -e $ERROR_MESSAGE
    usage
    exit $OUT
  fi
}

if [ -z "$APP_NAME" ]; then
  APP_NAME=$1

  if [ ! -z "$2" ]; then
    AMS_DIR=$2
  fi
fi

if [[ -z "$APP_NAME" ]]; then
    echo "Error: Missing parameter APPLICATION_NAME. Check instructions below"
    usage
    exit 1
fi

if [[ -z "$WAR_FILE" ]]; then
    WAR_FILE=$AMS_DIR/StreamApp*.war
fi

if [[ -z "$AS_WAR" ]]; then
    AS_WAR="false"
fi

if [[ "$IS_CLUSTER" == "true" ]]; then
    if [[ -z "$MONGO_URI" || -z "$REDIS_URI" ]]; then
       echo "Please set mongodb and redis URIs for cluster mode. "
       usage
       exit 1
    fi
fi

case $AMS_DIR in
  /*) AMS_DIR=$AMS_DIR;;
  *)  AMS_DIR=$PWD/$AMS_DIR;;
esac

APP_NAME_LOWER=$(echo $APP_NAME | awk '{print tolower($0)}')
APP_DIR=$AMS_DIR/webapps/$APP_NAME
RED5_PROPERTIES_FILE=$APP_DIR/WEB-INF/red5-web.properties
WEB_XML_FILE=$APP_DIR/WEB-INF/web.xml

mkdir $APP_DIR
check_result

echo $AMS_DIR

if [[ -n "$WAR_FILE" ]]; then
    cp $WAR_FILE $APP_DIR
    check_result
    
    WAR_FILE_NAME=`basename $WAR_FILE`
    
    cd $APP_DIR
    check_result
    
    unzip $WAR_FILE_NAME
    check_result
    
    rm $WAR_FILE_NAME
    check_result
fi

OS_NAME=`uname`

if [[ "$OS_NAME" == 'Darwin' ]]; then
  SED_COMPATIBILITY='.bak'
fi

# Modify MongoDB properties for cluster and standalone modes
if [[ -n "$MONGO_URI" ]]; then
  MONGO_HOST=$(echo $MONGO_URI | cut -d@ -f2 | cut -d: -f2)
  MONGO_PORT=$(echo $MONGO_URI | cut -d@ -f2 | cut -d: -f3)
  MONGO_USER=$(echo $MONGO_URI | cut -d: -f2 | cut -d/ -f3)
  MONGO_PASS=$(echo $MONGO_URI | cut -d: -f3 | cut -d@ -f1)
  
  sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='mongodb'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's#db.host=.*#db.host='$MONGO_HOST'#' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/db.port=.*/db.port='$MONGO_PORT'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$MONGO_USER'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$MONGO_PASS'/' $RED5_PROPERTIES_FILE
fi

# Modify Redis properties for cluster and standalone modes
if [[ -n "$REDIS_URI" ]]; then
  REDIS_HOST=$(echo $REDIS_URI | cut -d@ -f2 | cut -d: -f2)
  REDIS_PORT=$(echo $REDIS_URI | cut -d@ -f2 | cut -d: -f3)
  REDIS_USER=$(echo $REDIS_URI | cut -d: -f2 | cut -d/ -f3)
  REDIS_PASS=$(echo $REDIS_URI | cut -d: -f3 | cut -d@ -f1)
  
  sed -i $SED_COMPATIBILITY 's/redis.host=.*/redis.host='$REDIS_HOST'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/redis.port=.*/redis.port='$REDIS_PORT'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/redis.user=.*/redis.user='$REDIS_USER'/' $RED5_PROPERTIES_FILE
  sed -i $SED_COMPATIBILITY 's/redis.password=.*/redis.password='$REDIS_PASS'/' $RED5_PROPERTIES_FILE
fi

if [[ "$IS_CLUSTER" == "true" ]]; then
    echo "Cluster mode"
    ln -s $WAR_FILE $AMS_DIR/webapps/root/$APP_NAME.war
else
    echo "Not cluster mode."
    # No cluster mode modifications needed for Redis in standalone mode
fi

if [[ $AS_WAR == "true" ]]; then
  echo "Application will be deployed as war"
  cd $APP_DIR 
  zip -r ../$APP_NAME.war *  
  rm -r $APP_DIR
else
  echo "Application is deployed as directory."
  chown -R antmedia:antmedia $APP_DIR -f
fi

echo "$APP_NAME is created."
