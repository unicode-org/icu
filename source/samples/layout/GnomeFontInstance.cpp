
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  GnomeFontInstance.cpp
 *
 *   created on: 08/30/2001
 *   created by: Eric R. Mader
 */

#include <gnome.h>
#include "freetype/freetype.h"

#include "layout/LETypes.h"
#include "layout/LESwaps.h"

#include "GnomeFontInstance.h"
#include "sfnt.h"
#include "cmaps.h"

GnomeSurface::GnomeSurface(GtkWidget *theWidget)
    : fWidget(theWidget)
{
    // nothing else to do
}

GnomeSurface::~GnomeSurface()
{
    // nothing to do
}

void GnomeSurface::drawGlyphs(const LEFontInstance *font, const LEGlyphID *glyphs, le_int32 count,
                              const float *positions, le_int32 x, le_int32 y, le_int32 width, le_int32 height)
{
    GnomeFontInstance *gFont = (GnomeFontInstance *) font;
    TT_Instance instance = gFont->getFont();
    le_int32 *dx = LE_NEW_ARRAY(le_int32, count);
    le_int32 *dy = LE_NEW_ARRAY(le_int32, count);
    le_int32 xOffset, yOffset;
    TT_Raster_Map *raster;
    TT_Error error;

    for (le_int32 g = 0; g < count; g += 1) {
        dx[g] = (le_int32) (positions[g * 2 + 2] - positions[g * 2]);
        dy[g] = (le_int32) - positions[g * 2 + 1];
    }

    raster = gFont->rasterizeGlyphs(glyphs, count, dx, dy, xOffset, yOffset);

    if (raster->width > 0 && raster->rows > 0) {
        GdkBitmap *bitmap = gdk_bitmap_create_from_data(NULL, (const gchar *) raster->bitmap, raster->width, raster->rows);

        gint bitsx = x + (gint) positions[0] + xOffset;
        gint bitsy = y - yOffset;

        gdk_gc_set_clip_origin(fWidget->style->black_gc, bitsx, bitsy);
        gdk_gc_set_clip_mask(fWidget->style->black_gc, bitmap);

        gdk_draw_rectangle(fWidget->window,
                           fWidget->style->black_gc,
                           TRUE,
                           bitsx, bitsy,
                           raster->width, raster->rows);

        gdk_gc_set_clip_origin(fWidget->style->black_gc, 0, 0);
        gdk_gc_set_clip_mask(fWidget->style->black_gc, NULL);

        gdk_bitmap_unref(bitmap);
    }
    
    gFont->freeRaster(raster);
    LE_DELETE_ARRAY(dy);
    LE_DELETE_ARRAY(dx);
}

GnomeFontInstance::GnomeFontInstance(TT_Engine engine, const TT_Text *fontPathName, le_int16 pointSize, LEErrorCode &status)
    : FontTableCache(), fPointSize(pointSize), fUnitsPerEM(0), fAscent(0), fDescent(0), fLeading(0),
      fDeviceScaleX(1), fDeviceScaleY(1), fMapper(NULL)
{
    TT_Error error;
    TT_Face_Properties faceProperties;

    fFace.z = NULL;

    error = TT_Open_Face(engine, fontPathName, &fFace);

    if (error != 0) {
        status = LE_FONT_FILE_NOT_FOUND_ERROR;
        return;
    }

    error = TT_New_Instance(fFace, &fInstance);

    if (error != 0) {
        status = LE_MEMORY_ALLOCATION_ERROR;
        return;
    }

    // FIXME: what about the display resolution?
    // TT_Set_Instance_Resolutions(fInstance, 72, 72);
    fDeviceScaleX = ((float) 96) / 72;
    fDeviceScaleY = ((float) 96) / 72;

    TT_Set_Instance_CharSize(fInstance, pointSize << 6);

    TT_Get_Face_Properties(fFace, &faceProperties);

    fUnitsPerEM = faceProperties.header->Units_Per_EM;

    fAscent  = (le_int32) (yUnitsToPoints(faceProperties.horizontal->Ascender) * fDeviceScaleY);
    fDescent = (le_int32) -(yUnitsToPoints(faceProperties.horizontal->Descender) * fDeviceScaleY);
    fLeading = (le_int32) (yUnitsToPoints(faceProperties.horizontal->Line_Gap) * fDeviceScaleY);

    // printf("Face = %s, unitsPerEM = %d, ascent = %d, descent = %d\n", fontPathName, fUnitsPerEM, fAscent, fDescent);

    error = TT_New_Glyph(fFace, &fGlyph);

    if (error != 0) {
        status = LE_MEMORY_ALLOCATION_ERROR;
        return;
    }

    status = initMapper();
}

GnomeFontInstance::~GnomeFontInstance()
{
    if (fFace.z != NULL) {
        TT_Close_Face(fFace);
    }
}

LEErrorCode GnomeFontInstance::initMapper()
{
    LETag cmapTag = LE_CMAP_TABLE_TAG;
    const CMAPTable *cmap = (const CMAPTable *) readFontTable(cmapTag);

    if (cmap == NULL) {
        return LE_MISSING_FONT_TABLE_ERROR;
    }

    fMapper = CMAPMapper::createUnicodeMapper(cmap);

    if (fMapper == NULL) {
        return LE_MISSING_FONT_TABLE_ERROR;
    }

    return LE_NO_ERROR;
}

