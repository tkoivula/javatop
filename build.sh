#!/bin/sh
mkdir -p build/classes
files=$(find src/java -type f -name '*.java')
javac -source 1.7 -target 1.7 -cp /usr/java/default/lib/tools.jar -sourcepath src/java -d build/classes $files
jar cvfe build/javatop.jar org.koivula.javatop.Javatop -C build/classes .
OUTPUT="build/javatop"
cp src/script/javatop.sh $OUTPUT
echo "# ******* JAR BINARY STARTS FROM HERE ********" >> $OUTPUT
cat build/javatop.jar >> $OUTPUT
echo "Compiled $OUTPUT"
