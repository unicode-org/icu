#!/usr/bin/perl
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2004, International Business Machines Corporation and
#  * others. All Rights Reserved.
#  ********************************************************************

# Script to generate the ICULocaleData.jar file.  This file is
# part of icu4j.  It is checked into CVS.  It is generated from
# locale data in the icu4c project.  See usage() notes (below)
# for more information.

# This script requires perl.  For Win32, I recommend www.activestate.com.

# Alan Liu

use strict;
use warnings;
use File::Path;
use File::Copy;

my $isMSWin32 = ($^O eq 'MSWin32');
usage() unless (@ARGV >= ($isMSWin32 ? 3:2));
my $ICU_ROOT = shift;
my $ICU4J_ROOT = shift;
my $prefix = ''; # e.g. "LD_LIBRARY_PATH=..."
my $flavor = ''; # e.g. "Debug/"
if ($isMSWin32) {
    $flavor = shift() . '/';
} else {
    my $isDarwin = ($^O eq 'darwin');
    my $ldVar = ($isDarwin ? 'DYLD_LIBRARY_PATH' : 'LD_LIBRARY_PATH');
    $prefix = ($isMSWin32 ? '' : "$ldVar=$ICU_ROOT/source/common:$ICU_ROOT/source/i18n:$ICU_ROOT/source/tools/toolutil:$ICU_ROOT/source/data/out:$ICU_ROOT/source/data: ");
}
checkPlatform();

# Step 1.  Run genrb.
print "\n[Step 1: Run genrb]\n";
my $genrb = "$ICU_ROOT/source/tools/genrb/${flavor}genrb";
my $dataDir = "$ICU_ROOT/source/data/locales";
my $javaRootDir = "$dataDir/java";
my $pkg = "com/ibm/icu/impl/data";
my $javaDir = "$javaRootDir/$pkg";
chdir($dataDir);
mkpath($javaDir);
my $op = "$prefix$genrb -s. -d$javaDir -j -p com.ibm.icu.impl.data -b LocaleElements ";
print "{Command: $op*.txt}\n";
print "Directory: $dataDir\n";
my @list;
if (@ARGV) {
    @list = @ARGV;
    foreach (@list) { $_ .= ".txt" unless (/\.txt$/i); }
} else {
    @list = glob("*.txt");
}
my $count = 0;
my $errCount = 0;
foreach (sort @list) {
    cmd("$op $_", " $_ ");
    ++$count;
}

print "\nProcessed $count locale file(s)\n";

# Step 2.  Create LocaleElements_index.java.
print "\n[Step 2: Create LocaleElements_index.java]\n";
chdir("$ICU_ROOT/source/data/out/build");
cmd("$op res_index.txt");
chdir($javaDir);
my $f = "LocaleElements_index.java";
unlink $f if (-e $f);
rename "LocaleElements_res_index.java", $f;
patchIndex("LocaleElements_index.java");

