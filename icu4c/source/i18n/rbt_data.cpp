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
    setVariables = uhash_open(0, &status);
}

TransliterationRuleData::~TransliterationRuleData() {
    if (variableNames != 0) {
        uhash_close(variableNames);
    }
    if (setVariables != 0) {
        uhash_close(setVariables);
    }
}

void
TransliterationRuleData::defineVariable(const UnicodeString& name,
                                        UChar value,
                                        UErrorCode& status) {
    uhash_putKey(variableNames, name.hashCode() & 0x7FFFFFFF,
                 (void*) value,
                 &status);
}

void
TransliterationRuleData::defineVariable(const UnicodeString& name,
                                        UChar standIn,
                                        UnicodeSet* adoptedSet,
                                        UErrorCode& status) {
    defineVariable(name, standIn, status);
    defineSet(standIn, adoptedSet, status);
}

void
TransliterationRuleData::defineSet(UChar standIn,
                                   UnicodeSet* adoptedSet,
                                   UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    if (adoptedSet == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    uhash_putKey(setVariables, (int32_t) (standIn & 0x7FFFFFFF),
                 adoptedSet,
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

UnicodeSet*
TransliterationRuleData::lookupSet(UChar standIn) const {
    void* value = uhash_get(setVariables, (int32_t) (standIn & 0x7FFFFFFF));
    return (UnicodeSet*) value;
}

bool_t
TransliterationRuleData::isVariableDefined(const UnicodeString& name) const {
    return 0 != uhash_get(variableNames, name.hashCode() & 0x7FFFFFFF);
}
