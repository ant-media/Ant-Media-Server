#!/bin/bash

ps aux | grep org.red5.server.Bootstrap | awk '{print $2}' | xargs kill -9