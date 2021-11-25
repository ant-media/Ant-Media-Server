#!/bin/bash

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

check() {
  OUT=$?
  if [ $OUT -ne 0 ]; then
    echo "There is a problem in installing the tensorflow plugin. Please check the logs and if you don\'t resolve the problem, ask for help from community or support@antmedia.io"
    exit $OUT
  fi
}

DETECTION_MODEL_ZIP=detection_model.zip
DETECTION_MODEL_ZIP_URL=https://github.com/ant-media/Ant-Media-Server/raw/master/src/main/server/lib/detection_model.zip
$SUDO wget -O $DETECTION_MODEL_ZIP $DETECTION_MODEL_ZIP_URL
check

$SUDO unzip $DETECTION_MODEL_ZIP
check

$SUDO wget -O lib/libtensorflow_jni-1.15.0.jar https://repo1.maven.org/maven2/org/tensorflow/libtensorflow_jni/1.15.0/libtensorflow_jni-1.15.0.jar
check

$SUDO wget -O lib/libtensorflow-1.15.0.jar https://repo1.maven.org/maven2/org/tensorflow/libtensorflow/1.15.0/libtensorflow-1.15.0.jar -P lib
check

$SUDO wget -O lib/tensorflow-1.15.0.jar https://repo1.maven.org/maven2/org/tensorflow/tensorflow/1.15.0/tensorflow-1.15.0.jar -P lib
check

$SUDO chown -R antmedia:antmedia .
check

$SUDO service antmedia restart

