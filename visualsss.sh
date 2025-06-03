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
DEFAULT_OUTPUT="recuperado.bmp"

function usage() {
  echo "Usage: $0 -d|-r [options]"
  echo "       $0 clean"
  echo "       $0 clean -b   # Clean Java binaries in bin/"
  echo "Options:"
  echo "  -secret <file>   Secret BMP file (input for -d, output for -r)"
  echo "  -k <num>         Threshold k (required)"
  echo "  -n <num>         Number of shares n (required for -d, optional for -r)"
  echo "  -dir <dir>       Directory for carriers (-d) or shadows (-r) [default: $DEFAULT_RESOURCES or $DEFAULT_SHADOWS]"
  echo "  -h               Show this help message"
  exit 1
}

function clean_files() {
  echo "Cleaning generated shadow and output files..."
  for dir in "$DEFAULT_RESOURCES" "$DEFAULT_SHADOWS"; do
    if [ -d "$dir" ]; then
      found=$(ls "$dir"/sombra*.bmp 2>/dev/null | wc -l)
      if [ "$found" -gt 0 ]; then
        echo "Found shadow files in $dir:"
        ls "$dir"/sombra*.bmp
        read -p "Delete these files? [y/N] " yn
        if [[ "$yn" =~ ^[Yy]$ ]]; then
          rm "$dir"/sombra*.bmp
          echo "Deleted."
        else
          echo "Skipped."
        fi
      fi
    fi
  done
  if [ -f "$DEFAULT_RESOURCES/$DEFAULT_OUTPUT" ]; then
    echo "Found output file: $DEFAULT_RESOURCES/$DEFAULT_OUTPUT"
    read -p "Delete this file? [y/N] " yn
    if [[ "$yn" =~ ^[Yy]$ ]]; then
      rm "$DEFAULT_RESOURCES/$DEFAULT_OUTPUT"
      echo "Deleted."
    else
      echo "Skipped."
    fi
  fi
  echo "Cleanup done."
}

if [ "$1" == "clean" ]; then
  if [ "$2" == "-b" ]; then
    echo "Cleaning Java binaries in bin/ ..."
    rm -f $BIN_DIR/*.class
    echo "Java binaries cleaned."
  fi
  clean_files
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
      echo "Unknown option: $1"
      usage
      ;;
  esac
done

if [ -z "$MODE" ]; then
  echo "Error: Must specify -d (distribute) or -r (recover) mode."
  usage
fi

if [ -z "$K" ]; then
  read -p "Enter threshold k: " K
fi

if [ "$MODE" == "-d" ] && [ -z "$N" ]; then
  read -p "Enter number of shares n: " N
fi

if [ -z "$SECRET" ]; then
  if [ "$MODE" == "-d" ]; then
    read -p "Enter path to secret BMP file: " SECRET
  else
    read -p "Enter output BMP file name (default: $DEFAULT_OUTPUT): " SECRET
    SECRET=${SECRET:-$DEFAULT_OUTPUT}
  fi
fi

if [ -z "$DIR" ]; then
  if [ "$MODE" == "-d" ]; then
    DIR="$DEFAULT_RESOURCES"
  else
    DIR="$DEFAULT_SHADOWS"
  fi
fi

if [ "$1" != "clean" ]; then
  echo "Compiling Java sources..."
  javac -d $BIN_DIR src/VisualSSS.java src/SecretDistributor.java src/SecretRecoverer.java src/LsbSteganography.java src/BmpImage.java src/PermutationTable.java
fi

# Build the Java command
JAVA_CMD="java -cp $BIN_DIR $MAIN_CLASS $MODE -secret $SECRET -k $K"
if [ "$MODE" == "-d" ]; then
  if [ -n "$N" ]; then
    JAVA_CMD+=" -n $N"
  fi
  JAVA_CMD+=" -dir $DIR"
else
  if [ -n "$N" ]; then
    JAVA_CMD+=" -n $N"
  fi
  JAVA_CMD+=" -dir $DIR"
fi

echo "Running: $JAVA_CMD"
$JAVA_CMD

echo "Done."
echo "You can run '$0 clean' to remove generated shadow and output files." 