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
    variablesLength(other.variablesLength)
{
    UErrorCode status = U_ZERO_ERROR;
    variableNames = new Hashtable(status);
    if (U_SUCCESS(status)) {
        variableNames->setValueDeleter(uhash_deleteUnicodeString);
        int32_t pos = -1;
        const UHashElement *e;
        while ((e = other.variableNames->nextElement(pos)) != 0) {
            UnicodeString* value =
                new UnicodeString(*(const UnicodeString*)e->value.pointer);
            variableNames->put(*(UnicodeString*)e->key.pointer, value, status);
        }
    }

    variables = 0;
    if (other.variables != 0) {
        variables = new UnicodeFunctor*[variablesLength];
        for (int32_t i=0; i<variablesLength; ++i) {
            variables[i] = other.variables[i]->clone();
        }
    }    

    // Do this last, _after_ setting up variables[].
    ruleSet.setData(this); // ruleSet must already be frozen
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

UnicodeFunctor*
TransliterationRuleData::lookup(UChar32 standIn) const {
    int32_t i = standIn - variablesBase;
    return (i >= 0 && i < variablesLength) ? variables[i] : 0;
}

UnicodeMatcher*
TransliterationRuleData::lookupMatcher(UChar32 standIn) const {
    UnicodeFunctor *f = lookup(standIn);
    return (f != 0) ? f->toMatcher() : 0;
}

UnicodeReplacer*
TransliterationRuleData::lookupReplacer(UChar32 standIn) const {
    UnicodeFunctor *f = lookup(standIn);
    return (f != 0) ? f->toReplacer() : 0;
}

U_NAMESPACE_END
