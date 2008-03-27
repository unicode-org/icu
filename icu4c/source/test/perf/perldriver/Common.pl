#!perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2008, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************

$ICULatestVersion = "4.0";     # Change to respective version number
$ICUPreviousVersion = "3.8";   # Change to respective version number
$ICUPrevious2Version = "3.6";  # Change to respective version number

$PerformanceDataPath = "";          #Change to Performance Data Path

$ICULatest = "";      # Change to path of latest ICU (e.g. /home/user/Desktop/icu-4.0)
$ICUPrevious = "";    # Change to path of previous ICU
$ICUPrevious2 = "";   # Change to path of ICU before previous release

$OnWindows = 0;           # Change to 1 if on Windows


$CollationDataPath = $PerformanceDataPath."/collation";    # Collation Performance Data Path
$ConversionDataPath = $PerformanceDataPath."/conversion";  # Conversion Performance Data Path
$UDHRDataPath = $PerformanceDataPath."/udhr";              # UDHR Performance Data Path

$ICUPathLatest = $ICULatest."/antbuild/test/perf";
$ICUPathPrevious = $ICUPrevious."/source/test/perf";
$ICUPathPrevious2 = $ICUPrevious."/source/test/perf";


return 1;