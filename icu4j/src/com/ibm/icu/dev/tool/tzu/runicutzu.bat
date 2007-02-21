@ECHO OFF
@echo *********** Welcome to the ICU4J Time Zone Update Utility (ICUTZU) ***********

rem /*
rem *******************************************************************************
rem * Copyright (C) 2007, International Business Machines Corporation and         *
rem * others. All Rights Reserved.                                                *
rem *******************************************************************************
rem */


rem set ICUTZU_HOME to the current directory
set ICUTZU_HOME=%~dp0
@echo ICUTZU Home: %ICUTZU_HOME%


IF NOT EXIST "%ICUTZU_HOME%icutzu.jar" GOTO MissingICUTZUJAR
IF NOT EXIST "%ICUTZU_HOME%icu4j.jar" GOTO MissingICU4JJAR
Goto ICUTZUENV

:MissingICUTZUJAR
@echo The ICU4J Time Zone Update Utility (icutzu.jar) doesn't exist in %ICUTZU_HOME%.
IF NOT EXIST "%ICUTZU_HOME%icu4j.jar" GOTO MissingICU4JJAR
GOTO Exit

:MissingICU4JJAR
@echo ICU for Java (icu4j.jar) doesn't exist in %ICUTZU_HOME%.
GOTO Exit

:ICUTZUENV
rem set JAVA_HOME to the Java installation directory
IF NOT EXIST "%ICUTZU_HOME%runicutzuenv.bat" GOTO MissingICUTZUENV
	call "%ICUTZU_HOME%runicutzuenv.bat"
	@echo Java Home: %JAVA_HOME%
	GOTO Temp

:MissingICUTZUENV
@echo runjtzuenv.bat file doesn't exist in %ICUTZU_HOME%.
GOTO Exit

:Temp
IF EXIST "%ICUTZU_HOME%\Temp" GOTO Next
 
rem Create a temporary directory
mkdir "%ICUTZU_HOME%\Temp"

:Next
rem Run the ICUTZU tool
@echo Launching the ICU4J Time Zone Update Utility (ICUTZU) ...

@echo "%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icu4j.jar";"%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% com.ibm.icu.dev.tool.tzu.ICUCLI --backup "%ICUTZU_HOME%\Temp"
"%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icu4j.jar";"%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% com.ibm.icu.dev.tool.tzu.ICUCLI --backup "%ICUTZU_HOME%\Temp"

:Exit
@echo End of ICU4J Time Zone Update Utility (ICUTZU).

