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

#ifndef __CMAPS_H
#define __CMAPS_H

#include "LETypes.h"
#include "sfnt.h"

class CMAPMapper
{
public:
	virtual LEGlyphID unicodeToGlyph(LEUnicode32 unicode32) const = 0;

	virtual ~CMAPMapper();

	static CMAPMapper *createUnicodeMapper(const CMAPTable *cmap);

protected:
	CMAPMapper(const CMAPTable *cmap);

	CMAPMapper() {};

private:
	const CMAPTable *fcmap;
};

class CMAPFormat4Mapper : public CMAPMapper
{
public:
	CMAPFormat4Mapper(const CMAPTable *cmap, const CMAPFormat4Encoding *header);

	virtual ~CMAPFormat4Mapper();

	virtual LEGlyphID unicodeToGlyph(LEUnicode32 unicode32) const;

protected:
	CMAPFormat4Mapper() {};

private:
	le_uint16		fEntrySelector;
	le_uint16		fRangeShift;
	const le_uint16 *fEndCodes;
	const le_uint16 *fStartCodes;
	const le_uint16 *fIdDelta;
	const le_uint16 *fIdRangeOffset;
};

class CMAPGroupMapper : public CMAPMapper
{
public:
	CMAPGroupMapper(const CMAPTable *cmap, const CMAPGroup *groups, le_uint32 nGroups);

	virtual ~CMAPGroupMapper();

	virtual LEGlyphID unicodeToGlyph(LEUnicode32 unicode32) const;

protected:
	CMAPGroupMapper() {};

private:
	le_int32 fPower;
	le_int32 fRangeOffset;
	const CMAPGroup *fGroups;
};

inline CMAPMapper::CMAPMapper(const CMAPTable *cmap)
	: fcmap(cmap)
{
	// nothing else to do
}

inline CMAPMapper::~CMAPMapper()
{
	delete[] (char *) fcmap;
}

#endif

