/*
 *  %W% %E%
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

#include "LETypes.h"
#include "LESwaps.h"

#include "sfnt.h"
#include "cmaps.h"

//
// Finds the high bit by binary searching
// through the bits in value.
//
le_int8 highBit(le_uint32 value)
{
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

CMAPMapper *CMAPMapper::createUnicodeMapper(const CMAPTable *cmap)
{
	le_uint16 i;
	le_uint16 nSubtables = SWAPW(cmap->numberSubtables);
	const CMAPEncodingSubtable *subtable = NULL;
	le_uint32 offset1 = 0, offset10 = 0;

	for (i = 0; i < nSubtables; i += 1) {
		const CMAPEncodingSubtableHeader *esh = &cmap->encodingSubtableHeaders[i];

		if (SWAPW(esh->platformID) == 3) {
			switch (SWAPW(esh->platformSpecificID)) {
			case 1:
				offset1 = SWAPL(esh->encodingOffset);
				break;

			case 10:
				offset10 = SWAPL(esh->encodingOffset);
				break;
			}
		}
	}


	if (offset10 != 0)
	{
		subtable = (const CMAPEncodingSubtable *) ((const char *) cmap + offset10);
	} else if (offset1 != 0) {
		subtable = (const CMAPEncodingSubtable *) ((const char *) cmap + offset1);
	} else {
		return NULL;
	}

	switch (SWAPW(subtable->format)) {
	case 4:
		return new CMAPFormat4Mapper(cmap, (const CMAPFormat4Encoding *) subtable);

	case 12:
	{
		const CMAPFormat12Encoding *encoding = (const CMAPFormat12Encoding *) subtable;

		return new CMAPGroupMapper(cmap, encoding->groups, SWAPL(encoding->nGroups));
	}

	default:
		break;
	}

	return NULL;
}

CMAPFormat4Mapper::CMAPFormat4Mapper(const CMAPTable *cmap, const CMAPFormat4Encoding *header)
	: CMAPMapper(cmap)
{
	le_uint16 segCount = SWAPW(header->segCountX2) / 2;

	fEntrySelector = SWAPW(header->entrySelector);
	fRangeShift = SWAPW(header->rangeShift) / 2;
	fEndCodes = &header->endCodes[0];
	fStartCodes = &header->endCodes[segCount + 1]; // + 1 for reservedPad...
	fIdDelta = &fStartCodes[segCount];
	fIdRangeOffset = &fIdDelta[segCount];
}

LEGlyphID CMAPFormat4Mapper::unicodeToGlyph(LEUnicode32 unicode32) const
{
	if (unicode32 >= 0x10000) {
		return 0;
	}

	LEUnicode16 unicode = (LEUnicode16) unicode32;
	le_uint16 index = 0;
	le_uint16 probe = 1 << fEntrySelector;
	LEGlyphID result = 0;

	if (SWAPW(fStartCodes[fRangeShift]) <= unicode) {
		index = fRangeShift;
	}

	while (probe > (1 << 0)) {
		probe >>= 1;

		if (SWAPW(fStartCodes[index + probe]) <= unicode) {
			index += probe;
		}
	}

	if (unicode >= SWAPW(fStartCodes[index]) && unicode <= SWAPW(fEndCodes[index])) {
		if (fIdRangeOffset[index] == 0) {
			result = (LEGlyphID) unicode;
		} else {
			le_uint16 offset = unicode - SWAPW(fStartCodes[index]);
			le_uint16 rangeOffset = SWAPW(fIdRangeOffset[index]);
			le_uint16 *glyphIndexTable = (le_uint16 *) ((char *) &fIdRangeOffset[index] + rangeOffset);

			result = SWAPW(glyphIndexTable[offset]);
		}

		result += SWAPW(fIdDelta[index]);
	} else {
		result = 0;
	}

	return result;
}

CMAPFormat4Mapper::~CMAPFormat4Mapper()
{
	// parent destructor does it all
}

CMAPGroupMapper::CMAPGroupMapper(const CMAPTable *cmap, const CMAPGroup *groups, le_uint32 nGroups)
	: CMAPMapper(cmap), fGroups(groups)
{
    le_uint8 bit = highBit(nGroups);
    fPower = 1 << bit;
    fRangeOffset = nGroups - fPower;
}

LEGlyphID CMAPGroupMapper::unicodeToGlyph(LEUnicode32 unicode32) const
{
    le_int32 probe = fPower;
    le_int32 range = 0;

    if (SWAPL(fGroups[fRangeOffset].startCharCode) <= unicode32) {
        range = fRangeOffset;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (SWAPL(fGroups[range + probe].startCharCode) <= unicode32) {
            range += probe;
        }
    }

    if (SWAPL(fGroups[range].startCharCode) <= unicode32 && SWAPL(fGroups[range].endCharCode) >= unicode32) {
        return (LEGlyphID) (SWAPL(fGroups[range].startGlyphCode) + unicode32 - SWAPL(fGroups[range].startCharCode));
    }

    return 0;
}

CMAPGroupMapper::~CMAPGroupMapper()
{
	// parent destructor does it all
}

