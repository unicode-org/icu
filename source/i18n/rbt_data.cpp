/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "rbt_data.h"
#include "hash.h"
#include "unicode/unistr.h"
#include "unicode/uniset.h"

U_NAMESPACE_BEGIN

TransliterationRuleData::TransliterationRuleData(UErrorCode& status)
 : ruleSet(status),
    variableNames(0), variables(0)
{
    if (U_FAILURE(status)) {
        return;
    }
    variableNames = new Hashtable(status);
    if (U_SUCCESS(status)) {
        variableNames->setValueDeleter(uhash_deleteUnicodeString);
    }
    variables = 0;
    variablesLength = 0;
}

TransliterationRuleData::TransliterationRuleData(const TransliterationRuleData& other) :
    ruleSet(other.ruleSet),
    variablesBase(other.variablesBase),
    variablesLength(other.variablesLength),
    segmentBase(other.segmentBase)
{
    ruleSet.setData(this); // ruleSet must already be frozen

    UErrorCode status = U_ZERO_ERROR;
    variableNames = new Hashtable(status);
    if (U_SUCCESS(status)) {
        variableNames->setValueDeleter(uhash_deleteUnicodeString);
        int32_t pos = -1;
        const UHashElement *e;
        while ((e = other.variableNames->nextElement(pos)) != 0) {
            UnicodeString* value =
                new UnicodeString(*(const UnicodeString*)e->value);
            variableNames->put(*(UnicodeString*)e->key.pointer, value, status);
        }
    }

    variables = 0;
    if (other.variables != 0) {
        variables = new UnicodeMatcher*[variablesLength];
        for (int32_t i=0; i<variablesLength; ++i) {
            variables[i] = other.variables[i]->clone();
        }
    }    
}

TransliterationRuleData::~TransliterationRuleData() {
    delete variableNames;
    if (variables != 0) {
        for (int32_t i=0; i<variablesLength; ++i) {
            delete variables[i];
        }
        delete[] variables;
    }
}

const UnicodeMatcher*
TransliterationRuleData::lookup(UChar32 standIn) const {
    int32_t i = standIn - variablesBase;
    return (i >= 0 && i < variablesLength) ? variables[i] : 0;
}

int32_t
TransliterationRuleData::lookupSegmentReference(UChar32 c) const {
    int32_t i = segmentBase - c;
    return (i >= 0 && i < segmentCount) ? i : -1;
}

U_NAMESPACE_END
