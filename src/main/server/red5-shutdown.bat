@echo off

SETLOCAL

if NOT DEFINED RED5_HOME set RED5_HOME=%~dp0

set JAVA_OPTS=-Djavax.net.ssl.keyStore="%RED5_HOME%\conf\keystore.jmx" -Djavax.net.ssl.keyStorePassword=password
set RED5_MAINCLASS=org.red5.server.Shutdown
set RED5_OPTS=9999 red5user changeme

"%RED5_HOME%\red5.bat"

ENDLOCAL
