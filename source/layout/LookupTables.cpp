/*
 * @(#)LookupTables.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutTables.h"
#include "LookupTables.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

/*
    These are the rolled-up versions of the uniform binary search.
    Someday, if we need more performance, we can un-roll them.
    
    Note: I put these in the base class, so they only have to
    be written once. Since the base class doesn't define the
    segment table, these routines assume that it's right after
    the binary search header.

    Another way to do this is to put each of these routines in one
    of the derived classes, and implement it in the others by casting
    the "this" pointer to the type that has the implementation.
*/ 
const LookupSegment *BinarySearchLookupTable::lookupSegment(const LookupSegment *segments, le_uint32 glyph) const
{
    le_int16 unity = SWAPW(unitSize);
    le_int16 probe = SWAPW(searchRange);
    le_int16 extra = SWAPW(rangeShift);
    const LookupSegment *entry = segments;
    const LookupSegment *trial = (const LookupSegment *) ((char *) entry + extra);

    if (SWAPW(trial->lastGlyph) <= glyph) {
        entry = trial;
    }

    while (probe > unity) {
        probe >>= 1;
        trial = (const LookupSegment *) ((char *) entry + probe);

        if (SWAPW(trial->lastGlyph) <= glyph) {
            entry = trial;
        }
    }

    if (SWAPW(entry->firstGlyph) <= glyph) {
        return entry;
    }

    return NULL;
}

const LookupSingle *BinarySearchLookupTable::lookupSingle(const LookupSingle *entries, le_uint32 glyph) const
{
    le_int16 unity = SWAPW(unitSize);
    le_int16 probe = SWAPW(searchRange);
    le_int16 extra = SWAPW(rangeShift);
    const LookupSingle *entry = entries;
    const LookupSingle *trial = (const LookupSingle *) ((char *) entry + extra);

    if (SWAPW(trial->glyph) <= glyph) {
        entry = trial;
    }

    while (probe > unity) {
        probe >>= 1;
        trial = (const LookupSingle *) ((char *) entry + probe);

        if (SWAPW(trial->glyph) <= glyph) {
            entry = trial;
        }
    }

    if (SWAPW(entry->glyph) == glyph) {
        return entry;
    }

    return NULL;
}

U_NAMESPACE_END
