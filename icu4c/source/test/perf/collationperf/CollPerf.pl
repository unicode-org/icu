#!perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2008, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************

require "../perldriver/Common.pl";

use lib '../perldriver';

my $p;
if ($OnWindows) {
	$p = $ICUPathLatest."/collationperf/Release/collationperf.exe";
} else {
	$p = $ICUPathLatest."/collationperf/collperf";
}

my @locale=("en_US",
			"da_DK",
			"de_DE",
			"fr_FR",
			"ja_JP",
			"ja_JP",
			"ja_JP",
			"ja_JP",
			"zh_CN",
			"zh_CN",
			"zh_CN",
			"zh_TW",
			"zh_TW",
			"ko_KR",
			"ko_KR",
			"ru_RU",
			"ru_RU",
			"th_TH",
			"th_TH");
			
my $file=$CollationDataPath."TestNames_";

my @data=($file."Latin.txt",
		  $file."Latin.txt",
		  $file."Latin.txt",
		  $file."Latin.txt",
		  $file."Latin.txt",,
		  $file."Japanese_h.txt",
		  $file."Japanese_k.txt",
		  $file."Asian.txt",
		  $file."Latin.txt",
		  $file."Chinese.txt",
		  $file."Simplified_Chinese.txt",
		  $file."Latin.txt",
		  $file."Chinese.txt",
		  $file."Latin.txt",
		  $file."Korean.txt",
		  $file."Latin.txt",
		  $file."Russian.txt",
		  $file."Latin.txt",
		  $file."Thai.txt");

my @resultICU;
my @resultNIX;
my @resultPER;
my @resultFIN;
my $temp;

for ($n=0; $n<5; $n++) {
	@resultICU[$n]=@locale[$n].",".substr(@data[$n],15,65).",";
	@resultNIX[$n]=@locale[$n].",".substr(@data[$n],15,65).",";
	@resultFIN[$n]=@locale[$n].",".substr(@data[$n],15,65).",";
	
	#quicksort
	my @icu=`$p -locale @locale[$n] -loop 1000 -file @data[$n] -qsort`; 
	my @nix=`$p -locale @locale[$n] -unix -loop 1000 -file @data[$n] -qsort`;
	
	my @icua=split(' = ',$icu[2]);
	my @icub=split(' ',$icua[1]);
	my @nixa=split(' = ',$nix[2]);
	my @nixb=split(' ',$nixa[1]);
	
	@resultICU[$n]=@resultICU[$n].$icub[0].",";
	@resultNIX[$n]=@resultNIX[$n].$nixb[0].",";
	
	#keygen time
	@icu=`$p -locale @locale[$n] -loop 1000 -file @data[$n] -keygen`; 
	@nix=`$p -locale @locale[$n] -unix -loop 1000 -file @data[$n] -keygen`;
	
	@icua=split(' = ',$icu[2]);
	@icub=split(' ',$icua[1]);
	@nixa=split(' = ',$nix[2]);
	@nixb=split(' ',$nixa[1]);
	
	@resultICU[$n]=@resultICU[$n].$icub[0].",";
	@resultNIX[$n]=@resultNIX[$n].$nixb[0].",";
	
	#keygen len	
	@icua=split(' = ',$icu[3]);
	@nixa=split(' = ',$nix[3]);	
	
	chomp(@icua[1]); chomp(@nixa[1]);
	
	@resultICU[$n]=@resultICU[$n].$icua[1].",";
	@resultNIX[$n]=@resultNIX[$n].$nixa[1].",";
	
	my @resultSplitICU;
	my @resultSplitNIX;
	
	#percent
	for ($i=0;$i<3;$i++) {
		my $percent = 0;
		@resultSplitICU=split(',',@resultICU[$n]);
		@resultSplitNIX=split(',',@resultNIX[$n]);
		if (@resultSplitICU[2+$i] > 0) {
			$percent=substr((((@resultSplitNIX[2+$i]-@resultSplitICU[2+$i])/@resultSplitICU[2+$i])*100),0,7);
		}
		@resultPER[$n]=@resultPER[$n].$percent."%,";
	}
	
	#store ICU result
	for ($j=0;$j<3;$j++) {
		@resultFIN[$n]=@resultFIN[$n].@resultSplitICU[2+$j].",";
	}
	
	#store Unix result
	for ($j=0;$j<3;$j++) {
		@resultFIN[$n]=@resultFIN[$n].@resultSplitNIX[2+$j].",";
	}
	
	#store Percent result
	@resultFIN[$n]=@resultFIN[$n].@resultPER[$n];

	@resultICU[$n]=@resultICU[$n]."\n";
	@resultNIX[$n]=@resultNIX[$n]."\n";
	@resultPER[$n]=@resultPER[$n]."\n";
	@resultFIN[$n]=@resultFIN[$n]."\n";
}

print"@resultICU";
print"@resultNIX";
print"@resultPER";
print"@resultFIN";

print "Done!\n";
exit(0);