# Step 3.  Find %%ALIAS tags.
# Assume that it looks like this:
#    public LocaleElements_no_NO_NY  () {
#          contents = new Object[][] { 
#                {
#                    "%%ALIAS",
#                    "nn_NO",
#                },
#            };
#        }
print "\n[Step 3: Scan for %%ALIAS tags]\n";
print "Directory: $javaDir\n";
chdir($javaDir);
@list = glob("LocaleElements*.java");
my %aliases;
foreach my $file (sort @list) {
    my $aliasOf = '';
    open(IN, $file) or die;
    while (<IN>) {
        if (/^\s*\"\%\%ALIAS\"/) {
            # This is an alias of the locale on the next line
            $aliasOf = <IN>;
            die "Can't parse $aliasOf" unless
                ($aliasOf =~ s/^\s*\"(.+?)\",\s*$/$1/);
            last;
        }
    }
    close(IN);

    if ($aliasOf) {
        my $me = $file;
        $me =~ s/^LocaleElements_(.+)\.java$/$1/i;
        $aliases{$me} = $aliasOf;
        print " $me is an alias of $aliasOf\n";
    }
}

# Step 4.  Fix %%ALIAS tags.
print "\n[Step 4: Fix %%ALIAS tags]\n";
my %patched; # Record any locales that we patch
foreach my $loc (sort keys %aliases) {
    # $loc is an alias of $aliases{$loc}
    # Make $loc point to package private static _contents of $aliases{$loc}
    my $aliasee = $aliases{$loc};
    if (!exists($patched{$aliasee})) {
        # Patch the alias
        #patchAliasee($aliasee);
        $patched{$aliasee} = 1;
    }
    patchAlias($loc, $aliasee);
}

# Step 5.  Patch transliteration resources.
# ICU resources have TransliterateLATIN but ICU4J resources expect Transliterate_LATIN
print "\n[Step 5: Patch transliteration resources]\n";
foreach my $file (sort @list) {
    my $hasTrans = 0;
    open(IN, $file) or die;
    while (<IN>) {
        # Ignore files that are already patched
        if (/^\s*\"Transliterate[^_].*\"/) {
            $hasTrans = 1;
            last;
        }
    }
    close(IN);

    patchTrans($file) if ($hasTrans);
}

# Step 6.  Compile .java files
print "\n[Step 6: Compile .java files]\n";
my $cmd = "javac -classpath $ICU4J_ROOT/classes:$javaRootDir:%CLASSPATH% $pkg/*.java";
chdir($javaRootDir);
print "Directory: $javaRootDir\n";
cmd($cmd);

# Step 7.  Update .jar file.  Do a "jar u" to update the existing file.
print "\n[Step 7: Update .jar file]\n";
my $jarFile = "$ICU4J_ROOT/src/$pkg/ICULocaleData.jar";
my $filesToBePackaged= "$pkg/*.class $pkg/*.col $pkg/*.brk $pkg/*.utf8";
$cmd = "jar uf $jarFile $filesToBePackaged";
# Do jar command
print "Directory: $javaRootDir\n";
chdir($javaRootDir);
if(-e "$jarFile"){
   if (! -e "$jarFile.orig") {
       copy("$jarFile","$jarFile.orig");
   }
}else{
   $jarFile ="$ICU_ROOT/source/data/locales/java/ICULocaleData.jar";
   $cmd = "jar cvf $jarFile $filesToBePackaged";
}
cmd($cmd);
print " $jarFile updated\n";

# Done!
print "\n[All done]\n";
checkPlatform();
exit(0);

#-----------------------------------------------------------------------
# Execute a command
# Param: Command
# Param: Display line, or '' to display command
sub cmd {
    my $cmd = shift;
    my $prompt = shift;
    if ($prompt) {
        print $prompt;
    } else {
        print "{Command: $cmd}..";
    }
    my_system($cmd);
    my $exit_value  = $? >> 8;
    #my $signal_num  = $? & 127;
    #my $dumped_core = $? & 128;
    if ($exit_value == 0) {
        print "ok\n" unless ($prompt);
    } else {
        ++$errCount;
        print "ERROR ($exit_value)\n";
        exit(1);
    }
}

# A system()-like sub that does NOT ignore SIGINT
sub my_system {
    my $pid = fork;
    if (! defined $pid) {
        return -1;
    } elsif ($pid) {
        return waitpid($pid, 0);
    } else {
        exec(@_) or exit $!;
    }
}

#-----------------------------------------------------------------------
# Patch the file that an %%ALIAS tag points to
sub patchAliasee {
    my $loc = shift;
    my $file = "LocaleElements_$loc.java";
    my $omitNextBrace = 0;
    open(IN, $file) or die;
    open(OUT, ">$file.new") or die;
    while (<IN>) {
        #if (/^\s*data\s*=\s*new\s+Object/) {
         #   print OUT "        super.contents = data;\n";
       #     print OUT "    };\n";
       #     print OUT '    static final Object[][] data =', "\n";
       #     s/^\s*contents\s*=\s*/        /;
       #     print OUT;
       # } elsif (/^\s*\}\s*;/) {
       #     # Omit the "}" after this
       #     print OUT;
       #     $omitNextBrace = 1;
       # } elsif ($omitNextBrace && /^\s*\}\s*$/) {
       #     # Omit it
       #     $omitNextBrace = 0;
       # } else {
            print OUT;
       # }
    }
    close(IN);
    close(OUT);
    unlink($file);
    rename("$file.new", $file);
    print " $file patched (aliasee)\n";
}

#-----------------------------------------------------------------------
# Patch the file that contains the %%ALIAS tag
sub patchAlias {
    my $loc = shift;
    my $aliasee = shift;
    my $file = "LocaleElements_$loc.java";
    open(IN, $file) or die;
    open(OUT, ">$file.new") or die;
    my $var = "static final Object";
    while (<IN>) {
	if(/$var/){
            # Output our new data
            print OUT "          static final Object[][] data  = LocaleElements_$aliasee.data;\n";
	   #consume the next 3 lines
            <IN>;
            <IN>;
            <IN>;
	    <IN>;
            <IN>;
        } else {
            print OUT;
        }
    }
    close(IN);
    close(OUT);
    unlink($file);
    rename("$file.new", $file);
    print " $file patched (alias)\n";
}

#-----------------------------------------------------------------------
# Patch a file with a transliteration resource.
sub patchTrans {
    my $file = shift;
    open(IN, $file) or die;
    open(OUT, ">$file.new") or die;
    while (<IN>) {
        # This should look like "TransliterateFOO" but if underscores
        # have crept in, ignore them.
        s/^(\s*\"Transliterate)_*(.+?\")/$1_$2/;
        print OUT;
    }
    close(IN);
    close(OUT);
    unlink($file);
    rename("$file.new", $file);
    print " $file patched (trans)\n";
}

#-----------------------------------------------------------------------
# Patch the index file, renaming res_index to index
sub patchIndex {
    my $file = shift;
    open(IN, $file) or die;
    open(OUT, ">$file.new") or die;
    while (<IN>) {
        s/res_(index)/$1/;
        print OUT;
    }
    close(IN);
    close(OUT);
    unlink($file);
    rename("$file.new", $file);
    print " $file patched (index)\n";
}

#-----------------------------------------------------------------------
sub checkPlatform {
    my $is_big_endian = unpack("h*", pack("s", 1)) =~ /01/;
    if (!$is_big_endian) {
        print "*******\n";
        print "WARNING: You are running on a LITTLE ENDIAN machine.\n";
        print "WARNING: You cannot use the resulting ICULocaleData.jar\n";
        print "WARNING: *.col files will have incorrect byte order.\n";
        print "*******\n";
    }
}

#-----------------------------------------------------------------------
sub usage {
    print << "END";
Usage: genrbjar.pl <icu_root_dir> <icu4j_root_dir> [<locale>+]
       genrbjar.pl <icu_root_dir> <icu4j_root_dir> ('Debug' | 'Release') [<locale>+]

'Debug' or 'Release' is required on MSWin32, and absent on UNIX.

genrbjar creates the ICULocaleData.jar file in the icu4j project.  It
uses locale data files in the icu4c directory and processes them with
genrb to generate Java source.  It makes necessary edits to the Java
source, then compiles the Java to .class files, then creates a .jar
file.  The ICULocaleData.jar file is created in its correct location
within the icu4j directory structure.

Optionally, one or more locales may be specified on the command line.
If this is done, only those locales will be processed.  If no locales
are listed, all locales are processed.

Before running this tool, a JDK must be installed and the javac and
jar binaries for that JDK must be on the system path.
Examples:
 i) on Linux:  ./genrbjar.pl ~/icu ~/icu4j
 ii) on Win32: perl genrbjar.pl C:\\icu C:\\icu4j Debug

NOTE: You CANNOT use the ICULocaleData.jar created on little endian
machines (e.g. Win32) because the *.col files will have the wrong byte
order.  However, you can use the *.class files and look at the *.java
files.
END
  exit(0);
}

__END__
:endofperl
