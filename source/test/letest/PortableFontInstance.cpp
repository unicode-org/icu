/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  PortableFontInstance.cpp
 *
 *   created on: 11/22/1999
 *   created by: Eric R. Mader
 */

#include <stdio.h>

#include "LETypes.h"
#include "LEFontInstance.h"
#include "LESwaps.h"

#include "PortableFontInstance.h"

#include "sfnt.h"

#include <string.h>

//
// Finds the high bit by binary searching
// through the bits in n.
//
le_int8 PortableFontInstance::highBit(le_int32 value)
{
    if (value <= 0) {
        return -32;
    }

    le_uint8 bit = 0;

    if (value >= 1 << 16) {
        value >>= 16;
        bit += 16;
    }

    if (value >= 1 << 8) {
        value >>= 8;
        bit += 8;
    }

    if (value >= 1 << 4) {
        value >>= 4;
        bit += 4;
    }

    if (value >= 1 << 2) {
        value >>= 2;
        bit += 2;
    }

    if (value >= 1 << 1) {
        value >>= 1;
        bit += 1;
    }

    return bit;
}

PortableFontInstance::PortableFontInstance(char *fileName, float pointSize, PFIErrorCode &status)
    : fFile(NULL), fUnitsPerEM(0), fPointSize(pointSize), fDirectory(NULL),
      fTableCache(NULL), fTableCacheCurr(0), fTableCacheSize(0), fCMAPMapper(NULL),
      fHMTXTable(NULL), fNumGlyphs(0), fNumLongHorMetrics(0)
{
    le_uint32 length;

    if (LE_FAILURE(status)) {
        return;
    }

    // open the font file
    fFile = fopen(fileName, "rb");

    if (fFile == NULL) {
        status = PFI_FONT_FILE_NOT_FOUND_ERROR;
        return;
    }

    // read in the directory
    SFNTDirectory tempDir;

    fread(&tempDir, sizeof tempDir, 1, fFile);

    le_int32 dirSize = sizeof tempDir + ((SWAPW(tempDir.numTables) - ANY_NUMBER) * sizeof(DirectoryEntry));
    const LETag headTag = 0x68656164; // 'head'
    const HEADTable *headTable = NULL;
    le_uint16 numTables = 0;

    fDirectory = (const SFNTDirectory *) new char[dirSize];

    if (fDirectory == NULL) {
        status = PFI_OUT_OF_MEMORY_ERROR;
        goto error_exit;
    }

    fseek(fFile, 0L, SEEK_SET);
    fread((void *) fDirectory, sizeof(char), dirSize, fFile);

    //
    // We calculate these numbers 'cause some fonts
    // have bogus values for them in the directory header.
    //
    numTables = SWAPW(fDirectory->numTables);
    fDirPower = 1 << highBit(numTables);
    fDirExtra = numTables - fDirPower;

    // read unitsPerEm from 'head' table
    headTable = (const HEADTable *) readTable(headTag, &length);

    if (headTable == NULL) {
        status = PFI_MISSING_FONT_TABLE_ERROR;
        goto error_exit;
    }

    fUnitsPerEM = (float) SWAPW(headTable->unitsPerEm);
    deleteTable(headTable);

    fCMAPMapper = findUnicodeMapper();

    if (fCMAPMapper == NULL) {
        status = PFI_MISSING_FONT_TABLE_ERROR;
        goto error_exit;
    }

    status = initFontTableCache();

    return;

error_exit:
    fclose(fFile);
    fFile = NULL;
    return;
}

PortableFontInstance::~PortableFontInstance()
{
    if (fFile != NULL) {
        fclose(fFile);

        deleteTable(fHMTXTable);

        flushFontTableCache();
        delete fCMAPMapper;

        delete[] (void *) fDirectory;
    }
};

void PortableFontInstance::deleteTable(const void *table) const
{
    delete[] (char *) table;
}

const DirectoryEntry *PortableFontInstance::findTable(LETag tag) const
{
    if (fDirectory != NULL) {
        le_uint16 table = 0;
        le_uint16 probe = fDirPower;

        if (SWAPL(fDirectory->tableDirectory[fDirExtra].tag) <= tag) {
            table = fDirExtra;
        }

        while (probe > (1 << 0)) {
            probe >>= 1;

            if (SWAPL(fDirectory->tableDirectory[table + probe].tag) <= tag) {
                table += probe;
            }
        }

        if (SWAPL(fDirectory->tableDirectory[table].tag) == tag) {
            return &fDirectory->tableDirectory[table];
        }
    }

    return NULL;
}

