#!/usr/bin/perl -w
#
# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: https://www.unicode.org/copyright.html
#
# ***********************************************************************
# COPYRIGHT:
# Copyright (c) 2002-2011, International Business Machines Corporation
# and others. All Rights Reserved.
# ***********************************************************************
#
# Search for and list files which don't have a copyright notice, and should.
#
use strict;
use warnings;
use File::Find;

use FindBin qw($Bin);
use lib $Bin;

use Cpy;
my $icu_src = $ARGV[0] || ".";
my $exitStatus = 0;
my $icu_src_len = length($icu_src);
die "Can't open ICU directory: $icu_src" unless -d $icu_src;
find({
        wanted => sub {
            # save a little bit of time.
            if ($_ eq './.git') {
                $File::Find::prune = 1;
                return;
            }
            return unless -f;
            my $relpath = substr($_, $icu_src_len + 1);
            return if should_ignore($relpath);

            my $found = 0;  # Used as a bitfield. 1 = copyright, 2 = terms of use
            open F, "<$_" or die "Error opening '$_'.";
            while (<F>) {
                if (/(Copyright|©).*\d{4}.*Unicode/i) {
                    $found |= 1;
                }
                if (/terms of use.*http.*www\.unicode\.org\/(copyright|terms_of_use)\.html/i) {
                    $found |= 2;
                }
                if ($found == 3) {
                    # We found both notes, we end the loop, no point to scan the whole file
                    last;
                }
            }
            close F;

            if ($found != 3) { # Copyright or the terms of use missing
                print "$relpath\n";
                $exitStatus = 1;
            }
        },
        no_chdir => 1,
    }, $icu_src);

if ($exitStatus) {
    die "Above files did not contain the correct copyright notice.";
}
exit $exitStatus;
