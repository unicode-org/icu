#/*
#*******************************************************************************
#* Copyright (C) 1997-2000, International Business Machines Corporation and    *
#* others. All Rights Reserved.                                                *
#*******************************************************************************
#*
#* $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.bat,v $ 
#* $Date: 2000/03/10 04:17:55 $ 
#* $Revision: 1.2 $
#*
#*****************************************************************************************
#*/

@echo off
REM convience bat file to build with
java -classpath "build\javac.jar;build\ant.jar;build\projectx-tr2.jar;%CLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
