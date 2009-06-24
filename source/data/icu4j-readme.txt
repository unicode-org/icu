********************************************************************************
* Copyright (C) 2008, International Business Machines Corporation 	       *
* and others. All Rights Reserved.			      		       *
*									       *
* 6/26/08 - Created by Brian Rower - heavy copying from ICU4J readme & others  *
*									       *
********************************************************************************

Procedures for building ICU4J data from ICU4C data on a Unix system:


1. Download and build ICU4C. For more instructions on downloading and building
	ICU4C, see the ICU4C readme at:
	http://source.icu-project.org/repos/icu/icu/tags/release-3-8/readme.html#HowToBuild

2. In your shell, navigate to $icu4c_root/source/tools/genrb. $icu4c_root is 
	the root directory of ICU4C source package.

3. Create a new file named "Makefile.local" in this directory. In this file 
	set the ICU4J_HOME variable to be the root path of ICU4J. 
	Ex: ICU4J_HOME=/home/srl/icu4j

4. If there are spaces in your Java bin directory path (which is especially 
	common using cygwin)
	(EX: /cygdrive/c/Program Files/Java/jdk1.5.0_15/bin), you may need to 
	set GENDTJAR_JARHOME in Makefile.local. On a standard Unix based system, 
	with the Java bin directory in your PATH, this step is not required.

	You can set GENDTJAR_JARHOME by hard coding the path to the Java 
	bin directory.

	For example, on Cygwinc (notice the backslash used for the space):
		GENDTJAR_JARHOME=/cygdrive/c/Program\ Files/Java/jdk1.5.0_15/bin
	
5. In this same directory $icu4c_root/source/tools/genrb, 
	run the command 'make build-icu4j'

6. Build the resources target of the ICU4J ant build to unpack the jar files 
	with the following commands:
         cd $icu4j_root
         ant resources

********************************************************************************

If the above procedure fails to work, you may attempt to use the 
old procedure which uses less automation & path "guessing":

1. Download and build ICU4C. For more instructions on downloading and building
	ICU4C, see the ICU4C readme at:
	http://source.icu-project.org/repos/icu/icu/tags/release-3-8/readme.html#HowToBuild

2. Change directory to $icu4c_root/source/tools/genrb. $icu4c_root is the root 
	directory of ICU4C source package.

3. Run gendtjar.pl from that directory itself with the command:
	./gendtjar.pl 	--icu-root=$icu4c_root --jar=$jdk_home/bin 
			--icu4j-root=$icu4j_root

	e.g.
	./gendtjar.pl --icu-root=$HOME/icu4c --jar=/usr/local/bin/java/bin/ --icu4j-root=$HOME/icu4j

	Execution of gendtjar.pl script will create the required jar files in
	$icu4c_root/source/tools/genrb/temp and then copy them to their 
	final locations in the ICU4J structure:
		$icu4j_root/src/com/ibm/icu/impl/data 
	and 
		$icu4j_root/src/com/ibm/icu/dev/data.

4. Build resources target of ant to unpack the jar files 
	with the following commands:
        
	cd $icu4j_root
	ant resources

Note: if gendtjar.pl does not work, the --verbose option can help in 
	debugging why it went wrong.

********************************************************************************