/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
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

#include "layout/LEFontInstance.h"
#include "layout/ParagraphLayout.h"

#include "GUISupport.h"
#include "RenderingSurface.h"
//#include "FontMap.h"

U_NAMESPACE_USE

#define MARGIN 10

#if 0
class LineInfo;
#endif

class Paragraph
{
public:
    Paragraph(const LEUnicode chars[], le_int32 charCount, const FontRuns *fontRuns, LEErrorCode &status);

    ~Paragraph();

    le_int32 getAscent();
    le_int32 getLineHeight();
    le_int32 getLineCount();
    void breakLines(le_int32 width, le_int32 height);
    void draw(RenderingSurface *surface, le_int32 firstLine, le_int32 lastLine);

    static Paragraph *paragraphFactory(const char *fileName, const LEFontInstance *font, GUISupport *guiSupport);

private:
    ParagraphLayout *fParagraphLayout;

    le_int32         fLineCount;
    le_int32         fLinesMax;
    le_int32         fLinesGrow;

    const ParagraphLayout::Line **fLines;
          LEUnicode *fChars;

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


