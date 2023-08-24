#/**
# *******************************************************************************
# * Copyright (C) 2016 and later: Unicode, Inc. and others.                     *
# * License & terms of use: http://www.unicode.org/copyright.html       *
# *******************************************************************************
# *******************************************************************************
# * Copyright (C) 2000-2004, International Business Machines Corporation and    *
# * others. All Rights Reserved.                                                *
# *******************************************************************************
# */

#!perl

# Usage - $0 <remap file>
#  e.g. - indic indic.txt
# The input file should be a subset of the Unicode data file containing
# the blocks of interest.
#
# The remap file should have lines of the form
# "\u0D01>\u0D02;"
# including the quotes.  These will be interpreted as saying that the
# undefined code point U+D01 (derived via mapping from InterIndic)
# can be remapped to U+D02.
# 
# The purpose of this script is to process the Indic script data into
# a form usable by the IndicTransliterator, that is, the Indic-Indic
# transliterator.  The transliterator needs two things: A mapping of
# the code points in common, and a list of the exceptions.

# Assume we are located in icu4j/src/com/ibm/tools/translit/.
# We want the Unicode DB in icu4j/src/data/unicode/.
$UNICODE_DB = "../../../../data/unicode/UnicodeData.txt";
$EXCEPTIONS_FILE = shift;

# Assume we are located in icu4j/src/com/ibm/tools/translit/.
# We want to output files to icu4j/src/com/ibm/text/resources/.
# Output directory
$OUTDIR = "../../text/resources";

# The template file should contain java code that can be used
# to generate RuleBasedTransliterator resource files.  The template
# should contain the following embedded symbols, which this script
# will replace:
# $TOOL - name of generating tool
# $DATE - date of generation
# $SCRIPTFROM - name of source script
# $SCRIPTTO - name of target script
# $RULES - rules
$RBT_TEMPLATE = 'rbtTemplate.txt';

# Name of this tool in generated RBT files
$RBT_GEN_TOOL = 'icu4j/src/com/ibm/tools/translit/indic.pl';

$DUMP = 0; # If 1, dump out internal data

$DO_HEURISTIC_REMAP = 0; # If 1, do automatic heuristic remapping
$DO_DECOMP_REMAP = 0; # If 1, do decomp remapping

open(UNICODE_DB);
while (<UNICODE_DB>) {
    next if (m|^0[0-8]|); # Skip up to Devanagari block (0900)
    last if (m|^0D[8-F]|i); # Bail out after Malayam block (0D00)
    # 0D39;MALAYALAM LETTER HA;Lo;0;L;;;;;N;;;;;
    my @data = split(/;/);
    my $fullCode = hex($data[0]); # e.g., 0x093F
    my $code = $fullCode & 0x7F; # e.g., 0x3F
    my ($script, $name) = ($data[1] =~ /(\w+)\s+(.+)/);
    die "Can't parse $_" unless ($name);
    # e.g., $code/$script/$name = 3F/MALAYALAM/VOWEL SIGN I

    # Titlecase the script
    $script = ucfirst(lc($script));

    # Fix a couple inconsistencies in the 3.0 data
    # REVISIT: Is this okay to do?
    if ($DO_HEURISTIC_REMAP) {
        if ($script eq 'Gujarati' && $code >= 5 && $code <= 0x14) {
            $name =~ s/^VOWEL/LETTER/;
        }
    }

    # Keep track of all script names we encounter.  We also note the
    # base of the block.
    my $base = $fullCode & ~0x7F; # e.g., 0x900;
    if (exists $SCRIPT_TO_BASE{$script}) {
        die "Script base mismatch for $script: $base vs. $SCRIPT_TO_BASE{$script}"
            if ($SCRIPT_TO_BASE{$script} ne $base);
    } else {
        $SCRIPT_TO_BASE{$script} = $base;
    }

    # Build up a mapping by name.  For each name, keep a hash keyed by
    # code point.  For each code point, keep an array of script names.
    # Also keep a total use count for each name.
    push @{$NAME_CODE_TO_SCRIPTS{$name}{$code}}, $script;
    ++$NAME_CODE_TO_SCRIPTS{$name}{count};

    # Build a map that looks like this:
    # $SCRIPT_NAME_TO_CODE{<script>}{<name>} = <code>
    # or undef if there is no mapping.
    $SCRIPT_NAME_TO_CODE{$script}{$name} = $code;

    # Build a map that looks like this:
    $SCRIPT_CODE_TO_NAME{$script}{$code} = $name;

    # And a map from the fullCode point to the name
    $FULLCODE_TO_NAME{$fullCode} = $name;

    # Map code (0..7F) to name.  This is usually a 1-1 mapping, but
    # is 1-n in a few cases.
    if (exists $CODE_TO_NAME{$code}) {
        if ($name ne $CODE_TO_NAME{$code}) {
            # For multiple names on a code offset, use the format
            # (a/b), (a/b/c), etc.
            local $_ = $CODE_TO_NAME{$code};
            if (m|^\(|) {
                if (!m|[\(\)/]$name[\(\)/]|) {
                    s|\)$|/$name\)|;
                }
            } else {
                $_ = "($_/$name)";
            }
            $CODE_TO_NAME{$code} = $_;
        }
    } else {
        $CODE_TO_NAME{$code} = $name;
    }
}
close(UNICODE_DB);

