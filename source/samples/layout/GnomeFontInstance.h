
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
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
#include "layout/LEFontInstance.h"

#include "RenderingSurface.h"
#include "FontTableCache.h"
#include "cmaps.h"

class GnomeSurface : public RenderingSurface
{
public:
    GnomeSurface(GtkWidget *theWidget);
    virtual ~GnomeSurface();

    virtual void drawGlyphs(const LEFontInstance *font, const LEGlyphID *glyphs, le_int32 count,
        const float *positions, le_int32 x, le_int32 y, le_int32 width, le_int32 height);

    GtkWidget *getWidget() const;
    void setWidget(GtkWidget *theWidget);

private:
    GtkWidget *fWidget;
};

class GnomeFontInstance : public LEFontInstance, protected FontTableCache
{
 protected:
    TT_Face fFace;
    TT_Instance fInstance;
    TT_Glyph fGlyph;

    le_int32 fPointSize;
    le_int32 fUnitsPerEM;
    le_int32 fAscent;
    le_int32 fDescent;
    le_int32 fLeading;

    float fDeviceScaleX;
    float fDeviceScaleY;

    CMAPMapper *fMapper;

    virtual const void *readFontTable(LETag tableTag) const;

    virtual LEErrorCode initMapper();

 public:
    GnomeFontInstance(TT_Engine engine, const TT_Text *fontPathName, le_int16 pointSize, LEErrorCode &status);

    virtual ~GnomeFontInstance();

    TT_Instance getFont() const;

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_int32 getUnitsPerEM() const;

    virtual le_int32 getAscent() const;

    virtual le_int32 getDescent() const;

    virtual le_int32 getLeading() const;

    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch) const;

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    float getXPixelsPerEm() const;

    float getYPixelsPerEm() const;

    float getScaleFactorX() const;

    float getScaleFactorY() const;

    TT_Raster_Map *rasterizeGlyphs(const LEGlyphID *glyphs, le_int32 glyphCount, const le_int32 *dx, const le_int32 *dy,
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

inline le_int32 GnomeFontInstance::getUnitsPerEM() const
{
    return fUnitsPerEM;
}

inline le_int32 GnomeFontInstance::getAscent() const
{
    return fAscent;
}

inline le_int32 GnomeFontInstance::getDescent() const
{
    return fDescent;
}

inline le_int32 GnomeFontInstance::getLeading() const
{
    return fLeading;
}

inline LEGlyphID GnomeFontInstance::mapCharToGlyph(LEUnicode32 ch) const
{
    return fMapper->unicodeToGlyph(ch);
}

inline float GnomeFontInstance::getXPixelsPerEm() const
{
    return (float) fPointSize;
}

inline float GnomeFontInstance::getYPixelsPerEm() const
{
    return  (float) fPointSize;
}

inline float GnomeFontInstance::getScaleFactorX() const
{
    return fDeviceScaleX;
}

inline float GnomeFontInstance::getScaleFactorY() const
{
    return fDeviceScaleY;
}

#endif
