
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

class GDIFontInstance : public RenderingFontInstance
{
protected:
    HDC fHdc;
    HFONT fFont;

    virtual const void *readFontTable(LETag tableTag) const;

public:
    GDIFontInstance(HDC theHDC, TCHAR *faceName, le_int16 pointSize, RFIErrorCode &status);
    GDIFontInstance(HDC theHDC, const char *faceName, le_int16 pointSize, RFIErrorCode &status);
    //GDIFontInstance(HDC theHDC, le_int16 pointSize);

    virtual ~GDIFontInstance();

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    virtual const void setFont(void *surface) const
    {
        HDC hdc = (HDC) surface;
	GDIFontInstance *real = (GDIFontInstance *) this;

	real->fHdc = hdc;
        SelectObject(hdc, fFont);
    };
    
    virtual void drawGlyphs(void *surface, LEGlyphID *glyphs, le_uint32 count, le_int32 *dx,
        le_int32 x, le_int32 y, le_int32 width, le_int32 height) const;

};

#endif
