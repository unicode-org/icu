
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 64 (update #1: 64.2) with ICU 65

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 64](#removed)
- [Deprecated or Obsoleted in ICU 65](#deprecated)
- [Changed in  ICU 65](#changed)
- [Promoted to stable in ICU 65](#promoted)
- [Added in ICU 65](#added)
- [Other existing drafts in ICU 65](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 64
  
| File | API | ICU 64 | ICU 65 |
|---|---|---|---|
| decimfmt.h | const number::LocalizedNumberFormatter&amp; icu::DecimalFormat::toNumberFormatter() const |  DeprecatedICU 64 | (missing)
| edits.h | UBool icu::Edits::copyErrorTo(UErrorCode&amp;) |  StableICU 59 | (missing)
| platform.h | <tt>#define</tt> __has_attribute |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_builtin |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_cpp_attribute |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_declspec_attribute |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_extension |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_feature |  Internal | (missing)
| platform.h | <tt>#define</tt> __has_warning |  Internal | (missing)
| uversion.h | <tt>#define</tt> U_NAMESPACE_BEGIN |  StableICU 2.4 | (missing)
| uversion.h | <tt>#define</tt> U_NAMESPACE_END |  StableICU 2.4 | (missing)
| uversion.h | <tt>#define</tt> U_NAMESPACE_QUALIFIER |  StableICU 2.4 | (missing)
| uversion.h | <tt>#define</tt> U_NAMESPACE_USE |  StableICU 2.4 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 65
  
| File | API | ICU 64 | ICU 65 |
|---|---|---|---|

## Changed

Changed in  ICU 65 (old, new)


  
| File | API | ICU 64 | ICU 65 |
|---|---|---|---|
| decimfmt.h | int32_t icu::DecimalFormat::getMultiplierScale() const |  Draft→StableICU 62
| decimfmt.h | void icu::DecimalFormat::setMultiplierScale(int32_t) |  Draft→StableICU 62
| locid.h | Locale&amp; icu::Locale::operator=(Locale&amp;&amp;) |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::getKeywordValue(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::getUnicodeKeywordValue(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::toLanguageTag(UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringEnumeration* icu::Locale::createUnicodeKeywords(UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | icu::Locale::Locale(Locale&amp;&amp;) |  Draft→StableICU 63
| locid.h | <tt>static</tt> Locale icu::Locale::forLanguageTag(StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::addLikelySubtags(UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::getKeywordValue(StringPiece, ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getKeywords(OutputIterator, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getUnicodeKeywordValue(StringPiece, ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getUnicodeKeywords(OutputIterator, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::minimizeSubtags(UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::setKeywordValue(StringPiece, StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::setUnicodeKeywordValue(StringPiece, StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::toLanguageTag(ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createAtmosphere(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPercent(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermille(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPetabyte(UErrorCode&amp;) |  Draft→StableICU 63
| numberformatter.h | Appendable&amp; icu::number::FormattedNumber::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptPerUnit(icu::MeasureUnit*) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptPerUnit(icu::MeasureUnit*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptSymbols(NumberingSystem*) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptSymbols(NumberingSystem*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptUnit(icu::MeasureUnit*) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptUnit(icu::MeasureUnit*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::decimal(UNumberDecimalSeparatorDisplay) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::decimal(UNumberDecimalSeparatorDisplay)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::grouping(UNumberGroupingStrategy) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::grouping(UNumberGroupingStrategy)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::integerWidth(const IntegerWidth&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::integerWidth(const IntegerWidth&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::notation(const Notation&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::notation(const Notation&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::perUnit(const icu::MeasureUnit&amp;) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::perUnit(const icu::MeasureUnit&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::precision(const Precision&amp;) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::precision(const Precision&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::roundingMode(UNumberFormatRoundingMode) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::roundingMode(UNumberFormatRoundingMode)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::scale(const Scale&amp;) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::scale(const Scale&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::sign(UNumberSignDisplay) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::sign(UNumberSignDisplay)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::symbols(const DecimalFormatSymbols&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::symbols(const DecimalFormatSymbols&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unit(const icu::MeasureUnit&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unit(const icu::MeasureUnit&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitWidth(UNumberUnitWidth) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitWidth(UNumberUnitWidth)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Format* icu::number::LocalizedNumberFormatter::toFormat(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatDecimal(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatDouble(double, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatInt(int64_t, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber&amp; icu::number::FormattedNumber::operator=(FormattedNumber&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | IntegerWidth icu::number::IntegerWidth::truncateAt(int32_t) |  Draft→StableICU 60
| numberformatter.h | LocalizedNumberFormatter icu::number::UnlocalizedNumberFormatter::locale(const icu::Locale&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | LocalizedNumberFormatter icu::number::UnlocalizedNumberFormatter::locale(const icu::Locale&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | LocalizedNumberFormatter&amp; icu::number::LocalizedNumberFormatter::operator=(LocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | LocalizedNumberFormatter&amp; icu::number::LocalizedNumberFormatter::operator=(const LocalizedNumberFormatter&amp;) |  Draft→StableICU 62
| numberformatter.h | Precision icu::number::CurrencyPrecision::withCurrency(const CurrencyUnit&amp;) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::FractionPrecision::withMaxDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::FractionPrecision::withMinDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::IncrementPrecision::withMinFraction(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Scale&amp; icu::number::Scale::operator=(Scale&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | Scale&amp; icu::number::Scale::operator=(const Scale&amp;) |  Draft→StableICU 62
| numberformatter.h | ScientificNotation icu::number::ScientificNotation::withExponentSignDisplay(UNumberSignDisplay) const |  Draft→StableICU 60
| numberformatter.h | ScientificNotation icu::number::ScientificNotation::withMinExponentDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | UBool icu::number::NumberFormatterSettings&lt; Derived &gt;::copyErrorTo(UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | UnicodeString icu::number::FormattedNumber::toString(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | UnicodeString icu::number::NumberFormatterSettings&lt; Derived &gt;::toSkeleton(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | UnlocalizedNumberFormatter&amp; icu::number::UnlocalizedNumberFormatter::operator=(UnlocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | UnlocalizedNumberFormatter&amp; icu::number::UnlocalizedNumberFormatter::operator=(const UnlocalizedNumberFormatter&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::FormattedNumber::FormattedNumber(FormattedNumber&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::FormattedNumber::~FormattedNumber() |  Draft→StableICU 60
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter()=default |  Draft→StableICU 62
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(LocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(const LocalizedNumberFormatter&amp;) |  Draft→StableICU 60
| numberformatter.h | icu::number::LocalizedNumberFormatter::~LocalizedNumberFormatter() |  Draft→StableICU 60
| numberformatter.h | icu::number::Scale::Scale(Scale&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::Scale::Scale(const Scale&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::Scale::~Scale() |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter()=default |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(UnlocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(const UnlocalizedNumberFormatter&amp;) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CompactNotation icu::number::Notation::compactLong() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CompactNotation icu::number::Notation::compactShort() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CurrencyPrecision icu::number::Precision::currency(UCurrencyUsage) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::fixedFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::integer() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::maxFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::minFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::minMaxFraction(int32_t, int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> IncrementPrecision icu::number::Precision::increment(double) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> IntegerWidth icu::number::IntegerWidth::zeroFillTo(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> LocalizedNumberFormatter icu::number::NumberFormatter::withLocale(const Locale&amp;) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> Precision icu::number::Precision::unlimited() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDecimal(StringPiece) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDouble(double) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDoubleAndPowerOfTen(double, int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::none() |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::powerOfTen(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> ScientificNotation icu::number::Notation::engineering() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> ScientificNotation icu::number::Notation::scientific() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::fixedSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::maxSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::minMaxSignificantDigits(int32_t, int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::minSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SimpleNotation icu::number::Notation::simple() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::forSkeleton(const UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::with() |  Draft→StableICU 60
| numberrangeformatter.h | Appendable&amp; icu::number::FormattedNumberRange::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::collapse(UNumberRangeCollapse) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::collapse(UNumberRangeCollapse)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::identityFallback(UNumberRangeIdentityFallback) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::identityFallback(UNumberRangeIdentityFallback)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | FormattedNumberRange icu::number::LocalizedNumberRangeFormatter::formatFormattableRange(const Formattable&amp;, const Formattable&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | FormattedNumberRange&amp; icu::number::FormattedNumberRange::operator=(FormattedNumberRange&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter icu::number::UnlocalizedNumberRangeFormatter::locale(const icu::Locale&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter icu::number::UnlocalizedNumberRangeFormatter::locale(const icu::Locale&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter&amp; icu::number::LocalizedNumberRangeFormatter::operator=(LocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter&amp; icu::number::LocalizedNumberRangeFormatter::operator=(const LocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | UBool icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::copyErrorTo(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UNumberRangeIdentityResult icu::number::FormattedNumberRange::getIdentityResult(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::toString(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter&amp; icu::number::UnlocalizedNumberRangeFormatter::operator=(UnlocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter&amp; icu::number::UnlocalizedNumberRangeFormatter::operator=(const UnlocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_ALL |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_AUTO |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_NONE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_UNIT |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_APPROXIMATELY |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_RANGE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_SINGLE_VALUE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_NOT_EQUAL |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::FormattedNumberRange::FormattedNumberRange(FormattedNumberRange&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::FormattedNumberRange::~FormattedNumberRange() |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter()=default |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(LocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(const LocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::~LocalizedNumberRangeFormatter() |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter()=default |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(UnlocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(const UnlocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>static</tt> LocalizedNumberRangeFormatter icu::number::NumberRangeFormatter::withLocale(const Locale&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>static</tt> UnlocalizedNumberRangeFormatter icu::number::NumberRangeFormatter::with() |  Draft→StableICU 63
| reldatefmt.h | <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_QUARTER |  Draft→StableICU 63
| uchar.h | const UCPMap* u_getIntPropertyMap(UProperty, UErrorCode*) |  Draft→StableICU 63
| uchar.h | const USet* u_getBinaryPropertySet(UProperty, UErrorCode*) |  Draft→StableICU 63
| ucpmap.h | UChar32 ucpmap_getRange(const UCPMap*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_FIXED_ALL_SURROGATES |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_FIXED_LEAD_SURROGATES |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_NORMAL |  Draft→StableICU 63
| ucpmap.h | uint32_t ucpmap_get(const UCPMap*, UChar32) |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_16 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_32 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_8 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_ASCII_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_BMP_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_SUPP_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U16_NEXT |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U16_PREV |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U8_NEXT |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U8_PREV |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_SMALL_GET |  Draft→StableICU 63
| ucptrie.h | UCPTrie* ucptrie_openFromBinary(UCPTrieType, UCPTrieValueWidth, const void*, int32_t, int32_t*, UErrorCode*) |  Draft→StableICU 63
| ucptrie.h | UCPTrieType ucptrie_getType(const UCPTrie*) |  Draft→StableICU 63
| ucptrie.h | UCPTrieValueWidth ucptrie_getValueWidth(const UCPTrie*) |  Draft→StableICU 63
| ucptrie.h | UChar32 ucptrie_getRange(const UCPTrie*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_ANY |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_FAST |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_SMALL |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_16 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_32 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_8 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_ANY |  Draft→StableICU 63
| ucptrie.h | int32_t ucptrie_toBinary(const UCPTrie*, void*, int32_t, UErrorCode*) |  Draft→StableICU 63
| ucptrie.h | uint32_t ucptrie_get(const UCPTrie*, UChar32) |  Draft→StableICU 63
| ucptrie.h | void ucptrie_close(UCPTrie*) |  Draft→StableICU 63
| umutablecptrie.h | UCPTrie* umutablecptrie_buildImmutable(UMutableCPTrie*, UCPTrieType, UCPTrieValueWidth, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_clone(const UMutableCPTrie*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_fromUCPMap(const UCPMap*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_fromUCPTrie(const UCPTrie*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_open(uint32_t, uint32_t, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UChar32 umutablecptrie_getRange(const UMutableCPTrie*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| umutablecptrie.h | uint32_t umutablecptrie_get(const UMutableCPTrie*, UChar32) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_close(UMutableCPTrie*) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_set(UMutableCPTrie*, UChar32, uint32_t, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_setRange(UMutableCPTrie*, UChar32, UChar32, uint32_t, UErrorCode*) |  Draft→StableICU 63
| unumberformatter.h | <tt>enum</tt> UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_AUTO |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_AUTO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_MIN2 |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_OFF |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_ON_ALIGNED |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_THOUSANDS |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_AUTO |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_EXCEPT_ZERO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEVER |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_HIDDEN |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_NARROW |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_SHORT |  Draft→StableICU 60
| uspoof.h | <tt>enum</tt> USpoofChecks::USPOOF_HIDDEN_OVERLAY |  Draft→StableICU 62
| utf_old.h | <tt>#define</tt> U_HIDE_OBSOLETE_UTF_OLD_H |  DeprecatedICU 2.4 | Internal

## Promoted

Promoted to stable in ICU 65
  
| File | API | ICU 64 | ICU 65 |
|---|---|---|---|
| basictz.h | void* icu::BasicTimeZone::clone() const |  (missing) | StableICU 3.8
| datefmt.h | void* icu::DateFormat::clone() const |  (missing) | StableICU 2.0
| decimfmt.h | int32_t icu::DecimalFormat::getMultiplierScale() const |  Draft→StableICU 62
| decimfmt.h | void icu::DecimalFormat::setMultiplierScale(int32_t) |  Draft→StableICU 62
| edits.h | UBool icu::Edits::copyErrorTo(UErrorCode&amp;) const |  (missing) | StableICU 59
| locid.h | Locale&amp; icu::Locale::operator=(Locale&amp;&amp;) |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::getKeywordValue(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::getUnicodeKeywordValue(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringClass icu::Locale::toLanguageTag(UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | StringEnumeration* icu::Locale::createUnicodeKeywords(UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | icu::Locale::Locale(Locale&amp;&amp;) |  Draft→StableICU 63
| locid.h | <tt>static</tt> Locale icu::Locale::forLanguageTag(StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::addLikelySubtags(UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::getKeywordValue(StringPiece, ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getKeywords(OutputIterator, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getUnicodeKeywordValue(StringPiece, ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::getUnicodeKeywords(OutputIterator, UErrorCode&amp;) const |  Draft→StableICU 63
| locid.h | void icu::Locale::minimizeSubtags(UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::setKeywordValue(StringPiece, StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::setUnicodeKeywordValue(StringPiece, StringPiece, UErrorCode&amp;) |  Draft→StableICU 63
| locid.h | void icu::Locale::toLanguageTag(ByteSink&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createAtmosphere(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPercent(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermille(UErrorCode&amp;) |  Draft→StableICU 63
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPetabyte(UErrorCode&amp;) |  Draft→StableICU 63
| numberformatter.h | Appendable&amp; icu::number::FormattedNumber::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptPerUnit(icu::MeasureUnit*) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptPerUnit(icu::MeasureUnit*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptSymbols(NumberingSystem*) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptSymbols(NumberingSystem*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptUnit(icu::MeasureUnit*) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::adoptUnit(icu::MeasureUnit*)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::decimal(UNumberDecimalSeparatorDisplay) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::decimal(UNumberDecimalSeparatorDisplay)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::grouping(UNumberGroupingStrategy) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::grouping(UNumberGroupingStrategy)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::integerWidth(const IntegerWidth&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::integerWidth(const IntegerWidth&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::notation(const Notation&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::notation(const Notation&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::perUnit(const icu::MeasureUnit&amp;) const&amp; |  Draft→StableICU 61
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::perUnit(const icu::MeasureUnit&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::precision(const Precision&amp;) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::precision(const Precision&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::roundingMode(UNumberFormatRoundingMode) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::roundingMode(UNumberFormatRoundingMode)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::scale(const Scale&amp;) const&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::scale(const Scale&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::sign(UNumberSignDisplay) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::sign(UNumberSignDisplay)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::symbols(const DecimalFormatSymbols&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::symbols(const DecimalFormatSymbols&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unit(const icu::MeasureUnit&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unit(const icu::MeasureUnit&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitWidth(UNumberUnitWidth) const&amp; |  Draft→StableICU 60
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitWidth(UNumberUnitWidth)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | Format* icu::number::LocalizedNumberFormatter::toFormat(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatDecimal(StringPiece, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatDouble(double, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber icu::number::LocalizedNumberFormatter::formatInt(int64_t, UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | FormattedNumber&amp; icu::number::FormattedNumber::operator=(FormattedNumber&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | IntegerWidth icu::number::IntegerWidth::truncateAt(int32_t) |  Draft→StableICU 60
| numberformatter.h | LocalizedNumberFormatter icu::number::UnlocalizedNumberFormatter::locale(const icu::Locale&amp;) const&amp; |  Draft→StableICU 60
| numberformatter.h | LocalizedNumberFormatter icu::number::UnlocalizedNumberFormatter::locale(const icu::Locale&amp;)&amp;&amp; |  Draft→StableICU 62
| numberformatter.h | LocalizedNumberFormatter&amp; icu::number::LocalizedNumberFormatter::operator=(LocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | LocalizedNumberFormatter&amp; icu::number::LocalizedNumberFormatter::operator=(const LocalizedNumberFormatter&amp;) |  Draft→StableICU 62
| numberformatter.h | Precision icu::number::CurrencyPrecision::withCurrency(const CurrencyUnit&amp;) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::FractionPrecision::withMaxDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::FractionPrecision::withMinDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Precision icu::number::IncrementPrecision::withMinFraction(int32_t) const |  Draft→StableICU 60
| numberformatter.h | Scale&amp; icu::number::Scale::operator=(Scale&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | Scale&amp; icu::number::Scale::operator=(const Scale&amp;) |  Draft→StableICU 62
| numberformatter.h | ScientificNotation icu::number::ScientificNotation::withExponentSignDisplay(UNumberSignDisplay) const |  Draft→StableICU 60
| numberformatter.h | ScientificNotation icu::number::ScientificNotation::withMinExponentDigits(int32_t) const |  Draft→StableICU 60
| numberformatter.h | UBool icu::number::NumberFormatterSettings&lt; Derived &gt;::copyErrorTo(UErrorCode&amp;) const |  Draft→StableICU 60
| numberformatter.h | UnicodeString icu::number::FormattedNumber::toString(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | UnicodeString icu::number::NumberFormatterSettings&lt; Derived &gt;::toSkeleton(UErrorCode&amp;) const |  Draft→StableICU 62
| numberformatter.h | UnlocalizedNumberFormatter&amp; icu::number::UnlocalizedNumberFormatter::operator=(UnlocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | UnlocalizedNumberFormatter&amp; icu::number::UnlocalizedNumberFormatter::operator=(const UnlocalizedNumberFormatter&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::FormattedNumber::FormattedNumber(FormattedNumber&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::FormattedNumber::~FormattedNumber() |  Draft→StableICU 60
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter()=default |  Draft→StableICU 62
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(LocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(const LocalizedNumberFormatter&amp;) |  Draft→StableICU 60
| numberformatter.h | icu::number::LocalizedNumberFormatter::~LocalizedNumberFormatter() |  Draft→StableICU 60
| numberformatter.h | icu::number::Scale::Scale(Scale&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::Scale::Scale(const Scale&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::Scale::~Scale() |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter()=default |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(UnlocalizedNumberFormatter&amp;&amp;) |  Draft→StableICU 62
| numberformatter.h | icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(const UnlocalizedNumberFormatter&amp;) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CompactNotation icu::number::Notation::compactLong() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CompactNotation icu::number::Notation::compactShort() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> CurrencyPrecision icu::number::Precision::currency(UCurrencyUsage) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::fixedFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::integer() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::maxFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::minFraction(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> FractionPrecision icu::number::Precision::minMaxFraction(int32_t, int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> IncrementPrecision icu::number::Precision::increment(double) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> IntegerWidth icu::number::IntegerWidth::zeroFillTo(int32_t) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> LocalizedNumberFormatter icu::number::NumberFormatter::withLocale(const Locale&amp;) |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> Precision icu::number::Precision::unlimited() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDecimal(StringPiece) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDouble(double) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::byDoubleAndPowerOfTen(double, int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::none() |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> Scale icu::number::Scale::powerOfTen(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> ScientificNotation icu::number::Notation::engineering() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> ScientificNotation icu::number::Notation::scientific() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::fixedSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::maxSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::minMaxSignificantDigits(int32_t, int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SignificantDigitsPrecision icu::number::Precision::minSignificantDigits(int32_t) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> SimpleNotation icu::number::Notation::simple() |  Draft→StableICU 60
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::forSkeleton(const UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 62
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::with() |  Draft→StableICU 60
| numberrangeformatter.h | Appendable&amp; icu::number::FormattedNumberRange::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::collapse(UNumberRangeCollapse) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::collapse(UNumberRangeCollapse)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::identityFallback(UNumberRangeIdentityFallback) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::identityFallback(UNumberRangeIdentityFallback)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterBoth(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterFirst(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(UnlocalizedNumberFormatter&amp;&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(UnlocalizedNumberFormatter&amp;&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(const UnlocalizedNumberFormatter&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | Derived icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::numberFormatterSecond(const UnlocalizedNumberFormatter&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | FormattedNumberRange icu::number::LocalizedNumberRangeFormatter::formatFormattableRange(const Formattable&amp;, const Formattable&amp;, UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | FormattedNumberRange&amp; icu::number::FormattedNumberRange::operator=(FormattedNumberRange&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter icu::number::UnlocalizedNumberRangeFormatter::locale(const icu::Locale&amp;) const&amp; |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter icu::number::UnlocalizedNumberRangeFormatter::locale(const icu::Locale&amp;)&amp;&amp; |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter&amp; icu::number::LocalizedNumberRangeFormatter::operator=(LocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | LocalizedNumberRangeFormatter&amp; icu::number::LocalizedNumberRangeFormatter::operator=(const LocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | UBool icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::copyErrorTo(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UNumberRangeIdentityResult icu::number::FormattedNumberRange::getIdentityResult(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::toString(UErrorCode&amp;) const |  Draft→StableICU 63
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter&amp; icu::number::UnlocalizedNumberRangeFormatter::operator=(UnlocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter&amp; icu::number::UnlocalizedNumberRangeFormatter::operator=(const UnlocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_ALL |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_AUTO |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_NONE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeCollapse::UNUM_RANGE_COLLAPSE_UNIT |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_APPROXIMATELY |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_RANGE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityFallback::UNUM_IDENTITY_FALLBACK_SINGLE_VALUE |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING |  Draft→StableICU 63
| numberrangeformatter.h | <tt>enum</tt> UNumberRangeIdentityResult::UNUM_IDENTITY_RESULT_NOT_EQUAL |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::FormattedNumberRange::FormattedNumberRange(FormattedNumberRange&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::FormattedNumberRange::~FormattedNumberRange() |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter()=default |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(LocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(const LocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::LocalizedNumberRangeFormatter::~LocalizedNumberRangeFormatter() |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter()=default |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(UnlocalizedNumberRangeFormatter&amp;&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(const UnlocalizedNumberRangeFormatter&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>static</tt> LocalizedNumberRangeFormatter icu::number::NumberRangeFormatter::withLocale(const Locale&amp;) |  Draft→StableICU 63
| numberrangeformatter.h | <tt>static</tt> UnlocalizedNumberRangeFormatter icu::number::NumberRangeFormatter::with() |  Draft→StableICU 63
| numfmt.h | void* icu::NumberFormat::clone() const |  (missing) | StableICU 2.0
| reldatefmt.h | <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_QUARTER |  Draft→StableICU 63
| uchar.h | const UCPMap* u_getIntPropertyMap(UProperty, UErrorCode*) |  Draft→StableICU 63
| uchar.h | const USet* u_getBinaryPropertySet(UProperty, UErrorCode*) |  Draft→StableICU 63
| ucpmap.h | UChar32 ucpmap_getRange(const UCPMap*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_FIXED_ALL_SURROGATES |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_FIXED_LEAD_SURROGATES |  Draft→StableICU 63
| ucpmap.h | <tt>enum</tt> UCPMapRangeOption::UCPMAP_RANGE_NORMAL |  Draft→StableICU 63
| ucpmap.h | uint32_t ucpmap_get(const UCPMap*, UChar32) |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_16 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_32 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_8 |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_ASCII_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_BMP_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_SUPP_GET |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U16_NEXT |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U16_PREV |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U8_NEXT |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_FAST_U8_PREV |  Draft→StableICU 63
| ucptrie.h | <tt>#define</tt> UCPTRIE_SMALL_GET |  Draft→StableICU 63
| ucptrie.h | UCPTrie* ucptrie_openFromBinary(UCPTrieType, UCPTrieValueWidth, const void*, int32_t, int32_t*, UErrorCode*) |  Draft→StableICU 63
| ucptrie.h | UCPTrieType ucptrie_getType(const UCPTrie*) |  Draft→StableICU 63
| ucptrie.h | UCPTrieValueWidth ucptrie_getValueWidth(const UCPTrie*) |  Draft→StableICU 63
| ucptrie.h | UChar32 ucptrie_getRange(const UCPTrie*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_ANY |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_FAST |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieType::UCPTRIE_TYPE_SMALL |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_16 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_32 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_8 |  Draft→StableICU 63
| ucptrie.h | <tt>enum</tt> UCPTrieValueWidth::UCPTRIE_VALUE_BITS_ANY |  Draft→StableICU 63
| ucptrie.h | int32_t ucptrie_toBinary(const UCPTrie*, void*, int32_t, UErrorCode*) |  Draft→StableICU 63
| ucptrie.h | uint32_t ucptrie_get(const UCPTrie*, UChar32) |  Draft→StableICU 63
| ucptrie.h | void ucptrie_close(UCPTrie*) |  Draft→StableICU 63
| umutablecptrie.h | UCPTrie* umutablecptrie_buildImmutable(UMutableCPTrie*, UCPTrieType, UCPTrieValueWidth, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_clone(const UMutableCPTrie*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_fromUCPMap(const UCPMap*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_fromUCPTrie(const UCPTrie*, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UMutableCPTrie* umutablecptrie_open(uint32_t, uint32_t, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | UChar32 umutablecptrie_getRange(const UMutableCPTrie*, UChar32, UCPMapRangeOption, uint32_t, UCPMapValueFilter*, const void*, uint32_t*) |  Draft→StableICU 63
| umutablecptrie.h | uint32_t umutablecptrie_get(const UMutableCPTrie*, UChar32) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_close(UMutableCPTrie*) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_set(UMutableCPTrie*, UChar32, uint32_t, UErrorCode*) |  Draft→StableICU 63
| umutablecptrie.h | void umutablecptrie_setRange(UMutableCPTrie*, UChar32, UChar32, uint32_t, UErrorCode*) |  Draft→StableICU 63
| unifilt.h | void* icu::UnicodeFilter::clone() const |  (missing) | StableICU 2.4
| unumberformatter.h | <tt>enum</tt> UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_AUTO |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_AUTO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_MIN2 |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_OFF |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_ON_ALIGNED |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberGroupingStrategy::UNUM_GROUPING_THOUSANDS |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ALWAYS |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_AUTO |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_EXCEPT_ZERO |  Draft→StableICU 61
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEVER |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_HIDDEN |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_NARROW |  Draft→StableICU 60
| unumberformatter.h | <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_SHORT |  Draft→StableICU 60
| uspoof.h | <tt>enum</tt> USpoofChecks::USPOOF_HIDDEN_OVERLAY |  Draft→StableICU 62

## Added

Added in ICU 65
  
| File | API | ICU 64 | ICU 65 |
|---|---|---|---|
| basictz.h | void* icu::BasicTimeZone::clone() const |  (missing) | StableICU 3.8
| bytestrie.h | BytesTrie&amp; icu::BytesTrie::resetToState64(uint64_t) |  (missing) | DraftICU 65
| bytestrie.h | uint64_t icu::BytesTrie::getState64() const |  (missing) | DraftICU 65
| datefmt.h | void* icu::DateFormat::clone() const |  (missing) | StableICU 2.0
| edits.h | UBool icu::Edits::copyErrorTo(UErrorCode&amp;) const |  (missing) | StableICU 59
| localebuilder.h | UBool icu::LocaleBuilder::copyErrorTo(UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::addSupportedLocale(const Locale&amp;) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::operator=(Builder&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setDefaultLocale(const Locale*) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setDemotionPerDesiredLocale(ULocMatchDemotion) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setFavorSubtag(ULocMatchFavorSubtag) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocales(Iter, Iter) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocales(Locale::Iterator&amp;) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocalesFromListString(StringPiece) |  (missing) | DraftICU 65
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocalesViaConverter(Iter, Iter, Conv) |  (missing) | DraftICU 65
| localematcher.h | Locale icu::LocaleMatcher::Result::makeResolvedLocale(UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | LocaleMatcher icu::LocaleMatcher::Builder::build(UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | LocaleMatcher&amp; icu::LocaleMatcher::operator=(LocaleMatcher&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | Result icu::LocaleMatcher::getBestMatchResult(Locale::Iterator&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | Result icu::LocaleMatcher::getBestMatchResult(const Locale&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | Result&amp; icu::LocaleMatcher::Result::operator=(Result&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | UBool icu::LocaleMatcher::Builder::copyErrorTo(UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | const Locale* icu::LocaleMatcher::Result::getDesiredLocale() const |  (missing) | DraftICU 65
| localematcher.h | const Locale* icu::LocaleMatcher::Result::getSupportedLocale() const |  (missing) | DraftICU 65
| localematcher.h | const Locale* icu::LocaleMatcher::getBestMatch(Locale::Iterator&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | const Locale* icu::LocaleMatcher::getBestMatch(const Locale&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | const Locale* icu::LocaleMatcher::getBestMatchForListString(StringPiece, UErrorCode&amp;) const |  (missing) | DraftICU 65
| localematcher.h | double icu::LocaleMatcher::internalMatch(const Locale&amp;, const Locale&amp;, UErrorCode&amp;) const |  (missing) | Internal
| localematcher.h | <tt>enum</tt> ULocMatchDemotion::ULOCMATCH_DEMOTION_NONE |  (missing) | DraftICU 65
| localematcher.h | <tt>enum</tt> ULocMatchDemotion::ULOCMATCH_DEMOTION_REGION |  (missing) | DraftICU 65
| localematcher.h | <tt>enum</tt> ULocMatchFavorSubtag::ULOCMATCH_FAVOR_LANGUAGE |  (missing) | DraftICU 65
| localematcher.h | <tt>enum</tt> ULocMatchFavorSubtag::ULOCMATCH_FAVOR_SCRIPT |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::Builder::Builder() |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::Builder::Builder(Builder&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::Builder::~Builder() |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::LocaleMatcher(LocaleMatcher&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::Result::Result(Result&amp;&amp;) |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::Result::~Result() |  (missing) | DraftICU 65
| localematcher.h | icu::LocaleMatcher::~LocaleMatcher() |  (missing) | DraftICU 65
| localematcher.h | int32_t icu::LocaleMatcher::Result::getDesiredIndex() const |  (missing) | DraftICU 65
| localematcher.h | int32_t icu::LocaleMatcher::Result::getSupportedIndex() const |  (missing) | DraftICU 65
| locid.h | UBool icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::hasNext() const override |  (missing) | DraftICU 65
| locid.h | UBool icu::Locale::Iterator::hasNext() const |  (missing) | DraftICU 65
| locid.h | UBool icu::Locale::RangeIterator&lt; Iter &gt;::hasNext() const override |  (missing) | DraftICU 65
| locid.h | const Locale&amp; icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::next() override |  (missing) | DraftICU 65
| locid.h | const Locale&amp; icu::Locale::Iterator::next() |  (missing) | DraftICU 65
| locid.h | const Locale&amp; icu::Locale::RangeIterator&lt; Iter &gt;::next() override |  (missing) | DraftICU 65
| locid.h | icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::ConvertingIterator(Iter, Iter, Conv) |  (missing) | DraftICU 65
| locid.h | icu::Locale::Iterator::~Iterator() |  (missing) | DraftICU 65
| locid.h | icu::Locale::RangeIterator&lt; Iter &gt;::RangeIterator(Iter, Iter) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBar() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecade() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerCentimeter() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerInch() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getEm() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapixel() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPascal() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixel() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerCentimeter() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerInch() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getThermUs() |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBar(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDecade(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerCentimeter(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerInch(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEm(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapixel(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPascal(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixel(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerCentimeter(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerInch(UErrorCode&amp;) |  (missing) | DraftICU 65
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createThermUs(UErrorCode&amp;) |  (missing) | DraftICU 65
| numberformatter.h | StringClass icu::number::FormattedNumber::toDecimalNumber(UErrorCode&amp;) const |  (missing) | DraftICU 65
| numfmt.h | void* icu::NumberFormat::clone() const |  (missing) | StableICU 2.0
| platform.h | <tt>#define</tt> UPRV_HAS_ATTRIBUTE |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_BUILTIN |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_CPP_ATTRIBUTE |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_DECLSPEC_ATTRIBUTE |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_EXTENSION |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_FEATURE |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_WARNING |  (missing) | Internal
| platform.h | <tt>#define</tt> U_PF_EMSCRIPTEN |  (missing) | Internal
| reldatefmt.h | <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_HOUR |  (missing) | DraftICU 65
| reldatefmt.h | <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_MINUTE |  (missing) | DraftICU 65
| stringpiece.h | icu::StringPiece::StringPiece(T) |  (missing) | DraftICU 65
| ucal.h | int32_t ucal_getHostTimeZone(UChar*, int32_t, UErrorCode*) |  (missing) | DraftICU 65
| ucharstrie.h | UCharsTrie&amp; icu::UCharsTrie::resetToState64(uint64_t) |  (missing) | DraftICU 65
| ucharstrie.h | uint64_t icu::UCharsTrie::getState64() const |  (missing) | DraftICU 65
| uloc.h | UEnumeration* uloc_openAvailableByType(ULocAvailableType, UErrorCode*) |  (missing) | DraftICU 65
| uloc.h | <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_COUNT |  (missing) | Internal
| uloc.h | <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_DEFAULT |  (missing) | DraftICU 65
| uloc.h | <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_ONLY_LEGACY_ALIASES |  (missing) | DraftICU 65
| uloc.h | <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_WITH_LEGACY_ALIASES |  (missing) | DraftICU 65
| umachine.h | <tt>#define</tt> UPRV_BLOCK_MACRO_BEGIN |  (missing) | Internal
| umachine.h | <tt>#define</tt> UPRV_BLOCK_MACRO_END |  (missing) | Internal
| unifilt.h | void* icu::UnicodeFilter::clone() const |  (missing) | StableICU 2.4
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_RES_DATA_LIMIT |  (missing) | Internal
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_BUNDLE |  (missing) | DraftICU 65
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_DATA_FILE |  (missing) | DraftICU 65
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RESOURCE |  (missing) | DraftICU 65
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RES_FILE |  (missing) | DraftICU 65
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_START |  (missing) | DraftICU 65

## Other

Other existing drafts in ICU 65

| File | API | ICU 64 | ICU 65 |
|---|---|---|---|
| currunit.h |  icu::CurrencyUnit::CurrencyUnit(StringPiece, UErrorCode&amp;) | DraftICU 64 | 
| decimfmt.h |  UBool icu::DecimalFormat::isFormatFailIfMoreThanMaxDigits() const | DraftICU 64 | 
| decimfmt.h |  UBool icu::DecimalFormat::isParseCaseSensitive() const | DraftICU 64 | 
| decimfmt.h |  UBool icu::DecimalFormat::isParseNoExponent() const | DraftICU 64 | 
| decimfmt.h |  UBool icu::DecimalFormat::isSignAlwaysShown() const | DraftICU 64 | 
| decimfmt.h |  const number::LocalizedNumberFormatter* icu::DecimalFormat::toNumberFormatter(UErrorCode&amp;) const | DraftICU 64 | 
| decimfmt.h |  int32_t icu::DecimalFormat::getMinimumGroupingDigits() const | DraftICU 64 | 
| decimfmt.h |  void icu::DecimalFormat::setFormatFailIfMoreThanMaxDigits(UBool) | DraftICU 64 | 
| decimfmt.h |  void icu::DecimalFormat::setMinimumGroupingDigits(int32_t) | DraftICU 64 | 
| decimfmt.h |  void icu::DecimalFormat::setParseCaseSensitive(UBool) | DraftICU 64 | 
| decimfmt.h |  void icu::DecimalFormat::setParseNoExponent(UBool) | DraftICU 64 | 
| decimfmt.h |  void icu::DecimalFormat::setSignAlwaysShown(UBool) | DraftICU 64 | 
| dtitvfmt.h |  Appendable&amp; icu::FormattedDateInterval::appendTo(Appendable&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  FormattedDateInterval icu::DateIntervalFormat::formatToValue(Calendar&amp;, Calendar&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  FormattedDateInterval icu::DateIntervalFormat::formatToValue(const DateInterval&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  FormattedDateInterval&amp; icu::FormattedDateInterval::operator=(FormattedDateInterval&amp;&amp;) | DraftICU 64 | 
| dtitvfmt.h |  UBool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  UnicodeString icu::FormattedDateInterval::toString(UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| dtitvfmt.h |  icu::FormattedDateInterval::FormattedDateInterval() | DraftICU 64 | 
| dtitvfmt.h |  icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&amp;&amp;) | DraftICU 64 | 
| dtitvfmt.h |  icu::FormattedDateInterval::~FormattedDateInterval() | DraftICU 64 | 
| formattedvalue.h |  Appendable&amp; icu::FormattedValue::appendTo(Appendable&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| formattedvalue.h |  UBool icu::ConstrainedFieldPosition::matchesField(int32_t, int32_t) const | DraftICU 64 | 
| formattedvalue.h |  UBool icu::FormattedValue::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| formattedvalue.h |  UnicodeString icu::FormattedValue::toString(UErrorCode&amp;) const | DraftICU 64 | 
| formattedvalue.h |  UnicodeString icu::FormattedValue::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| formattedvalue.h |  icu::ConstrainedFieldPosition::ConstrainedFieldPosition() | DraftICU 64 | 
| formattedvalue.h |  icu::ConstrainedFieldPosition::~ConstrainedFieldPosition() | DraftICU 64 | 
| formattedvalue.h |  icu::FormattedValue::~FormattedValue() | DraftICU 64 | 
| formattedvalue.h |  int32_t icu::ConstrainedFieldPosition::getCategory() const | DraftICU 64 | 
| formattedvalue.h |  int32_t icu::ConstrainedFieldPosition::getField() const | DraftICU 64 | 
| formattedvalue.h |  int32_t icu::ConstrainedFieldPosition::getLimit() const | DraftICU 64 | 
| formattedvalue.h |  int32_t icu::ConstrainedFieldPosition::getStart() const | DraftICU 64 | 
| formattedvalue.h |  int64_t icu::ConstrainedFieldPosition::getInt64IterationContext() const | DraftICU 64 | 
| formattedvalue.h |  void icu::ConstrainedFieldPosition::constrainCategory(int32_t) | DraftICU 64 | 
| formattedvalue.h |  void icu::ConstrainedFieldPosition::constrainField(int32_t, int32_t) | DraftICU 64 | 
| formattedvalue.h |  void icu::ConstrainedFieldPosition::reset() | DraftICU 64 | 
| formattedvalue.h |  void icu::ConstrainedFieldPosition::setInt64IterationContext(int64_t) | DraftICU 64 | 
| formattedvalue.h |  void icu::ConstrainedFieldPosition::setState(int32_t, int32_t, int32_t, int32_t) | DraftICU 64 | 
| listformatter.h |  Appendable&amp; icu::FormattedList::appendTo(Appendable&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| listformatter.h |  FormattedList icu::ListFormatter::formatStringsToValue(const UnicodeString items[], int32_t, UErrorCode&amp;) const | DraftICU 64 | 
| listformatter.h |  FormattedList&amp; icu::FormattedList::operator=(FormattedList&amp;&amp;) | DraftICU 64 | 
| listformatter.h |  UBool icu::FormattedList::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| listformatter.h |  UnicodeString icu::FormattedList::toString(UErrorCode&amp;) const | DraftICU 64 | 
| listformatter.h |  UnicodeString icu::FormattedList::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| listformatter.h |  UnicodeString&amp; icu::ListFormatter::format(const UnicodeString items[], int32_t, UnicodeString&amp;, FieldPositionIterator*, UErrorCode&amp;) const | DraftICU 63 | 
| listformatter.h |  icu::FormattedList::FormattedList() | DraftICU 64 | 
| listformatter.h |  icu::FormattedList::FormattedList(FormattedList&amp;&amp;) | DraftICU 64 | 
| listformatter.h |  icu::FormattedList::~FormattedList() | DraftICU 64 | 
| localebuilder.h |  Locale icu::LocaleBuilder::build(UErrorCode&amp;) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::addUnicodeLocaleAttribute(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::clear() | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::clearExtensions() | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::removeUnicodeLocaleAttribute(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setExtension(char, StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setLanguage(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setLanguageTag(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setLocale(const Locale&amp;) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setRegion(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setScript(StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setUnicodeLocaleKeyword(StringPiece, StringPiece) | DraftICU 64 | 
| localebuilder.h |  LocaleBuilder&amp; icu::LocaleBuilder::setVariant(StringPiece) | DraftICU 64 | 
| localebuilder.h |  icu::LocaleBuilder::LocaleBuilder() | DraftICU 64 | 
| localebuilder.h |  icu::LocaleBuilder::~LocaleBuilder() | DraftICU 64 | 
| localpointer.h |  LocalArray&lt;T&gt;&amp; icu::LocalArray&lt; T &gt;::operator=(std::unique_ptr&lt; T[]&gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  LocalPointer&lt;T&gt;&amp; icu::LocalPointer&lt; T &gt;::operator=(std::unique_ptr&lt; T &gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalArray&lt; T &gt;::LocalArray(std::unique_ptr&lt; T[]&gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalArray&lt; T &gt;::operator std::unique_ptr&lt; T[]&gt;() &amp;&amp; | DraftICU 64 | 
| localpointer.h |  icu::LocalPointer&lt; T &gt;::LocalPointer(std::unique_ptr&lt; T &gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalPointer&lt; T &gt;::operator std::unique_ptr&lt; T &gt;() &amp;&amp; | DraftICU 64 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcre() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcreFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAmpere() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcMinute() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcSecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAstronomicalUnit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAtmosphere() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBarrel() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBritishThermalUnit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBushel() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getByte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCalorie() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCarat() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCelsius() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentiliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentury() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicCentimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicInch() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicKilometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMile() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicYard() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCup() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupMetric() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDalton() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDay() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDayPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDeciliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDegree() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDunam() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthMass() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getElectronvolt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFahrenheit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFathom() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunce() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunceImperial() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoodcalorie() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFurlong() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGForce() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallonImperial() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGenericTemperature() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigahertz() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigawatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGram() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectare() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectoliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectopascal() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHertz() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHorsepower() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHour() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getInch() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getInchHg() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getJoule() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKarat() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKelvin() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilocalorie() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilogram() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilohertz() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilojoule() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometerPerHour() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilopascal() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHour() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKnot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightYear() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPer100Kilometers() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPerKilometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLux() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegahertz() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegaliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapascal() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegawatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecondSquared() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMetricTon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrogram() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrosecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMile() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallonImperial() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerHour() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMileScandinavian() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliampere() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillibar() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligram() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramPerDeciliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeterOfMercury() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimolePerLiter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillisecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliwatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMinute() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMole() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonth() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonthPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanosecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNauticalMile() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewton() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewtonMeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOhm() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunce() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunceTroy() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getParsec() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPerMillion() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPercent() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermille() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermyriad() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPetabyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPicometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPint() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintMetric() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoint() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPound() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundForce() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundPerSquareInch() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuart() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRadian() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRevolutionAngle() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarLuminosity() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarMass() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarRadius() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareCentimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareInch() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareKilometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMile() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareYard() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getStone() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTablespoon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTeaspoon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getVolt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeek() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeekPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYard() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYear() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYearPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBarrel(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnit(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDalton(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDayPerson(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDunam(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthMass(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createElectronvolt(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceImperial(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilopascal(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapascal(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMole(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMonthPerson(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewton(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewtonMeter(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermyriad(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundFoot(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundForce(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarLuminosity(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarMass(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarRadius(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createWeekPerson(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createYearPerson(UErrorCode&amp;) | DraftICU 64 | 
| nounit.h |  UClassID icu::NoUnit::getDynamicClassID() const | DraftICU 60 | 
| nounit.h |  icu::NoUnit::NoUnit(const NoUnit&amp;) | DraftICU 60 | 
| nounit.h |  icu::NoUnit::~NoUnit() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::base() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::percent() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::permille() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> UClassID icu::NoUnit::getStaticClassID() | DraftICU 60 | 
| nounit.h |  void* icu::NoUnit::clone() const | DraftICU 60 | 
| numberformatter.h |  LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; | DraftICU 64 | 
| numberformatter.h |  LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() const &amp; | DraftICU 64 | 
| numberformatter.h |  UBool icu::number::FormattedNumber::nextFieldPosition(FieldPosition&amp;, UErrorCode&amp;) const | DraftICU 62 | 
| numberformatter.h |  UBool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| numberformatter.h |  UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| numberformatter.h |  icu::number::FormattedNumber::FormattedNumber() | DraftICU 64 | 
| numberformatter.h |  <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::forSkeleton(const UnicodeString&amp;, UParseError&amp;, UErrorCode&amp;) | DraftICU 64 | 
| numberformatter.h |  void icu::number::FormattedNumber::getAllFieldPositions(FieldPositionIterator&amp;, UErrorCode&amp;) const | DraftICU 62 | 
| numberrangeformatter.h |  LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; | DraftICU 64 | 
| numberrangeformatter.h |  LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() const &amp; | DraftICU 64 | 
| numberrangeformatter.h |  UBool icu::number::FormattedNumberRange::nextFieldPosition(FieldPosition&amp;, UErrorCode&amp;) const | DraftICU 63 | 
| numberrangeformatter.h |  UBool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| numberrangeformatter.h |  UnicodeString icu::number::FormattedNumberRange::getFirstDecimal(UErrorCode&amp;) const | DraftICU 63 | 
| numberrangeformatter.h |  UnicodeString icu::number::FormattedNumberRange::getSecondDecimal(UErrorCode&amp;) const | DraftICU 63 | 
| numberrangeformatter.h |  UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| numberrangeformatter.h |  void icu::number::FormattedNumberRange::getAllFieldPositions(FieldPositionIterator&amp;, UErrorCode&amp;) const | DraftICU 63 | 
| numfmt.h |  <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kCompactField | DraftICU 64 | 
| numfmt.h |  <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kMeasureUnitField | DraftICU 64 | 
| plurrule.h |  UnicodeString icu::PluralRules::select(const number::FormattedNumber&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  Appendable&amp; icu::FormattedRelativeDateTime::appendTo(Appendable&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatNumericToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(UDateDirection, UDateAbsoluteUnit, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, UDateDirection, UDateRelativeUnit, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  FormattedRelativeDateTime&amp; icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&amp;&amp;) | DraftICU 64 | 
| reldatefmt.h |  UBool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&amp;) const | DraftICU 64 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::FormattedRelativeDateTime() | DraftICU 64 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&amp;&amp;) | DraftICU 64 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() | DraftICU 64 | 
| udateintervalformat.h |  UFormattedDateInterval* udtitvfmt_openResult(UErrorCode*) | DraftICU 64 | 
| udateintervalformat.h |  const UFormattedValue* udtitvfmt_resultAsValue(const UFormattedDateInterval*, UErrorCode*) | DraftICU 64 | 
| udateintervalformat.h |  void udtitvfmt_closeResult(UFormattedDateInterval*) | DraftICU 64 | 
| udateintervalformat.h |  void udtitvfmt_formatToResult(const UDateIntervalFormat*, UFormattedDateInterval*, UDate, UDate, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  UBool ucfpos_matchesField(const UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  UBool ufmtval_nextPosition(const UFormattedValue*, UConstrainedFieldPosition*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  UConstrainedFieldPosition* ucfpos_open(UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  const UChar* ufmtval_getString(const UFormattedValue*, int32_t*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE_INTERVAL_SPAN | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST_SPAN | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_RELATIVE_DATETIME | DraftICU 64 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_UNDEFINED | DraftICU 64 | 
| uformattedvalue.h |  int32_t ucfpos_getCategory(const UConstrainedFieldPosition*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  int32_t ucfpos_getField(const UConstrainedFieldPosition*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  int64_t ucfpos_getInt64IterationContext(const UConstrainedFieldPosition*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_close(UConstrainedFieldPosition*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_constrainCategory(UConstrainedFieldPosition*, int32_t, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_constrainField(UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_getIndexes(const UConstrainedFieldPosition*, int32_t*, int32_t*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_reset(UConstrainedFieldPosition*, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_setInt64IterationContext(UConstrainedFieldPosition*, int64_t, UErrorCode*) | DraftICU 64 | 
| uformattedvalue.h |  void ucfpos_setState(UConstrainedFieldPosition*, int32_t, int32_t, int32_t, int32_t, UErrorCode*) | DraftICU 64 | 
| ulistformatter.h |  UFormattedList* ulistfmt_openResult(UErrorCode*) | DraftICU 64 | 
| ulistformatter.h |  const UFormattedValue* ulistfmt_resultAsValue(const UFormattedList*, UErrorCode*) | DraftICU 64 | 
| ulistformatter.h |  <tt>enum</tt> UListFormatterField::ULISTFMT_ELEMENT_FIELD | DraftICU 63 | 
| ulistformatter.h |  <tt>enum</tt> UListFormatterField::ULISTFMT_LITERAL_FIELD | DraftICU 63 | 
| ulistformatter.h |  void ulistfmt_closeResult(UFormattedList*) | DraftICU 64 | 
| ulistformatter.h |  void ulistfmt_formatStringsToResult(const UListFormatter*, const UChar* const strings[], const int32_t*, int32_t, UFormattedList*, UErrorCode*) | DraftICU 64 | 
| unum.h |  <tt>enum</tt> UNumberFormatAttribute::UNUM_MINIMUM_GROUPING_DIGITS | DraftICU 64 | 
| unum.h |  <tt>enum</tt> UNumberFormatAttribute::UNUM_PARSE_CASE_SENSITIVE | DraftICU 64 | 
| unum.h |  <tt>enum</tt> UNumberFormatAttribute::UNUM_SIGN_ALWAYS_SHOWN | DraftICU 64 | 
| unum.h |  <tt>enum</tt> UNumberFormatFields::UNUM_COMPACT_FIELD | DraftICU 64 | 
| unum.h |  <tt>enum</tt> UNumberFormatFields::UNUM_MEASURE_UNIT_FIELD | DraftICU 64 | 
| unumberformatter.h |  UNumberFormatter* unumf_openForSkeletonAndLocaleWithError(const UChar*, int32_t, const char*, UParseError*, UErrorCode*) | DraftICU 64 | 
| unumberformatter.h |  const UFormattedValue* unumf_resultAsValue(const UFormattedNumber*, UErrorCode*) | DraftICU 64 | 
| upluralrules.h |  int32_t uplrules_selectFormatted(const UPluralRules*, const struct UFormattedNumber*, UChar*, int32_t, UErrorCode*) | DraftICU 64 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| ureldatefmt.h |  UFormattedRelativeDateTime* ureldatefmt_openResult(UErrorCode*) | DraftICU 64 | 
| ureldatefmt.h |  const UFormattedValue* ureldatefmt_resultAsValue(const UFormattedRelativeDateTime*, UErrorCode*) | DraftICU 64 | 
| ureldatefmt.h |  <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_LITERAL_FIELD | DraftICU 64 | 
| ureldatefmt.h |  <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_NUMERIC_FIELD | DraftICU 64 | 
| ureldatefmt.h |  void ureldatefmt_closeResult(UFormattedRelativeDateTime*) | DraftICU 64 | 
| ureldatefmt.h |  void ureldatefmt_formatNumericToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) | DraftICU 64 | 
| ureldatefmt.h |  void ureldatefmt_formatToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) | DraftICU 64 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.

- **`void* icu::ChoiceFormat::clone() const`**
  - `ChoiceFormat* icu::ChoiceFormat::clone() const`
  - `Format* icu::ChoiceFormat::clone() const`
- **`void* icu::CompactDecimalFormat::clone() const`**
  - `CompactDecimalFormat* icu::CompactDecimalFormat::clone() const U_OVERRIDE`
  - `Format* icu::CompactDecimalFormat::clone() const U_OVERRIDE`
- **`void* icu::CurrencyAmount::clone() const`**
  - `CurrencyAmount* icu::CurrencyAmount::clone() const`
  - `UObject* icu::CurrencyAmount::clone() const`
- **`void* icu::CurrencyUnit::clone() const`**
  - `CurrencyUnit* icu::CurrencyUnit::clone() const`
  - `UObject* icu::CurrencyUnit::clone() const`
- **`void* icu::DateIntervalFormat::clone() const`**
  - `DateIntervalFormat* icu::DateIntervalFormat::clone() const`
  - `Format* icu::DateIntervalFormat::clone() const`
- **`void* icu::DecimalFormat::clone() const`**
  - `DecimalFormat* icu::DecimalFormat::clone() const U_OVERRIDE`
  - `Format* icu::DecimalFormat::clone() const U_OVERRIDE`
- **`void* icu::GregorianCalendar::clone() const`**
  - `Calendar* icu::GregorianCalendar::clone() const`
  - `GregorianCalendar* icu::GregorianCalendar::clone() const`
- **`void* icu::Measure::clone() const`**
  - `Measure* icu::Measure::clone() const`
  - `UObject* icu::Measure::clone() const`
- **`void* icu::MeasureFormat::clone() const`**
  - `Format* icu::MeasureFormat::clone() const`
  - `MeasureFormat* icu::MeasureFormat::clone() const`
- **`void* icu::MeasureUnit::clone() const`**
  - `MeasureUnit* icu::MeasureUnit::clone() const`
  - `UObject* icu::MeasureUnit::clone() const`
- **`void* icu::MessageFormat::clone() const`**
  - `Format* icu::MessageFormat::clone() const`
  - `MessageFormat* icu::MessageFormat::clone() const`
- **`void* icu::NoUnit::clone() const`**
  - `NoUnit* icu::NoUnit::clone() const`
  - `UObject* icu::NoUnit::clone() const`
- **`void* icu::PluralFormat::clone() const`**
  - `Format* icu::PluralFormat::clone() const`
  - `PluralFormat* icu::PluralFormat::clone() const`
- **`void* icu::RuleBasedBreakIterator::clone() const`**
  - `BreakIterator* icu::RuleBasedBreakIterator::clone() const`
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::clone() const`
- **`void* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&)`**
  - `BreakIterator* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&)`
  - `RuleBasedBreakIterator* icu::RuleBasedBreakIterator::createBufferClone(void*, int32_t&, UErrorCode&)`
- **`void* icu::RuleBasedCollator::clone() const`**
  - `Collator* icu::RuleBasedCollator::clone() const`
  - `RuleBasedCollator* icu::RuleBasedCollator::clone() const`
- **`void* icu::RuleBasedNumberFormat::clone() const`**
  - `Format* icu::RuleBasedNumberFormat::clone() const`
  - `RuleBasedNumberFormat* icu::RuleBasedNumberFormat::clone() const`
- **`void* icu::RuleBasedTimeZone::clone() const`**
  - `RuleBasedTimeZone* icu::RuleBasedTimeZone::clone() const`
  - `TimeZone* icu::RuleBasedTimeZone::clone() const`
- **`void* icu::SelectFormat::clone() const`**
  - `Format* icu::SelectFormat::clone() const`
  - `SelectFormat* icu::SelectFormat::clone() const`
- **`void* icu::SimpleDateFormat::clone() const`**
  - `Format* icu::SimpleDateFormat::clone() const`
  - `SimpleDateFormat* icu::SimpleDateFormat::clone() const`
- **`void* icu::SimpleTimeZone::clone() const`**
  - `SimpleTimeZone* icu::SimpleTimeZone::clone() const`
  - `TimeZone* icu::SimpleTimeZone::clone() const`
- **`void* icu::StringCharacterIterator::clone() const`**
  - `CharacterIterator* icu::StringCharacterIterator::clone() const`
  - `StringCharacterIterator* icu::StringCharacterIterator::clone() const`
- **`void* icu::StringSearch::safeClone() const`**
  - `SearchIterator* icu::StringSearch::safeClone() const`
  - `StringSearch* icu::StringSearch::safeClone() const`
- **`void* icu::TimeUnit::clone() const`**
  - `TimeUnit* icu::TimeUnit::clone() const`
  - `UObject* icu::TimeUnit::clone() const`
- **`void* icu::TimeUnitAmount::clone() const`**
  - `TimeUnitAmount* icu::TimeUnitAmount::clone() const`
  - `UObject* icu::TimeUnitAmount::clone() const`
- **`void* icu::TimeUnitFormat::clone() const`**
  - `Format* icu::TimeUnitFormat::clone() const`
  - `TimeUnitFormat* icu::TimeUnitFormat::clone() const`
- **`void* icu::TimeZoneFormat::clone() const`**
  - `Format* icu::TimeZoneFormat::clone() const`
  - `TimeZoneFormat* icu::TimeZoneFormat::clone() const`
- **`void* icu::UCharCharacterIterator::clone() const`**
  - `CharacterIterator* icu::UCharCharacterIterator::clone() const`
  - `UCharCharacterIterator* icu::UCharCharacterIterator::clone() const`
- **`void* icu::UnicodeSet::clone() const`**
  - `UnicodeFunctor* icu::UnicodeSet::clone() const`
  - `UnicodeSet* icu::UnicodeSet::clone() const`
- **`void* icu::UnicodeSet::cloneAsThawed() const`**
  - `UnicodeFunctor* icu::UnicodeSet::cloneAsThawed() const`
  - `UnicodeSet* icu::UnicodeSet::cloneAsThawed() const`
- **`void* icu::UnicodeSet::freeze()`**
  - `UnicodeFunctor* icu::UnicodeSet::freeze()`
  - `UnicodeSet* icu::UnicodeSet::freeze()`
- **`void* icu::UnicodeString::clone() const`**
  - `Replaceable* icu::UnicodeString::clone() const`
  - `UnicodeString* icu::UnicodeString::clone() const`
- **`void* icu::VTimeZone::clone() const`**
  - `TimeZone* icu::VTimeZone::clone() const`
  - `VTimeZone* icu::VTimeZone::clone() const`

## Colophon

Contents generated by StableAPI tool on Wed Oct 02 10:22:36 PDT 2019

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  