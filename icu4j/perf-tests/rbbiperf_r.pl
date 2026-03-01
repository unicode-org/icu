#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * © 2016 and later: Unicode, Inc. and others.
#  * License & terms of use: http://www.unicode.org/copyright.html
#  * Copyright (c) 2002-2007, International Business Machines
#  * Corporation and others. All Rights Reserved.
#  ********************************************************************

use strict;

use lib 'svn-icu4j/src/com/ibm/icu/dev/test/perf/perldriver';

use PerfFramework4j;

my $options = {
	       "title"=>"Rule Based BreakIterator performance regression (ICU4J 3.6 and 3.8)",
	       "headers"=>"ICU4J36 ICU4J38",
	       "operationIs"=>"code point",
	       "eventIs"=>"break",
	       "passes"=>"10",
	       "time"=>"5",
	       "outputType"=>"HTML",
	       "dataDir"=>"svn-icu4j/src/com/ibm/icu/dev/test/perf/data/collation",
	       "outputDir"=>"svn-icu4j/results_ICU4J"
	      };

# programs
# Test class
my $TESTCLASS = "com.ibm.icu.dev.test.perf.RBBIPerf";
# tests will be done for all the programs. Results will be stored and connected
my $m1 = "char";
my $m2 = "word";
my $m3 = "line";
my $m4 = "jdkline";

my $m;

if(@_ >= 0) {
  $m = "-- -m ".shift;
} else {
  $m = $m1;
}

my $jvm1 = "java -classpath svn-icu4j_3-6/classes $TESTCLASS";
my $jvm2 = "java -classpath svn-icu4j/classes $TESTCLASS";

my $dataFiles = {
"en", [
	   #"thesis.txt", 
       #"2drvb10.txt", 
       #"ulyss10.txt",  
       #"nvsbl10.txt", 
       #"vfear11a.txt", 		  
       "TestNames_Asian.txt",
       "TestNames_Chinese.txt",
       "TestNames_Japanese.txt",
       "TestNames_Japanese_h.txt",
       "TestNames_Japanese_k.txt",
       "TestNames_Korean.txt",
       "TestNames_Latin.txt",
       "TestNames_SerbianSH.txt",
       "TestNames_SerbianSR.txt",
       "TestNames_Thai.txt",
       "Testnames_Russian.txt",
],
#"th", ["TestNames_Thai.txt", "th18057.txt"]
};


my $tests = { 
"TestNextChar",      ["$jvm1 testRBBINext $m1", "$jvm2 testRBBINext $m1"],
"TestNextWord",      ["$jvm1 testRBBINext $m2", "$jvm2 testRBBINext $m2"],
"TestNextLine",      ["$jvm1 testRBBINext $m3", "$jvm2 testRBBINext $m3"],
"TestNextJDKLine",   ["$jvm1 testRBBINext $m4", "$jvm2 testRBBINext $m4"],
##
"TestPreviousChar",      ["$jvm1 testRBBIPrevious $m1", "$jvm2 testRBBIPrevious $m1"],
"TestPreviousWord",      ["$jvm1 testRBBIPrevious $m2", "$jvm2 testRBBIPrevious $m2"],
"TestPreviousLine",      ["$jvm1 testRBBIPrevious $m3", "$jvm2 testRBBIPrevious $m3"],
#"TestPreviousSentence",  ["$jvm1 testRBBIPrevious $m4", "$jvm2 testRBBIPrevious $m4"],
##						                                                     
"TestIsBoundaryChar",      ["$jvm1 testRBBIIsBoundary $m1", "$jvm2 testRBBIIsBoundary $m1"],
"TestIsBoundaryWord",      ["$jvm1 testRBBIIsBoundary $m2", "$jvm2 testRBBIIsBoundary $m2"],
"TestIsBoundaryLine",      ["$jvm1 testRBBIIsBoundary $m3", "$jvm2 testRBBIIsBoundary $m3"],
#"TestIsBoundarySentence",  ["$jvm1 testRBBIIsBoundary $m4", "$jvm2 testRBBIIsBoundary $m4"],
};

runTests($options, $tests, $dataFiles);


