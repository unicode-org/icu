
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
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

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

#include "FontTableCache.h"

#include "sfnt.h"
#include "cmaps.h"

class PortableFontInstance : public LEFontInstance, protected FontTableCache
{
private:
    FILE *fFile;

    float    fPointSize;
    le_int32 fUnitsPerEM;
    le_int32 fAscent;
    le_int32 fDescent;
    le_int32 fLeading;

    const SFNTDirectory *fDirectory;
    le_uint16 fDirPower;
    le_uint16 fDirExtra;

    float fDeviceScaleX;
    float fDeviceScaleY;

    CMAPMapper *fCMAPMapper;

    const HMTXTable *fHMTXTable;
    le_uint16 fNumGlyphs;
    le_uint16 fNumLongHorMetrics;

    static le_int8 highBit(le_int32 value);

    const DirectoryEntry *findTable(LETag tag) const;
    const void *readTable(LETag tag, le_uint32 *length) const;
    void deleteTable(const void *table) const;
    void getMetrics();

    CMAPMapper *findUnicodeMapper();

protected:
    const void *readFontTable(LETag tableTag) const;

public:
    PortableFontInstance(char *fileName, float pointSize, LEErrorCode &status);

    virtual ~PortableFontInstance();

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_int32 getUnitsPerEM() const
    {
        return fUnitsPerEM;
    };

    virtual le_int32 getAscent() const
    {
        return fAscent;
    }

    virtual le_int32 getDescent() const
    {
        return fDescent;
    }

    virtual le_int32 getLeading() const
    {
        return fLeading;
    }

    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch) const
    {
        return fCMAPMapper->unicodeToGlyph(ch);
    }

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

    float getScaleFactorX() const
    {
        return 1.0;
    }

    float getScaleFactorY() const
    {
        return 1.0;
    }

};

#endif
