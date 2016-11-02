#!/bin/bash

if [ "$1" == "force" ]; then
  OS=`uname`
  case "$OS" in
    Linux*)
      # give peace a chance
      ps aux | grep -i red5 | awk {'print $2'} | xargs kill -15
      # wait a moment
      sleep 1
      # send the nukes
      ps aux | grep -i red5 | awk {'print $2'} | xargs kill -9
    ;;
    Darwin*)
      ps aux | grep -i red5 | grep -v extension-process | tr -s ' ' | cut -d ' ' -f2 | xargs kill
    ;;
    *)
      # Do nothing
    ;;
  esac
else
  if [ -z "$RED5_HOME" ]; then 
    export RED5_HOME=`pwd`; 
  fi
  export RED5_MAINCLASS=org.red5.server.Shutdown
  exec $RED5_HOME/red5.sh
fi


