@echo off

SETLOCAL

if NOT DEFINED RED5_HOME set RED5_HOME=%~dp0

REM Previous option set
REM -Xrs -Xms512M -Xmx768M -Xss128K -XX:PermSize=256M -XX:MaxPermSize=512M -XX:NewRatio=2 -XX:MinHeapFreeRatio=20 -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=2 -XX:+UseParallelOldGC -XX:+MaxFDLimit -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000 -Djava.net.preferIPv4Stack=true -Xverify:none

REM Update 06/2008
REM -Xmx768m -Xms256 -Xmn512m -Xss128k -XX:+AggressiveOpts -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000

REM Update 03/2013
set JVM_OPTS=-Xmx1024m -Xms512m -Xss256k -XX:+AggressiveOpts -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -Xverify:none -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:+UseStringCache -XX:+UseParNewGC -XX:SurvivorRatio=16 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true

echo Running High Performance Red5
"%RED5_HOME%\red5.bat" >> "%RED5_HOME%\log\jvm.stdout"

ENDLOCAL