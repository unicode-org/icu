/*
********************************************************************************
*   Copyright (C) 2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File decimfmtimpl.h
********************************************************************************
*/

#ifndef DECIMFMTIMPL_H
#define DECIMFMTIMPL_H

#include "unicode/decimfmt.h"
#include "unicode/uobject.h"
#include "unicode/utypes.h"
#include "affixpatternparser.h"
#include "digitaffixesandpadding.h"
#include "digitformatter.h"
#include "digitgrouping.h"
#include "precision.h"

U_NAMESPACE_BEGIN

class UnicodeString;
class FieldPosition;
class ValueFormatter;
class FieldPositionHandler;
class FixedDecimal;

class DecimalFormatImpl : public UObject {
public:

DecimalFormatImpl(
        NumberFormat *super,
        const Locale &locale,
        const UnicodeString &pattern,
        UErrorCode &status);
DecimalFormatImpl(
        NumberFormat *super,
        const UnicodeString &pattern,
        DecimalFormatSymbols *symbolsToAdopt,
        UParseError &parseError,
        UErrorCode &status);
DecimalFormatImpl(
        NumberFormat *super,
        const DecimalFormatImpl &other,
        UErrorCode &status);
DecimalFormatImpl &assign(
        const DecimalFormatImpl &other, UErrorCode &status);
virtual ~DecimalFormatImpl();
void adoptDecimalFormatSymbols(DecimalFormatSymbols *symbolsToAdopt);
const DecimalFormatSymbols &getDecimalFormatSymbols() const {
    return *fSymbols;
}
UnicodeString &format(
        int32_t number,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;
UnicodeString &format(
        int32_t number,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;
UnicodeString &format(
        int64_t number,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;
UnicodeString &format(
        double number,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;
UnicodeString &format(
        const DigitList &number,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;
UnicodeString &format(
        int64_t number,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;
UnicodeString &format(
        double number,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;
UnicodeString &format(
        const DigitList &number,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;
UnicodeString &format(
        const StringPiece &number,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;
UnicodeString &format(
        const VisibleDigitsWithExponent &digits,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;
UnicodeString &format(
        const VisibleDigitsWithExponent &digits,
        UnicodeString &appendTo,
        FieldPositionIterator *posIter,
        UErrorCode &status) const;

UBool operator==(const DecimalFormatImpl &) const;

UBool operator!=(const DecimalFormatImpl &other) const {
    return !(*this == other);
}

void setRoundingMode(DecimalFormat::ERoundingMode mode) {
    fRoundingMode = mode;
    fEffPrecision.fMantissa.fExactOnly = (fRoundingMode == DecimalFormat::kRoundUnnecessary);
    fEffPrecision.fMantissa.fRoundingMode = mode;
}
DecimalFormat::ERoundingMode getRoundingMode() const {
    return fRoundingMode;
}
void setFailIfMoreThanMaxDigits(UBool b) {
    fEffPrecision.fMantissa.fFailIfOverMax = b;
}
UBool isFailIfMoreThanMaxDigits() const { return fEffPrecision.fMantissa.fFailIfOverMax; }
void setMinimumSignificantDigits(int32_t newValue);
void setMaximumSignificantDigits(int32_t newValue);
void setMinMaxSignificantDigits(int32_t min, int32_t max);
void setScientificNotation(UBool newValue);
void setSignificantDigitsUsed(UBool newValue);

int32_t getMinimumSignificantDigits() const { 
        return fMinSigDigits; }
int32_t getMaximumSignificantDigits() const { 
        return fMaxSigDigits; }
UBool isScientificNotation() const { return fUseScientific; }
UBool areSignificantDigitsUsed() const { return fUseSigDigits; }
void setGroupingSize(int32_t newValue);
void setSecondaryGroupingSize(int32_t newValue);
void setMinimumGroupingDigits(int32_t newValue);
int32_t getGroupingSize() const { return fGrouping.fGrouping; }
int32_t getSecondaryGroupingSize() const { return fGrouping.fGrouping2; }
int32_t getMinimumGroupingDigits() const { return fGrouping.fMinGrouping; }
void applyPattern(const UnicodeString &pattern, UErrorCode &status);
void applyPatternFavorCurrencyPrecision(
        const UnicodeString &pattern, UErrorCode &status);
void applyPattern(
        const UnicodeString &pattern, UParseError &perror, UErrorCode &status);
void applyLocalizedPattern(const UnicodeString &pattern, UErrorCode &status);
void applyLocalizedPattern(
        const UnicodeString &pattern, UParseError &perror, UErrorCode &status);
void setCurrencyUsage(UCurrencyUsage usage, UErrorCode &status);
UCurrencyUsage getCurrencyUsage() const { return fCurrencyUsage; }
void setRoundingIncrement(double d);
double getRoundingIncrement() const;
int32_t getMultiplier() const;
void setMultiplier(int32_t m);
UChar32 getPadCharacter() const { return fAffixes.fPadChar; }
void setPadCharacter(UChar32 c) { fAffixes.fPadChar = c; }
int32_t getFormatWidth() const { return fAffixes.fWidth; }
void setFormatWidth(int32_t x) { fAffixes.fWidth = x; }
DigitAffixesAndPadding::EPadPosition getPadPosition() const {
    return fAffixes.fPadPosition;
}
void setPadPosition(DigitAffixesAndPadding::EPadPosition x) {
    fAffixes.fPadPosition = x;
}
int32_t getMinimumExponentDigits() const {
    return fEffPrecision.fMinExponentDigits;
}
void setMinimumExponentDigits(int32_t x) {
    fEffPrecision.fMinExponentDigits = x;
}
UBool isExponentSignAlwaysShown() const {
    return fOptions.fExponent.fAlwaysShowSign;
}
void setExponentSignAlwaysShown(UBool x) {
    fOptions.fExponent.fAlwaysShowSign = x;
}
UBool isDecimalSeparatorAlwaysShown() const {
    return fOptions.fMantissa.fAlwaysShowDecimal;
}
void setDecimalSeparatorAlwaysShown(UBool x) {
    fOptions.fMantissa.fAlwaysShowDecimal = x;
}
UnicodeString &getPositivePrefix(UnicodeString &result) const;
UnicodeString &getPositiveSuffix(UnicodeString &result) const;
UnicodeString &getNegativePrefix(UnicodeString &result) const;
UnicodeString &getNegativeSuffix(UnicodeString &result) const;
void setPositivePrefix(const UnicodeString &str);
void setPositiveSuffix(const UnicodeString &str);
void setNegativePrefix(const UnicodeString &str);
void setNegativeSuffix(const UnicodeString &str);
UnicodeString &toPattern(UnicodeString& result) const;
FixedDecimal &getFixedDecimal(double value, FixedDecimal &result, UErrorCode &status) const;
FixedDecimal &getFixedDecimal(DigitList &number, FixedDecimal &result, UErrorCode &status) const;
DigitList &round(DigitList &number, UErrorCode &status) const;

VisibleDigitsWithExponent &
initVisibleDigitsWithExponent(
        int64_t number,
        VisibleDigitsWithExponent &digits,
        UErrorCode &status) const;
VisibleDigitsWithExponent &
initVisibleDigitsWithExponent(
        double number,
        VisibleDigitsWithExponent &digits,
        UErrorCode &status) const;
VisibleDigitsWithExponent &
initVisibleDigitsWithExponent(
        DigitList &number,
        VisibleDigitsWithExponent &digits,
        UErrorCode &status) const;

void updatePrecision();
void updateGrouping();
void updateCurrency(UErrorCode &status);


private:
// Disallow copy and assign
DecimalFormatImpl(const DecimalFormatImpl &other);
DecimalFormatImpl &operator=(const DecimalFormatImpl &other);
NumberFormat *fSuper;
DigitList fMultiplier;
int32_t fScale;

DecimalFormat::ERoundingMode fRoundingMode;

// These fields include what the user can see and set.
// When the user updates these fields, it triggers automatic updates of
// other fields that may be invisible to user

// Updating any of the following fields triggers an update to
// fEffPrecision.fMantissa.fMin,
// fEffPrecision.fMantissa.fMax,
// fEffPrecision.fMantissa.fSignificant fields
// We have this two phase update because of backward compatibility. 
// DecimalFormat has to remember all settings even if those settings are
// invalid or disabled.
int32_t fMinSigDigits;
int32_t fMaxSigDigits;
UBool fUseScientific;
UBool fUseSigDigits;
// In addition to these listed above, changes to min/max int digits and
// min/max frac digits from fSuper also trigger an update.

// Updating any of the following fields triggers an update to
// fEffGrouping field Again we do it this way because original
// grouping settings have to be retained if grouping is turned off.
DigitGrouping fGrouping;
// In addition to these listed above, changes to isGroupingUsed in
// fSuper also triggers an update to fEffGrouping.

// Updating any of the following fields triggers updates on the following:
// fMonetary, fRules, fAffixParser, fCurrencyAffixInfo,
// fFormatter, fAffixes.fPositivePrefiix, fAffixes.fPositiveSuffix,
// fAffixes.fNegativePrefiix, fAffixes.fNegativeSuffix
// We do this two phase update because localizing the affix patterns
// and formatters can be expensive. Better to do it once with the setters
// than each time within format.
AffixPattern fPositivePrefixPattern;
AffixPattern fNegativePrefixPattern;
AffixPattern fPositiveSuffixPattern;
AffixPattern fNegativeSuffixPattern;
DecimalFormatSymbols *fSymbols;
UCurrencyUsage fCurrencyUsage;
// In addition to these listed above, changes to getCurrency() in
// fSuper also triggers an update.

// Optional may be NULL
PluralRules *fRules;

// These fields are totally hidden from user and are used to derive the affixes
// in fAffixes below from the four affix patterns above.
UBool fMonetary;
AffixPatternParser fAffixParser;
CurrencyAffixInfo fCurrencyAffixInfo;

// The actual precision used when formatting
ScientificPrecision fEffPrecision;

// The actual grouping used when formatting
DigitGrouping fEffGrouping;
SciFormatterOptions fOptions;   // Encapsulates fixed precision options
DigitFormatter fFormatter;
DigitAffixesAndPadding fAffixes;

UnicodeString &formatInt32(
        int32_t number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

UnicodeString &formatInt64(
        int64_t number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

UnicodeString &formatDouble(
        double number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

// Scales for precent or permille symbols
UnicodeString &formatDigitList(
        DigitList &number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

// Does not scale for precent or permille symbols
UnicodeString &formatAdjustedDigitList(
        DigitList &number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

UnicodeString &formatVisibleDigitsWithExponent(
        const VisibleDigitsWithExponent &number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

VisibleDigitsWithExponent &
initVisibleDigitsFromAdjusted(
        DigitList &number,
        VisibleDigitsWithExponent &digits,
        UErrorCode &status) const;

template<class T>
UBool maybeFormatWithDigitList(
        T number,
        UnicodeString &appendTo,
        FieldPositionHandler &handler,
        UErrorCode &status) const;

template<class T>
UBool maybeInitVisibleDigitsFromDigitList(
        T number,
        VisibleDigitsWithExponent &digits,
        UErrorCode &status) const;

DigitList &adjustDigitList(DigitList &number, UErrorCode &status) const;

void applyPattern(
        const UnicodeString &pattern,
        UBool localized, UParseError &perror, UErrorCode &status);

ValueFormatter &prepareValueFormatter(ValueFormatter &vf) const;
void setMultiplierScale(int32_t s);
int32_t getPatternScale() const;
void setScale(int32_t s) { fScale = s; }
int32_t getScale() const { return fScale; }

// Updates everything
void updateAll(UErrorCode &status);
void updateAll(
        int32_t formattingFlags,
        UBool updatePrecisionBasedOnCurrency,
        UErrorCode &status);

// Updates from formatting pattern changes
void updateForApplyPattern(UErrorCode &status);
void updateForApplyPatternFavorCurrencyPrecision(UErrorCode &status);

// Updates from changes to third group of attributes
void updateFormatting(int32_t changedFormattingFields, UErrorCode &status);
void updateFormatting(
        int32_t changedFormattingFields,
        UBool updatePrecisionBasedOnCurrency,
        UErrorCode &status);

// Helper functions for updatePrecision
void updatePrecisionForScientific();
void updatePrecisionForFixed();
void extractMinMaxDigits(DigitInterval &min, DigitInterval &max) const;
void extractSigDigits(SignificantDigitInterval &sig) const;

// Helper functions for updateFormatting
void updateFormattingUsesCurrency(int32_t &changedFormattingFields);
void updateFormattingPluralRules(
        int32_t &changedFormattingFields, UErrorCode &status);
void updateFormattingAffixParser(int32_t &changedFormattingFields);
void updateFormattingCurrencyAffixInfo(
        int32_t &changedFormattingFields,
        UBool updatePrecisionBasedOnCurrency,
        UErrorCode &status);
void updateFormattingFixedPointFormatter(
        int32_t &changedFormattingFields);
void updateFormattingLocalizedPositivePrefix(
        int32_t &changedFormattingFields, UErrorCode &status);
void updateFormattingLocalizedPositiveSuffix(
        int32_t &changedFormattingFields, UErrorCode &status);
void updateFormattingLocalizedNegativePrefix(
        int32_t &changedFormattingFields, UErrorCode &status);
void updateFormattingLocalizedNegativeSuffix(
        int32_t &changedFormattingFields, UErrorCode &status);

int32_t computeExponentPatternLength() const;
int32_t countFractionDigitAndDecimalPatternLength(int32_t fracDigitCount) const;
UnicodeString &toNumberPattern(
        UBool hasPadding, int32_t minimumLength, UnicodeString& result) const;

int32_t getOldFormatWidth() const;
const UnicodeString &getConstSymbol(
        DecimalFormatSymbols::ENumberFormatSymbol symbol) const;
UBool isParseFastpath() const;

friend class DecimalFormat;

};


U_NAMESPACE_END

#endif // DECIMFMTIMPL_H
//eof
