#!/usr/local/bin/perl
# * © 2016 and later: Unicode, Inc. and others.
# * License & terms of use: http://www.unicode.org/copyright.html
# *******************************************************************************
# * Copyright (C) 2002-2007 International Business Machines Corporation and     *
# * others. All Rights Reserved.                                                *
# *******************************************************************************

use strict;

# Assume we are running within the icu4j/perf-tests root directory
use lib 'src/com/ibm/icu/dev/test/perf';
use Dataset;

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = 'com.ibm.icu.dev.test.perf.ConverterPerformanceTest';

my $CLASSES = './out/bin:../tools/misc/out/bin/:../icu4j.jar:../icu4j-charset.jar';

# Methods to be tested.  Each pair represents a test method and
# a baseline method which is used for comparison.
# Some tests do not compile at this time.
my @METHODS  = (
##               ['TestByteToCharConverter', 'TestByteToCharConverterICU'],
##               ['TestCharToByteConverter', 'TestCharToByteConverterICU'],
                 ['TestCharsetDecoder',      'TestCharsetDecoderICU'],
                 ['TestCharsetEncoder',      'TestCharsetEncoderICU']
               );

# Patterns which define the set of characters used for testing.

my $SOURCEDIR ="./data/conversion/";

# Note that some tests are unavailable
my @OPTIONS = (
#   src text          src encoding    test encoding
  [ "arabic.txt",     "UTF-8",        "csisolatinarabic"],
  [ "french.txt",     "UTF-8",        "csisolatin1"],
  [ "greek.txt",      "UTF-8",        "csisolatingreek"],
  [ "hebrew.txt",     "UTF-8",        "csisolatinhebrew"],
#  [ "hindi.txt" ,     "UTF-8",        "iscii"],
  [ "japanese.txt",   "UTF-8",        "EUC-JP"],
  [ "japanese.txt",   "UTF-8",        "csiso2022jp"],
# [ "japanese.txt",   "UTF-8",        "shift_jis"],
  [ "korean.txt",     "UTF-8",        "csiso2022kr"],
#  [ "korean.txt",     "UTF-8",        "EUC-KR"],
  [ "s-chinese.txt",  "UTF-8",        "EUC_CN"],
  [ "arabic.txt",     "UTF-8",        "UTF-8"],
  [ "french.txt",     "UTF-8",        "UTF-8"],
  [ "greek.txt",      "UTF-8",        "UTF-8"],
  [ "hebrew.txt",     "UTF-8",        "UTF-8"],
  [ "hindi.txt" ,     "UTF-8",        "UTF-8"],
  [ "japanese.txt",   "UTF-8",        "UTF-8"],
  [ "korean.txt",     "UTF-8",        "UTF-8"],
  [ "s-chinese.txt",  "UTF-8",        "UTF-8"],
  [ "french.txt",     "UTF-8",        "UTF-16BE"],
  [ "french.txt",     "UTF-8",        "UTF-16LE"],
  [ "english.txt",    "UTF-8",        "US-ASCII"],
  );

my $CALIBRATE = 2;  # duration in seconds for initial calibration
my $DURATION  = 10; # duration in seconds for each pass
my $NUMPASSES = 4;  # number of passes.  If > 1 then the first pass
                    # is discarded as a JIT warm-up pass.

my $TABLEATTR = 'BORDER="1" CELLPADDING="4" CELLSPACING="0"';

my $PLUS_MINUS = "&plusmn;";

if ($NUMPASSES < 3) {
    die "Need at least 3 passes.  One is discarded (JIT warmup) and need two to have 1 degree of freedom (t distribution).";
}

my $OUT; # see out()

main();

