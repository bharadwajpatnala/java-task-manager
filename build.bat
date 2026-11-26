@echo off
if not exist out mkdir out
for /r src %%f in (*.java) do echo %%f>> sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt
if errorlevel 1 exit /b 1
echo Build successful.
