/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "ScriptAndLanguage.h"
#include "GlyphLookupTables.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

le_bool GlyphLookupTableHeader::coversScript(LETag scriptTag) const
{
    const ScriptListTable *scriptListTable = (const ScriptListTable *) ((char *)this + SWAPW(scriptListOffset));

    return scriptListTable->findScript(scriptTag) != NULL;
}

le_bool GlyphLookupTableHeader::coversScriptAndLanguage(LETag scriptTag, LETag languageTag) const
{
    const ScriptListTable *scriptListTable = (const ScriptListTable *) ((char *)this + SWAPW(scriptListOffset));

    return scriptListTable->findLanguage(scriptTag, languageTag, TRUE) != NULL;
}

U_NAMESPACE_END
