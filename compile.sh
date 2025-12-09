#!/bin/bash

# Adaptez ici le chemin vers votre JDK
JAVAC=/Users/lehuen/bin/JDK-25.0.1+8/Contents/Home/bin/javac
CLASSPATH=class:lib/*

find src -name "*.java" | xargs $JAVAC -encoding UTF8 \
	-classpath $CLASSPATH \
	-sourcepath src \
	-d class
