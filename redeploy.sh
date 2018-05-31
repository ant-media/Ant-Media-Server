#!/bin/bash

mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true

OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi

ANT_MEDIA_SERVER_DIR=~/softwares/ant-media-server

ANT_MEDIA_SERVER_JAR=./target/ant-media-server.jar

SRC_CONF_DIR=./src/main/server/conf/

#copy red5 jar from target dir to red5 dir
cp  $ANT_MEDIA_SERVER_JAR  $ANT_MEDIA_SERVER_DIR/ant-media-server.jar

OUT=$?

if [ $OUT -ne 0 ]; then
    echo "Cannot copy ant-media-server.jar"
    exit $OUT
fi


cp -rf $SRC_CONF_DIR   $ANT_MEDIA_SERVER_DIR/conf/

OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi



#go to ant media server dir
cd $ANT_MEDIA_SERVER_DIR

#shutdown ant media server
./shutdown.sh


#start ant media server
./start-debug.sh
