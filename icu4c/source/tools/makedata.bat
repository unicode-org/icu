@echo off
rem makedata.bat
rem batch file for Windows for creating the ICU data files
rem parameter:
rem %1 path where the icu folder resides

if "%1"=="" goto :error

if "%ICU_DATA%"=="" set ICU_DATA=%1\icu\data\

rem toolversion: Debug or Release
set toolversion=Release
if not "%2"=="" set toolversion=%2

echo create conversion tables
cd makeconv
call mkcnvfle %toolversion% %1

echo create locale resource bundles
cd ..\genrb
call genrb %toolversion% %1

echo create binary collation tables
cd ..\gencol
%toolversion%\gencol

cd ..

goto :end

:error
echo call makedata with the absolute path to the icu directory
echo for example, if the full path is d:\mytools\icu then call
echo makedata d:\mytools
echo the current directory must be the icu\source\tools directory with makedata.bat
echo a second, optional, parameter can be Debug or Release to specify the tools versions

:end
