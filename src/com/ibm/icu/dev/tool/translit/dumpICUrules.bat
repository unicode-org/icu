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
# below) and created in the current directory.	They should be manually
# checked and then copied into the icu/data directory.  An ICU build must
# then be initiated, and the standard suite of ICU transliterator tests
# should be run after that.
#
# Alan Liu 5/19/00 2/27/01

use Getopt::Long;

my $DIR = "../../../impl/data";
my $ID = '';

GetOptions('dir=s' => \$DIR,
           'id=s' => \$ID,
           '<>' => \&usage) || die;

usage() if (@ARGV);

$ID =~ s/-/_/;
if (! -d $DIR) {
    print STDERR "$DIR is not a directory\n";
    usage();
}

sub usage {
    my $me = $0;
    $me =~ s|.+[/\\]||;
    print "Usage: $me [-dir <dir>] [-id <id>]\n";
    print " --dir <dir> Specify the directory containing the\n";
    print " 	Transliterator_*.txt files\n";
    print " --id <id>	Specify a single ID to transform, e.g.\n";
    print " 	Fullwidth-Halfwidth\n";
    die;
}

$JAVA_ONLY = '-';

$OUTDIR = "icu4c";
mkdir($OUTDIR,0777);

# Mapping from Java file names to ICU file names
%NAME_MAP = (
	 # An ICU name of "" means the ICU name == the ID

	 "Any_Accents" => "",
	 "Any_Publishing" => "",
	 "Bengali_InterIndic" => "Beng_InterIndic",
	 "Cyrillic_Latin" => "Cyrl_Latn",
	 "Devanagari_InterIndic" => "Deva_InterIndic",
	 "Fullwidth_Halfwidth" => "FWidth_HWidth",
	 "Greek_Latin" => "Grek_Latn",
	 "Gujarati_InterIndic" => "Gujr_InterIndic",
	 "Gurmukhi_InterIndic" => "Guru_InterIndic",
	 "Hiragana_Katakana" => "Hira_Kana",
	 "Hiragana_Latin" => "Hira_Latn",
	 "InterIndic_Bengali" => "InterIndic_Beng",
	 "InterIndic_Devanagari" => "InterIndic_Deva",
	 "InterIndic_Gujarati" => "InterIndic_Gujr",
	 "InterIndic_Gurmukhi" => "InterIndic_Guru",
	 "InterIndic_Kannada" => "InterIndic_Knda",
	 "InterIndic_Latin" => "InterIndic_Latn",
	 "InterIndic_Malayalam" => "InterIndic_Mlym",
	 "InterIndic_Oriya" => "InterIndic_Orya",
	 "InterIndic_Tamil" => "InterIndic_Taml",
	 "InterIndic_Telugu" => "InterIndic_Telu",
	 "Kannada_InterIndic" => "Knda_InterIndic",
	 "Latin_InterIndic" => "Latn_InterIndic",
	 "Latin_Jamo" => "Latn_Jamo",
	 "Latin_Katakana" => "Latn_Kana",
	 "Malayalam_InterIndic" => "Mlym_InterIndic",
	 "Oriya_InterIndic" => "Orya_InterIndic",
	 "Tamil_InterIndic" => "Taml_InterIndic",
	 "Telugu_InterIndic" => "Telu_InterIndic",
	 
	 "Han_Pinyin" => $JAVA_ONLY,
	 "Kanji_English" => $JAVA_ONLY,
	 "Kanji_OnRomaji" => $JAVA_ONLY,
	 );

# Header blocks of text written at start of ICU output files
$HEADER1 = <<END;
//--------------------------------------------------------------------
// Copyright (c) 1999-2004, International Business Machines
// Corporation and others.  All Rights Reserved.
//--------------------------------------------------------------------
// THIS IS A MACHINE-GENERATED FILE
END
$HEADER2 = <<END;
//--------------------------------------------------------------------
END

$TOOL = $0;

