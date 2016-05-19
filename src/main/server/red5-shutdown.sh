#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=`pwd`; 
fi

export RED5_MAINCLASS=org.red5.server.Shutdown

exec $RED5_HOME/red5.sh
