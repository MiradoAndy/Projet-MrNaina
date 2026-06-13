@echo off
echo Nettoyage...
if exist build rmdir /s /q build
mkdir build\classes

echo Compilation...
javac -cp "lib\*" -d build\classes src\*.java

if %errorlevel% neq 0 (
    echo Compilation failed.
    exit /b 1
)

echo Creation du jar...
jar cf Framework.jar -C build\classes .

echo Termine.