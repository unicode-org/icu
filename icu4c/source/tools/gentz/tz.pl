#!/usr/bin/perl
######################################################################
# Copyright (C) 1999, International Business Machines
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
# - The file contains a header and 3 lists.
# - The header contains the version of the unix data, the total
#   zone count, and the length of the name table in bytes.
#   |1999 # (tzdata1999j) ftp://elsie.nci.nih.gov data version YEAR
#   |10 # (tzdata1999j) ftp://elsie.nci.nih.gov data version SUFFIX
#   |387 # total zone count
#   |5906 # length of name table in bytes
# - Lists start with a count of the records to follow, the records
#   themselves (one per line), and a single line with the keyword
#   'end'.
# - The first list is the list of standard zones:
#   |208 # count of standard zones to follow
#   |0,0 # Africa/Abidjan GMT+0:00
#   |28,10800 # Africa/Addis_Ababa GMT+3:00
#   ...
#   |end
#   Each standard zone record contains two integers.  The first
#   is a byte offset into the name table for the name of the zone.
#   The second integer is the GMT offset in SECONDS for this zone.
# - The second list is the list of DST zones:
#   |179 # count of dst zones to follow
#   |15,0,8,1,0,0,w,11,31,0,0,w,20 # Africa/Accra GMT+0:00 Sep 1...
#   |184,7200,3,-1,6,0,s,8,-1,5,1380,s,60 # Africa/Cairo GMT+2:0...
#   ...
#   |end
#   Each record starts with the same two integers as a standard
#   zone record.  Following this are data for the onset rule and
#   the cease rule.  Each rule is described by the following integers:
#     month (JAN = 0)
#     dowim } These two values are in SimpleTimeZone encoded
#     dow   } format for DOM, DOWIM, DOW>=DOM, or DOW<=DOM.
#     time MINUTES
#     time mode ('w', 's', 'u')
#   The last integer in the record is the DST savings in MINUTES,
#   typically 60.
# - The third list is the name table:
#   |387 # count of names to follow
#   |Africa/Abidjan
#   |Africa/Accra
#   ...
#   |end
#   Each name is terminated by a newline (like all lines in the file).
#   The offsets in the first two lists refer to this table.
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
#     Sun>=16, Sun>=23, S un>=8, Sun>=9, lastFri, lastSun, lastThu
# save: 0, 0:20, 0:30, 1:00
# type: -

require 5; # Minimum version of perl needed
use strict;
use Getopt::Long;
use vars qw(@FILES $YEAR $DATA_DIR $OUT $SEP @MONTH
            $VERSION_YEAR $VERSION_SUFFIX $RAW_VERSION);
require 'dumpvar.pl';
use TZFileParser;
use TZUtility;

# We get the current year from the system here.  Later
# we double check this against the zone data version.
$YEAR = 1900+@{[localtime]}[5]; # Get the current year

$DATA_DIR = shift;

if (!$DATA_DIR || ! -d $DATA_DIR) {
    print STDERR "No data directory or invalid directory specified\n\n";
    usage();
}

