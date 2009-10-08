#!/usr/bin/perl 
#*
#*******************************************************************************
#*   Copyright (C) 2001-2009, International Business Machines
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

use POSIX qw(strftime);

$headername = 'urename.h';

$path = substr($0, 0, rindex($0, "/")+1)."../../common/unicode/uversion.h";

$nmopts = '-Cg -f s';
$post = '';

$mode = 'POSIX';

(-e $path) || die "Cannot find uversion.h";

open(UVERSION, $path);

while(<UVERSION>) {
    if(/\#define U_ICU_VERSION_SUFFIX/) {
        chop;
        s/\#define U_ICU_VERSION_SUFFIX //;
        $U_ICU_VERSION_SUFFIX = "$_";
        last;
    }
}

while($ARGV[0] =~ /^-/) { # detects whether there are any arguments
    $_ = shift @ARGV;      # extracts the argument for processing
    /^-v/ && ($VERBOSE++, next);                      # verbose
    /^-h/ && (&printHelpMsgAndExit, next);               # help
    /^-o/ && (($headername = shift (@ARGV)), next);   # output file
    /^-n/ && (($nmopts = shift (@ARGV)), next);   # nm opts
    /^-p/ && (($post = shift (@ARGV)), next);   # nm opts
    /^-x/ && (($mode = shift (@ARGV)), next);   # nm opts
    /^-S/ && (($U_ICU_VERSION_SUFFIX = shift(@ARGV)), next); # pick the suffix
    warn("Invalid option $_\n");
    &printHelpMsgAndExit;
}

unless(@ARGV > 0) {
    warn "No libraries, exiting...\n";
    &printHelpMsgAndExit;
}

#$headername = "uren".substr($ARGV[0], 6, index(".", $ARGV[0])-7).".h";
    
$HEADERDEF = uc($headername);  # this is building the constant for #define
$HEADERDEF =~ s/\./_/;

    
    open HEADER, ">$headername"; # opening a header file

#We will print our copyright here + warnings

$YEAR = strftime "%Y",localtime;

print HEADER <<"EndOfHeaderComment";
/*
*******************************************************************************
*   Copyright (C) 2002-$YEAR, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  $headername
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: Perl script written by Vladimir Weinstein
*
*  Contains data for renaming ICU exports.
*  Gets included by umachine.h
*
*  THIS FILE IS MACHINE-GENERATED, DON'T PLAY WITH IT IF YOU DON'T KNOW WHAT
*  YOU ARE DOING, OTHERWISE VERY BAD THINGS WILL HAPPEN!
*/

#ifndef $HEADERDEF
#define $HEADERDEF

/* Uncomment the following line to disable renaming on platforms
   that do not use Autoconf. */
/* #define U_DISABLE_RENAMING 1 */

#if !U_DISABLE_RENAMING

/* We need the U_ICU_ENTRY_POINT_RENAME definition. There's a default one in unicode/uvernum.h we can use, but we will give 
   the platform a chance to define it first.
   Normally (if utypes.h or umachine.h was included first) this will not be necessary as it will already be defined.
 */
#ifndef U_ICU_ENTRY_POINT_RENAME
/**
 * Are we using platform.h? If so, include platform.h here. 
 * NOTE: this logic was copied from umachine.h and must be kept in sync with it.
 */
#if defined(U_PALMOS)
   /* No equivalent platform.h.  Was: include "unicode/ppalmos.h" */
#elif defined(WIN32) || defined(_WIN32) || defined(WIN64) || defined(_WIN64)
   /* No equivalent platform.h.  Was: include "unicode/pwin32.h" */
#else
#   include "unicode/platform.h"  /* platform.h is 'pure defines' */
   /* Do not include "unicode/ptypes.h" - ptypes.h contains typedefs and other includes */
#endif
#endif

/* If we still don't have U_ICU_ENTRY_POINT_RENAME use the default. */
#ifndef U_ICU_ENTRY_POINT_RENAME
#include "unicode/uvernum.h"
#endif

/* Error out before the following defines cause very strange and unexpected code breakage */
#ifndef U_ICU_ENTRY_POINT_RENAME
#error U_ICU_ENTRY_POINT_RENAME is not defined - cannot continue. Consider defining U_DISABLE_RENAMING if renaming should not be used.
#endif

EndOfHeaderComment

