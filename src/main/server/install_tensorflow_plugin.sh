#!/bin/bash

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

DETECTION_MODEL_ZIP=https://github.com/ant-media/Ant-Media-Server/blob/master/src/main/server/lib/detection_model.zip
$SUDO wget $DETECTION_MODEL_ZIP
$SUDO unzip $DETECTION_MODEL_ZIP

$SUDO wget https://repo1.maven.org/maven2/org/tensorflow/libtensorflow_jni/1.15.0/libtensorflow_jni-1.15.0.jar -P lib
$SUDO wget https://repo1.maven.org/maven2/org/tensorflow/libtensorflow/1.15.0/libtensorflow-1.15.0.jar -P lib
$SUDO wget https://repo1.maven.org/maven2/org/tensorflow/tensorflow/1.15.0/tensorflow-1.15.0.jar -P lib

$SUDO chown -R antmedia:antmedia .
$SUDO service antmedia restart

