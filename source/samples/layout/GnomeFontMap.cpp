/*
 ******************************************************************************
 * Copyright (C) 1998-2001, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#include <stdio.h>

#include "freetype/freetype.h"

#include "RenderingFontInstance.h"
#include "GnomeFontInstance.h"

#include "GUISupport.h"
#include "FontMap.h"
#include "GnomeFontMap.h"

GnomeFontMap::GnomeFontMap(TT_Engine engine, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, RFIErrorCode &status)
    : FontMap(fileName, pointSize, guiSupport, status), fEngine(engine)
{
    // nothing to do?
}

GnomeFontMap::~GnomeFontMap()
{
    // anything?
}

const RenderingFontInstance *GnomeFontMap::openFont(const char *fontName, le_int16 pointSize, RFIErrorCode &status)
{
    return new GnomeFontInstance(fEngine, fontName, pointSize, status);
}