const void *GnomeFontInstance::getFontTable(LETag tableTag) const
{
    return FontTableCache::find(tableTag);
}

const void *GnomeFontInstance::readFontTable(LETag tableTag) const
{
    TT_Long len = 0;
    void *result = NULL;

    TT_Get_Font_Data(fFace, tableTag, 0, NULL, &len);

    if (len > 0) {
        result = LE_NEW_ARRAY(char, len);
        TT_Get_Font_Data(fFace, tableTag, 0, result, &len);
    }

    return result;
}

void GnomeFontInstance::getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const
{
    advance.fX = 0;
    advance.fY = 0;

    if (glyph >= 0xFFFE) {
        return;
    }

    TT_Glyph_Metrics metrics;
    TT_Error error;

    error = TT_Load_Glyph(fInstance, fGlyph, glyph, TTLOAD_SCALE_GLYPH | TTLOAD_HINT_GLYPH);

    if (error != 0) {
        return;
    }

    TT_Get_Glyph_Metrics(fGlyph, &metrics);

    advance.fX = metrics.advance >> 6;
    return;
}

le_bool GnomeFontInstance::getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const
{
    TT_Outline outline;
    TT_Error error;

    error = TT_Load_Glyph(fInstance, fGlyph, glyph, TTLOAD_SCALE_GLYPH | TTLOAD_HINT_GLYPH);

    if (error != 0) {
        return FALSE;
    }

    error = TT_Get_Glyph_Outline(fGlyph, &outline);

    if (error != 0 || pointNumber >= outline.n_points) {
        return FALSE;
    }

    point.fX = outline.points[pointNumber].x >> 6;
    point.fY = outline.points[pointNumber].y >> 6;

    return TRUE;
}

// This table was generated by a little Java program.
const char bitReverse[256] = {
    0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
    0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
    0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
    0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
    0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
    0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
    0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
    0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
    0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
    0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
    0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
    0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
    0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
    0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
    0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
    0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF, 0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF
};

// FIXME: this would be much faster if we cached the TT_Glyph objects based on the glyph ID...
TT_Raster_Map *GnomeFontInstance::rasterizeGlyphs(const LEGlyphID *glyphs, le_int32 glyphCount,
                                                  const le_int32 *dx, const le_int32 *dy, le_int32 &xOffset, le_int32 &yOffset) const
{
    le_int32 i, xx = 0, yy = 0, zz;
    le_int32 minx = 0, maxx = 0, miny = 0, maxy = 0;
    TT_Raster_Map *raster = new TT_Raster_Map;
    TT_Glyph_Metrics metrics;
    TT_Error error;

    for (i = 0; i < glyphCount; i += 1) {
        error = TT_Load_Glyph(fInstance, fGlyph, (TT_UShort) LE_GET_GLYPH(glyphs[i]), TTLOAD_SCALE_GLYPH | TTLOAD_HINT_GLYPH);
        if (error == 0) {
            TT_Get_Glyph_Metrics(fGlyph, &metrics);

            zz = xx + metrics.bbox.xMin;
            if (minx > zz) {
                minx = zz;
            }

            zz = xx + metrics.bbox.xMax;
            if (maxx < zz) {
                maxx = zz;
            }

            yy = dy[i] * 64;
            zz = yy + metrics.bbox.yMin;
            if (miny > zz) {
                miny = zz;
            }

            zz = yy + metrics.bbox.yMax;
            if (maxy < zz) {
                maxy = zz;
            }
        }

        xx += (dx[i] * 64);
    }


    minx = (minx & -64) >> 6;
    miny = (miny & -64) >> 6;

    maxx = ((maxx + 63) & -64) >> 6;
    maxy = ((maxy + 63) & -64) >> 6;

    //printf("minx = %d, maxx = %d, miny = %d, maxy = %d\n", minx, maxx, miny, maxy);

    unsigned char *bits;

    raster->flow   = TT_Flow_Down;
    raster->width  = maxx - minx;
    raster->rows   = maxy - miny;
    raster->cols   = (raster->width + 7) / 8;
    raster->size   = raster->cols * raster->rows;

    raster->bitmap = bits = LE_NEW_ARRAY(unsigned char, raster->size);

    for (i = 0; i < raster->size; i += 1) {
        bits[i] = 0;
    }

    xx = (-minx) * 64; yy = (-miny) * 64;

    for (i = 0; i < glyphCount; i += 1) {
        if (glyphs[i] < 0xFFFE) {
            error = TT_Load_Glyph(fInstance, fGlyph, (TT_UShort) LE_GET_GLYPH(glyphs[i]), TTLOAD_SCALE_GLYPH | TTLOAD_HINT_GLYPH);
        
            if (error == 0) {
                TT_Get_Glyph_Bitmap(fGlyph, raster, xx, yy + (dy[i] * 64));
            }
        }

        xx += (dx[i] * 64);
    }

    for (i = 0; i < raster->size; i += 1) {
        bits[i] = bitReverse[bits[i]];
    }

    xOffset = minx;
    yOffset = maxy;

    return raster;
}

void GnomeFontInstance::freeRaster(TT_Raster_Map *raster)
{
    LE_DELETE_ARRAY(raster->bitmap);
    delete raster;
}
