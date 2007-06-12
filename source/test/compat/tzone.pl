#!/usr/bin/perl

if ($ARGV[0] eq '-h' || $ARGV[0] eq '--help') {
	print "Usage: tzone [year month day]\n";
	exit(0);
}

my $LIBRARY = '../../lib';

my @TZONE_RAW = `locate zoneinfo`;
my @TZONE;
my $index = 0;
my $USECURRENT = 0;
my $year = 0;
my $month = 0;
my $day = 0;


if (scalar(@ARGV) == 3) {
	($year, $month, $day) = @ARGV;
	print "The date we are using is:  $month-$day-$year.\n";
} else {
	print "We are using the current date.\n";
	$USECURRENT = 1;
}

#filter out the time zones
foreach my $tzone (@TZONE_RAW) {
	chomp($tzone);
	if ($tzone =~ m|^/usr/share/zoneinfo/| && $tzone !~ m/tab$/) {
		if (-e $tzone) {
			$TZONE[$index] = substr($tzone, 20);
			$index++;
		}
	}
}

#go through each timezone and test 
$count = 0;
$ENV{'LD_LIBRARY_PATH'} = $LIBRARY;

print "The following time zones had wrong results.\n";

foreach my $tzone (@TZONE) {
	#set system time zone
	$ENV{'TZ'} = "$tzone";	
	
	my @result = `./tzdate $year $month $day $USECURRENT`;
	
	#if the result is wrong print the time zone information to a log file
	if (scalar(@result) > 0) {
		print "\n\nTIME ZONE: $tzone\n";
		print "@result\n";
		$count++;
	}
}

print "\nThe number of time zones with wrong results:  $count out of $index\n";

print("\n\nGood Bye!\n");
exit(0);