# Read and parse the manual remapping file.  This contains lines
# of the form:

# |"\u0956>\u0948;"  // AI Length Mark -> Devanagari Vowel Sign AI 

# The left hand side contains a non-existent full code value.  It
# should be a single value.  The right hand side contains one or more
# real full code values.  The idea is that when a mapping from another
# script ends up at the non-existent code point on the left, the
# sequence on the right should be substituted.  In this example,
# Devanagari has no AI Length Mark.  So, if transliterating from
# Oriya, then the character 0B56 (Oriya AI Length Mark) will remap to
# the non-existent 0956, and that remaps to 0948, our chosen
# Devanagari equivalent.  For our purposes, the left hand side should
# be taken to mean its equivalent point in the InterIndic range.  In
# this example, what it really says is E056>0948 in the
# InterIndic-Devanagari transliterator.

if ($EXCEPTIONS_FILE) {
    open(EXCEPTIONS_FILE) or die;
    while (<EXCEPTIONS_FILE>) {
        if (m|^\s*\"([^\"]*?)\"|) {
            my $line = $_;
            $_ = $1;
            if (/^(.*)>(.*);$/) {
                my ($rawFrom, $rawTo) = ($1, $2);
                my @from = parseUnicodeEscape($rawFrom);
                my @to = parseUnicodeEscape($rawTo);
                my $from = hexArray(@from);
                # Some entries look like this:
                # |"\u0955>\u0955;"
                # these do nothing; ignore them.
                if (intArraysEqual(\@from, \@to)) {
                    #print STDERR "Ignoring NOOP remap of $from\n";
                } elsif (exists $EXCEPTIONS{$from}) {
                    print STDERR "ERROR in $EXCEPTIONS_FILE - Duplicate remap entries for $from\n";
                } elsif (scalar @from > 1) {
                    print STDERR "ERROR in $EXCEPTIONS_FILE - Ignoring multichar remap: ", hexArray(@from), "->", hexArray(@to), "\n";                    
                } else {
                    # Check this for validity.  Full code on the left
                    # should NOT exist.  Full code seq on the right should.
                    if (exists $FULLCODE_TO_NAME{$from[0]}) {
                        print STDERR "ERROR in $EXCEPTIONS_FILE - Invalid remap; left side defined: ", hexArray(@from), "->", hexArray(@to), "\n";
                    } elsif (grep(! exists $FULLCODE_TO_NAME{$_}, @to)) {
                        print STDERR "ERROR in $EXCEPTIONS_FILE - Invalid remap; right side undefined: ", hexArray(@from), "->", hexArray(@to), "\n";
                    } else {
                        $EXCEPTIONS{$from[0]} = \@to;
                    }
                }
            } else { die "ERROR in $EXCEPTIONS_FILE - Can't parse \"$_\" in line $line"; }
        }
    }
    close(EXCEPTIONS_FILE);
    print STDERR "$EXCEPTIONS_FILE: Loaded ", scalar keys %EXCEPTIONS, " remappings\n";
}

