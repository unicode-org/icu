#!/usr/local/bin/perl

use strict;

my $TABLEATTR = 'BORDER="1" CELLPADDING="4" CELLSPACING="0"';
my $outType = "HTML";
my $html = "noName";
my $inTable;
my @headers;
my @timetypes = ("per iteration", "per operation", "events", "per event");
my %raw;
my $current;
my $exp = 0;

sub startTest {
  $current = shift;
  $exp = 0;
  outputData($current);
}

sub startTable {
  my $printEvents = shift;
  $inTable = 1;
  print HTML "<table $TABLEATTR>\n";
  if($#headers >= 0) {
    my ($header, $i);
    print HTML "<tr>";
    print HTML "<th>Test Name</th>";
    print HTML "<th>Operations</th>";
    foreach $i (@timetypes) {
      foreach $header (@headers) {
	print HTML "<th>$header<br>$i</th>" unless ($i =~ /event/ && !$printEvents);
      }
    }
    print HTML "</tr>\n";
  }
}

sub closeTable {
  if($inTable) {
    undef $inTable;
    print HTML "</tr>\n";
    print HTML "</table>\n";
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
  my $message;
  if($inTable) {
    print HTML "<td>";
    foreach $message (@_) {
      print HTML "$message";
    }
    print HTML "</td>";
  } else {
    foreach $message (@_) {
      print HTML "$message";
    }
  }
}

sub setupOutput {
  my $date = localtime;
  my $options = shift;
  my %options = %{ $options };
  my $title = $options{ "title" };
  my $headers = $options{ "headers" };
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
  for $key (sort keys %raw) {
    print HTML $key;
     foreach $i ( 0 .. $#{ $raw{$key} } ) {
         print HTML " $i = $raw{$key}[$i]";
     }
    
    
  }
  print %raw;
}

sub outputRow {
  $raw{$current}[$exp++] =  [@_];
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
      startTable(1);
    } else {
      debug("No events header\n");
      startTable;
    }
  }
  debug("No events: @noevents, $#noevents\n");

  my $j;

  # Finished one row of results. Outputting
  newRow;
  outputData($testName);
  #outputData($iterCount);
  outputData($noopers[0]);
  for $j ( 0 .. $#timedata ) {
    my $perOperation = $timedata[$j]->divideByScalar($iterPerPass[$j]); # time per operation
    #debug("Time per operation: ".formatSeconds(4, $perOperation->getMean, $perOperation->getError)."\n");
    outputData(formatSeconds(4, $perOperation->getMean, $perOperation->getError));
  }
  for $j ( 0 .. $#timedata ) {
    my $perOperation = $timedata[$j]->divideByScalar($iterPerPass[$j]*$noopers[$j]); # time per operation
    #debug("Time per operation: ".formatSeconds(4, $perOperation->getMean, $perOperation->getError)."\n");
    outputData(formatSeconds(4, $perOperation->getMean, $perOperation->getError));
  }

  if(@noevents) {
    for $j ( 0 .. $#timedata ) {
      outputData($noevents[$j]);
    }

    for $j ( 0 .. $#timedata ) {
      my $perEvent =  $timedata[$j]->divideByScalar($iterPerPass[$j]*$noevents[$j]); # time per event
      #debug("Time per operation: ".formatSeconds(4, $perEvent->getMean, $perEvent->getError)."\n");
      outputData(formatSeconds(4, $perEvent->getMean, $perEvent->getError));
    }   
  }
}


1;

#eof
