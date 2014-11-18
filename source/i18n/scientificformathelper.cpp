/*
**********************************************************************
* Copyright (c) 2014, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/scientificformathelper.h"
#include "unicode/scientificnumberformatter.h"
#include "decfmtst.h"

U_NAMESPACE_BEGIN

ScientificFormatHelper::ScientificFormatHelper(
        const DecimalFormatSymbols &dfs, UErrorCode &status)
        : fPreExponent(), fStaticSets(NULL) {
    if (U_FAILURE(status)) {
        return;
    }
    ScientificNumberFormatter::getPreExponent(dfs, fPreExponent);
    fStaticSets = DecimalFormatStaticSets::getStaticSets(status);
}

ScientificFormatHelper::ScientificFormatHelper(
        const ScientificFormatHelper &other)
        : UObject(other),
          fPreExponent(other.fPreExponent),
          fStaticSets(other.fStaticSets) {
}

ScientificFormatHelper &ScientificFormatHelper::operator=(const ScientificFormatHelper &other) {
    if (this == &other) {
        return *this;
    }
    fPreExponent = other.fPreExponent;
    fStaticSets = other.fStaticSets;
    return *this;
}

ScientificFormatHelper::~ScientificFormatHelper() {
}

UnicodeString &ScientificFormatHelper::insertMarkup(
        const UnicodeString &s,
        FieldPositionIterator &fpi,
        const UnicodeString &beginMarkup,
        const UnicodeString &endMarkup,
        UnicodeString &result,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return result;
    }
    ScientificNumberFormatter::MarkupStyle style(beginMarkup, endMarkup);
    return style.format(
            s,
            fpi,
            fPreExponent,
            *fStaticSets,
            result,
            status);
}

UnicodeString &ScientificFormatHelper::toSuperscriptExponentDigits(
        const UnicodeString &s,
        FieldPositionIterator &fpi,
        UnicodeString &result,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
      return result;
    }
    ScientificNumberFormatter::SuperscriptStyle style;
    return style.format(
            s,
            fpi,
            fPreExponent,
            *fStaticSets,
            result,
            status);
}

U_NAMESPACE_END

#endif /* !UCONFIG_NO_FORMATTING */