if ($DO_DECOMP_REMAP) {
    # Read the NamesList.txt file.  This contains decomposition data.
    # Gather these into %DECOMP, which maps a name to n1.n2..., where n1
    # etc. are decomposed names.  E.g. $DECOMP{'LETTER RRA'} -> 'LETTER
    # RA.SIGN NUKTA'.  There may be different mappings in different script
    # blocks (LETTER RRA is mapped differently in Devanagari and Bengali),
    # in which case the name goes into %DECOMP_MISMATCH, and is removed
    # from %DECOMP.
    $NAMES = "NamesList.txt";
    open(NAMES);
    while (<NAMES>) {
        # Skip to start of DEVANAGARI block
        last if (/^\@\@\s+0900/);
    }
    while (<NAMES>) {
        # Continue until start of SINHALA block
        last if (/^\@\@\s+0D80/);
        if (/^([0-9A-Z]{4})/i) {
            $code = $1;
        } elsif (/^\s+:\s*(.+)/) {
            # We've found a mapping of the form:
            # 0929    DEVANAGARI LETTER NNNA
            #     * for transcribing Dravidian alveolar n
            #     : 0928 093C
            my $from = $FULLCODE_TO_NAME{hex($code)};
            my @to = map($FULLCODE_TO_NAME{hex($_)}, split(/\s+/, $1));
            if (exists $DECOMP{$from}) {
                my $aref = $DECOMP{$from};
                if (join(".", @$aref) ne join(".", @to)) {
                    print STDERR "ERROR: Decomp mismatch for $from\n";
                    print STDERR "     : $from = ", join(".", @$aref), "\n";
                    print STDERR "     : $from = ", join(".", @to), "\n";
                    $DECOMP_MISMATCH{$from} = 1;
                }
            } else {
                $DECOMP{$from} = \@to;
            }
        }
    }
    close(NAMES);
    # Remove mismatches
    foreach (keys %DECOMP_MISMATCH) {
        delete $DECOMP{$_};
    }
    if ($DUMP) {
        foreach (keys %DECOMP) {
            print "$_ = ", join(" + ", @{$DECOMP{$_}}), "\n";
        }
    }
}

# Count the total number of scripts

$SCRIPT_COUNT = scalar keys %SCRIPT_TO_BASE;
#print join("\n", sort keys %SCRIPT_TO_BASE), "\n";

# Dump out the %NAME_CODE_TO_SCRIPTS map.

if ($DUMP) {
    print "\nBY NAME:\n";
    foreach my $pass ((1, 2)) {
        print "\nBY NAME - SINGLETONS:\n" if ($pass eq 2);
        foreach my $name (sort keys %NAME_CODE_TO_SCRIPTS) {
            if ($pass eq 1) {
                next if (1 >= $NAME_CODE_TO_SCRIPTS{$name}{count});
            } else {
                next if (1 < $NAME_CODE_TO_SCRIPTS{$name}{count});
            }
            print "$name:";
            my $href = $NAME_CODE_TO_SCRIPTS{$name};
            foreach my $code (sort {$a <=> $b} keys %$href) {
                next if ($code eq 'count');
                my $aref = $href->{$code};
                print " ", hex2($code), " (", formatScriptList($aref), ")";
            }
            print "\n";
        }
    }
}

# Create some transliterators, based on the scripts and the %NAME_CODE_TO_SCRIPTS
# map.  Only use %NAME_CODE_TO_SCRIPTS entries with a count of 2 or more, that is,
# names that occur in two or more scripts.  For those scripts where
# the names occur, map both up to the InterIndic range, and down to
# the target script.

$INTERINDIC = 0xE000;
$INTERINDIC_EXTRA = 0xE080;
$INTERINDIC_EXTRA_NEXT = $INTERINDIC_EXTRA;

