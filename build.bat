@echo off
setlocal

set SRC=src
set OUT=build\classes
set LIB=lib
set WAR=build\app.war
set TOMCAT_WEBAPPS=C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps
set WEBAPP=src\webapp
set SOURCES=build\sources.txt

echo === Nettoyage ===
if exist build rmdir /s /q build
mkdir %OUT%

echo === Collecte des sources ===
dir /s /b %SRC%\*.java > %SOURCES%

echo === Compilation ===
javac -cp "%LIB%\*" -d %OUT% @%SOURCES%

if %errorlevel% neq 0 (
    echo Compilation echouee.
    exit /b 1
)

echo === Creation du WAR ===
mkdir build\war\WEB-INF\classes
xcopy /s /q %OUT% build\war\WEB-INF\classes\
xcopy /s /q %WEBAPP%\WEB-INF build\war\WEB-INF\

jar cf %WAR% -C build\war .

echo === Deploiement vers Tomcat ===
copy /y %WAR% "%TOMCAT_WEBAPPS%\"

if %errorlevel% neq 0 (
    echo Deploiement echoue. Verifier que Tomcat est accessible.
    exit /b 1
)

echo === Termine : deploye dans %TOMCAT_WEBAPPS% ===
endlocal
