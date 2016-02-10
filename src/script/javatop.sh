#!/bin/sh
if [ -z "$JAVA_HOME" ]; then
    GUESS=true

    JAVA=$(ps -eo args |grep java |awk '{ print $1 }'|grep java)
    JAVA_HOME="$(dirname $JAVA)/../"
fi
TOOLS_JAR=$JAVA_HOME/lib/tools.jar
#SA_JDI_JAR=$JAVA_HOME/lib/sa-jdi.jar

if [ ! -f $TOOLS_JAR ]; then
    if [ $GUESS ]; then 
        echo "JDK not found. Try setting JAVA_HOME."
    else
        echo "Java found but no tools.jar. This command requires JDK. Change JAVA_HOME to point into JDK instead of JRE."
    fi    
    exit 2
fi
$JAVA_HOME/bin/java -cp "$TOOLS_JAR:$0" "org.koivula.javatop.Javatop" $@
exit 0
