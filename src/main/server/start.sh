#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=`pwd`; 
fi

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
    JVM_OPTS="-Xms256m -Xmx1g -Xverify:none -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true -XX:MaxMetaspaceSize=128m  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:ParallelGCThreads=2"
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
