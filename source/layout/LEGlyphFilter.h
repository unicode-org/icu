/*
 * @(#)LEGlyphFilter.h	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001, 2002 - All Rights Reserved
 *
 */

#ifndef __LEGLYPHFILTER__H
#define __LEGLYPHFILTER__H

#include "LETypes.h"

U_NAMESPACE_BEGIN

/**
 * This is a helper class that is used to
 * recognize a set of glyph indices.
 *
 * @internal
 */
class LEGlyphFilter /* not : public UObject because this is an interface/mixin class */ {
public:
    /**
     * Destructor.
     * @draft ICU 2.4
     */
    virtual inline ~LEGlyphFilter() {};

    /**
     * This method is used to test a particular
     * glyph index to see if it is in the set
     * recognized by the filter.
     *
     * @param glyph - the glyph index to be tested
     *
     * @return true if the glyph index is in the set.
     *
     * @internal
     */
    virtual le_bool accept(LEGlyphID glyph) const = 0;
};

U_NAMESPACE_END
#endif
