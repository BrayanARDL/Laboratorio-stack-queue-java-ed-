#!/usr/bin/env bash
# Compila, ejecuta las pruebas de correctitud, mide tiempos y genera las gráficas.
# Uso: ./ejecutar.sh [maxExp]     (maxExp = 8 => tamaños hasta 10^8; requiere ~6 GB de RAM)
set -e
cd "$(dirname "$0")"
MAXEXP="${1:-8}"

echo "== Compilando =="
rm -rf out
javac -encoding UTF-8 -d out $(find src -name "*.java")

echo "== Pruebas de correctitud =="
java -cp out pruebas.Pruebas

echo "== Benchmark (hasta 10^$MAXEXP) =="
# -Xms/-Xmx: heap fijo de 5 GB para que quepa la lista de 10^8 nodos.
# ParallelGC: recolector sin hilos concurrentes que interfieran con la medición.
# AlwaysPreTouch: reserva la memoria al inicio (evita fallos de página durante las mediciones).
java -Xms5g -Xmx5g -XX:+UseParallelGC -XX:+AlwaysPreTouch -cp out benchmark.Benchmark "$MAXEXP" resultados/tiempos.csv

echo "== Gráficas y tablas =="
python3 analisis/graficar.py resultados/tiempos.csv
