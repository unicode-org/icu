#!/usr/bin/perl
######################################################################
# Copyright (C) 1999-2003, International Business Machines
# Corporation and others.  All Rights Reserved.
######################################################################
# See: ftp://elsie.nci.nih.gov/pub/tzdata<year>
# where <year> is "1999b" or a similar string.
######################################################################
# This script takes time zone data in elsie.nci.nih.gov format and
# parses it into a form usable by ICU.  The raw data contains more
# data than we need, since it contains historical zone data.  We
# parse out the current zones and create a listing of current zones.
# Author: Alan Liu
######################################################################
# This script reads an alias table, $TZ_ALIAS, and creates clones of
# standard UNIX zones with alias names.
######################################################################
# To update the zone data, download the latest data from the NIH URL
# listed above into a directory.  Run this script with the directory
# name as an argument.  THE DIRECTORY NAME MUST END IN tzdataYYYYR.
######################################################################
# OUTPUT FILE FORMAT (filename $OUT)
#
# As a matter of policy, this script wants to do as much of
# the parsing, data processing, and error checking as possible,
# leaving the C++ program that parses this file to just do the binary
# translation step.
#
# - The file is line based, with one record per line.
# - Lines may be followed by a comment; the parser must ignore
#   anything of the form /\s+#.*$/ in each line.
#   |3065,14400 # Asia/Dubai GMT+4:00
# - The file contains a header and 4 lists.
# - The header contains the version of this data file:
#    2 original version, without equivalency groups
#    3 current version, described here
#   then the version of the unix data, and other counts:
#   | 3 # format version number of this file
#   | 1999 # (tzdata1999j) version of Olson zone
#   | 10 #  data from ftp://elsie.nci.nih.gov
#   | 402 # total zone count
#   | 40 # maximum zones per offset (used by gentz)
# - Lists start with a count of the records to follow, the records
#   themselves (one per line), and a single line with the keyword
#   'end'.
# - The first list is the name table:
#   | 387 # count of names to follow
#   | 34,Africa/Abidjan
#   | 23,Africa/Accra
#   ...
#   | end
#   Each name is terminated by a newline (like all lines in the file).
#   The zone numbers in other lists refer to this table.  The
#   integer that precedes the name is an index into the equivalency
#   table, with the first table entry being entry 0.
# - The second list is the equivalency table.  It lists, in sorted
#   order, the equivalency groups.  Each group represents a
#   set of one or more zones that have the same GMT offset and the
#   same rules.  While there are about 400 zones, there are less than
#   120 equivalency groups (as of this writing).
#   | 120 # count of equivalency groups to follow
#   | s,0,1,0 # GMT+0:00
#   | d,0,8,1,0,0,w,11,31,0,0,w,20,4,15,16,17,18 # GMT+0:00 Sep 1...
#   ...
#   | end
#   Entries start with 's' for standard zones, or 'd' for DST zones.
#   Both zone descriptors start with the GMT offset in SECONDS.  DST
#   zones contain, in addition, data for the onset rule and the cease
#   rule.  Each rule is described by the following integers:
#     month (JAN = 0)
#     dowim } These two values are in SimpleTimeZone encoded
#     dow   } format for DOM, DOWIM, DOW>=DOM, or DOW<=DOM.
#     time MINUTES
#     time mode ('w', 's', 'u')
#   The last rule integer in the record is the DST savings in MINUTES,
#   typically 60.

#   After either a standard or a DST zone, there is a list of the
#   members of the equivalency group.  This consists of a number of
#   entries to follow (>=1), then the zone numbers themselves.
# - The third list is an index by GMT offset.  Each line lists the
#   zones with the same offset.  The first number on the line is the
#   GMT offset in seconds.  The second number is the default zone
#   number in the following list, taken from tz.default.  The list
#   consists of a number of entries to follow (>=1), then the zone
#   numbers themselves.
#   | 39 # index by offset entries to follow
#   | -43200,280,1,280 # -12:00 d=Etc/GMT+12 Etc/GMT+12
#   | -39600,374,6,279,366,374,394,396,399 # -11:00 d=Pacific/Apia Etc/GMT+11 MIT Pacific/Apia Pacific/Midway Pacific/Niue Pacific/Pago_Pago
#   ...
#   | end
# - The fourth list is an index by ISO 3166 country code.  Each line
#   lists a country and the zones mapped into that country by the
#   zone.tab file.  Zones not mapped into any file are listed on the
#   first line.  The first number on each line is the intcode for the
#   country code.  The intcode for 'US' for example is ('U'-'A') * 32
#   + ('S' - 'A') == 658.  The second number is the count of list
#   items, and the following number are the zone indices.
#   | 238 # index by country entries to follow
#   | 0,38,230,231,232,276,282,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,364,380,429,431 # (None) Asia/Riyadh87 Asia/Riyadh88 Asia/Riyadh89 CET EET Etc/GMT Etc/GMT+1 Etc/GMT+10 Etc/GMT+11 Etc/GMT+12 Etc/GMT+2 Etc/GMT+3 Etc/GMT+4 Etc/GMT+5 Etc/GMT+6 Etc/GMT+7 Etc/GMT+8 Etc/GMT+9 Etc/GMT-1 Etc/GMT-10 Etc/GMT-11 Etc/GMT-12 Etc/GMT-13 Etc/GMT-14 Etc/GMT-2 Etc/GMT-3 Etc/GMT-4 Etc/GMT-5 Etc/GMT-6 Etc/GMT-7 Etc/GMT-8 Etc/GMT-9 Etc/UCT Etc/UTC GMT MET UTC WET
#   | 3,1,314 # AD (Andorra) Europe/Andorra
#   | 4,1,199 # AE (United Arab Emirates) Asia/Dubai
#   | ...
#   | 822,2,28,275 # ZW (Zimbabwe) Africa/Harare CAT
#   | end
######################################################################
# As of 1999j, here are the various possible values taken by the
# rule fields.  See code below that generates this data.
# 
# at: 0:00, 0:00s, 1:00, 1:00s, 1:00u, 23:00s, 2:00, 2:00s, 2:30, 2:45s,
#     3:00, 3:00s
# in: Apr, Dec, Feb, Jan, Jun, Mar, May, Nov, Oct, Sep
# letter: -, D, GHST, GMT, HS, S, SLST
# on: 1, 12, 15, 18, 2, 20, 21, 22, 23, 25, 28, 3, 30, 31, 4, 7, Fri>=1,
#     Fri>=15, Sat>=1, Sat>=15, Sun<=14, Sun>=1, Sun>=10, Sun>=11, Sun>=15,
#     Sun>=16, Sun>=23, Sun>=8, Sun>=9, lastFri, lastSun, lastThu
# save: 0, 0:20, 0:30, 1:00
# type: -

require 5; # Minimum version of perl needed
use strict;
use Getopt::Long;
use vars qw(@FILES $YEAR $DATA_DIR $OUT $SEP @MONTH
            $VERSION_YEAR $VERSION_SUFFIX $RAW_VERSION
            $TZ_ALIAS $TZ_DEFAULT $URL $TXT_FILE $HTML_FILE $JAVA_FILE
            $TZ_TXT_VERSION %ZONE_ID_TO_INDEX $END_MARKER
            %COUNTRY_CODES);
require 'dumpvar.pl';
use tzparse;
use tzutil;

# Current version of the data file.  Matches formatVersion[0] in the
# binary data file.  SEE tzdat.h
# 1 - unreleased version (?)
# 2 - original version
# 3 - added equivalency groups
# 4 - added country code index
$TZ_TXT_VERSION = 4;

# File names
$TZ_ALIAS = 'tz.alias';
$TZ_DEFAULT = 'tz.default';

# Source of our data
$URL = "ftp://elsie.nci.nih.gov/pub";

