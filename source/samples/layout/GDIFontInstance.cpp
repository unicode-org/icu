/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  GDIFontInstance.cpp
 *
 *   created on: 08/09/2000
 *   created by: Eric R. Mader
 */

#include <windows.h>

#include "layout/LETypes.h"
#include "layout/LESwaps.h"

#include "RenderingFontInstance.h"
#include "GDIFontInstance.h"
#include "sfnt.h"
#include "cmaps.h"

GDIFontInstance::GDIFontInstance(HDC hdc, TCHAR *faceName, le_int16 pointSize, RFIErrorCode &status)
  : fHdc(hdc), fFont(NULL), RenderingFontInstance(hdc, pointSize)
{
    LOGFONT lf;
    FLOAT dpiX, dpiY;
    POINT pt;
    OUTLINETEXTMETRIC otm;

    if (LE_FAILURE(status)) {
        return;
    }

    SaveDC(hdc);

    SetGraphicsMode(hdc, GM_ADVANCED);
    ModifyWorldTransform(hdc, NULL, MWT_IDENTITY);
    SetViewportOrgEx(hdc, 0, 0, NULL);
    SetWindowOrgEx(hdc, 0, 0, NULL);

    dpiX = (FLOAT) GetDeviceCaps(hdc, LOGPIXELSX);
    dpiY = (FLOAT) GetDeviceCaps(hdc, LOGPIXELSY);

#if 1
    pt.x = (int) (pointSize * dpiX / 72);
    pt.y = (int) (pointSize * dpiY / 72);

    DPtoLP(hdc, &pt, 1);
#else
    pt.x = pt.y = pointSize;
#endif

    lf.lfHeight = - pt.y;
    lf.lfWidth = 0;
    lf.lfEscapement = 0;
    lf.lfOrientation = 0;
    lf.lfWeight = 0;
    lf.lfItalic = 0;
    lf.lfUnderline = 0;
    lf.lfStrikeOut = 0;
    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfOutPrecision = 0;
    lf.lfClipPrecision = 0;
    lf.lfQuality = 0;
    lf.lfPitchAndFamily = 0;

    lstrcpy(lf.lfFaceName, faceName);

    fFont = CreateFontIndirect(&lf);

    if (fFont == NULL) {
        status = RFI_FONT_FILE_NOT_FOUND_ERROR;
        return;
    }

    SelectObject(hdc, fFont);

    UINT ret = GetOutlineTextMetrics(hdc, sizeof otm, &otm);

    if (ret == 0) {
        status = RFI_MISSING_FONT_TABLE_ERROR;
        goto restore;
    }

    fUnitsPerEM = otm.otmEMSquare;
    fAscent  = otm.otmTextMetrics.tmAscent;
    fDescent = otm.otmTextMetrics.tmDescent;
    fLeading = otm.otmTextMetrics.tmExternalLeading;

    status = initMapper();

    if (LE_FAILURE(status)) {
        goto restore;
    }

    status = initFontTableCache();

restore:
    RestoreDC(hdc, -1);
}

