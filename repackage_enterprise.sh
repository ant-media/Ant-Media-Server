#!/bin/bash

mvn -s "$MAVEN_SETTINGS_FILE" clean package -P assemble_enterprise -Dmaven.javadoc.skip=true -Dmaven.test.skip=true