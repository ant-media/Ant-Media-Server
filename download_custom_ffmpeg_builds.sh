#!/bin/bash


if [ ! -f "ffmpeg-7.1-1.5.11-SNAPSHOT-linux-x86_64.jar" ]; then
    wget -O ffmpeg-7.1-1.5.11-SNAPSHOT-linux-x86_64.jar https://storage.sbg.cloud.ovh.net/v1/AUTH_8cb28f9bc6ee43f0a3a1825efbb4311e/test-storage/ffmpeg-7.1-1.5.11-SNAPSHOT-linux-x86_64.jar
fi

if [ ! -f "ffmpeg-7.1-1.5.11-SNAPSHOT-linux-arm64.jar" ]; then
    wget -O ffmpeg-7.1-1.5.11-SNAPSHOT-linux-arm64.jar https://storage.sbg.cloud.ovh.net/v1/AUTH_8cb28f9bc6ee43f0a3a1825efbb4311e/test-storage/ffmpeg-7.1-1.5.11-SNAPSHOT-linux-arm64.jar
fi

       