#!/bin/sh

# Locate Java
if [ -z "$JAVA_HOME" ]; then
    JAVA=$(ps -eo args |grep java |awk '{ print $1 }'|grep java)
    if [ -z "$JAVA" ]; then
        # Use default java
        JAVA="java"
        TOOLS_JAR="./tools.jar"
    else
        # Use same java with running process
        JAVA_HOME="$(dirname $JAVA)/../"
        TOOLS_JAR=$JAVA_HOME/lib/tools.jar
    fi
else
    # Use java from JAVA_HOME
    JAVA=$JAVA_HOME/bin/java
    TOOLS_JAR=$JAVA_HOME/lib/tools.jar
fi

# Locate tools.jar
if [ ! -f $TOOLS_JAR ]; then TOOLS_JAR=$JAVA_HOME/tools.jar; fi
if [ ! -f $TOOLS_JAR ]; then TOOLS_JAR=$(dirname $0)/tools.jar; fi
if [ ! -f $TOOLS_JAR ]; then TOOLS_JAR=./tools.jar; fi

# Exec
$JAVA -cp "$TOOLS_JAR:$0" "org.koivula.javatop.Javatop" $@

exit $?
