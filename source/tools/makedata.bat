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

echo create unames.dat and unames_dat.c from UnicodeData.txt
gennames\%toolversion%\gennames -v- -c- "%ICU_DATA%UnicodeData-3.0.0.txt"
genccode\%toolversion%\genccode "%ICU_DATA%unames.dat"

echo create cnvalias.dat and cnvalias_dat.c from convrtrs.txt
gencnval\%toolversion%\gencnval -c-
genccode\%toolversion%\genccode "%ICU_DATA%cnvalias.dat"

echo create tz.dat from tz.txt
rem - This currently creates a spurious zero byte  -
rem - tz.dat file in the gentz dir, as well as the -
rem - actual tz.dat file in the data directory.    -
gentz\%toolversion%\gentz -c- gentz\tz.txt gentz\tz.dat
genccode\%toolversion%\genccode "%ICU_DATA%tz.dat"

echo create the data DLL
cl "/I..\..\include" /GD /c "%ICU_DATA%unames_dat.c" "%ICU_DATA%cnvalias_dat.c" "%ICU_DATA%tz_dat.c"
echo "/out:%ICU_DATA%icudata.dll">mkdll.tmp
echo unames_dat.obj>>mkdll.tmp
echo cnvalias_dat.obj>>mkdll.tmp
echo tz_dat.obj>>mkdll.tmp
type mkdll.lk>>mkdll.tmp
link @mkdll.tmp

echo create the common, memory-mappable file
del "%ICU_DATA%icudata.dat"
echo %ICU_DATA%unames.dat>mkmap.tmp
echo %ICU_DATA%cnvalias.dat>>mkmap.tmp
echo %ICU_DATA%tz.dat>>mkmap.tmp
gencmn\%toolversion%\gencmn 1000000 mkmap.tmp


goto :end

:error
echo call makedata with the absolute path to the icu directory
echo for example, if the full path is d:\mytools\icu then call
echo makedata d:\mytools
echo a second, optional, parameter can be Debug or Release to specify the tools versions
echo.
echo the current directory must be the icu\source\tools directory with makedata.bat
echo also, the cl compiler and link linker must be on the PATH

:end
