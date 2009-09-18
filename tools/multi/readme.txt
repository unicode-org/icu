#!/bin/cat
# Copyright (c) 2009 IBM Corp. and Others. All Rights Reserved
#

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
