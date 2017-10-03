#! /usr/bin/perl -w

#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#

use strict;


my $locale = $ARGV[0];


my $long_name = `/home/weiv/src/icu/source/extra/colprobe/longname $locale`;
my $pageTitle = $locale."_collation";
my $filename = $pageTitle.".html";

open TABLE, ">$filename";


print TABLE <<"EndOfTemplate";
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>$pageTitle</title>
<style>
         <!--
         table        { border-spacing: 0; border-collapse: collapse; width: 100%; 
               border: 1px solid black }
td, th       { width: 10%; border-spacing: 0; border-collapse: collapse; color: black; 
               vertical-align: top; border: 1px solid black }
-->
     </style>
</head>

<body bgcolor="#FFFFFF">

<p><b><font color="#FF0000">Collation:</font> $locale ($long_name) <a href="http://oss.software.ibm.com/cgi-bin/icu/lx/en/?_=$locale">Demo</a>, 

<a href="../all_diff_xml/comparison_charts.html">Cover 
Page</a>, <a href="../all_diff_xml/index.html">Index</a></b></p>
<table>
  <tr>
EndOfTemplate

my $dirCommon = "common";
my $refCommon = $dirCommon."/UCARules.txt";
my $nameCommon = $dirCommon."/".$locale."_collation.html";
my $colorCommon = "#AD989D";

my $loc;

if(!(-e $nameCommon)) {
  $locale =~ /_/;
  $loc = $`;
  $nameCommon = "$dirCommon/$loc"."_collation.html";
}
print TABLE "    <th bgcolor=\"$colorCommon\">COMMON (<a href=\"$refCommon\">UCA</a> <a href=\"../$dirCommon/xml/$locale.xml\">xml</a>)</th>\n";

my $dirLinux = "linux";
my $refLinux = $dirLinux."/".$locale.".utf8_default_raw.html";
my $rawLinux = $dirLinux."/".$locale.".utf8_raw.html";
my $defLinux = $dirLinux."/".$locale;
my $nameLinux = "$dirLinux/$locale".".utf8_collation.html";
my $colorLinux = "#1191F1";

print TABLE "    <th bgcolor=\"$colorLinux\">LINUX (";
if (!(-e $nameLinux)) {
#try the variant that has @euro stuck in
  $nameLinux = "$dirLinux/$locale".'.utf8@euro_collation.html';
  if(-e $nameLinux) {
    $refLinux = $dirLinux."/".$locale.'.utf8@euro_default_raw.html';
    $rawLinux = $dirLinux."/".$locale.'.utf8@euro_raw.html';
  }
}
if (-e $nameLinux) {
    print TABLE "<a href=\"$rawLinux\">Ordering</a> <a href=\"$defLinux\">Definition</a> <a href=\"$refLinux\">base</a>";
} 

print TABLE " <a href=\"../$dirLinux/xml/$locale.xml\">xml</a>)</th>\n";

my $dirWin = "winxp";
my $refWin = $dirWin."/".$locale."_default_raw.html";
my $rawWin = $dirWin."/".$locale."_raw.html";
my $nameWin = "$dirWin/$locale"."_collation.html";
my $colorWin = "#98FB98";

print TABLE "    <th bgcolor=\"$colorWin\">WINDOWS ("; 
if (-e $nameWin) {
   print TABLE "<a href=\"$rawWin\">Ordering</a> <a href=\"$refWin\">base</a> ";
} 
print TABLE "<a href=\"../windows/xml/$locale.xml\">xml</a>)</th>\n";

print TABLE "  </tr>\n  <tr>";


readRules($nameCommon, "#AD989D", "Same as the UCA.");
readRules($nameLinux, "#1191F1", "No data available.");      
readRules($nameWin, "#98FB98", "No data available.");


print TABLE <<"EndOfFooter";
  </tr>
</table>

</body>
</html>
EndOfFooter


sub readRules {
  # readRules($file, $color)
  my $filename  = shift;
  my $color = shift;
  my $comment = shift;
  my $noLines = 0;
  my $printOut = 0;

  my $file;

  if(-e $filename) {
    open($file, "<$filename") || die "something very strange happened\n";
    print TABLE "<td bgcolor=\"$color\">\n";
    while (<$file>) {
      if (/\}\<br\>$/) {
        $printOut = 0;

      }
      if ($printOut) {
        print TABLE $_;
        $noLines++;
      }
      if (/Sequence/) {
        $printOut = 1;
        print "found sequence\n";
        $noLines = 0;
      }

    }
    if (!$noLines) {
      print TABLE "Same ordering as base\n";
    }
    print TABLE "</td>\n";
  } else {
    print TABLE "<td bgcolor=\"$color\">\n$comment</td>\n";
  }    
}


# Tasting of food product
# 650-574-4551 $50 1 hour


#     <td bgcolor="#AD989D">1.0-alpha</td>
#     <td bgcolor="#FF6633">1.0</td>
#     <td bgcolor="#FF6633">=</td>
#     <td bgcolor="#FF6633"><span title="006E {LATIN SMALL LETTER N}">&amp;n</span><br>
#       <span title="006E 0079 {LATIN SMALL LETTER N} {LATIN SMALL LETTER Y}">&nbsp;&nbsp;&lt;&nbsp;ny</span><br>

#       <span title="006E 006E 0079 {LATIN SMALL LETTER N} {LATIN SMALL LETTER N} {LATIN SMALL LETTER Y} / 006E 0079 {LATIN SMALL LETTER N} {LATIN SMALL LETTER Y}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;nny&nbsp;/&nbsp;ny</span><br>
#       <span title="006E 0059 {LATIN SMALL LETTER N} {LATIN CAPITAL LETTER Y}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;&lt;&lt;&nbsp;nY</span><br>
#     </td>
#     <td bgcolor="#FF6633">=</td>
#     <td bgcolor="#FFFF33">1.2</td>

#     <td bgcolor="#98FB98">Windows XP</td>
#     <td bgcolor="#FF6633">=</td>
#     <td bgcolor="#FF6633">=</td>
