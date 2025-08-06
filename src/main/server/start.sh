#!/bin/bash
#
# Options:
#
# -g: Use global(Public) IP in network communication. Its value can be true or false. Default value is false.
#
# -s: Use Public IP as server name. Its value can be true or false. Default value is false.
#
# -r: Replace candidate address with server name. Its value can be true or false. Default value is false
#
# -m: Server mode. It can be standalone or cluster. If cluster mode is specified then mongodb host, username and password should also be provided.
#     There is no default value for mode
#
# -h: MongoDB or Redist host. It's either IP address or full connection string such as mongodb://[username:password@]host1[:port1] or mongodb+srv://[username:password@]host1[:port1] or redis://[username:password@]host1[:port1] or redis yaml configuration
#
# -u: MongoDB username: Deprecated. Just give the username in the connection string with -h parameter
#
# -p: MongoDB password: Deprecated. Just give the password in the connection string with -h parameter
#
# -l: Licence Key

# -a: TURN/STUN Server URL for the server side. It should start with "turn:" or "stun:" such as stun:stun.l.google.com:19302 or turn:ovh36.antmedia.io
#     this url is not visible to frontend users just for server side.
#
# -n: TURN Server Usermame: Provide the TURN server username to get relay candidates.
#
# -w: TURN Server Password: Provide the TURN server password to get relay candidates.
#
# -k: Kafka Address: Provide the Kafka URL address to collect data. (It must contain the port number. Example: localhost:9092)
#
# -j: JVM Memory Options(-Xms1g -Xmx4g): Set the Java heap size. Default value is 1g. (Example usage: ./start.sh -j "-Xms1g -XmX4g")
#
# -c: CPU Limit: Set the CPU limit percentage that server does not exceed. Default value is 75. 
#       If CPU is more than this value, server reports highResourceUsage and does not allow publish or play.
#       Example usage: ./start.sh -c 60
#
# -e: Memory Limit: Set the Memory Limit percentage that server does not exceed. Default value is 75
#       If Memory usage is more than this value, server reports highResourceUsage and does not allow publish or play
#       Example usage: ./start.sh -e 60

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
DB_URL=
DB_USERNAME=
DB_PASSWORD=
LICENSE_KEY=
CPU_LIMIT=
MEMORY_LIMIT=

# Set the default value for JVM_MEMORY_OPTIONS
JVM_MEMORY_OPTIONS="-Xms1g"


while getopts g:s:r:m:h:u:p:l:a:n:w:k:j:c:e:t option
do
  case "${option}" in
    g) USE_GLOBAL_IP=${OPTARG};;
    s) USE_PUBLIC_IP_AS_SERVER_NAME=${OPTARG};;
    r) REPLACE_CANDIDATE_ADDRESS_WITH_SERVER_NAME=${OPTARG};;
    m) SERVER_MODE=${OPTARG};;
    h) DB_URL=${OPTARG};;
    u) DB_USERNAME=${OPTARG};;
    p) DB_PASSWORD=${OPTARG};;
    l) LICENSE_KEY=${OPTARG};;
    a) TURN_URL=${OPTARG};;
    n) TURN_USERNAME=${OPTARG};;
    w) TURN_PASSWORD=${OPTARG};;
    k) KAFKA_URL=${OPTARG};;
    j) JVM_MEMORY_OPTIONS=${OPTARG};;
    c) CPU_LIMIT=${OPTARG};;
    e) MEMORY_LIMIT=${OPTARG};;
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

if [ ! -z "$DB_USERNAME" ] && [ ! -z "$DB_PASSWORD" ]; then
  echo -e "\033[0;31mYou can just use mongodb://[username:password@]host1[:port1] or mongodb+srv://[username:password@]host1[:port1] connection strings with -h parameter. No need give mongodb username and password parameters explicityly. These parameters are deprecated.\033[0m"
fi

################################################
# Set server mode cluster or standalone. Below method is available is functions.sh
if [ ! -z "${SERVER_MODE}" ]; then
  change_server_mode $SERVER_MODE $DB_URL $DB_USERNAME $DB_PASSWORD
fi
################################################
# Set the license key
if [ ! -z "${LICENSE_KEY}" ]; then
  sed -i $SED_COMPATIBILITY 's/server.licence_key=.*/server.licence_key='$LICENSE_KEY'/' $RED5_HOME/conf/red5.properties
fi
# Set the kafka address
if [ ! -z "${KAFKA_URL}" ]; then
  sed -i $SED_COMPATIBILITY 's/server.kafka_brokers=.*/server.kafka_brokers='$KAFKA_URL'/' $RED5_HOME/conf/red5.properties
fi
# Turn server configuration.
if [ ! -z "${TURN_URL}" ]; then
  for applist in $LIST_APPS; do
    if [ $(grep -E "settings.webrtc.stunServerURI" $applist/WEB-INF/red5-web.properties | wc -l) -eq "0" ]; then
        echo " " >> $applist/WEB-INF/red5-web.properties #add new line
        echo  "settings.webrtc.stunServerURI=${TURN_URL}" >> $applist/WEB-INF/red5-web.properties
    else
        sed -i $SED_COMPATIBILITY 's/settings.webrtc.stunServerURI=.*/settings.webrtc.stunServerURI='${TURN_URL}'/' $applist/WEB-INF/red5-web.properties
    fi
 done
