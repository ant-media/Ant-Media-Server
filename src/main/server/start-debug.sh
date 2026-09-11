#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=.; 
fi

export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:8787 $JAVA_OPTS"

# Start Red5
exec $RED5_HOME/start.sh "$@"