#---------------------------------------------------------------------
# ...
sub main {
    my $date = localtime;
    my $title = "ICU4J Performance Test $date";

    my $html = $date;
    $html =~ s/://g; # ':' illegal
    $html =~ s/\s*\d+$//; # delete year
    $html =~ s/^\w+\s*//; # delete dow
    $html = "perf $html.html";

    open(HTML,">$html") or die "Can't write to $html: $!";

    print HTML <<EOF;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
   "http://www.w3.org/TR/html4/strict.dtd">
<HTML>
   <HEAD>
      <TITLE>$title</TITLE>
   </HEAD>
   <BODY>
EOF
    print HTML "<H1>$title</H1>\n";

    print HTML "<H2>$TESTCLASS</H2>\n";

    my $raw = "";

    for my $methodPair (@METHODS) {

        my $testMethod = $methodPair->[0];
        my $baselineMethod = $methodPair->[1];

        print HTML "<P><TABLE $TABLEATTR><TR><TD>\n";
        print HTML "<P><B>$testMethod vs. $baselineMethod</B></P>\n";

        print HTML "<P><TABLE $TABLEATTR BGCOLOR=\"#CCFFFF\">\n";
        print HTML "<TR><TD>Options</TD><TD>$testMethod</TD>";
        print HTML "<TD>$baselineMethod</TD><TD>Ratio</TD></TR>\n";

        $OUT = '';

        for my $pat (@OPTIONS) {
            print HTML "<TR><TD>@$pat[0], @$pat[2]</TD>\n";

            out("<P><TABLE $TABLEATTR WIDTH=\"100%\">");

            # measure the test method
            out("<TR><TD>");
            print "\n$testMethod [@$pat]\n";
            my $t = measure2($testMethod, $pat, -$DURATION);
            out("</TD></TR>");
            print HTML "<TD>", formatSeconds(4, $t->getMean(), $t->getError);
            print HTML "/event</TD>\n";

            # measure baseline method
            out("<TR><TD>");
            print "\n$baselineMethod [@$pat]\n";
            my $b = measure2($baselineMethod, $pat, -$DURATION);
            out("</TD></TR>");
            print HTML "<TD>", formatSeconds(4, $b->getMean(), $t->getError);
            print HTML "/event</TD>\n";

            out("</TABLE></P>");

            # output ratio
            my $r = $t->divide($b);
            my $mean = $r->getMean() - 1;
            my $color = $mean < 0 ? "RED" : "BLACK";
            print HTML "<TD><B><FONT COLOR=\"$color\">", formatPercent(3, $mean, $r->getError);
            print HTML "</FONT></B></TD></TR>\n";
        }

        print HTML "</TABLE></P>\n";

        print HTML "<P>Raw data:</P>\n";
        print HTML $OUT;
        print HTML "</TABLE></P>\n";
    }

    print HTML <<EOF;
   </BODY>
</HTML>
EOF
    close(HTML) or die "Can't close $html: $!";
}

#---------------------------------------------------------------------
# Append text to the global variable $OUT
sub out {
    $OUT .= join('', @_);
}

