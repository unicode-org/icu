#!/usr/bin/perl -w

use strict;

my $localeMinusA = `cat /home/weiv/src/icu/source/extra/colprobe/locale.txt`;
my @locales = split(/\n/, $localeMinusA);
my $locale;
my $command;

foreach $locale (@locales) {
  if($locale =~ /_/ && !($locale =~ /^#/)) {
    $command = "/home/weiv/src/icu/source/extra/colprobe/doComparisonTable.pl $locale";
    print "$command\n";
    `$command`;
  }
}
