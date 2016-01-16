@echo off

if NOT DEFINED RED5_HOME set RED5_HOME=%~dp0

REM Debug options
REM http://docs.oracle.com/javase/8/docs/technotes/guides/jpda/conninv.html#Invocation
REM set JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=myhost:8787,server=y,suspend=n $JAVA_OPTS
REM set JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n $JAVA_OPTS
REM Pre-Java5 options for jdwp
set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n

%RED5_HOME%\red5.bat
