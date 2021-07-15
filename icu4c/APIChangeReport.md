
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 68 (update #1: 68.2) with ICU 69

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 68](#removed)
- [Deprecated or Obsoleted in ICU 69](#deprecated)
- [Changed in  ICU 69](#changed)
- [Promoted to stable in ICU 69](#promoted)
- [Added in ICU 69](#added)
- [Other existing drafts in ICU 69](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 68
  
| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| listformatter.h | <tt>static</tt> ListFormatter* icu::ListFormatter::createInstance(const Locale&amp;, const char*, UErrorCode&amp;) |  Internal | (missing)
| measunit.h | MeasureUnit icu::MeasureUnit::withSIPrefix(UMeasureSIPrefix, UErrorCode&amp;) const |  DraftICU 67 | (missing)
| measunit.h | UMeasureSIPrefix icu::MeasureUnit::getSIPrefix(UErrorCode&amp;) const |  DraftICU 67 | (missing)
| numberformatter.h | Usage&amp; icu::number::impl::Usage::operator=(Usage&amp;&amp;) |  Internal | (missing)
| numberformatter.h | Usage&amp; icu::number::impl::Usage::operator=(const Usage&amp;) |  Internal | (missing)
| numberformatter.h | bool icu::number::impl::Usage::isSet() const |  Internal | (missing)
| numberformatter.h | icu::number::impl::Usage::Usage(Usage&amp;&amp;) |  Internal | (missing)
| numberformatter.h | icu::number::impl::Usage::Usage(const Usage&amp;) |  Internal | (missing)
| numberformatter.h | icu::number::impl::Usage::~Usage() |  Internal | (missing)
| numberformatter.h | int16_t icu::number::impl::Usage::length() const |  Internal | (missing)
| numberformatter.h | void icu::number::impl::Usage::set(StringPiece) |  Internal | (missing)
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::getFirstDecimal(UErrorCode&amp;) const |  DeprecatedICU 68 | (missing)
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::getSecondDecimal(UErrorCode&amp;) const |  DeprecatedICU 68 | (missing)
| rbtz.h | void icu::RuleBasedTimeZone::getOffsetFromLocal(UDate, int32_t, int32_t, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Internal | (missing)
| simpletz.h | void icu::SimpleTimeZone::getOffsetFromLocal(UDate, int32_t, int32_t, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Internal | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 69
  
| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| ubrk.h | UBreakIterator* ubrk_safeClone(const UBreakIterator*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 69

## Changed

Changed in  ICU 69 (old, new)


  
| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| bytestream.h | void icu::ByteSink::AppendU8(const char*, int32_t) |  Draft→StableICU 67
| bytestream.h | void icu::ByteSink::AppendU8(const char8_t*, int32_t) |  Draft→StableICU 67
| dtptngen.h | UDateFormatHourCycle icu::DateTimePatternGenerator::getDefaultHourCycle(UErrorCode&amp;) const |  Draft→StableICU 67
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setDirection(ULocMatchDirection) |  Draft→StableICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_ONLY_TWO_WAY |  Draft→StableICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_WITH_ONE_WAY |  Draft→StableICU 67
| locid.h | void icu::Locale::canonicalize(UErrorCode&amp;) |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::product(const MeasureUnit&amp;, UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::reciprocal(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::withDimensionality(int32_t, UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit&amp; icu::MeasureUnit::operator=(MeasureUnit&amp;&amp;) noexcept |  Draft→StableICU 67
| measunit.h | UMeasureUnitComplexity icu::MeasureUnit::getComplexity(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | const char* icu::MeasureUnit::getIdentifier() const |  Draft→StableICU 67
| measunit.h | icu::MeasureUnit::MeasureUnit(MeasureUnit&amp;&amp;) noexcept |  Draft→StableICU 67
| measunit.h | int32_t icu::MeasureUnit::getDimensionality(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::forIdentifier(StringPiece, UErrorCode&amp;) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*, int32_t) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const std::u8string&amp;) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(std::nullptr_t) |  Draft→StableICU 67
| stringpiece.h | int32_t icu::StringPiece::compare(StringPiece) |  Draft→StableICU 67
| stringpiece.h | int32_t icu::StringPiece::find(StringPiece, int32_t) |  Draft→StableICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*) |  Draft→StableICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*, int32_t) |  Draft→StableICU 67
| translit.h | UnicodeSet&amp; icu::Transliterator::getSourceSet(UnicodeSet&amp;) const |   _untagged _  | StableICU 2.4
| translit.h | <tt>static</tt> UnicodeString&amp; icu::Transliterator::getDisplayName(const UnicodeString&amp;, UnicodeString&amp;) |   _untagged _  | StableICU 2.0
| translit.h | void icu::Transliterator::finishTransliteration(Replaceable&amp;, UTransPosition&amp;) const |   _untagged _  | StableICU 2.0
| translit.h | void icu::Transliterator::transliterate(Replaceable&amp;, UTransPosition&amp;, UErrorCode&amp;) const |   _untagged _  | StableICU 2.0
| ubrk.h | UBreakIterator* ubrk_safeClone(const UBreakIterator*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 69
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_11 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_12 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_23 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_24 |  Draft→StableICU 67
| udateintervalformat.h | void udtitvfmt_formatCalendarToResult(const UDateIntervalFormat*, UCalendar*, UCalendar*, UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 67
| udateintervalformat.h | void udtitvfmt_formatToResult(const UDateIntervalFormat*, UDate, UDate, UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 67
| udatpg.h | UDateFormatHourCycle udatpg_getDefaultHourCycle(const UDateTimePatternGenerator*, UErrorCode*) |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_BREAK_ENGINE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_CHARACTER |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_LINE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_SENTENCE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_TITLE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_WORD |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_START |  Draft→StableICU 67

## Promoted

Promoted to stable in ICU 69
  
| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| bytestream.h | void icu::ByteSink::AppendU8(const char*, int32_t) |  Draft→StableICU 67
| bytestream.h | void icu::ByteSink::AppendU8(const char8_t*, int32_t) |  Draft→StableICU 67
| dtptngen.h | UDateFormatHourCycle icu::DateTimePatternGenerator::getDefaultHourCycle(UErrorCode&amp;) const |  Draft→StableICU 67
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setDirection(ULocMatchDirection) |  Draft→StableICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_ONLY_TWO_WAY |  Draft→StableICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_WITH_ONE_WAY |  Draft→StableICU 67
| locid.h | void icu::Locale::canonicalize(UErrorCode&amp;) |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::product(const MeasureUnit&amp;, UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::reciprocal(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::withDimensionality(int32_t, UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | MeasureUnit&amp; icu::MeasureUnit::operator=(MeasureUnit&amp;&amp;) noexcept |  Draft→StableICU 67
| measunit.h | UMeasureUnitComplexity icu::MeasureUnit::getComplexity(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | const char* icu::MeasureUnit::getIdentifier() const |  Draft→StableICU 67
| measunit.h | icu::MeasureUnit::MeasureUnit(MeasureUnit&amp;&amp;) noexcept |  Draft→StableICU 67
| measunit.h | int32_t icu::MeasureUnit::getDimensionality(UErrorCode&amp;) const |  Draft→StableICU 67
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::forIdentifier(StringPiece, UErrorCode&amp;) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*, int32_t) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const std::u8string&amp;) |  Draft→StableICU 67
| stringpiece.h | icu::StringPiece::StringPiece(std::nullptr_t) |  Draft→StableICU 67
| stringpiece.h | int32_t icu::StringPiece::compare(StringPiece) |  Draft→StableICU 67
| stringpiece.h | int32_t icu::StringPiece::find(StringPiece, int32_t) |  Draft→StableICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*) |  Draft→StableICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*, int32_t) |  Draft→StableICU 67
| translit.h | UnicodeSet&amp; icu::Transliterator::getSourceSet(UnicodeSet&amp;) const |   _untagged _  | StableICU 2.4
| translit.h | <tt>static</tt> UnicodeString&amp; icu::Transliterator::getDisplayName(const UnicodeString&amp;, UnicodeString&amp;) |   _untagged _  | StableICU 2.0
| translit.h | void icu::Transliterator::finishTransliteration(Replaceable&amp;, UTransPosition&amp;) const |   _untagged _  | StableICU 2.0
| translit.h | void icu::Transliterator::transliterate(Replaceable&amp;, UTransPosition&amp;, UErrorCode&amp;) const |   _untagged _  | StableICU 2.0
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_11 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_12 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_23 |  Draft→StableICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_24 |  Draft→StableICU 67
| udateintervalformat.h | void udtitvfmt_formatCalendarToResult(const UDateIntervalFormat*, UCalendar*, UCalendar*, UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 67
| udateintervalformat.h | void udtitvfmt_formatToResult(const UDateIntervalFormat*, UDate, UDate, UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 67
| udatpg.h | UDateFormatHourCycle udatpg_getDefaultHourCycle(const UDateTimePatternGenerator*, UErrorCode*) |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_BREAK_ENGINE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_CHARACTER |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_LINE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_SENTENCE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_TITLE |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_WORD |  Draft→StableICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_START |  Draft→StableICU 67

## Added

Added in ICU 69
  
| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| basictz.h | void icu::BasicTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 69
| measunit.h | MeasureUnit icu::MeasureUnit::withPrefix(UMeasurePrefix, UErrorCode&amp;) const |  (missing) | DraftICU 69
| measunit.h | UMeasurePrefix icu::MeasureUnit::getPrefix(UErrorCode&amp;) const |  (missing) | DraftICU 69
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramOfglucosePerDeciliter() |  (missing) | DraftICU 69
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramOfglucosePerDeciliter(UErrorCode&amp;) |  (missing) | DraftICU 69
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitDisplayCase(StringPiece) const&amp; |  (missing) | InternalICU 69
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitDisplayCase(StringPiece)&amp;&amp; |  (missing) | InternalICU 69
| numberformatter.h | Precision icu::number::FractionPrecision::withSignificantDigits(int32_t, int32_t, UNumberRoundingPriority) const |  (missing) | DraftICU 69
| numberformatter.h | Precision icu::number::Precision::trailingZeroDisplay(UNumberTrailingZeroDisplay) const |  (missing) | DraftICU 69
| numberformatter.h | StringProp&amp; icu::number::impl::StringProp::operator=(StringProp&amp;&amp;) |  (missing) | Internal
| numberformatter.h | StringProp&amp; icu::number::impl::StringProp::operator=(const StringProp&amp;) |  (missing) | Internal
| numberformatter.h | bool icu::number::impl::StringProp::isSet() const |  (missing) | Internal
| numberformatter.h | const char* icu::number::FormattedNumber::getGender(UErrorCode&amp;) const |  (missing) | InternalICU 69
| numberformatter.h | icu::number::impl::StringProp::StringProp(StringProp&amp;&amp;) |  (missing) | Internal
| numberformatter.h | icu::number::impl::StringProp::StringProp(const StringProp&amp;) |  (missing) | Internal
| numberformatter.h | icu::number::impl::StringProp::~StringProp() |  (missing) | Internal
| numberformatter.h | int16_t icu::number::impl::StringProp::length() const |  (missing) | Internal
| numberformatter.h | void icu::number::impl::StringProp::set(StringPiece) |  (missing) | Internal
| rbtz.h | void icu::RuleBasedTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 69
| simpletz.h | void icu::SimpleTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 69
| ubrk.h | UBreakIterator* ubrk_clone(const UBreakIterator*, UErrorCode*) |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_FORMER |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_LATTER |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_FORMER |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_LATTER |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_FORMER |  (missing) | DraftICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_LATTER |  (missing) | DraftICU 69
| ucal.h | void ucal_getTimeZoneOffsetFromLocal(const UCalendar*, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t*, int32_t*, UErrorCode*) |  (missing) | DraftICU 69
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER_RANGE_SPAN |  (missing) | DraftICU 69
| uniset.h | UnicodeSet&amp; icu::UnicodeSet::retain(const UnicodeString&amp;) |  (missing) | DraftICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_CEILING |  (missing) | DraftICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_FLOOR |  (missing) | DraftICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_ODD |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_RELAXED |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_STRICT |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_NEGATIVE |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEGATIVE |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_AUTO |  (missing) | DraftICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_HIDE_IF_WHOLE |  (missing) | DraftICU 69
| uset.h | void uset_complementAllCodePoints(USet*, const UChar*, int32_t) |  (missing) | DraftICU 69
| uset.h | void uset_complementRange(USet*, UChar32, UChar32) |  (missing) | DraftICU 69
| uset.h | void uset_complementString(USet*, const UChar*, int32_t) |  (missing) | DraftICU 69
| uset.h | void uset_removeAllCodePoints(USet*, const UChar*, int32_t) |  (missing) | DraftICU 69
| uset.h | void uset_retainAllCodePoints(USet*, const UChar*, int32_t) |  (missing) | DraftICU 69
| uset.h | void uset_retainString(USet*, const UChar*, int32_t) |  (missing) | DraftICU 69
| vtzone.h | void icu::VTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 69

## Other

Other existing drafts in ICU 69

| File | API | ICU 68 | ICU 69 |
|---|---|---|---|
| dtitvfmt.h |  UDisplayContext icu::DateIntervalFormat::getContext(UDisplayContextType, UErrorCode&amp;) const | DraftICU 68 | 
| dtitvfmt.h |  void icu::DateIntervalFormat::setContext(UDisplayContext, UErrorCode&amp;) | DraftICU 68 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setMaxDistance(const Locale&amp;, const Locale&amp;) | DraftICU 68 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setNoDefaultLocale() | DraftICU 68 | 
| localematcher.h |  UBool icu::LocaleMatcher::isMatch(const Locale&amp;, const Locale&amp;, UErrorCode&amp;) const | DraftICU 68 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCandela() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoon() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDessertSpoonImperial() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDot() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDram() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDrop() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthRadius() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGrain() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getJigger() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLumen() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPinch() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuartImperial() | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCandela(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoon(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDessertSpoonImperial(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDot(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDram(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDrop(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthRadius(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGrain(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createJigger(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLumen(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPinch(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuartImperial(UErrorCode&amp;) | DraftICU 68 | 
| measunit.h |  std::pair&lt; LocalArray&lt; MeasureUnit &gt;, int32_t &gt; icu::MeasureUnit::splitToSingleUnits(UErrorCode&amp;) const | DraftICU 68 | 
| numberformatter.h |  Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece) const&amp; | DraftICU 68 | 
| numberformatter.h |  Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::usage(StringPiece)&amp;&amp; | DraftICU 68 | 
| numberformatter.h |  MeasureUnit icu::number::FormattedNumber::getOutputUnit(UErrorCode&amp;) const | DraftICU 68 | 
| numberrangeformatter.h |  std::pair&lt; StringClass, StringClass &gt; icu::number::FormattedNumberRange::getDecimalNumbers(UErrorCode&amp;) const | DraftICU 68 | 
| plurrule.h |  UnicodeString icu::PluralRules::select(const number::FormattedNumberRange&amp;, UErrorCode&amp;) const | DraftICU 68 | 
| ucurr.h |  <tt>enum</tt> UCurrNameStyle::UCURR_FORMAL_SYMBOL_NAME | DraftICU 68 | 
| ucurr.h |  <tt>enum</tt> UCurrNameStyle::UCURR_VARIANT_SYMBOL_NAME | DraftICU 68 | 
| udateintervalformat.h |  UDisplayContext udtitvfmt_getContext(const UDateIntervalFormat*, UDisplayContextType, UErrorCode*) | DraftICU 68 | 
| udateintervalformat.h |  void udtitvfmt_setContext(UDateIntervalFormat*, UDisplayContext, UErrorCode*) | DraftICU 68 | 
| unum.h |  <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_AUTO | DraftICU 68 | 
| unum.h |  <tt>enum</tt> UNumberFormatMinimumGroupingDigits::UNUM_MINIMUM_GROUPING_DIGITS_MIN2 | DraftICU 68 | 
| unumberformatter.h |  <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_FORMAL | DraftICU 68 | 
| unumberformatter.h |  <tt>enum</tt> UNumberUnitWidth::UNUM_UNIT_WIDTH_VARIANT | DraftICU 68 | 
| unumberformatter.h |  int32_t unumf_resultToDecimalNumber(const UFormattedNumber*, char*, int32_t, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  UFormattedNumberRange* unumrf_openResult(UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  UNumberRangeFormatter* unumrf_openForSkeletonWithCollapseAndIdentityFallback(const UChar*, int32_t, UNumberRangeCollapse, UNumberRangeIdentityFallback, const char*, UParseError*, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  UNumberRangeIdentityResult unumrf_resultGetIdentityResult(const UFormattedNumberRange*, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  const UFormattedValue* unumrf_resultAsValue(const UFormattedNumberRange*, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  int32_t unumrf_resultGetFirstDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  int32_t unumrf_resultGetSecondDecimalNumber(const UFormattedNumberRange*, char*, int32_t, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  void unumrf_close(UNumberRangeFormatter*) | DraftICU 68 | 
| unumberrangeformatter.h |  void unumrf_closeResult(UFormattedNumberRange*) | DraftICU 68 | 
| unumberrangeformatter.h |  void unumrf_formatDecimalRange(const UNumberRangeFormatter*, const char*, int32_t, const char*, int32_t, UFormattedNumberRange*, UErrorCode*) | DraftICU 68 | 
| unumberrangeformatter.h |  void unumrf_formatDoubleRange(const UNumberRangeFormatter*, double, double, UFormattedNumberRange*, UErrorCode*) | DraftICU 68 | 
| upluralrules.h |  int32_t uplrules_selectForRange(const UPluralRules*, const struct UFormattedNumberRange*, UChar*, int32_t, UErrorCode*) | DraftICU 68 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Thu Mar 11 16:09:31 PST 2021

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  