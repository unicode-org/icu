// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2015, International Business Machines
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

    float     fPointSize;
    le_int32  fUnitsPerEM;
    le_uint32 fFontChecksum;
    le_int32  fAscent;
    le_int32  fDescent;
    le_int32  fLeading;

    const SFNTDirectory *fDirectory;
    le_uint16 fDirPower;
    le_uint16 fDirExtra;

    float fDeviceScaleX;
    float fDeviceScaleY;

    const NAMETable *fNAMETable;
    le_uint16 fNameCount;
    le_uint16 fNameStringOffset;

    CMAPMapper *fCMAPMapper;

    const HMTXTable *fHMTXTable;
    le_uint16 fNumGlyphs;
    le_uint16 fNumLongHorMetrics;

    static le_int8 highBit(le_int32 value);

    const DirectoryEntry *findTable(LETag tag) const;
    const void *readTable(LETag tag, le_uint32 *length) const;
    void getMetrics();

    CMAPMapper *findUnicodeMapper();

protected:
    const void *readFontTable(LETag tableTag) const { size_t ignored; return readFontTable(tableTag, ignored); }
    const void *readFontTable(LETag tableTag, size_t &length) const override;

public:
    PortableFontInstance(const char *fileName, float pointSize, LEErrorCode &status);

    virtual ~PortableFontInstance();

    const void *getFontTable(LETag tableTag, size_t &length) const override;

    virtual const char *getNameString(le_uint16 nameID, le_uint16 platform, le_uint16 encoding, le_uint16 language) const;

    virtual const LEUnicode16 *getUnicodeNameString(le_uint16 nameID, le_uint16 platform, le_uint16 encoding, le_uint16 language) const;

    virtual void deleteNameString(const char *name) const;

    virtual void deleteNameString(const LEUnicode16 *name) const;

    le_int32 getUnitsPerEM() const override;

    virtual le_uint32 getFontChecksum() const;

    virtual le_uint32 getRawChecksum() const;

    le_int32 getAscent() const override;

    le_int32 getDescent() const override;

    le_int32 getLeading() const override;

    // We really want to inherit this method from the superclass, but some compilers
    // issue a warning if we don't implement it...
    LEGlyphID mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper, le_bool filterZeroWidth) const override;
    
    // We really want to inherit this method from the superclass, but some compilers
    // issue a warning if we don't implement it...
    LEGlyphID mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const override;

    LEGlyphID mapCharToGlyph(LEUnicode32 ch) const override;

    void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const override;

    le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const override;

    float getXPixelsPerEm() const override;

    float getYPixelsPerEm() const override;

    float getScaleFactorX() const override;

    float getScaleFactorY() const override;

};

#endif
