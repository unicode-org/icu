/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
 *
 */

#ifndef __STATETABLEPROCESSOR2_H
#define __STATETABLEPROCESSOR2_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor2.h"
#include "LookupTables.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class StateTableProcessor2 : public SubtableProcessor2
{
public:
    void process(LEGlyphStorage &glyphStorage);

    virtual void beginStateTable() = 0;

    virtual le_uint16 processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph, EntryTableIndex2 index) = 0;

    virtual void endStateTable() = 0;

protected:
    StateTableProcessor2(const MorphSubtableHeader2 *morphSubtableHeader);
    virtual ~StateTableProcessor2();

    StateTableProcessor2();

    le_int32  dir;
    le_uint16 format;
    le_uint32 nClasses;
    le_uint32 classTableOffset;
    le_uint32 stateArrayOffset;
    le_uint32 entryTableOffset;

    const LookupTable *classTable;
    const EntryTableIndex2 *stateArray;
    const MorphStateTableHeader2 *stateTableHeader;

private:
    StateTableProcessor2(const StateTableProcessor2 &other); // forbid copying of this class
    StateTableProcessor2 &operator=(const StateTableProcessor2 &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
