/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/24/99    aliu        Creation.
*   12/13/1999  srl         Padded OffsetIndex to 4 byte values
*   02/01/01    aliu        Added country index
**********************************************************************
*/

#ifndef TZDAT_H
#define TZDAT_H

#include "unicode/utypes.h"

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

/* tz.icu data file format
 *
 * Here is the overall structure of the tz.icu file, expressed as a
 * pseudo-C-struct.  Refer to actual struct declarations below for
 * more details on each subelement of tz.icu.  Many of the elements
 * are of variable size.  Padding is used when necessary to align
 * words and longs properly; see structure declarations for details.
 *
 * struct tz.icu {
 *
 *  // The header gives offsets to various tables within tz.icu.
 *
 *  struct TZHeader            header;
 * 
 *  // The equivalency groups; repeated; one element for each
 *  // equivalency group.  Each one is of variable size.  Typically,
 *  // an equivalency group is found given an ID index.  The index is
 *  // used to find an entry of nameToEquiv[].  That entry is added to
 *  // the start of the header to obtain a pointer to one of the
 *  // entries equivTable[i].  The number of equivalency groups (n1)
 *  // is not stored anywhere; it can be discovered by walking the
 *  // table.
 *
 *  struct TZEquivalencyGroup  equivTable[n1];
 * 
 *  // An index which groups timezones having the same raw offset
 *  // together; repeated; one element for each raw offset struct.
 *  // Typically the code walks through this table starting at the
 *  // beginning until the desired index is found or the end of the
 *  // table is reached.  The number of offset groups (n2) is not
 *  // stored anywhere; it can be discovered by walking the table.
 *
 *  struct OffsetIndex         offsetIndex[n2];
 * 
 *  // An index which groups timezones having the same country
 *  // together; repeated; one element for each country.  Typically
 *  // the code walks through this table starting at the beginning
 *  // until the desired country is found or the end of the table is
 *  // reached.  The number of offset groups (n3) is not stored
 *  // anywhere; it can be discovered by walking the table.
 *
 *  struct CountryIndex        countryIndex[n3];
 * 
 *  // An array of offsets, one for each name.  Each offset, when
 *  // added to the start of the header, gives a pointer to an entry
 *  // equivTable[i], the equivalency group struct for the given zone.
 *  // The nubmer of names is given by TZHeader.count.  The order of
 *  // entries is the same as nameTable[].
 *
 *  uint32                     nameToEquiv[header.count];
 * 
 *  // All the time zone IDs, in sorted order, with 0 termination.
 *  // The number of entries is given by TZHeader.count.  The total
 *  // number of characters in this table (n4) is not stored anywhere;
 *  // it can be discovered by walking the table.  The order of
 *  // entries is the same as nameToEquiv[].
 *
 *  char                       nameTable[n4];
 * };
 */

// Information used to identify and validate the data

#define TZ_DATA_NAME "tz"
#define TZ_DATA_TYPE "icu"

#if !UCONFIG_NO_FORMATTING

// Fields in UDataInfo:

// TZ_SIG[] is encoded as numeric literals for compatibility with the HP compiler
static const uint8_t TZ_SIG_0 = 0x7a; // z
static const uint8_t TZ_SIG_1 = 0x6f; // o
static const uint8_t TZ_SIG_2 = 0x6e; // n
static const uint8_t TZ_SIG_3 = 0x65; // e

// This must match the version number at the top of tz.txt as
// well as the version number in the udata header.
static const int8_t TZ_FORMAT_VERSION = 4; // formatVersion[0]

struct TZHeader {    
    uint16_t versionYear;     // e.g. "1999j" -> 1999
    uint16_t versionSuffix;   // e.g. "1999j" -> 10

    uint32_t count;           // standardCount + dstCount

    uint32_t equivTableDelta;  // delta to equivalency group table
    uint32_t offsetIndexDelta; // delta to gmtOffset index table

    uint32_t countryIndexDelta; // delta to country code index table

    uint32_t nameIndexDelta;   // delta to name index table
    // The name index table is an array of 'count' 32-bit offsets from
    // the start of this header to equivalency group table entries.

    uint32_t nameTableDelta;   // delta to name (aka ID) table
    // The name table contains all zone IDs, in sort order, each name
    // terminated by a zero byte.
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
 * This variable-sized struct represents a time zone equivalency group.
 * This is a set of one or more zones that are identical in GMT offset
 * and rules, but differ in ID.  The struct has a variable size because
 * the standard zone has no rule data, and also because it contains a
 * variable number of index values listing the zones in the group.
 * The struct is padded to take up 4n bytes so that 4-byte integers
 * within the struct stay 4-aligned (namely, the gmtOffset members of
 * the zone structs).
 */
struct TZEquivalencyGroup {
    uint16_t nextEntryDelta;    // 0 for last entry
    uint8_t  isDST;             // != 0 for DSTZone
    uint8_t  reserved;
    union {
        struct {
            StandardZone zone;
            uint16_t     count;
            uint16_t     index; // There are actually 'count' uint16_t's here
        } s;
        struct {
            DSTZone      zone;
            uint16_t     count;
            uint16_t     index; // There are actually 'count' uint16_t's here
        } d;
    } u;
    // There may be two bytes of padding HERE to make the whole struct
    // have size 4n bytes.
};

/**
 * This variable-sized struct makes up the offset index table.  To get
 * from one table entry to the next, add the nextEntryDelta.  If the
 * nextEntryDelta is zero then this is the last entry.  The offset
 * index table is designed for sequential access, not random access.
 * Given the small number of distinct offsets (39 in 1999j), this
 * suffices.
 *
 * The value of default is the zone within this list that should be
 * selected as the default zone in the absence of any other
 * discriminating information.  This information comes from the file
 * tz.default.  Note that this is itself a zone number, like
 * those in the array starting at &zoneNumber.
 *
 * The gmtOffset field must be 4-aligned for some architectures.  To
 * ensure this, we do two things: 1. The entire struct is 4-aligned.
 * 2. The gmtOffset is placed at a 4-aligned position within the
 * struct.  3. The size of the whole structure is padded out to 4n
 * bytes.  We achieve this last condition by adding two bytes of
 * padding after the last zoneNumber, if count is _even_.  That is,
 * the struct size is 10+2count+padding, where padding is (count%2==0
 * ? 2:0).  See gentz for implementation.
 */
struct OffsetIndex {
    int32_t   gmtOffset;  // in ms - 4-aligned
    uint16_t  nextEntryDelta;
    uint16_t  defaultZone; // a zone number from 0..TZHeader.count-1
    uint16_t  count;
    uint16_t  zoneNumber; // There are actually 'count' uint16_t's here
    // Following the 'count' uint16_t's starting with zoneNumber,
    // there may be two bytes of padding to make the whole struct have
    // a size of 4n.  nextEntryDelta skips over any padding.
};

/**
 * This variable-sized struct makes up the country index table.  To get
 * from one table entry to the next, add the nextEntryDelta.  If the
 * nextEntryDelta is zero then this is the last entry.  The country
 * index table is designed for sequential access, not random access.
 *
 * The intcode is an integer representation of the two-letter country
 * code.  It is computed as (c1-'A')*32 + (c0-'A') where the country
 * code is a two-character string c1 c0, 'A' <= ci <= 'Z'.
 *
 * There are no 4-byte integers in this table, so we don't 4-align the
 * entries.
 */
struct CountryIndex {
    uint16_t  intcode; // see above
    uint16_t  nextEntryDelta;
    uint16_t  count;
    uint16_t  zoneNumber; // There are actually 'count' uint16_t's here
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
