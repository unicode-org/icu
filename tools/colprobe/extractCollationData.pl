#!/usr/bin/perl

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;
use Unicode::UCD 'charinfo';
use Unicode::Normalize;
use utf8;
use open ':utf8';

my $printout = 0;
my $braces = 0;
my $colls = 0;
my $aliased = 0;
my $newName = "";
my $filename;
my $suffix;
my $locale;

NEW_FILE:
foreach my $arg (@ARGV) {
  if($newName =~ /^$/) {
    $locale = $arg;
    $locale =~ s#^.*/##g;
    $locale =~ s/\.txt//;
  } else {
    $newName = "";
  }
  my $command = "/home/weiv/build/current/bin/uconv -x hex-any/Java -f utf8 -t utf8 $arg";
  print $command."\n";
  my @bundle = `$command`;
  foreach $_ (@bundle) {
 #while(<>) {
   #print $ARGV if eof;
   if(/^\/\//) {
     next;
   }
   if(/collations/) {
     print "found Collations\n";
     $colls = 1;
     if(/alias/) {
       print "collations are aliased\n";
       $aliased = 1;
     }
   }
   if($aliased) {
     print "processing aliased data: $_\n";
     if(/\{/) {
       print "Braces opened\n";
       $braces = 1;
     }
     if($braces && /\"(.*)\"/) {
       $newName = $1;
       print "Aliasing to $newName\n";
     }
     if($braces && /\}/) {
       $braces = 0;
       print "Braces closed\n";
       $aliased = 0;
       print "Switching from $filename to $newName\n";
       $arg =~ s/$locale\.txt$/$newName\.txt/;
       print "$arg\n";
       redo NEW_FILE;
     }

   }
   if(/standard|phonebook|traditional|pinyin|stroke|direct/ && $colls) {
     print "found $& collation\n";
     $suffix = "_".uc($&);
     if(/standard/) {
       $suffix = "";
     }
   }
   if(/Sequence/ && $colls) {
     #binmode ARGV, ":utf8";
     $printout = 1;
     #$filename = $ARGV;
     $filename = $locale;
     if($suffix) {
       $filename .= "_".$suffix;
     }
     $filename .= "_collation.html";
     print "filename is $filename\n";
     #open(OUT, ">:utf8", "$filename");
     open(OUT, ">$filename");
     printHeading($arg);
     #next;
   }
   my $line = $_;
   if($line =~ /\{/ && $printout) {
     $braces++;
   }
   if($printout) {
     print OUT processLine($line);
     print OUT "\n";
   }
   if( $line =~ /\}/ && $printout) {
     $braces--;
     if($braces == 0) {
       $printout = 0;
       printFooting();
       close(OUT);
     }
   }
 } 
}

sub processLine {
  my $line = shift;
  $_ = $line;
  my $i = 0;
  my $j = 0;
  my $result;
# remove comments
  s#//.*$##g;
# remove "Sequence" if present
  s/Sequence\s*//;
# remove leading brace if present
  s/^\s*{//;
# remove trailing brace if present
  s/}\s*$//;
# remove trailing quote
  s/"\s*$//;
#remove lead quote
  s/^\s*"//;
#separate options
  s/(\[.*\])/\n\1/g;
#separate resets
  s/\s*\&\s*/\n\& /g;
#separate strengths and insert spaces
  s/\s*(<{1,4})\s*/\n\1 /g;
#separate equals and insert spaces
  s/\s*=\s*/\n= /g;

# break into individual reset/strength/setting lines
  my @lines = split(/\n/);

  my $line;
  my $name;
  my $spanEnd = "";
  my $result = "";
  my $names = "";
  my $codes = "";
  my $lrm = "";

  foreach $line (@lines) {
    # skip empty lines
    if($line =~ /^$/) {
      next;
    }
    $spanEnd = "";
    $name = "";
    $lrm = "";
    $line = NFC($line);
    # for resets and strengths we will get name for elements
    if($line =~ /<{1,4} |= |& \[.*\]|& /) {
      $name = "<span title=\"";
      $names = "";
      $codes = "";
      my $start = $&;
      my $rest = $';
      for ($j = 0; $j < length($rest); $j++) {
	my $char = substr($rest, $j, 1);
	my $charVal = ord($char);
	# some of elements are part of the syntax, so they are
	# entered without translation to the name
	if($charVal == 0x002F || $charVal == 0x007C) {
	  $name .= $codes.$names." $char ";
	  $codes = "";
	  $names = "";
	} elsif($charVal == 0x0027) { #quote requires more processing
	  #$name .= "'";
	} else {
	  my $charinfo = charinfo($charVal);
	  $codes .= $charinfo->{'code'}." ";
	  $names .= "{".$charinfo->{'name'}."} ";
          if($charinfo->{'bidi'} eq "R" || $charinfo->{'bidi'} eq "AL") {
            $lrm = "&lrm;";
          }
	  #$name .= $charinfo->{'code'}." {".$charinfo->{'name'}."} ";
	}
      }
      $name .= $codes.$names."\" >";
      $spanEnd = "</span>";
    }
    #print $name."\n";
    if($line =~ /^<<<</) {
      $line = "    $line";
    } elsif($line =~ /^<<</) {
      $line = "   $line";
    } elsif($line =~ /^<</) {
      $line = "  $line";
    } elsif($line =~ /^</) {
      $line = " $line";
    } elsif($line =~ /^=/) {
      $line = "    $line";
    }
    # insert spaces around vertical bars (fix prefixes)

    # insert spaces around slashes (fix expansions)
    $line =~ s#/# / #g;
    # replace &
    $line =~ s/\&/&amp;/g;
    # replace spaces
    $line =~ s/ /&nbsp;/g;
    # replace <
    $line =~ s/</&lt;/g;
    # replace >
    $line =~ s/>/&gt;/g;

    #$lines[$i] = $name.$lrm.$line."</span><br>"; 
    #$i++;
    $result .=  $name.$lrm.$line.$spanEnd."<br>\n";
  }

  #$_ = join("\n", @lines);
  return $result;

}
 
sub printHeading {
my $filename = shift;
$filename =~ s/\.txt//;
print OUT <<"EndOfHeading";
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
</head>
# Collation data resource bundle generated for locale: $filename<br>
# For platform icu reference platform UCA<br><br>


$filename&nbsp;{<br>
&nbsp;&nbsp;CollationElements&nbsp;{<br>
&nbsp;&nbsp;&nbsp;&nbsp;Sequence&nbsp;{<br>
EndOfHeading
}

sub printFooting {
print OUT <<"EndOfFooting";
&nbsp;&nbsp;&nbsp;&nbsp;}<br>
&nbsp;&nbsp;}<br>
}<br>

</pre>
</html>
EndOfFooting
}