#---------------------------------------------------------------------
# Append text to the global variable $OUT
sub outln {
    $OUT .= join('', @_) . "\n";
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
    my $method = shift;
    my $pat = shift;
    my $param3 = shift;  # Either -seconds/pass or iteration count

    my $iterCount = 0;  # Set later based on param3.

    out("<P>Measuring $method for input file @$pat[0] for encoding @$pat[2] , ");
    if ($param3 > 0) {
        $iterCount = $param3;
        out("$iterCount iterations/pass, $NUMPASSES passes</P>\n");
    } else {
        my $timePerPass = -$param3;
        out(-$timePerPass, " seconds/pass, $NUMPASSES passes</P>\n");

        # Value given was -seconds/pass

        # calibrate: estimate ms/iteration
        print "Calibrating...";
        my @t = callJava($method, $pat, -$CALIBRATE, 1);
        print "done.\n";

        my @data = split(/\s+/, $t[0]->[2]);
        $data[0] *= 1.0e+3;

        my $timePerIter = 1.0e-3 * $data[0] / $data[1];

        # determine iterations/pass from timePerPass and timePerIteration
        $iterCount = int($timePerPass / $timePerIter + 0.5);

        out("<P>Calibration pass ($CALIBRATE sec): ");
        out("$data[0] ms, ");
        out("$data[1] iterations = ");
        out(formatSeconds(4, $timePerIter), "/iteration<BR>\n");
    }

    # run passes
    print "Measuring $iterCount iterations x $NUMPASSES passes...";
    my @t = callJava($method, $pat, $iterCount, $NUMPASSES);
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

    out("Iterations per pass: $iterCount<BR>\n");
    out("Events per iteration: $eventsPerIter<BR>\n");

    my @ms_str = @ms;
    $ms_str[0] .= " (discarded)" if (@ms_str > 1);
    out("Raw times (ms/pass): ", join(", ", @ms_str), "<BR>\n");

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
    my $method = shift;
    my $pat = shift;
    my $n = shift;
    my $passes = shift;

    my $fileName = $SOURCEDIR.@$pat[0] ;
    my $n = ($n < 0) ? "-t ".(-$n) : "-i ".$n;

    my $cmd = "java -classpath $CLASSES $TESTCLASS $method $n -p $passes -f $fileName -e @$pat[1] -T @$pat[2]";
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

#|#---------------------------------------------------------------------
#|# Format a confidence interval, as given by a Dataset.  Output is as
#|# as follows:
#|#   241.23 - 241.98 => 241.5 +/- 0.3
#|#   241.2 - 243.8 => 242 +/- 1
#|#   211.0 - 241.0 => 226 +/- 15 or? 230 +/- 20
#|#   220.3 - 234.3 => 227 +/- 7
#|#   220.3 - 300.3 => 260 +/- 40
#|#   220.3 - 1000 => 610 +/- 390 or? 600 +/- 400
#|#   0.022 - 0.024 => 0.023 +/- 0.001
#|#   0.022 - 0.032 => 0.027 +/- 0.005
#|#   0.022 - 1.000 => 0.5 +/- 0.5
#|# In other words, take one significant digit of the error value and
#|# display the mean to the same precision.
#|sub formatDataset {
#|    my $ds = shift;
#|    my $lower = $ds->getMean() - $ds->getError();
#|    my $upper = $ds->getMean() + $ds->getError();
#|    my $scale = 0;
#|    # Find how many initial digits are the same
#|    while ($lower < 1 ||
#|           int($lower) == int($upper)) {
#|        $lower *= 10;
#|        $upper *= 10;
#|        $scale++;
#|    }
#|    while ($lower >= 10 &&
#|           int($lower) == int($upper)) {
#|        $lower /= 10;
#|        $upper /= 10;
#|        $scale--;
#|    }
#|}

#---------------------------------------------------------------------
# Format a number, optionally with a +/- delta, to n significant
# digits.
#
# @param significant digit, a value >= 1
# @param multiplier
# @param time in seconds to be formatted
# @optional delta in seconds
#
# @return string of the form "23" or "23 +/- 10".
#
sub formatNumber {
    my $sigdig = shift;
    my $mult = shift;
    my $a = shift;
    my $delta = shift; # may be undef

    my $result = formatSigDig($sigdig, $a*$mult);
    if (defined($delta)) {
        my $d = formatSigDig($sigdig, $delta*$mult);
        # restrict PRECISION of delta to that of main number
        if ($result =~ /\.(\d+)/) {
            # TODO make this work for values with all significant
            # digits to the left of the decimal, e.g., 1234000.

            # TODO the other thing wrong with this is that it
            # isn't rounding the $delta properly.  Have to put
            # this logic into formatSigDig().
            my $x = length($1);
            $d =~ s/\.(\d{$x})\d+/.$1/;
        }
        $result .= " $PLUS_MINUS " . $d;
    }
    $result;
}

#---------------------------------------------------------------------
# Format a time, optionally with a +/- delta, to n significant
# digits.
#
# @param significant digit, a value >= 1
# @param time in seconds to be formatted
# @optional delta in seconds
#
# @return string of the form "23 ms" or "23 +/- 10 ms".
#
sub formatSeconds {
    my $sigdig = shift;
    my $a = shift;
    my $delta = shift; # may be undef

    my @MULT = (1   , 1e3,  1e6,  1e9,  1e12);
    my @SUFF = ('s' , 'ms', 'us', 'ns', 'ps');

    # Determine our scale
    my $i = 0;
    ++$i while ($a*$MULT[$i] < 1 && $i < @MULT);

    formatNumber($sigdig, $MULT[$i], $a, $delta) . ' ' . $SUFF[$i];
}

#---------------------------------------------------------------------
# Format a percentage, optionally with a +/- delta, to n significant
# digits.
#
# @param significant digit, a value >= 1
# @param value to be formatted, as a fraction, e.g. 0.5 for 50%
# @optional delta, as a fraction
#
# @return string of the form "23 %" or "23 +/- 10 %".
#
sub formatPercent {
    my $sigdig = shift;
    my $a = shift;
    my $delta = shift; # may be undef

    formatNumber($sigdig, 100, $a, $delta) . ' %';
}

#---------------------------------------------------------------------
# Format a number to n significant digits without using exponential
# notation.
#
# @param significant digit, a value >= 1
# @param number to be formatted
#
# @return string of the form "1234" "12.34" or "0.001234".  If
#         number was negative, prefixed by '-'.
#
sub formatSigDig {
    my $n = shift() - 1;
    my $a = shift;

    local $_ = sprintf("%.${n}e", $a);
    my $sign = (s/^-//) ? '-' : '';

    my $a_e;
    my $result;
    if (/^(\d)\.(\d+)e([-+]\d+)$/) {
        my ($d, $dn, $e) = ($1, $2, $3);
        $a_e = $e;
        $d .= $dn;
        $e++;
        $d .= '0' while ($e > length($d));
        while ($e < 1) {
            $e++;
            $d = '0' . $d;
        }
        if ($e == length($d)) {
            $result = $sign . $d;
        } else {
            $result = $sign . substr($d, 0, $e) . '.' . substr($d, $e);
        }
    } else {
        die "Can't parse $_";
    }
    $result;
}

#eof
