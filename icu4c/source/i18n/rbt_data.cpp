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
#include "uhash.h"
#include "unicode/unistr.h"

TransliterationRuleData::TransliterationRuleData(UErrorCode& status) :
    variableNames(0), setVariables(0) {
    if (U_FAILURE(status)) {
        return;
    }
    variableNames = uhash_open((UHashFunction)uhash_hashUString, &status);
    setVariables = 0;
    setVariablesLength = 0;
}

TransliterationRuleData::~TransliterationRuleData() {
    if (variableNames != 0) {
        uhash_close(variableNames);
    }
    delete[] setVariables;
}

void
TransliterationRuleData::defineVariable(const UnicodeString& name,
                                        UChar value,
                                        UErrorCode& status) {
    uhash_putKey(variableNames, name.hashCode() & 0x7FFFFFFF,
                 (void*) value,
                 &status);
}

UChar
TransliterationRuleData::lookupVariable(const UnicodeString& name,
                                        UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    void* value = uhash_get(variableNames, name.hashCode() & 0x7FFFFFFF);
    if (value == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return (UChar) (int32_t) value;
}

const UnicodeSet*
TransliterationRuleData::lookupSet(UChar standIn) const {
    int32_t i = standIn - setVariablesBase;
    return (i >= 0 && i < setVariablesLength) ? setVariables[i] : 0;
}

bool_t
TransliterationRuleData::isVariableDefined(const UnicodeString& name) const {
    return 0 != uhash_get(variableNames, name.hashCode() & 0x7FFFFFFF);
}
