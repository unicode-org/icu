/*
 **********************************************************************
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

#include "RunArrays.h"

le_int32 RunArray::ensureCapacity()
{
    if (fCount >= fCapacity) {
        if (fCapacity == 0) {
            fCapacity = INITIAL_CAPACITY;
            init(fCapacity);
        } else {
            fCapacity += (fCapacity < CAPACITY_GROW_LIMIT ? fCapacity : CAPACITY_GROW_LIMIT);
            grow(fCapacity);
        }
    }

    return fCount++;
}

void RunArray::init(le_int32 capacity)
{
    fLimits = LE_NEW_ARRAY(le_int32, capacity);
}

void RunArray::grow(le_int32 newCapacity)
{
    fLimits = (le_int32 *) LE_GROW_ARRAY(fLimits, newCapacity);
}

le_int32 RunArray::add(le_int32 limit)
{
    if (fClientArrays) {
        return -1;
    }

    le_int32  index  = ensureCapacity();
    le_int32 *limits = (le_int32 *) fLimits;

    limits[index] = limit;

    return index;
}

void FontRuns::init(le_int32 capacity)
{
    RunArray::init(capacity);
    fFonts = LE_NEW_ARRAY(const LEFontInstance *, capacity);
}

void FontRuns::grow(le_int32 capacity)
{
    RunArray::grow(capacity);
    fFonts = (const LEFontInstance **) LE_GROW_ARRAY(fFonts, capacity);
}

le_int32 FontRuns::add(const LEFontInstance *font, le_int32 limit)
{
    le_int32 index = RunArray::add(limit);

    if (index >= 0) {
        LEFontInstance **fonts = (LEFontInstance **) fFonts;

        fonts[index] = (LEFontInstance *) font;
    }

    return index;
}

const LEFontInstance *FontRuns::getFont(le_int32 run) const
{
    if (run < 0 || run >= getCount()) {
        return NULL;
    }

    return fFonts[run];
}

void ValueRuns::init(le_int32 capacity)
{
    RunArray::init(capacity);
    fValues = LE_NEW_ARRAY(le_int32, capacity);
}

void ValueRuns::grow(le_int32 capacity)
{
    RunArray::grow(capacity);
    fValues = (const le_int32 *) LE_GROW_ARRAY(fValues, capacity);
}

le_int32 ValueRuns::add(le_int32 value, le_int32 limit)
{
    le_int32 index = RunArray::add(limit);

    if (index >= 0) {
        le_int32 *values = (le_int32 *) fValues;

        values[index] = value;
    }

    return index;
}

le_int32 ValueRuns::getValue(le_int32 run) const
{
    if (run < 0 || run >= getCount()) {
        return -1;
    }

    return fValues[run];
}

