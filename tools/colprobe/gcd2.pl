#!/usr/bin/perl -w

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;

#my $localeMinusA = `locale -a`;
my $localeMinusA = `cat ~/src/icu/source/extra/colprobe/locale.txt`;
my @locales = split(/\n/, $localeMinusA);
my $locale;
my $command;

my $platform = $ARGV[0];

mkdir $platform."logs2";
mkdir $platform;

foreach $locale (@locales) {
  $command = "~/src/icu/source/extra/colprobe/colprobe --platform $platform --ref $platform --diff $locale >$platform"."logs2/$locale"."Log.txt 2>&1";
  ($locale, $_) = split(/\./, $locale);
  $command .= "; cp /usr/share/i18n/locales/$locale $platform/";
  print "$command\n";
  `$command`;
  #chdir "..";

}
