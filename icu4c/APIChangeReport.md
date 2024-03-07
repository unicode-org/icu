
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 74 with ICU 75

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 74](#removed)
- [Deprecated or Obsoleted in ICU 75](#deprecated)
- [Changed in  ICU 75](#changed)
- [Promoted to stable in ICU 75](#promoted)
- [Added in ICU 75](#added)
- [Other existing drafts in ICU 75](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 74
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| caniter.h | <tt>static</tt> void icu::CanonicalIterator::permute(UnicodeString&amp;, bool, Hashtable*, UErrorCode&amp;) |  Internal | (missing)
| platform.h | <tt>#define</tt> U_HAVE_INTTYPES_H |  Internal | (missing)
| platform.h | <tt>#define</tt> U_HAVE_STDINT_H |  Internal | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 75
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|

## Changed

Changed in  ICU 75 (old, new)


  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| calendar.h | bool icu::Calendar::inTemporalLeapYear(UErrorCode&amp;) const |  Draft→StableICU 73
| calendar.h | const char* icu::Calendar::getTemporalMonthCode(UErrorCode&amp;) const |  Draft→StableICU 73
| calendar.h | void icu::Calendar::setTemporalMonthCode(const char*, UErrorCode&amp;) |  Draft→StableICU 73
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBeaufort() |  Draft→StableICU 73
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBeaufort(UErrorCode&amp;) |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfCeiling |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfFloor |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfOdd |  Draft→StableICU 73
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::format(SimpleNumber, UErrorCode&amp;) const |  Draft→StableICU 73
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::formatInt64(int64_t, UErrorCode&amp;) const |  Draft→StableICU 73
| simplenumberformatter.h | SimpleNumber&amp; icu::number::SimpleNumber::operator=(SimpleNumber&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | SimpleNumberFormatter&amp; icu::number::SimpleNumberFormatter::operator=(SimpleNumberFormatter&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber()=default |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber(SimpleNumber&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::~SimpleNumber() |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter()=default |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter(SimpleNumberFormatter&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::~SimpleNumberFormatter() |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumber icu::number::SimpleNumber::forInt64(int64_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocale(const icu::Locale&amp;, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndGroupingStrategy(const icu::Locale&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndSymbolsAndGroupingStrategy(const icu::Locale&amp;, const DecimalFormatSymbols&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::multiplyByPowerOfTen(int32_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setSign(USimpleNumberSign, UErrorCode&amp;) |  Draft→StableICU 73
| ucal.h | <tt>enum</tt> UCalendarDateFields::UCAL_ORDINAL_MONTH |  Draft→StableICU 73
| uset.h | <tt>enum</tt> ::USET_SIMPLE_CASE_INSENSITIVE |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumber* usnum_openForInt64(int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocale(const char*, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocaleAndGroupingStrategy(const char*, UNumberGroupingStrategy, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_MINUS_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_NO_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_PLUS_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_close(USimpleNumber*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_multiplyByPowerOfTen(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_close(USimpleNumberFormatter*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_format(const USimpleNumberFormatter*, USimpleNumber*, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_formatInt64(const USimpleNumberFormatter*, int64_t, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73

## Promoted

Promoted to stable in ICU 75
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| calendar.h | bool icu::Calendar::inTemporalLeapYear(UErrorCode&amp;) const |  Draft→StableICU 73
| calendar.h | const char* icu::Calendar::getTemporalMonthCode(UErrorCode&amp;) const |  Draft→StableICU 73
| calendar.h | void icu::Calendar::setTemporalMonthCode(const char*, UErrorCode&amp;) |  Draft→StableICU 73
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBeaufort() |  Draft→StableICU 73
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBeaufort(UErrorCode&amp;) |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfCeiling |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfFloor |  Draft→StableICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfOdd |  Draft→StableICU 73
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::format(SimpleNumber, UErrorCode&amp;) const |  Draft→StableICU 73
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::formatInt64(int64_t, UErrorCode&amp;) const |  Draft→StableICU 73
| simplenumberformatter.h | SimpleNumber&amp; icu::number::SimpleNumber::operator=(SimpleNumber&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | SimpleNumberFormatter&amp; icu::number::SimpleNumberFormatter::operator=(SimpleNumberFormatter&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber()=default |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber(SimpleNumber&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::~SimpleNumber() |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter()=default |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter(SimpleNumberFormatter&amp;&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::~SimpleNumberFormatter() |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumber icu::number::SimpleNumber::forInt64(int64_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocale(const icu::Locale&amp;, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndGroupingStrategy(const icu::Locale&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndSymbolsAndGroupingStrategy(const icu::Locale&amp;, const DecimalFormatSymbols&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::multiplyByPowerOfTen(int32_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setSign(USimpleNumberSign, UErrorCode&amp;) |  Draft→StableICU 73
| ucal.h | <tt>enum</tt> UCalendarDateFields::UCAL_ORDINAL_MONTH |  Draft→StableICU 73
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_ARABIC_NASTALIQ |  (missing) | StableICU 75| *(Born Stable)* |
| uset.h | <tt>enum</tt> ::USET_SIMPLE_CASE_INSENSITIVE |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumber* usnum_openForInt64(int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocale(const char*, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocaleAndGroupingStrategy(const char*, UNumberGroupingStrategy, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_MINUS_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_NO_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_PLUS_SIGN |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_close(USimpleNumber*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_multiplyByPowerOfTen(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_close(USimpleNumberFormatter*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_format(const USimpleNumberFormatter*, USimpleNumber*, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_formatInt64(const USimpleNumberFormatter*, int64_t, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73

## Added

Added in ICU 75
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| caniter.h | <tt>static</tt> void icu::CanonicalIterator::permute(UnicodeString&amp;, bool, Hashtable*, UErrorCode&amp;, int32_t depth=) |  (missing) | Internal
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() &amp;&amp; |  (missing) | DraftICU 75
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() const &amp; |  (missing) | DraftICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() &amp;&amp; |  (missing) | DraftICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() const &amp; |  (missing) | DraftICU 75
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_ARABIC_NASTALIQ |  (missing) | StableICU 75| *(Born Stable)* |

## Other

Other existing drafts in ICU 75

| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGasolineEnergyDensity() | DraftICU 74 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGasolineEnergyDensity(UErrorCode&amp;) | DraftICU 74 | 
| measure.h |  bool icu::Measure::operator!=(const UObject&amp;) const | DraftICU 74 | 
| normalizer2.h |  <tt>static</tt> const Normalizer2* icu::Normalizer2::getNFKCSimpleCasefoldInstance(UErrorCode&amp;) | DraftICU 74 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::roundTo(int32_t, UNumberFormatRoundingMode, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setMinimumFractionDigits(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setMinimumIntegerDigits(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| timezone.h |  <tt>static</tt> UnicodeString&amp; icu::TimeZone::getIanaID(const UnicodeString&amp;, UnicodeString&amp;, UErrorCode&amp;) | DraftICU 74 | 
| ucal.h |  int32_t ucal_getIanaTimeZoneID(const UChar*, int32_t, UChar*, int32_t, UErrorCode*) | DraftICU 74 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_IDS_UNARY_OPERATOR | DraftICU 74 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_CONTINUE | DraftICU 74 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_START | DraftICU 74 | 
| ulocale.h |  UEnumeration* ulocale_getKeywords(const ULocale*, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  UEnumeration* ulocale_getUnicodeKeywords(const ULocale*, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  ULocale* ulocale_openForLanguageTag(const char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  ULocale* ulocale_openForLocaleID(const char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  bool ulocale_isBogus(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getBaseName(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getLanguage(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getLocaleID(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getRegion(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getScript(const ULocale*) | DraftICU 74 | 
| ulocale.h |  const char* ulocale_getVariant(const ULocale*) | DraftICU 74 | 
| ulocale.h |  int32_t ulocale_getKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  int32_t ulocale_getUnicodeKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocale.h |  void ulocale_close(ULocale*) | DraftICU 74 | 
| ulocbuilder.h |  ULocale* ulocbld_buildULocale(ULocaleBuilder*, UErrorCode*) | DraftICU 74 | 
| ulocbuilder.h |  ULocaleBuilder* ulocbld_open() | DraftICU 74 | 
| ulocbuilder.h |  bool ulocbld_copyErrorTo(const ULocaleBuilder*, UErrorCode*) | DraftICU 74 | 
| ulocbuilder.h |  int32_t ulocbld_buildLanguageTag(ULocaleBuilder*, char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocbuilder.h |  int32_t ulocbld_buildLocaleID(ULocaleBuilder*, char*, int32_t, UErrorCode*) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_addUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_adoptULocale(ULocaleBuilder*, ULocale*) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_clear(ULocaleBuilder*) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_clearExtensions(ULocaleBuilder*) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_close(ULocaleBuilder*) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_removeUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setExtension(ULocaleBuilder*, char, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setLanguage(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setLanguageTag(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setLocale(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setRegion(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setScript(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setUnicodeLocaleKeyword(ULocaleBuilder*, const char*, int32_t, const char*, int32_t) | DraftICU 74 | 
| ulocbuilder.h |  void ulocbld_setVariant(ULocaleBuilder*, const char*, int32_t) | DraftICU 74 | 
| unorm2.h |  const UNormalizer2* unorm2_getNFKCSimpleCasefoldInstance(UErrorCode*) | DraftICU 74 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| usimplenumberformatter.h |  void usnum_roundTo(USimpleNumber*, int32_t, UNumberFormatRoundingMode, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setMinimumFractionDigits(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setMinimumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| uspoof.h |  icu::UnicodeString&amp; uspoof_getBidiSkeletonUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, icu::UnicodeString&amp;, UErrorCode*) | DraftICU 74 | 
| uspoof.h |  int32_t uspoof_getBidiSkeleton(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, UChar*, int32_t, UErrorCode*) | DraftICU 74 | 
| uspoof.h |  int32_t uspoof_getBidiSkeletonUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, char*, int32_t, UErrorCode*) | DraftICU 74 | 
| uspoof.h |  uint32_t uspoof_areBidiConfusable(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, const UChar*, int32_t, UErrorCode*) | DraftICU 74 | 
| uspoof.h |  uint32_t uspoof_areBidiConfusableUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, const char*, int32_t, UErrorCode*) | DraftICU 74 | 
| uspoof.h |  uint32_t uspoof_areBidiConfusableUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, const icu::UnicodeString&amp;, UErrorCode*) | DraftICU 74 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Thu Mar 07 10:40:45 PST 2024

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  