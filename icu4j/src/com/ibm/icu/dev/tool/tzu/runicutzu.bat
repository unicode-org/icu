@echo off
rem /*
rem *******************************************************************************
rem * Copyright (C) 2007, International Business Machines Corporation and         *
rem * others. All Rights Reserved.                                                *
rem *******************************************************************************
rem */

@echo *********** Welcome to the ICU4J Time Zone Update Utility (ICUTZU) ***********

rem Set ICUTZU_HOME to the current directory.
set ICUTZU_HOME=%~dp0
@echo ICUTZU Home: %ICUTZU_HOME%

rem Make sure certain files are present.
IF NOT EXIST "%ICUTZU_HOME%icutzu.jar" GOTO MissingICUTZUJAR
IF NOT EXIST "%ICUTZU_HOME%icu4j.jar" GOTO MissingICU4JJAR
IF NOT EXIST "%ICUTZU_HOME%runicutzuenv.bat" GOTO MissingICUTZUENV

rem Set environmental variables.
call "%ICUTZU_HOME%runicutzuenv.bat"
rem Double-check that JAVA_HOME is set.
@echo Java Home: %JAVA_HOME%
IF NOT EXIST "%JAVA_HOME%/bin/java" GOTO MissingJAVAHOME

IF EXIST "%ICUTZU_HOME%\Temp" GOTO Next
rem Create a temporary directory.
mkdir "%ICUTZU_HOME%\Temp"
:Next

rem Run the ICUTZU tool.
@echo Launching the ICU4J Time Zone Update Utility (ICUTZU) ...
@echo "%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icu4j.jar";"%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% com.ibm.icu.dev.tool.tzu.ICUTZUMain --recurse --backup "%ICUTZU_HOME%Temp"
"%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icu4j.jar";"%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% com.ibm.icu.dev.tool.tzu.ICUTZUMain --recurse --backup "%ICUTZU_HOME%Temp"

GOTO Exit



:MissingICUTZUJAR
@echo The ICU4J Time Zone Update Utility (icutzu.jar) doesn't exist in %ICUTZU_HOME%.
IF NOT EXIST "%ICUTZU_HOME%icu4j.jar" GOTO MissingICU4JJAR
GOTO Exit

:MissingICU4JJAR
@echo ICU for Java (icu4j.jar) doesn't exist in %ICUTZU_HOME%.
GOTO Exit

:MissingICUTZUENV
@echo runicutzuenv.bat file doesn't exist in %ICUTZU_HOME%.
GOTO Exit

:MissingJAVAHOME
@echo java.exe does not exist in %JAVA_HOME%\bin. Please update the JAVA_HOME enviroment variable in runicutzuenv.bat
GOTO Exit


:Exit
@echo End of ICU4J Time Zone Update Utility (ICUTZU).
