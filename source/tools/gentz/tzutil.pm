######################################################################
# Copyright (C) 1999, International Business Machines
# Corporation and others.  All Rights Reserved.
######################################################################
# See: ftp://elsie.nci.nih.gov/pub/tzdata<year>
# where <year> is "1999b" or a similar string.
######################################################################
# This package contains utility functions for time zone data.
# Author: Alan Liu

######################################################################
# Zones - A time zone object is a hash with the following keys:
# {gmtoff} The offset from GMT, e.g. "-5:00"
# {rule}   The name of the rule, e.g. "-", "Canada", "EU", "US"
# {format} The local abbreviation, e.g. "E%sT"
# {until}  Data is good until this year, e.g., "2000".  Often blank.

# These correspond to file entries:
#|# Zone NAME           GMTOFF  RULES   FORMAT  [UNTIL]
#|Zone America/Montreal -4:54:16 -      LMT     1884
#|                      -5:00   Mont    E%sT

# Optionally, a zone may also have the key:
# {link}   An old name for this zone, e.g. "HST" (for Pacific/Honolulu)
# Links come from the file entries:
#|# Link    LINK-FROM               LINK-TO
#|Link      America/New_York        EST5EDT
#|Link      America/Chicago         CST6CDT

# The name of the zone itself is not kept in the zone object.
# Instead, zones are kept in a big hash.  The keys are the names; the
# values are references to the zone objects.  The big hash of all
# zones is referred to in all caps: %ZONES ($ZONES if it's a
# reference).

# Example: $ZONES->{"America/Los_Angeles"} =
#   'format' => 'P%sT'
#   'gmtoff' => '-8:00'
#   'link' => 'US/Pacific-New'
#   'rule' => 'US'
#   'until' => ''

######################################################################
# Rules - A time zone rule is an array with the following elements:
# [0] Onset rule
# [1] Cease rule
# [2] Encoded string

# The onset rule and cease rule have the same format.  They are each
# references to a hash with keys:
# {from}   Start year
# {to}     End year, or "only" or "max"
# {type}   Unknown, usually "-"
# {in}     Month, 3 letters
# {on}     Day specifier, e.g. "lastSun", "Sun>=1", "23"
# {at}     Time, e.g. "2:00", "1:00u"
# {save}   Amount of savings, for the onset; 0 for the cease
# {letter} Guess: the letter that goes into %s in the zone {format}

# These correspond to the file entries thus:
#|# Rule NAME FROM TO   TYPE IN  ON      AT   SAVE LETTER/S
#|Rule   US   1942 only -    Feb 9       2:00 1:00 W # War
#|Rule   US   1945 only -    Sep 30      2:00 0    S
#|Rule   US   1967 max  -    Oct lastSun 2:00 0    S
#|Rule   US   1967 1973 -    Apr lastSun 2:00 1:00 D
#|Rule   US   1974 only -    Jan 6       2:00 1:00 D
#|Rule   US   1975 only -    Feb 23      2:00 1:00 D
#|Rule   US   1976 1986 -    Apr lastSun 2:00 1:00 D
#|Rule   US   1987 max  -    Apr Sun>=1  2:00 1:00 D

# Entry [2], the encoded string, is used to see if two rules are the
# same.  It consists of "[0]->{in},[0]->{on},[0]->{at},[0]->{save};
# [1]->{in},[1]->{on},[1]->{at}".  Note that the separator between
# values is a comma, between onset and cease is a semicolon.  Also
# note that the cease {save} is not used as this is always 0.  The
# whole string is forced to lowercase.

# Rules don't contain their own name.  Like zones, rules are kept in a
# big hash; the keys are the names, the values the references to the
# arrays.  This hash of all rules is referred to in all caps, %RULES
# or for a reference, $RULES.

# Example: $RULES->{"US"} =
#   0  HASH(0x8fa03c)
#      'at' => '2:00'
#      'from' => 1987
#      'in' => 'Apr'
#      'letter' => 'D'
#      'on' => 'Sun>=1'
#      'save' => '1:00'
#      'to' => 'max'
#      'type' => '-'
#   1  HASH(0x8f9fc4)
#      'at' => '2:00'
#      'from' => 1967
#      'in' => 'Oct'
#      'letter' => 'S'
#      'on' => 'lastSun'
#      'save' => 0
#      'to' => 'max'
#      'type' => '-'
#   2  'apr,sun>=1,2:00,1:00;oct,lastsun,2:00'

package TZ;
use strict;
use Carp;
use vars qw(@ISA @EXPORT $VERSION $STANDARD);
require 'dumpvar.pl';

@ISA = qw(Exporter);
@EXPORT = qw(ZoneEquals
             RuleEquals
             FormZoneEquivalencyGroups
             );
$VERSION = '0.1';

$STANDARD = '-'; # Name of the Standard Time rule

######################################################################
# Param: zone object (hash ref)
# Param: zone object (hash ref)
# Param: ref to hash of all rules
# Return: true if two zones are equivalent
sub ZoneEquals {
    my $z1 = shift;
    my $z2 = shift;
    my $RULES = shift;

    ($z1, $z2) = ($z1->{rule}, $z2->{rule});

    return ($z1 eq $z2) || 
        RuleEquals($RULES->{$z1}, $RULES->{$z2});
}

######################################################################
# Param: rule object (hash ref)
# Param: rule object (hash ref)
# Return: true if two rules are equivalent
sub RuleEquals {
    my $r1 = shift;
    my $r2 = shift;

    # Just compare the precomputed encoding strings.
    # defined() catches undefined rules.  The only undefined
    # rule is $STANDARD; any others would be cause by
    # Postprocess().
    return defined($r1) && defined($r2) && $r1->[2] eq $r2->[2];

    # There's actually one more level of equivalency analysis we could
    # do.  This is to recognize that Sun >=1 is the same as First Sun.
    # We don't do this yet.
}

######################################################################
# Given a hash of all zones and a hash of all rules, create a list
# of equivalency groups.  These are groups of zones with the same
# offset and equivalent rules.   Equivalency is tested with
# ZoneEquals and RuleEquals.  The resultant equivalency list is an
# array of refs to groups.  Each group is an array of one or more
# zone names.
# Param: IN  ref to hash of all zones
# Param: IN  ref to hash of all rules
# Param: OUT ref to array to receive group refs
sub FormZoneEquivalencyGroups {
    my ($ZONES, $RULES, $EQUIV) = @_;

    # Group the zones by offset.  This improves efficiency greatly;
    # instead of an n^2 computation, we just need to do n^2 within
    # each offset; a much smaller total number.
    my %ZONES_BY_OFFSET;
    foreach (keys %$ZONES) {
        push @{$ZONES_BY_OFFSET{$ZONES->{$_}->{gmtoff}}}, $_;
    }

    # Find equivalent rules
    foreach my $gmtoff (keys %ZONES_BY_OFFSET) {
        # Make an array of equivalency groups
        # (array of refs to array of names)
        my @equiv;
        foreach my $name1 (@{$ZONES_BY_OFFSET{$gmtoff}}) {
            my $found = 0;
            foreach my $group (@equiv) {
                my $name2 = $group->[0];
                if (ZoneEquals($ZONES->{$name1}, $ZONES->{$name2}, $RULES)) {
                    push @$group, $name1;
                    $found = 1;
                    last;
                }
            }
            if (!$found) {
                my @newGroup = ( $name1 );
                push @equiv, \@newGroup;
            }
        }
        push @$EQUIV, @equiv;
    }
}
