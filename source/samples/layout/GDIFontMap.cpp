/*
 ******************************************************************************
 * Copyright (C) 1998-2001, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#include <windows.h>

#include "RenderingFontInstance.h"
#include "GDIFontInstance.h"

#include "GUISupport.h"
#include "FontMap.h"
#include "GDIFontMap.h"

GDIFontMap::GDIFontMap(HDC hdc, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, RFIErrorCode &status)
    : FontMap(fileName, pointSize, guiSupport, status), fHdc(hdc)
{
    // nothing to do?
}

GDIFontMap::~GDIFontMap()
{
    // anything?
}

const RenderingFontInstance *GDIFontMap::openFont(const char *fontName, le_int16 pointSize, RFIErrorCode &status)
{
    return new GDIFontInstance(fHdc, fontName, pointSize, status);
}
