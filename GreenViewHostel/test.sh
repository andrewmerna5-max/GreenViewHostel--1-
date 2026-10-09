#!/bin/sh
mkdir -p out
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt || { echo "COMPILE FAILED"; exit 1; }
java -cp out ug.ac.vu.greenview.tests.SystemTests