# Separator between fields in the output file
$SEP = ','; # Don't use ':'!

# Marker between sections
$END_MARKER = 'end';

@FILES = qw(africa      
            antarctica  
            asia        
            australasia 
            backward    
            etcetera    
            europe      
            factory     
            northamerica
            pacificnew  
            solar87     
            solar88     
            solar89     
            southamerica);

# We get the current year from the system here.  Later
# we double check this against the zone data version.
$YEAR = 1900+@{[localtime]}[5]; # Get the current year

$DATA_DIR = shift;

if (!$DATA_DIR || ! -d $DATA_DIR) {
    print STDERR "No data directory or invalid directory specified\n\n";
    usage();
}

$TXT_FILE = '';
$HTML_FILE = '';
$JAVA_FILE = '';
while (@ARGV) {
    local $_ = shift;
    if (/\.java$/i) {
        if ($JAVA_FILE) {
            print STDERR "Error: Multiple java files specified\n";
            usage();
        }
        $JAVA_FILE = $_;
    } elsif (/\.html?$/i) {
        if ($HTML_FILE) {
            print STDERR "Error: Multiple html files specified\n";
            usage();
        }
        $HTML_FILE = $_;
    } elsif (/\.txt$/i) {
        if ($TXT_FILE) {
            print STDERR "Error: Multiple txt files specified\n";
            usage();
        }
        $TXT_FILE = $_;
    } else {
        print STDERR "Error: Unexpected command line parameter \"$_\"\n";
        usage();
    }
}

if (!($TXT_FILE || $JAVA_FILE || $HTML_FILE)) {
    print STDERR "Nothing to do!  Please specify one or more output files.\n";
    usage();
}

if ($DATA_DIR =~ /(tzdata(\d{4})(\w?))/) {
    $RAW_VERSION = $1;
    $VERSION_YEAR = $2;
    $VERSION_SUFFIX = $3;
    if ($YEAR != $VERSION_YEAR) {
        print STDERR "WARNING: You appear to be building $VERSION_YEAR data. Don't you want to use current $YEAR data?\n\n";
        #usage(); # Add an override option for this check, if needed
    }
    $VERSION_SUFFIX =~ tr/a-z/A-Z/;
    if ($VERSION_SUFFIX =~ /[A-Z]/) {
        $VERSION_SUFFIX = ord($VERSION_SUFFIX) - ord('A') + 1;
    } else {
        if ($VERSION_SUFFIX) {
            print STDERR "Warning: Ignoring version suffix '$VERSION_SUFFIX' for \"$DATA_DIR\"\n";
        }
        $VERSION_SUFFIX = 0;
    }
    print "Time zone version $RAW_VERSION = $VERSION_YEAR($VERSION_SUFFIX)\n";
} else {
    print STDERR "The directory specified doesn't contain \"tzdataNNNNR\", so I can't tell what version the data is.  Please rename the directory and try again.\n";
    usage();
}

@MONTH = qw(jan feb mar apr may jun
            jul aug sep oct nov dec);

main();
exit();

sub usage {
    print STDERR "Usage: $0 data_dir [txt_out] [html_out] [java_out]\n\n";
    print STDERR "  data_dir contains the unpacked files from\n";
    print STDERR "  $URL/tzdataYYYYR,\n";
    print STDERR "  where YYYY is the year and R is the revision\n";
    print STDERR "  letter.\n";
    print STDERR "\n";
    print STDERR "  Files that are expected to be present are:\n";
    print STDERR "  ", join(", ", @FILES), "\n";
    print STDERR "\n";
    print STDERR "  [txt_out]  optional name of .txt file to output\n";
    print STDERR "  [html_out] optional name of .htm|.html file to output\n";
    print STDERR "  [java_out] optional name of .java file to output\n";
    exit 1;
}

sub main {
    my (%ZONES, %RULES, @EQUIV, %LINKS, %COUNTRIES);

    print "Reading";
    foreach (@FILES) {
        if (! -e "$DATA_DIR/$_") {
            print STDERR "\nMissing file $DATA_DIR/$_\n\n";
            usage();
        }
        print ".";
        TZ::ParseFile("$DATA_DIR/$_", \%ZONES, \%RULES, \%LINKS, $YEAR);
    }
    print "done\n";

    # Add country data from zone.tab
    TZ::ParseZoneTab("$DATA_DIR/zone.tab", \%ZONES, \%LINKS);

    # We'll also read the iso3166.tab file here.  We don't really need
    # this except for documentation purposes (in generated files)
    # and for the HTML file.
    local(*FILE);
    open(FILE, "$DATA_DIR/iso3166.tab") or die "Can't open $DATA_DIR/iso3166.tab";
    while (<FILE>) {
        s/\#.*//;
        next unless (/\S/);
        s/\s+$//;
        if (/^([A-Z]{2})\s+(\S.*)/) {
            $COUNTRY_CODES{$1} = $2; # Map from code to country name
        } else {
            print STDERR "Ignoring $DATA_DIR/iso3166.tab line: $_";
        }
    }
    close(FILE);

    TZ::Postprocess(\%ZONES, \%RULES);

    my $aliases = incorporateAliases($TZ_ALIAS, \%ZONES, \%LINKS);

    print
        "Read ", scalar keys %ZONES, " current zones and ",
        scalar keys %RULES, " rules for $YEAR\n";

    # Make sure we have a zone named GMT from either the
    # UNIX data or the alias table.  If not, add one.
    if (!exists $ZONES{GMT}) {
        print "Adding GMT zone\n";
        my %GMT = ('format' => 'GMT',
                   'gmtoff' => '0:00',
                   'rule' => $TZ::STANDARD,
                   'until' => '');
        $ZONES{GMT} = \%GMT;
    }

    # Validate names
    foreach my $z (keys %ZONES) {
        # Make sure zone IDs only contain invariant chars
        assertInvariantChars($z);
    }

    # Create the offset index table, that includes the zones
    # for each offset and the default zone for each offset.
    # This is a hash{$name -> array ref}.  Element [0] of
    # the array is the default name.  Elements [1..n] are the
    # zones for the offset, in sorted order, including the default.
    my $offsetIndex = createOffsetIndex(\%ZONES, $TZ_DEFAULT);

    # Group zones into equivalency groups
    TZ::FormZoneEquivalencyGroups(\%ZONES, \%RULES, \@EQUIV);
    print
        "Equivalency groups (including unique zones): ",
        scalar @EQUIV, "\n";

    # Sort equivalency table first by GMT offset, then by
    # alphabetic order of encoded rule string.
    @EQUIV = sort { my $x = $ZONES{$a->[0]};
                    my $y = $ZONES{$b->[0]};
                TZ::ParseOffset($x->{gmtoff}) <=>
                TZ::ParseOffset($y->{gmtoff}) ||
                TZ::ZoneCompare($x, $y, \%RULES); } @EQUIV;

    # Sort the zones in each equivalency table entry
    foreach my $eg (@EQUIV) {
        next unless (@$eg > 1); # Skip single-zone entries
        my @zoneList = sort @$eg;
        $eg = \@zoneList;
    }

    # Create an index from zone ID to index #
    my $i = 0;
    foreach my $z (sort keys %ZONES) {
        $ZONE_ID_TO_INDEX{$z} = $i++;
    }

    # Create the country -> zone array hash
    # This hash has the form:
    # $COUNTRIES{'US'}->{zones}->[13] == "America/Los_Angeles"
    # $COUNTRIES{'US'}->{intcode} == 658

    # Some zones are not affiliated with any country (e.g., UTC).  We
    # use a fake country code for these, chosen to precede any real
    # country code.  'A' or 'AA' work.
    my $NONE = 'A';
    foreach (sort keys %ZONES) {
        my $country = $ZONES{$_}->{country};
        $country = $NONE unless ($country);
        push @{$COUNTRIES{$country}->{zones}}, $_;
    }
    foreach my $country (keys %COUNTRIES) {
        # Compute the int code, which is just a numerical
        # rep. of the two letters.  Use 0 to represent no
        # country; this MUST BE CHANGED if AA ever becomes
        # a valid country code.
        my $intcode = 0;
        if ($country ne $NONE) {
            if ($country =~ /^([A-Z])([A-Z])$/) {
                $intcode = ((ord($1) - ord('A')) << 5) |
                    (ord($2) - ord('A'));
            } else {
                die "Can't parse country code $country";
            }
        }
        $COUNTRIES{$country}->{intcode} = $intcode;
    }

    # Emit the text file
    if ($TXT_FILE) {
        emitText($TXT_FILE, \%ZONES, \%RULES, \@EQUIV, $offsetIndex, $aliases,
                 \%COUNTRIES);
        print "$TXT_FILE written.\n";
    }

    # Emit the Java file
    if ($JAVA_FILE) {
        emitJava($JAVA_FILE, \%ZONES, \%RULES, \@EQUIV, $offsetIndex, $aliases,
                 \%COUNTRIES);
        print "$JAVA_FILE written.\n";
    }

    # Emit the HTML file
    if ($HTML_FILE) {
        emitHTML($HTML_FILE, \%ZONES, \%RULES, \@EQUIV, $offsetIndex, $aliases,
                 \%COUNTRIES);
        print "$HTML_FILE written.\n";
    }

    #::dumpValue($ZONES{"America/Los_Angeles"});
    #::dumpValue($RULES{"US"});
    #::dumpValue($RULES{"Tonga"});

    # Find all the different values of rule fields:
    # in, at, on, save, type, letter
    if (0) {
        my %RULEVALS;
        foreach my $ruleName (keys %RULES) {
            for (my $i=0; $i<2; ++$i) {
                foreach my $key (qw(in on at save type letter)) {
                    if (@{$RULES{$ruleName}} < 2) {
                        print $ruleName, ":";
                        ::dumpValue($RULES{$ruleName});
                    }
                    my $x = $RULES{$ruleName}->[$i]->{$key};
                    $RULEVALS{$key}->{$x} = 1;
                }
            }
        }
        foreach my $key (sort keys %RULEVALS) {
            print "$key: ", join(", ", sort keys %{$RULEVALS{$key}}), "\n";
        }
    }
}

