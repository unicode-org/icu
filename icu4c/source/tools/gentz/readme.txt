Copyright (C) 1999-2001, International Business Machines Corporation 
and others.  All Rights Reserved.

Readme file for ICU time zone data (source/tools/gentz)

Alan Liu
Last updated 2 Feb 2001


RAW DATA
--------
The time zone data in ICU is taken from the UNIX data files at
ftp://elsie.nci.nih.gov/pub/tzdata<year>.  The other input to the
process is an alias table, described below.


BUILD PROCESS
-------------
Two tools are used to process the data into a format suitable for ICU:

   tz.pl    directory of raw data files + tz.alias -> tz.txt
   gentz    tz.txt -> tz.dat (memory mappable binary file)

After gentz is run, standard ICU data tools are used to incorporate
tz.dat into the icudata module.  The tz.pl script is run manually;
everything else is automatic.

In order to incorporate the raw data from that source into ICU, take
the following steps.

1. Download the archive of current zone data.  This should be a file
   named something like tzdata1999j.tar.gz.  Use the URL listed above.

2. Unpack the archive into a directory, retaining the name of the
   archive.  For example, unpack tzdata1999j.tar.gz into tzdata1999j/.
   Place this directory anywhere; one option is to place it within
   source/tools/gentz.

3. Run the perl script tz.pl, passing it the directory location as a
   command-line argument.  On Windows system use the batch file
   tz.bat.  Also specify one or more ourput files: .txt, .htm|.html,
   and .java.

   For ICU4C specify .txt; typically

     <icu>/source/data/misc/timezone.txt

   where icu is the ICU4C root directory.  Double check that this is
   the correct location and file name; they change periodically.

   It is useful to generate an html file.  After it is generated,
   review it for correctness.

   As the third argument, pass in "tz.java".  This will generate a
   java source file that will be used to update the ICU4J data.

4. Do a standard build.  The build scripts will automatically detect
   that a new .txt file is present and rebuild the binary data (using
   gentz) from that.

The .txt and .htm files and typically checked into CVS, whereas
the raw data files are not, since they are readily available from the
URL listed above.

Additional steps are required to update the ICU4J data.  First you
must have a current, working installation of icu4j.  These instructions
will assume it is in directory "/icu4j".

5. Copy the tz.java file generated in step 3 to /icu4j/tz.java.

6. Change to the /icu4j directory and compile the tz.java file, with
   /icu4j/classes on the classpath.

7. Run the resulting java program (again with /icu4j/classes on the
   classpath) and capture the output in a file named tz.tmp.

8. Open /icu4j/src/com/ibm/util/TimeZoneData.java.  Delete the section
   that starts with the line "BEGIN GENERATED SOURCE CODE" and ends
   with the line "END GENERATED SOURCE CODE".  Replace it with the
   contents of tz.tmp.  If there are extraneous control-M characters
   or other similar problems, fix them.

9. Rebuild icu4j and make sure there are no build errors.  Rerun all
   the tests in /icu4j/src/com/ibm/test/timezone and make sure they
   all pass.  If all is well, check the new TimeZoneData.java into
   CVS.


ALIAS TABLE
-----------
For backward compatibility, we define several three-letter IDs that
have been used since early ICU and correspond to IDs used in old JDKs.
These IDs are listed in tz.alias.  The tz.pl script processes this
alias table and issues errors if there are problems.


IDS
---
All *system* zone IDs must consist only of characters in the invariant
set.  See utypes.h for an explanation of what this means.  If an ID is
encountered that contains a non-invariant character, tz.pl complains.
Non-system zones may use non-invariant characters.


Etc/GMT...
----------
Users may be confused by the fact that various zones with names of the
form Etc/GMT+n appear to have an offset of the wrong sign.  For
example, Etc/GMT+8 is 8 hours *behind* GMT; that is, it corresponds to
what one typically sees displayed as "GMT-8:00".  The reason for this
inversion is explained in the UNIX zone data file "etcetera".
Briefly, this is done intentionally in order to comply with
POSIX-style signedness.  In ICU we reproduce the UNIX zone behavior
faithfully, including this confusing aspect.
