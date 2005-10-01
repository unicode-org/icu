#/**
# *******************************************************************************
# * Copyright (C) 2002-2004, International Business Machines Corporation and    *
# * others. All Rights Reserved.                                                *
# *******************************************************************************
# */
#
#  ICU and Windows Collation performance test script
#      Used in conjunction with the collperf test program.
#      This script defines the locales and data files to be tested,
#        runs the collperf program, and formats and prints the results.
#
#        7 June 2001   Andy Heninger
#
#  ICU4J and Java Collator performance test script
#  2002-09-25 modified by Richard Liang

print "To run this performance test\n";
print "cd to the ICU4J root directory, one directory below src\n";
print "run perl src\\com\\ibm\\icu\\dev\\test\\perf\\collationperf.pl\n";

#
# Map defines the set of data files to run in each locale
#
%dataFiles = (
   "en_US",         "TestNames_Latin.txt",
   "da_DK",         "TestNames_Latin.txt",
   "de_DE",         "TestNames_Latin.txt",
   "de__PHONEBOOK", "TestNames_Latin.txt",
   "fr_FR",         "TestNames_Latin.txt",
   "ja_JP",         "TestNames_Latin.txt TestNames_Japanese_h.txt TestNames_Japanese_k.txt TestNames_Asian.txt",
   "zh_CN",         "TestNames_Latin.txt TestNames_Chinese.txt",
   "zh_TW",         "TestNames_Latin.txt TestNames_Chinese.txt",
   "zh__PINYIN",    "TestNames_Latin.txt TestNames_Chinese.txt",
   "ru_RU", 	    "TestNames_Latin.txt TestNames_Russian.txt",
   "th",            "TestNames_Latin.txt TestNames_Thai.txt",
   "ko_KR",         "TestNames_Latin.txt TestNames_Korean.txt",
   );


#
#  Outer loop runs through the locales to test
#     (Edit this list dirctly to make changes)
#
   foreach $locale (
	   "en_US",
	   "da_DK",
	   "de_DE",
	   "de__PHONEBOOK",
	   "fr_FR",
	   "ja_JP",
       "zh_CN",
	   "zh_TW",
	   "zh__PINYIN",
       "ko_KR",
	   "ru_RU",
	   "th",
                   )
       {
       #
       # Inner loop runs over the set of data files specified for each locale.
       #    (Edit the %datafiles initialization, above, to make changes.
       #
       $ff = $dataFiles{$locale};
       @ff = split(/[\s]+/, $ff);
       foreach $data (@ff) {

          #
          # Run ICU Test for this (locale, data file) pair.
          #
          $iStrCol = `java -classpath classes com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file src/com/ibm/icu/dev/test/perf/data/collation/$data -locale $locale -loop 1000 -binsearch`;
          $iStrCol =~s/[,\s]*//g;  # whack off the leading "  ," in the returned result.
          doKeyTimes("java -classpath classes com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file src/com/ibm/icu/dev/test/perf/data/collation/$data -locale $locale -loop 1000 -keygen",
                     $iKeyGen, $iKeyLen);


          #
          # Run Windows test for this (locale, data file) pair.  Only do if
          #    we are not on Windows 98/ME and we hava a windows langID
          #    for the locale.
          #
          $wStrCol = $wKeyGen = $wKeyLen = 0;
          $wStrCol = `java -classpath classes com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file src/com/ibm/icu/dev/test/perf/data/collation/$data -locale $locale -loop 1000 -binsearch -java`;
          $wStrCol =~s/[,\s]*//g;  # whack off the leading "  ," in the returned result.
          doKeyTimes("java -classpath classes com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file src/com/ibm/icu/dev/test/perf/data/collation/$data -locale $locale -loop 1000 -keygen -java",
                     $wKeyGen, $wKeyLen);
                     
          $collDiff = $keyGenDiff = $keyLenDiff = 0;
          if ($wKeyLen > 0) {
              $collDiff   = (($wStrCol - $iStrCol) / $iStrCol) * 100;
              $keyGenDiff = (($wKeyGen - $iKeyGen) / $iKeyGen) * 100;
              $keyLenDiff = (($wKeyLen - $iKeyLen) / $iKeyLen) * 100;
          }

         #
         #  Write the line of results for this (locale, data file).
         #
         write;
    }
 }

#
#  doKeyGenTimes($Command_to_run, $time, $key_length)
#       Do a key-generation test and return the time and key length/char values.
#
sub doKeyTimes($$$) {
   # print "$_[0]";
   local($x) = `$_[0]`;                  # execute the collperf command.
   ($_[1], $_[2]) = split(/\,/, $x);     # collperf returns "time, keylength" string.
}


#
#  Output Formats ...
#
#
format STDOUT_TOP =
                                      -------- ICU --------   ------ JAVA -------      (JAVA - ICU)/ICU
Locale     Data file                  strcoll keygen  keylen  strcoll keygen  keylen    coll  keygen  keylen
------------------------------------------------------------------------------------------------------------
.

format STDOUT =
@<<<<<<<<< @<<<<<<<<<<<<<<<<<<<<<<<<| @######  @####   @#.##  |@##### @#####   @#.## | @###%  @###%   @###%
$locale, $data, $iStrCol, $iKeyGen, $iKeyLen, $wStrCol, $wKeyGen, $wKeyLen, $collDiff, $keyGenDiff, $keyLenDiff
.
