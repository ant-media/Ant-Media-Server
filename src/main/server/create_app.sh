#!/bin/bash

APP_NAME=$1
AMS_DIR=$2
AMS_DIR_DEFAULT=/usr/local/antmedia
APP_NAME_LOWER=$(echo $APP_NAME | awk '{print tolower($0)}')
APP_DIR=$AMS_DIR/webapps/$APP_NAME
APP_DIR_DEFAULT=$AMS_DIR_DEFAULT/webapps/$APP_NAME
RED5_PROPERTIES_FILE=$APP_DIR/WEB-INF/red5-web.properties
WEB_XML_FILE=$APP_DIR/WEB-INF/web.xml

OS_NAME=`uname`

if [[ "$OS_NAME" == 'Darwin' ]]; then
  SED_COMPATIBILITY='.bak'
fi

usage() {

	echo ""
	echo "Usage: "
	echo "$0  app_name ams_dir"
	echo ""
	echo "Example usage: "
	echo "$0 live /usr/local/antmedia/"
	echo ""
	echo "If you have any question, send e-mail to contact@antmedia.io"

}

replace() {

	sed -i $SED_COMPATIBILITY 's^webapp.dbName=.*^webapp.dbName='$APP_NAME_LOWER'.db^' $RED5_PROPERTIES_FILE
	sed -i $SED_COMPATIBILITY 's^webapp.contextPath=.*^webapp.contextPath=/'$APP_NAME'^' $RED5_PROPERTIES_FILE
	sed -i $SED_COMPATIBILITY 's^db.app.name=.*^db.app.name='$APP_NAME'^' $RED5_PROPERTIES_FILE
	sed -i $SED_COMPATIBILITY 's^db.name=.*^db.name='$APP_NAME_LOWER'^' $RED5_PROPERTIES_FILE
	sed -i $SED_COMPATIBILITY 's^<display-name>StreamApp^<display-name>'$APP_NAME'^' $WEB_XML_FILE
	sed -i $SED_COMPATIBILITY 's^<param-value>/StreamApp^<param-value>/'$APP_NAME'^' $WEB_XML_FILE

}

if [ "$#" = 0 ] || [ "$#" != 2 ]; then

	usage

elif [ "$#" = 1 ]; then

	WEB_XML_FILE=$APP_DIR_DEFAULT/WEB-INF/web.xml
	RED5_PROPERTIES_FILE=$APP_DIR_DEFAULT/WEB-INF/red5-web.properties
	mkdir $APP_DIR_DEFAULT
	unzip $AMS_DIR_DEFAULT/StreamApp*.war -d $APP_DIR_DEFAULT/
	replace 

elif [ "$#" = 2 ]; then

	mkdir $APP_DIR
	unzip $AMS_DIR/StreamApp*.war -d $APP_DIR/
	replace

fi
