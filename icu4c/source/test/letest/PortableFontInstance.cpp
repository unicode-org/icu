/*
 * @(#)PortableFontInstance.cpp	1.2 99/12/14
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * Portions Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

#include <stdio.h>

#include "LETypes.h"
#include "LEFontInstance.h"
#include "LESwaps.h"

#include "PortableFontInstance.h"

#include "sfnt.h"

PortableFontInstance::PortableFontInstance(char *fileName, float pointSize, PFIErrorCode &status)
    : fFile(NULL), fUnitsPerEM(0), fPointSize(pointSize), fDirectory(NULL), fCMAPMapper(NULL),
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

	fDirectory = (const SFNTDirectory *) new char[dirSize];

    if (fDirectory == NULL) {
        status = PFI_OUT_OF_MEMORY_ERROR;
        goto error_exit;
    }

	fseek(fFile, 0L, SEEK_SET);
	fread((void *) fDirectory, sizeof(char), dirSize, fFile);

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
	    delete fCMAPMapper;
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
	    le_uint16 probe = 1 << SWAPW(fDirectory->entrySelector);
	    le_uint16 rangeShift = SWAPW(fDirectory->rangeShift) >> 4;

	    if (SWAPL(fDirectory->tableDirectory[rangeShift].tag) <= tag) {
		    table = rangeShift;
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
