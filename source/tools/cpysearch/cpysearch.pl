#!/usr/bin/perl -w
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************
use strict;

my $icuSource = "/icu/source";
my $ignore = "CVS|\\~|\\#|Debug|Release|dsp|dsw|opt|ncb|cvsignore|cnv|res|\\.icu|exe|out|build|plg|positions|unidata";

my $command = "find $icuSource -type f";
#my $command = "find $icuSource -type f\|grep -v \"$ignore\"";
my @files = `$command`;
@files = grep(!/$ignore/, @files);
my $file;
foreach $file (@files) {
  my @lines = `head -n 10 $file`;
  if (grep(/copyright/i, @lines) == 0) {
    print "$file";
  }
}
