#!/bin/sh
mkdir -p build/classes
files=$(find test/java -type f -name '*.java')
javac -cp /usr/java/default/lib/tools.jar -sourcepath test/java -d build/classes $files
java -cp build/classes/ org.koivula.javatop.Sample

