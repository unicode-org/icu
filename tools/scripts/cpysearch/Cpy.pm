#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#  ***********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2011, International Business Machines Corporation
#  * and others. All Rights Reserved.
#  ***********************************************************************
#
# Common functionality between cpysearch.pl and cpyscan.pl
#

package Cpy;
use strict;
use warnings;
use base 'Exporter';

our @EXPORT = qw(any glob_to_regex should_ignore);

# any(CODE, LIST)
# Evaluate CODE for each element of LIST till CODE($_) returns 1. Return 0 if
# not found.
sub any(&@) {
    my $code = shift;
    local $_;
    &$code && return 1 for @_;
    0;
}

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

# Load cpyskip.txt contents.
# Try local .cpyskip.txt
# no support for HTTP fetch.
our $cpyskip_file = ".cpyskip.txt";
our @cpyskip_lines;
if (open(our $cpyskip_fh, "<", $cpyskip_file)) {
    @cpyskip_lines = <$cpyskip_fh>;
    close $cpyskip_fh;
    # print "Using local cpyskip.txt\n";
} else {
    die "Could not open $cpyskip_file";
}
our @ignore_globs = map  { chomp; glob_to_regex($_) }
                    grep { /^\s*[^#\s]+/ }
                    @cpyskip_lines;

#for my $rgx (@ignore_globs) {print $rgx . "\n"}
#exit(0);

# list of file extensions to ignore
our @ignore_extensions = qw(svn dll ilk idb pdb dsp dsw opt ncb vcproj sln suo
    cvsignore cnv res icu exe obj bin exp lib out plg jar spp stub policy ttf
    TTF otf);
our $ignore_exts = join '|',
                   map { "\\.$_" }
                   @ignore_extensions;

# ignore regex
our $ignore_regex = "data/out/build|CVS|\\~|\\#|Debug|Release|positions|unidata|sources\.txt|$ignore_exts";

# Check if this file should be ignored.
sub should_ignore($) {
    my $filename = shift;
    return 1 if $filename eq $cpyskip_file;
    return 1 if $filename =~ /$ignore_regex/;
    for my $r (@ignore_globs) { return 1 if $filename =~ /$r/ }
    0;
}

1;
