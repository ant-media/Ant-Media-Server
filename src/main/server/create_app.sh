#!/bin/bash

# Script to create an application in Ant Media Server

# Function to display usage instructions
usage() {
  echo "Usage:"
  echo "$0 -n APPLICATION_NAME [-p INSTALLATION_PATH] [-w true|false] [-h DATABASE_HOST] [-f WAR_FILE]"
  echo "Options:"
  echo "-n: Name of the application that you want to create. (Mandatory)"
  echo "-p: Path is the installation location of Ant Media Server. Default: /usr/local/antmedia"
  echo "-w: Flag to deploy the application as a war file. Default: false"
  echo "-h: Database host with optional username and password (e.g., mongodb://username:password@host:port)."
  echo "-f: War file path for custom app deployment"
  echo "-q: Print this usage"
  echo " "
  echo "Example: "
  echo "$0 -n live -w"
  echo "$0 -n live -h mongodb://localhost:27017"
  echo " "
  echo "If you have any questions, send an email to contact@antmedia.io"
}

# Print all parameters passed to the script
echo "All parameters: $@"

ERROR_MESSAGE="Error: Application creation failed. Please check the error in the terminal and refer to the instructions."

AMS_DIR=/usr/local/antmedia
AS_WAR=false
MONGO_PORT=27017

# Parse command-line options
while getopts 'n:p:w:h:f:q' option; do
  case "${option}" in
    n) APP_NAME=${OPTARG} ;;
    p) AMS_DIR=${OPTARG} ;;
    w) AS_WAR=${OPTARG} ;;
    h) DATABASE_HOST=${OPTARG} ;;
    f) WAR_FILE=${OPTARG} ;;
    q) usage
       exit 1 ;;
  esac
done

# Check if APPLICATION_NAME is provided as an argument
if [ -z "$APP_NAME" ]; then
  APP_NAME=$1

  if [ ! -z "$2" ]; then
    AMS_DIR=$2
  fi
fi

# Check if APPLICATION_NAME is still missing
if [[ -z "$APP_NAME" ]]; then
  echo "Error: Missing parameter APPLICATION_NAME. Check the instructions below."
  usage
  exit 1
fi

# Set the WAR file path if not provided
if [[ -z "$WAR_FILE" ]]; then
  WAR_FILE=$AMS_DIR/StreamApp*.war
fi

# Set AS_WAR flag to false if not provided
if [[ -z "$AS_WAR" ]]; then
  AS_WAR=false
fi

# Format the AMS_DIR path
case $AMS_DIR in
  /*) AMS_DIR=$AMS_DIR ;;
  *) AMS_DIR=$PWD/$AMS_DIR ;;
esac

APP_NAME_LOWER=$(echo $APP_NAME | awk '{print tolower($0)}')
APP_DIR=$AMS_DIR/webapps/$APP_NAME
RED5_PROPERTIES_FILE=$APP_DIR/WEB-INF/red5-web.properties
WEB_XML_FILE=$APP_DIR/WEB-INF/web.xml

# Create the application directory
mkdir -p $APP_DIR

# Copy the WAR file to the application directory
cp $WAR_FILE $APP_DIR

cd $APP_DIR

WAR_FILE_NAME=$(basename $WAR_FILE)

# Unzip the WAR file
unzip $WAR_FILE_NAME

# Remove the WAR file
rm $WAR_FILE_NAME

OS_NAME=$(uname)

if [[ "$OS_NAME" == 'Darwin' ]]; then
  SED_COMPATIBILITY='.bak'
fi

# Update database configuration in red5-web.properties
if [[ -n "$DATABASE_HOST" ]]; then
  # Extract database type, username, password, host, and port from DATABASE_HOST
  DATABASE_TYPE=$(echo $DATABASE_HOST | cut -d ':' -f 1)
  HOST_PORT=$(echo $DATABASE_HOST | cut -d '@' -f 2)
  if [[ "$DATABASE_TYPE" == "mongodb" ]]; then
    # Extract username, password, host, and port for MongoDB
    MONGO_USERNAME=$(echo $HOST_PORT | cut -d ':' -f 1 | cut -d '/' -f 3)
    MONGO_PASSWORD=$(echo $MONGO_USERNAME | cut -d ':' -f 2)
    MONGO_HOST=$(echo $HOST_PORT | cut -d '@' -f 2 | cut -d ':' -f 1)
    MONGO_PORT=$(echo $HOST_PORT | cut -d '@' -f 2 | cut -d ':' -f 2)
    if [ -n "$MONGO_USERNAME" ]; then
      # If MongoDB username is provided, add it to the connection string
      MONGO_CONNECTION="mongodb://$MONGO_USERNAME:$MONGO_PASSWORD@$MONGO_HOST:$MONGO_PORT"
    else
      MONGO_CONNECTION="mongodb://$MONGO_HOST:$MONGO_PORT"
    fi
    sed -i $SED_COMPATIBILITY 's#db.type=.*#db.type=mongodb#' $RED5_PROPERTIES_FILE
    sed -i $SED_COMPATIBILITY 's#db.host=.*#db.host='$MONGO_CONNECTION'#' $RED5_PROPERTIES_FILE
  fi
fi

# Update other properties in red5-web.properties
sed -i $SED_COMPATIBILITY 's#webapp.dbName=.*#webapp.dbName='$APP_NAME_LOWER'.db#' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's#webapp.contextPath=.*#webapp.contextPath=/'$APP_NAME'#' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's#db.app.name=.*#db.app.name='$APP_NAME'#' $RED5_PROPERTIES_FILE
sed -i $SED_COMPATIBILITY 's#db.name=.*#db.name='$APP_NAME_LOWER'#' $RED5_PROPERTIES_FILE

# Update display name and context path in web.xml
sed -i $SED_COMPATIBILITY 's#<display-name>StreamApp#<display-name>'$APP_NAME'#' $WEB_XML_FILE
sed -i $SED_COMPATIBILITY 's#<param-value>/StreamApp#<param-value>/'$APP_NAME'#' $WEB_XML_FILE

# Perform additional operations based on AS_WAR flag
if [[ $AS_WAR == "true" ]]; then
  echo "Application will be deployed as a WAR file"
  cd $APP_DIR
  zip -r ../$APP_NAME.war *
  rm -r $APP_DIR
else
  echo "Application is deployed as a directory."
  chown -R antmedia:antmedia $APP_DIR -f
fi

echo "$APP_NAME is created."
