/*
 ******************************************************************************
 * Copyright (C) 1998-2001, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#ifndef __GNOMEFONTMAP_H
#define __GNOMEFONTMAP_H

#include "freetype/freetype.h"

#include "unicode/uscript.h"

#include "layout/LETypes.h"

#include "GUISupport.h"
#include "FontMap.h"
#include "RenderingFontInstance.h"

#define BUFFER_SIZE 128

class GnomeFontMap : public FontMap
{
 public:
    GnomeFontMap(TT_Engine engine, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, RFIErrorCode &status);

    virtual ~GnomeFontMap();

 protected:
    virtual const RenderingFontInstance *openFont(const char *fontName, le_int16 pointSize, RFIErrorCode &status);

 private:
    TT_Engine fEngine;
};

#endif
