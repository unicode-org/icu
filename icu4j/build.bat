@echo off
REM convience bat file to build with
java -classpath "build\javac.jar;build\ant.jar;build\projectx-tr2.jar;%CLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
