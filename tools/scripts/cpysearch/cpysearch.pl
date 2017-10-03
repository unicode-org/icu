#!/usr/bin/perl -w
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#  ***********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2011, International Business Machines Corporation
#  * and others. All Rights Reserved.
#  ***********************************************************************
#
# Search for files modified this year, that need to have copyright indicating
# this current year on them.
#
use strict;
use warnings;
use Time::localtime;
use File::stat;
use File::Find;

# Add script directory to Perl PATH.
use FindBin qw($Bin);
use lib $Bin;

use Cpy;

my $icu_src = $ARGV[0] || ".";
die "Can't open ICU directory: $icu_src" unless -d $icu_src;
my $year = localtime->year + 1900;

find({
        wanted => sub { # $_ is the full path to the file
            return unless -f;
            return if (localtime(stat($_)->mtime)->year + 1900) < $year;
            return if should_ignore($_);
            # file is recent and shouldn't be ignored. find copyright.

            # if file contains a line with "copyright" and current year on the
            # same line, we're good.
            open F, "<$_" or die "Error opening '$_'.";
            my $result = any { $_ =~ /copyright.*$year/i } <F>;
            close F;

            print "$_\n" unless $result;
        },
        no_chdir => 1,
    }, $icu_src);
