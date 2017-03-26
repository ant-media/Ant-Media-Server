#!/bin/bash

mvn install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true

RED5_DIR=../../softwares/red5-server

RED5_JAR=./target/red5-server-1.0.9-M2.jar

SRC_CONF_DIR=./src/main/server/conf/

#copy red5 jar from target dir to red5 dir
cp  $RED5_JAR  $RED5_DIR/red5-server.jar

cp -rf $SRC_CONF_DIR   $RED5_DIR/conf

#go to red5 dir
cd $RED5_DIR

#shutdown red5 
#./red5-shutdown.sh


#start red5
#./red5-debug.sh
