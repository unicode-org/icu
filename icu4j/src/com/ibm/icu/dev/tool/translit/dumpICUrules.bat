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
# in icu/data, from ICU4J UTF8 transliterator data files, in
# icu4j/src/com/ibm/text/resources.
#
# The transformation that is done is very minimal.  The script assumes
# that the input files use only # comments
# and that they follow a rigid format.
#
# The output files are named according to ICU conventions (see NAME_MAP
# below) and created in the current directory.  They should be manually
# checked and then copied into the icu/data directory.  An ICU build must
# then be initiated, and the standard suite of ICU transliterator tests
# should be run after that.
#
# Alan Liu 5/19/00 2/27/01

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
    print " where <dir> contains the Transliterator_*.utf8.txt\n";
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
        { "Hiragana-Katakana",   "Katakana-Hiragana",   "kana"     }

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
// Copyright (c) 1999-2001, International Business Machines
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
foreach (<$DIR/Transliterator_*.utf8.txt>) {
    next if (/~$/);
    my $id;
    if (m|Transliterator_(.+)\.utf8\.txt$|) {
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
#  f:/icu4j/src/com/ibm/text/resources/Transliterator_Fullwidth_Halfwidth.utf8.txt
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
    binmode OUT; # Must do this so we can write our UTF8 marker
    
    # Write UTF8 marker
    print OUT pack("C3", 0xEF, 0xBB, 0xBF);
    print OUT " // -*- Coding: utf-8; -*-\n";

    print OUT $HEADER1;
    print OUT "// Tool: $TOOL\n// Source: $IN\n";
    print OUT "// Date: ", scalar localtime, "\n";
    print OUT $HEADER2;
    print OUT "\n";
    print OUT "// $id\n";
    print OUT "\n";
    print OUT "$out {\n";
    print OUT "  Rule {\n";

    open(IN, $IN) or die;
    binmode IN; # IN is a UTF8 file

    # Process each line by changing # comments to // comments
    # and taking other text and enclosing it in double quotes
    while (<IN>) {
        my $raw = $_;
        
        # Clean the eol junk up
        s/[\x0D\x0A]+$//;

        # Transform escaped characters
        hideEscapes();

        if (/^(\s*)(\#.*)$/) {
            # Comment-only line
            my ($white, $cmt) = ($1, $2);
            $cmt =~ s|\#|//|;
            $_ = $white . $cmt;

        } elsif (/^(\s*)(\S.*?)(\s*)(\#.*)?$/) {
            # Rule line with optional comment
            my ($white1, $rule, $white2, $cmt) = ($1, $2, $3, $4);
            $cmt =~ s|\#|//| if ($cmt);
            $_ = $white1 . '"' . $rule . '"' . $white2 . $cmt;

        } elsif (!/\S/) {
            # Blank line -- leave as-is

        } else {
            # Unparseable line
            print STDERR "Error: Can't parse line: $raw";
        }
        
        # Restore escaped characters
        restoreEscapes();

        print OUT $_, "\n";
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
