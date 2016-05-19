@echo off

SETLOCAL

if NOT DEFINED RED5_HOME set RED5_HOME=%~dp0

set RED5_MAINCLASS=org.red5.server.Shutdown

"%RED5_HOME%\red5.bat"

ENDLOCAL
