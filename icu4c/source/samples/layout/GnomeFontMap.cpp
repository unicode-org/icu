/*
 ******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#include <stdio.h>

#include "freetype/freetype.h"

#include "layout/LEFontInstance.h"
#include "GnomeFontInstance.h"

#include "GUISupport.h"
#include "FontMap.h"
#include "GnomeFontMap.h"

GnomeFontMap::GnomeFontMap(TT_Engine engine, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, LEErrorCode &status)
    : FontMap(fileName, pointSize, guiSupport, status), fEngine(engine)
{
    // nothing to do?
}

GnomeFontMap::~GnomeFontMap()
{
    // anything?
}

const LEFontInstance *GnomeFontMap::openFont(const char *fontName, le_int16 pointSize, LEErrorCode &status)
{
    LEFontInstance *result = new GnomeFontInstance(fEngine, fontName, pointSize, status);

    if (LE_FAILURE(status)) {
      delete result;
      result = NULL;
    }

    return result;
}
