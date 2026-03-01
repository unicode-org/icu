#!/usr/local/bin/perl
# * © 2016 and later: Unicode, Inc. and others.
# * License & terms of use: http://www.unicode.org/copyright.html
# *******************************************************************************
# * Copyright (C) 2002-2012 International Business Machines Corporation and     *
# * others. All Rights Reserved.                                                *
# *******************************************************************************

use XML::LibXML;

# Assume we are running within the icu4j root directory
use lib 'src/com/ibm/icu/dev/test/perf';
use Dataset;
my $OS=$^O;

my $CLASSPATH;
if ($OS eq "linux" || $OS eq "darwin") {
	$CLASSPATH="../icu4j.jar:../tools/misc/out/lib/icu4j-tools.jar:out/bin";
} else {
	$CLASSPATH="../icu4j.jar;../tools/misc/out/lib/icu4j-tools.jar;out/bin";
}
#---------------------------------------------------------------------

# Methods to be tested.  Each pair represents a test method and
# a baseline method which is used for comparison.
my @METHODS  = (
                 ['TestJDKConstruction',     'TestICUConstruction'],
                 ['TestJDKParse',            'TestICUParse'],
                 ['TestJDKFormat',           'TestICUFormat']
               );
# Patterns which define the set of characters used for testing.
my @OPTIONS = (
#                 locale    pattern              date string
                [ "en_US",  "dddd MMM yyyy",     "15 Jan 2007"],
                [ "sw_KE",  "dddd MMM yyyy",     "15 Jan 2007"],
                [ "en_US",  "HH:mm",             "13:13"],
                [ "en_US",  "HH:mm zzzz",        "13:13 Pacific Standard Time"],
                [ "en_US",  "HH:mm z",           "13:13 PST"],
                [ "en_US",  "HH:mm Z",           "13:13 -0800"],
              );

my $THREADS;        # number of threads (input from command-line args)
my $CALIBRATE = 2;  # duration in seconds for initial calibration
my $DURATION  = 10; # duration in seconds for each pass
my $NUMPASSES = 4;  # number of passes.  If > 1 then the first pass
                    # is discarded as a JIT warm-up pass.

my $TABLEATTR = 'BORDER="1" CELLPADDING="4" CELLSPACING="0"';

my $PLUS_MINUS = "&plusmn;";

if ($NUMPASSES < 3) {
    die "Need at least 3 passes.  One is discarded (JIT warmup) and need two to have 1 degree of freedom (t distribution).";
}


# run all tests with the specified number of threads from command-line input
# (if there is no arguments, use $THREADS = 1)
foreach my $arg ($#ARGV >= 0 ? @ARGV : "1") {
  $THREADS = $arg;
  main();
}


