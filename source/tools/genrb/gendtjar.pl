#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2004, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************

# Script to generate the icudata.jar and testdata.jar files.  This file is
# part of icu4j.  It is checked into CVS.  It is generated from
# locale data in the icu4c project.  See usage() notes (below)
# for more information.

# This script requires perl.  For Win32, I recommend www.activestate.com.

# Ram Viswanadha
# copied heavily from genrbjar.pl
use File::Find;
use File::Basename;
use IO::File;
use Cwd;
use File::Copy;
use Getopt::Long;
use File::Path;
use File::Copy;


main();

#------------------------------------------------------------------
sub main(){

GetOptions(
         "--icu-root=s" => \$icuRootDir,
         "--jar=s" => \$jarDir,
         "--icu4j-root=s" => \$icu4jDir,
         "--version=s" => \$version
         );
usage() unless defined $icuRootDir;
usage() unless defined $jarDir;
  #usage() unless defined $icu4jRootDir;
  $icuswap = $icuRootDir."/bin/icuswap -tb";
  $tempDir =cwd();
  $tempDir .= "/temp";
  $version =~ s/\.//;
  $icu4jDataDir ="com/ibm/icu/impl/data/icudt".$version."b";
  $icu4jTestDataDir = "com/ibm/icu/dev/data/testdata";
  $icuDataDir =$icuRootDir."/source/data/out/build/icudt".$version.checkPlatform();
  $icuTestDataDir =$icuRootDir."/source/test/testdata/out/build/";
  convertData($icuDataDir, $icuswap, $tempDir, $icu4jDataDir);
  convertData($icuDataDir."/coll/", $icuswap, $tempDir, $icu4jDataDir."/coll");
  createJar("$jarDir/jar", "icudata.jar", $tempDir, $icu4jDataDir);

  convertTestData($icuTestDataDir, $icuswap, $tempDir, $icu4jTestDataDir);
  createJar("$jarDir/jar", "testdata.jar", $tempDir, $icu4jTestDataDir);
}

#-----------------------------------------------------------------------
sub createJar{
    local($jar, $jarFile, $tempDir, $dirToJar) = @_;
    chdir($tempDir);
    $command = "$jar cvf $jarFile -C $tempDir $dirToJar";
    cmd($command);
}
#-----------------------------------------------------------------------
sub checkPlatform {
    my $is_big_endian = unpack("h*", pack("s", 1)) =~ /01/;
    if ($is_big_endian) {
        return "b";
    }else{
        return "l";
    }
}
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
sub convertData{
    local($icuDataDir, $icuswap, $tempDir, $icu4jDataDir)  =@_;
    my $dir = $tempDir."/".$icu4jDataDir;
    # create the temp directory
    mkpath("$tempDir/$icu4jDataDir");
    # cd to the temp directory
    chdir($tempDir);

    my @list;
    opendir(DIR,$icuDataDir);
    print $icuDataDir;
    @list =  readdir(DIR);
    closedir(DIR);
    print "{Command: $op*.*}\n";
    my $op = $icuswap;
    $i=0;
    # now convert
    foreach $item (@list){
        next if($item eq "." || $item eq "..");
        next if($item =~ /^t_.*$\.res/ ||$item =~ /^translit_.*$\.res/   || $item =~ /$\.cnv/ ||
               $item=~/$\.crs/ || $item=~ /$\.txt/ || $item=~/coll/ || $item=~ /^zoneinfo/  ||
               $item=~/icudata\.res/ || $item=~/$\.exp/ || $item=~/$\.lib/ || $item=~/$\.obj/ ||
               $item=~/cnvalias\.icu/);

        $command = $icuswap." $icuDataDir/$item $tempDir/$icu4jDataDir/$item";
        cmd($command);

    }
    chdir("..");
    print "\nDONE\n";
}
#-----------------------------------------------------------------------
sub convertTestData{
    local($icuDataDir, $icuswap, $tempDir, $icu4jDataDir)  =@_;
    my $dir = $tempDir."/".$icu4jDataDir;
    # create the temp directory
    mkpath("$tempDir/$icu4jDataDir");
    # cd to the temp directory
    chdir($tempDir);
    print "{Command: $op*.*}\n";
    my $op = $icuswap;
    my @list;
    opendir(DIR,$icuDataDir);
    print $icuDataDir;
    @list =  readdir(DIR);
    closedir(DIR);

    $i=0;
    # now convert
    foreach $item (@list){
        next if($item eq "." || $item eq "..");
        next if($item =~ /$\.cnv/ || item=~/$\.crs/ || $item=~ /$\.txt/ ||
                $item=~/$\.exp/ || $item=~/$\.lib/ || $item=~/$\.obj/ ||
                $item=~/$\.mak/ || $item=~/test\.icu/);
        
        if($item =~ /^testdata_/){
            $file = $item;
            $file =~ s/testdata_//g;
            $command = "$icuswap $icuDataDir/$item $tempDir/$icu4jDataDir/$file";
            cmd($command);
        }

    }
    chdir("..");
    print "\nDONE\n";
}
#------------------------------------------------------------------------------------------------
sub cmd {
    my $cmd = shift;
    my $prompt = shift;
    $prompt = "Command: $cmd.." unless ($prompt);
    print $prompt."\n";
    system($cmd);
    my $exit_value  = $? >> 8;
    #my $signal_num  = $? & 127;
    #my $dumped_core = $? & 128;
    if ($exit_value == 0) {
        print "ok\n";
    } else {
        ++$errCount;
        print "ERROR ($exit_value)\n";
        exit(1);
    }
}
#-----------------------------------------------------------------------
sub usage {
    print << "END";
Usage:
gendtjar.pl
Options:
        --icu-root=<directory where icu4c lives>
        --jar=<directory where jar.exe lives>
        --icu4j-root=<directory>
        --version=<ICU4C version>
e.g:
gendtjar.pl --icu-root=\\work\\icu --jar=\\jdk1.4.1\\bin --icu4j-root=\\work\\icu4j --version=3.0
END
  exit(0);
}

