#!/bin/bash

./clean.sh
./compile.sh
./build.sh

mkdir "distrib"

./distrib-linux.sh
./distrib-macos.sh
