// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2003-2013, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: July 10 2003
* Since: ICU 2.8
**********************************************************************
*/

#ifndef _TZ2ICU_H_
#define _TZ2ICU_H_

/* We have modified the zoneinfo binary format (we write raw offset
 * and DST offset separately instead of their sum) so we notate the
 * file with a distinct signature.  This prevents someone from trying
 * to use our output files as normal zoneinfo files, and also prevents
 * someone from trying to use normal zoneinfo files for ICU.  We also
 * use the first byte of the reserved section as a version integer, to
 * be incremented each time the data format changes.
 */

#define TZ_ICU_MAGIC "TZic" /* cf. TZ_MAGIC = "TZif" */

typedef unsigned char ICUZoneinfoVersion;

#define TZ_ICU_VERSION ((ICUZoneinfoVersion) 1)

/* File into which we will write supplemental ICU data.  This allows
 * zic to communicate final zone data to tz2icu. */
#define ICU_ZONE_FILE "icu_zone.txt"

/* Output resource name.  This determines both the file name and the
 * resource name within the file.  That is, the output will be to the
 * file ICU_TZ_RESOURCE ".txt" and the resource within it will be
 * ICU_TZ_RESOURCE. */
#define ICU_TZ_RESOURCE_OLD "zoneinfo"
#define ICU_TZ_RESOURCE "zoneinfo64"

/* Files generated into the ICU source tree by the CLDR-to-ICU conversion tool
 * (see tools/cldr/cldr-to-icu/), which are deliberately *not* packaged into
 * ICU's runtime data, since they are only needed offline, here.  The paths are
 * normally supplied by the build (see Makefile.in); the defaults below assume
 * tz2icu is run from the ICU data directory. */

/* File containing the zone/region mapping extracted from CLDR's BCP 47 time
 * zone identifiers (common/bcp47/timezone.xml). */
#ifndef ICU_ZONE_REGIONS
#define ICU_ZONE_REGIONS "tzdata/zoneRegions.txt"
#endif

/* File containing the metazone standard/DST offsets extracted from CLDR's
 * metaZones.xml. */
#ifndef ICU_METAZONE_OFFSETS
#define ICU_METAZONE_OFFSETS "tzdata/metazoneOffsets.txt"
#endif

#endif
