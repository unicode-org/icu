@echo off
rem Copyright (c) 1999-2002, International Business Machines Corporation and
rem others. All Rights Reserved.
rem makedata.bat
rem batch file for Windows for creating the ICU data files
rem parameter:
rem %1 path where the icu folder resides

if "%1"=="" goto :error

if "%ICU_DATA%"=="" set ICU_DATA=%1\data\

rem toolversion: Debug or Release
set toolversion=Release
if not "%2"=="" set toolversion=%2

nmake /f makedata.mak icup=%1 cfg=%2 %3 %4
if not errorlevel 9009 goto :othererror
echo Build tools are not on path! Please make sure that MSVC++ is setup correctly!
goto :error
:othererror
if errorlevel 0 goto :end
echo Nmake has unsuccesfully finished with errorcode %errorlevel%!
goto :end

:error
echo call makedata with the absolute path to the icu directory
echo for example, if the full path is d:\mytools\icu then call
echo makedata d:\mytools\icu
echo a second, optional, parameter can be Debug or Release to specify the tools versions
echo.
echo the current directory must be the icu\source\tools directory with makedata.bat
echo also, the cl compiler and link linker must be on the PATH

:end
