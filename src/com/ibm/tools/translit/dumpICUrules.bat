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

$JAVA_ONLY = '-';

# Mapping from Java file names to ICU file names
%NAME_MAP = (
             "Fullwidth_Halfwidth" =>        "fullhalf",
             "Hiragana_Katakana" =>          "kana",
             "KeyboardEscape_Latin1" =>      "kbdescl1",
             "Latin_Arabic" =>               "larabic",
             "Latin_Cyrillic" =>             "lcyril",
             "Latin_Devanagari" =>           "ldevan",
             "Latin_Greek" =>                "lgreek",
             "Latin_Hebrew" =>               "lhebrew",
             "Latin_Jamo" =>                 "ljamo",
             "Latin_Kana" =>                 "lkana",
             "StraightQuotes_CurlyQuotes" => "quotes",
             "UnicodeName_UnicodeChar" =>    "ucname",
             
             # An ICU name of "" means the ICU name == the ID
             "Bengali_InterIndic" =>         "",
             "Devanagari_InterIndic" =>      "",
             "Gujarati_InterIndic" =>        "",
             "Gurmukhi_InterIndic" =>        "",
             "Kannada_InterIndic" =>         "",
             "Malayalam_InterIndic" =>       "",
             "Oriya_InterIndic" =>           "",
             "Tamil_InterIndic" =>           "",
             "Telugu_InterIndic" =>          "",
             "InterIndic_Bengali" =>         "",
             "InterIndic_Devanagari" =>      "",
             "InterIndic_Gujarati" =>        "",
             "InterIndic_Gurmukhi" =>        "",
             "InterIndic_Kannada" =>         "",
             "InterIndic_Malayalam" =>       "",
             "InterIndic_Oriya" =>           "",
             "InterIndic_Tamil" =>           "",
             "InterIndic_Telugu" =>          "",
             
             # These files are large, so ICU doesn't want them
             "Han_Pinyin" => $JAVA_ONLY,
             "Kanji_English" => $JAVA_ONLY,
             "Kanji_OnRomaji" => $JAVA_ONLY,
             );

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

# Iterate over all Java RBT rule files
foreach (<$DIR/Transliterator_*.utf8.txt>) {
    next if (/~$/);
    my ($out, $id) = convertFileName($_);
    if ($out) {
        if ($out eq $JAVA_ONLY) {
            print STDERR "$id: Java only\n";
            next;
        }
        file($id, $_, $out);
    }
}

convertIndex();

######################################################################
# Convert a Java file name to C
# Param: Java file name of the form m|Transliterator_(.+)\.utf8\.txt$|
# Return: A C file name (e.g., ldevan.txt) or the empty string,
#  if there is no mapping, or $JAVA_ONLY if the given file isn't
#  intended to be incorporated into C.
sub convertFileName {
    local $_ = shift;
    my $id;
    if (m|Transliterator_(.+)\.utf8\.txt$|) {
        $id = $1;
    } else { die "Can't parse Java file name $_"; }
    if (!exists $NAME_MAP{$id}) {
        print STDERR "ERROR: $id not in map; please update $0\n";
        return '';
    }
    my $out = $NAME_MAP{$id};
    if ($out eq '') {
        $out = $id;
    }
    return ($out, $id);
}

######################################################################
# Convert the index file from Java to C format
sub convertIndex {
    $JAVA_INDEX = "Transliterator_index.txt";
    $C_INDEX = "translit_index.txt";
    open(JAVA_INDEX, "$DIR/$JAVA_INDEX") or die;
    open(C_INDEX, ">$C_INDEX") or die;
    
    header(\*C_INDEX, $JAVA_INDEX);
    
    print C_INDEX <<END;
//--------------------------------------------------------------------
// N.B.: This file has been generated mechanically from the
// corresponding ICU4J file, which is the master file that receives
// primary updates.  The colon-delimited fields have been split into
// separate strings.  For 'file' and 'internal' lines, the encoding
// field has been deleted, since the encoding is processed at build
// time in ICU4C.  Certain large rule sets not intended for general
// use have been commented out with the notation "Java only".
//--------------------------------------------------------------------

translit_index {
  RuleBasedTransliteratorIDs {
END
                
    while (<JAVA_INDEX>) {
        # Comments; change # to //
        if (s|^(\s*)\#|$1//|) {
            print C_INDEX;
            next;
        }
        # Blank lines
        if (!/\S/) {
            print C_INDEX;
            next;
        }
        # Content lines
        chomp;
        my $prefix = '';
        my @a = split(':', $_);
        if ($a[1] eq 'file' || $a[1] eq 'internal') {
            # Convert the file name
            my $id;
            ($a[2], $id) = convertFileName($a[2]);
            if ($a[2] eq $JAVA_ONLY) {
                $prefix = '// Java only: ';
            }
            # Delete the encoding field
            splice(@a, 3, 1);
        } elsif ($a[1] eq 'alias') {
            # Pad out with extra blank fields to make the
            # 2-d array square
            push @a, "";
        } else {
            die "Can't parse $_";
        }
        print C_INDEX
            $prefix, "{ ",
            join(", ", map("\"$_\"", @a)),
            " },\n";
    }

    print C_INDEX <<END;
  }
}
END

    close(C_INDEX);
    close(JAVA_INDEX);
    print STDERR "$JAVA_INDEX -> $C_INDEX\n";
}

######################################################################
# Output a header
# Param: Filehandle
sub header {
    my $out = shift;
    my $in = shift;
    print $out $HEADER1;
    print $out "// Tool: $TOOL\n// Source: $in\n";
    print $out "// Date: ", scalar localtime, "\n";
    print $out $HEADER2;
    print $out "\n";
}

######################################################################
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

    header(\*OUT, $IN);
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

######################################################################
sub hideEscapes {
    # Transform escaped characters
    s|\\u([a-zA-Z0-9]{4})|<<u$1>>|g; # Transform Unicode escapes
    s|\\\"|<<dq>>|; # Transform backslash double quote
    s|\\(.)|<<q$1>>|; # Transform backslash escapes
}

######################################################################
sub restoreEscapes {
    # Restore escaped characters
    s|<<dq>>|\\\"|g;
    s|<<q(.)>>|\\$1|g;
    s|<<u0000>>|\\\\u0000|g; # Double escape U+0000
    s|<<u(....)>>|\\u$1|g;
}

__END__
:endofperl
