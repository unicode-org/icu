
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  GDIFontInstance.h
 *
 *   created on: 08/09/2000
 *   created by: Eric R. Mader
 */

#ifndef __GDIFONTINSTANCE_H
#define __GDIFONTINSTANCE_H

#include <windows.h>

#include "layout/LETypes.h"
#include "RenderingFontInstance.h"
#include "cmaps.h"

class GDISurface : public RenderingSurface
{
public:
    GDISurface(HDC theHDC);
    virtual ~GDISurface();

    virtual void drawGlyphs(const RenderingFontInstance *font, const LEGlyphID *glyphs, le_int32 count,
        const le_int32 *dx, le_int32 x, le_int32 y, le_int32 width, le_int32 height);

    void setFont(const RenderingFontInstance *font);
    HDC  getHDC();
    void setHDC(HDC theHDC);

private:
    HDC fHdc;
    const LEFontInstance *fCurrentFont;
};

inline HDC GDISurface::getHDC()
{
    return fHdc;
}

class GDIFontInstance : public RenderingFontInstance
{
protected:
    GDISurface *fSurface;
    HFONT fFont;

    virtual const void *readFontTable(LETag tableTag) const;

public:
    GDIFontInstance(GDISurface *surface, TCHAR *faceName, le_int16 pointSize, RFIErrorCode &status);
    GDIFontInstance(GDISurface *surface, const char *faceName, le_int16 pointSize, RFIErrorCode &status);
    //GDIFontInstance(GDISurface *surface, le_int16 pointSize);

    virtual ~GDIFontInstance();

    HFONT getFont() const;

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;
};

inline HFONT GDIFontInstance::getFont() const
{
    return fFont;
}

#endif