fi

if [ ! -z "${TURN_USERNAME}" ]; then
  for applist in $LIST_APPS; do

    if [ $(grep -E "settings.webrtc.turnServerUsername" $applist/WEB-INF/red5-web.properties | wc -l) -eq "0"  ]; then
        echo " " >> $applist/WEB-INF/red5-web.properties #add new line
        echo  "settings.webrtc.turnServerUsername=${TURN_USERNAME}" >> $applist/WEB-INF/red5-web.properties
    else
        sed -i $SED_COMPATIBILITY 's/settings.webrtc.turnServerUsername=.*/settings.webrtc.turnServerUsername='${TURN_USERNAME}'/' $applist/WEB-INF/red5-web.properties
    fi
 done
fi

if [ ! -z ${TURN_PASSWORD} ]; then
  for applist in $LIST_APPS; do
    if [ $(grep -E "settings.webrtc.turnServerCredential" $applist/WEB-INF/red5-web.properties | wc -l) -eq "0"  ]; then
        echo " " >> $applist/WEB-INF/red5-web.properties #add new line
        echo  "settings.webrtc.turnServerCredential=${TURN_PASSWORD}" >> $applist/WEB-INF/red5-web.properties
    else
        sed -i $SED_COMPATIBILITY 's/settings.webrtc.turnServerCredential=.*/settings.webrtc.turnServerCredential='${TURN_PASSWORD}'/' $applist/WEB-INF/red5-web.properties
    fi
 done
fi

if [ ! -z ${CPU_LIMIT} ]; then
  sed -i $SED_COMPATIBILITY 's/server.cpu_limit=.*/server.cpu_limit='$CPU_LIMIT'/' $RED5_HOME/conf/red5.properties
fi

if [ ! -z ${MEMORY_LIMIT} ]; then
  sed -i $SED_COMPATIBILITY 's/server.memory_limit_percentage=.*/server.memory_limit_percentage='$MEMORY_LIMIT'/' $RED5_HOME/conf/red5.properties
fi

P=":" # The default classpath separator
OS=`uname`
case "$OS" in
  CYGWIN*|MINGW*) # Windows Cygwin or Windows MinGW
  P=";" # Since these are actually Windows, let Java know
  ;;
  Linux*)
      ARCH=`uname -m`
      LD_LIBRARY_PATH=$RED5_HOME/lib/native-linux-$ARCH
      export LD_LIBRARY_PATH
      # Native path
      # First arch parameter is running start.sh directly and second lib/native parameter is installation for init.d scripts
      NATIVE="-Djava.library.path=$LD_LIBRARY_PATH:$RED5_HOME/lib/native"
  ;;
  Darwin*)
      DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$RED5_HOME/lib/native-mac
      export DYLD_LIBRARY_PATH
      # Native path
      NATIVE="-Djava.library.path=$DYLD_LIBRARY_PATH:$RED5_HOME/lib/native"
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
    JVM_OPTS="$JVM_MEMORY_OPTIONS -Djava.io.tmpdir=/tmp -XX:ErrorFile=/var/log/antmedia/hs_err_${TIMESTAMP}.log -Djava.awt.headless=true -Xverify:none -XX:+HeapDumpOnOutOfMemoryError -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true -XX:MaxMetaspaceSize=128m  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:ParallelGCThreads=10 -XX:ConcGCThreads=5 -Djava.system.class.loader=org.red5.server.classloading.ServerClassLoader -Xshare:off "
fi
# Set up security options
SECURITY_OPTS="-Djava.security.debug=failure -Djava.security.egd=file:/dev/./urandom"
# Set up tomcat options
TOMCAT_OPTS="-Dcatalina.home=$RED5_HOME -Dcatalina.useNaming=true -Djava.net.preferIPv4Stack=true"
# Jython options
JYTHON="-Dpython.home=lib"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

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

# create log directory if not exist
if [ ! -d "/var/log/antmedia" ]
then
  mkdir -p /var/log/antmedia
  OUT=$?
  if [ $OUT -ne 0 ]; then
    echo "You're likely running start.sh directly. The problem is /var/log/antmedia directory cannot not created"
    echo "If you run the start.sh with your current user, please run the following commands"
    echo "sudo mkdir -p /var/log/antmedia"
    echo "sudo chown $USER /var/log/antmedia"
    exit $OUT
  fi
fi

#create soft link if not exists
if [ ! -L  "${RED5_HOME}/log" ]
then
  ln -sf /var/log/antmedia ${RED5_HOME}/log
fi


# start Ant Media Server

if [ "$RED5_MAINCLASS" = "org.red5.server.Bootstrap" ]; then
    # start Ant Media Server
    echo "Starting Ant Media Server"
elif [ "$RED5_MAINCLASS" = "org.red5.server.Shutdown" ]; then
    # stop Ant Media Server
    echo "Stopping Ant Media Server"
fi
exec "$JAVA" -Dred5.root="${RED5_HOME}" $JAVA_OPTS -cp "${RED5_CLASSPATH}" "$RED5_MAINCLASS" $RED5_OPTS 2>>${RED5_HOME}/log/antmedia-error.log
