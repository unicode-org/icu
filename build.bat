@echo off

REM *******************************************************************************
REM * Copyright (C) 1997-2000, International Business Machines Corporation and    *
REM * others. All Rights Reserved.                                                *
REM *******************************************************************************

REM $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.bat,v $ 
REM $Date: 2000/03/15 00:40:27 $ 
REM $Revision: 1.4 $

REM *******************************************************************************

REM convience bat file to build with
java -classpath "build\icu4jtools.jar;jakarta-ant\lib\ant.jar;jakarta-ant\lib\xml.jar;%CLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
