#!/usr/bin/perl
#*
#*******************************************************************************
#*   Copyright (C) 2001, International Business Machines
#*   Corporation and others.  All Rights Reserved.
#*******************************************************************************
#*
#*   file name:  genren.pl
#*   encoding:   US-ASCII
#*   tab size:   8 (not used)
#*   indentation:4
#*
#*   Created by: Vladimir Weinstein
#*   07/19/2001
#*
#*  Used to generate renaming headers.
#*  Run on UNIX platforms (linux) in order to catch all the exports

#if($ARGV[0] == "--v") {
#    @VERBOSE = STDOUT;
#    splice @ARGV, 0, 1;
#} else {
#    print "use --v for VERBOSE";
#}

#foreach $a (@ARGV) {
#    print "argument is $a\n";
#}

$U_ICU_VERSION_SUFFIX = "_1_8";

@NMRESULT = `nm -Cg -f p $ARGV[0]`;

$HEADERNAME = join('', ("uren", substr($ARGV[0], 6, index(".", $ARGV[0])-7),".h"));

$_ = $HEADERNAME;
s/\./_/;
$HEADERDEF = uc($_);

print VERBOSE substr($ARGV[0], 6, 2), ", $HEADERNAME, $HEADERDEF\n";

open HEADER, ">$HEADERNAME"; # opening a header file

#We will print our copyright here + warnings
print HEADER "
/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  $HEADERNAME
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: Perl script written by Vladimir Weinstein
*
*  Contains data for renaming ICU exports in the $ARGV[0] library
*  Gets included by utypes.h
*
*  THIS FILE IS MACHINE-GENERATED, DON'T PLAY WITH IT IF YOU DON'T KNOW WHAT
*  YOU ARE DOING, OTHERWISE VERY BAD THINGS WILL HAPPEN!
*/
";

print HEADER "
#ifndef $HEADERDEF
#define $HEADERDEF\n\n";

#print HEADER "#include \"unicode/uversion.h\"\n\n";

print HEADER "\n/* C exports renaming data */\n\n";

foreach (@NMRESULT) { # Process every line of result and stuff it in $_
    if(/@@/ or /\./ or /\(/ ) { # These would be imports
        print VERBOSE "Import: $_";
    } elsif (/::/) { # C++ methods, stuff class name in associative array
        print VERBOSE "C++ method: $_";
        @CppName = split(/::/);
        $CppClasses{$CppName[0]}++;
    } else {
        print VERBOSE "C func: $_";
        @funcname = split(/\s+/);
        print HEADER "#define $funcname[0] $funcname[0]$U_ICU_VERSION_SUFFIX\n";
    }
}

print HEADER "\n/* C++ class names renaming defines */\n\n";

foreach(keys(%CppClasses)) {
    print HEADER "#define $_ $_$U_ICU_VERSION_SUFFIX\n";
}

print HEADER "#endif\n";








