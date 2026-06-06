#!/bin/sh
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ -z "$JAVA_HOME" ]; then
    JAVACMD="java"
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

exec "$JAVACMD" \
    -Xmx64m \
    -Xms64m \
    -Dorg.gradle.appname="gradlew" \
    -classpath "$GRADLE_WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
