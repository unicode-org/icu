/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/Attic/readme.txt,v $
* $Date: 2003/03/18 16:38:11 $
* $Revision: 1.9 $
*
*******************************************************************************
*/

This file provides instructions for building and running the UnicodeTools, which
can be used to:

- build the Derived Unicode files in the UCD (Unicode Character Database),
- build the transformed UCA (Unicode Collation Algorithm) files needed by ICU.
- run consistency checks on beta releases of the UCD and the UCA , and


WARNING!!

- This is NOT production level code, and should never be used in programs.
- The API is subject to change without notice, and will not be maintained.
- The source is uncommented, and has many warts; since it is not production code,
  it has not been worth the time to clean it up.
- It will probably not work on Unix or Mac without changing the file separator.
- Currently it uses hard-coded directory names.
- The contents of multiple versions of the UCD must be copied to a local directory.


Instructions:

0. If you are using Eclipse for your IDE, look at the instructions on

http://oss.software.ibm.com/icu/docs/eclipse_howto/eclipse_howto.htm


1. You must edit UCD_Types.java at the top, to set the directories for the build:

    public static final String DATA_DIR = "C:\\DATA\\";
    public static final String UCD_DIR = BASE_DIR + "UCD\\";
    public static final String BIN_DIR = DATA_DIR + "BIN\\";
    public static final String GEN_DIR = DATA_DIR + "GEN\\";

Make sure that each of these directories exist. Also make sure that the following
exist:

<GEN_DIR>/DerivedData
<GEN_DIR>/DerivedData/ExtractedProperties
<UCD_DIR>/EXTRAS-Update


2. Download all of the UnicodeData files for each version into UCD_DIR.
The folder names must be of the form: "3.2.0-Update", so rename the folders on the
Unicode site to this format.


2a. If you are downloading any "incomplete" release (one that does not contain
a complete set of data files for that release, you need to also download the previous
complete release). Most of the N.M-Update directorys are complete, *except*:

    4.0-Update, which does not contain a copy of Unihan.txt
    3.1-Update, which does not contain a copy of BidiMirroring.txt


2b. If you are building any of the UCA tools, you need to get a copy of the UCA data file
from http://www.unicode.org/reports/tr10/#AllKeys. The default location for this is:

        BASE_DIR + "Collation\\allkeys" + VERSION + ".txt".
        
If you have it in a different location, change that value for KEYS in UCA.java, and 
the value for BASE_DIR


2c. Here is an example of the default directory structure with files:

C://DATA/

        BIN/
    
        Collation/
            allkeys-3.1.1.txt
        
        GEN/
            DerivedData/
                ExtractedProperties
        UCD/
            3.0.0-Update/
                Unihan-3.2.0.txt
                ...
            3.0.1-Update/
                ...
            3.1.0-Update/
                ...
            3.1.1-Update/
                ...
            3.2.0-Update/
                ...
            4.0.0-Update/
                ArabicShaping-4.0.0d14b.txt
                BidiMirroring-4.0.0d1b.txt
                ...
            EXTRAS-Update/


3. All of the following have "version X" in the options you give to Java (either on the 
command line, or in the Eclipse 'run' options. If you want a specific version
like 3.1.0, then you would write "version 3.1.1". If you want the latest version (4.0.0),
you can omit the "version X".


4. Running UCD, you will use com.ibm.text.UCD.Main as your main class.

4a. For each version, the tools build a set of binary data in BIN that contain
the information for that release. This is done automatically, or you can manually do it
with the options

  version X build

This builds an compressed format of all the UCD data (except blocks and Unihan)
into the BIN directory. Don't worry about the voluminous console messages, unless one says
"FAIL". 

4b. All of the generated files get a "d" version number, e.g. CaseFolding-4.0.0d3.txt.
To change the D version on generated files, edit the link in GenerateData.java:

    static final int dVersion = 2; // change to fix the generated file D version. If less than zero, no "d"
    
Note: if for any reason you modify the binary format of the BIN files, you also have
to bump the value in that file:

    static final byte BINARY_FORMAT = 8; // bumped if binary format of UCD changes


4c. To build all of the Unicode files for a particular version X, run

  version X all
  

4d. To build a particular file, like CaseFolding, use that file name instead of all

  version X CaseFolding
  

4e. To run basic consistency checking, run:

  version X verify

Don't worry about any console messages except those that say FAIL.


5. Running UCA, you will use com.ibm.text.UCA.Main as your main class.

5a. To build all the UCA files used by ICU, use the option:

    java <UCA>Main ICU