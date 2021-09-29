
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 69 with ICU 70

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 69](#removed)
- [Deprecated or Obsoleted in ICU 70](#deprecated)
- [Changed in  ICU 70](#changed)
- [Promoted to stable in ICU 70](#promoted)
- [Added in ICU 70](#added)
- [Other existing drafts in ICU 70](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 69
  
| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| tmutfmt.h | bool icu::TimeUnitFormat::operator!=(const Format&amp;) const |  DeprecatedICU 53 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 70
  
| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration() |  StableICU 2.4 | DeprecatedICU 70
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(const char*) |  StableICU 2.4 | DeprecatedICU 70
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(int32_t) |  StableICU 2.4 | DeprecatedICU 70

## Changed

Changed in  ICU 70 (old, new)


  
| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| dtitvfmt.h | UDisplayContext icu::DateIntervalFormat::getContext(UDisplayContextType, UErrorCode&amp;) const |  Draft→StableICU 68
| dtitvfmt.h | void icu::DateIntervalFormat::setContext(UDisplayContext, UErrorCode&amp;) |  Draft→StableICU 68
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setMaxDistance(const Locale&amp;, const Locale&amp;) |  Draft→StableICU 68
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setNoDefaultLocale() |  Draft→StableICU 68
| localematcher.h | bool icu::LocaleMatcher::isMatch(const Locale&amp;, const Locale&amp;, UErrorCode&amp;) const |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCandela() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoon() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoonImperial() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDot() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDram() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDrop() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthRadius() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGrain() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getJigger() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLumen() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPinch() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuartImperial() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCandela(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoon(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoonImperial(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDot(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDram(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDrop(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthRadius(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGrain(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createJigger(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLumen(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPinch(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuartImperial(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | std::pair&lt; LocalArray&lt; MeasureUnit &gt;, int32_t &gt; icu::MeasureUnit::splitToSingleUnits(UErrorCode&amp;) const |  Draft→StableICU 68
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece) const&amp; |  Draft→StableICU 68
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece)&amp;&amp; |  Draft→StableICU 68
| numberformatter.h | MeasureUnit icu::number::FormattedNumber::getOutputUnit(UErrorCode&amp;) const |  Draft→StableICU 68
| numberrangeformatter.h | std::pair&lt; StringClass, StringClass &gt; icu::number::FormattedNumberRange::getDecimalNumbers(UErrorCode&amp;) const |  Draft→StableICU 68
| plurrule.h | UnicodeString icu::PluralRules::select(const number::FormattedNumberRange&amp;, UErrorCode&amp;) const |  Draft→StableICU 68
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration() |  StableICU 2.4 | DeprecatedICU 70
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(const char*) |  StableICU 2.4 | DeprecatedICU 70
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(int32_t) |  StableICU 2.4 | DeprecatedICU 70
| ucurr.h | <tt>enum</tt> UCurrNameStyle::UCURR_FORMAL_SYMBOL_NAME |  Draft→StableICU 68
| ucurr.h | <tt>enum</tt> UCurrNameStyle::UCURR_VARIANT_SYMBOL_NAME |  Draft→StableICU 68
| udateintervalformat.h | UDisplayContext udtitvfmt_getContext(const UDateIntervalFormat*, UDisplayContextType, UErrorCode*) |  Draft→StableICU 68
| udateintervalformat.h | void udtitvfmt_setContext(UDateIntervalFormat*, UDisplayContext, UErrorCode*) |  Draft→StableICU 68
| unum.h | <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_AUTO |  Draft→StableICU 68
| unum.h | <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_MIN2 |  Draft→StableICU 68
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_FORMAL |  Draft→StableICU 68
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_VARIANT |  Draft→StableICU 68
| unumberformatter.h | int32_t unumf_resultToDecimalNumber(const UFormattedNumber*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UFormattedNumberRange* unumrf_openResult(UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UNumberRangeFormatter* unumrf_openForSkeletonWithCollapseAndIdentityFallback(const UChar*, int32_t, UNumberRangeCollapse, UNumberRangeIdentityFallback, const char*, UParseError*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UNumberRangeIdentityResult unumrf_resultGetIdentityResult(const UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | const UFormattedValue* unumrf_resultAsValue(const UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | int32_t unumrf_resultGetFirstDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | int32_t unumrf_resultGetSecondDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_close(UNumberRangeFormatter*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_closeResult(UFormattedNumberRange*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_formatDecimalRange(const UNumberRangeFormatter*, const char*, int32_t, const char*, int32_t, UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_formatDoubleRange(const UNumberRangeFormatter*, double, double, UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| upluralrules.h | int32_t uplrules_selectForRange(const UPluralRules*, const struct UFormattedNumberRange*, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 68

## Promoted

Promoted to stable in ICU 70
  
| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| dtitvfmt.h | UDisplayContext icu::DateIntervalFormat::getContext(UDisplayContextType, UErrorCode&amp;) const |  Draft→StableICU 68
| dtitvfmt.h | void icu::DateIntervalFormat::setContext(UDisplayContext, UErrorCode&amp;) |  Draft→StableICU 68
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setMaxDistance(const Locale&amp;, const Locale&amp;) |  Draft→StableICU 68
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setNoDefaultLocale() |  Draft→StableICU 68
| localematcher.h | bool icu::LocaleMatcher::isMatch(const Locale&amp;, const Locale&amp;, UErrorCode&amp;) const |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCandela() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoon() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoonImperial() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDot() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDram() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDrop() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthRadius() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGrain() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getJigger() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLumen() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPinch() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuartImperial() |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCandela(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoon(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoonImperial(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDot(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDram(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDrop(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthRadius(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGrain(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createJigger(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLumen(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPinch(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuartImperial(UErrorCode&amp;) |  Draft→StableICU 68
| measunit.h | std::pair&lt; LocalArray&lt; MeasureUnit &gt;, int32_t &gt; icu::MeasureUnit::splitToSingleUnits(UErrorCode&amp;) const |  Draft→StableICU 68
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece) const&amp; |  Draft→StableICU 68
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece)&amp;&amp; |  Draft→StableICU 68
| numberformatter.h | MeasureUnit icu::number::FormattedNumber::getOutputUnit(UErrorCode&amp;) const |  Draft→StableICU 68
| numberrangeformatter.h | std::pair&lt; StringClass, StringClass &gt; icu::number::FormattedNumberRange::getDecimalNumbers(UErrorCode&amp;) const |  Draft→StableICU 68
| plurrule.h | UnicodeString icu::PluralRules::select(const number::FormattedNumberRange&amp;, UErrorCode&amp;) const |  Draft→StableICU 68
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumerationForRawOffset(int32_t, UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumerationForRegion(const char*, UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARABIC_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CYPRO_MINOAN |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ETHIOPIC_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KANA_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LATIN_EXTENDED_F |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LATIN_EXTENDED_G |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_OLD_UYGHUR |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGSA |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TOTO |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED_A |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_VITHKUQI |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ZNAMENNY_MUSICAL_NOTATION |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_THIN_YEH |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_VERTICAL_TAIL |  (missing) | StableICU 70| *(Born Stable)* |
| ucurr.h | <tt>enum</tt> UCurrNameStyle::UCURR_FORMAL_SYMBOL_NAME |  Draft→StableICU 68
| ucurr.h | <tt>enum</tt> UCurrNameStyle::UCURR_VARIANT_SYMBOL_NAME |  Draft→StableICU 68
| udateintervalformat.h | UDisplayContext udtitvfmt_getContext(const UDateIntervalFormat*, UDisplayContextType, UErrorCode*) |  Draft→StableICU 68
| udateintervalformat.h | void udtitvfmt_setContext(UDateIntervalFormat*, UDisplayContext, UErrorCode*) |  Draft→StableICU 68
| unum.h | <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_AUTO |  Draft→StableICU 68
| unum.h | <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_MIN2 |  Draft→StableICU 68
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_FORMAL |  Draft→StableICU 68
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_VARIANT |  Draft→StableICU 68
| unumberformatter.h | int32_t unumf_resultToDecimalNumber(const UFormattedNumber*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UFormattedNumberRange* unumrf_openResult(UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UNumberRangeFormatter* unumrf_openForSkeletonWithCollapseAndIdentityFallback(const UChar*, int32_t, UNumberRangeCollapse, UNumberRangeIdentityFallback, const char*, UParseError*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | UNumberRangeIdentityResult unumrf_resultGetIdentityResult(const UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | const UFormattedValue* unumrf_resultAsValue(const UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | int32_t unumrf_resultGetFirstDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | int32_t unumrf_resultGetSecondDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_close(UNumberRangeFormatter*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_closeResult(UFormattedNumberRange*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_formatDecimalRange(const UNumberRangeFormatter*, const char*, int32_t, const char*, int32_t, UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| unumberrangeformatter.h | void unumrf_formatDoubleRange(const UNumberRangeFormatter*, double, double, UFormattedNumberRange*, UErrorCode*) |  Draft→StableICU 68
| upluralrules.h | int32_t uplrules_selectForRange(const UPluralRules*, const struct UFormattedNumberRange*, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 68
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_CYPRO_MINOAN |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_OLD_UYGHUR |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TANGSA |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TOTO |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_VITHKUQI |  (missing) | StableICU 70| *(Born Stable)* |
| utypes.h | <tt>enum</tt> UErrorCode::U_INPUT_TOO_LONG_ERROR |  (missing) | StableICU 68

## Added

Added in ICU 70
  
| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| dcfmtsym.h | <tt>enum</tt>  							icu::DecimalFormatSymbols::ENumberFormatSymbol::kApproximatelySignSymbol |  (missing) | Internal
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getItem() |  (missing) | DraftICU 70
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHourPer100Kilometer() |  (missing) | DraftICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createItem(UErrorCode&amp;) |  (missing) | DraftICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilowattHourPer100Kilometer(UErrorCode&amp;) |  (missing) | DraftICU 70
| numberformatter.h | const DecimalFormatSymbols* icu::number::LocalizedNumberFormatter::getDecimalFormatSymbols() const |  (missing) | Internal
| numberrangeformatter.h | icu::number::FormattedNumberRange::FormattedNumberRange() |  (missing) | DraftICU 70
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumeration(UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumerationForRawOffset(int32_t, UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| timezone.h | <tt>static</tt> StringEnumeration* icu::TimeZone::createEnumerationForRegion(const char*, UErrorCode&amp;) |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | bool u_stringHasBinaryProperty(const UChar*, int32_t, UProperty) |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARABIC_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CYPRO_MINOAN |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ETHIOPIC_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KANA_EXTENDED_B |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LATIN_EXTENDED_F |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LATIN_EXTENDED_G |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_OLD_UYGHUR |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGSA |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TOTO |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED_A |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_VITHKUQI |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ZNAMENNY_MUSICAL_NOTATION |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_THIN_YEH |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_VERTICAL_TAIL |  (missing) | StableICU 70| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_BASIC_EMOJI |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_EMOJI_KEYCAP_SEQUENCE |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_FLAG_SEQUENCE |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_MODIFIER_SEQUENCE |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_TAG_SEQUENCE |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_ZWJ_SEQUENCE |  (missing) | DraftICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI |  (missing) | DraftICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_NARROW_QUARTERS |  (missing) | DraftICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_STANDALONE_NARROW_QUARTERS |  (missing) | DraftICU 70
| uniset.h | bool icu::UnicodeSet::hasStrings() const |  (missing) | DraftICU 70
| unum.h | <tt>enum</tt> UNumberFormatSymbol::UNUM_APPROXIMATELY_SIGN_SYMBOL |  (missing) | Internal
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_CYPRO_MINOAN |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_OLD_UYGHUR |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TANGSA |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TOTO |  (missing) | StableICU 70| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_VITHKUQI |  (missing) | StableICU 70| *(Born Stable)* |
| uset.h | bool uset_hasStrings(const USet*) |  (missing) | DraftICU 70
| uset.h | int32_t uset_getRangeCount(const USet*) |  (missing) | DraftICU 70
| usetiter.h | UnicodeSetIterator&amp; icu::UnicodeSetIterator::skipToStrings() |  (missing) | DraftICU 70
| utypes.h | <tt>enum</tt> UErrorCode::U_INPUT_TOO_LONG_ERROR |  (missing) | StableICU 68

## Other

Other existing drafts in ICU 70

| File | API | ICU 69 | ICU 70 |
|---|---|---|---|
| basictz.h |  void icu::BasicTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const | DraftICU 69 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  MeasureUnit icu::MeasureUnit::withPrefix(UMeasurePrefix, UErrorCode&amp;) const | DraftICU 69 | 
| measunit.h |  UMeasurePrefix icu::MeasureUnit::getPrefix(UErrorCode&amp;) const | DraftICU 69 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramOfglucosePerDeciliter() | DraftICU 69 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramOfglucosePerDeciliter(UErrorCode&amp;) | DraftICU 69 | 
| numberformatter.h |  Precision icu::number::FractionPrecision::withSignificantDigits(int32_t, int32_t, UNumberRoundingPriority) const | DraftICU 69 | 
| numberformatter.h |  Precision icu::number::Precision::trailingZeroDisplay(UNumberTrailingZeroDisplay) const | DraftICU 69 | 
| rbtz.h |  void icu::RuleBasedTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const | DraftICU 69 | 
| simpletz.h |  void icu::SimpleTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const | DraftICU 69 | 
| ubrk.h |  UBreakIterator* ubrk_clone(const UBreakIterator*, UErrorCode*) | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_FORMER | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_LATTER | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_FORMER | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_LATTER | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_FORMER | DraftICU 69 | 
| ucal.h |  <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_LATTER | DraftICU 69 | 
| ucal.h |  void ucal_getTimeZoneOffsetFromLocal(const UCalendar*, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t*, int32_t*, UErrorCode*) | DraftICU 69 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER_RANGE_SPAN | DraftICU 69 | 
| uniset.h |  UnicodeSet&amp; icu::UnicodeSet::retain(const UnicodeString&amp;) | DraftICU 69 | 
| unum.h |  <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_CEILING | DraftICU 69 | 
| unum.h |  <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_FLOOR | DraftICU 69 | 
| unum.h |  <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_ODD | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_RELAXED | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_STRICT | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_NEGATIVE | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEGATIVE | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_AUTO | DraftICU 69 | 
| unumberformatter.h |  <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_HIDE_IF_WHOLE | DraftICU 69 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| uset.h |  void uset_complementAllCodePoints(USet*, const UChar*, int32_t) | DraftICU 69 | 
| uset.h |  void uset_complementRange(USet*, UChar32, UChar32) | DraftICU 69 | 
| uset.h |  void uset_complementString(USet*, const UChar*, int32_t) | DraftICU 69 | 
| uset.h |  void uset_removeAllCodePoints(USet*, const UChar*, int32_t) | DraftICU 69 | 
| uset.h |  void uset_retainAllCodePoints(USet*, const UChar*, int32_t) | DraftICU 69 | 
| uset.h |  void uset_retainString(USet*, const UChar*, int32_t) | DraftICU 69 | 
| vtzone.h |  void icu::VTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const | DraftICU 69 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.

- **`UClassID icu::BreakIterator::getDynamicClassID() const`**
  - `UClassID icu::BreakIterator::getDynamicClassID() const override=0`
  - `UClassID icu::BreakIterator::getDynamicClassID() const=0`
- **`UClassID icu::Calendar::getDynamicClassID() const`**
  - `UClassID icu::Calendar::getDynamicClassID() const override=0`
  - `UClassID icu::Calendar::getDynamicClassID() const=0`
- **`UClassID icu::Collator::getDynamicClassID() const`**
  - `UClassID icu::Collator::getDynamicClassID() const override=0`
  - `UClassID icu::Collator::getDynamicClassID() const=0`
- **`UClassID icu::ForwardCharacterIterator::getDynamicClassID() const`**
  - `UClassID icu::ForwardCharacterIterator::getDynamicClassID() const override=0`
  - `UClassID icu::ForwardCharacterIterator::getDynamicClassID() const=0`
- **`UClassID icu::NumberFormat::getDynamicClassID() const`**
  - `UClassID icu::NumberFormat::getDynamicClassID() const override=0`
  - `UClassID icu::NumberFormat::getDynamicClassID() const=0`
- **`UClassID icu::TimeZone::getDynamicClassID() const`**
  - `UClassID icu::TimeZone::getDynamicClassID() const override=0`
  - `UClassID icu::TimeZone::getDynamicClassID() const=0`
- **`UClassID icu::Transliterator::getDynamicClassID() const`**
  - `UClassID icu::Transliterator::getDynamicClassID() const override=0`
  - `UClassID icu::Transliterator::getDynamicClassID() const=0`
- **`UClassID icu::UnicodeFunctor::getDynamicClassID() const`**
  - `UClassID icu::UnicodeFunctor::getDynamicClassID() const override=0`
  - `UClassID icu::UnicodeFunctor::getDynamicClassID() const=0`
- **`UMatchDegree icu::UnicodeFilter::matches(const Replaceable&, int32_t&, int32_t, bool)`**
  - `UMatchDegree icu::UnicodeFilter::matches(const Replaceable&, int32_t&, int32_t, UBool)`
  - `UMatchDegree icu::UnicodeFilter::matches(const Replaceable&, int32_t&, int32_t, UBool) override`
- **`UMatchDegree icu::UnicodeSet::matches(const Replaceable&, int32_t&, int32_t, bool)`**
  - `UMatchDegree icu::UnicodeSet::matches(const Replaceable&, int32_t&, int32_t, UBool)`
  - `UMatchDegree icu::UnicodeSet::matches(const Replaceable&, int32_t&, int32_t, UBool) override`
- **`UnicodeString& icu::UnicodeSet::toPattern(UnicodeString&, bool escapeUnprintable=) const`**
  - `UnicodeString& icu::UnicodeSet::toPattern(UnicodeString&, UBool escapeUnprintable=) const`
  - `UnicodeString& icu::UnicodeSet::toPattern(UnicodeString&, UBool escapeUnprintable=) const override`
- **`bool icu::AnnualTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::AnnualTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::AnnualTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::AnnualTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::AnnualTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::AnnualTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::AnnualTimeZoneRule::getNextStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::AnnualTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::AnnualTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::AnnualTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::AnnualTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::AnnualTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::AnnualTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`**
  - `UBool icu::AnnualTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`
  - `UBool icu::AnnualTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const override`
- **`bool icu::AnnualTimeZoneRule::operator!=(const TimeZoneRule&) const`**
  - `UBool icu::AnnualTimeZoneRule::operator!=(const TimeZoneRule&) const`
  - `bool icu::AnnualTimeZoneRule::operator!=(const TimeZoneRule&) const override`
- **`bool icu::AnnualTimeZoneRule::operator==(const TimeZoneRule&) const`**
  - `UBool icu::AnnualTimeZoneRule::operator==(const TimeZoneRule&) const`
  - `bool icu::AnnualTimeZoneRule::operator==(const TimeZoneRule&) const override`
- **`bool icu::BreakIterator::operator==(const BreakIterator&) const`**
  - `UBool icu::BreakIterator::operator==(const BreakIterator&) const=0`
  - `bool icu::BreakIterator::operator==(const BreakIterator&) const=0`
- **`bool icu::ChoiceFormat::operator==(const Format&) const`**
  - `UBool icu::ChoiceFormat::operator==(const Format&) const`
  - `bool icu::ChoiceFormat::operator==(const Format&) const override`
- **`bool icu::DateFormat::operator==(const Format&) const`**
  - `UBool icu::DateFormat::operator==(const Format&) const`
  - `bool icu::DateFormat::operator==(const Format&) const override`
- **`bool icu::DateIntervalFormat::operator==(const Format&) const`**
  - `UBool icu::DateIntervalFormat::operator==(const Format&) const`
  - `bool icu::DateIntervalFormat::operator==(const Format&) const override`
- **`bool icu::DecimalFormat::operator==(const Format&) const`**
  - `UBool icu::DecimalFormat::operator==(const Format&) const U_OVERRIDE`
  - `bool icu::DecimalFormat::operator==(const Format&) const U_OVERRIDE`
- **`bool icu::Format::operator==(const Format&) const`**
  - `UBool icu::Format::operator==(const Format&) const=0`
  - `bool icu::Format::operator==(const Format&) const=0`
- **`bool icu::ForwardCharacterIterator::operator==(const ForwardCharacterIterator&) const`**
  - `UBool icu::ForwardCharacterIterator::operator==(const ForwardCharacterIterator&) const=0`
  - `bool icu::ForwardCharacterIterator::operator==(const ForwardCharacterIterator&) const=0`
- **`bool icu::GregorianCalendar::haveDefaultCentury() const`**
  - `UBool icu::GregorianCalendar::haveDefaultCentury() const`
  - `UBool icu::GregorianCalendar::haveDefaultCentury() const override`
- **`bool icu::GregorianCalendar::inDaylightTime(UErrorCode&) const`**
  - `UBool icu::GregorianCalendar::inDaylightTime(UErrorCode&) const`
  - `UBool icu::GregorianCalendar::inDaylightTime(UErrorCode&) const override`
- **`bool icu::GregorianCalendar::isEquivalentTo(const Calendar&) const`**
  - `UBool icu::GregorianCalendar::isEquivalentTo(const Calendar&) const`
  - `UBool icu::GregorianCalendar::isEquivalentTo(const Calendar&) const override`
- **`bool icu::InitialTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::InitialTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::InitialTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::InitialTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::InitialTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::InitialTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::InitialTimeZoneRule::getNextStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::InitialTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::InitialTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::InitialTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::InitialTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::InitialTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::InitialTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`**
  - `UBool icu::InitialTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`
  - `UBool icu::InitialTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const override`
- **`bool icu::InitialTimeZoneRule::operator!=(const TimeZoneRule&) const`**
  - `UBool icu::InitialTimeZoneRule::operator!=(const TimeZoneRule&) const`
  - `bool icu::InitialTimeZoneRule::operator!=(const TimeZoneRule&) const override`
- **`bool icu::InitialTimeZoneRule::operator==(const TimeZoneRule&) const`**
  - `UBool icu::InitialTimeZoneRule::operator==(const TimeZoneRule&) const`
  - `bool icu::InitialTimeZoneRule::operator==(const TimeZoneRule&) const override`
- **`bool icu::MeasureFormat::operator==(const Format&) const`**
  - `UBool icu::MeasureFormat::operator==(const Format&) const`
  - `bool icu::MeasureFormat::operator==(const Format&) const override`
- **`bool icu::MessageFormat::operator==(const Format&) const`**
  - `UBool icu::MessageFormat::operator==(const Format&) const`
  - `bool icu::MessageFormat::operator==(const Format&) const override`
- **`bool icu::NumberFormat::operator==(const Format&) const`**
  - `UBool icu::NumberFormat::operator==(const Format&) const`
  - `bool icu::NumberFormat::operator==(const Format&) const override`
- **`bool icu::PluralFormat::operator==(const Format&) const`**
  - `UBool icu::PluralFormat::operator==(const Format&) const`
  - `bool icu::PluralFormat::operator==(const Format&) const override`
- **`bool icu::RuleBasedBreakIterator::isBoundary(int32_t)`**
  - `UBool icu::RuleBasedBreakIterator::isBoundary(int32_t)`
  - `UBool icu::RuleBasedBreakIterator::isBoundary(int32_t) override`
- **`bool icu::RuleBasedBreakIterator::operator==(const BreakIterator&) const`**
  - `UBool icu::RuleBasedBreakIterator::operator==(const BreakIterator&) const`
  - `bool icu::RuleBasedBreakIterator::operator==(const BreakIterator&) const override`
- **`bool icu::RuleBasedCollator::operator==(const Collator&) const`**
  - `UBool icu::RuleBasedCollator::operator==(const Collator&) const`
  - `bool icu::RuleBasedCollator::operator==(const Collator&) const override`
- **`bool icu::RuleBasedNumberFormat::isLenient() const`**
  - `UBool icu::RuleBasedNumberFormat::isLenient() const`
  - `UBool icu::RuleBasedNumberFormat::isLenient() const override`
- **`bool icu::RuleBasedNumberFormat::operator==(const Format&) const`**
  - `UBool icu::RuleBasedNumberFormat::operator==(const Format&) const`
  - `bool icu::RuleBasedNumberFormat::operator==(const Format&) const override`
- **`bool icu::RuleBasedTimeZone::getNextTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::RuleBasedTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::RuleBasedTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::RuleBasedTimeZone::getPreviousTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::RuleBasedTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::RuleBasedTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::RuleBasedTimeZone::hasSameRules(const TimeZone&) const`**
  - `UBool icu::RuleBasedTimeZone::hasSameRules(const TimeZone&) const`
  - `UBool icu::RuleBasedTimeZone::hasSameRules(const TimeZone&) const override`
- **`bool icu::RuleBasedTimeZone::inDaylightTime(UDate, UErrorCode&) const`**
  - `UBool icu::RuleBasedTimeZone::inDaylightTime(UDate, UErrorCode&) const`
  - `UBool icu::RuleBasedTimeZone::inDaylightTime(UDate, UErrorCode&) const override`
- **`bool icu::RuleBasedTimeZone::operator==(const TimeZone&) const`**
  - `UBool icu::RuleBasedTimeZone::operator==(const TimeZone&) const`
  - `bool icu::RuleBasedTimeZone::operator==(const TimeZone&) const override`
- **`bool icu::RuleBasedTimeZone::useDaylightTime() const`**
  - `UBool icu::RuleBasedTimeZone::useDaylightTime() const`
  - `UBool icu::RuleBasedTimeZone::useDaylightTime() const override`
- **`bool icu::SelectFormat::operator==(const Format&) const`**
  - `UBool icu::SelectFormat::operator==(const Format&) const`
  - `bool icu::SelectFormat::operator==(const Format&) const override`
- **`bool icu::SimpleDateFormat::operator==(const Format&) const`**
  - `UBool icu::SimpleDateFormat::operator==(const Format&) const`
  - `bool icu::SimpleDateFormat::operator==(const Format&) const override`
- **`bool icu::SimpleNumberFormatFactory::visible() const`**
  - `UBool icu::SimpleNumberFormatFactory::visible() const`
  - `UBool icu::SimpleNumberFormatFactory::visible() const override`
- **`bool icu::SimpleTimeZone::getNextTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::SimpleTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::SimpleTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::SimpleTimeZone::getPreviousTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::SimpleTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::SimpleTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::SimpleTimeZone::hasSameRules(const TimeZone&) const`**
  - `UBool icu::SimpleTimeZone::hasSameRules(const TimeZone&) const`
  - `UBool icu::SimpleTimeZone::hasSameRules(const TimeZone&) const override`
- **`bool icu::SimpleTimeZone::inDaylightTime(UDate, UErrorCode&) const`**
  - `UBool icu::SimpleTimeZone::inDaylightTime(UDate, UErrorCode&) const`
  - `UBool icu::SimpleTimeZone::inDaylightTime(UDate, UErrorCode&) const override`
- **`bool icu::SimpleTimeZone::operator==(const TimeZone&) const`**
  - `UBool icu::SimpleTimeZone::operator==(const TimeZone&) const`
  - `bool icu::SimpleTimeZone::operator==(const TimeZone&) const override`
- **`bool icu::SimpleTimeZone::useDaylightTime() const`**
  - `UBool icu::SimpleTimeZone::useDaylightTime() const`
  - `UBool icu::SimpleTimeZone::useDaylightTime() const override`
- **`bool icu::StringCharacterIterator::operator==(const ForwardCharacterIterator&) const`**
  - `UBool icu::StringCharacterIterator::operator==(const ForwardCharacterIterator&) const`
  - `bool icu::StringCharacterIterator::operator==(const ForwardCharacterIterator&) const override`
- **`bool icu::StringSearch::operator==(const SearchIterator&) const`**
  - `UBool icu::StringSearch::operator==(const SearchIterator&) const`
  - `bool icu::StringSearch::operator==(const SearchIterator&) const override`
- **`bool icu::TimeArrayTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::TimeArrayTimeZoneRule::getFinalStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::TimeArrayTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const`
  - `UBool icu::TimeArrayTimeZoneRule::getFirstStart(int32_t, int32_t, UDate&) const override`
- **`bool icu::TimeArrayTimeZoneRule::getNextStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::TimeArrayTimeZoneRule::getNextStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::TimeArrayTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, bool, UDate&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const`
  - `UBool icu::TimeArrayTimeZoneRule::getPreviousStart(UDate, int32_t, int32_t, UBool, UDate&) const override`
- **`bool icu::TimeArrayTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const`
  - `UBool icu::TimeArrayTimeZoneRule::isEquivalentTo(const TimeZoneRule&) const override`
- **`bool icu::TimeArrayTimeZoneRule::operator!=(const TimeZoneRule&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::operator!=(const TimeZoneRule&) const`
  - `bool icu::TimeArrayTimeZoneRule::operator!=(const TimeZoneRule&) const override`
- **`bool icu::TimeArrayTimeZoneRule::operator==(const TimeZoneRule&) const`**
  - `UBool icu::TimeArrayTimeZoneRule::operator==(const TimeZoneRule&) const`
  - `bool icu::TimeArrayTimeZoneRule::operator==(const TimeZoneRule&) const override`
- **`bool icu::TimeZoneFormat::operator==(const Format&) const`**
  - `UBool icu::TimeZoneFormat::operator==(const Format&) const`
  - `bool icu::TimeZoneFormat::operator==(const Format&) const override`
- **`bool icu::TimeZoneNames::operator==(const TimeZoneNames&) const`**
  - `UBool icu::TimeZoneNames::operator==(const TimeZoneNames&) const=0`
  - `bool icu::TimeZoneNames::operator==(const TimeZoneNames&) const=0`
- **`bool icu::UCharCharacterIterator::hasNext()`**
  - `UBool icu::UCharCharacterIterator::hasNext()`
  - `UBool icu::UCharCharacterIterator::hasNext() override`
- **`bool icu::UCharCharacterIterator::hasPrevious()`**
  - `UBool icu::UCharCharacterIterator::hasPrevious()`
  - `UBool icu::UCharCharacterIterator::hasPrevious() override`
- **`bool icu::UCharCharacterIterator::operator==(const ForwardCharacterIterator&) const`**
  - `UBool icu::UCharCharacterIterator::operator==(const ForwardCharacterIterator&) const`
  - `bool icu::UCharCharacterIterator::operator==(const ForwardCharacterIterator&) const override`
- **`bool icu::UnicodeSet::contains(UChar32) const`**
  - `UBool icu::UnicodeSet::contains(UChar32) const`
  - `UBool icu::UnicodeSet::contains(UChar32) const override`
- **`bool icu::UnicodeString::hasMetaData() const`**
  - `UBool icu::UnicodeString::hasMetaData() const`
  - `UBool icu::UnicodeString::hasMetaData() const override`
- **`bool icu::UnicodeStringAppendable::appendCodePoint(UChar32)`**
  - `UBool icu::UnicodeStringAppendable::appendCodePoint(UChar32)`
  - `UBool icu::UnicodeStringAppendable::appendCodePoint(UChar32) override`
- **`bool icu::UnicodeStringAppendable::appendCodeUnit(char16_t)`**
  - `UBool icu::UnicodeStringAppendable::appendCodeUnit(char16_t)`
  - `UBool icu::UnicodeStringAppendable::appendCodeUnit(char16_t) override`
- **`bool icu::UnicodeStringAppendable::appendString(const char16_t*, int32_t)`**
  - `UBool icu::UnicodeStringAppendable::appendString(const char16_t*, int32_t)`
  - `UBool icu::UnicodeStringAppendable::appendString(const char16_t*, int32_t) override`
- **`bool icu::UnicodeStringAppendable::reserveAppendCapacity(int32_t)`**
  - `UBool icu::UnicodeStringAppendable::reserveAppendCapacity(int32_t)`
  - `UBool icu::UnicodeStringAppendable::reserveAppendCapacity(int32_t) override`
- **`bool icu::VTimeZone::getNextTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::VTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::VTimeZone::getNextTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::VTimeZone::getPreviousTransition(UDate, bool, TimeZoneTransition&) const`**
  - `UBool icu::VTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const`
  - `UBool icu::VTimeZone::getPreviousTransition(UDate, UBool, TimeZoneTransition&) const override`
- **`bool icu::VTimeZone::hasSameRules(const TimeZone&) const`**
  - `UBool icu::VTimeZone::hasSameRules(const TimeZone&) const`
  - `UBool icu::VTimeZone::hasSameRules(const TimeZone&) const override`
- **`bool icu::VTimeZone::inDaylightTime(UDate, UErrorCode&) const`**
  - `UBool icu::VTimeZone::inDaylightTime(UDate, UErrorCode&) const`
  - `UBool icu::VTimeZone::inDaylightTime(UDate, UErrorCode&) const override`
- **`bool icu::VTimeZone::operator==(const TimeZone&) const`**
  - `UBool icu::VTimeZone::operator==(const TimeZone&) const`
  - `bool icu::VTimeZone::operator==(const TimeZone&) const override`
- **`bool icu::VTimeZone::useDaylightTime() const`**
  - `UBool icu::VTimeZone::useDaylightTime() const`
  - `UBool icu::VTimeZone::useDaylightTime() const override`
- **`void icu::RuleBasedNumberFormat::setLenient(bool)`**
  - `void icu::RuleBasedNumberFormat::setLenient(UBool)`
  - `void icu::RuleBasedNumberFormat::setLenient(UBool) override`
- **`void icu::RuleBasedTimeZone::getOffset(UDate, bool, int32_t&, int32_t&, UErrorCode&) const`**
  - `void icu::RuleBasedTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const`
  - `void icu::RuleBasedTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const override`
- **`void icu::SimpleTimeZone::getOffset(UDate, bool, int32_t&, int32_t&, UErrorCode&) const`**
  - `void icu::SimpleTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const`
  - `void icu::SimpleTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const override`
- **`void icu::VTimeZone::getOffset(UDate, bool, int32_t&, int32_t&, UErrorCode&) const`**
  - `void icu::VTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const`
  - `void icu::VTimeZone::getOffset(UDate, UBool, int32_t&, int32_t&, UErrorCode&) const override`
- **`void* icu::AnnualTimeZoneRule::clone() const`**
  - `AnnualTimeZoneRule* icu::AnnualTimeZoneRule::clone() const`
  - `AnnualTimeZoneRule* icu::AnnualTimeZoneRule::clone() const override`
- **`void* icu::BasicTimeZone::clone() const`**
  - `BasicTimeZone* icu::BasicTimeZone::clone() const override=0`
  - `BasicTimeZone* icu::BasicTimeZone::clone() const=0`
- **`void* icu::ChoiceFormat::clone() const`**
  - `ChoiceFormat* icu::ChoiceFormat::clone() const`
  - `ChoiceFormat* icu::ChoiceFormat::clone() const override`
- **`void* icu::CurrencyAmount::clone() const`**
  - `CurrencyAmount* icu::CurrencyAmount::clone() const`
  - `CurrencyAmount* icu::CurrencyAmount::clone() const override`
- **`void* icu::CurrencyUnit::clone() const`**
  - `CurrencyUnit* icu::CurrencyUnit::clone() const`
  - `CurrencyUnit* icu::CurrencyUnit::clone() const override`
- **`void* icu::DateFormat::clone() const`**
  - `DateFormat* icu::DateFormat::clone() const override=0`
  - `DateFormat* icu::DateFormat::clone() const=0`
- **`void* icu::DateIntervalFormat::clone() const`**
  - `DateIntervalFormat* icu::DateIntervalFormat::clone() const`
  - `DateIntervalFormat* icu::DateIntervalFormat::clone() const override`
- **`void* icu::GregorianCalendar::clone() const`**
  - `GregorianCalendar* icu::GregorianCalendar::clone() const`
  - `GregorianCalendar* icu::GregorianCalendar::clone() const override`
- **`void* icu::InitialTimeZoneRule::clone() const`**
  - `InitialTimeZoneRule* icu::InitialTimeZoneRule::clone() const`
  - `InitialTimeZoneRule* icu::InitialTimeZoneRule::clone() const override`
- **`void* icu::MeasureFormat::clone() const`**
  - `MeasureFormat* icu::MeasureFormat::clone() const`
  - `MeasureFormat* icu::MeasureFormat::clone() const override`
- **`void* icu::MessageFormat::clone() const`**
  - `MessageFormat* icu::MessageFormat::clone() const`
  - `MessageFormat* icu::MessageFormat::clone() const override`
- **`void* icu::NumberFormat::clone() const`**
  - `NumberFormat* icu::NumberFormat::clone() const override=0`
  - `NumberFormat* icu::NumberFormat::clone() const=0`
- **`void* icu::PluralFormat::clone() const`**
  - `PluralFormat* icu::PluralFormat::clone() const`
  - `PluralFormat* icu::PluralFormat::clone() const override`
- **`void* icu::RuleBasedBreakIterator::clone() const`**
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::clone() const`
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::clone() const override`
- **`void* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&)`**
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&)`
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&) override`
- **`void* icu::RuleBasedCollator::clone() const`**
  - `RuleBasedCollator* icu::RuleBasedCollator::clone() const`
  - `RuleBasedCollator* icu::RuleBasedCollator::clone() const override`
- **`void* icu::RuleBasedNumberFormat::clone() const`**
  - `RuleBasedNumberFormat* icu::RuleBasedNumberFormat::clone() const`
  - `RuleBasedNumberFormat* icu::RuleBasedNumberFormat::clone() const override`
- **`void* icu::RuleBasedTimeZone::clone() const`**
  - `RuleBasedTimeZone* icu::RuleBasedTimeZone::clone() const`
  - `RuleBasedTimeZone* icu::RuleBasedTimeZone::clone() const override`
- **`void* icu::SelectFormat::clone() const`**
  - `SelectFormat* icu::SelectFormat::clone() const`
  - `SelectFormat* icu::SelectFormat::clone() const override`
- **`void* icu::SimpleDateFormat::clone() const`**
  - `SimpleDateFormat* icu::SimpleDateFormat::clone() const`
  - `SimpleDateFormat* icu::SimpleDateFormat::clone() const override`
- **`void* icu::SimpleTimeZone::clone() const`**
  - `SimpleTimeZone* icu::SimpleTimeZone::clone() const`
  - `SimpleTimeZone* icu::SimpleTimeZone::clone() const override`
- **`void* icu::StringCharacterIterator::clone() const`**
  - `StringCharacterIterator* icu::StringCharacterIterator::clone() const`
  - `StringCharacterIterator* icu::StringCharacterIterator::clone() const override`
- **`void* icu::StringSearch::safeClone() const`**
  - `StringSearch* icu::StringSearch::safeClone() const`
  - `StringSearch* icu::StringSearch::safeClone() const override`
- **`void* icu::TimeArrayTimeZoneRule::clone() const`**
  - `TimeArrayTimeZoneRule* icu::TimeArrayTimeZoneRule::clone() const`
  - `TimeArrayTimeZoneRule* icu::TimeArrayTimeZoneRule::clone() const override`
- **`void* icu::TimeUnit::clone() const`**
  - `TimeUnit* icu::TimeUnit::clone() const`
  - `TimeUnit* icu::TimeUnit::clone() const override`
- **`void* icu::TimeUnitAmount::clone() const`**
  - `TimeUnitAmount* icu::TimeUnitAmount::clone() const`
  - `TimeUnitAmount* icu::TimeUnitAmount::clone() const override`
- **`void* icu::TimeUnitFormat::clone() const`**
  - `TimeUnitFormat* icu::TimeUnitFormat::clone() const`
  - `TimeUnitFormat* icu::TimeUnitFormat::clone() const override`
- **`void* icu::TimeZoneFormat::clone() const`**
  - `TimeZoneFormat* icu::TimeZoneFormat::clone() const`
  - `TimeZoneFormat* icu::TimeZoneFormat::clone() const override`
- **`void* icu::UCharCharacterIterator::clone() const`**
  - `UCharCharacterIterator* icu::UCharCharacterIterator::clone() const`
  - `UCharCharacterIterator* icu::UCharCharacterIterator::clone() const override`
- **`void* icu::UnicodeFilter::clone() const`**
  - `UnicodeFilter* icu::UnicodeFilter::clone() const override=0`
  - `UnicodeFilter* icu::UnicodeFilter::clone() const=0`
- **`void* icu::UnicodeSet::clone() const`**
  - `UnicodeSet* icu::UnicodeSet::clone() const`
  - `UnicodeSet* icu::UnicodeSet::clone() const override`
- **`void* icu::UnicodeString::clone() const`**
  - `UnicodeString* icu::UnicodeString::clone() const`
  - `UnicodeString* icu::UnicodeString::clone() const override`
- **`void* icu::VTimeZone::clone() const`**
  - `VTimeZone* icu::VTimeZone::clone() const`
  - `VTimeZone* icu::VTimeZone::clone() const override`

## Colophon

Contents generated by StableAPI tool on Wed Sep 29 11:59:13 PDT 2021

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  