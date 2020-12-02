#!/bin/bash

usage() {
  echo "Usage:"
  echo "$0 [-n APPLICATION_NAME] [-p INSTALLATION_PATH]"
  echo "Options:"
  echo "-n: Application Name is the application name that you want to have. It's mandatory"
  echo "-p: Path is the install location of Ant Media Server which is /usr/local/antmedia by default."
  echo "-h: print this usage"
  echo " "
  echo "Example: "
  echo "$0 -n live"
  echo " "
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

ERROR_MESSAGE="Error: App is not created. Please check the error in the terminal and take a look at the instructions below"

AMS_DIR=/usr/local/antmedia

while getopts 'n:p:h' option
do
  case "${option}" in
    n) APP_NAME=${OPTARG};;
    p) AMS_DIR=${OPTARG};;
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

if [[ -z "$APP_NAME" ]]; then
    echo "Error: Missing parameter APPLICATON_NAME. Check instructions below"
    usage
    exit 1
fi

case $AMS_DIR in
  /*) AMS_DIR=$AMS_DIR;;
  *)  AMS_DIR=$PWD/$AMS_DIR;;
esac

APP_DIR=$AMS_DIR/webapps/$APP_NAME

rm -r $APP_DIR*
check_result

echo "$APP_NAME is deleted."