/*
 * @(#)LEGlyphFilter.h	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LEGLYPHFILTER__H
#define __LEGLYPHFILTER__H

#include "LETypes.h"

/**
 * This is a helper class that is used to
 * recognize a set of glyph indices.
 */
class LEGlyphFilter
{
public:
	/**
	 * This method is used to test a particular
	 * glyph index to see if it is in the set
	 * recognized by the filter.
	 *
	 * @param glyph - the glyph index to be tested
	 *
	 * @return true if the glyph index is in the set.
	 */
    virtual le_bool accept(LEGlyphID glyph) = 0;
};

#endif
