echo off
rem Bertrand A. DAMIBA - IBM Corp.
rem This is a batch file that should be included in the MSVC build process
rem it's purpose is to build all the converter binary files (".cnv") from the 
rem existing *".ucm" files on the system
rem this  batch file should be passed a string either "Release" or "Debug"
rem so the script will know where "makeconv.exe" is
for %%i in (%2\icu\data\*.ucm %2\icu\data\ibm-16684.ucm %2\icu\data\ibm-16804.ucm %2\icu\data\ibm-17248.ucm %2\icu\data\ibm-21427.ucm %2\icu\data\ibm-12712.ucm) do %2\icu\source\tools\makeconv\%1\makeconv %%i
