#/usr/bin/perl

####################################################################################
# xml2res.pl:
# This tool invokes xml2txt and genrb to produce res files from xml files
# Author: Ram Viswanadha
#        
####################################################################################
use File::Find;
use File::Basename;
use IO::File;
use Cwd;
use File::Copy;
use Getopt::Long;
use File::Path;
use File::Copy;

GetOptions(
           "--lib=s" => \$envVar,
           "--icuroot=s" => \$icuRoot,
           "--xerces=s" => \$xercesBin,
           "--xml2txt=s" => \$xml2txt,
           "--genrb=s" => \$genrb,
           "--sourcedir=s" => \$sourceDir,
           "--destdir=s" => \$destDir);
           

usage() unless defined $icuRoot;
usage() unless defined $xercesBin;
usage() unless defined $sourceDir;
usage() unless defined $destDir;
usage() unless defined $xml2txt;
usage() unless defined $genrb;

# create a temp directory and copy all the txt files there
my $tempDir = $destDir."/temp";
mkpath($tempDir);
my $prefix;

# set up environment 
if($$^O =~ /win/){
    $prefix ="";
    cmd("set PATH=%PATH%;$icuRoot/bin;$xercesBin;");
}else{
    $prefix ="$ldVar=$ICU_ROOT/source/common:$ICU_ROOT/source/i18n:$ICU_ROOT/source/tools/toolutil:$ICU_ROOT/source/data/out:$ICU_ROOT/source/data: "
}

# create list of xml files
my @list;
if (@ARGV) {
    @list = @ARGV;
    foreach (@list) { $_ .= ".xml" unless (/\.xml$/i); }
} else {
    opendir(DIR,$sourceDir);
    @list = grep{/\.xml$/} readdir(DIR);
    closedir(DIR);
}

# now convert
foreach $item (@list){
    next if($item eq "." || $item eq "..");
    texify($item);
    $txt = $item;;
    $txt =~ s/xml$/txt/i;
    resify($txt);
}

# run the xml2txt converter
sub texify{
    my $infile = shift;
    my $xml2txtExec = $xml2txt."/xml2txt";
    cmd("$prefix $xml2txtExec --sourcedir $sourceDir --destdir $tempDir $infile");
}

# run genrb 
sub resify{
    my $infile = shift;
    my $genrbExec = $genrb."/genrb";
    cmd("$prefix $genrbExec --sourcedir $tempDir --destdir $destDir --encoding UTF8 $infile");
}

#-----------------------------------------------------------------------
# Execute a command
# Param: Command
# Param: Display line, or '' to display command
sub cmd {
    my $cmd = shift;
    my $prompt = shift;
    $prompt = "Command: $cmd.." unless ($prompt);
    print $prompt;
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
xml2res.pl 
Options:
        --lib=<environment variable for lib path> 
        --sourcedir=<directory> 
        --icuroot=<path to ICU's root directory> 
        --xerces=<path to bin directory of Xerces>
        --xml2txt=<path to xml2txt executatble>
        --genrb=<path to genrb executatble>

xml2res creates *.res file from *.xml files by invoking the respective tools
Optionally, one or more locales may be specified on the command line.
If this is done, only those locales will be processed.  If no locales
are listed, all locales are processed.

END
  exit(0);
}