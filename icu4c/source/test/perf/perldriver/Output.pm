#!/usr/local/bin/perl

#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************


use strict;

use Dataset;

my $TABLEATTR = 'BORDER="1" CELLPADDING="4" CELLSPACING="0"';
my $outType = "HTML";
my $html = "noName";
my $inTable;
my @headers;
my @timetypes = ("mean per op", "error per op", "events", "per event");
my %raw;
my $current = "";
my $exp = 0;
my $mult = 1e9; #use nanoseconds
my $perc = 100; #for percent
my $printEvents = 0;
my $legend = "<a name=\"Legend\">\n<h2>Table legend</h2></a><ul>";
my $legendDone = 0;
my %options;
my $operationIs = "operation";
my $eventIs = "event";

sub startTest {
  $current = shift;
  $exp = 0;
  outputData($current);
}

sub printLeg {
  if(!$legendDone) {
    my $message;
    foreach $message (@_) {
      $legend .= "<li>".$message."</li>\n";
    }
  }
}

sub startTable {
  #my $printEvents = shift;
  $inTable = 1;
  my $i;
  print HTML "<font size=3>";
  print HTML "<table $TABLEATTR>\n";
  if($#headers >= 0) {
    my ($header, $i);
    print HTML "<tr>";
    print HTML "<th><a href=\"#Test Name\">Test Name</a></th>";
    print HTML "<th><a href=\"#Ops\">Ops</a></th>";
    printLeg("<a name=\"Test Name\">Test Name</a> - name of the test as set by the test writer", "<a name=\"Ops\">Ops</a> - number of ".$operationIs."s per iteration");
    if(!$printEvents) {
      foreach $header (@headers) {
	print HTML "<th><a href=\"#meanop $header\">$header<br>mean/op</a></th>";
	print HTML "<th><a href=\"#errorop $header\">$header<br>error/op</a></th>";
	printLeg("<a name=\"meanop $header\">$header mean/op</a> - mean time for $header per $operationIs");
	printLeg("<a name=\"errorop $header\">$header error/op - error range for mean time");
      }
    }
    for $i (1 .. $#headers) {
      print HTML "<th><a href=\"#mean $i op\">ratio $i<br>mean/op</a></th>";
      print HTML "<th><a href=\"#error $i op\">ratio $i<br>error/op</a></th>";      
      printLeg("<a name=\"mean $i op\">ratio $i mean/op</a> - ratio of per $operationIs time, calculated as: (($headers[0] - $headers[$i])/$headers[$i])*100%, mean value");
      printLeg("<a name=\"error $i op\">ratio $i error/op</a> - error range of the above value");
    }
    if($printEvents) {
      foreach $header (@headers) {
	print HTML "<th><a href=\"#events $header\">$header<br>events</a></th>";
	printLeg("<a name=\"events $header\">$header events</a> - number of ".$eventIs."s for $header per iteration");
      }
      foreach $header (@headers) {
	print HTML "<th><a href=\"#mean ev $header\">$header<br>mean/ev</a></th>";
	print HTML "<th><a href=\"#error ev $header\">$header<br>error/ev</a></th>";      
	printLeg("<a name=\"mean $header mean/op - mean time for $header per $eventIs");
	printLeg("$header error/op - error range for mean time");
      }
      for $i (1 .. $#headers) {
	print HTML "<th><a href=\"#mean $i ev\">ratio $i<br>mean/ev</a></th>";
	print HTML "<th><a href=\"#error $i ev\">ratio $i<br>error/ev</a></th>";      
	printLeg("<a name=\"mean $i ev\">ratio $i mean/ev</a> - ratio of per $eventIs time, calculated as: (($headers[0] - $headers[$i])/$headers[$i])*100%, mean value");
	printLeg("<a name=\"error $i ev\">ratio $i error/ev</a> - error range of the above value");
      }
    }
    
    
    
#     foreach $i (@timetypes) {
#       foreach $header (@headers) {
# 	print HTML "<th>$header<br>$i</th>" unless ($i =~ /event/ && !$printEvents);
#       }
#     }
    print HTML "</tr>\n";
  }
  $legendDone = 1;
}

