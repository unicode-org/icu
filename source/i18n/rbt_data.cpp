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

TransliterationRuleData::TransliterationRuleData(UErrorCode& status) :
    variableNames(0), setVariables(0) {
    if (U_FAILURE(status)) {
        return;
    }
    variableNames = new Hashtable(status);
    if (U_SUCCESS(status)) {
        variableNames->setValueDeleter(uhash_deleteUnicodeString);
    }
    setVariables = 0;
    setVariablesLength = 0;
}

TransliterationRuleData::TransliterationRuleData(const TransliterationRuleData& other) :
    ruleSet(other.ruleSet),
    setVariablesBase(other.setVariablesBase),
    setVariablesLength(other.setVariablesLength),
    segmentBase(other.segmentBase) {

    UErrorCode status = U_ZERO_ERROR;
    variableNames = new Hashtable(status);
    if (U_SUCCESS(status)) {
        variableNames->setValueDeleter(uhash_deleteUnicodeString);
        int32_t pos = -1;
        const UHashElement *e;
        while ((e = other.variableNames->nextElement(pos)) != 0) {
            UnicodeString* value =
                new UnicodeString(*(const UnicodeString*)e->value);
            variableNames->put(*(UnicodeString*)e->key, value, status);
        }
    }

    setVariables = 0;
    if (other.setVariables != 0) {
        setVariables = new UnicodeSet*[setVariablesLength];
        for (int32_t i=0; i<setVariablesLength; ++i) {
            setVariables[i] = new UnicodeSet(*other.setVariables[i]);
        }
    }    
}

TransliterationRuleData::~TransliterationRuleData() {
    delete variableNames;
    if (setVariables != 0) {
        for (int32_t i=0; i<setVariablesLength; ++i) {
            delete setVariables[i];
        }
        delete[] setVariables;
    }
}

const UnicodeSet*
TransliterationRuleData::lookupSet(UChar standIn) const {
    int32_t i = standIn - setVariablesBase;
    return (i >= 0 && i < setVariablesLength) ? setVariables[i] : 0;
}

int32_t
TransliterationRuleData::lookupSegmentReference(UChar c) const {
    int32_t i = c - segmentBase;
    return (i >= 0 && i < 9) ? i : -1;
}
