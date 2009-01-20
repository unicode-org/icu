/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 2008 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphSubstitutionTables.h"
#include "LookupProcessor.h"
#include "ExtensionSubtables.h"
#include "GlyphIterator.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN


// FIXME: should look at the format too... maybe have a sub-class for it?
le_uint32 ExtensionSubtable::process(const LookupProcessor *lookupProcessor, le_uint16 lookupType,
                                      GlyphIterator *glyphIterator, const LEFontInstance *fontInstance, LEErrorCode& success) const
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    le_uint16 elt = SWAPW(extensionLookupType);

    if (elt != lookupType) {
        le_uint32 extOffset = SWAPL(extensionOffset);
        LookupSubtable *subtable = (LookupSubtable *) ((char *) this + extOffset);

        return lookupProcessor->applySubtable(subtable, elt, glyphIterator, fontInstance, success);
    }

    return 0;
}

U_NAMESPACE_END
