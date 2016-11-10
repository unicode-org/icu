/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __STATETABLEPROCESSOR_H
#define __STATETABLEPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class StateTableProcessor : public SubtableProcessor
{
public:
    void process(LEGlyphStorage &glyphStorage, LEErrorCode &success);

    virtual void beginStateTable() = 0;

    virtual ByteOffset processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph, EntryTableIndex index) = 0;

    virtual void endStateTable() = 0;

protected:
    StateTableProcessor(const LEReferenceTo<MorphSubtableHeader> &morphSubtableHeader, LEErrorCode &success);
    virtual ~StateTableProcessor();

    StateTableProcessor();

    le_int16 stateSize;
    ByteOffset classTableOffset;
    ByteOffset stateArrayOffset;
    ByteOffset entryTableOffset;

    LEReferenceTo<ClassTable> classTable;
    TTGlyphID firstGlyph;
    TTGlyphID lastGlyph;

    LEReferenceTo<MorphStateTableHeader> stateTableHeader;
    LEReferenceTo<StateTableHeader> stHeader; // for convenience

private:
    StateTableProcessor(const StateTableProcessor &other); // forbid copying of this class
    StateTableProcessor &operator=(const StateTableProcessor &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