sub closeTable {
  if($inTable) {
    undef $inTable;
    print HTML "</tr>\n";
    print HTML "</table>\n";
    print HTML "</font>";
  }
}

sub newRow {
  if(!$inTable) {
    startTable;
  } else {
    print HTML "</tr>\n";
  }
  print HTML "<tr>";
}

sub outputData {
  if($inTable) {
    my $msg = shift;
    my $align = shift;
    print HTML "<td";
    if($align) {
      print HTML " align = $align>";
    } else {
      print HTML ">";
    }
    print HTML "$msg";
    print HTML "</td>";
  } else {
    my $message;
    foreach $message (@_) {
      print HTML "$message";
    }
  }
}

sub setupOutput {
  my $date = localtime;
  my $options = shift;
  %options = %{ $options };
  my $title = $options{ "title" };
  my $headers = $options{ "headers" };
  if($options{ "operationIs" }) {
    $operationIs = $options{ "operationIs" };
  }
  if($options{ "eventIs" }) {
    $eventIs = $options{ "eventIs" };
  }
  @headers = split(/ /, $headers);
  my ($t, $rest);
  ($t, $rest) = split(/\.\w+/, $0);
  $t =~ /^.*\W(\w+)$/;
  $t = $1;
  if($outType eq 'HTML') {
    $html = $date;
    $html =~ s/://g; # ':' illegal
    $html =~ s/\s*\d+$//; # delete year
    $html =~ s/^\w+\s*//; # delete dow
    $html = "$t $html.html";
    if($options{ "outputDir" }) {
      $html = $options{ "outputDir" }."/".$html;
    }

    open(HTML,">$html") or die "Can't write to $html: $!";

    print HTML <<EOF;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
   "http://www.w3.org/TR/html4/strict.dtd">
<HTML>
   <HEAD>
      <TITLE>$title</TITLE>
<!--
<style>
<!--
td { text-align: "." }
td:before { content: "$" }
-->
</style>
-->
   </HEAD>
   <BODY>
EOF
    print HTML "<H1>$title</H1>\n";

    #print HTML "<H2>$TESTCLASS</H2>\n";
  }
}

sub closeOutput {
  if($outType eq 'HTML') {
    if($inTable) {
      closeTable;
    }
    $legend .= "</ul>\n";
    print HTML $legend;
    printRaw();
    print HTML <<EOF;
   </BODY>
</HTML>
EOF
    close(HTML) or die "Can't close $html: $!";
  }
}


sub printRaw {
  print HTML "<h2>Raw data</h2>";
  my $key;
  my $i;
  my $j;
  my $k;
  print HTML "<table $TABLEATTR>\n";
  for $key (sort keys %raw) {
    my $printkey = $key;
    $printkey =~ s/\<br\>/ /;
    if($printEvents) {
      print HTML "<tr><td colspan = 7>$printkey</td></tr>\n"; # locale and data file
      print HTML "<tr><th>test name</th><th>interesting arguments</th><th>iterations</th><th>operations</th><th>mean time (ns)</th><th>error (ns)</th><th>events</th></tr>\n";
    } else {
      print HTML "<tr><td colspan = 6>$printkey</td></tr>\n"; # locale and data file
      print HTML "<tr><th>test name</th><th>interesting arguments</th><th>iterations</th><th>operations</th><th>mean time (ns)</th><th>error (ns)</th></tr>\n";
    }
      

    for $i ( $raw{$key} ) {
      print HTML "<tr>";
      for $j ( @$i ) {
	my ($test, $args);
	($test, $args) = split(/,/, shift(@$j));
	print HTML "<td>".$test."</td>";
	print HTML "<td>".$args."</td>";
	
	#print HTML "<td>".shift(@$j)."</td>";
	print HTML "<td align=\"right\">".shift(@$j)."</td>";
	print HTML "<td align=\"right\">".shift(@$j)."</td>";
	my @data = @{ shift(@$j) };
# 	for $k (@data) {
# 	  print HTML "$k, ";
# 	}
	my $ds = Dataset->new(@data);
	print HTML "<td align=\"right\">".formatNumber(4, $mult, $ds->getMean)."</td><td align=\"right\">".formatNumber(4, $mult, $ds->getError)."</td>";
	if($#{ $j } >= 0) {
	  print HTML "<td align=\"right\">".shift(@$j)."</td>";
	}
	print HTML "</tr>\n";
      }
    }
#    print HTML "<br>\n";
  }
#  print %raw;
}

