#!/bin/cat
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2009-2010 IBM Corp. and Others. All Rights Reserved
#


NOTE:
NOTE: For information about the 'Provider' feature, see proj/provider/readme.txt
NOTE:



 This directory contains pieces of the 'MultiIcu' feature, which contains
scripts for building multiple versions of ICU at the same time, and running
various pieces of code against it.

 Note, if you copy one of these projects outside the multi/ directory,
you must ensure that the 'MULTIICU_ROOT' variable in build scripts points
to the top level directory here, 'multi/'.

 multi/              ( You are Here )
 multi/packages/     .tgz and .jar files for C and J respectively
                        (see the readme in that directory)
 multi/c/            Scripts for building against ICU4C
 multi/c/patch/         Patches for building old ICU versions
 
 multi/common/       Common makefiles

 multi/j/            Scripts for building against ICU4J

 multi/tmp/          Temporary directory, contains intermediate builds
 multi/tmp/src/         Unpacked and patched source
 multi/tmp/build/       Built ICUs
 multi/tmp/inst/        Installed ICUs

 multi/proj/         Contains various projects which make use of multi-icu.
                       (See individual readmes under each project.)
 multi/proj/chello/  C "hello world" against multiple ICUs
 multi/proj/jhello/  J "hello world" against multiple ICUs

--
HOW TO USE

1. Copy some ICU source files into multi/packages/

2. in "multi/c"  run "make iicus" - this will take a while.

3.To verify the ICU build, in "multi/proj/chello",  run "make check".

   You should see output like this:

       out/3_8_1.txt:  ICU 3.8.1
       out/4_2_0_1.txt:  ICU 4.2
       out/4_4_1.txt:  ICU 4.4.1

4. The 'Provider' project is more complex. To see its information, see
the readme in that directory. 
