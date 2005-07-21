/*
 ******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#ifndef __GNOMEFONTMAP_H
#define __GNOMEFONTMAP_H

#include "freetype/freetype.h"

#include "unicode/uscript.h"

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

#include "GUISupport.h"
#include "FontMap.h"

#define BUFFER_SIZE 128

class GnomeFontMap : public FontMap
{
 public:
    GnomeFontMap(TT_Engine engine, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, LEErrorCode &status);

    virtual ~GnomeFontMap();

 protected:
    virtual const LEFontInstance *openFont(const char *fontName, le_int16 pointSize, LEErrorCode &status);

 private:
    TT_Engine fEngine;
};

#endif