PFIErrorCode PortableFontInstance::initFontTableCache()
{
    fTableCacheSize = TABLE_CACHE_INIT;
    fTableCache = new TableCacheEntry[fTableCacheSize];

    if (fTableCache == 0) {
        return PFI_OUT_OF_MEMORY_ERROR;
    }

    for (int i = 0; i < fTableCacheSize; i += 1) {
        fTableCache[i].tag = 0;
        fTableCache[i].table = NULL;
    }

    return PFI_NO_ERROR;
}

void PortableFontInstance::flushFontTableCache()
{
    for (int i = fTableCacheCurr - 1; i >= 0; i -= 1) {
        delete[] (char *) fTableCache[i].table;
    }

    fTableCacheCurr = 0;
}

const void *PortableFontInstance::readTable(LETag tag, le_uint32 *length) const
{
    const DirectoryEntry *entry = findTable(tag);

    if (entry == NULL) {
        *length = 0;
        return NULL;
    }

    *length = SWAPL(entry->length);

    void *table = new char[*length];

    if (table != NULL) {
        fseek(fFile, SWAPL(entry->offset), SEEK_SET);
        fread(table, sizeof(char), *length, fFile);
    }

    return table;
}

const void *PortableFontInstance::getFontTable(LETag tableTag) const
{
    for (int i = 0; i < fTableCacheCurr; i += 1) {
        if (fTableCache[i].tag == tableTag) {
            return fTableCache[i].table;
        }
    }

    PortableFontInstance *realThis = (PortableFontInstance *) this;

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

    le_uint32 tableLength;

    realThis->fTableCache[realThis->fTableCacheCurr].tag = tableTag;
    realThis->fTableCache[realThis->fTableCacheCurr].table = (void *) realThis->readTable(tableTag, &tableLength);

    return fTableCache[realThis->fTableCacheCurr++].table;
};

CMAPMapper *PortableFontInstance::findUnicodeMapper()
{
    le_uint32 length;
    LETag cmapTag = 0x636D6170; // 'cmap'
    const CMAPTable *cmap = (CMAPTable *) readTable(cmapTag, &length);

    if (cmap == NULL) {
        return NULL;
    }

    return CMAPMapper::createUnicodeMapper(cmap);
}


void PortableFontInstance::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, const LECharMapper *mapper, LEGlyphID glyphs[]) const
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

LEGlyphID PortableFontInstance::mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const
{
    LEUnicode32 mappedChar = mapper->mapChar(ch);

    if (mappedChar == 0xFFFF || mappedChar == 0xFFFE) {
        return 0xFFFF;
    }

    if (mappedChar == 0x200C || mappedChar == 0x200D) {
        return 1;
    }

    if (fCMAPMapper == NULL) {
        return 0;
    }

    return fCMAPMapper->unicodeToGlyph(mappedChar);
}

void PortableFontInstance::getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const
{
    if (fHMTXTable == NULL) {
        LETag maxpTag = 0x6D617870; // 'maxp'
        LETag hheaTag = 0x68686561; // 'hhea'
        LETag hmtxTag = 0x686D7478; // 'hmtx'
        le_uint32 length;
        const HHEATable *hheaTable;
        const MAXPTable *maxpTable = (MAXPTable *) readTable(maxpTag, &length);
        PortableFontInstance *realThis = (PortableFontInstance *) this;

        if (maxpTable != NULL) {
            realThis->fNumGlyphs = SWAPW(maxpTable->numGlyphs);
            deleteTable(maxpTable);
        }

        hheaTable = (HHEATable *) readTable(hheaTag, &length);

        if (hheaTable != NULL) {
            realThis->fNumLongHorMetrics = SWAPW(hheaTable->numOfLongHorMetrics);
            deleteTable((void *) hheaTable);
        }

        realThis->fHMTXTable = (const HMTXTable *) readTable(hmtxTag, &length);
    }

    le_uint16 index = glyph;

    if (glyph >= fNumGlyphs || fHMTXTable == NULL) {
        advance.fX = advance.fY = 0;
        return;
    }

    if (glyph >= fNumLongHorMetrics) {
        index = fNumLongHorMetrics - 1;
    }

    advance.fX = xUnitsToPoints(SWAPW(fHMTXTable->hMetrics[index].advanceWidth));
    advance.fY = 0;
}

le_bool PortableFontInstance::getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const
{
#if 0
    hsFixedPoint2 pt;
    le_bool result;

    result = fFontInstance->getGlyphPoint(glyph, pointNumber, pt);

    if (result) {
        point.fX = xUnitsToPoints(pt.fX);
        point.fY = yUnitsToPoints(pt.fY);
    }

    return result;
#else
    return false;
#endif
}

void PortableFontInstance::transformFunits(float xFunits, float yFunits, LEPoint &pixels) const
{
    pixels.fX = xUnitsToPoints(xFunits);
    pixels.fY = yUnitsToPoints(yFunits);
}
