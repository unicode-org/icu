
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  PortableFontInstance.h
 *
 *   created on: 11/12/1999
 *   created by: Eric R. Mader
 */

#ifndef __PORTABLEFONTINSTANCE_H
#define __PORTABLEFONTINSTANCE_H

#include <stdio.h>

#include "LETypes.h"
#include "LEFontInstance.h"

#include "sfnt.h"
#include "cmaps.h"

#define TABLE_CACHE_INIT 5
#define TABLE_CACHE_GROW 5

struct TableCacheEntry
{
    LETag tag;
    void *table;
};

enum PFIErrorCode {
    PFI_NO_ERROR = 0,

    PFI_FONT_FILE_NOT_FOUND_ERROR = 1,
    PFI_MISSING_FONT_TABLE_ERROR  = 2,
    PFI_OUT_OF_MEMORY_ERROR       = 3
};

#ifndef XP_CPLUSPLUS
typedef enum PFIErrorCode PFIErrorCode;
#endif

class PortableFontInstance : public LEFontInstance
{
private:
    FILE *fFile;

    float fUnitsPerEM;
    float fPointSize;

    const SFNTDirectory *fDirectory;
    le_uint16 fDirPower;
    le_uint16 fDirExtra;

    TableCacheEntry *fTableCache;
    le_int32 fTableCacheCurr;
    le_int32 fTableCacheSize;

    CMAPMapper *fCMAPMapper;

    const HMTXTable *fHMTXTable;
    le_uint16 fNumGlyphs;
    le_uint16 fNumLongHorMetrics;

    static le_int8 highBit(le_int32 value);

    PFIErrorCode PortableFontInstance::initFontTableCache();
    void PortableFontInstance::flushFontTableCache();

    const DirectoryEntry *findTable(LETag tag) const;
    const void *readTable(LETag tag, le_uint32 *length) const;
    void deleteTable(const void *table) const;

    CMAPMapper *PortableFontInstance::findUnicodeMapper();

public:
    PortableFontInstance(char *fileName, float pointSize, PFIErrorCode &status);

    virtual ~PortableFontInstance();

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_bool canDisplay(LEUnicode32 ch) const
    {
        return (le_bool) fCMAPMapper->unicodeToGlyph(ch) != 0;
    };

    virtual le_int32 getUnitsPerEM() const
    {
        return (le_int32) fUnitsPerEM;
    };

    virtual le_int32 getLineHeight() const
    {
        // this is a cheap hack!!
    return (le_int32) fPointSize;
    };

    virtual void mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, const LECharMapper *mapper, LEGlyphID glyphs[]) const;

    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const;

    virtual le_int32 getName(le_uint16 platformID, le_uint16 scriptID, le_uint16 languageID, le_uint16 nameID, LEUnicode *name) const
    {
        // This is only used for CDAC fonts, and we'll have to loose that support anyhow...
        //return (le_int32) fFontObject->getName(platformID, scriptID, languageID, nameID, name);
        if (name != NULL) {
            *name = 0;
        }

        return 0;
    };

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    float getXPixelsPerEm() const
    {
        return fPointSize;
    };

    float getYPixelsPerEm() const
    {
        return fPointSize;
    };

    float xUnitsToPoints(float xUnits) const
    {
        return (xUnits * fPointSize) / fUnitsPerEM;
    };

    float yUnitsToPoints(float yUnits) const
    {
        return (yUnits * fPointSize) / fUnitsPerEM;
    };

    void unitsToPoints(LEPoint &units, LEPoint &points) const
    {
        points.fX = xUnitsToPoints(units.fX);
        points.fY = yUnitsToPoints(units.fY);
    }

    float xPixelsToUnits(float xPixels) const
    {
        return (xPixels * fUnitsPerEM) / fPointSize;
    };

    float yPixelsToUnits(float yPixels) const
    {
        return (yPixels * fUnitsPerEM) / fPointSize;
    };

    void pixelsToUnits(LEPoint &pixels, LEPoint &units) const
    {
        units.fX = xPixelsToUnits(pixels.fX);
        units.fY = yPixelsToUnits(pixels.fY);
    };

    void transformFunits(float xFunits, float yFunits, LEPoint &pixels) const;
};

#endif
