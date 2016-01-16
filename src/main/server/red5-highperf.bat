@echo off

SETLOCAL

if NOT DEFINED RED5_HOME set RED5_HOME=%~dp0

set JVM_OPTS=-Xmx1024m -Xms512m -Xss256k -XX:+AggressiveOpts -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -Xverify:none -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:+UseStringCache -XX:SurvivorRatio=16 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true

echo Running High Performance Red5
"%RED5_HOME%\red5.bat" >> "%RED5_HOME%\log\jvm.stdout"

ENDLOCAL