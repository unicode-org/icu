@echo off

REM *******************************************************************************
REM * Copyright (C) 1997 - 2001, International Business Machines Corporation and    
REM * others. All Rights Reserved.                                                
REM *******************************************************************************

REM $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.bat,v $ 
REM $Date: 2001/11/29 18:18:10 $ 
REM $Revision: 1.5 $

REM *******************************************************************************

REM convience bat file to build with
java -classpath "%JAVA_HOME%\lib\tools.jar;%ANT_HOME%\lib\ant.jar;%CLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
