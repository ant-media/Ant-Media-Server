#!/bin/bash


#
# Changes the configuration parameters to switching cluster or standalone mode.
# - First parameter is the mode of the cluster. It can be standalone or cluster
# if the first parameter is cluster than the following parameter should also be given
#
# - Second parameter is the host of the mongodb. 
# - Third parameter is the username of the mongodb
# - Fourth parameter is the password of the mongodb
#
change_server_mode() {
  AMS_INSTALL_LOCATION=/usr/local/antmedia
  
  OS_NAME=`uname`

  if [ "$OS_NAME" = "Darwin" ]; then
    AMS_INSTALL_LOCATION=`pwd`
    SED_COMPATIBILITY='.bak'
  fi
  LIST_APPS=`ls -d $AMS_INSTALL_LOCATION/webapps/*/`

  MODE=$1
  if [ $MODE = "cluster" ]; then
    echo "Mode: cluster"
    DB_TYPE=mongodb
    MONGO_SERVER_IP=$2
      if [ -z "$MONGO_SERVER_IP" ]; then
        echo "No Mongo DB Server specified. Missing parameter"
        usage
        exit 1
      fi
    
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start|<!-- cluster start -->)/<!-- cluster start -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e  's/(cluster end -->|<!-- cluster end -->)/<!-- cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
        
  else
    echo "Mode: standalone"
    DB_TYPE=mapdb
    MONGO_SERVER_IP=localhost
    sed -i $SED_COMPATIBILITY -E -e  's/(<!-- cluster start -->|<!-- cluster start)/<!-- cluster start /g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
    sed -i $SED_COMPATIBILITY -E -e 's/(<!-- cluster end -->|cluster end -->)/cluster end -->/g' $AMS_INSTALL_LOCATION/conf/jee-container.xml
  fi

  sed -i $SED_COMPATIBILITY 's#clusterdb.host=.*#clusterdb.host='$MONGO_SERVER_IP'#' $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY 's/clusterdb.user=.*/clusterdb.user='$3'/' $AMS_INSTALL_LOCATION/conf/red5.properties
  sed -i $SED_COMPATIBILITY 's/clusterdb.password=.*/clusterdb.password='$4'/' $AMS_INSTALL_LOCATION/conf/red5.properties

  for i in $LIST_APPS; do 

    sed -i $SED_COMPATIBILITY 's/db.type=.*/db.type='$DB_TYPE'/' $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY 's#db.host=.*#db.host='$MONGO_SERVER_IP'#' $i/WEB-INF/red5-web.properties  
    sed -i $SED_COMPATIBILITY 's/db.user=.*/db.user='$3'/' $i/WEB-INF/red5-web.properties
    sed -i $SED_COMPATIBILITY 's/db.password=.*/db.password='$4'/' $i/WEB-INF/red5-web.properties
  done
  
  if [ "$OS_NAME" != "Darwin" ]; then
    LOCAL_IPv4=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
    HOST_NAME=`cat /proc/sys/kernel/hostname`
    HOST_LINE="$LOCAL_IPv4 $HOST_NAME"

    # Change /etc/hosts file
    # In docker changing /etc/hosts produces device or resource busy error. 
    # Above commands takes care the changing host file

    # temp hosts file  
    NEW_HOST_FILE=~/.hosts.new
    # cp hosts file
    cp /etc/hosts $NEW_HOST_FILE  
    # delete hostname line from the file  
    sed -i '/'$HOST_NAME'/d' $NEW_HOST_FILE
    # add host line to the file
    echo  "$HOST_LINE" | tee -a $NEW_HOST_FILE
    # change the /etc/hosts file - (mv does not work)
    cp -f $NEW_HOST_FILE /etc/hosts
    # remove temp hosts file
    rm $NEW_HOST_FILE
  fi

}
