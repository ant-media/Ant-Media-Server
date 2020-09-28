#!/bin/bash

usage() {
  echo "Usage:"
  echo "$0  {APPLICATION_NAME} [{INSTALL_DIRECTORY}]"
  echo "{APPLICATION_NAME} is the application name that you want to have. It's mandatory"
  echo "{INSTALL_DIRECTORY} is the install location of ant media server which is /usr/local/antmedia by default. It's optional"
  echo " "
  echo "Example: "
  echo "$0 live "
  echo " "
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

ERROR_MESSAGE="Error: App is not created. Please check the error in the terminal and take a look at the instructions below"
FILE=`cat /usr/local/antmedia/conf/red5.properties |grep -c "clusterdb.host=localhost"`

check_result() {
  OUT=$?
      if [ $OUT -ne 0 ]; then
          echo -e $ERROR_MESSAGE
          usage
          exit $OUT
    fi
}

if [[ -z "$1" ]]; then
    echo "Error: Missing parameter APPLICATON_NAME. Check instructions below"
    usage
    exit 1
fi

AMS_DIR=/usr/local/antmedia
if [[ ! -z "$2" ]]; then
    echo "Using install directory as $2"  #because install directory is optional
    case $2 in
      /*) AMS_DIR=$2;;
      *)  AMS_DIR=$PWD/$2;;
    esac
fi

RED='\033[0;31m'
NOCOLOR="\033[0m"

#set the app name
APP_NAME=$1

APP_NAME_LOWER=$(echo $APP_NAME | awk '{print tolower($0)}')
APP_DIR=$AMS_DIR/webapps/$APP_NAME
RED5_PROPERTIES_FILE=$APP_DIR/WEB-INF/red5-web.properties
WEB_XML_FILE=$APP_DIR/WEB-INF/web.xml

mkdir $APP_DIR
check_result

echo $AMS_DIR
cp $AMS_DIR/StreamApp*.war $APP_DIR
check_result

cd $APP_DIR
check_result

jar -xf StreamApp*.war
check_result

rm StreamApp*.war
check_result

OS_NAME=`uname`

if [[ "$OS_NAME" == 'Darwin' ]]; then
  SED_COMPATIBILITY='.bak'
fi

sed -i $SED_COMPATIBILITY 's^webapp.dbName=.*^webapp.dbName='$APP_NAME_LOWER'.db^' $RED5_PROPERTIES_FILE
check_result
sed -i $SED_COMPATIBILITY 's^webapp.contextPath=.*^webapp.contextPath=/'$APP_NAME'^' $RED5_PROPERTIES_FILE
check_result
sed -i $SED_COMPATIBILITY 's^db.app.name=.*^db.app.name='$APP_NAME'^' $RED5_PROPERTIES_FILE
check_result
sed -i $SED_COMPATIBILITY 's^db.name=.*^db.name='$APP_NAME_LOWER'^' $RED5_PROPERTIES_FILE
check_result
sed -i $SED_COMPATIBILITY 's^<display-name>StreamApp^<display-name>'$APP_NAME'^' $WEB_XML_FILE
check_result
sed -i $SED_COMPATIBILITY 's^<param-value>/StreamApp^<param-value>/'$APP_NAME'^' $WEB_XML_FILE
check_result

chown -R antmedia:antmedia $APP_DIR

if [ $FILE != "1" ]; then
  grep "clusterdb.host=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.host/db.host/g' | xargs -I '{}' sed -i 's/db.host=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
  grep "clusterdb.user=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.user/db.user/g' | xargs -I '{}' sed -i 's/db.user=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
  grep "clusterdb.password=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.password/db.password/g' | xargs -I '{}' sed -i 's/db.password=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
  sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='mongodb'/' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
#else
#  grep "clusterdb.host=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.host/db.host/g' | xargs -I '{}' sed -i 's/db.host=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
#  grep "clusterdb.user=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.user/db.user/g' | xargs -I '{}' sed -i 's/db.user=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
#  grep "clusterdb.password=" $AMS_DIR/conf/red5.properties | sed 's/clusterdb.password/db.password/g' | xargs -I '{}' sed -i 's/db.password=.*/{}/g' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
#  sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='mapdb'/' $AMS_DIR/webapps/$APP_NAME/WEB-INF/red5-web.properties
fi

echo "$APP_NAME is created."
echo -e "${RED}Don't forget to restart Ant Media server.\nsystemctl restart antmedia${NOCOLOR}"

