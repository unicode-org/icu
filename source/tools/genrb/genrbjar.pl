#!/usr/bin/perl
#line 14

# Script to generate the ICULocaleData.jar file.  This file is
# part of icu4j.  It is checked into CVS.  It is generated from
# locale data in the icu4c project.  See usage() notes (below)
# for more information.

# This script requires perl.  For Unixes, I recommend www.activestate.com.

# Alan Liu

use File::Path;
use File::Copy;

usage() unless (@ARGV >= 3);
my $ICU_ROOT = shift;
my $ICU4J_ROOT = shift;
my $ldVar = shift;

# Step 1.  Run genrb.
my $genrb = "$ICU_ROOT/source/tools/genrb/genrb";
my $dataDir = "$ICU_ROOT/source/data/locales";
my $javaRootDir = "$dataDir/java";
my $pkg = "com/ibm/icu/impl/data";
my $javaDir = "$javaRootDir/$pkg";
chdir($dataDir);
mkpath($javaDir);
my $op = "$ldVar=$ICU_ROOT/source/common:$ICU_ROOT/source/i18n:$ICU_ROOT/source/tools/toolutil:$ICU_ROOT/source/data/out:$ICU_ROOT/source/data: $genrb -s. -d$javaDir -j -p com.ibm.icu.impl.data -b LocaleElements ";
print "Command: $op*.txt\n";
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
    cmd("$op $_", " $_...");
    ++$count;
}

print "Processed $count locale file(s)\n";

# Step 2.  Create LocaleElements_index.java.
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
print "Scanning for %%ALIAS tags\n";
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
my %patched; # Record any locales that we patch
foreach my $loc (sort keys %aliases) {
    # $loc is an alias of $aliases{$loc}
    # Make $loc point to package private static _contents of $aliases{$loc}
    my $aliasee = $aliases{$loc};
    if (!exists($patched{$alias})) {
        # Patch the alias
        #patchAliasee($aliasee);
        $patched{$aliasee} = 1;
    }
    patchAlias($loc, $aliasee);
}

# Step 5.  Patch transliteration resources.
# ICU resources have TransliterateLATIN but ICU4J resources expect Transliterate_LATIN
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
my $cmd = "javac -classpath $ICU4J_ROOT/classes:$javaRootDir:%CLASSPATH% $pkg/*.java";
chdir($javaRootDir);
print "Compiling .java files..";
print "Directory: $javaRootDir\n";
cmd($cmd);

# Step 7.  Create .jar file.  Since we don't yet generate correct
# CollationElement_*.res files, leave those as they are.  Do a
# "jar u" -- update the existing file.
my $jarFile = "$ICU4J_ROOT/src/$pkg/ICULocaleData.jar";
my $filesToBePackaged= "$pkg/*.class $pkg/*.col $pkg/*.brk $pkg/*.utf8";
my $cmd = "jar uf $jarFile $fileToBePackaged";
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

# Done!
print "All done.\n";
exit(0);

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
sub usage {
    print << "END";
Usage: genrbjar.pl <icu_root_dir> <icu4j_root_dir> <ld_path_variable_name> [<locale>+]

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
e.g: 
	i)  on MacOSX: ./genrbjar.pl /Users/build/ICU_MACOSX/icu /Users/build/icu4j DYLD_LIBRARY_PATH
	ii) on Linux: ./genrbjar.pl /Users/build/ICU_MACOSX/icu /Users/build/icu4j LD_LIBRARY_PATH
END
  exit(0);
}

__END__
:endofperl
