#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************

use strict;

#Assume we are running outside of the ICU4J source
use lib './icu4jsrc_3_4/src/com/ibm/icu/dev/test/perf/perldriver';

use PerfFramework4j;

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = 'com.ibm.icu.dev.test.perf.UnicodeSetPerf'; 

my $options = {
	       "title"=>"Unicode Set performance regression (ICU4J 3.2 and 3.4)",
	       "headers"=>"ICU4J32 ICU4J34",
	       "operationIs"=>"code point",
	       "timePerOperationIs"=>"Time per code point",
	       "passes"=>"10",
	       "time"=>"5",
	       #"outputType"=>"HTML",
	       "dataDir"=>"c:\\src\\perf\\data",
	       "outputDir"=>"results_ICU4J"
	      };

# programs

my $jvm1 = "java -cp ./icu4jsrc_3_2/icu4j32.jar;./icu4jsrc_3_2/perf32.jar $TESTCLASS";
my $jvm2 = "java -cp ./icu4jsrc_3_4/icu4j34.jar;./icu4jsrc_3_4/perf34.jar $TESTCLASS";

my $dataFiles = "";
my $pat1 = '[:Lt:]';
my $pat2 = '[:Cn:]';

my $tests = { 
	     "UnicodeSetAdd ($pat1)",  ["$jvm1 UnicodeSetAdd $pat1"  ,  "$jvm2 UnicodeSetAdd $pat1" ],
	     "UnicodeSetAdd ($pat2)",  ["$jvm1 UnicodeSetAdd $pat2"  ,  "$jvm2 UnicodeSetAdd $pat2" ],
	     "UnicodeSetContains ($pat1)",  ["$jvm1 UnicodeSetContains $pat1"  ,  "$jvm2 UnicodeSetContains $pat1" ],
	     "UnicodeSetContains ($pat2)",  ["$jvm1 UnicodeSetContains $pat2"  ,  "$jvm2 UnicodeSetContains $pat2" ],
	     "UnicodeSetIterate ($pat1)",  ["$jvm1 UnicodeSetIterate $pat1"  ,  "$jvm2 UnicodeSetIterate $pat1" ],
	     "UnicodeSetIterate ($pat2)",  ["$jvm1 UnicodeSetIterate $pat2"  ,  "$jvm2 UnicodeSetIterate $pat2" ],
	     ##
	     "HashSetAdd ($pat1)",  ["$jvm1 HashSetAdd $pat1"  ,  "$jvm2 HashSetAdd $pat1" ],
	     "HashSetAdd ($pat2)",  ["$jvm1 HashSetAdd $pat2"  ,  "$jvm2 HashSetAdd $pat2" ],
	     "HashSetContains ($pat1)",  ["$jvm1 HashSetContains $pat1"  ,  "$jvm2 HashSetContains $pat1" ],
	     "HashSetContains ($pat2)",  ["$jvm1 HashSetContains $pat2"  ,  "$jvm2 HashSetContains $pat2" ],
	     "HashSetIterate ($pat1)",  ["$jvm1 HashSetIterate $pat1"  ,  "$jvm2 HashSetIterate $pat1" ],
	     "HashSetIterate ($pat2)",  ["$jvm1 HashSetIterate $pat2"  ,  "$jvm2 HashSetIterate $pat2" ]
	    };


runTests($options, $tests, $dataFiles);


