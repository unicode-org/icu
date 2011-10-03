#!/usr/bin/perl -w
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

my $icu_src = $ARGV[0] || ".";
my $cpyskip = 'cpyskip.txt';

die "Can't open ICU directory: $icu_src" unless -d $icu_src;

die ("Can't find $cpyskip. Please download it from ".
     "http://source.icu-project.org/cpyskip.txt (see ".
     "http://site.icu-project.org/processes/copyright-scan for more details).")
    unless -f $cpyskip;

# list of file extensions to ignore
my @ignore_extensions = qw(svn dll ilk idb pdb dsp dsw opt ncb vcproj sln suo
    cvsignore cnv res icu exe obj bin exp lib out plg jar spp stub policy ttf
    TTF otf);
my $ignore_exts = join '|',
                  map { "\\.$_" }
                  @ignore_extensions;

# ignore regex
my $ignore = "CVS|\\~|\\#|Debug|Release|positions|unidata|$ignore_exts";

my $year = localtime->year + 1900;

# Perl doesn't have fnmatch. Closest thing is File::FnMatch but that's
# UNIX-only (see its caveats). So, as a workaround, convert globs to regular
# expressions. Translated from Python's fnmatch module.
sub glob_to_regex($) {
    my ($glob, $i, $len, $regex);
    $glob = shift;
    $i = 0;
    $len = length($glob);
    $regex = "";

    # charat(STR, IDX)
    # Return the character in the argument at the given index.
    my $charat = sub($$) { return substr(shift, shift, 1) };

    while ($i < $len) {
        my ($c, $out);
        $c = &$charat($glob, $i++);
        if    ($c eq '*') { $out = '.*' }
        elsif ($c eq '?') { $out = '.' }
        elsif ($c eq '[') { # glob classes
            my $j = $i;

            # Get the closing index of the class. ] appearing here is part
            # of the class.
            if    ($j < $len && &$charat($glob, $j) eq '!') { $j++ }
            if    ($j < $len && &$charat($glob, $j) eq ']') { $j++ }
            while ($j < $len && &$charat($glob, $j) ne ']') { $j++ }

            # Didn't find closing brace. Use literal [
            if ($j >= $len) { $out = "\\[" }

            else {
                # The complete class contents (except the braces)
                my $s = substr($regex, $i, $j - $i);
                $s =~ s/\\/\\\\/g;
                $i = $j + 1; # change position to outside class

                # Negation
                if (&$charat($s, 0) eq '!') { $s = '^'.substr($s, 1); }
                # Literal ^
                elsif (&$charat($s, 0) eq '^') { $s = '\\'.$s; }

                $out = "[$s]";
            }
        }
        else { $out = quotemeta($c) }
        $regex .= $out;
    }
    return $regex;
}

open SKIP, "<$cpyskip" or die "Error opening $cpyskip.";
my @ignore_globs = map { chomp; glob_to_regex($_) } <SKIP>;
close SKIP;

# Check if this file should be ignored.
sub ignore($) {
    my $filename = shift;
    return 1 if $filename =~ /$ignore/;
    for my $r (@ignore_globs) { return 1 if $filename =~ /$r/ }
    0;
}

# any(CODE, LIST)
# Evaluate CODE for each element of LIST till CODE($_) returns 1. Return 0 if
# not found.
sub any(&@) {
    my $code = shift;
    local $_;
    &$code && return 1 for @_;
    0;
}

find({
        wanted => sub { # $_ is the full path to the file
            return unless -f;
            return if (localtime(stat($_)->mtime)->year + 1900) < $year;
            return if ignore($_);
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
