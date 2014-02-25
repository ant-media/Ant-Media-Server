#!/bin/bash

if [ -z "$RED5_HOME" ]; then export RED5_HOME=.; fi

export JVM_OPTS="-Xmx1024m -Xms512m -Xss256k -XX:+AggressiveOpts -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -Xverify:none -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:+UseStringCache -XX:+UseParNewGC -XX:SurvivorRatio=16 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true"

# start Red5
echo "Setting Hi Performance Options"
exec $RED5_HOME/red5.sh >> $RED5_HOME/log/jvm.stdout 2>&1 &
