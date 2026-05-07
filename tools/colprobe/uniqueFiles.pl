#!/usr/bin/perl

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;

my $file;
my $secondfile;
my %secondfilelist;
my @same;
my %list;
my $samefile;

foreach $secondfile (@ARGV) {
  $secondfilelist{$secondfile} = "";
}

foreach $file (sort keys(%secondfilelist)) {
  if(exists $secondfilelist{$file}) {
    delete $secondfilelist{$file};
    foreach $secondfile (sort(keys %secondfilelist)) {
      #print "diffing: $file and $secondfile\n";
      if (!`diff $file $secondfile`) {
        #print "$file and $secondfile are the same\n";
        push @same, $secondfile;
      }
    }
#    if ($#same > -1) {
      print "Adding @same to $file\n";
      $list{$file} =  [@same] ;
      foreach $samefile (@same) {
        delete $secondfilelist{$samefile};
      }
      delete @same[0..$#same];
#    }
  }
}


my $i = 0;
my $j = 0;
 foreach $file (sort( keys %list)) {
   #print "$file -> "; #@{list{$file}}\n";
   print "<$file> <$j>\n";
   foreach $i ( 0 .. $#{ $list{$file} } ) {
     #print "$list{$file}[$i] ";
     print "<$list{$file}[$i]> <$j>\n ";
   }
   $j++;
 }