# Create an index of all the zones by GMT offset.  This index will
# list the zones for each offset and also the default zone for that
# offset.
# 
# Param: Ref to zone table
# Param: Name of default file
# 
# Return: ref to hash; the hash has offset integers as keys and arrays
# of zone names as values.  If there are n zone names at an offset,
# the array contains n+1 items.  The first item, [0], is the default
# zone.  Items [1..n] are the zones sorted lexically.  Thus the
# default appears twice, once in slot [0], and once somewhere in
# [1..n].
sub createOffsetIndex {
    my $zones = shift;
    my $defaultFile = shift;

    # Create an index by gmtoff.
    my %offsetMap;
    foreach (sort keys %{$zones}) {
        my $offset = TZ::ParseOffset($zones->{$_}->{gmtoff});
        push @{$offsetMap{$offset}}, $_;
    }

    # Select defaults.  We do this by reading the file $defaultFile.
    # If there are multiple errors, we want to report them all,
    # so we set a flag and die at the end if there are problems.
    my %defaults; # key=offset integer, value=zone name
    my $ok = 1;
    open(IN, $defaultFile) or die "Can't open $defaultFile: $!";
    while (<IN>) {
        my $raw = $_;
        s/\#.*//; # Trim comments
        next unless (/\S/); # Skip blank lines
        if (/^\s*(\S+)\s*$/) {
            my $z = $1;
            if (! exists $zones->{$z}) {
                print "Error: Nonexistent zone $z listed in $defaultFile line: $raw";
                $ok = 0;
                next;
            }
            my $offset = TZ::ParseOffset($zones->{$z}->{gmtoff});
            if (exists $defaults{$offset}) {
                print
                    "Error: Offset ", formatOffset($offset), " has both ",
                    $defaults{$offset}, " and ", $z,
                    " specified as defaults\n";
                $ok = 0;
                next;
            }
            $defaults{$offset} = $z;
        } else {
            print "Error: Can't parse line in $defaultFile: $raw";
            $ok = 0;
        }
    }
    close(IN);
    die "Error: Aborting due to errors in $defaultFile\n" unless ($ok);
    print "Incorporated ", scalar keys %defaults, " defaults from $defaultFile\n";

    # Go through and record the default for each GMT offset, and unshift
    # it into slot [0].
    # Fill in the blanks, since the default table will typically
    # not list a default for every single offset.
    my $missing;
    foreach my $gmtoff (keys %offsetMap) {
        my $aref = $offsetMap{$gmtoff};
        my $def;
        if (exists $defaults{$gmtoff}) {
            $def = $defaults{$gmtoff};
        } else {
            # If there is an offset for which we have no listed default
            # in $defaultFile, we try to figure out a reasonable default
            # ourselves.  We ignore any zone named Etc/ because that's not
            # a "real" zone; it's just one listed as a POSIX convience.
            # We take the first (alphabetically) zone of what's left,
            # and if there are more than one of those, we emit a warning.

            my $ambiguous;
            # Ignore zones named Etc/ and take the first one we otherwise see;
            # if there is more than one of those, emit a warning.
            foreach (sort @{$aref}) {
                next if (m|^Etc/|i);
                if (!$def) {
                    $def = $_;
                } else {
                    $ambiguous = 1;
                }
            }
            $def = $aref->[0] unless ($def);
            if ($ambiguous) {
                $missing = 1;
                print
                    "Warning: No default for GMT", formatOffset($gmtoff),
                    ", using ", $def, "\n";
            }
        }
        # Push $def onto front of list
        unshift @{$aref}, $def;
    }
    print "Defaults may be specified in $TZ_DEFAULT\n" if ($missing);

    return \%offsetMap;
}

# Given a zone and an offset index, return the gmtoff if the name
# is a default zone, otherwise return ''.
# Param: zone name
# Param: zone offset, as a string (that is, raw {gmtoff})
# Param: ref to offset index hash
sub isDefault {
    my $name = shift;
    my $offset = shift;
    my $offsetIndex = shift;
    my $aref = $offsetIndex->{TZ::ParseOffset($offset)};
    return ($aref->[0] eq $name);
}