for(;@ARGV; shift(@ARGV)) {
    @NMRESULT = `nm $nmopts $ARGV[0] $post`;
    if($?) {
        warn "Couldn't do 'nm' for $ARGV[0], continuing...\n";
        next; # Couldn't do nm for the file
    }
    if($mode =~ /POSIX/) {
        splice @NMRESULT, 0, 6;
    } elsif ($mode =~ /Mach-O/) {
#        splice @NMRESULT, 0, 10;
    }
    foreach (@NMRESULT) { # Process every line of result and stuff it in $_
        if($mode =~ /POSIX/) {
            ($_, $address, $type) = split(/\|/);
        } elsif ($mode =~ /Mach-O/) {
            if(/^(?:[0-9a-fA-F]){8} ([A-Z]) (?:_)?(.*)$/) {
                ($_, $type) = ($2, $1);
            } else {
                next;
            }
        } else {
            die "Unknown mode $mode";
        }
        &verbose( "type: \"$type\" ");
        if(!($type =~ /[UAwW?]/)) {
            if(/@@/) { # These would be imports
                &verbose( "Import: $_ \"$type\"\n");
                &verbose( "C++ method: $_\n");
            } elsif (/^[^\(]*::/) { # C++ methods, stuff class name in associative array
	        ##  DON'T match    ...  (   foo::bar ...   want :: to be to the left of paren
                ## icu_2_0::CharString::~CharString(void) -> CharString
                @CppName = split(/::/); ## remove scope stuff
                if(@CppName>1) {
                    ## MessageFormat virtual table -> MessageFormat
                    @CppName = split(/ /, $CppName[1]); ## remove debug stuff
                }
                ## ures_getUnicodeStringByIndex(UResourceBundle -> ures_getUnicodeStringByIndex
                @CppName = split(/\(/, $CppName[0]); ## remove function args
                if($CppName[0] =~ /^operator/) {
                    &verbose ("Skipping C++ function: $_\n");
                } elsif($CppName[0] =~ /^~/) {
                    &verbose ("Skipping C++ destructor: $_\n");
                } else {
		    &verbose( " Class: '$CppName[0]': $_ \n");
                    $CppClasses{$CppName[0]}++;
                }
	    } elsif ( my ($cfn) = m/^([A-Za-z0-9_]*)\(.*/ ) {
		&verbose ( "$ARGV[0]:  got global C++ function  $cfn with '$_'\n" );
                $CFuncs{$cfn}++;
            } elsif ( /\(/) { # These are strange functions
                print STDERR "$ARGV[0]: Not sure what to do with '$_'\n";
            } elsif ( /icu_/) {
                print STDERR "$ARGV[0]: Skipped strange mangled function $_\n";
            } elsif ( /^vtable for /) {
                print STDERR "$ARGV[0]: Skipped vtable $_\n";
            } elsif ( /^typeinfo for /) {
                print STDERR "$ARGV[0]: Skipped typeinfo $_\n";
            } elsif ( /operator\+/ ) {
                print STDERR "$ARGV[0]: Skipped ignored function $_\n";
            } else { # This is regular C function 
                &verbose( "C func: $_\n");
                @funcname = split(/[\(\s+]/);
                $CFuncs{$funcname[0]}++;
            }
        } else {
            &verbose( "Skipped: $_ $1\n");
        }
    }
}

print HEADER "\n/* C exports renaming data */\n\n";
foreach(sort keys(%CFuncs)) {
    print HEADER "#define $_ U_ICU_ENTRY_POINT_RENAME($_)\n";
#    print HEADER "#define $_ $_$U_ICU_VERSION_SUFFIX\n";
}

print HEADER "/* C++ class names renaming defines */\n\n";
print HEADER "#ifdef XP_CPLUSPLUS\n";
print HEADER "#if !U_HAVE_NAMESPACE\n\n";
foreach(sort keys(%CppClasses)) {
    print HEADER "#define $_ U_ICU_ENTRY_POINT_RENAME($_)\n";
}
print HEADER "\n#endif\n";
print HEADER "#endif\n";
print HEADER "\n#endif\n";
print HEADER "\n#endif\n";

close HEADER;

sub verbose {
    if($VERBOSE) {
        print STDERR @_;
    }
}


sub printHelpMsgAndExit {
    print STDERR <<"EndHelpText";
Usage: $0 [OPTIONS] LIBRARY_FILES
  Options: 
    -v - verbose
    -h - help
    -o - output file name (defaults to 'urename.h'
    -S - suffix (defaults to _MAJOR_MINOR of current ICU version)
Will produce a renaming .h file

EndHelpText

    exit 0;

}

