#!/bin/bash

#This is a simple script just edit css file to show message in community

APP_PATH=$1

OS_NAME=`uname`
if [ "$OS_NAME" = "Darwin" ]; then
    SED_COMPATIBILITY='.bak'
fi

sed -i $SED_COMPATIBILITY 's/\.enterprise-feature.*/\.enterprise-feature{display:block;}/' $APP_PATH/css/common.css 