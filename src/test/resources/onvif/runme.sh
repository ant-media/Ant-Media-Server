#!/bin/sh
cd /usr/local/onvif/happytime-rtsp-server
./rtspserver &
cd ..
./onvifserver &

