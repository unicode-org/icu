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
 */

struct TZHeader {    
    uint16_t versionYear;     // e.g. "1999j" -> 1999
    uint16_t versionSuffix;   // e.g. "1999j" -> 10
    uint32_t standardCount;   // # of standard rules     
    uint32_t standardOffset;  // offset to standard rules
    uint32_t dstCount;        // # of dst rules          
    uint32_t dstOffset;       // offset to dst rules     
    uint32_t nameTableOffset; // offset to name table    
};

struct StandardZone {
    uint32_t nameOffset;  // offset *within name table* to name
    int32_t  gmtOffset;   // gmtoffset in seconds
};

struct TZRule {
    uint8_t  month;  // month
    int8_t   dowim;  // dowim
    int8_t   dow;    // dow
    uint16_t time;   // time minutes
    int8_t   mode;   // mode ('w', 's', 'u')
};

struct DSTZone {
    uint32_t nameOffset;  // offset within name table to name
    int32_t  gmtOffset;   // gmtoffset in seconds
    uint16_t dstSavings;  // savings in minutes
    TZRule   onsetRule;   // onset rule
    TZRule   ceaseRule;   // cease rule
};

#endif
