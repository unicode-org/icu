/*
 * @(#)ScriptAndLanguage.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __SCRIPTANDLANGUAGE_H
#define __SCRIPTANDLANGUAGE_H

#include "LETypes.h"
#include "OpenTypeTables.h"

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
    Offset          defaultLangSysTableOffset;
    le_uint16       langSysCount;
    LangSysRecord   langSysRecordArray[ANY_NUMBER];

    LangSysTable    *findLanguage(LETag languageTag);
};

typedef TagAndOffsetRecord ScriptRecord;

struct ScriptListTable
{
    le_uint16       scriptCount;
    ScriptRecord    scriptRecordArray[ANY_NUMBER];

    ScriptTable     *findScript(LETag scriptTag);
    LangSysTable    *findLanguage(LETag scriptTag, LETag languageTag);
};

#endif

