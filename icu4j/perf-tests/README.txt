© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
Copyright (C) 2012, International Business Machines Corporation and others.  All Rights Reserved.

README for ICU4J Performance Test

This directory includes a number of performance tests. Most are
comparing ICU operations with built in Java functions. Many tests run
numerous iterations with a variety of locales.

Several tests create .html output files that can be opened and viewed in a web  browser.

The collation test produces output in the terminal window. Some are executed
individually via command line and others run via an `ant` command.

Note: Tests with "_r" in the name are obsolete tests that compared
versions of ICU4J with each other. These may be useful in the future,
but require reworking to locate, compile, and run different versions.


Note: To run the performance test for ICU4J as a part of continuous build, you will
need to set up Perl with the following modules:
    a) Statistics/Distributions.pm
    b) Statistics/Descriptive.pm
    c) XML/LibXML.pm

CONTINUOUS BUILD:
    To run a set of performance tests defined in file perlftests.pl, use these commands:
    cd <icu_root>/icu4j
    mvn install -DskipTests -DskipITs
    cd perf-tests
    ... run tests from here ...

Output is created in perf.xml. This output contains results comparing ICU and JDK for the
following operations
    DateFmt-open
    DateFmt-parse
    NumFmt-open
    NumFnt-parse
    Collation in several locales

COLLATION TESTS
    The collation tests run only on the command line with tabular output:
    perl collationperf.pl |& tee collation_output.txt

JMH
Some performance tests run using OpenJDK JMH. Example invocation:
    mvn clean package exec:java -pl perf-tests -Pjmh_benchmark

OTHER COMMAND LINE TESTS
Additional tests are run from the command line, each producing an HTML
output file with with the name "perf" followed by a timestamp of when
it was run. For example:

    "perf Jul 22 141434.html"

Each result can be loaded for review in a browser.

SETUP:
The environment variable PERL5LIB must be set as follows:
    export PERL5LIB=`pwd`

Then the command line is run for each as follows:
    perl dateformatperf.pl
    perl converterperf.pl
    perl decimalformat.pl
    perl normperf.pl
    perl ucharacterperf.pl
    perl unicodesetperf.pl


converterperf compares ICU Decoder and ICU Encoder with JDK versions for timing.

decimalformatperf compares JDK with ICU in contruction, parsing, and
formatting in en_US and de_DE locales.

normperf tests various normalization methods in both JKD and ICU usign
a variety of locales

ucharacterperf compares JDK with ICU for character handling with
digits, numeric values, types of characters, casing, and other
attributes

unicodesetperf compares UnicodeSet with HashSet with the following:
    UnicodeSetAdd
    HashSetAdd
    UnicodSetContains
    HashSetContains
    UnicodeSetIterate
    HashSetIterate
