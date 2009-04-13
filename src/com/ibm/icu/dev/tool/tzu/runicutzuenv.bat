@echo off
rem *******************************************************************************
rem * Copyright (C) 2007-2008 International Business Machines Corporation and         *
rem * others. All Rights Reserved.                                                *
rem *******************************************************************************


rem Set JAVA_HOME to the Java installation directory.
IF  EXIST "%JAVA_HOME%" GOTO Next
set JAVA_HOME=/*ENTER JAVA PATH UPTO JRE*/
:Next
@echo JAVA_HOME=%JAVA_HOME%

rem Set whether the GUI will be run or not.
set NOGUI=true
@echo NOGUI=%NOGUI%

rem Set whether the cmd-line utility will only discover update icu4j files.
set DISCOVERONLY=true
@echo DISCOVERONLY=%DISCOVERONLY%

rem Set whether the cmd-line utility will run silently.
set SILENTPATCH=false
@echo SILENTPATCH=%SILENTPATCH%

rem Set whether the cmd-line utility will not use online sources.
set OFFLINE=false
@echo OFFLINE=%OFFLINE%
