/*
 * @(#)LookupTables.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutTables.h"
#include "LookupTables.h"
#include "LESwaps.h"

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
LookupSegment *BinarySearchLookupTable::lookupSegment(LookupSegment *segments, le_uint32 glyph)
{
    le_int16 unity = SWAPW(unitSize);
    le_int16 probe = SWAPW(searchRange);
    le_int16 extra = SWAPW(rangeShift);
    LookupSegment *entry = segments;
    LookupSegment *trial = (LookupSegment *) ((char *) entry + extra);

    if (SWAPW(trial->lastGlyph) <= glyph)
    {
        entry = trial;
    }

    while (probe > unity)
    {
        probe >>= 1;
        trial = (LookupSegment *) ((char *) entry + probe);

        if (SWAPW(trial->lastGlyph) <= glyph)
        {
            entry = trial;
        }
    }

    if (SWAPW(entry->firstGlyph) <= glyph)
    {
        return entry;
    }

    return NULL;
}

LookupSingle *BinarySearchLookupTable::lookupSingle(LookupSingle *entries, le_uint32 glyph)
{
    le_int16 unity = SWAPW(unitSize);
    le_int16 probe = SWAPW(searchRange);
    le_int16 extra = SWAPW(rangeShift);
    LookupSingle *entry = entries;
    LookupSingle *trial = (LookupSingle *) ((char *) entry + extra);

    if (SWAPW(trial->glyph) <= glyph)
    {
        entry = trial;
    }

    while (probe > unity)
    {
        probe >>= 1;
        trial = (LookupSingle *) ((char *) entry + probe);

        if (SWAPW(trial->glyph) <= glyph)
        {
            entry = trial;
        }
    }

    if (SWAPW(entry->glyph) == glyph)
    {
        return entry;
    }

    return NULL;
}
