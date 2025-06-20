#!/bin/bash

# VisualSSS easy runner and cleanup script
# Usage:
#   ./visualsss.sh -d|-r [options]
#   ./visualsss.sh clean
#   ./visualsss.sh clean -b   # Clean Java binaries in bin/

set -e

BIN_DIR="bin"
MAIN_CLASS="VisualSSS"
DEFAULT_RESOURCES="resources"
DEFAULT_SHADOWS="resources/sombras"
DEFAULT_OUTPUT="resources/recovered/recuperado.bmp"

function usage() {
  echo "Uso: $0 -d|-r [opciones]"
  echo "       $0 clean"
  echo "       $0 clean -b   # Clean Java binaries in bin/"
  echo "Opciones:"
  echo "  -secret <file>   Archivo BMP secreto (entrada para -d, salida para -r)"
  echo "  -k <num>         Umbral k (requerido)"
  echo "  -n <num>         Número de particiones n (requerido para -d, opcional para -r)"
  echo "  -dir <dir>       Directorio para portadoras (-d) o sombras (-r) [predeterminado: $DEFAULT_RESOURCES o $DEFAULT_SHADOWS]"
  echo "  -h               Mostrar este mensaje de ayuda"
  exit 1
}

function clean_files() {
  echo "Eliminando archivos de sombras y de salida generados..."
  for dir in "$DEFAULT_RESOURCES" "$DEFAULT_SHADOWS"; do
    if [ -d "$dir" ]; then
      found=$(find "$dir" -maxdepth 1 -type f -name '*.bmp' 2>/dev/null | wc -l)
      if [ "$found" -gt 0 ]; then
        echo "Archivos de sombras encontrados en $dir:"
        find "$dir" -maxdepth 1 -type f -name '*.bmp'
        find "$dir" -maxdepth 1 -type f -name '*.bmp' -exec rm {} \;
        echo "Eliminadas."
      fi
    fi
  done
  if [ -f "$DEFAULT_OUTPUT" ]; then
    echo "Archivo de salida encontrado: $DEFAULT_OUTPUT"
    read -p "¿Eliminar este archivo? [y/N] " yn
    if [[ "$yn" =~ ^[Yy]$ ]]; then
      rm "$DEFAULT_OUTPUT"
      echo "Eliminado."
    else
      echo "Omitido."
    fi
  fi
  echo "Limpieza completada."
}

if [ "$1" == "clean" ]; then
  if [ "$2" == "-b" ]; then
    echo "Eliminando archivos binarios de Java en bin/ ..."
    rm -f $BIN_DIR/*.class
    echo "Archivos binarios de Java eliminados."
  fi
  clean_files
  exit 0
fi

if [ "$1" == "-c" ]; then
  echo "Compilando archivos fuente de Java..."
  COMPILE_CMD="javac -d $BIN_DIR src/VisualSSS.java src/SecretDistributor.java src/SecretRecoverer.java src/LsbSteganography.java src/BmpImage.java src/PermutationTable.java"
  echo "Corriendo $COMPILE_CMD"
  $COMPILE_CMD
  echo "Compilación completada."
  exit 0
fi

if [ $# -lt 1 ]; then
  usage
fi

MODE=""
SECRET=""
K=""
N=""
DIR=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -d)
      MODE="-d"
      shift
      ;;
    -r)
      MODE="-r"
      shift
      ;;
    -secret)
      SECRET="$2"
      shift 2
      ;;
    -k)
      K="$2"
      shift 2
      ;;
    -n)
      N="$2"
      shift 2
      ;;
    -dir)
      DIR="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Opción desconocida: $1"
      usage
      ;;
  esac
done

if [ -z "$MODE" ]; then
  echo "Error: Debe especificar el modo -d (distribuir) o -r (recuperar)."
  usage
fi

if [ -z "$K" ]; then
  read -p "Ingrese el umbral k: " K
fi

if [ "$MODE" == "-d" ] && [ -z "$N" ]; then
  read -p "Ingrese el número de shares n: " N
fi

if [ -z "$SECRET" ]; then
  if [ "$MODE" == "-d" ]; then
    read -p "Ingrese la dirección al archivo BMP secreto: " SECRET
  else
    read -p "Ingrese el nombre del archivo BMP de salida (predeterminado: $DEFAULT_OUTPUT): " SECRET
    SECRET=${SECRET:-$DEFAULT_OUTPUT}
  fi
fi

if [ -z "$DIR" ]; then
  DIR="."
fi

if [ "$MODE" == "-d" ]; then
  echo "Compilando archivos fuente de Java..."
  COMPILE_CMD="javac -d $BIN_DIR src/VisualSSS.java src/SecretDistributor.java src/SecretRecoverer.java src/LsbSteganography.java src/BmpImage.java src/PermutationTable.java"
  echo "Corriendo: $COMPILE_CMD"
  $COMPILE_CMD
fi

if [ "$MODE" == "-r" ]; then
  echo "Compilando archivos fuente de Java..."
  COMPILE_CMD="javac -d $BIN_DIR src/VisualSSS.java src/SecretDistributor.java src/SecretRecoverer.java src/LsbSteganography.java src/BmpImage.java src/PermutationTable.java"
  echo "Corriendo: $COMPILE_CMD"
  $COMPILE_CMD
fi

# Build the Java command
JAVA_CMD="java -cp $BIN_DIR $MAIN_CLASS $MODE -secret $SECRET -k $K"
JAVA_CMD+=" -n $N"
JAVA_CMD+=" -dir $DIR"

echo "Corriendo: $JAVA_CMD"
$JAVA_CMD

echo "Puedes correr '$0 clean' para eliminar los archivos de sombras y el archivo recuperado."