# Emit a text file that contains data for the system time zones.
# Param: File name
# Param: ref to zone hash
# Param: ref to rule hash
# Param: ref to equiv table
# Param: ref to offset index
# Param: ref to alias hash
sub emitText {
    my $file = shift;
    my $zones = shift;
    my $rules = shift;
    my $equiv = shift;
    my $offsetIndex = shift;
    my $aliases = shift;
    my $countries = shift;

    # Find the maximum number of zones with the same value of
    # gmtOffset.
    my %perOffset; # Hash of offset -> count
    foreach my $z (keys %$zones) {
        # Use TZ::ParseOffset to normalize values - probably unnecessary
        ++$perOffset{TZ::ParseOffset($zones->{$z}->{gmtoff})};
    }
    my $maxPerOffset = 0;
    foreach (values %perOffset) {
        $maxPerOffset = $_ if ($_ > $maxPerOffset);
    }

    # Count maximum number of zones per equivalency group
    my $maxPerEquiv = 0;
    foreach my $eg (@$equiv) {
        $maxPerEquiv = @$eg if (@$eg > $maxPerEquiv);
    }

    # Count total name size
    my $name_size = 0;
    foreach my $z (keys %$zones) {
        $name_size += 1 + length($z);
    }

    local(*OUT);
    open(OUT,">$file") or die "Can't open $file for writing: $!";

    ############################################################
    # EMIT HEADER
    ############################################################
    # Zone data version
    print OUT "#####################################################################\n";
    print OUT "# Copyright (C) 2000-$YEAR, International Business Machines Corporation and\n";
    print OUT "# others. All Rights Reserved.\n";
    print OUT "#####################################################################\n";
    print OUT "#--- Header --- Generated by tz.pl\n";
    print OUT $TZ_TXT_VERSION, " # format version number of this file\n";
    print OUT $VERSION_YEAR, " # ($RAW_VERSION) version of Olson zone\n";
    print OUT $VERSION_SUFFIX, " #  data from $URL\n";
    print OUT scalar keys %$zones, " # total zone count\n";
    # The following counts are all used by gentz during its parse
    # of the tz.txt file and creation of the tz.dat file, even
    # if they don't show up in the tz.dat file header.  For example,
    # gentz needs the maxPerOffset to preallocate the offset index
    # entries.  It needs the $name_size to allocate the big buffer
    # that will receive all the names.
    print OUT scalar @$equiv, " # equivalency groups count\n";
    print OUT $maxPerOffset, " # max zones with same gmtOffset\n";
    print OUT $maxPerEquiv, " # max zones in an equivalency group\n";
    print OUT $name_size, " # length of name table in bytes\n";
    print OUT $END_MARKER, "\n\n";

    ############################################################
    # EMIT ZONE TABLE
    ############################################################
    # Output the name table, followed by 'end' keyword
    print OUT "#--- Zone table ---\n";
    print OUT "#| equiv_index,name\n";
    print OUT scalar keys %$zones, " # count of zones to follow\n";

    # IMPORTANT: This sort must correspond to the sort
    #            order of UnicodeString::compare.  That
    #            is, it must be a plain sort.
    foreach my $z (sort keys %$zones) {
        # Make sure zone IDs only contain invariant chars
        assertInvariantChars($z);

        print OUT equivIndexOf($z, $equiv), ',', $z, "\n";        
    }
    print OUT $END_MARKER, "\n\n";

    ############################################################
    # EMIT EQUIVALENCY TABLE
    ############################################################
    print OUT "#--- Equivalency table ---\n";
    print OUT "#| ('s'|'d'),zone_spec,id_count,id_list\n";
    print OUT scalar @$equiv, " # count of equivalency groups to follow\n";
    my $i = 0;
    foreach my $aref (@$equiv) {
        # $aref is an array ref; the array is full of zone IDs
        # Use the ID of the first array element
        my $z = $aref->[0];

        # Output either 's' or 'd' to indicate standard or DST
        my $isStd = ($zones->{$z}->{rule} eq $TZ::STANDARD);
        if (!$isStd) {
        	my $rule = $rules->{$zones->{$z}->{rule}};
        	if (!(@{$rule} >= 4 && ($rule->[3] & 1) && ($rule->[3] & 2))) {
        		$isStd = 1;
        	}
        }

        print OUT $isStd ? 's,' : 'd,';
        
        # Format the zone
        my ($spec, $notes) = formatZone($z, $zones->{$z}, $rules);

        # Now add the equivalency list
        push @$spec, scalar @$aref;
        push @$notes, "[";
        my $min = -1;
        foreach $z (@$aref) {
            my $index = $ZONE_ID_TO_INDEX{$z};
            # Make sure they are in order
            die("Unsorted equiv table indices") if ($index <= $min);
            $min = $index;
            push @$spec, $index;
            push @$notes, $z;
        }
        push @$notes, "]";
        
        unshift @$notes, $i++; # Insert index of this group at front
        print OUT join($SEP, @$spec) . " # " . join(' ', @$notes), "\n";
    }
    print OUT $END_MARKER, "\n\n";

    ############################################################
    # EMIT INDEX BY GMT OFFSET
    ############################################################
    # Create a hash mapping zone name -> integer, from 0..n-1.
    # Create an array mapping zone number -> name.
    my %zoneNumber;
    my @zoneName;
    $i = 0;
    foreach (sort keys %$zones) {
        $zoneName[$i] = $_;
        $zoneNumber{$_} = $i++;
    }

    # Emit offset index
    print OUT "#--- Offset INDEX ---\n";
    print OUT "#| gmt_offset,default_id,id_count,id_list\n";
    print OUT scalar keys %{$offsetIndex}, " # index by offset entries to follow\n";
    foreach (sort {$a <=> $b} keys %{$offsetIndex}) {
        my $aref = $offsetIndex->{$_};
        my $def = $aref->[0];
        # Make a slice of 1..n
        my @b = @{$aref}[1..$#{$aref}];
        print OUT
            $_, ",", $zoneNumber{$def}, ",",
            scalar @b, ",",
            join(",", map($zoneNumber{$_}, @b)),
            " # ", formatOffset($_), " d=", $def, " ",
            join(" ", @b), "\n";
    }

    print OUT $END_MARKER, "\n\n";

    ############################################################
    # EMIT INDEX BY COUNTRY
    ############################################################
    print OUT "#--- Country INDEX ---\n";
    print OUT "#| country_int_code,id_count,id_list\n";
    print OUT scalar keys %$countries, " # index by country entries to follow\n";
    foreach my $country (sort keys %$countries) {
        my $intcode = $countries->{$country}->{intcode};
        my $aref = $countries->{$country}->{zones};
        print OUT
            $intcode, ",", scalar @$aref, ",",
            join(",", map($zoneNumber{$_}, @$aref)), " # ",
            ($intcode ? ($country . " (" . $COUNTRY_CODES{$country} . ") ") : "(None) "),
            join(" ", @$aref), "\n";
    }

    print OUT $END_MARKER, "\n";

    ############################################################
    # END
    ############################################################
    close(OUT);
}

# Emit a Java file that contains data for the system time zones.
# Param: File name
# Param: ref to zone hash
# Param: ref to rule hash
# Param: ref to equiv table
# Param: ref to offset index
# Param: ref to alias hash
sub emitJava {
    my $file = shift;
    my $zones = shift;
    my $rules = shift;
    my $equiv = shift;
    my $offsetIndex = shift;
    my $aliases = shift;
    my $countries = shift;

    my $_indent = "        ";
    
    #############################################################
    # Zone table
    my $_IDS;
    foreach my $z (sort keys %$zones) {
        $_IDS .= "$_indent\"$z\",\n";
    }

    #############################################################
    # Equivalency table
    # - While we output this, keep track of a mapping from equivalency table ID
    #   (a value from, e.g., 0..114) to equivalency int[] array index (e.g.,
    #   0, 15, 30, 34, etc.).
    my $_DATA;

    my %equiv_id_to_index;
    my $i = 0;
    my $index = 0;
    foreach my $aref (@$equiv) {
        $equiv_id_to_index{$i} = $index;

        # $aref is an array ref; the array is full of zone IDs
        # Use the ID of the first array element
        my $z = $aref->[0];

        $_DATA .= $_indent; # Indent

        # Output either 's' or 'd' to indicate standard or DST
        my $isStd = ($zones->{$z}->{rule} eq $TZ::STANDARD);
        if (!$isStd) {
        	my $rule = $rules->{$zones->{$z}->{rule}};
        	if (!(@{$rule} >= 4 && ($rule->[3] & 1) && ($rule->[3] & 2))) {
        		$isStd = 1;
        	}
        }
        $_DATA .= $isStd ? '0/*s*/,' : '1/*d*/,';
        
        # Format the zone
        my ($spec, $notes) = formatZone($z, $zones->{$z}, $rules);

        # Now add the equivalency list
        push @$spec, scalar @$aref;
        push @$notes, "[";
        my $min = -1;
        foreach $z (@$aref) {
            my $index = $ZONE_ID_TO_INDEX{$z};
            # Make sure they are in order
            die("Unsorted equiv table indices") if ($index <= $min);
            $min = $index;
            push @$spec, $index;
            push @$notes, $z;
        }
        push @$notes, "]";
        
        unshift @$notes, $i++; # Insert index of this group at front

        # Convert to Java constants:
        # 'w' -> 0, 's' -> 1, 'u' -> 2
        foreach (@$spec) {
            if (/^w$/) {
                $_ = "0/*w*/";
            } elsif (/^s$/) {
                $_ = "1/*s*/";
            } elsif (/^u$/) {
                $_ = "2/*u*/";
            }
        }

        $_DATA .= join($SEP, @$spec) . ", // " . join(' ', @$notes) . "\n";
        $index += (scalar @$spec) + 1; # +1 for s/d
    }

    #############################################################
    # Zone->Equivalency mapping
    my $_INDEX_BY_NAME;
    foreach my $z (sort keys %$zones) {
        $_INDEX_BY_NAME .=
            $_indent .
            $equiv_id_to_index{equivIndexOf($z, $equiv)} .
            ", // $z\n";
    }

    #############################################################
    # Index by offset
    # Create a hash mapping zone name -> integer, from 0..n-1.
    # Create an array mapping zone number -> name.
    my $_INDEX_BY_OFFSET;
    my %zoneNumber;
    my @zoneName;
    $i = 0;
    foreach (sort keys %$zones) {
        $zoneName[$i] = $_;
        $zoneNumber{$_} = $i++;
    }
    # Emit offset index
    foreach (sort {$a <=> $b} keys %{$offsetIndex}) {
        my $aref = $offsetIndex->{$_};
        my $def = $aref->[0];
        # Make a slice of 1..n
        my @b = @{$aref}[1..$#{$aref}];
        $_INDEX_BY_OFFSET .=
            $_indent . $_ . "," . $zoneNumber{$def} . "," .
            scalar @b . "," .
            join(",", map($zoneNumber{$_}, @b)) .
            ", // " . formatOffset($_) . " d=" . $def . " " .
            join(" ", @b) . "\n";
    }

    ############################################################
    # Index by country
    my $_INDEX_BY_COUNTRY;
    foreach my $country (sort keys %$countries) {
        my $intcode = $countries->{$country}->{intcode};
        my $aref = $countries->{$country}->{zones};
        # Emit int code (n1*32 + n0), #of zones,
        # and list of zones.
        $_INDEX_BY_COUNTRY .=
            $_indent . $intcode . ", " .
            scalar(@$aref) . ", " .
            join(", ", map($zoneNumber{$_}, @$aref)) . ", // " .
            ($intcode ? ($country . " (" . $COUNTRY_CODES{$country} . ")") : "(None)") . ": " .
            join(" ", @$aref) .
            "\n";
    }
    
############################################################
# BEGIN JAVA TEMPLATE
############################################################
    my $java = <<"END";
// Instructions: Build against icu4j. Run and save output.
// Paste output into icu4j/src/com/ibm/util/TimeZoneData.java
import com.ibm.icu.impl.Utility;
import java.util.Date;
public class tz {
    public static void main(String[] args) {
        System.out.println("    // BEGIN GENERATED SOURCE CODE");
        System.out.println("    // Date: " + new Date());
        System.out.println("    // Version: $RAW_VERSION from $URL");
        System.out.println("    // Tool: icu/source/tools/gentz");
        System.out.println("    // See: icu/source/tools/gentz/readme.txt");
        System.out.println("    // DO NOT EDIT THIS SECTION");
        System.out.println();

        System.out.println("    /**");
        System.out.println("     * Array of IDs in lexicographic order.  The INDEX_BY_OFFSET and DATA");
        System.out.println("     * arrays refer to zones using indices into this array.  To map from ID");
        System.out.println("     * to equivalency group, use the INDEX_BY_NAME Hashtable.");
        System.out.println("     * >> GENERATED DATA: DO NOT EDIT <<");
        System.out.println("     */");
        System.out.println("    static final String[] IDS = {");
        for (int i=0;i<IDS.length;++i) {
            System.out.println("        \\\"" + IDS[i] + "\\\",");
        }
        System.out.println("    };\\n");

        System.out.println("    /**");
        System.out.println("     * RLE encoded form of DATA.");
        System.out.println("     * \@see com.ibm.util.Utility.RLEStringToIntArray");
        System.out.println("     * >> GENERATED DATA: DO NOT EDIT <<");
        System.out.println("     */");
        System.out.println("    static final String DATA_RLE =");
        System.out.println(Utility.formatForSource(Utility.arrayToRLEString(DATA)));
        System.out.println("        ;\\n");

        System.out.println("    /**");
        System.out.println("     * RLE encoded form of INDEX_BY_NAME_ARRAY.");
        System.out.println("     * \@see com.ibm.util.Utility.RLEStringToIntArray");
        System.out.println("     * >> GENERATED DATA: DO NOT EDIT <<");
        System.out.println("     */");
        System.out.println("    static final String INDEX_BY_NAME_ARRAY_RLE =");
        System.out.println(Utility.formatForSource(Utility.arrayToRLEString(INDEX_BY_NAME_ARRAY)));
        System.out.println("        ;\\n");

        System.out.println("    /**");
        System.out.println("     * RLE encoded form of INDEX_BY_OFFSET.");
        System.out.println("     * \@see com.ibm.util.Utility.RLEStringToIntArray");
        System.out.println("     * >> GENERATED DATA: DO NOT EDIT <<");
        System.out.println("     */");
        System.out.println("    static final String INDEX_BY_OFFSET_RLE =");
        System.out.println(Utility.formatForSource(Utility.arrayToRLEString(INDEX_BY_OFFSET)));
        System.out.println("        ;\\n");

        System.out.println("    /**");
        System.out.println("     * RLE encoded form of INDEX_BY_COUNTRY.");
        System.out.println("     * \@see com.ibm.util.Utility.RLEStringToIntArray");
        System.out.println("     * >> GENERATED DATA: DO NOT EDIT <<");
        System.out.println("     */");
        System.out.println("    static final String INDEX_BY_COUNTRY_RLE =");
        System.out.println(Utility.formatForSource(Utility.arrayToRLEString(INDEX_BY_COUNTRY)));
        System.out.println("        ;\\n");

        System.out.println("    // END GENERATED SOURCE CODE");
    }

    static final String[] IDS = {
$_IDS
    };

    static final int[] DATA = {
$_DATA
    };

    static final int[] INDEX_BY_NAME_ARRAY = {
$_INDEX_BY_NAME
    };

    static final int[] INDEX_BY_OFFSET = {
        // gmt_offset,default_id,id_count,id_list
$_INDEX_BY_OFFSET
    };

    static final int[] INDEX_BY_COUNTRY = {
$_INDEX_BY_COUNTRY
    };
}
END
############################################################
# END JAVA TEMPLATE
############################################################

    open(OUT, ">$file") or die "Can't open $file for writing: $!";
    print OUT $java;
    close(OUT);
}

# Emit an HTML file that contains a description of the system zones.
# Param: File name
# Param: ref to zone hash
# Param: ref to rule hash
# Param: ref to equiv table
# Param: ref to offset index
# Param: ref to alias hash
sub emitHTML {
    my $file = shift;
    my $zones = shift;
    my $rules = shift;
    my $equiv = shift;
    my $offsetIndex = shift;
    my $aliases = shift;
    my $countries = shift;

    # These are variables for the template
    my $_count = scalar keys %{$zones};
    my $_equiv = scalar @$equiv;

    # Build table in order of zone offset
    my $_offsetTable = "<p><table>\n";
    foreach (sort {$a <=> $b} keys %{$offsetIndex}) {
        my $aref = $offsetIndex->{$_};
        my $def = $aref->[0];
        # Make a slice of 1..n
        my @b = @{$aref}[1..$#{$aref}];
        my $gmtoff = "GMT" . formatOffset($_);
        $_offsetTable .=
            "<tr valign=top>" .
            "<td><a name=\"" . bookmark($gmtoff) . "\">$gmtoff</a></td>" .
            "<td>" .
            join(", ", map($_ eq $def ?
                           "<a href=\"#" . bookmark($_) . "\"><b>$_</b></a>" :
                           "<a href=\"#" . bookmark($_) . "\">$_</a>", @b)) .
            "</td>" .
            "</tr>\n";
    }
    $_offsetTable .= "</table>\n";

    # Build table in alphabetical order of zone name
    my $_nameTable = "<p><table>\n";
    $_nameTable .= "<tr><td>ID</td>";
    $_nameTable .= "<td>Offset</td><td>DST Begins</td><td>DST Ends</td>";
    $_nameTable .= "<td>Savings</td><td></td></tr>\n";

    $_nameTable .= "<tr><td><hr></td>";
    $_nameTable .= "<td><hr></td><td><hr></td>";
    $_nameTable .= "<td><hr></td><td><hr></td><td></td></tr>\n";
    # Need a reverse alias table
    my %revaliases = reverse(%$aliases);
    foreach my $z (sort keys %$zones) {
        $_nameTable .= emitHTMLZone($z, $zones->{$z}, $rules, $offsetIndex,
                                    $aliases, \%revaliases);
    }
    $_nameTable .= "</table>\n";

    # Build equivalency group table
    my $_equivTable = "<p><table>\n";
    $_equivTable .= "<tr><td>Offset</td><td>DST Begins</td><td>DST Ends</td>";
    $_equivTable .= "<td>Savings</td><td>Zones</td></tr>\n";

    $_equivTable .= "<tr><td><hr></td>";
    $_equivTable .= "<td><hr></td><td><hr></td>";
    $_equivTable .= "<td><hr></td><td><hr></td><td><hr></td></tr>\n";

    # Equiv table is sorted elsewhere -- output it in native order
    foreach my $eg (@$equiv) {
        $_equivTable .= emitHTMLEquiv($eg, $zones, $rules);
    }
    $_equivTable .= "</table>\n";

    # Build country table
    my $_countryTable;
    $_countryTable .= "<p><table>\n";
    $_countryTable .= "<tr><td>Country</td><td>Zones</td></tr>\n";
    $_countryTable .= "<tr><td><hr></td><td><hr></td></tr>\n";

    foreach my $country (sort keys %$countries) {
        $_countryTable .=
            "<tr valign=top><td nowrap>" .
            (($country ne 'A') ? ($country . " (" . $COUNTRY_CODES{$country} . ")") : "(None)") .
            "</td>" . "<td>" .
            join(", ", map("<a href=\"#" . bookmark($_) . "\">$_</a>", @{$countries->{$country}->{zones}})) .
            #join(", ", @{$countries->{$country}->{zones}}) .
            "</td></tr>\n";
    }    
    $_countryTable .= "</table>\n";

    # Time stamp
    my $_timeStamp = localtime;

############################################################
# BEGIN HTML TEMPLATE
############################################################
    my $html = <<"END";
<html>

<head>
<title>ICU System Time Zones</title>
</head>

<body>

<h1>ICU System Time Zones</h1>

<table border="0">
  <tr>
    <td>Version</td>
    <td><strong>$RAW_VERSION</strong> ($VERSION_YEAR.$VERSION_SUFFIX)</td>
  </tr>
  <tr>
    <td>Total zone count</td>
    <td><strong>$_count</strong> in <strong>$_equiv</strong> equivalency groups</td>
  </tr>
  <tr>
    <td>Original source</td>
    <td><strong><a href="$URL">$URL</a></strong></td>
  </tr>
  <tr>
    <td>Author</td>
    <td><strong>Alan Liu <a href="mailto:liuas\@us.ibm.com">&lt;liuas\@us.ibm.com&gt;</a></strong></td>
  </tr>
  <tr>
    <td>This document generated</td>
    <td><strong>$_timeStamp</strong></td>
  </tr>
</table>

<h3>Background</h3>

<p>A time zone represents an offset applied to Greenwich Mean Time
(GMT) to obtain local time. The offset may vary throughout the year,
if daylight savings time (DST) is used, or may be the same all year
long. Typically, regions closer to the equator do not use DST. If DST
is in use, then specific rules define the point at which the offset
changes, and the amount by which it changes. Thus, a time zone is
described by the following information:

<ul>
  <li><a name="cols">An</a> identifying string, or ID. This consists only of invariant characters (see the file <code>utypes.h</code>).
    It typically has the format <em>continent</em> / <em>city</em>. The city chosen is
    not the only city in which the zone applies, but rather a representative city for the
    region. Some IDs consist of three or four uppercase letters; these are legacy zone
    names that are aliases to standard zone names.</li>
  <li>An offset from GMT, either positive or negative. Offsets range from approximately minus
    half a day to plus half a day.</li>
</ul>

<p>If DST is observed, then three additional pieces of information are needed:

<ul>
  <li>The precise date and time during the year when DST begins. This is in the first
    half of the year in the northern hemisphere, and in the second half of the year in the
    southern hemisphere.</li>
  <li>The precise date and time during the year when DST ends. This is in the first half
    of the year in the southern hemisphere, and in the second half of the year in the northern
    hemisphere.</li>
  <li>The amount by which the GMT offset changes when DST is in effect. This is almost
    always one hour.</li>
</ul>

<h3>System and User Time Zones</h3>

<p>ICU supports local time zones through the classes
<code>TimeZone</code> and <code>SimpleTimeZone</code> in the C++
API. In the C API, time zones are designated by their ID strings.</p>

<p>Users may construct their own time zone objects by specifying the
above information to the C++ API. However, it is more typical for
users to use a pre-existing system time zone, since these represent
all current international time zones in use. This document lists the
system time zones, both in order of GMT offset, and in alphabetical
order of ID.</p>

<p>Since this list changes one or more times a year, <em>this document
only represents a snapshot</em>. For the current list of ICU system
zones, use the method <code>TimeZone::getAvailableIDs()</code>.</p>

<h3>Notes</h3>

<p><a name="order">The</a> zones are listed in binary sort order.  That is, 'A' through
'Z' come before 'a' through 'z'.  This is the same order in which the
zones are stored internally, and the same order in which they are
returned by <code>TimeZone::getAvailableIDs()</code>.  The reason for
this is that ICU locates zones using a binary search, and the binary
search relies on this sort order.</p>

<p>You may notice that zones such as <a href="#EtcGMTp1">Etc/GMT+1</a>
appear to have the wrong sign for their GMT offset.  In fact, their
sign is inverted because the the Etc zones follow the POSIX sign
conventions.  This is the way the original Olson data is set up, and
ICU reproduces the Olson data faithfully, including this confusing
aspect.  See the Olson files for more details.

<h3>References</h3>

<p>The ICU system time zones are derived from the Olson data at <a
href="$URL">$URL</a>. This is the data used by UNIX systems and is
updated one or more times each year. Unlike the Olson zone data, ICU
only contains data for current zone usage. There is no support for
historical zone data in ICU at this time.</p>

<hr>

<h2>Time Zones in order of GMT offset</h2>

<p>Zone listed in <strong>bold</strong> are the default zone for a
given GMT offset. This default is used by ICU if it cannot identify
the host OS time zone by name. In that case, it uses the default zone
for the host zone offset.</p>

$_offsetTable
<hr>

<h2>Time Zones in order of ID</h2>

<p>Zone listed in <strong>bold</strong> are the default zone for their
GMT offset. This default is used by ICU if it cannot identify the host
OS time zone by name. In that case, it uses the default zone for the
host zone offset. See above for a description of <a
href="#cols">columns</a>. See note above for an explanation of the
sort <a href="#order">order</a>.</p>

<p>Times suffixed with 's' are in standard time. Times suffixed with 'u' are in UTC time.
Times without suffixes are in wall time (that is, either standard time or daylight savings
time, depending on which is in effect).</p>

$_nameTable
<hr>

<h2>Time Zone Equivalency Groups</h2>

<p>ICU groups zones into <em>equivalency groups</em>.  These are
groups of zones that are identical in GMT offset and in rules, but
that have different IDs.  Knowledge of equivalency groups allows ICU
to reduce the amount of data stored.  More importantly, it allows ICU
to apply data for one zone to other equivalent zones when appropriate
(e.g., in formatting).  Equivalency groups are formed at build time,
not at runtime, so the runtime cost to lookup the equivalency group of
a given zone is negligible.</p>

$_equivTable
<hr>

<h2>Time Zones by Country</h2>

<p>ICU captures and exports the country data from the Olson database.
The country code is the ISO 3166 two-letter code.  Some zones have no
associated country; these are listed under the entry "(None)".

$_countryTable
</body>
</html>
END
############################################################
# END HTML TEMPLATE
############################################################

    open(HTML, ">$file") or die "Can't open $file for writing: $!";
    print HTML $html;
    close(HTML);
}

# Make a bookmark name out of a string.  This just means normalizing
# non-word characters.
sub bookmark {
    local $_ = shift;
    s/-/m/g;
    s/\+/p/g;
    s/\W//g;
    $_;
}

# Emit an equivalency group as an HTML table row.  Return the string.
# Param: ref to array of zone IDs
# Param: ref to zone hash
# Param: ref to rule hash
sub emitHTMLEquiv {
    my $eg = shift;
    my $zone = shift;
    my $rule = shift;
    local $_ = "<tr valign=top>";
    $_ .= _emitHTMLZone($zone->{$eg->[0]}, $rule);
    # Don't sort @$eg -- output in native order
    $_ .= "<td>" . join(" ", @$eg) . "</td>";
    $_ .= "</tr>\n";
    $_;
}

# Emit a zone description without ID, alias info etc.
# Param: zone OBJECT hash ref
# Param: rule hash ref
sub _emitHTMLZone {
    my ($zone, $rules) = @_;
    my $gmtoff = "GMT" . formatOffset(TZ::ParseOffset($zone->{gmtoff}));
    local $_ = "<td><a href=\"#" . bookmark($gmtoff) . "\">$gmtoff</a></td>";
    if ($zone->{rule} ne $TZ::STANDARD) {
        my $rule = $rules->{$zone->{rule}};
        $_ .= "<td nowrap>" . emitHTMLRule($rule->[0]) . "</td>";
        $_ .= "<td nowrap>" . emitHTMLRule($rule->[1]) . "</td>";
        $_ .= "<td>" . $rule->[0]->{save} . "</td>";
    } else {
        $_ .= "<td colspan=3></td>";
    }
    $_;
}

# Emit a single zone description as HTML table row.  Return the string.
# Param: Zone name
# Param: Zone hash object ref
# Param: Ref to rules hash
# Param: ref to offset index
# Param: ref to alias hash
# Param: ref to reverse alias hash
sub emitHTMLZone {
    my ($name, $zone, $rules, $offsetIndex, $aliases, $revaliases) = @_;
    my $isDefault = isDefault($name, $zone->{gmtoff}, $offsetIndex);
    my $alias = exists $aliases->{$name} ? $aliases->{$name} : '';
    my $revalias = exists $revaliases->{$name} ? $revaliases->{$name} : '';
    local $_ = "<tr><td>" . ($isDefault?"<b>":"") .
        "<a name=\"" . bookmark($name) . "\">$name</a>" . ($isDefault?"</b>":"") . "</td>";
    $_ .= _emitHTMLZone($zone, $rules);
    if ($alias) {
        $_ .= "<td><em>alias for</em> <a href=\"#" .
            bookmark($alias) . "\">$alias</a></td>";
    } elsif ($revalias) {
        $_ .= "<td><em>alias </em> <a href=\"#" .
            bookmark($revalias) . "\">$revalias</a></td>";
    } else {
        $_ .= "<td></td>";
    }
    $_ .= "</tr>\n";
    $_;
}

# Emit a zone rule as HTML.  Return the string.
# Param: Rule hash object ref
sub emitHTMLRule {
    my $rule = shift;
    $rule->{in} ." ". $rule->{on} ." ". $rule->{at};
}

# Read the alias list and create clones with alias names.  This
# sub should be called AFTER all standard zones have been read in.
# Param: File name of alias list
# Param: Ref to zone hash
# Param: Ref to LINK hash
# Return: Ref to hash of {alias name -> zone name}
sub incorporateAliases {
    my $aliasFile = shift;
    my $zones = shift;
    my $links = shift;
    my $n = 0;
    my %hash;
    local *IN;
    open(IN,$aliasFile) or die "Can't open $aliasFile: $!";
    while (<IN>) {
        s/\#.*//; # Trim comments
        next unless (/\S/); # Skip blank lines
        if (/^\s*(\S+)\s+(\S+)\s*$/) {
            my ($alias, $original) = ($1, $2);
            if (exists $zones->{$alias}) {
                die "Bad alias in $aliasFile: $alias is a standard UNIX zone. " .
                    "Please remove $alias from the alias table.\n";
            }
            if (!exists $zones->{$original}) {
                die "Bad alias in $aliasFile: $alias maps to the nonexistent " .
                    "zone $original. Please fix this entry in the alias table.\n";
            }
            if (exists $links->{$alias} &&
                $links->{$alias} ne $original) {
                print STDERR "Warning: Alias $alias for $original exists as link for ",
                    $links->{$alias}, "\n";
            }
            # Create the alias!
            $zones->{$alias} = $zones->{$original};
            $hash{$alias} = $original;
            $n++;
        } else {
            die "Bad line in alias table $aliasFile: $_\n";
        }
    }
    print "Incorporated $n aliases from $aliasFile\n";
    close(IN);
    \%hash;
}

# Format a time zone as a machine-readable line of text.  Another
# tool will read this line to construct a binary data structure
# representing this zone.
# Param: Zone name
# Param: Zone hash
# Param: Ref to hash of all rules
# Return: Two array refs, one to the specs, one to the notes
sub formatZone { # ($z, $ZONES{$z}, \%RULES)
    my $name = shift;
    my $zone = shift;
    my $rules = shift;

    my @spec;
    #my @notes = ( $name );
    my @notes;
    
    # GMT offset
    push @notes, ($zone->{gmtoff}=~/^-/?"GMT":"GMT+") . $zone->{gmtoff};
    push @spec, TZ::ParseOffset($zone->{gmtoff});

    #|rawOffset      The new SimpleTimeZone's raw GMT offset
    #|ID             The new SimpleTimeZone's time zone ID.
    #|startMonth     The daylight savings starting month. Month is
    #|               0-based. eg, 0 for January.
    #|startDay       The daylight savings starting
    #|               day-of-week-in-month. See setStartRule() for a
    #|               complete explanation.
    #|startDayOfWeek The daylight savings starting day-of-week. See
    #|               setStartRule() for a complete explanation.
    #|startTime      The daylight savings starting time, expressed as the
    #|               number of milliseconds after midnight.
    #|endMonth       The daylight savings ending month. Month is
    #|               0-based. eg, 0 for January.
    #|endDay         The daylight savings ending day-of-week-in-month.
    #|               See setStartRule() for a complete explanation.
    #|endDayOfWeek   The daylight savings ending day-of-week. See
    #|               setStartRule() for a complete explanation.
    #|endTime        The daylight savings ending time, expressed as the
    #|               number of milliseconds after midnight.

    my $rule = $zone->{rule};
    if ($rule ne $TZ::STANDARD) {
        $rule = $rules->{$rule};
        # $rule is now an array ref, with [0] being the onset and
        # [1] being the cease.
        
        if (@{$rule} >= 4 && ($rule->[3] & 1) && ($rule->[3] & 2)) {
			
			formatRule($rule->[0], \@spec, \@notes); # Onset
			formatRule($rule->[1], \@spec, \@notes); # Cease
	
			my @a = parseTime($rule->[0]->{save});
			if ($a[1] ne 'w') {
				die "Strange DST savings value: \"$rule->[0]->{save}\"";
			}
			push @notes, $rule->[0]->{save};
			push @spec, $a[0];
        }
    }

    (\@spec, \@notes);
}

# Format a rule and return the string
# Param: reference to rule hash
# Param: ref to spec array (this is a result param)
# Param: ref to annotation array (this is a result param)
sub formatRule {
    my $rule = shift;
    my $spec = shift;
    my $notes = shift;
    push @$notes, $rule->{in}, $rule->{on}, $rule->{at};
    push @$spec, parseMonth($rule->{in}); # Month
    push @$spec, parseDaySpecifier($rule->{on}); # Day
    push @$spec, parseTime($rule->{at}); # Time
}

# Format an offset in seconds and return a string of the form
# /[+-]\d{1,2}:\d\d(:\d\d)?/.
# Param: Offset in seconds
# Return: String
sub formatOffset {
    local $_ = shift;
    my $result = $_<0 ? "-":"+";
    $_ = -$_ if ($_ < 0);
    my $sec = $_ % 60;  $_ = ($_ - $sec) / 60;
    my $min = $_ % 60;  $_ = ($_ - $min) / 60;
    $min = "0$min" if ($min < 10);
    $sec = $sec ? ($sec < 10 ? ":0$sec" : ":$sec") : "";
    $result . $_ . ":" . $min . $sec;
}

# Parse a time of the format dd:dds, where s is a suffix character.
# Return the time, in minutes, and the suffix, in an array.
# Only the suffixes 's' and 'u' are recognized.
# Param: String, with optional suffix
# Return: Array ( seconds, suffix ).  If no suffix, 'w' is used.
sub parseTime {
    local $_ = shift;
    if (/^(\d{1,2}):(\d\d)([su])?$/) {
        my $a = ($1*60) + $2;
        my $s = defined $3?$3:'w';
        return ( $a, $s );
    } else {
        die "Cannot parse time \"$_\"";
    }
}

# Given a month string, return an integer from 0 (Jan) to 11 (Dec).
# Param: Str
# Return: Int 0..11.
sub parseMonth {
    local $_ = shift;
    for (my $i=0; $i<12; $i++) {
        return $i if (/$MONTH[$i]/i);
    }
    die "Can't parse month \"$_\"";
}

# Given a specifier for the day of the month on which a rule triggers,
# return an array of two integers encoding that information.  We use
# the ICU/java.util.SimpleTimeZone encoding scheme using two integers.
# We return the two integers in an array of ( dowim dow ).
# Param: String, such as
#     1, 12, 15, 18, 2, 20, 21, 22, 23, 25, 28, 3, 30, 31, 4, 7, Fri>=1,
#     Fri>=15, Sat>=1, Sat>=15, Sun<=14, Sun>=1, Sun>=10, Sun>=11, Sun>=15,
#     Sun>=16, Sun>=23, Sun>=8, Sun>=9, lastFri, lastSun, lastThu
#   This is the {on} field of the rule hash.
# Return: Array of two integers, ( dowim dow ).
#   The dow has Sunday = 1 .. Saturday = 7.
sub parseDaySpecifier {
    local $_ = shift;

    #|+If both dayOfWeekInMonth and dayOfWeek are positive, they specify the
    #| day of week in the month (e.g., (2, WEDNESDAY) is the second Wednesday
    #| of the month).
    #|+If dayOfWeek is positive and dayOfWeekInMonth is negative, they specify
    #| the day of week in the month counting backward from the end of the month.
    #| (e.g., (-1, MONDAY) is the last Monday in the month)
    #|+If dayOfWeek is zero and dayOfWeekInMonth is positive, dayOfWeekInMonth
    #| specifies the day of the month, regardless of what day of the week it is.
    #| (e.g., (10, 0) is the tenth day of the month)
    #|+If dayOfWeek is zero and dayOfWeekInMonth is negative, dayOfWeekInMonth
    #| specifies the day of the month counting backward from the end of the
    #| month, regardless of what day of the week it is (e.g., (-2, 0) is the
    #| next-to-last day of the month).
    #|+If dayOfWeek is negative and dayOfWeekInMonth is positive, they specify the
    #| first specified day of the week on or after the specfied day of the month.
    #| (e.g., (15, -SUNDAY) is the first Sunday after the 15th of the month
    #| [or the 15th itself if the 15th is a Sunday].)
    #|+If dayOfWeek and dayOfWeekInMonth are both negative, they specify the
    #| last specified day of the week on or before the specified day of the month.
    #| (e.g., (-20, -TUESDAY) is the last Tuesday before the 20th of the month
    #| [or the 20th itself if the 20th is a Tuesday].)

    # dowim dow
    # >0    >0    day of week in month
    # <0    >0    day of week in month (from end)
    # >0    0     day of month
    # <0    0     day of month (from end; -1 is last dom)
    # >0    <0    first dow on or after dom
    # <0    <0    last dow on or before dom

    my $dowim;
    my $dow = 0;

    # Check for straight DOM
    if (/^\d+$/) {
        $dowim = $_;
        $dow = 0;
        return ( $dowim, $dow );
    }

    # Anything else must have a dow embedded in it; parse it out
    my @DOW = ( 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat' );
    for (my $i=0; $i<@DOW; $i++) {
        if (s/$DOW[$i]//) {
            $dow = $i + 1;
            last;
        }
    }
    if ($dow == 0) {
        die "Cannot parse day specifier \"$_\"";
    }

    # Now we have either >=n, <=n, last, or first.
    if (/^last$/) {
        $dowim = -1;
    } elsif (/^first$/) {
        $dowim = 1;
    } elsif (/^>=(\d+)$/) {
        $dowim = $1;
        $dow = -$dow;
    } elsif (/^<=(\d+)$/) {
        $dowim = -$1;
        $dow = -$dow;
    } else {
        die "Cannot parse day specifier \"$_\"";
    }

    ( $dowim, $dow );
}

# Confirm that the given ID contains only invariant characters.
# See utypes.h for an explanation.
# Param: string to be checked
sub assertInvariantChars {
    local $_ = shift;
    if (/[^A-Za-z0-9 \"%&\'()*+,-.\/:;<=>?_]/) {
        die "Error: Zone ID \"$_\" contains non-invariant characters\n";
    }
}

# Map ID to equivalency table index.  Return the index of the given ID
# in the equivalency array.  The array contains array refs.  Each ref
# points to an array of strings.
# Param: ID to find
# Param: Ref to equiv array (ref to array of refs to arrays of IDs)
# Return: Index into array where ID is found, or -1 if not found
# NOTE: This function can be eliminated by generating a reverse
#       mapping hash when we create the equivalency table.
sub equivIndexOf {
    my $id = shift;
    my $a = shift;
    for (my $i=0; $i < scalar @{$a}; ++$i) {
        my $aa = $a->[$i];
        foreach (@$aa) {
            return $i if ($_ eq $id);
        }
    }
    return -1;
}

__END__
