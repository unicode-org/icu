/*
 *
 * (C) Copyright IBM Corp.  and others 2013 - All Rights Reserved
 *
 */

#ifndef __CONTEXTUALGLYPHINSERTIONPROCESSOR2_H
#define __CONTEXTUALGLYPHINSERTIONPROCESSOR2_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "StateTableProcessor2.h"
#include "ContextualGlyphInsertionProc2.h"
#include "ContextualGlyphInsertion.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class ContextualGlyphInsertionProcessor2 : public StateTableProcessor2
{
public:
    virtual void beginStateTable();

    virtual le_uint16 processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph, EntryTableIndex2 index);

    virtual void endStateTable();

    ContextualGlyphInsertionProcessor2(const MorphSubtableHeader2 *morphSubtableHeader);
    virtual ~ContextualGlyphInsertionProcessor2();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.8
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.8
     */
    static UClassID getStaticClassID();

private:
    ContextualGlyphInsertionProcessor2();

protected:
    le_int32 markGlyph;
    const le_uint16* insertionTable;
    const ContextualGlyphInsertionStateEntry2 *entryTable;
    const ContextualGlyphInsertionHeader2 *contextualGlyphHeader;

};

U_NAMESPACE_END
#endif
