#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=`pwd`; 
fi

# JMX options
export JAVA_OPTS="-Djavax.net.ssl.keyStore=$RED5_HOME/conf/keystore.jmx -Djavax.net.ssl.keyStorePassword=password"

# port, username, password
export RED5_OPTS="9999 red5user changeme"
export RED5_MAINCLASS=org.red5.server.Shutdown
exec $RED5_HOME/red5.sh
