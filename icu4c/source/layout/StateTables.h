/*
 * @(#)StateTables.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __STATETABLES_H
#define __STATETABLES_H

#include "LETypes.h"
#include "LayoutTables.h"

struct StateTableHeader
{
    le_int16 stateSize;
    ByteOffset classTableOffset;
    ByteOffset stateArrayOffset;
    ByteOffset entryTableOffset;
};

enum ClassCodes
{
    classCodeEOT = 0,
    classCodeOOB = 1,
    classCodeDEL = 2,
    classCodeEOL = 3,
    classCodeFirstFree = 4,
    classCodeMAX = 0xFF
};

typedef le_uint8 ClassCode;

struct ClassTable
{
    LEGlyphID firstGlyph;
    le_uint16 nGlyphs;
    ClassCode classArray[ANY_NUMBER];
};

enum StateNumber
{
    stateSOT        = 0,
    stateSOL        = 1,
    stateFirstFree  = 2,
    stateMAX        = 0xFF
};

typedef le_uint8 EntryTableIndex;

struct StateEntry
{
    ByteOffset  newStateOffset;
    le_int16    flags;
};

#endif

