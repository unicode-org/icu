@echo off

echo This file is obsolete.  Please use source\tools\makedata.bat instead.
goto :end

rem Bertrand A. DAMIBA - IBM Corp.
rem This is a batch file that should be included in the MSVC build process
rem it's purpose is to build all the converter binary files (".cnv") from the 
rem existing *".ucm" files on the system
rem this  batch file should be passed a string either "Release" or "Debug"
rem so the script will know where "makeconv.exe" is
rem Added argument checking, Vladimir Weinsten - IBM Corp., 10/25/99

if "%1"=="" goto :error

if "%2"=="" goto :error

if "%ICU_DATA%"=="" set ICU_DATA=%2\icu\data\

REM for %%i in (%2\icu\data\*.ucm) do %2\icu\source\tools\makeconv\%1\makeconv %%i
for %%i in (%2\icu\data\*.ucm %2\icu\data\ibm-16684.ucm %2\icu\data\ibm-16804.ucm %2\icu\data\ibm-17248.ucm %2\icu\data\ibm-21427.ucm %2\icu\data\ibm-12712.ucm) do %2\icu\source\tools\makeconv\%1\makeconv %%i


goto :end

:error

echo call mkcnvfle with "Debug" or "Release" as the first argument
echo and the absolute path to the icu directory as the second.
echo for example, if you built the Debug version on icu, 
echo and the full path of icu is d:\mytools\icu then call
echo mkcnvfle Debug d:\mytools
echo the current directory must be the icu\source\tools\makeconv directory with genrb.bat

:end