GDIFontInstance::GDIFontInstance(HDC hdc, const char *faceName, le_int16 pointSize, RFIErrorCode &status)
  : fHdc(hdc), fFont(NULL), RenderingFontInstance(hdc, pointSize)
{
    LOGFONTA lf;
    FLOAT dpiX, dpiY;
    POINT pt;
    OUTLINETEXTMETRIC otm;

    if (LE_FAILURE(status)) {
        return;
    }

    SaveDC(hdc);

    SetGraphicsMode(hdc, GM_ADVANCED);
    ModifyWorldTransform(hdc, NULL, MWT_IDENTITY);
    SetViewportOrgEx(hdc, 0, 0, NULL);
    SetWindowOrgEx(hdc, 0, 0, NULL);

    dpiX = (FLOAT) GetDeviceCaps(hdc, LOGPIXELSX);
    dpiY = (FLOAT) GetDeviceCaps(hdc, LOGPIXELSY);

    fDeviceScaleX = dpiX / 72;
    fDeviceScaleY = dpiY / 72;

#if 1
    pt.x = (int) (pointSize * fDeviceScaleX);
    pt.y = (int) (pointSize * fDeviceScaleY);

    DPtoLP(hdc, &pt, 1);
#else
    pt.x = pt.y = pointSize;
#endif

    lf.lfHeight = - pt.y;
    lf.lfWidth = 0;
    lf.lfEscapement = 0;
    lf.lfOrientation = 0;
    lf.lfWeight = 0;
    lf.lfItalic = 0;
    lf.lfUnderline = 0;
    lf.lfStrikeOut = 0;
    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfOutPrecision = 0;
    lf.lfClipPrecision = 0;
    lf.lfQuality = 0;
    lf.lfPitchAndFamily = 0;

    strcpy(lf.lfFaceName, faceName);

    fFont = CreateFontIndirectA(&lf);

    if (fFont == NULL) {
        status = RFI_FONT_FILE_NOT_FOUND_ERROR;
        return;
    }

    SelectObject(hdc, fFont);

    UINT ret = GetOutlineTextMetrics(hdc, sizeof otm, &otm);

    if (ret == 0) {
        status = RFI_MISSING_FONT_TABLE_ERROR;
        goto restore;
    }

    fUnitsPerEM = otm.otmEMSquare;
    fAscent  = otm.otmTextMetrics.tmAscent;
    fDescent = otm.otmTextMetrics.tmDescent;
    fLeading = otm.otmTextMetrics.tmExternalLeading;

    status = initMapper();

    if (LE_FAILURE(status)) {
        goto restore;
    }

    status = initFontTableCache();

restore:
    RestoreDC(hdc, -1);
}

GDIFontInstance::~GDIFontInstance()
{
#if 0
    flushFontTableCache();
    delete[] fTableCache;
#endif

    if (fFont != NULL) {
        // FIXME: call RemoveObject first?
        DeleteObject(fFont);
    }
}

const void *GDIFontInstance::readFontTable(LETag tableTag) const
{
    DWORD stag = SWAPL(tableTag);
    DWORD len = GetFontData(fHdc, stag, 0, NULL, 0);
    void *result = NULL;

    if (len != GDI_ERROR) {
        result = new char[len];
        GetFontData(fHdc, stag, 0, result, len);
    }

    return result;
}

void GDIFontInstance::getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const
{
    advance.fX = 0;
    advance.fY = 0;

    if (glyph == 0xFFFE || glyph == 0xFFFF) {
        return;
    }

    GLYPHMETRICS metrics;
    DWORD result;
    MAT2 identity = {{0, 1}, {0, 0}, {0, 0}, {0, 1}};

    result = GetGlyphOutline(fHdc, glyph, GGO_GLYPH_INDEX | GGO_METRICS, &metrics, 0, NULL, &identity);

    if (result == GDI_ERROR) {
        return;
    }

    advance.fX = metrics.gmCellIncX;
    return;
}

le_bool GDIFontInstance::getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const
{
#if 0
    hsFixedPoint2 pt;
    le_bool result;

    result = fFontInstance->getGlyphPoint(glyph, pointNumber, pt);

    if (result) {
        point.fX = xUnitsToPoints(pt.fX);
        point.fY = yUnitsToPoints(pt.fY);
    }

    return result;
#else
    return false;
#endif
}

void GDIFontInstance::drawGlyphs(void *surface, LEGlyphID *glyphs, le_uint32 count, le_int32 *dx,
    le_int32 x, le_int32 y, le_int32 width, le_int32 height) const
{
    HDC  hdc = (HDC) surface;
    RECT clip;

    clip.top    = 0;
    clip.left   = 0;
    clip.bottom = height;
    clip.right  = width;

    ExtTextOut(hdc, x, y - fAscent, ETO_CLIPPED | ETO_GLYPH_INDEX, &clip, glyphs, count, (INT *) dx);
};
