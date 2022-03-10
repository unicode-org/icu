
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 70 with ICU 71

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 70](#removed)
- [Deprecated or Obsoleted in ICU 71](#deprecated)
- [Changed in  ICU 71](#changed)
- [Promoted to stable in ICU 71](#promoted)
- [Added in ICU 71](#added)
- [Other existing drafts in ICU 71](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 70
  
| File | API | ICU 70 | ICU 71 |
|---|---|---|---|

## Deprecated

Deprecated or Obsoleted in ICU 71
  
| File | API | ICU 70 | ICU 71 |
|---|---|---|---|
| numberformatter.h | const char* icu::number::FormattedNumber::getGender(UErrorCode&amp;) const |  InternalICU 69 | Deprecated
| ucnv.h | UConverter* ucnv_safeClone(const UConverter*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 71
| ucol.h | UCollator* ucol_safeClone(const UCollator*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 71

## Changed

Changed in  ICU 71 (old, new)


  
| File | API | ICU 70 | ICU 71 |
|---|---|---|---|
| basictz.h | void icu::BasicTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | MeasureUnit icu::MeasureUnit::withPrefix(UMeasurePrefix, UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | UMeasurePrefix icu::MeasureUnit::getPrefix(UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramOfglucosePerDeciliter() |  Draft→StableICU 69
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramOfglucosePerDeciliter(UErrorCode&amp;) |  Draft→StableICU 69
| numberformatter.h | Precision icu::number::FractionPrecision::withSignificantDigits(int32_t, int32_t, UNumberRoundingPriority) const |  Draft→StableICU 69
| numberformatter.h | Precision icu::number::Precision::trailingZeroDisplay(UNumberTrailingZeroDisplay) const |  Draft→StableICU 69
| numberformatter.h | const char* icu::number::FormattedNumber::getGender(UErrorCode&amp;) const |  InternalICU 69 | Deprecated
| rbtz.h | void icu::RuleBasedTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| simpletz.h | void icu::SimpleTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| ubrk.h | UBreakIterator* ubrk_clone(const UBreakIterator*, UErrorCode*) |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_LATTER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_LATTER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_LATTER |  Draft→StableICU 69
| ucal.h | void ucal_getTimeZoneOffsetFromLocal(const UCalendar*, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t*, int32_t*, UErrorCode*) |  Draft→StableICU 69
| ucnv.h | UConverter* ucnv_safeClone(const UConverter*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 71
| ucol.h | UCollator* ucol_safeClone(const UCollator*, void*, int32_t*, UErrorCode*) |  StableICU 2.0 | DeprecatedICU 71
| uniset.h | UnicodeSet&amp; icu::UnicodeSet::retain(const UnicodeString&amp;) |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_CEILING |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_FLOOR |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_ODD |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_RELAXED |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_STRICT |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_NEGATIVE |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEGATIVE |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_AUTO |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_HIDE_IF_WHOLE |  Draft→StableICU 69
| uset.h | void uset_complementAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_complementRange(USet*, UChar32, UChar32) |  Draft→StableICU 69
| uset.h | void uset_complementString(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_removeAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_retainAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_retainString(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| vtzone.h | void icu::VTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69

## Promoted

Promoted to stable in ICU 71
  
| File | API | ICU 70 | ICU 71 |
|---|---|---|---|
| basictz.h | void icu::BasicTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | MeasureUnit icu::MeasureUnit::withPrefix(UMeasurePrefix, UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | UMeasurePrefix icu::MeasureUnit::getPrefix(UErrorCode&amp;) const |  Draft→StableICU 69
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramOfglucosePerDeciliter() |  Draft→StableICU 69
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramOfglucosePerDeciliter(UErrorCode&amp;) |  Draft→StableICU 69
| numberformatter.h | Precision icu::number::FractionPrecision::withSignificantDigits(int32_t, int32_t, UNumberRoundingPriority) const |  Draft→StableICU 69
| numberformatter.h | Precision icu::number::Precision::trailingZeroDisplay(UNumberTrailingZeroDisplay) const |  Draft→StableICU 69
| rbtz.h | void icu::RuleBasedTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| simpletz.h | void icu::SimpleTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69
| ubrk.h | UBreakIterator* ubrk_clone(const UBreakIterator*, UErrorCode*) |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_DAYLIGHT_LATTER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_LATTER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_FORMER |  Draft→StableICU 69
| ucal.h | <tt>enum</tt> UTimeZoneLocalOption::UCAL_TZ_LOCAL_STANDARD_LATTER |  Draft→StableICU 69
| ucal.h | void ucal_getTimeZoneOffsetFromLocal(const UCalendar*, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t*, int32_t*, UErrorCode*) |  Draft→StableICU 69
| ucnv.h | UConverter* ucnv_clone(const UConverter*, UErrorCode*) |  (missing) | StableICU 71| *(Born Stable)* |
| ucol.h | UCollator* ucol_clone(const UCollator*, UErrorCode*) |  (missing) | StableICU 71| *(Born Stable)* |
| uniset.h | UnicodeSet&amp; icu::UnicodeSet::retain(const UnicodeString&amp;) |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_CEILING |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_FLOOR |  Draft→StableICU 69
| unum.h | <tt>enum</tt> UNumberFormatRoundingMode::UNUM_ROUND_HALF_ODD |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_RELAXED |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberRoundingPriority::UNUM_ROUNDING_PRIORITY_STRICT |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_NEGATIVE |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberSignDisplay::UNUM_SIGN_NEGATIVE |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_AUTO |  Draft→StableICU 69
| unumberformatter.h | <tt>enum</tt> UNumberTrailingZeroDisplay::UNUM_TRAILING_ZERO_HIDE_IF_WHOLE |  Draft→StableICU 69
| uset.h | void uset_complementAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_complementRange(USet*, UChar32, UChar32) |  Draft→StableICU 69
| uset.h | void uset_complementString(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_removeAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_retainAllCodePoints(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| uset.h | void uset_retainString(USet*, const UChar*, int32_t) |  Draft→StableICU 69
| vtzone.h | void icu::VTimeZone::getOffsetFromLocal(UDate, UTimeZoneLocalOption, UTimeZoneLocalOption, int32_t&amp;, int32_t&amp;, UErrorCode&amp;) const |  Draft→StableICU 69

## Added

Added in ICU 71
  
| File | API | ICU 70 | ICU 71 |
|---|---|---|---|
| dtptngen.h | const UnicodeString&amp; icu::DateTimePatternGenerator::getDateTimeFormat(UDateFormatStyle, UErrorCode&amp;) const |  (missing) | DraftICU 71
| dtptngen.h | void icu::DateTimePatternGenerator::setDateTimeFormat(UDateFormatStyle, const UnicodeString&amp;, UErrorCode&amp;) |  (missing) | DraftICU 71
| numberformatter.h | NounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const |  (missing) | DraftICU 71
| numberformatter.h | <tt>static</tt> IncrementPrecision icu::number::Precision::incrementExact(uint64_t, int16_t) |  (missing) | DraftICU 71
| ucnv.h | UConverter* ucnv_clone(const UConverter*, UErrorCode*) |  (missing) | StableICU 71| *(Born Stable)* |
| ucol.h | UCollator* ucol_clone(const UCollator*, UErrorCode*) |  (missing) | StableICU 71| *(Born Stable)* |
| udatpg.h | const UChar* udatpg_getDateTimeFormatForStyle(const UDateTimePatternGenerator*, UDateFormatStyle, int32_t*, UErrorCode*) |  (missing) | DraftICU 71
| udatpg.h | void udatpg_setDateTimeFormatForStyle(UDateTimePatternGenerator*, UDateFormatStyle, const UChar*, int32_t, UErrorCode*) |  (missing) | DraftICU 71
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_APPROXIMATELY_SIGN_FIELD |  (missing) | DraftICU 71

## Other

Other existing drafts in ICU 71

| File | API | ICU 70 | ICU 71 |
|---|---|---|---|
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getItem() | DraftICU 70 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHourPer100Kilometer() | DraftICU 70 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createItem(UErrorCode&amp;) | DraftICU 70 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilowattHourPer100Kilometer(UErrorCode&amp;) | DraftICU 70 | 
| numberrangeformatter.h |  icu::number::FormattedNumberRange::FormattedNumberRange() | DraftICU 70 | 
| uchar.h |  bool u_stringHasBinaryProperty(const UChar*, int32_t, UProperty) | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_BASIC_EMOJI | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_EMOJI_KEYCAP_SEQUENCE | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_FLAG_SEQUENCE | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_MODIFIER_SEQUENCE | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_TAG_SEQUENCE | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_ZWJ_SEQUENCE | DraftICU 70 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI | DraftICU 70 | 
| udat.h |  <tt>enum</tt> UDateFormatSymbolType::UDAT_NARROW_QUARTERS | DraftICU 70 | 
| udat.h |  <tt>enum</tt> UDateFormatSymbolType::UDAT_STANDALONE_NARROW_QUARTERS | DraftICU 70 | 
| uformattedvalue.h |  <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER_RANGE_SPAN | DraftICU 69 | 
| uniset.h |  bool icu::UnicodeSet::hasStrings() const | DraftICU 70 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| uset.h |  bool uset_hasStrings(const USet*) | DraftICU 70 | 
| uset.h |  int32_t uset_getRangeCount(const USet*) | DraftICU 70 | 
| usetiter.h |  UnicodeSetIterator&amp; icu::UnicodeSetIterator::skipToStrings() | DraftICU 70 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Thu Mar 10 11:13:13 PST 2022

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  