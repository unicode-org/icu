Readme file for ICU time zone data (source/tools/gentz)


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
tz.dat into the icudata module.

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
   tz.bat.  The output of this step is the intermediate text file
   source/tools/gentz/tz.txt.

4. Run source/tools/makedata on Windows.  On UNIX systems the
   equivalent build steps are performed by 'make' and 'make install'.

The tz.txt file is typically checked into CVS, whereas the raw data
files are not, since they are readily available from the URL listed
above.


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
Non-system zones may try to use non-invariant characters, but they
shouldn't because of possible collisions with system IDs when the
invariant char converter is used (see TimeZone class for details).


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


Alan Liu 1999
