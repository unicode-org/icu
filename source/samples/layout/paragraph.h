/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  Paragraph.h
 *
 *   created on: 09/06/2000
 *   created by: Eric R. Mader
 */
#ifndef __PARAGRAPH_H
#define __PARAGRAPH_H

#include "unicode/utypes.h"
#include "unicode/uscript.h"

#include "GUISupport.h"
#include "RenderingFontInstance.h"
#include "FontMap.h"

#include "ParagraphLayout.h"

U_NAMESPACE_USE

#define MARGIN 10

class LineInfo;

class Paragraph
{
public:
    Paragraph(const LEUnicode chars[], le_int32 charCount,
              const RenderingFontInstance *runFonts[], const UScriptCode runScripts[],
              const le_int32 runLimits[], le_int32 runCount);

    ~Paragraph();

    le_int32 getAscent();
    le_int32 getLineHeight();
    le_int32 getLineCount();
    void breakLines(le_int32 width, le_int32 height);
    void draw(RenderingSurface *surface, le_int32 firstLine, le_int32 lastLine);

    static Paragraph *paragraphFactory(const char *fileName, FontMap *fontMap, GUISupport *guiSupport);

private:
    ParagraphLayout *fParagraphLayout;

    le_int32         fLineCount;
    le_int32         fLinesMax;
    le_int32         fLinesGrow;
    LineInfo       **fLines;

    le_int32         fLineHeight;
    le_int32         fAscent;
    le_int32         fWidth;
    le_int32         fHeight;
};

inline le_int32 Paragraph::getLineHeight()
{
    return fLineHeight;
}

inline le_int32 Paragraph::getLineCount()
{
    return fLineCount;
}

inline le_int32 Paragraph::getAscent()
{
    return fAscent;
}


#endif


