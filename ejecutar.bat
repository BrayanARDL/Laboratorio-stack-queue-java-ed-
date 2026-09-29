@echo off
REM Compila, ejecuta las pruebas de correctitud, mide tiempos y genera las gráficas (Windows).
REM Uso: ejecutar.bat [maxExp]    (maxExp = 8 => tamaños hasta 10^8; requiere ~6 GB de RAM)
setlocal
cd /d "%~dp0"
set MAXEXP=%1
if "%MAXEXP%"=="" set MAXEXP=8

echo == Compilando ==
if exist out rmdir /s /q out
REM Rutas relativas: funciona aunque la carpeta del proyecto tenga espacios en el nombre
javac -encoding UTF-8 -d out src\listas\*.java src\pilacola\*.java src\benchmark\*.java src\pruebas\*.java || exit /b 1

echo == Pruebas de correctitud ==
java -cp out pruebas.Pruebas || exit /b 1

echo == Benchmark (hasta 10^%MAXEXP%) ==
java -Xms5g -Xmx5g -XX:+UseParallelGC -XX:+AlwaysPreTouch -cp out benchmark.Benchmark %MAXEXP% resultados\tiempos.csv || exit /b 1

echo == Graficas y tablas ==
python analisis\graficar.py resultados\tiempos.csv