#---------------------------------------------------------------------
sub main {

#-----------DATE FORMAT PERFORMANCE TESTS-----------------------------
    my $testclass = 'com.ibm.icu.dev.test.perf.DateFormatPerformanceTest';
    #my $threads = ($THREADS > 1) ? "($THREADS threads)" : "";
    
    my $doc = XML::LibXML::Document->new('1.0', 'utf-8');
    my $root = $doc->createElement("perfTestResults");

 #   my $raw = "";
    my @shortNames = ( "open" , "parse", "fmt");
    my $index=0;

    for my $methodPair (@METHODS) {

        my $testMethod = $methodPair->[0];
        my $baselineMethod = $methodPair->[1];
	my $testname = $shortNames[$index];
	$index++;

        $OUT = '';
	my $patternCounter=1;

        for my $pat (@OPTIONS) { 

            # measure the test method
            my $t = measure2($testclass, $testMethod, $pat, -$DURATION);
	    my $testResult = $t->getMean();
	    my $jdkElement = $doc->createElement("perfTestResult");
	    my $testName = "DateFmt-$testname-pat$patternCounter-JDK";
	    $jdkElement->setAttribute("test" => $testName);
	    $jdkElement->setAttribute("iterations" => 1);
	    $jdkElement->setAttribute("time" => $testResult);
	    $root->appendChild($jdkElement);

            # measure baseline method
            my $b = measure2($testclass, $baselineMethod, $pat, -$DURATION);
            my $baseResult = $b->getMean();
	    my $icuElement = $doc->createElement("perfTestResult");
	    my $testName = "DateFmt-$testname-pat$patternCounter";
	    $patternCounter++;
	    $icuElement->setAttribute("test"=> $testName);
 	    $icuElement->setAttribute("iterations" => 1); 
	    $icuElement->setAttribute("time" => $baseResult);
	    $root->appendChild($icuElement);

       }
    }

#------------------DECIMAL FORMAT TESTS---------------------------------

    my $testclass = 'com.ibm.icu.dev.test.perf.DecimalFormatPerformanceTest';
    my @OPTIONS = (
#		locale	    pattern	date string
		[ "en_US", "#,###.##", "1,234.56"],
		[ "de_DE", "#,###.##", "1.234,56"],
		);
    my $index=0;
    for my $methodPair (@METHODS) {

        my $testMethod = $methodPair->[0];
        my $baselineMethod = $methodPair->[1];
	my $testname = $shortNames[$index];
	$index++;
	

        for my $pat (@OPTIONS) {
	       my $patternName = $pat->[0]; 

            # measure the test method
            my $t = measure2($testclass, $testMethod, $pat, -$DURATION);
	    my $testResult = $t->getMean();
	    my $jdkElement = $doc->createElement("perfTestResult");
	    my $testName = "NumFmt-$testname-$patternName-JDK";
	    $jdkElement->setAttribute("test" => $testName);
	    $jdkElement->setAttribute("iterations"=>1);
	    $jdkElement->setAttribute("time" => $testResult);
	    $root->appendChild($jdkElement);

            # measure baseline method
            my $b = measure2($testclass, $baselineMethod, $pat, -$DURATION);
            my $baseResult = $b->getMean();
	    my $icuElement = $doc->createElement("perfTestResult");
	    my $testName = "NumFmt-$testname-$patternName";
	    $icuElement->setAttribute("test"=> $testName);
	    $icuElement->setAttribute("iterations"=>1);
	    $icuElement->setAttribute("time" => $baseResult);
	    $root->appendChild($icuElement);
	}
    }

#----------------COLLATION PERFORMANCE TESTS--------------------------_

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
	
    #  Outer loop runs through the locales to test
    #     (Edit this list dirctly to make changes)
    #
    foreach  $locale (
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
        $counter = 1;
        foreach  $data (@ff) {
          #
          # Run ICU Test for this (locale, data file) pair.
          #
           $iStrCol = `java -classpath $CLASSPATH com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file data/collation/$data -locale $locale -loop 1000 -binsearch`;
print "java -classpath $CLASSPATH com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file data/collation/$data -locale $locale -loop 1000 -binsearch\n";
  $iStrCol =~s/[,\s]*//g;  # whack off the leading "  ," in the returned result.
          doKeyTimes("java -classpath $CLASSPATH com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file data/collation/$data -locale $locale -loop 1000 -keygen",
                    my $iKeyGen, my $iKeyLen);

          #
          # Run Windows test for this (locale, data file) pair.  Only do if
          #    we are not on Windows 98/ME and we hava a windows langID
          #    for the locale.
          #
           $wStrCol =  $wKeyGen =  $wKeyLen = 0;
          my $wStrCol = `java -classpath $CLASSPATH com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file data/collation/$data -locale $locale -loop 1000 -binsearch -java`;
          $wStrCol =~s/[,\s]*//g;  # whack off the leading "  ," in the returned result.
          doKeyTimes("java -classpath $CLASSPATH com.ibm.icu.dev.test.perf.CollationPerformanceTest -terse -file data/collation/$data -locale $locale -loop 1000 -keygen -java",
                     $wKeyGen, $wKeyLen);
                     
           $collDiff =  $keyGenDiff =  $keyLenDiff = 0;
          if ($wKeyLen > 0) {
               $collDiff   = (($wStrCol - $iStrCol) / $iStrCol) * 100;
               $keyGenDiff = (($wKeyGen - $iKeyGen) / $iKeyGen) * 100;
               $keyLenDiff = (($wKeyLen - $iKeyLen) / $iKeyLen) * 100;
          }

	my $ICU = $doc->createElement("perfTestResult");
	my $testname = "Coll-$locale-data$counter-StrCol";
	#write the results corresponding to this local,data pair
	$ICU->setAttribute("test"=> $testname);
	$ICU->setAttribute("iterations"=>1000);
	$ICU->setAttribute("time"=> $iStrCol);
	$root->appendChild($ICU);

	my $Key = $doc->createElement("perfTestResult");
	my $testname = "Coll-$locale-data$counter-keyGen";
	$Key->setAttribute("test"=> $testname);
	$Key->setAttribute("iterations"=>1000);
	$Key->setAttribute("time"=>$iKeyGen);
	$root->appendChild($Key);

	my $JDK = $doc->createElement("perfTestResult");
	my $testname = "Coll-$locale-data$counter-StrCol-JDK";
	$JDK->setAttribute("test"=>$testname);
	$JDK->setAttribute("iterations"=>1000);
	$JDK->setAttribute("time"=>$wStrCol);
	$root->appendChild($JDK);

	my $Key = $doc->createElement("perfTestResult");
	my $testname = "Coll-$locale-data$counter-keyGen-JDK";
	$Key->setAttribute("test"=>$testname);
	$Key->setAttribute("iterations"=>1000);
	$Key->setAttribute("time"=>$wKeyGen);
	$root->appendChild($Key);
	$counter++;
     }
   }



#----------WRITE RESULTS TO perf.xml-----------------------
    $doc->setDocumentElement($root);
    open my $out_fh, '>', "perf.xml";
    print {$out_fh} $doc->toString;
}


