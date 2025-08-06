#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

function check_command() {
  echo -ne "Checking for ${1}... "
  if ! type "$1" > /dev/null; then
    echo "[failed]"
    echo "Missing command $1" >&2
    exit -1
  fi
  echo "[ok]"
}

function configure() {
  check_command "cmake"
  check_command "g++"

  mkdir -p $DIR/lib/Executor/build
  (cd $DIR/lib/Executor/build && cmake ..)
}

function build() {
  check_command "g++"
  check_command "cmake"

  (cd $DIR/lib/Executor/build &&  make -j 4)
}

echo "Configure Executor"
configure

echo "Build executor"
build
