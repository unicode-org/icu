/***************************************************************************
*
*   Copyright (C) 1998-2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
************************************************************************/

#ifndef __SFNT_H
#define __SFNT_H

#include "LETypes.h"


#ifndef ANY_NUMBER
#define ANY_NUMBER 1
#endif

struct DirectoryEntry
{
    le_uint32   tag;
    le_uint32   checksum;
    le_uint32   offset;
    le_uint32   length;
};

struct SFNTDirectory
{
    le_uint32       scalerType;
    le_uint16       numTables;
    le_uint16       searchRange;
    le_uint16       entrySelector;
    le_uint16       rangeShift;
    DirectoryEntry  tableDirectory[ANY_NUMBER];
};


struct CMAPEncodingSubtableHeader
{
    le_uint16   platformID;
    le_uint16   platformSpecificID;
    le_uint32   encodingOffset;
};

struct CMAPTable
{
    le_uint16   version;
    le_uint16   numberSubtables;
    CMAPEncodingSubtableHeader encodingSubtableHeaders[ANY_NUMBER];
};

struct CMAPEncodingSubtable
{
    le_uint16   format;
    le_uint16   length;
    le_uint16   language;
};

struct CMAPFormat0Encoding : CMAPEncodingSubtable
{
    le_uint8    glyphIndexArray[256];
};

struct CMAPFormat2Subheader
{
    le_uint16   firstCode;
    le_uint16   entryCount;
    le_int16    idDelta;
    le_uint16   idRangeOffset;
};

struct CMAPFormat2Encoding : CMAPEncodingSubtable
{
    le_uint16  subHeadKeys[256];
    CMAPFormat2Subheader subheaders[ANY_NUMBER];
};

struct CMAPFormat4Encoding : CMAPEncodingSubtable
{
    le_uint16   segCountX2;
    le_uint16   searchRange;
    le_uint16   entrySelector;
    le_uint16   rangeShift;
    le_uint16   endCodes[ANY_NUMBER];
//  le_uint16   reservedPad;
//  le_uint16   startCodes[ANY_NUMBER];
//  le_uint16   idDelta[ANY_NUMBER];
//  le_uint16   idRangeOffset[ANY_NUMBER];
//  le_uint16   glyphIndexArray[ANY_NUMBER];
};

struct CMAPFormat6Encoding : CMAPEncodingSubtable
{
    le_uint16   firstCode;
    le_uint16   entryCount;
    le_uint16   glyphIndexArray[ANY_NUMBER];
};

struct CMAPEncodingSubtable32
{
    le_uint32   format;
    le_uint32   length;
    le_uint32   language;
};

struct CMAPGroup
{
    le_uint32   startCharCode;
    le_uint32   endCharCode;
    le_uint32   startGlyphCode;
};

struct CMAPFormat8Encoding : CMAPEncodingSubtable32
{
    le_uint32   is32[65536/32];
    le_uint32   nGroups;
    CMAPGroup   groups[ANY_NUMBER];
};

struct CMAPFormat10Encoding : CMAPEncodingSubtable32
{
    le_uint32   startCharCode;
    le_uint32   numCharCodes;
    le_uint16   glyphs[ANY_NUMBER];
};

struct CMAPFormat12Encoding : CMAPEncodingSubtable32
{
    le_uint32   nGroups;
    CMAPGroup   groups[ANY_NUMBER];
};

typedef le_int32 fixed;

struct BigDate
{
    le_uint32   bc;
    le_uint32   ad;
};

struct HEADTable
{
    fixed       version;
    fixed       fontRevision;
    le_uint32   checksumAdjustment;
    le_uint32   magicNumber;
    le_uint16   flags;
    le_uint16   unitsPerEm;
    BigDate     created;
    BigDate     modified;
    le_int16    xMin;
    le_int16    yMin;
    le_int16    xMax;
    le_int16    yMax;
    le_int16    lowestRecPPEM;
    le_int16    fontDirectionHint;
    le_int16    indexToLocFormat;
    le_int16    glyphDataFormat;
};

struct MAXPTable
{
    fixed       version;
    le_uint16   numGlyphs;
    le_uint16   maxPoints;
    le_uint16   maxContours;
    le_uint16   maxComponentPoints;
    le_uint16   maxComponentContours;
    le_uint16   maxZones;
    le_uint16   maxTwilightPoints;
    le_uint16   maxStorage;
    le_uint16   maxFunctionDefs;
    le_uint16   maxInstructionDefs;
    le_uint16   maxStackElements;
    le_uint16   maxSizeOfInstructions;
    le_uint16   maxComponentElements;
    le_uint16   maxComponentDepth;
};

struct HHEATable
{
    fixed       version;
    le_int16    ascent;
    le_int16    descent;
    le_int16    lineGap;
    le_uint16   advanceWidthMax;
    le_int16    minLeftSideBearing;
    le_int16    minRightSideBearing;
    le_int16    xMaxExtent;
    le_int16    caretSlopeRise;
    le_int16    caretSlopeRun;
    le_int16    caretOffset;
    le_int16    reserved1;
    le_int16    reserved2;
    le_int16    reserved3;
    le_int16    reserved4;
    le_int16    metricDataFormat;
    le_uint16   numOfLongHorMetrics;
};

struct LongHorMetric
{
    le_uint16   advanceWidth;
    le_int16    leftSideBearing;
};

struct HMTXTable
{
    LongHorMetric hMetrics[ANY_NUMBER];        // ANY_NUMBER = numOfLongHorMetrics from hhea table
//  le_int16        leftSideBearing[ANY_NUMBER]; // ANY_NUMBER = numGlyphs - numOfLongHorMetrics
};

enum PlatformID
{
    PLATFORM_UNICODE = 0,
    PLATFORM_MACINTOSH = 1,
    PLATFORM_ISO       = 2,
    PLATFORM_MICROSOFT = 3,
    PLATFORM_CUSTOM    = 4
};

enum MacintoshEncodingID
{
    MACINTOSH_ROMAN = 0
};

enum MacintoshLanguageID
{
    MACINTOSH_ENGLISH = 0
};

enum NameID
{
    NAME_COPYRIGHT_NOTICE     = 0,
    NAME_FONT_FAMILY          = 1,
    NAME_FONT_SUB_FAMILY      = 2,
    NAME_UNIQUE_FONT_ID       = 3,
    NAME_FULL_FONT_NAME       = 4,
    NAME_VERSION_STRING       = 5,
    NAME_POSTSCRIPT_NAME      = 6,
    NAME_TRADEMARK            = 7,
    NAME_MANUFACTURER         = 8,
    NAME_DESIGNER             = 9,
    NAME_DESCRIPTION          = 10,
    NAME_VENDOR_URL           = 11,
    NAME_DESIGNER_URL         = 12,
    NAME_LICENSE_DESCRIPTION  = 13,
    NAME_LICENSE_URL          = 14,
    NAME_RESERVED             = 15,
    NAME_PREFERRED_FAMILY     = 16,
    NAME_PREFERRED_SUB_FAMILY = 17,
    NAME_COMPATIBLE_FULL      = 18,
    NAME_SAMPLE_TEXT          = 19,
    NAME_POSTSCRIPT_CID       = 20
};

struct NameRecord
{
    le_uint16 platformID;
    le_uint16 encodingID;
    le_uint16 languageID;
    le_uint16 nameID;
    le_uint16 length;
    le_uint16 offset;
};

struct NAMETable
{
    le_uint16 version;
    le_uint16 count;
    le_uint16 stringOffset;
    NameRecord nameRecords[ANY_NUMBER];
};

#endif

