/*
 ******************************************************************************
 * Copyright (C) 1998-2001, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

#ifndef __FONTMAP_H
#define __FONTMAP_H

#include "layout/LETypes.h"
#include "layout/LEScripts.h"

#include "RenderingFontInstance.h"
#include "GUISupport.h"

#define BUFFER_SIZE 128

class FontMap
{
public:
    FontMap(const char *fileName, le_int16 pointSize, GUISupport *guiSupport, RFIErrorCode &status);

    virtual ~FontMap();

    virtual const RenderingFontInstance *getScriptFont(le_int32 scriptCode, RFIErrorCode &status);

protected:
    virtual const RenderingFontInstance *openFont(const char *fontName, le_int16 pointSize, RFIErrorCode &status) = 0;

    char errorMessage[256];

private:
    static char *strip(char *s);
    le_int32 getFontIndex(const char *fontName);

    le_int16 fPointSize;
    le_int32 fFontCount;

    GUISupport *fGUISupport;

    const RenderingFontInstance *fFontInstances[scriptCodeCount];
    const char *fFontNames[scriptCodeCount];
    le_int32 fFontIndices[scriptCodeCount];
};

#endif

