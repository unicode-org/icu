#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2007, International Business Machines
#  * Corporation and others. All Rights Reserved.
#  ********************************************************************

use strict;

#Assume we are running outside of the ICU4J source
use lib 'svn-icu4j/src/com/ibm/icu/dev/test/perf/perldriver';

use PerfFramework4j;

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = 'com.ibm.icu.dev.test.perf.NormalizerPerformanceTest'; 

my $options = {
	       "title"=>"Normalization performance regression (ICU4J 3.6 and 3.8)",
	       "headers"=>"ICU4J36 ICU4J38",
	       "operationIs"=>"code point",
	       "timePerOperationIs"=>"Time per code point",
	       "passes"=>"10",
	       "time"=>"5",
	       "outputType"=>"HTML",
	       "dataDir"=>"C:\\svn-icu4j\\src\\com\\ibm\\icu\\dev\\test\\perf\\data\\collation",
	       "outputDir"=>"svn-icu4j\\results_ICU4J"
	      };

# programs

my $jvm1 = "java -cp svn-icu4j_3-6/classes $TESTCLASS -b -e UTF-8";
my $jvm2 = "java -cp svn-icu4j/classes $TESTCLASS -b -e UTF-8";

my $dataFiles = {
		 "",
		 [
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
#		  "th18057.txt",
#		  "thesis.txt",
#		  "vfear11a.txt",
		 ]
		};


my $tests = { 
#	     "NFC_NFD_Text",  ["$jvm1 TestICU_NFC_NFD_Text"  ,  "$jvm2 TestICU_NFC_NFD_Text" ],
	     "NFC_NFC_Text",  ["$jvm1 TestICU_NFC_NFC_Text"  ,  "$jvm2 TestICU_NFC_NFC_Text" ],
	     "NFC_Orig_Text", ["$jvm1 TestICU_NFC_Orig_Text" ,  "$jvm2 TestICU_NFC_Orig_Text"],
	     "NFD_NFD_Text",  ["$jvm1 TestICU_NFD_NFD_Text"  ,  "$jvm2 TestICU_NFD_NFD_Text" ],
	     "NFD_NFC_Text",  ["$jvm1 TestICU_NFD_NFC_Text"  ,  "$jvm2 TestICU_NFD_NFC_Text" ],
	     "NFD_Orig_Text", ["$jvm1 TestICU_NFD_Orig_Text" ,  "$jvm2 TestICU_NFD_Orig_Text"],
	     ##
	     "QC_NFC_NFD_Text",  ["$jvm1 TestQC_NFC_NFD_Text"  ,  "$jvm2 TestQC_NFC_NFD_Text" ],
	     "QC_NFC_NFC_Text",  ["$jvm1 TestQC_NFC_NFC_Text"  ,  "$jvm2 TestQC_NFC_NFC_Text" ],
	     "QC_NFC_Orig_Text", ["$jvm1 TestQC_NFC_Orig_Text" ,  "$jvm2 TestQC_NFC_Orig_Text"],
	     "QC_NFD_NFD_Text",  ["$jvm1 TestQC_NFD_NFD_Text"  ,  "$jvm2 TestQC_NFD_NFD_Text" ],
	     "QC_NFD_NFC_Text",  ["$jvm1 TestQC_NFD_NFC_Text"  ,  "$jvm2 TestQC_NFD_NFC_Text" ],
	     "QC_NFD_Orig_Text", ["$jvm1 TestQC_NFD_Orig_Text" ,  "$jvm2 TestQC_NFD_Orig_Text"],
	     ##
	     "IsNormalized_NFC_NFD_Text",  ["$jvm1 TestIsNormalized_NFC_NFD_Text"  ,  "$jvm2 TestIsNormalized_NFC_NFD_Text" ],
	     "IsNormalized_NFC_NFC_Text",  ["$jvm1 TestIsNormalized_NFC_NFC_Text"  ,  "$jvm2 TestIsNormalized_NFC_NFC_Text" ],
	     "IsNormalized_NFC_Orig_Text", ["$jvm1 TestIsNormalized_NFC_Orig_Text" ,  "$jvm2 TestIsNormalized_NFC_Orig_Text"],
	     "IsNormalized_NFD_NFD_Text",  ["$jvm1 TestIsNormalized_NFD_NFD_Text"  ,  "$jvm2 TestIsNormalized_NFD_NFD_Text" ],
	     "IsNormalized_NFD_NFC_Text",  ["$jvm1 TestIsNormalized_NFD_NFC_Text"  ,  "$jvm2 TestIsNormalized_NFD_NFC_Text" ],
	     "IsNormalized_NFD_Orig_Text", ["$jvm1 TestIsNormalized_NFD_Orig_Text" ,  "$jvm2 TestIsNormalized_NFD_Orig_Text"]
	    };


runTests($options, $tests, $dataFiles);


