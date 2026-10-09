@echo off
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo COMPILE FAILED
    pause
    exit /b 1
)
java -cp out ug.ac.vu.greenview.tests.SystemTests
pause
