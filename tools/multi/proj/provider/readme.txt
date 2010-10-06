#!/bin/cat
# Copyright (c) 2009-2010 IBM Corp. and Others. All Rights Reserved

# ICU Provider Feature. - $Id$

BUILDING:

1a. Download the ICU source (.tgz) you wish to TARGET (i.e. link your application against).  This must be in the 4.4. series. 

    4.4, 4.4.1, and 4.4.2 have been tested.  The latest available is recommended (4.4.2 as of this writing).

1b. Download one or more ICUs (.tgz) you wish to have AVAILABLE (via the provider interface).  As of this writing, 4.2.0.1, 3.8.1, 3.6, 3.4.1, and 3.2.1 have been tested.

   (Known issue: As of this writing, ICU 4.4+,4.5+ etc may NOT be an AVAILABLE locale (it may be a TARGET). )


   Note that the MAJOR+MINOR version numbers must not conflict between the TARGET  and AVAILABLE ICUs. Only one ICU of each major+minor is allowed. 
   So, only one 4.4.X, one 3.8.X, etc.  This is due to ICU binary compatibility rules.


1c. Copy the ICUs (named as they were downloaded) into the ../../packages/ directory relative to this readme.


2a. Copy the file "Makefile-local.sample" into a new file "Makefile.local".

2b. Edit the Makefile.local to modify the PROVIDER_TARGET and PROVIDER_AVAILABLE settings.


3. Check the makefile settings by running 'make info', you should see output similar to the following:

   ICU Provider Build
   Targetting Provider against ICU 4.4.2 (4_4_2, major 44)
   Available plugins: 3.8.1 4.2.0.1  (3_8_1 4_2_0_1)
   Available keywords:
      ...@provider=icu38
      ...@provider=icu42
   Plugin library will be libicuprov.44.so

   Available ICU tarballs: icu4c-3_8_1-src.tgz icu4c-4_2_0_1-src.tgz icu4c-4_4_2-src.tgz 
   Available ICU versions: 3.8.1 4.2.0.1 4.4.2 


 The last two lines show which ICU .tgz files are available. If a version is not listed, make sure the filename is as above.

4. Now, you are ready to build and test:

     make check

   This will take quite a while as it has to build N copies of ICU.

   When done, it should show a list of collation keys, and indicate that there were differences between ICU 3.8, 4.4, 4.2.   You may need to modify coldiff.cpp if you are using a different set of ICU versions.

5. You can test in an 'installed' ICU (not the system install) with this command:

      make install-check

DEPLOYING:

6.  To install the plugin in your own ICU, copy  out/icuplugins44.txt into your lib/icu,  and out/lib/libicuprov.44.so into your lib/ directories where ICU is installed. 

 Note that running 'icuinfo' will tell you where the plugin file is expected to be located, and 'icuinfo -L' will debug any load issues.

USING:

Collators opened with an id such as that shown in 'make info', such as  'en_US@provider=icu38' will load, for example, an ICU 3.8 collator.

