
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

class GnomeSurface : public RenderingSurface
{
public:
    GnomeSurface(GtkWidget *theWidget);
    virtual ~GnomeSurface();

    virtual void drawGlyphs(const RenderingFontInstance *font, const LEGlyphID *glyphs, le_int32 count,
        const le_int32 *dx, le_int32 x, le_int32 y, le_int32 width, le_int32 height) const;

    GtkWidget *getWidget() const;
    void setWidget(GtkWidget *theWidget);

private:
    GtkWidget *fWidget;
};

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

    TT_Instance getFont() const;

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    TT_Raster_Map *rasterizeGlyphs(const LEGlyphID *glyphs, le_int32 glyphCount, const le_int32 *dx,
				   le_int32 &xOffset, le_int32 &yOffset) const;

    void freeRaster(TT_Raster_Map *raster);
};

inline GtkWidget *GnomeSurface::getWidget() const
{
    return fWidget;
}

inline void GnomeSurface::setWidget(GtkWidget *theWidget)
{
    fWidget = theWidget;
}

inline TT_Instance GnomeFontInstance::getFont() const
{
    return fInstance;
}

#endif
