#!/bin/sh
# Gradle Wrapper entry point; paths with spaces are supported.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "$JAVA_HOME" ]; then
    JAVA_COMMAND="$JAVA_HOME/bin/java"
else
    JAVA_COMMAND=java
fi
exec "$JAVA_COMMAND" -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
