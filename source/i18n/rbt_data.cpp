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

TransliterationRuleData::TransliterationRuleData(UErrorCode& status) :
    variableNames(0), setVariables(0) {
    if (U_FAILURE(status)) {
        return;
    }
    variableNames = new Hashtable(status);
    setVariables = 0;
    setVariablesLength = 0;
}

TransliterationRuleData::~TransliterationRuleData() {
    delete variableNames;
    delete[] setVariables;
}

void
TransliterationRuleData::defineVariable(const UnicodeString& name,
                                        UChar value,
                                        UErrorCode& status) {
    int32_t v = value | 0x10000; // Set bit 16
    variableNames->put(name, (void*) v, status);
}

UChar
TransliterationRuleData::lookupVariable(const UnicodeString& name,
                                        UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    void* value = variableNames->get(name);
    /* Even U+0000 can be stored in the table because we set
     * bit 16 in defineVariable().
     */
    if (value == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return (UChar) (int32_t) (unsigned long) value;
}

const UnicodeSet*
TransliterationRuleData::lookupSet(UChar standIn) const {
    int32_t i = standIn - setVariablesBase;
    return (i >= 0 && i < setVariablesLength) ? setVariables[i] : 0;
}

bool_t
TransliterationRuleData::isVariableDefined(const UnicodeString& name) const {
    return 0 != variableNames->get(name);
}
