/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/24/99    aliu        Creation.
**********************************************************************
*/

#ifndef TZDAT_H
#define TZDAT_H

#include "utypes.h"

/* This file defines the format of the memory-mapped data file
 * containing system time zone data for icu.  See also gentz
 * and tz.pl.
 *
 * The format is designed specifically to allow certain operations:
 *
 * 1. Performing a fast binary search by name, and locating the
 *    corresponding zone data.  This is the most important operation.
 *    It corresponds to the TimeZone::createTimeZone() method.
 *
 * 2. Performing a fast iteration over zones having a specific GMT
 *    offset.  For this operation, the zone data need not be
 *    retrieved, just the IDs.  This corresponds to the
 *    TimeZone::createAvailableIDs(int32_t) method.
 *
 * 3. Iterating over all zone IDs.  This corresponds to the
 *    TimeZone::createAvailableIDs() method.
 *
 * The createAvailableIDs() methods return arrays of pointers to
 * existing static UnicodeString IDs that it owns.  Thus
 * createAvailableIDs() needs a way to reference one of these IDs when
 * iterating.  Note that these IDs are _not_ stored in the
 * memory-mapped data file, so we cannot store offsets.  To solve this
 * problem, we define a canonical index number for each zone.  This
 * index number runs from 0..n-1, where n is the total number of
 * zones.  The name table is stored in index number order, and we
 * provide a table that is sorted by GMT offset with keys being GMT
 * offset values and values being canonical index numbers.
 *
 * (Later, we might change createAvailableIDs() to return char*
 * strings rather than UnicodeString pointers.  In that case, this
 * data structure could be modified to index into the name table
 * directly.)
 *
 * In the following table, sizes are estimated sizes for a zone list
 * of about 200 standard and 200 DST zones, which is typical in 1999.
 *
 *  0K    TZHeader
 *  2K    Standard zone table (StandardZone[])
 *  4K    DST zone table (Zone[])
 *  2K    Index table, sorted by name, 4 bytes / zone
 *        This is a list of 'count' deltas sorted in ascending
 *        lexicographic order of name string.
 *  1K    Index table, sorted by gmtOffset then name.  See
 *        OffsetIndex struct.
 *  6K    Name table - always last
 *        This is all the zone names, in lexicographic order,
 *        with zero bytes terminating each name.
 * 14K    TOTAL
 *
 * Any field with a name ending in "delta" is an offset value
 * from the first byte of the TZHeader structure, unless otherwise
 * specified.
 *
 * When using the name index table and the offset index table,
 * code can determine whether an indexed zone is a standard
 * zone or a DST zone by examining its delta.  If the delta is
 * less than dstDelta, it is a standard zone.  Otherwise it 
 * is a DST zone.
 */

struct TZHeader {    
    uint16_t versionYear;     // e.g. "1999j" -> 1999
    uint16_t versionSuffix;   // e.g. "1999j" -> 10

    uint32_t count;           // standardCount + dstCount
    uint32_t standardCount;   // # of standard zones
    uint32_t dstCount;        // # of dst zones  

    uint32_t nameIndexDelta;   // delta to name index table
    uint32_t offsetIndexDelta; // delta to gmtOffset index table
    uint32_t standardDelta;    // delta to standard zones ALWAYS < dstDelta
    uint32_t dstDelta;         // delta to dst zones ALWAYS > standardDelta
    uint32_t nameTableDelta;   // delta to name (aka ID) table

    /* NOTE: Currently the standard and DST zone counts and deltas are
     * unused (all zones are referenced via the name index table).
     * However, they are retained for possible future use.
     */
};

struct StandardZone {
    int32_t  gmtOffset;   // gmt offset in milliseconds
};

struct TZRule {
    uint8_t  month;  // month
    int8_t   dowim;  // dowim
    int8_t   dow;    // dow
    uint16_t time;   // time in minutes
    int8_t   mode;   // (w/s/u) == TimeZone::TimeMode enum as int
};

struct DSTZone {
    int32_t  gmtOffset;   // gmtoffset in milliseconds
    uint16_t dstSavings;  // savings in minutes
    TZRule   onsetRule;   // onset rule
    TZRule   ceaseRule;   // cease rule
};

/**
 * This variable-sized struct makes up the offset index table.  To get
 * from one table entry to the next, add the nextEntryDelta.  If the
 * nextEntryDelta is zero then this is the last entry.  The offset
 * index table is designed for sequential access, not random access.
 * Given the small number of distinct offsets (39 in 1999j), this
 * suffices.
 */
struct OffsetIndex {
    uint16_t  nextEntryDelta;
    uint16_t  count;
    int32_t   gmtOffset;  // in ms
    uint16_t  zoneNumber; // There are actually 'count' uint16_t's here
};

// Information used to identify and validate the data

#define TZ_DATA_NAME "tz"
#define TZ_DATA_TYPE "dat"

// Fields in UDataInfo:
static const char TZ_SIG[] = "zone";     // dataFormat
static const int8_t TZ_FORMAT_VERSION = 1; // formatVersion[0]

#endif
