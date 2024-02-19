#!/usr/bin/perl -w

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;

my $localeMinusA = `locale -a`;
my @locales = split(/\n/, $localeMinusA);
my $locale;
my $command;

my $platform = $ARGV[0];

mkdir $platform."logs";
mkdir $platform;

foreach $locale (@locales) {
  $command = "~/src/icu/source/extra/colprobe/colprobe --output resb --platform linux --ref linux $locale >$platform"."logs/$locale"."Log.txt 2>&1";
  ($locale, $_) = split(/\./, $locale);
  $command .= "; cp /usr/share/i18n/locales/$locale $platform/";
  print "$command\n";
  `$command`;
  #chdir "..";

}
