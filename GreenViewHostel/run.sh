#!/bin/sh
# Compiles and runs Green View Hostel. Needs JDK 17 or newer.
mkdir -p out
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt || { echo "COMPILE FAILED"; exit 1; }
java -cp out ug.ac.vu.greenview.app.GreenViewHostelApp
