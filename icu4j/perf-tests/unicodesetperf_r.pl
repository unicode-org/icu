#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * © 2016 and later: Unicode, Inc. and others.
#  * License & terms of use: http://www.unicode.org/copyright.html
#  * Copyright (c) 2002-2007, International Business Machines
#  * Corporation and others. All Rights Reserved.
#  ********************************************************************

use strict;

#Assume we are running outside of the ICU4J source
use lib 'svn-icu4j/src/com/ibm/icu/dev/test/perf/perldriver';

use PerfFramework4j;

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = 'com.ibm.icu.dev.test.perf.UnicodeSetPerf'; 

my $options = {
	       "title"=>"Unicode Set performance regression (ICU4J 3.6 and 3.8)",
	       "headers"=>"ICU4J36 ICU4J38",
	       "operationIs"=>"code point",
	       "timePerOperationIs"=>"Time per code point",
	       "passes"=>"10",
	       "time"=>"5",
	       "outputType"=>"HTML",
	       "dataDir"=>"svn-icu4j/src/com/ibm/icu/dev/test/perf/data/collation",
	       "outputDir"=>"svn-icu4j/results_ICU4J"
	      };

# programs

my $jvm1 = "java -classpath svn-icu4j_3-6/classes $TESTCLASS";
my $jvm2 = "java -classpath svn-icu4j/classes $TESTCLASS";

my $dataFiles = "";
my $pat1 = '[:Lt:]';
my $pat2 = '[:Cn:]';

my $tests = { 
	     "UnicodeSetAdd ($pat1)",       ["$jvm1 UnicodeSetAdd $pat1"  ,       "$jvm2 UnicodeSetAdd $pat1" ],
	     "UnicodeSetAdd ($pat2)",       ["$jvm1 UnicodeSetAdd $pat2"  ,       "$jvm2 UnicodeSetAdd $pat2" ],
	     "UnicodeSetContains ($pat1)",  ["$jvm1 UnicodeSetContains $pat1"  ,  "$jvm2 UnicodeSetContains $pat1" ],
	     "UnicodeSetContains ($pat2)",  ["$jvm1 UnicodeSetContains $pat2"  ,  "$jvm2 UnicodeSetContains $pat2" ],
	     "UnicodeSetIterate ($pat1)",   ["$jvm1 UnicodeSetIterate $pat1"  ,   "$jvm2 UnicodeSetIterate $pat1" ],
	     "UnicodeSetIterate ($pat2)",   ["$jvm1 UnicodeSetIterate $pat2"  ,   "$jvm2 UnicodeSetIterate $pat2" ],
	     ##
	     "HashSetAdd ($pat1)",          ["$jvm1 HashSetAdd $pat1"  ,          "$jvm2 HashSetAdd $pat1" ],
	     "HashSetAdd ($pat2)",          ["$jvm1 HashSetAdd $pat2"  ,          "$jvm2 HashSetAdd $pat2" ],
	     "HashSetContains ($pat1)",     ["$jvm1 HashSetContains $pat1"  ,     "$jvm2 HashSetContains $pat1" ],
	     "HashSetContains ($pat2)",     ["$jvm1 HashSetContains $pat2"  ,     "$jvm2 HashSetContains $pat2" ],
	     "HashSetIterate ($pat1)",      ["$jvm1 HashSetIterate $pat1"  ,      "$jvm2 HashSetIterate $pat1" ],
	     "HashSetIterate ($pat2)",      ["$jvm1 HashSetIterate $pat2"  ,      "$jvm2 HashSetIterate $pat2" ]
	    };


runTests($options, $tests, $dataFiles);


