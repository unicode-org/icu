## Copyright (C) 2016 and later: Unicode, Inc. and others.
## License & terms of use: http://www.unicode.org/copyright.html
##
## Copyright (c) 2002-2010, International Business Machines Corporation 
## and others. All Rights Reserved.

This directory contains sample code using ICU4C routines. Below is a
short description of the contents of this directory.

break   - demonstrates how to use BreakIterators in C and C++.

cal     - prints out a calendar. 

case    - demonstrates how to do Unicode case conversion in C and C++.

coll    - shows how collation compares strings

csdet   -  demonstrates using ICU's CharSet Detection API

date    - prints out the current date, localized. 

datecal - demonstrates how a calendar object provides information
    
datefmt - an exercise using the date formatting API

dtitvfmtsample - shows how date interval format uses predefined skeletons

dtptngsample - uses DateTimePatternGenerator to create customized date/time pattern
       
layout   - demonstrates the ICU LayoutEngine (obsolete)

legacy   - demonstrates using two versions of ICU in one application (obsolete)

msgfmt   - demonstrates the use of the Message Format

numfmt   - demonstrates the use of the number format

plurfmtsample - uses PluralFormat and Messageformat to get Plural Form
    
props    - demonstrates the use of Unicode properties

strsrch - demonstrates how to search for patterns in Unicode text using the usearch interface.

translit - demonstrates the use of ICU transliteration

uciter8 - demonstrates how to leniently read 8-bit Unicode text.

ucnv     - demonstrates the use of ICU codepage conversion

udata    - demonstrates the use of ICU low level data routines (reader/writer in 'all' MSVC solution)

ufortune - demonstrates packaging and use of resources in an application

ugrep    - demonstrates ICU Regular Expressions. 

uresb    - demonstrates building and loading resource bundles

ustring  - demonstrates ICU string manipulation functions


==
* Where can I find more sample code?

 - The "uconv" utility is a full-featured command line application.
   It is normally built with ICU, and is located in icu/source/extra/uconv

 - The "icu-demos" contains other applications and libraries not
   included with ICU.  You can check it out from https://github.com/unicode-org/icu-demos
   using github clone. See the README file for additional information.

==
* How do I build the samples?

 - See the Readme in each subdirectory

 To build all samples at once:

    Windows MSVC:   
            - build ICU
	    - open 'all' project file in 'all' subdirectory
            - build project
            - sample executables will be located in /x86/Debug folders of each sample subdirectory

    Unix:   - build and install (make install) ICU
            - be sure 'icu-config' is accessible from the PATH
            - type 'make all-samples' from this directory 
               (other targets:  clean-samples, check-samples)
      Note: 'make all-samples' won't work correctly in out of source builds.

      Note that legacy and layout are obsolete samples that may not compile or run without
            adjustments to their makefiles.
