/*
 ******************************************************************************
 * Copyright (C) 1998-2001, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#ifndef __GDIFONTMAP_H
#define __GDIFONTMAP_H

#include <windows.h>

#include "unicode/uscript.h"

#include "layout/LETypes.h"

#include "FontMap.h"
#include "GUISupport.h"
#include "RenderingFontInstance.h"

#define BUFFER_SIZE 128

class GDIFontMap : public FontMap
{
public:
    GDIFontMap(HDC hdc, const char *fileName, le_int16 pointSize, GUISupport *guiSupport, RFIErrorCode &status);

    virtual ~GDIFontMap();

protected:
    virtual const RenderingFontInstance *openFont(const char *fontName, le_int16 pointSize, RFIErrorCode &status);

private:
    HDC fHdc;
};

#endif
