#!/usr/bin/perl
#/**
# *******************************************************************************
# * Copyright (C) 2006-2008, International Business Machines Corporation and    *
# * others. All Rights Reserved.                                                *
# *******************************************************************************
# */
use lib 'src/com/ibm/icu/dev/test/perf';
use lib 'src/com/ibm/icu/dev/test/perf/perldriver';
use PerfFramework4j;

$TEST_DATA="src/com/ibm/icu/dev/test/perf/data/collation";

#---------------------------------------------------------------------
# Test class
my $TESTCLASS = "com.ibm.icu.dev.test.perf.ResourceBundlePerf"; 

mkdir "results_ICU4J";

my $options = {
         "title"=>"ResourceBundle performance test",
         "headers"=>"Java ICU",
         "operationIs"=>"various",
         "timePerOperationIs"=>"Time per each fetch",
         "passes"=>"1",
         "time"=>"1",
         "outputType"=>"HTML",
         "dataDir"=>$TEST_DATA,
         "outputDir"=>"results_ICU4J"
        };

# programs

my $cmd = 'java -classpath "classes" '.$TESTCLASS;

my $dataFiles = "";

my $tests = { 
               "Empty array",           ["$cmd TestEmptyArrayJava",                 "$cmd TestEmptyArrayICU"],
               "Empty Explicit String", ["$cmd TestEmptyExplicitStringJava",        "$cmd TestEmptyExplicitStringICU"],
               "Empty String",          ["$cmd TestEmptyStringJava",                "$cmd TestEmptyStringICU"],
               "Get 123",               ["$cmd TestGet123Java",                     "$cmd TestGet123ICU"],
               "Get Binary Test",       ["$cmd TestGetBinaryTestJava",              "$cmd TestGetBinaryTestICU"],
               "Get Empty Binary",      ["$cmd TestGetEmptyBinaryJava",             "$cmd TestGetBinaryTestICU"],
               "Get Empty Menu",        ["$cmd TestGetEmptyMenuJava",               "$cmd TestGetEmptyMenuICU"],
               "Get Empty Int",         ["$cmd TestGetEmptyIntJava",                "$cmd TestGetEmptyIntICU"],
               "Get Empty Int Array",   ["$cmd TestGetEmptyIntegerArrayJava",       "$cmd TestGetEmptyIntegerArrayICU"],
               "Get Int Array",         ["$cmd TestGetIntegerArrayJava",            "$cmd TestGetIntegerArrayICU"],
               "Get Menu",              ["$cmd TestGetMenuJava",                    "$cmd TestGetMenuICU"],
               "Get Minus One",         ["$cmd TestGetMinusOneJava",                "$cmd TestGetMinusOneICU"],
               "Get Minus One Uint",    ["$cmd TestGetMinusOneUintJava",            "$cmd TestGetMinusOneUintICU"],
               "Get One",               ["$cmd TestGetOneJava",                     "$cmd TestGetOneICU"],
               "Get Plus One",          ["$cmd TestGetPlusOneJava",                 "$cmd TestGetPlusOneICU"],
               "Construction",          ["$cmd TestResourceBundleConstructionJava", "$cmd TestResourceBundleConstructionICU"],
               "Zero Test",             ["$cmd TestZeroTestJava",                   "$cmd TestZeroTestICU"]
            };


runTests($options, $tests, $dataFiles);


