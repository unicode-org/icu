**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: August 18 2003
* Since: ICU 2.8
**********************************************************************

----------------------------------------------------------------------
OVERVIEW

This file describes the tools in icu/source/tools/gentz/tzcode

The purpose of these tools is to process the zoneinfo or "Olson" time
zone database into a form usable by ICU4C (release 2.8 and later).
Unlike earlier releases, ICU4C 2.8 supports historical time zone
behavior, as well as the full set of Olson compatibility IDs.

References:

ICU4C:  http://oss.software.ibm.com/icu/
Olson:  ftp://elsie.nci.nih.gov/pub/

----------------------------------------------------------------------
ICU4C vs. ICU4J

As of release 2.8, the ICU time zone implementations in C and Java
have diverged for various reasons.  The C implementation is a full
implementation, whereas ICU4J (as of 2.8) uses the underlying JDK
1.3 or 1.4 time zone implementation.

The "present day snapshot" only reflects current time zone behavior,
without historical variation.  Furthermore, it lacks the full set of
Olson compatibility IDs.  The "present day snapshot" is implemented in
ICU (C and Java) <= 2.6.

----------------------------------------------------------------------
BACKGROUND

The zoneinfo or "Olson" time zone package is used by various systems
to describe the behavior of time zones.  The package consists of
several parts.  E.g.:

  Index of ftp://elsie.nci.nih.gov/pub/

  classictzcode.tar.gz    65 KB        12/10/1994    12:00:00 AM
  classictzdata.tar.gz    67 KB        12/10/1994    12:00:00 AM
  e5+57.tar.gz            2909 KB      3/22/1993     12:00:00 AM
  iso8601.ps.gz           16 KB        7/27/1996     12:00:00 AM
  leastsq.xls             49 KB        4/24/1997     12:00:00 AM
  ltroff.tar.gz           36 KB        7/16/1993     12:00:00 AM
  pi.shar.gz              4 KB         3/9/1994      12:00:00 AM
  tzarchive.gz            3412 KB      8/18/2003     4:00:00 AM
  tzcode2003a.tar.gz      98 KB        3/24/2003     2:32:00 PM
  tzdata2003a.tar.gz      132 KB       3/24/2003     2:32:00 PM

ICU only uses the tzcodeYYYYV.tar.gz and tzdataYYYYV.tar.gz files,
where YYYY is the year and V is the version letter ('a'...'z').

----------------------------------------------------------------------
HOWTO

1. Obtain the current versions of tzcodeYYYYV.tar.gz (aka `tzcode')
   and tzdataYYYYV.tar.gz (aka `tzdata') from the FTP site given
   above.  Either manually download or use wget:

   $ cd {path_to}/icu/source/tools/gentz/tzcode
   $ wget "ftp://elsie.nci.nih.gov/pub/tz*.tar.gz"

2. Unpack tzcode and tzdata directly into the directory gentz/tzcode:

   $ tar xzvf tzcode*.tar.gz
   $ tar xzvf tzdata*.tar.gz

3. Apply the ICU patch to zic.c:

   $ patch < patch-icu-tzcode

   If patch complains at this point, there is a mismatch that must be
   manually addressed.  See the CVS log of `patch-icu-tzcode' for
   version details.

4. Build:

   $ make icu_data

5. Copy the data file to the correct location in the ICU source tree:

   $ cp zoneinfo.txt ../../../data/misc/

6. Rebuild ICU:

   $ cd ../../../
   $ {*make}

7. Don't forget to check in the new zoneinfo.txt (from its location at
   {path_to}/icu/source/data/misc/zoneinfo.txt) into CVS.

----------------------------------------------------------------------
eof