# Iterate over all Java RBT rule files
foreach (<$DIR/Transliterator_*.txt>) {
    next if (/~$/);
    next if (/_index\.txt$/);
    next if ($ID && !/$ID/);
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
    if (m|Transliterator_(.+)\.utf8\.txt$| ||
        m|Transliterator_(.+)\.txt$|) {
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
    if ($out ne $JAVA_ONLY) {
        $out = 't_' . $out;
    }
    return ($out, $id);
}

######################################################################
# Convert the index file from Java to C format
sub convertIndex {
    $JAVA_INDEX = "Transliterator_index.txt";
    $C_INDEX = "translit_index.txt";
    open(JAVA_INDEX, "$DIR/$JAVA_INDEX") or die;
    open(C_INDEX, ">$OUTDIR/$C_INDEX") or die;
    
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
        # ignore $Source $Revision $Date CVS keyword substitutions
        next if /\$Source/ ;
        next if /\$Revision/ ;
        next if /\$Date/ ;

        # we have printed out the copyright info ... ignore one in Java version
        next if /Copyright/ ;
        next if /Corporation/;

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
        #replace \p with \\p
        $_=~ s/\\p/\\\\p/g;
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

    # Open file, write UTF8 marker, close it, and reopen in text mode
    open(OUT, ">$OUTDIR/$OUT") or die;
    binmode OUT; # Must do this so we can write our UTF8 marker
    print OUT pack("C3", 0xEF, 0xBB, 0xBF); # Write UTF8 marker
    close(OUT);

    open(OUT, ">>$OUTDIR/$OUT") or die;
    print OUT " // -*- Coding: utf-8; -*-\n";

    header(\*OUT, $IN);
    print OUT "// $id\n";
    print OUT "\n";
    print OUT "$out {\n";
    print OUT "  Rule {\n";

    open(IN, $IN) or die;
    binmode IN; # IN is a UTF8 file

    my $first = 1;
    my $BOM = pack("C3", 239, 187, 191); # a UTF8 byte order mark

    # Process each line by changing # comments to // comments
    # and taking other text and enclosing it in double quotes
    while (<IN>) {
        my $raw = $_;
		# ignore $Source $Revision $Date CVS keyword substitutions
		next if /\$Source/ ;
		next if /\$Revision/ ;
		next if /\$Date/ ;
		
		# we have printed out the copyright info ... ignore one in Java version
		next if /Copyright/ ;
		next if /Corporation/;
		
        # Look for and delete BOM
        if ($first) {
	s/^$BOM//;
	$first = 0;
        }
        
        # Clean the eol junk up
        s/[\x0D\x0A]+$//;

        # If there is a trailing backslash, then delete it -- we don't
        # need line continuation in C, since adjacent strings are
        # concatenated.  Count trailing backslashes; if they are odd,
        # one is trailing.
        if (m|(\\+)$|) {
	if ((length($1) % 2) == 1) {
	    s|\\$||;
	}
        }

        # Transform escaped characters
        hideEscapes();

        if (/^(\s*)(\#.*)$/) {
	# Comment-only line
	my ($white, $cmt) = ($1, $2);
	$cmt =~ s|\#|//|;
	$_ = $white . $cmt;

        } elsif (!/\S/) {
	# Blank line -- leave as-is

        } else {
	# Remove single-quoted matter 
	my @quotes;
	my $nquotes = 0;
	my $x = $_;
	while (s/^([^\']*)(\'[^\']*\')/$1<<x$nquotes>>/) {
	    push @quotes, $2;
	    ++$nquotes;
	}

	# Extract comment
	my $cmt = '';
	if (s|\#(.*)||) {
	    $cmt = '//' . $1;
	}
	
	# Add quotes
	s|^(\s*)(\S.*?)(\s*)$|$1\"$2\"$3|;

	# Restore single-quoted matter
	for (my $i=0; $i<$nquotes; ++$i) {
	    s|<<x$i>>|$quotes[$i]|;
	}

	# Restore comment
	$_ .= $cmt;
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
    print -s "$OUTDIR/$OUT", ")\n";
}

######################################################################
sub hideEscapes {
    # Transform escaped characters
    s|\\\\|<<bs>>|g; # DO THIS FIRST Transform backslashes
    s|\\u([a-zA-Z0-9]{4})|<<u$1>>|g; # Transform Unicode escapes
    s|\\\"|<<dq>>|g; # Transform backslash double quote
    s|\\\'|<<sq>>|g; # Transform backslash single quote
    s|\\\#|<<lb>>|g; # Transform backslash pound
    s|\\(.)|<<q$1>>|g; # Transform backslash escapes
}

######################################################################
sub restoreEscapes {
    # Restore escaped characters
    s|<<bs>>|\\\\|g;
    s|<<dq>>|\\\\\\\"|g;
    s|<<sq>>|\\\\\\\'|g;
    s|<<lb>>|\\\\\\\#|g;
    s|<<q(.)>>|\\\\\\$1|g;
    s|<<u0000>>|\\\\u0000|g; # Double escape U+0000
    s|<<u(....)>>|\\u$1|g;
}

__END__
:endofperl
