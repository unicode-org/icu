/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/Attic/readme.txt,v $
* $Date: 2003/03/17 23:03:13 $
* $Revision: 1.5 $
*
*******************************************************************************
*/

WARNING!!

These directories contain some Unicode tools used to build various files,
and to check the consistency of the Unicode releases.

- They are NOT production level code, and should never be used in programs.
- The API is subject to change without notice, and will not be maintained.
- The source is uncommented, and not well structured -- classic spaghetti style.
- There is no build mechanism.
- I have not checked to make sure it works on Unix; probably the only change that
  needs to be made is to fix the file separator.

Instructions:

1. You must edit UCD_Types at the top, to set the directories for the build:

    public static final String DATA_DIR = "C:\\DATA\\";
    public static final String BIN_DIR = DATA_DIR + "BIN\\";
    public static final String GEN_DIR = DATA_DIR + "GEN\\";

Make sure that each of these directories exist. Also make sure that the following
exist:

<GEN_DIR>/DerivedData
<GEN_DIR>/DerivedData/ExtractedProperties


2. Download all of the UnicodeData files for each version into DATA_DIR
The folder names must be of the form: "3.2.0-Update", so rename the folders on the Unicode site to this format.

2a. If you are downloading any "incomplete" release (one that does not contain
a complete set of data files for that release, you need to also download the previous
complete release). All of the N.M-Update directorys are complete, *except* for 4.0-Update,
which does not contain a copy of Unihan.txt.


2b. If you are building any of the UCA tools, you need to get a copy of the UCA data file
from http://www.unicode.org/reports/tr10/#AllKeys. The default location for this is:

        BASE_DIR + "Collation\\allkeys" + VERSION + ".txt".
        
If you have it in a different location, change that value for KEYS in UCA.java, and 
the value for BASE_DIR


3. For each version X (like 3.1.0), run

  java version X build

This builds an compressed format of all the UCD data (except blocks and Unihan)
into the BIN directory. Don't worry about the voluminous console messages, unless one says
"FAIL".


4. To build all of the files for a particular version X, run

  java version X all

To build a particular file, like CaseFolding, use that file name instead of all

  java version X CaseFolding

To change the D version, edit the link in GenerateData.java:

    static final int dVersion = 2; // change to fix the generated file D version. If less than zero, no "d"


5. To run basic consistency checking, run:

  java version X verify

Don't worry about any console messages except those that say FAIL.