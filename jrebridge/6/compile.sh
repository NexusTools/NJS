#!/bin/bash

rm bridge.jar
javac -cp ../../build/classes *.java
mkdir -p net/nexustools/njs/jrebridge
mv *.class net/nexustools/njs/jrebridge
zip -r bridge.jar net
rm -r net
