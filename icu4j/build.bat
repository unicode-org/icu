@echo off

REM *******************************************************************************
REM * Copyright (C) 1997-2000, International Business Machines Corporation and    *
REM * others. All Rights Reserved.                                                *
REM *******************************************************************************

REM $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.bat,v $ 
REM $Date: 2000/03/10 23:50:23 $ 
REM $Revision: 1.3 $

REM *******************************************************************************

REM convience bat file to build with
java -classpath "build\javac.jar;build\ant.jar;build\projectx-tr2.jar;%CLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
