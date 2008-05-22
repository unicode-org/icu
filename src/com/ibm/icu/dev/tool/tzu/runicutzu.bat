@echo off
rem *******************************************************************************
rem * Copyright (C) 2007, International Business Machines Corporation and         *
rem * others. All Rights Reserved.                                                *
rem *******************************************************************************

@echo.
@echo *********** Welcome to the ICU4J Time Zone Update Utility (ICUTZU) ***********

rem Set ICUTZU_HOME to the current directory.
set ICUTZU_HOME=%~dp0
@echo ICUTZU Home: %ICUTZU_HOME%
@echo.

rem Make sure certain files are present.
IF NOT EXIST "%ICUTZU_HOME%icutzu.jar" GOTO MissingICUTZUJAR
IF NOT EXIST "%ICUTZU_HOME%icu4j.jar" GOTO MissingICU4JJAR
IF NOT EXIST "%ICUTZU_HOME%runicutzuenv.bat" GOTO MissingICUTZUENV

rem Set environmental variables.
call "%ICUTZU_HOME%runicutzuenv.bat"
IF NOT EXIST "%JAVA_HOME%\bin\java.exe" GOTO MissingJAVAHOME

rem Create a temporary directory if one doesn't exit already.
IF EXIST "%ICUTZU_HOME%Temp" GOTO TempAlreadyExists
mkdir "%ICUTZU_HOME%Temp"
:TempAlreadyExists



rem Run the ICUTZU tool.
@echo.
@echo Launching the ICU4J Time Zone Update Utility (ICUTZU)...
@echo "%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% -Doffline=%OFFLINE% com.ibm.icu.dev.tool.tzu.ICUTZUMain "%ICUTZU_HOME%\" DirectorySearch.txt ICUList.txt zoneinfo.res Temp icu.gif
@echo.
"%JAVA_HOME%\bin\java.exe" -cp "%ICUTZU_HOME%icutzu.jar" -Dnogui=%NOGUI% -Ddiscoveronly=%DISCOVERONLY% -Dsilentpatch=%SILENTPATCH% -Doffline=%OFFLINE% com.ibm.icu.dev.tool.tzu.ICUTZUMain "%ICUTZU_HOME%\" DirectorySearch.txt ICUList.txt zoneinfo.res Temp icu.gif
IF ERRORLEVEL==0 GOTO Success
GOTO Failure



:MissingICUTZUJAR
@echo The ICU4J Time Zone Update Utility (icutzu.jar) doesn't exist in %ICUTZU_HOME%.
GOTO Failure

:MissingICU4JJAR
@echo ICU for Java (icu4j.jar) doesn't exist in %ICUTZU_HOME%.
GOTO Failure

:MissingICUTZUENV
@echo runicutzuenv.bat file doesn't exist in %ICUTZU_HOME%.
GOTO Failure

:MissingJAVAHOME
@echo java.exe does not exist in %JAVA_HOME%\bin. Please update the JAVA_HOME enviroment variable in runicutzuenv.bat
GOTO Failure

:Success
@echo.
@echo End of ICU4J Time Zone Update Utility (ICUTZU) completed successfully.
GOTO Exit

:Failure
@echo.
@echo ICU4J Time Zone Update Utility (ICUTZU) did not complete successfully.
GOTO Exit

:Exit
