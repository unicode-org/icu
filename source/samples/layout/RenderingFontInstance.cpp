/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  RenderingFontInstance.cpp
 *
 *   created on: 10/22/2001
 *   created by: Eric R. Mader
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "RenderingFontInstance.h"
#include "LESwaps.h"
#include "sfnt.h"
#include "cmaps.h"

#include <string.h>

#define TABLE_CACHE_INIT 5
#define TABLE_CACHE_GROW 5

struct TableCacheEntry
{
    LETag tag;
    void *table;
};

RenderingFontInstance::RenderingFontInstance(RenderingSurface *surface, le_int16 pointSize)
  : fSurface(surface), fPointSize(pointSize), fUnitsPerEM(0), fAscent(0), fDescent(), fLeading(0),
    fDeviceScaleX(1), fDeviceScaleY(1), fTableCache(NULL), fTableCacheCurr(0), fTableCacheSize(0), fMapper(NULL)
{
    // we expect the subclass to call
    // initMapper() and initFontTableCache
}

RenderingFontInstance::~RenderingFontInstance()
{
    flushFontTableCache();
    delete[] fTableCache;

    delete fMapper;
}

void RenderingFontInstance::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count,
                                       le_bool reverse, const LECharMapper *mapper, LEGlyphID glyphs[]) const
{
    le_int32 i, out = 0, dir = 1;

    if (reverse) {
        out = count - 1;
        dir = -1;
    }

    for (i = offset; i < offset + count; i += 1, out += dir) {
        LEUnicode16 high = chars[i];
        LEUnicode32 code = high;

        if (i < offset + count - 1 && high >= 0xD800 && high <= 0xDBFF) {
            LEUnicode16 low = chars[i + 1];

            if (low >= 0xDC00 && low <= 0xDFFF) {
                code = (high - 0xD800) * 0x400 + low - 0xDC00 + 0x10000;
            }
        }

        glyphs[out] = mapCharToGlyph(code, mapper);

        if (code >= 0x10000) {
            i += 1;
            glyphs[out += dir] = 0xFFFF;
        }
    }
}

LEGlyphID RenderingFontInstance::mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const
{
    LEUnicode32 mappedChar = mapper->mapChar(ch);

    if (mappedChar == 0xFFFE || mappedChar == 0xFFFF) {
        return 0xFFFF;
    }

    if (mappedChar == 0x200C || mappedChar == 0x200D) {
        return 1;
    }

    return fMapper->unicodeToGlyph(mappedChar);
}

const void *RenderingFontInstance::getFontTable(LETag tableTag) const
{
    for (int i = 0; i < fTableCacheCurr; i += 1) {
        if (fTableCache[i].tag == tableTag) {
            return fTableCache[i].table;
        }
    }

    RenderingFontInstance *realThis = (RenderingFontInstance *) this;

    if (realThis->fTableCacheCurr >= realThis->fTableCacheSize) {
        le_int32 newSize = realThis->fTableCacheSize + TABLE_CACHE_GROW;
        TableCacheEntry *newTable = new TableCacheEntry[newSize];

        // FIXME: need a better strategy than this...
        if (newTable == NULL) {
            return NULL;
        }

        memcpy(newTable, realThis->fTableCache, realThis->fTableCacheSize * sizeof realThis->fTableCache[0]);
        delete[] realThis->fTableCache;

        for (int i = realThis->fTableCacheSize; i < newSize; i += 1) {
            newTable[i].tag = 0;
            newTable[i].table = NULL;
        }

        realThis->fTableCache = newTable;
        realThis->fTableCacheSize = newSize;
    }

    realThis->fTableCache[realThis->fTableCacheCurr].tag = tableTag;
    realThis->fTableCache[realThis->fTableCacheCurr].table = (void *) realThis->readFontTable(tableTag);

    return fTableCache[realThis->fTableCacheCurr++].table;
};

RFIErrorCode RenderingFontInstance::initMapper()
{
    LETag cmapTag = 0x636D6170; // 'cmap'
    const CMAPTable *cmap = (const CMAPTable *) readFontTable(cmapTag);

    if (cmap == NULL) {
        return RFI_MISSING_FONT_TABLE_ERROR;
    }

    fMapper = CMAPMapper::createUnicodeMapper(cmap);

    if (fMapper == NULL) {
        return RFI_MISSING_FONT_TABLE_ERROR;
    }

    return RFI_NO_ERROR;
}

RFIErrorCode RenderingFontInstance::initFontTableCache()
{
    fTableCacheSize = TABLE_CACHE_INIT;
    fTableCache = new TableCacheEntry[fTableCacheSize];

    if (fTableCache == 0) {
        return RFI_OUT_OF_MEMORY_ERROR;
    }

    for (int i = 0; i < fTableCacheSize; i += 1) {
        fTableCache[i].tag = 0;
        fTableCache[i].table = NULL;
    }

    return RFI_NO_ERROR;
}

void RenderingFontInstance::flushFontTableCache()
{
    for (int i = fTableCacheCurr - 1; i >= 0; i -= 1) {
        delete[] (char *) fTableCache[i].table;
    }

    fTableCacheCurr = 0;
}
