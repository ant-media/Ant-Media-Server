#!/bin/bash

mvn install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true

RED5_DIR=/home/faraklit/softwares/red5-server

RED5_JAR=/home/faraklit/git/red5-plus-server/target/red5-server-1.0.9-M2.jar

SRC_CONF_DIR=/home/faraklit/git/red5-plus-server/src/main/server/conf/

#copy red5 jar from target dir to red5 dir
cp  $RED5_JAR  $RED5_DIR/red5-server.jar

cp -rf $SRC_CONF_DIR   $RED5_DIR/conf

#go to red5 dir
cd $RED5_DIR

#shutdown red5 
#./red5-shutdown.sh


#start red5
#./red5-debug.sh
