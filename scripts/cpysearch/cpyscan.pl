#!/usr/bin/perl -w
#  ***********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2011, International Business Machines Corporation
#  * and others. All Rights Reserved.
#  ***********************************************************************
# 
#  Search for and list files which don't have a copyright notice, and should.
#
use strict;
use warnings;
use File::Find;

use FindBin qw($Bin);
use lib $Bin;

use Cpy;
my $icu_src = $ARGV[0] || ".";
die "Can't open ICU directory: $icu_src" unless -d $icu_src;
find({
        wanted => sub {
            return unless -f;
            return if should_ignore($_);

            open F, "<$_" or die "Error opening '$_'.";
            my $result = any { $_ =~ /copyright.*(international|ibm)/i } <F>;
            close F;

            print "$_\n" unless $result;
        },
        no_chdir => 1,
    }, $icu_src);
