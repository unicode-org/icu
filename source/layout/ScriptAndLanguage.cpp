/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "ScriptAndLanguage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const LangSysTable *ScriptTable::findLanguage(LETag languageTag) const
{
    le_uint16 count = SWAPW(langSysCount);
    Offset langSysTableOffset = defaultLangSysTableOffset;

    if (count > 0) {
        Offset foundOffset =
            OpenTypeUtilities::getTagOffset(languageTag, langSysRecordArray, count);

        if (foundOffset != 0) {
            langSysTableOffset = foundOffset;
        }
    }

	if (langSysTableOffset != 0) {
		return (const LangSysTable *) ((char *)this + SWAPW(langSysTableOffset));
	}

	return 0;
}

const ScriptTable *ScriptListTable::findScript(LETag scriptTag) const
{
    le_uint16 count = SWAPW(scriptCount);
    Offset scriptTableOffset =
        OpenTypeUtilities::getTagOffset(scriptTag, scriptRecordArray, count);

    if (scriptTableOffset != 0) {
        return (const ScriptTable *) ((char *)this + scriptTableOffset);
    }

    return 0;
}

const LangSysTable *ScriptListTable::findLanguage(LETag scriptTag, LETag languageTag) const
{
    const ScriptTable *scriptTable = findScript(scriptTag);

    if (scriptTable == 0) {
        return 0;
    }

    return scriptTable->findLanguage(languageTag);
}

U_NAMESPACE_END
