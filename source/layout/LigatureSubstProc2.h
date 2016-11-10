/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __LIGATURESUBSTITUTIONPROCESSOR2_H
#define __LIGATURESUBSTITUTIONPROCESSOR2_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "StateTableProcessor2.h"
#include "LigatureSubstitution.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

#define nComponents 16

class LigatureSubstitutionProcessor2 : public StateTableProcessor2
{
public:
    virtual void beginStateTable();

    virtual le_uint16 processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph, 
                                        EntryTableIndex2 index, LEErrorCode &success);

    virtual void endStateTable();

    LigatureSubstitutionProcessor2(const LEReferenceTo<MorphSubtableHeader2> &morphSubtableHeader, LEErrorCode &success);
    virtual ~LigatureSubstitutionProcessor2();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @deprecated ICU 54. See {@link icu::LayoutEngine}
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @deprecated ICU 54. See {@link icu::LayoutEngine}
     */
    static UClassID getStaticClassID();

private:
    LigatureSubstitutionProcessor2();

protected:
    le_uint32 ligActionOffset;
    le_uint32 componentOffset;
    le_uint32 ligatureOffset;

    LEReferenceToArrayOf<LigatureSubstitutionStateEntry2> entryTable;

    le_int32 componentStack[nComponents];
    le_int16 m;

    const LEReferenceTo<LigatureSubstitutionHeader2> ligatureSubstitutionHeader;

};

U_NAMESPACE_END
#endif