# For each script, create a hash.  The hash has a key for each
# code point, either within its block, or in the InterIndic block.
# the value of the key is the mapping.

# The script hashes are named %DEVANAGARI, etc., and referenced
# with symbolic refs.

@REMAP = ('s/\bSHORT\s+//i',
          's/\bCANDRA\s+//i',
          's/\bQA$/KA/i',
          's/\bKHHA$/KHA/i',
          's/\bGHHA$/GA/i',
          's/\bZA$/JA/i',
          's/\bFA$/PHA/i',
          's/\bVA$/BA/i',
          's/\bNNNA$/NA/i',
          's/\bRRA$/RA/i',
          's/\bLLLA$/LLA/i',
          's/\bLLLA$/LA/i',
          's/\bLLA$/LA/i',
          's/^A(.) LENGTH MARK$/VOWEL SIGN A$1/i',
          's/CANDRABINDU/BINDI/i',
          's/BINDI/CANDRABINDU/i',
          );

# Do this so we see zero counts:
foreach my $remap (@REMAP) { $REMAP{$remap} = 0; }

# This loop iterates over the names in the NAME_CODE_TO_SCRIPTS hash.
# These names are things like "LETTER NNNA".  For each name, it then
# creates script mappings up to the InterIndic area, and back down
# to the script areas.  If a name maps to more than one offset,
# then it uses the InterIndic extra range.  Either way, it picks
# a single InterIndic point, either an offset point or something in
# the extra range, and maps up and down from that point.
foreach my $name (sort keys %NAME_CODE_TO_SCRIPTS) {
    next if (1 >= $NAME_CODE_TO_SCRIPTS{$name}{count});
    my $href = $NAME_CODE_TO_SCRIPTS{$name};
    # Count the number of different codes assigned to this name.
    # Usually 1, but 2 for a handful of names.
    my $codeCount = (keys %{$NAME_CODE_TO_SCRIPTS{$name}}) - 1; # less 1: {count}
    # If $codeCount is 1, then map directly up to the $INTERINDIC
    # base.  If $codeCount is 2, then map into unused spots starting
    # at $INTERINDIC_EXTRA.
    my $interIndicCode;
    if ($codeCount > 1) {
        # Map into the InterIndic extra range
        $interIndicCode = $INTERINDIC_EXTRA_NEXT++;
    }
    my %seen;
    foreach my $code (sort {$a ne 'count' && $b ne 'count' && $a <=> $b} keys %$href) {
        next if ($code eq 'count');
        my $aref = $href->{$code}; # Ref to array of scripts
        if ($codeCount == 1) {
            # Map directly
            $interIndicCode = $INTERINDIC + $code;
        }
        # Keep track of the names of the extra InterIndic points
        $INTERINDIC_NAME_TO_FULLCODE{$name} = $interIndicCode;

        foreach my $scr (@$aref) {
            $seen{$scr} = 1;
            my $fullCode = $SCRIPT_TO_BASE{$scr} + $code;
            $ {$scr}{$fullCode} = hex4($interIndicCode) . "; // $name";
            $ {$scr}{$interIndicCode} = hex4($fullCode) . "; // $name";
        }
    }
    # Now handle InterIndic->Script unmapped points.  For each name,
    # some of the scripts will be left out -- will have no mappings
    # to that name.  For these scripts, we can either leave them
    # unmapped (so the InterIndic->Local mapping is empty), or
    # try to remap.
 unmappedScript:
    foreach my $scr (keys %SCRIPT_TO_BASE) {
        next if ($seen{$scr});

        if ($DO_HEURISTIC_REMAP) {
            # Try to remap through the known equivalences in our
            # remapping table
            foreach my $remapRE (@REMAP) {
                local $_ = $name;
                if (eval($remapRE)) {
                    if (exists $SCRIPT_NAME_TO_CODE{$scr}{$_}) {
                        $ {$scr}{$interIndicCode} =
                            hex4($SCRIPT_TO_BASE{$scr} + $SCRIPT_NAME_TO_CODE{$scr}{$_}) .
                                "; // REMAP: $name -> $_";
                        ++$REMAP{$remapRE};
                        next unmappedScript;
                    }
                }
            }
        }

        # Try to remap through the file.  This contains remappings of
        # the form 0991->0993.  That is, it contains local remappings
        # that we can substitute and try again with.
        #|GURMUKHI-InterIndic ------------------------------
        #|// 0A02>; // UNMAPPED INTERNAL: SIGN BINDI
        #|InterIndic-GURMUKHI ------------------------------
        #|// E001>; // UNMAPPED EXTERNAL: SIGN CANDRABINDU
        #|"\u0A01>\u0A02;"
        # In this example, the remapping tells us that the non-existent
        # character A01 should be considered equivalent to the real
        # character A02.
        # We implement this by adding two mappings; one from
        # the InterIndic equivalent of A01, that is, E001, to A02,
        # and one from A02, which otherwise has no mapping, to E001.
        if ($EXCEPTIONS_FILE && $interIndicCode < $INTERINDIC_EXTRA) {
            # Try to map this InterIndic character back to a the spot
            # it would occupy in this script if it had a mapping.
            my $code = $interIndicCode & 0x7F;
            my $pseudoFullCode = $SCRIPT_TO_BASE{$scr} + $code;
            if (exists $EXCEPTIONS{$pseudoFullCode}) {
                my $fullCodeArray = $EXCEPTIONS{$pseudoFullCode};
                my $comment;
                foreach my $c (@$fullCodeArray) {
                    $comment .= "." if ($comment);
                    $comment .= $FULLCODE_TO_NAME{$c};
                }
                $comment = "; // REMAP ($EXCEPTIONS_FILE): " .
                    hex4($pseudoFullCode) . ">" . hexArray(@$fullCodeArray) . " = " .
                    $CODE_TO_NAME{$code} . ">" . $comment;
                $ {$scr}{$interIndicCode} = hexArray(@$fullCodeArray) . $comment;
                if (scalar @$fullCodeArray == 1) {
                    if (exists $ {$scr}{$fullCodeArray->[0]}) {
                        # There's already a proper mapping; no need to fill
                        # in reverse
                    } else {
                        $ {$scr}{$fullCodeArray->[0]} = hex4($interIndicCode) . $comment;
                    }
                }
                next unmappedScript;
            }
        }

        $SCRIPT_FULLCODE_TO_IS_UNMAPPED{$scr}{$interIndicCode} = 1;
        local $_ = "; // UNMAPPED InterIndic-$scr: $name";
        if (exists $SCRIPT_CODE_TO_NAME{$scr}{$interIndicCode & 0x7F}) {
            my $fullCode = $SCRIPT_TO_BASE{$scr} + ($interIndicCode & 0x7F);
            $_ .= " (" . hex4($fullCode) . " = " . $FULLCODE_TO_NAME{$fullCode} . ")";
        }
        $ {$scr}{$interIndicCode} = $_;
    }
}