#---------------------------------------------------------------------
# Append text to the global variable $OUT
sub out {
   $OUT .= join('', @_);
}


#---------------------------------------------------------------------
# Measure a given test method with a give test pattern using the
# global run parameters.
#
# @param the method to run
# @param the pattern defining characters to test
# @param if >0 then the number of iterations per pass.  If <0 then
#        (negative of) the number of seconds per pass.
#
# @return a Dataset object, scaled by iterations per pass and
#         events per iteration, to give time per event
#
sub measure2 {
    my @data = measure1(@_);
    my $iterPerPass = shift(@data);
    my $eventPerIter = shift(@data);

    shift(@data) if (@data > 1); # discard first run

    my $ds = Dataset->new(@data);
    $ds->setScale(1.0e-3 / ($iterPerPass * $eventPerIter));
    $ds;
}

#---------------------------------------------------------------------
# Measure a given test method with a give test pattern using the
# global run parameters.
#
# @param the method to run
# @param the pattern defining characters to test
# @param if >0 then the number of iterations per pass.  If <0 then
#        (negative of) the number of seconds per pass.
#
# @return array of:
#         [0] iterations per pass
#         [1] events per iteration
#         [2..] ms reported for each pass, in order
#
sub measure1 {
    my $testclass = shift;
    my $method = shift;
    my $pat = shift;
    my $iterCount = shift; # actually might be -seconds/pass

    # is $iterCount actually -seconds/pass?
    if ($iterCount < 0) {

        # calibrate: estimate ms/iteration
        print "Calibrating...";
        my @t = callJava($testclass, $method, $pat, -$CALIBRATE, 1);
        print "done.\n";

        my @data = split(/\s+/, $t[0]->[2]);
        $data[0] *= 1.0e+3;

        my $timePerIter = 1.0e-3 * $data[0] / $data[1];
        
        # determine iterations/pass
        $iterCount = int(-$iterCount / $timePerIter + 0.5);
   }
    
    # run passes
    print "Measuring $iterCount iterations x $NUMPASSES passes...";
    my @t = callJava($testclass, $method, $pat, $iterCount, $NUMPASSES);
    print "done.\n";
    my @ms = ();
    my @b; # scratch
    for my $a (@t) {
        # $a->[0]: method name, corresponds to $method
        # $a->[1]: 'begin' data, == $iterCount
        # $a->[2]: 'end' data, of the form <ms> <loops> <eventsPerIter>
        # $a->[3...]: gc messages from JVM during pass
        @b = split(/\s+/, $a->[2]);
        push(@ms, $b[0] * 1.0e+3);
    }
    my $eventsPerIter = $b[2];

    my @ms_str = @ms;
    $ms_str[0] .= " (discarded)" if (@ms_str > 1);

    ($iterCount, $eventsPerIter, @ms);
}

