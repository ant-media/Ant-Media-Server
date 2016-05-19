#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=.; 
fi

# Debug options
# http://docs.oracle.com/javase/8/docs/technotes/guides/jpda/conninv.html#Invocation
#export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=myhost:8787,server=y,suspend=n $JAVA_OPTS"
#export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n $JAVA_OPTS"
# Pre-Java5 options for jdwp
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n $JAVA_OPTS"

# Start Red5
exec $RED5_HOME/red5.sh