# Add in unmapped entries for each script
foreach my $scr (keys %SCRIPT_TO_BASE) {
    my $base = $SCRIPT_TO_BASE{$scr};
 unmappedInt:
    foreach my $code (keys %{$SCRIPT_CODE_TO_NAME{$scr}}) {
        my $fullCode = $code + $base;
        next if (exists $ {$scr}{$fullCode});
        my $name = $SCRIPT_CODE_TO_NAME{$scr}{$code};

        if ($DO_HEURISTIC_REMAP) {
            foreach my $remapRE (@REMAP) {
                local $_ = $name;
                if (eval($remapRE)) {
                    if (exists $INTERINDIC_NAME_TO_FULLCODE{$_}) {
                        $ {$scr}{$fullCode} =
                            hex4($INTERINDIC_NAME_TO_FULLCODE{$_}) .
                                "; // REMAP: $name -> $_";
                        ++$REMAP{$remapRE};
                        next unmappedInt;
                    }
                }
            }
        }

        # Now try the decomp table
        if ($DO_DECOMP_REMAP && exists $DECOMP{$name}) {
            my $x;
            my $cmt = "; // DECOMP: $name -> ";
            foreach my $n (@{$DECOMP{$name}}) {
                if (exists $SCRIPT_NAME_TO_CODE{$scr}{$n}) {
                    $x .= hex4($SCRIPT_TO_BASE{$scr} + $SCRIPT_NAME_TO_CODE{$scr}{$n});
                    $cmt .= $n . " + ";
                } else {
                    $cmt = 0;
                    last;
                }
            }
            if ($cmt) {
                $ {$scr}{$fullCode} = $x . $cmt;
                next unmappedInt;
            }
        }

        $SCRIPT_FULLCODE_TO_IS_UNMAPPED{$scr}{$fullCode} = 1;
        $ {$scr}{$fullCode} = "; // UNMAPPED $scr-InterIndic: $name";
    }
}