if ($DATA_DIR =~ /(tzdata(\d{4})(\w?))/) {
    $RAW_VERSION = $1;
    $VERSION_YEAR = $2;
    $VERSION_SUFFIX = $3;
    if ($YEAR != $VERSION_YEAR) {
        print STDERR "WARNING: You appear to be building $VERSION_YEAR data. Don't you want to use current $YEAR data?\n";
        usage(); # Add an override option for this check, if needed
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

# Output file name
$OUT = 'tz.txt';

# Separator between fields in the output file
$SEP = ','; # Don't use ':'!

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

@MONTH = qw(jan feb mar apr may jun
            jul aug sep oct nov dec);

main();

sub usage {
    print STDERR "Usage: $0 data_dir\n\n";
    print STDERR "data_dir contains the unpacked files from\n";
    print STDERR "ftp://elsie.nci.nih.gov/pub/tzdataYYYYR,\n";
    print STDERR "where YYYY is the year and R is the revision\n";
    print STDERR "letter.\n";
    print STDERR "\n";
    print STDERR "Files that are expected to be present are:\n";
    print STDERR join(", ", @FILES), "\n";
    exit 1;
}

sub main {
    my (%ZONES, %RULES, @EQUIV);

    print "Reading";
    foreach (@FILES) {
        if (! -e "$DATA_DIR/$_") {
            print STDERR "\nMissing file $DATA_DIR/$_\n\n";
            usage();
        }
        print ".";
        TZ::ParseFile("$DATA_DIR/$_", \%ZONES, \%RULES, $YEAR);
    }
    print "done\n";

    TZ::Postprocess(\%ZONES, \%RULES);

    print
        "Read ", scalar keys %ZONES, " current zones and ",
        scalar keys %RULES, " rules for $YEAR\n";

    # Write out the zone data in a compact readable format.

    # Create a name table from the zone names.  The format of
    # the name table is:
    #
    # The names are listed in lexical order, and each name
    # is assigned an offset.  The first name's offset is 0.
    # The offset of name i+1 is the offset of name i + the
    # length of name i + 1 (for the zero byte).
    #
    # Store the offsets in a hash %NAME_OFFSET.  Store the
    # names in a big scalar, $NAME_LIST, with "\n" between
    # each name and after the last.
    #
    # Store the length of the entire name table in $NAME_SIZE.
    #
    # Also, count the number of standard and DST zones.
    my $offset = 0;
    my $NAME_LIST = '';
    my %NAME_OFFSET;
    my $STD_COUNT = 0; # Count of standard zones
    my $DST_COUNT = 0; # Count of DST zones
    foreach my $z (sort keys %ZONES) {
        $NAME_OFFSET{$z} = $offset;
        $offset += length($z) + 1;
        $NAME_LIST .= "$z\n";
        if ($ZONES{$z}->{rule} eq $TZ::STANDARD) {
            $STD_COUNT++;
        } else {
            $DST_COUNT++;
        }
    }
    my $NAME_SIZE = $offset;
    
    open(OUT,">$OUT") or die "Can't open $OUT for writing: $!";
    # Zone data version
    print OUT $VERSION_YEAR, " # ($RAW_VERSION) ftp://elsie.nci.nih.gov data version YEAR\n";
    print OUT $VERSION_SUFFIX, " # ($RAW_VERSION) ftp://elsie.nci.nih.gov data version SUFFIX\n";
    print OUT scalar keys %ZONES, " # total zone count\n";
    print OUT $NAME_SIZE, " # length of name table in bytes\n";

    # Output first the standard zones, then the dst zones.
    # Precede each list with the count of zones to follow,
    # and follow it with the keyword 'end'.
    for my $type (qw(standard dst)) {
        print OUT ($type eq 'standard'
            ? $STD_COUNT : $DST_COUNT), " # count of $type zones to follow\n";
        foreach my $z (sort keys %ZONES) {
            my $isStd = ($ZONES{$z}->{rule} eq $TZ::STANDARD);
            next if ($isStd ne ($type eq 'standard'));
            print OUT $NAME_OFFSET{$z}, ",";
            print OUT formatZone($z, $ZONES{$z}, \%RULES), "\n";
        }
        print OUT "end\n"; # 'end' keyword for error checking
    }

    # Output the name table, followed by 'end' keyword
    print OUT scalar keys %ZONES, " # count of names to follow\n";
    print OUT $NAME_LIST, "end\n";

    close(OUT);
    print "$OUT written.\n";

    if (0) {
        TZ::FormZoneEquivalencyGroups(\%ZONES, \%RULES, \@EQUIV);
        print
            "Equivalency groups (including unique zones): ",
            scalar @EQUIV, "\n";
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

# Format a time zone as a machine-readable line of text.  Another
# tool will read this line to construct a binary data structure
# representing this zone.
# Param: Zone name
# Param: Zone hash
# Param: Ref to hash of all rules
# Return: One line description of this zone.
sub formatZone { # ($z, $ZONES{$z}, \%RULES)
    my $name = shift;
    my $zone = shift;
    my $rules = shift;

    my @spec;
    my @notes = ( $name );
    
    # GMT offset
    push @notes, ($zone->{gmtoff}=~/^-/?"GMT":"GMT+") . $zone->{gmtoff};
    push @spec, parseOffset($zone->{gmtoff});

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
        
        formatRule($rule->[0], \@spec, \@notes); # Onset
        formatRule($rule->[1], \@spec, \@notes); # Cease

        my @a = parseTime($rule->[0]->{save});
        if ($a[1] ne 'w') {
            die "Strange DST savings value: \"$rule->[0]->{save}\"";
        }
        push @notes, $rule->[0]->{save};
        push @spec, $a[0];
    }

    join($SEP, @spec) . " # " . join(' ', @notes);
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

# Parse an offset of the form d, d:dd, or d:dd:dd, or any of the above
# preceded by a '-'.  Return the total number of seconds represented.
# Param: String
# Return: Integer number of seconds
sub parseOffset {
    local $_ = shift;
    if (/^(-)?(\d{1,2})(:(\d\d))?(:(\d\d))?$/) {
        #        1   2      4        6
        my $a = (($2 * 60) + (defined $4?$4:0)) * 60 + (defined $6?$6:0);
        $a = -$a if (defined $1 && $1 eq '-');
        return $a;
    } else {
        die "Cannot parse offset \"$_\"";
    }
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

__END__