sub store {
  $raw{$current}[$exp++] = [@_];
}

sub outputRow {
  #$raw{$current}[$exp++] =  [@_];
  my $testName = shift;
  my @iterPerPass = @{shift(@_)};
  my @noopers =  @{shift(@_)};
   my @timedata =  @{shift(@_)};
  my @noevents;
  if($#_ >= 0) {
    @noevents =  @{shift(@_)};
  }
  if(!$inTable) {
    if(@noevents) {
      debug("Have events header\n");
      $printEvents = 1;
      startTable;
    } else {
      debug("No events header\n");
      startTable;
    }
  }
  debug("No events: @noevents, $#noevents\n");

  my $j;

  # Finished one row of results. Outputting
  newRow;
  outputData($testName, "LEFT");
  #outputData($iterCount);
  outputData($noopers[0], "RIGHT");

  if(!$printEvents) {
    for $j ( 0 .. $#timedata ) {
      my $perOperation = $timedata[$j]->divideByScalar($iterPerPass[$j]*$noopers[$j]); # time per operation
      #debug("Time per operation: ".formatSeconds(4, $perOperation->getMean, $perOperation->getError)."\n");
      outputData(formatNumber(2, $mult, $perOperation->getMean), "RIGHT");
      outputData(formatNumber(2, $mult, $perOperation->getError), "RIGHT");
    }
  }
  my $baseLinePO = $timedata[0]->divideByScalar($iterPerPass[0]*$noopers[0]);
  for $j ( 1 .. $#timedata ) {
    my $perOperation = $timedata[$j]->divideByScalar($iterPerPass[$j]*$noopers[$j]); # time per operation
    my $ratio = $baseLinePO->subtract($perOperation);
    $ratio = $ratio->divide($perOperation);
    outputData(formatPercent(2, $ratio->getMean), "RIGHT"); 
    outputData(formatPercent(2, $ratio->getError), "RIGHT"); 
  }   
  if (@noevents) {
    for $j ( 0 .. $#timedata ) {
      outputData($noevents[$j], "RIGHT");
    }
    for $j ( 0 .. $#timedata ) {
      my $perEvent =  $timedata[$j]->divideByScalar($iterPerPass[$j]*$noevents[$j]); # time per event
      #debug("Time per operation: ".formatSeconds(4, $perEvent->getMean, $perEvent->getError)."\n");
      outputData(formatNumber(2, $mult, $perEvent->getMean), "RIGHT");
      outputData(formatNumber(2, $mult, $perEvent->getError), "RIGHT");
    }   
    my $baseLinePO = $timedata[0]->divideByScalar($iterPerPass[0]*$noevents[0]);
    for $j ( 1 .. $#timedata ) {
      my $perOperation = $timedata[$j]->divideByScalar($iterPerPass[$j]*$noevents[$j]); # time per operation
      my $ratio = $baseLinePO->subtract($perOperation);
      $ratio = $ratio->divide($perOperation);
      outputData(formatPercent(2, $ratio->getMean), "RIGHT"); 
      outputData(formatPercent(2, $ratio->getError), "RIGHT"); 
    }   
  }
}


1;

#eof