# GUR
# E00B>; // UNMAPPED EXTERNAL: LETTER VOCALIC R "\u0A0B>\u0A30\u0A3F;"
# E00C>; // UNMAPPED EXTERNAL: LETTER VOCALIC L "\u0A0C>\u0A07;"
# E00D>; // UNMAPPED EXTERNAL: LETTER CANDRA E "\u0A0D>\u0A10;"
# E011>; // UNMAPPED EXTERNAL: LETTER CANDRA O "\u0A11>\u0A14;"
# E037>; // UNMAPPED EXTERNAL: LETTER SSA "\u0A37>\u0A36;"
# E045>; // UNMAPPED EXTERNAL: VOWEL SIGN CANDRA E "\u0A45>\u0A48;"
# E049>; // UNMAPPED EXTERNAL: VOWEL SIGN CANDRA O "\u0A49>\u0A4C;"
# Fix QA too

# Dump out script maps
foreach my $scr (sort keys %SCRIPT_TO_BASE) {
    ## next unless ($scr eq 'TELUGU'); # Debugging
    my @rules;
    my $flag = 1;
    foreach my $fullCode (sort {$a <=> $b} keys %{$scr}) {
        if ($flag && $fullCode >= $INTERINDIC) {
            # We have the complete <scr>-InterIndic rules; dump
            # them out.
            generateRBT($scr, "InterIndic", \@rules, $OUTDIR);
            @rules = ();
            $flag = 0;
        }
        if (exists $SCRIPT_FULLCODE_TO_IS_UNMAPPED{$scr}{$fullCode}) {
            push @rules, "// " . hex4($fullCode) . ">" . $ {$scr}{$fullCode};
        } else {
            push @rules, hex4($fullCode) . ">" . $ {$scr}{$fullCode};
        }
    }
    # Now generate the InterIndic-<scr> rules.
    generateRBT("InterIndic", $scr, \@rules, $OUTDIR);

#    print "$scr-InterIndic ------------------------------\n";
#    my $flag = 1;
#    foreach my $fullCode (sort {$a <=> $b} keys %{$scr}) {
#        if ($flag && $fullCode >= $INTERINDIC) {
#            print "InterIndic-$scr ------------------------------\n";
#            $flag = 0;
#        }
#        if (exists $SCRIPT_FULLCODE_TO_IS_UNMAPPED{$scr}{$fullCode}) {
#            print "// ", hex4($fullCode), ">", $ {$scr}{$fullCode}, "\n";
#        } else {
#            print hex4($fullCode), ">", $ {$scr}{$fullCode}, "\n";
#        }
#    }
}

# List successful remappings
if ($DO_HEURISTIC_REMAP) {
    foreach my $remap (sort keys %REMAP) {
        print STDERR "REMAP ", $REMAP{$remap}, " x $remap\n";
    }
}

#----------------------------------------------------------------------
# SUBROUTINES

# Return a listing of an array of scripts
# Param: array ref
sub formatScriptList {
    my $aref = shift;
    if ($SCRIPT_COUNT == @$aref) {
        return "all";
    } elsif (($SCRIPT_COUNT - 3) <= @$aref) {
        my $s = "all but";
        my %temp;
        foreach (@$aref) { $temp{$_} = 1; }
        foreach (sort keys %SCRIPT_TO_BASE) {
            $s .= " $_" unless exists $temp{$_};
        }
        return $s;
    } else {
        return join(" ", @$aref);
    }
}

