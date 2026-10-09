@echo off
rem Compiles and runs Green View Hostel. Needs JDK 17 or newer (javac must work in this window).
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo COMPILE FAILED - see the messages above.
    pause
    exit /b 1
)
java -cp out ug.ac.vu.greenview.app.GreenViewHostelApp
pause
