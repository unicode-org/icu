/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "ScriptAndLanguage.h"
#include "LESwaps.h"

LangSysTable *ScriptTable::findLanguage(LETag languageTag)
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
		return (LangSysTable *) ((char *)this + SWAPW(langSysTableOffset));
	}

	return 0;
}

ScriptTable *ScriptListTable::findScript(LETag scriptTag)
{
    le_uint16 count = SWAPW(scriptCount);
    Offset scriptTableOffset =
        OpenTypeUtilities::getTagOffset(scriptTag, scriptRecordArray, count);

    if (scriptTableOffset != 0) {
        return (ScriptTable *) ((char *)this + scriptTableOffset);
    }

    return 0;
}

LangSysTable *ScriptListTable::findLanguage(LETag scriptTag, LETag languageTag)
{
    ScriptTable *scriptTable = findScript(scriptTag);

    if (scriptTable == 0) {
        return 0;
    }

    return scriptTable->findLanguage(languageTag);
}