# Format as %02X hex
sub hex2 {
    sprintf("%02X", $_[0]);
}

# Format as %04X hex
sub hex4 {
    sprintf("\\u%04X", $_[0]);
}

# Format an array as %04X hex, delimited by "."s
sub hexArray {
    join("", map { hex4($_); } @_);
}

# Parse a string of the form "\u0D01" to an array of integers.
# Must ONLY contain escapes.
# Return the array.
sub parseUnicodeEscape {
    local $_ = shift;
    my $orig = $_;
    my @result;
    while (length($_)) {
        if (/^\\u([0-9a-f]{4})(.*)/i) {
            push @result, hex($1);
            $_ = $2;
        } else {
            die "Can't parse Unicode escape $orig\n";
        }
    }
    if (0 == @result) {
        die "Can't parse Unicode escape $orig\n";        
    }
    @result;
}

# Return 1 if the two arrays of ints are equal.
# Param: ref to array of ints
# Param: ref to array of ints
sub intArraysEqual {
    my $a = shift;
    my $b = shift;
    if (scalar @$a == scalar @$b) {
        for (my $i=0; $i<@$a; ++$i) {
            if ($a->[$i] != $b->[$i]) {
                return 0;
            }
        }
        return 1;
    }
    return 0;
}

# Given a rule, possibly with trailing // comment,
# quote the rule part and add a trailing "+" after
# it.
sub quoteRule {
    my $cmt;
    $cmt = $1 if (s|(\s*//.*)||); # isolate trailing // comment
    s/^(.*;)/\"$1\"+/;
    s/$/$cmt/;
    $_;
}

# Given the name of the source script, name of the target script,
# and array of rule strings, return a string containing the source
# for a RuleBasedTransliterator file.
# Param: source script name
# Param: target script name
# Param: ref to array of rules.  These rules are unquoted, without
#  concatenators between them, but do have trailing ';' separators.
# Param: name of output directory
sub generateRBT {
    # $TOOL - name of generating tool
    # $DATE - date of generation
    # $SCRIPTFROM - name of source script
    # $SCRIPTTO - name of target script
    # $RULES - rules
    my ($source, $target, $rules, $outdir) = @_;
    my $text;
    $outdir =~ s|[/\\]$||; # Delete trailing / or \
    my $OUT = "$outdir/TransliterationRule_${source}_$target.java";
    open(RBT_TEMPLATE) or die;
    open(OUT, ">$OUT") or die;
    while (<RBT_TEMPLATE>) {
        while (/\$([A-Za-z0-9]+)/) {
            my $tag = $1;
            my $sub;
            if ($tag eq 'TOOL') {
                $sub = $RBT_GEN_TOOL;
            } elsif ($tag eq 'DATE') {
                $sub = localtime;
            } elsif ($tag eq 'SCRIPTFROM') {
                $sub = $source;
            } elsif ($tag eq 'SCRIPTTO') {
                $sub = $target;
            } elsif ($tag eq 'RULES') {
                # Get any whitespace-only indent off the front of this tag
                my $indent;
                $indent = $1 if (/^(\s+)\$$tag/);

                # The rules in the array are not quoted.  We need to quote
                # them and add '+' operators between them.  We do NOT need
                # to add ';' separators.  We DO need to separate trailing
                # // comments and handle them.
                $sub = join("\n$indent", map(&quoteRule, @$rules)) .
                    "\n$indent\"\"";
            } else {
                print STDERR "ERROR in $RBT_TEMPLATE: Unknown tag $tag\n";
                $sub = "[ERROR:Unknown tag \$$tag]";
            }
            s/\$$tag/$sub/;
        }
        print OUT;
    }
    close(OUT);
    close(RBT_TEMPLATE);
    print STDERR "Written: $OUT\n";
}

__END__