#---------------------------------------------------------------------
# Invoke java to run $TESTCLASS, passing it the given parameters.
#
# @param the method to run
# @param the number of iterations, or if negative, the duration
#        in seconds.  If more than on pass is desired, pass in
#        a string, e.g., "100 100 100".
# @param the pattern defining characters to test
#
# @return an array of results.  Each result is an array REF
#         describing one pass.  The array REF contains:
#         ->[0]: The method name as reported
#         ->[1]: The params on the '= <meth> begin ...' line
#         ->[2]: The params on the '= <meth> end ...' line
#         ->[3..]: GC messages from the JVM, if any
#
sub callJava {
    my $testclass = shift;
    my $method = shift;
    my $pat = shift;
    my $n = shift;
    my $passes = shift;
    
    my $n = ($n < 0) ? "-t ".(-$n) : "-i ".$n;
    
    my $cmd = "java -classpath $CLASSPATH $testclass $method $n -p $passes -L @$pat[0] \"@$pat[1]\" \"@$pat[2]\" -r $THREADS";
    print "[$cmd]\n"; # for debugging
    open(PIPE, "$cmd|") or die "Can't run \"$cmd\"";
    my @out;
    while (<PIPE>) {
        push(@out, $_);
    }
    close(PIPE) or die "Java failed: \"$cmd\"";

    @out = grep(!/^\#/, @out);  # filter out comments

    #print "[", join("\n", @out), "]\n";

    my @results;
    my $method = '';
    my $data = [];
    foreach (@out) {
        next unless (/\S/);

        if (/^=\s*(\w+)\s*(\w+)\s*(.*)/) {
            my ($m, $state, $d) = ($1, $2, $3);
            #print "$_ => [[$m $state $data]]\n";
            if ($state eq 'begin') {
                die "$method was begun but not finished" if ($method);
                $method = $m;
                push(@$data, $d);
                push(@$data, ''); # placeholder for end data
            } elsif ($state eq 'end') {
                if ($m ne $method) {
                    die "$method end does not match: $_";
                }
                $data->[1] = $d; # insert end data at [1]
                #print "#$method:", join(";",@$data), "\n";
                unshift(@$data, $method); # add method to start

                push(@results, $data);
                $method = '';
                $data = [];
            } else {
                die "Can't parse: $_";
           }
        }
       elsif (/^\[/) {
            if ($method) {
                push(@$data, $_);
            } else {
                # ignore extraneous GC notices
            }
        }
        else {
            die "Can't parse: $_";
        }
    }

    die "$method was begun but not finished" if ($method);

    @results;
}

#-----------------------------------------------------------------------------------
#  doKeyGenTimes($Command_to_run, $time, $key_length)
#       Do a key-generation test and return the time and key length/char values.
#
sub doKeyTimes($$$) {
   # print "$_[0]\n";
   local($x) = `$_[0]`;                  # execute the collperf command.
   ($_[1], $_[2]) = split(/\,/, $x);     # collperf returns "time, keylength" string.
}


#eof
