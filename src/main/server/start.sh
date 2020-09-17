#!/bin/bash
#
# Options 
# -g: Use global(Public) IP in network communication. Its value can be true or false. Default value is false.
#
# -s: Use Public IP as server name. Its value can be true or false. Default value is false.
#
# -r: Replace candidate address with server name. Its value can be true or false. Default value is false
#
# -m: Server mode. It can be standalone or cluster. If cluster mode is specified then mongodb host, username and password should also be provided.
#     There is no default value for mode
#
# -h: MongoDB host
#
# -u: MongoDB username
#
# -p: MongoDB password

if [ -z "$RED5_HOME" ]; then 
  BASEDIR=$(dirname "$0")
  cd $BASEDIR
  export RED5_HOME=`pwd`
fi

source $RED5_HOME/conf/functions.sh

USE_GLOBAL_IP=false
USE_PUBLIC_IP_AS_SERVER_NAME=false
REPLACE_CANDIDATE_ADDRESS_WITH_SERVER_NAME=false
SERVER_MODE=
MONGODB_HOST=
MONGODB_USERNAME=
MONGODB_PASSWORD=

while getopts g:s:r:m:h:u:p:t option
do
  case "${option}" in
    g) USE_GLOBAL_IP=${OPTARG};;
    s) USE_PUBLIC_IP_AS_SERVER_NAME=${OPTARG};;
    r) REPLACE_CANDIDATE_ADDRESS_WITH_SERVER_NAME=${OPTARG};;
    m) SERVER_MODE=${OPTARG};;
    h) MONGODB_HOST=${OPTARG};;
    u) MONGODB_USERNAME=${OPTARG};;
    p) MONGODB_PASSWORD=${OPTARG};;
   esac
done

OS_NAME=`uname`

if [ "$OS_NAME" = "Darwin" ]; then
  AMS_INSTALL_LOCATION=`pwd`
  SED_COMPATIBILITY='.bak'
fi

# Set use global IP
sed -i $SED_COMPATIBILITY 's/useGlobalIp=.*/useGlobalIp='$USE_GLOBAL_IP'/' $RED5_HOME/conf/red5.properties

################################################
# Set server name 
SERVER_ADDRESS=
if [ "$USE_PUBLIC_IP_AS_SERVER_NAME" = "true" ]; then
  # get server public ip address
  SERVER_ADDRESS=`curl -s http://checkip.amazonaws.com`
fi
sed -i $SED_COMPATIBILITY 's/server.name=.*/server.name='$SERVER_ADDRESS'/' $RED5_HOME/conf/red5.properties
################################################

################################################
# Replace candidate with Server address property
replaceCandidateAddressWithServer() {
  # first parameter is the properties file of the application
  # second parameter is the value of the property
  if [ $(grep -E "settings.replaceCandidateAddrWithServerAddr" $1 | wc -l) -eq "0" ]; then
    echo " " >> $1 #add new line
    echo "settings.replaceCandidateAddrWithServerAddr=$2" >> $1
  else
    sed -i $SED_COMPATIBILITY 's/settings.replaceCandidateAddrWithServerAddr=.*/settings.replaceCandidateAddrWithServerAddr='$2'/' $1 
  fi
}
LIST_APPS=`ls -d $RED5_HOME/webapps/*/`

for i in $LIST_APPS; do 
  replaceCandidateAddressWithServer $i/WEB-INF/red5-web.properties $REPLACE_CANDIDATE_ADDRESS_WITH_SERVER_NAME
done
################################################

################################################
# Set server mode cluster or standalone. Below method is available is functions.sh
if [ ! -z "${SERVER_MODE}" ]; then
  change_server_mode $SERVER_MODE $MONGODB_HOST $MONGODB_USERNAME $MONGODB_PASSWORD
fi
################################################


P=":" # The default classpath separator
OS=`uname`
case "$OS" in
  CYGWIN*|MINGW*) # Windows Cygwin or Windows MinGW
  P=";" # Since these are actually Windows, let Java know
  ;;
  Linux*)
      LD_LIBRARY_PATH=$RED5_HOME/lib/native
      export LD_LIBRARY_PATH
      # Native path
      NATIVE="-Djava.library.path=$LD_LIBRARY_PATH"
  ;;
  Darwin*)
      DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$RED5_HOME/lib/native
      export DYLD_LIBRARY_PATH
      # Native path
      NATIVE="-Djava.library.path=$DYLD_LIBRARY_PATH"
  ;;
  SunOS*)
      if [ -z "$JAVA_HOME" ]; then 
          export JAVA_HOME=/opt/local/java/sun6; 
      fi
  ;;
  *)
  # Do nothing
  ;;
esac

echo "Running on " $OS
# JAVA options
# You can set JVM additional options here if you want
if [ -z "$JVM_OPTS" ]; then 
    JVM_OPTS="-Xms256m -Djava.awt.headless=true -Xverify:none -XX:+HeapDumpOnOutOfMemoryError -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true -XX:MaxMetaspaceSize=128m  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:ParallelGCThreads=10 -XX:ConcGCThreads=5"
fi
# Set up security options
SECURITY_OPTS="-Djava.security.debug=failure -Djava.security.egd=file:/dev/./urandom"
# Set up tomcat options
TOMCAT_OPTS="-Dcatalina.home=$RED5_HOME -Dcatalina.useNaming=true -Djava.net.preferIPv4Stack=true"
# Jython options
JYTHON="-Dpython.home=lib"

export JAVA_OPTS="$SECURITY_OPTS $JAVA_OPTS $JVM_OPTS $TOMCAT_OPTS $NATIVE $JYTHON"

if [ -z "$RED5_MAINCLASS" ]; then
  export RED5_MAINCLASS=org.red5.server.Bootstrap
fi

if [ -z "$RED5_OPTS" ]; then
  export RED5_OPTS=9999
fi

for JAVA in "${JAVA_HOME}/bin/java" "${JAVA_HOME}/Home/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x "$JAVA" ]
  then
    break
  fi
done

if [ ! -x "$JAVA" ]
then
  echo "Unable to locate Java. Please set JAVA_HOME environment variable."
  exit
fi

export RED5_CLASSPATH="${RED5_HOME}/ant-media-server-service.jar${P}${RED5_HOME}/conf${P}${CLASSPATH}"

# start Red5
echo "Starting Ant Media Server"
exec "$JAVA" -Dred5.root="${RED5_HOME}" $JAVA_OPTS -cp "${RED5_CLASSPATH}" "$RED5_MAINCLASS" $RED5_OPTS
