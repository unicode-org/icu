
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 71 with ICU 72

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 71](#removed)
- [Deprecated or Obsoleted in ICU 72](#deprecated)
- [Changed in  ICU 72](#changed)
- [Promoted to stable in ICU 72](#promoted)
- [Added in ICU 72](#added)
- [Other existing drafts in ICU 72](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 71
  
| File | API | ICU 71 | ICU 72 |
|---|---|---|---|
| calendar.h | bool icu::Calendar::isSet(EDateFields) const |  DeprecatedICU 2.6 | (missing)
| calendar.h | int32_t icu::Calendar::get(EDateFields, UErrorCode&amp;) const |  DeprecatedICU 2.6 | (missing)
| calendar.h | int32_t icu::Calendar::getActualMaximum(EDateFields, UErrorCode&amp;) const |  DeprecatedICU 2.6 | (missing)
| calendar.h | void icu::Calendar::clear(EDateFields) |  DeprecatedICU 2.6 | (missing)
| calendar.h | void icu::Calendar::set(EDateFields, int32_t) |  DeprecatedICU 2.6 | (missing)
| calendar.h | void icu::Calendar::setFirstDayOfWeek(EDaysOfWeek) |  DeprecatedICU 2.6 | (missing)
| dtptngen.h | <tt>static</tt> DateTimePatternGenerator* icu::DateTimePatternGenerator::internalMakeInstance(const Locale&amp;, UErrorCode&amp;) |  Internal | (missing)
| gregocal.h | int32_t icu::GregorianCalendar::getActualMaximum(EDateFields) const |  DeprecatedICU 2.6 | (missing)
| numberformatter.h | NounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const |  DraftICU 71 | (missing)
| numberformatter.h | const char* icu::number::FormattedNumber::getGender(UErrorCode&amp;) const |  Deprecated | (missing)
| plurrule.h | int32_t icu::PluralRules::getSamples(const UnicodeString&amp;, FixedDecimal*, int32_t, UErrorCode&amp;) |  Internal | (missing)
| plurrule.h | <tt>static</tt> bool icu::PluralRules::hasOverride(const Locale&amp;) |  Internal | (missing)
| tzrule.h | bool icu::InitialTimeZoneRule::getStartInYear(int32_t, int32_t, int32_t, UDate&amp;) const |  StableICU 3.8 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 72
  
| File | API | ICU 71 | ICU 72 |
|---|---|---|---|

## Changed

Changed in  ICU 72 (old, new)


  
| File | API | ICU 71 | ICU 72 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getItem() |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHourPer100Kilometer() |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createItem(UErrorCode&amp;) |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilowattHourPer100Kilometer(UErrorCode&amp;) |  Draft→StableICU 70
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitDisplayCase(StringPiece) const&amp; |  InternalICU 69 | Internal
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::unitDisplayCase(StringPiece)&amp;&amp; |  InternalICU 69 | Internal
| numberrangeformatter.h | icu::number::FormattedNumberRange::FormattedNumberRange() |  Draft→StableICU 70
| uchar.h | bool u_stringHasBinaryProperty(const UChar*, int32_t, UProperty) |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_BASIC_EMOJI |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_EMOJI_KEYCAP_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_FLAG_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_MODIFIER_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_TAG_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_ZWJ_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI |  Draft→StableICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_NARROW_QUARTERS |  Draft→StableICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_STANDALONE_NARROW_QUARTERS |  Draft→StableICU 70
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER_RANGE_SPAN |  Draft→StableICU 69
| uniset.h | bool icu::UnicodeSet::hasStrings() const |  Draft→StableICU 70
| uset.h | bool uset_hasStrings(const USet*) |  Draft→StableICU 70
| uset.h | int32_t uset_getRangeCount(const USet*) |  Draft→StableICU 70
| usetiter.h | UnicodeSetIterator &amp; icu::UnicodeSetIterator::skipToStrings() |  Draft→StableICU 70

## Promoted

Promoted to stable in ICU 72
  
| File | API | ICU 71 | ICU 72 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getItem() |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHourPer100Kilometer() |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createItem(UErrorCode&amp;) |  Draft→StableICU 70
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilowattHourPer100Kilometer(UErrorCode&amp;) |  Draft→StableICU 70
| numberrangeformatter.h | icu::number::FormattedNumberRange::FormattedNumberRange() |  Draft→StableICU 70
| uchar.h | bool u_stringHasBinaryProperty(const UChar*, int32_t, UProperty) |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARABIC_EXTENDED_C |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CYRILLIC_EXTENDED_D |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_DEVANAGARI_EXTENDED_A |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KAKTOVIK_NUMERALS |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KAWI |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_NAG_MUNDARI |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_BASIC_EMOJI |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_EMOJI_KEYCAP_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_FLAG_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_MODIFIER_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_TAG_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI_ZWJ_SEQUENCE |  Draft→StableICU 70
| uchar.h | <tt>enum</tt> UProperty::UCHAR_RGI_EMOJI |  Draft→StableICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_NARROW_QUARTERS |  Draft→StableICU 70
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_STANDALONE_NARROW_QUARTERS |  Draft→StableICU 70
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER_RANGE_SPAN |  Draft→StableICU 69
| uniset.h | bool icu::UnicodeSet::hasStrings() const |  Draft→StableICU 70
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KAWI |  (missing) | StableICU 72| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_NAG_MUNDARI |  (missing) | StableICU 72| *(Born Stable)* |
| uset.h | bool uset_hasStrings(const USet*) |  Draft→StableICU 70
| uset.h | int32_t uset_getRangeCount(const USet*) |  Draft→StableICU 70
| usetiter.h | UnicodeSetIterator &amp; icu::UnicodeSetIterator::skipToStrings() |  Draft→StableICU 70

## Added

Added in ICU 72
  
| File | API | ICU 71 | ICU 72 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuarter() |  (missing) | DraftICU 72
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTonne() |  (missing) | DraftICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuarter(UErrorCode&amp;) |  (missing) | DraftICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTonne(UErrorCode&amp;) |  (missing) | DraftICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;) const&amp; |  (missing) | DraftICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;)&amp;&amp; |  (missing) | DraftICU 72
| numberformatter.h | UDisplayOptionsNounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const |  (missing) | DraftICU 72
| platform.h | <tt>#define</tt> U_HIDDEN |  (missing) | Internal
| plurrule.h | int32_t icu::PluralRules::getSamples(const UnicodeString&amp;, DecimalQuantity*, int32_t, UErrorCode&amp;) |  (missing) | Internal
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARABIC_EXTENDED_C |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CYRILLIC_EXTENDED_D |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_DEVANAGARI_EXTENDED_A |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KAKTOVIK_NUMERALS |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KAWI |  (missing) | StableICU 72| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_NAG_MUNDARI |  (missing) | StableICU 72| *(Born Stable)* |
| udisplayoptions.h | UDisplayOptionsGrammaticalCase udispopt_fromGrammaticalCaseIdentifier(const char*) |  (missing) | DraftICU 72
| udisplayoptions.h | UDisplayOptionsNounClass udispopt_fromNounClassIdentifier(const char*) |  (missing) | DraftICU 72
| udisplayoptions.h | UDisplayOptionsPluralCategory udispopt_fromPluralCategoryIdentifier(const char*) |  (missing) | DraftICU 72
| udisplayoptions.h | const char* udispopt_getGrammaticalCaseIdentifier(UDisplayOptionsGrammaticalCase) |  (missing) | DraftICU 72
| udisplayoptions.h | const char* udispopt_getNounClassIdentifier(UDisplayOptionsNounClass) |  (missing) | DraftICU 72
| udisplayoptions.h | const char* udispopt_getPluralCategoryIdentifier(UDisplayOptionsPluralCategory) |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_BEGINNING_OF_SENTENCE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_MIDDLE_OF_SENTENCE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_STANDALONE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UI_LIST_OR_MENU |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_FULL |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_SHORT |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ABLATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ACCUSATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_COMITATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_DATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ERGATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_GENITIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_INSTRUMENTAL |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE_COPULATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_NOMINATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_OBLIQUE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_PREPOSITIONAL |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_SOCIATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_VOCATIVE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_DIALECT_NAMES |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_STANDARD_NAMES |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_ANIMATE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_COMMON |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_FEMININE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_INANIMATE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_MASCULINE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_NEUTER |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_OTHER |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_PERSONAL |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_FEW |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_MANY |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ONE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_OTHER |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_TWO |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ZERO |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_NO_SUBSTITUTE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_SUBSTITUTE |  (missing) | DraftICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_UNDEFINED |  (missing) | DraftICU 72
| unum.h | bool unum_hasAttribute(const UNumberFormat*, UNumberFormatAttribute) |  (missing) | DraftICU 72
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KAWI |  (missing) | StableICU 72| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_NAG_MUNDARI |  (missing) | StableICU 72| *(Born Stable)* |

## Other

Other existing drafts in ICU 72

| File | API | ICU 71 | ICU 72 |
|---|---|---|---|
| dtptngen.h |  const UnicodeString&amp; icu::DateTimePatternGenerator::getDateTimeFormat(UDateFormatStyle, UErrorCode&amp;) const | DraftICU 71 | 
| dtptngen.h |  void icu::DateTimePatternGenerator::setDateTimeFormat(UDateFormatStyle, const UnicodeString&amp;, UErrorCode&amp;) | DraftICU 71 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| numberformatter.h |  <tt>static</tt> IncrementPrecision icu::number::Precision::incrementExact(uint64_t, int16_t) | DraftICU 71 | 
| udatpg.h |  const UChar* udatpg_getDateTimeFormatForStyle(const UDateTimePatternGenerator*, UDateFormatStyle, int32_t*, UErrorCode*) | DraftICU 71 | 
| udatpg.h |  void udatpg_setDateTimeFormatForStyle(UDateTimePatternGenerator*, UDateFormatStyle, const UChar*, int32_t, UErrorCode*) | DraftICU 71 | 
| unum.h |  <tt>enum</tt> UNumberFormatFields::UNUM_APPROXIMATELY_SIGN_FIELD | DraftICU 71 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Thu Sep 22 13:25:27 PDT 2022

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  