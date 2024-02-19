#!/usr/bin/perl -w

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;

#my $localeMinusA = `locale -a`;
my $localeMinusA = `cat locale.txt`;

my @locales = split(/\r\n/, $localeMinusA);
my $locale;
my $command;

#my $commandPath = "~/src/icu/source/extra/colprobe/";
my $commandPath = "c:/dev/0_icu/source/extra/colprobe/release/";


my $platform = $ARGV[0];

mkdir $platform."logs";
mkdir $platform;

foreach $locale (@locales) {
  $_ = $locale;
  chomp;
  if(!/^\#/) { # && /\_/) {
    $command = $commandPath."colprobe --platform $platform --ref $platform --output resb $locale >$platform"."logs/$locale"."_log.txt 2>&1";
 
    print "$command\n";
    `$command`;
  }
}
