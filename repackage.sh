#!/bin/bash

mvn clean package -P assemble_enterprise -Dmaven.javadoc.skip=true -Dmaven.test.skip=true


