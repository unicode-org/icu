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

# This perl script creates ICU transliterator data files, that live
# in icu/data, from ICU4J Java transliterator data files, in
# icu4j/src/com/ibm/text/resources.
#
# The transformation that is done is very minimal.  The script assumes
# that the Java input files use only // comments (no /**/ comments)
# and that they follow a rigid format.  Leading or trailing '+' (but not both)
# concatenation operators are stripped from each line.
#
# The output files are named according to ICU conventions (see NAME_MAP
# below) and created in the current directory.  They should be manually
# checked and then copied into the icu/data directory.  An ICU build must
# then be initiated, and the standard suite of ICU transliterator tests
# should be run after that.
#
# Alan Liu 5/19/00

if (scalar @ARGV != 1) {
    usage();
}
$DIR = shift;
if (! -d $DIR) {
    usage();
}

sub usage {
    my $me = $0;
    $me =~ s|.+[/\\]||;
    print "Usage: $me <dir>\n";
    print " where <dir> contains the TransliteratorRule_*.java\n";
    print " files.\n";
    print "e.g., $me F:/icu4j/src/com/ibm/text/resources\n";
    die;
}

# Mapping from Java IDs to ICU file names
# Copied from icu/data/translit_index.txt, with long lines folded into 1 line
$NAME_MAP = <<'END';
        { "Fullwidth-Halfwidth", "Halfwidth-Fullwidth", "fullhalf" }
        { "Latin-Arabic",        "Arabic-Latin",        "larabic"  }
        { "Latin-Cyrillic",      "Cyrillic-Latin",      "lcyril"   }
        { "Latin-Devanagari",    "Devanagari-Latin",    "ldevan"   }
        { "Latin-Greek",         "Greek-Latin",         "lgreek"   }
        { "Latin-Hebrew",        "Hebrew-Latin",        "lhebrew"  }
        { "Latin-Jamo",          "Jamo-Latin",          "ljamo"    }
        { "Latin-Kana",          "Kana-Latin",          "lkana"    }

        // Other miscellaneous rules
        { "StraightQuotes-CurlyQuotes", "CurlyQuotes-StraightQuotes", "quotes" }
        { "KeyboardEscape-Latin1", "", "kbdescl1" }
        { "UnicodeName-UnicodeChar", "", "ucname" }
END

foreach (split(/\n/, $NAME_MAP)) {
    s|//.+||;
    if (m|\"(.+)\".+\"(.*)\".+\"(.+)\".+|) {
        $NAME_MAP{$1} = $3;
    } elsif (/\S/) {
        print STDERR "Ignoring $_\n";
    }
}

# Header blocks of text written at start of ICU output files
$HEADER1 = <<END;
//--------------------------------------------------------------------
// Copyright (c) 1999-2000, International Business Machines
// Corporation and others.  All Rights Reserved.
//--------------------------------------------------------------------
// THIS IS A MACHINE-GENERATED FILE
END
$HEADER2 = <<END;
//--------------------------------------------------------------------
END

$TOOL = $0;

# Iterate over all Java RBT resource files.  Process those with a mapping to
# an ICU name.
foreach (<$DIR/TransliterationRule_*.java>) {
    next if (/~$/);
    my $id;
    if (m|TransliterationRule_(.+)\.java$|) {
        $id = $1;
    } else { die; }
    $id =~ s/_/-/g;
    if (!exists $NAME_MAP{$id}) {
        print STDERR "$id: skipping, no ICU file name\n";
        next;
    }
    file($id, $_, $NAME_MAP{$id});
}

# Process one file
# Param: ID, e.g. Fullwidth-Halfwidth
# Param: Java input file name, e.g.
#  f:/icu4j/src/com/ibm/text/resources/TransliterationRule_Fullwidth_Halfwidth.java
# Param: ICU output file name, e.g. fullhalf
sub file {
    my $id = shift;
    my $IN = shift;
    my $out = shift;

    my $OUT = "$out.txt";

    # Show input size. Show output size later -- useful for quick sanity check.
    print "$id (", -s $IN, ") -> $OUT (";

    # Write output file header
    open(OUT, ">$OUT") or die;
    print OUT $HEADER1;
    print OUT "// Tool: $TOOL\n// Source: $IN\n";
    print OUT "// Date: ", scalar localtime, "\n";
    print OUT $HEADER2;
    print OUT "\n";
    print OUT "// $id\n";
    print OUT "\n";
    print OUT "$out {\n";
    print OUT "  Rule {\n";

    # Open input file and skip over everything before "Rule" RB key
    open(IN, $IN) or die;
    while (<IN>) {
        last if (/\"Rule\"/);
    }

    # Process each line by deleting leading or trailing '+' (but not both)
    # and by normalizing leading space.

    # Recognize these kinds of lines:
    #  "9>\u0669;"+ // optional comment
    #  +"9>\u0669;" // optional comment
    #  // comment
    #  + "Zh>$ZH;" + "Zh<$ZH}$lower;"
    #  "'account of%'>\u2100",  -- this occurs in a String[] resource
    while (<IN>) {
        last if (/^\s*\}/); # Any line starting with '}' ends the rule set

        # NOTE: We have to handle a rule like this:
        #   "a", "b", "c",
        # that fails to terminate statements with separators.

        # Trim leading and trailing space
        s|^\s+||;
        s|\s+$||;

        my $raw = $_;

        # Transform escaped characters
        hideEscapes();
        
        # Process double-quoted strings
        my $q;
        for (;;) {
            if (s|^\s*,\s*||) { # Trim leading ','
                # Add separator between comma-separated rules
                # if it isn't there already:
                # "a>b", "c>d" -> "a>b;" "c>d"
                print STDERR "Error: Can't parse \"$raw\"" unless ($q);
                $q =~ s|\"$|;\"| unless ($q =~ m|;\"$|);
            } else {
                s|^\s*\+\s*||; # Trim leading '+'
            }
            if (s|^(\".*?\")||) {
                $q .= ' ' if ($q);
                $q .= $1;
            } else {
                last;
            }
        }

        if (s|^\s*,\s*||) { # Trim final ','
            print STDERR "Error: Can't parse \"$raw\"" unless ($q);
            $q =~ s|\"$|;\"| unless ($q =~ m|;\"$|);            
        } else {
            s|^\s*\+\s*||; # Trim final '+'
        }

        # Remove and save trailing // comment
        my $cmt;
        if (s|^\s*(//.*)$||) {
            $cmt = ' ' if ($q);
            $cmt .= $1;
        }
        
        if (/\S/) {
            chomp($raw);
            print STDERR "Error: left over \"$_\" in \"$raw\"\n";
        }

        $_ = "    " . $q . $cmt . "\n";

        # Restore escaped characters
        restoreEscapes();

        print OUT;
    }

    # Finish up
    close(IN);
    print OUT "  }\n";
    print OUT "}\n";
    close(OUT);

    # Write output file size for sanity check
    print -s $OUT, ")\n";
}

sub hideEscapes {
    # Transform escaped characters
    s|\\u([a-zA-Z0-9]{4})|<<u$1>>|g; # Transform Unicode escapes
    s|\\\"|<<dq>>|; # Transform backslash double quote
    s|\\(.)|<<q$1>>|; # Transform backslash escapes
}

sub restoreEscapes {
    # Restore escaped characters
    s|<<dq>>|\\\"|g;
    s|<<q(.)>>|\\$1|g;
    s|<<u0000>>|\\\\u0000|g; # Double escape U+0000
    s|<<u(....)>>|\\u$1|g;
}

__END__
:endofperl
