
/*
 * @(#)PortableFontInstance.h	1.1 99/11/22
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

#ifndef __PORTABLEFONTINSTANCE_H
#define __PORTABLEFONTINSTANCE_H

#include <stdio.h>

#include "LETypes.h"
#include "LEFontInstance.h"

#include "sfnt.h"
#include "cmaps.h"

enum PFIErrorCode {
    PFI_NO_ERROR = 0,

    PFI_FONT_FILE_NOT_FOUND_ERROR = 1,
    PFI_MISSING_FONT_TABLE_ERROR  = 2,
    PFI_OUT_OF_MEMORY_ERROR       = 3
};

#ifndef XP_CPLUSPLUS
typedef enum PFIErrorCode PFIErrorCode;
#endif

class PortableFontInstance : public LEFontInstance
{
private:
	FILE *fFile;

    float fUnitsPerEM;
    float fPointSize;

	const SFNTDirectory *fDirectory;

	CMAPMapper *fCMAPMapper;

	const HMTXTable *fHMTXTable;
	le_uint16 fNumGlyphs;
	le_uint16 fNumLongHorMetrics;

	const DirectoryEntry *findTable(LETag tag) const;
	const void *readTable(LETag tag, le_uint32 *length) const;
	void deleteTable(const void *table) const;

	CMAPMapper *PortableFontInstance::findUnicodeMapper();

public:
    PortableFontInstance(char *fileName, float pointSize, PFIErrorCode &status);

    virtual ~PortableFontInstance();

    virtual const void *getFontTable(LETag tableTag) const
    {
		le_uint32 length;

        return readTable(tableTag, &length);
    };

    virtual le_bool canDisplay(LEUnicode32 ch) const
    {
        return (le_bool) fCMAPMapper->unicodeToGlyph(ch) != 0;
    };

    virtual le_int32 getUnitsPerEM() const
    {
        return (le_int32) fUnitsPerEM;
    };

    virtual le_int32 getLineHeight() const
    {
        // this is a cheap hack!!
	return (le_int32) fPointSize;
    };

    virtual void mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, const LECharMapper *mapper, LEGlyphID glyphs[]) const;

    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const;

    virtual le_int32 getName(le_uint16 platformID, le_uint16 scriptID, le_uint16 languageID, le_uint16 nameID, LEUnicode *name) const
    {
		// This is only used for CDAC fonts, and we'll have to loose that support anyhow...
        //return (le_int32) fFontObject->getName(platformID, scriptID, languageID, nameID, name);
		if (name != NULL) {
			*name = 0;
		}

		return 0;
    };

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    float getXPixelsPerEm() const
    {
        return fPointSize;
    };

    float getYPixelsPerEm() const
    {
        return fPointSize;
    };

    float xUnitsToPoints(float xUnits) const
    {
        return (xUnits * fPointSize) / fUnitsPerEM;
    };

    float yUnitsToPoints(float yUnits) const
    {
        return (yUnits * fPointSize) / fUnitsPerEM;
    };

    void unitsToPoints(LEPoint &units, LEPoint &points) const
    {
        points.fX = xUnitsToPoints(units.fX);
        points.fY = yUnitsToPoints(units.fY);
    }

    float xPixelsToUnits(float xPixels) const
    {
        return (xPixels * fUnitsPerEM) / fPointSize;
    };

    float yPixelsToUnits(float yPixels) const
    {
        return (yPixels * fUnitsPerEM) / fPointSize;
    };

    void pixelsToUnits(LEPoint &pixels, LEPoint &units) const
    {
        units.fX = xPixelsToUnits(pixels.fX);
        units.fY = yPixelsToUnits(pixels.fY);
    };

    void transformFunits(float xFunits, float yFunits, LEPoint &pixels) const;
};

#endif
