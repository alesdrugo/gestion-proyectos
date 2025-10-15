#!/usr/bin/env bash
# Render build script for Java 21 / Spring Boot
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
chmod +x mvnw
./mvnw clean package -DskipTests

