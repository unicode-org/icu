#/**
# *******************************************************************************
# * Copyright (C) 2001-2004, International Business Machines Corporation and    *
# * others. All Rights Reserved.                                                *
# *******************************************************************************
# */

@rem = '--*-Perl-*--
@echo off
if "%OS%" == "Windows_NT" goto WinNT
perl -x -S "%0" %1 %2 %3 %4 %5 %6 %7 %8 %9
goto endofperl
:WinNT
perl -x -S "%0" %*
if NOT "%COMSPEC%" == "%SystemRoot%\system32\cmd.exe" goto endofperl
if %errorlevel% == 9009 echo You do not have Perl in your PATH.
goto endofperl
@rem ';
#!perl
#line 14

# Usage: perl varsub.bat [-n|-nr] <infile> <outfile>
#
# Substitutes variables into rules and deletes variable definition
# statements.  Variables that expand to UnicodeSets are NOT
# substituted.
#
#  -n   Afterwards, run native2ascii -encoding UTF8
#  -nr  Afterwards, run native2ascii -encoding UTF8 -reverse

$N2A = 0;

$IN = shift;
if ($IN =~ /^-n/) {
    $N2A = 1;
    $N2Aoption = ($IN eq '-nr') ? " -reverse " : "";
    $IN = shift;
}
$OUT = shift;

if (!($IN && $OUT)) {
    die "Usage: $0 [-n|-nr] <infile> <outfile>";
}

open(IN) or die "Can't open $IN: $!";
open(OUT, ">$OUT") or die "Can't open $OUT: $!";

while (<IN>) {
    if (/^\s*\$([a-zA-Z0-9_]+)\s*=\s*([^;\#]+)\s*;\s*(\#.*)?$/) {
        # This looks like a variable definition
        my ($var, $def) = ($1, $2);
        # Don't substitute UnicodeSet vars
        if ($def !~ /^\[/) {
            if (exists $VAR{$var}) {
                print STDERR "Error: Duplicate definition of $var\n";
            } else {
                $VAR{$var} = $def;
            }
            next;
        }
    }
    
    # Do variable substitutions, and output line
    foreach my $var (keys %VAR) {
        my $def = $VAR{$var};
        s/\$$var\b/$def/g;
    }
    print OUT;
}

close(OUT);
close(IN);

if ($N2A) {
    `native2ascii -encoding UTF8 $N2Aoption $OUT $OUT.native2ascii`;
    unlink $OUT;
    rename "$OUT.native2ascii", $OUT;
}

__END__
:endofperl
