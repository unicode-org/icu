#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2007, International Business Machines
#  * Corporation and others. All Rights Reserved.
#  ********************************************************************

use strict;

#Assume we are running outside of the ICU4J source
use lib './icu4jsrc_3_4/src/com/ibm/icu/dev/test/perf/perldriver';

use PerfFramework4j;

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = 'com.ibm.icu.dev.test.perf.UCharacterPerf'; 

my $options = {
	       "title"=>"UnicodeCharacter Property performance regression (ICU4J 3.2 and 3.4)",
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
my $pat = '0 ffff';

my $tests = { 
	     "Digit ($pat)",  ["$jvm1 Digit $pat"  ,  "$jvm2 Digit $pat" ],
	     "GetNumericValue ($pat)",  ["$jvm1 GetNumericValue $pat"  ,  "$jvm2 GetNumericValue $pat" ],
	     "GetType ($pat)",  ["$jvm1 GetType $pat"  ,  "$jvm2 GetType $pat" ],
	     "IsDefined ($pat)",  ["$jvm1 IsDefined $pat"  ,  "$jvm2 IsDefined $pat" ],
	     "IsDigit ($pat)",  ["$jvm1 IsDigit $pat"  ,  "$jvm2 IsDigit $pat" ],
	     "IsIdentifierIgnorable ($pat)",  ["$jvm1 IsIdentifierIgnorable $pat"  ,  "$jvm2 IsIdentifierIgnorable $pat" ],
	     "IsISOControl ($pat)",  ["$jvm1 IsISOControl $pat"  ,  "$jvm2 IsISOControl $pat" ],
	     "IsLetter ($pat)",  ["$jvm1 IsLetter $pat"  ,  "$jvm2 IsLetter $pat" ],
	     "IsLetterOrDigit ($pat)",  ["$jvm1 IsLetterOrDigit $pat"  ,  "$jvm2 IsLetterOrDigit $pat" ],
	     "IsLowerCase ($pat)",  ["$jvm1 IsLowerCase $pat"  ,  "$jvm2 IsLowerCase $pat" ],
	     "IsSpaceChar ($pat)",  ["$jvm1 IsSpaceChar $pat"  ,  "$jvm2 IsSpaceChar $pat" ],
	     "IsTitleCase ($pat)",  ["$jvm1 IsTitleCase $pat"  ,  "$jvm2 IsTitleCase $pat" ],
	     "IsUnicodeIdentifierPart ($pat)",  ["$jvm1 IsUnicodeIdentifierPart $pat"  ,  "$jvm2 IsUnicodeIdentifierPart $pat" ],
	     "IsUnicodeIdentifierStart ($pat)",  ["$jvm1 IsUnicodeIdentifierStart $pat"  ,  "$jvm2 IsUnicodeIdentifierStart $pat" ],
	     "IsUpperCase ($pat)",  ["$jvm1 IsUpperCase $pat"  ,  "$jvm2 IsUpperCase $pat" ],
	     "IsWhiteSpace ($pat)",  ["$jvm1 IsWhiteSpace $pat"  ,  "$jvm2 IsWhiteSpace $pat" ]
	    };


runTests($options, $tests, $dataFiles);


