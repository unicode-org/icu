/*
 * @(#)ScriptAndLanguage.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __SCRIPTANDLANGUAGE_H
#define __SCRIPTANDLANGUAGE_H

#include "LETypes.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

typedef TagAndOffsetRecord LangSysRecord;

struct LangSysTable
{
    Offset    lookupOrderOffset;
    le_uint16 reqFeatureIndex;
    le_uint16 featureCount;
    le_uint16 featureIndexArray[ANY_NUMBER];
};

struct ScriptTable
{
    Offset				defaultLangSysTableOffset;
    le_uint16			langSysCount;
    LangSysRecord		langSysRecordArray[ANY_NUMBER];

    const LangSysTable	*findLanguage(LETag languageTag) const;
};

typedef TagAndOffsetRecord ScriptRecord;

struct ScriptListTable
{
    le_uint16			scriptCount;
    ScriptRecord		scriptRecordArray[ANY_NUMBER];

    const ScriptTable	*findScript(LETag scriptTag) const;
    const LangSysTable	*findLanguage(LETag scriptTag, LETag languageTag) const;
};

U_NAMESPACE_END
#endif

