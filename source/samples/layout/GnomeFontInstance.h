
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  GnomeFontInstance.h
 *
 *   created on: 08/30/2001
 *   created by: Eric R. Mader
 */

#ifndef __GNOMEFONTINSTANCE_H
#define __GNOMEFONTINSTANCE_H

#include <gnome.h>
#include "freetype/freetype.h"

#include "layout/LETypes.h"
#include "RenderingFontInstance.h"
#include "cmaps.h"

class GnomeFontInstance : public RenderingFontInstance
{
 protected:
    TT_Face fFace;
    TT_Instance fInstance;
    TT_Glyph fGlyph;

    virtual const void *readFontTable(LETag tableTag) const;

 public:
    GnomeFontInstance(TT_Engine engine, const TT_Text *fontPathName, le_int16 pointSize, RFIErrorCode &status);

    virtual ~GnomeFontInstance();


    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    virtual void drawGlyphs(void *surface, LEGlyphID *glyphs, le_uint32 count, le_int32 *dx,
                            le_int32 x, le_int32 y, le_int32 width, le_int32 height) const;

};

#endif
