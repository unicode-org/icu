
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2006, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  SimpleFontInstance.h
 *
 *   created on: 03/30/2006
 *   created by: Eric R. Mader
 */

#ifndef __SIMPLEFONTINSTANCE_H
#define __SIMPLEFONTINSTANCE_H

#include <stdio.h>

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

class SimpleFontInstance : public LEFontInstance
{
private:
    float     fPointSize;
    le_int32  fAscent;
    le_int32  fDescent;

protected:
    const void *readFontTable(LETag tableTag) const;

public:
    SimpleFontInstance(float pointSize, LEErrorCode &status);

    virtual ~SimpleFontInstance();

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_int32 getUnitsPerEM() const
    {
        return 2048;
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
        return 0;
    }

    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch) const
    {
        return (LEGlyphID) ch;
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
