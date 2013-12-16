#!/usr/bin/perl
# ********************************************************************
#  COPYRIGHT:
#  Copyright (c) 2013, International Business Machines Corporation and
#  others. All Rights Reserved.
# ********************************************************************

#use strict;

use lib '../perldriver';

require "../perldriver/Common.pl";

use PerfFramework;

my $options = {
    "title"=>"Collation Performance Regression: ICU (".$ICUPreviousVersion." and ".$ICULatestVersion.")",
    "headers"=>"ICU".$ICUPreviousVersion." ICU".$ICULatestVersion,
    "operationIs"=>"Collator",
    "passes"=>"1",
    "time"=>"5",
    #"outputType"=>"HTML",
    "dataDir"=>$CollationDataPath,
    "outputDir"=>"../results"
};

# programs
# tests will be done for all the programs. Results will be stored and connected
my $p1, $p2;

if ($OnWindows) {
    $p1 = "cd ".$ICUPrevious."/bin && ".$ICUPathPrevious."/collperf2/$WindowsPlatform/Release/collperf2.exe";
    $p2 = "cd ".$ICULatest."/bin && ".$ICUPathLatest."/collperf2/$WindowsPlatform/Release/collperf2.exe";
} else {
    $p1 = "LD_LIBRARY_PATH=".$ICUPrevious."/source/lib:".$ICUPrevious."/source/tools/ctestfw ".$ICUPathPrevious."/collperf2/collperf2";
    $p2 = "LD_LIBRARY_PATH=".$ICULatest."/source/lib:".$ICULatest."/source/tools/ctestfw ".$ICUPathLatest."/collperf2/collperf2";
}

my $tests = {
    "ucol_strcoll/len",             ["$p1,TestStrcoll", "$p2,TestStrcoll"],
    "ucol_strcoll/null",            ["$p1,TestStrcollNull", "$p2,TestStrcollNull"],
    "ucol_strcoll/len/similar",     ["$p1,TestStrcollSimilar", "$p2,TestStrcollSimilar"],

    "ucol_strcollUTF8/len",         ["$p1,TestStrcollUTF8", "$p2,TestStrcollUTF8"],
    "ucol_strcollUTF8/null",        ["$p1,TestStrcollUTF8Null", "$p2,TestStrcollUTF8Null"],
    "ucol_strcollUTF8/len/similar", ["$p1,TestStrcollUTF8Similar", "$p2,TestStrcollUTF8Similar"],

    "ucol_getSortKey/len",          ["$p1,TestGetSortKey", "$p2,TestGetSortKey"],
    "ucol_getSortKey/null",         ["$p1,TestGetSortKeyNull", "$p2,TestGetSortKeyNull"],

    "ucol_nextSortKeyPart/4_all",   ["$p1,TestNextSortKeyPart_4All", "$p2,TestNextSortKeyPart_4All"],
    "ucol_nextSortKeyPart/4x4",     ["$p1,TestNextSortKeyPart_4x4", "$p2,TestNextSortKeyPart_4x4"],
    "ucol_nextSortKeyPart/4x8",     ["$p1,TestNextSortKeyPart_4x8", "$p2,TestNextSortKeyPart_4x8"],
    "ucol_nextSortKeyPart/32_all",  ["$p1,TestNextSortKeyPart_32All", "$p2,TestNextSortKeyPart_32All"],
    "ucol_nextSortKeyPart/32x2",    ["$p1,TestNextSortKeyPart_32x2", "$p2,TestNextSortKeyPart_32x2"],

    "ucol_nextSortKeyPart/UTF8/4_all",  ["$p1,TestNextSortKeyPartUTF8_4All", "$p2,TestNextSortKeyPartUTF8_4All"],
    "ucol_nextSortKeyPart/UTF8/4x4",    ["$p1,TestNextSortKeyPartUTF8_4x4", "$p2,TestNextSortKeyPartUTF8_4x4"],
    "ucol_nextSortKeyPart/UTF8/4x8",    ["$p1,TestNextSortKeyPartUTF8_4x8", "$p2,TestNextSortKeyPartUTF8_4x8"],
    "ucol_nextSortKeyPart/UTF8/32_all", ["$p1,TestNextSortKeyPartUTF8_32All", "$p2,TestNextSortKeyPartUTF8_32All"],
    "ucol_nextSortKeyPart/UTF8/32x2",   ["$p1,TestNextSortKeyPartUTF8_32x2", "$p2,TestNextSortKeyPartUTF8_32x2"],

    "Collator::compare/len",                ["$p1,TestCppCompare", "$p2,TestCppCompare"],
    "Collator::compare/null",               ["$p1,TestCppCompareNull", "$p2,TestCppCompareNull"],
    "Collator::compare/len/similar",        ["$p1,TestCppCompareSimilar", "$p2,TestCppCompareSimilar"],

    "Collator::compareUTF8/len",            ["$p1,TestCppCompareUTF8", "$p2,TestCppCompareUTF8"],
    "Collator::compareUTF8/null",           ["$p1,TestCppCompareUTF8Null", "$p2,TestCppCompareUTF8Null"],
    "Collator::compareUTF8/len/similar",    ["$p1,TestCppCompareUTF8Similar", "$p2,TestCppCompareUTF8Similar"],

    "Collator::getCollationKey/len",        ["$p1,TestCppGetCollationKey", "$p2,TestCppGetCollationKey"],
    "Collator::getCollationKey/null",       ["$p1,TestCppGetCollationKeyNull", "$p2,TestCppGetCollationKeyNull"],
};

my $dataFiles = {
    "en_US",
    [
        "TestNames_Latin.txt"
    ],

    "de\@collation=phonebook;strength=secondary",
    [
        "TestRandomWordsUDHR_de.txt"
    ],

    "fr\@strength=primary;caseLevel=on",
    [
        "TestRandomWordsUDHR_fr.txt"
    ],

    "es",
    [
        "TestRandomWordsUDHR_es.txt"
    ],

    "pl",
    [
        "TestRandomWordsUDHR_pl.txt"
    ],

    "tr",
    [
        "TestRandomWordsUDHR_tr.txt"
    ],

    "el",
    [
        "TestRandomWordsUDHR_el.txt"
    ],

    "ru",
    [
        "TestNames_Russian.txt"
    ],

    "ru\@strength=quaternary;alternate=shifted",
    [
        "TestRandomWordsUDHR_ru.txt"
    ],

    "ja",
    [
        "TestNames_Japanese.txt",
        "TestNames_Japanese_h.txt",
        "TestNames_Japanese_k.txt"
    ],

    "ja\@strength=identical",
    [
        "TestNames_Latin.txt",
        "TestNames_Japanese.txt"
    ],

    "ko",
    [
        "TestNames_Korean.txt"
    ],

    "zh_Hans",
    [
        "TestNames_Simplified_Chinese.txt"
    ],

    "zh_Hans\@collation=pinyin",
    [
        "TestNames_Latin.txt",
        "TestNames_Simplified_Chinese.txt"
    ],

    "zh_Hant",
    [
        "TestNames_Chinese.txt",
    ],

    "th\@normalization=on;alternate=shifted",
    [
        "TestNames_Latin.txt",
        "TestNames_Thai.txt",
        "TestRandomWordsUDHR_th.txt"
    ],

    "ar",
    [
        "TestRandomWordsUDHR_ar.txt"
    ],

    "he",
    [
        "TestRandomWordsUDHR_he.txt"
    ]
};

runTests($options, $tests, $dataFiles);
