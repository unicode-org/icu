#!/usr/bin/perl -w
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2003, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************
use strict;

my $icuSource = $ARGV[0];
my $ignore = "CVS|\\~|\\#|Debug|Release|dsp|dsw|opt|ncb|vcproj|sln|suo|cvsignore|cnv|res|\\.icu|exe|\\.out|build|plg|positions|unidata";

my ($sec, $min, $hour, , $day, $mon, $year, $wday, $yday, $isdst) = localtime;
$year += 1900;

my $command = "find $icuSource -type f -mtime -$yday";
my @files = `$command`;
@files = grep(!/$ignore/, @files);
my $file;
foreach $file (@files) {
  my @lines = `head -n 10 $file`;
  if (grep(/copyright.*$year/i, @lines) == 0) {
    print "$file";
  }
}
