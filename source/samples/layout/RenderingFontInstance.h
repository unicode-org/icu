
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  RenderingFontInstance.h
 *
 *   created on: 10/23/2001
 *   created by: Eric R. Mader
 */

#ifndef __RENDERINGFONTINSTANCE_H
#define __RENDERINGFONTINSTANCE_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "cmaps.h"

#define TABLE_CACHE_INIT 5
#define TABLE_CACHE_GROW 5

struct TableCacheEntry
{
    LETag tag;
    void *table;
};

enum RFIErrorCode {
    RFI_NO_ERROR = 0,

    RFI_ILLEGAL_ARGUMENT_ERROR    = 1,
    RFI_FONT_FILE_NOT_FOUND_ERROR = 2,
    RFI_MISSING_FONT_TABLE_ERROR  = 3,
    RFI_OUT_OF_MEMORY_ERROR       = 4
};

class RenderingFontInstance : public LEFontInstance
{
protected:
    void *fSurface;

    le_int32 fPointSize;
    le_int32 fUnitsPerEM;
    le_int32 fAscent;
    le_int32 fDescent;
    le_int32 fLeading;

    float fDeviceScaleX;
    float fDeviceScaleY;

    TableCacheEntry *fTableCache;
    le_int32 fTableCacheCurr;
    le_int32 fTableCacheSize;

    CMAPMapper *fMapper;

    virtual RFIErrorCode initMapper();
    virtual RFIErrorCode initFontTableCache();
    virtual void flushFontTableCache();
    virtual const void *readFontTable(LETag tableTag) const = 0;

public:
    RenderingFontInstance(void *surface, le_int16 pointSize);

    virtual ~RenderingFontInstance();

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_bool canDisplay(LEUnicode32 ch) const
    {
        return fMapper->unicodeToGlyph(ch) != 0;
    };

    virtual le_int32 getUnitsPerEM() const
    {
        return fUnitsPerEM;
    };

    virtual le_int32 getLineHeight() const
    {
        return getAscent() + getDescent() + getLeading();
    };

    virtual le_int32 getAscent() const
    {
        return fAscent;
    };

    virtual le_int32 getDescent() const
    {
        return fDescent;
    };

    virtual le_int32 getLeading() const
    {
        return fLeading;
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

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const = 0;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const = 0;

    virtual const void setFont(void *surface) const
    {
        // default implementation is to ignore this
    };
    
    virtual void drawGlyphs(void *surface, LEGlyphID *glyphs, le_uint32 count, le_int32 *dx,
        le_int32 x, le_int32 y, le_int32 width, le_int32 height) const = 0;

    float getXPixelsPerEm() const
    {
        return (float) fPointSize;
    };

    float getYPixelsPerEm() const
    {
        return  (float) fPointSize;
    };

    float xUnitsToPoints(float xUnits) const
    {
        return (xUnits * fPointSize) / (float) fUnitsPerEM;
    };

    float yUnitsToPoints(float yUnits) const
    {
        return (yUnits * fPointSize) / (float) fUnitsPerEM;
    };

    void unitsToPoints(LEPoint &units, LEPoint &points) const
    {
        points.fX = xUnitsToPoints(units.fX);
        points.fY = yUnitsToPoints(units.fY);
    }

    float xPixelsToUnits(float xPixels) const
    {
        return (xPixels * fUnitsPerEM) / (float) fPointSize;
    };

    float yPixelsToUnits(float yPixels) const
    {
        return (yPixels * fUnitsPerEM) / (float) fPointSize;
    };

    void pixelsToUnits(LEPoint &pixels, LEPoint &units) const
    {
        units.fX = xPixelsToUnits(pixels.fX);
        units.fY = yPixelsToUnits(pixels.fY);
    };

    void transformFunits(float xFunits, float yFunits, LEPoint &pixels) const
    {
        pixels.fX = xUnitsToPoints(xFunits) * fDeviceScaleX;
        pixels.fY = yUnitsToPoints(yFunits) * fDeviceScaleY;
    }
};

#endif
