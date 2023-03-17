
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 72 with ICU 73

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 72](#removed)
- [Deprecated or Obsoleted in ICU 73](#deprecated)
- [Changed in  ICU 73](#changed)
- [Promoted to stable in ICU 73](#promoted)
- [Added in ICU 73](#added)
- [Other existing drafts in ICU 73](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 72
  
| File | API | ICU 72 | ICU 73 |
|---|---|---|---|
| gregocal.h | bool icu::GregorianCalendar::inDaylightTime(UErrorCode&amp;) const |  StableICU 2.0 | (missing)
| platform.h | <tt>#define</tt> U_NOEXCEPT |  Internal | (missing)
| umachine.h | <tt>#define</tt> U_FINAL |  Internal | (missing)
| umachine.h | <tt>#define</tt> U_OVERRIDE |  Internal | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 73
  
| File | API | ICU 72 | ICU 73 |
|---|---|---|---|

## Changed

Changed in  ICU 73 (old, new)


  
| File | API | ICU 72 | ICU 73 |
|---|---|---|---|
| dtptngen.h | const UnicodeString&amp; icu::DateTimePatternGenerator::getDateTimeFormat(UDateFormatStyle, UErrorCode&amp;) const |  Draft→StableICU 71
| dtptngen.h | void icu::DateTimePatternGenerator::setDateTimeFormat(UDateFormatStyle, const UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 71
| numberformatter.h | <tt>static</tt> IncrementPrecision icu::number::Precision::incrementExact(uint64_t, int16_t) |  Draft→StableICU 71
| udatpg.h | const UChar* udatpg_getDateTimeFormatForStyle(const UDateTimePatternGenerator*, UDateFormatStyle, int32_t*, UErrorCode*) |  Draft→StableICU 71
| udatpg.h | void udatpg_setDateTimeFormatForStyle(UDateTimePatternGenerator*, UDateFormatStyle, const UChar*, int32_t, UErrorCode*) |  Draft→StableICU 71
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_APPROXIMATELY_SIGN_FIELD |  Draft→StableICU 71

## Promoted

Promoted to stable in ICU 73
  
| File | API | ICU 72 | ICU 73 |
|---|---|---|---|
| dtptngen.h | const UnicodeString&amp; icu::DateTimePatternGenerator::getDateTimeFormat(UDateFormatStyle, UErrorCode&amp;) const |  Draft→StableICU 71
| dtptngen.h | void icu::DateTimePatternGenerator::setDateTimeFormat(UDateFormatStyle, const UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 71
| numberformatter.h | <tt>static</tt> IncrementPrecision icu::number::Precision::incrementExact(uint64_t, int16_t) |  Draft→StableICU 71
| udatpg.h | const UChar* udatpg_getDateTimeFormatForStyle(const UDateTimePatternGenerator*, UDateFormatStyle, int32_t*, UErrorCode*) |  Draft→StableICU 71
| udatpg.h | void udatpg_setDateTimeFormatForStyle(UDateTimePatternGenerator*, UDateFormatStyle, const UChar*, int32_t, UErrorCode*) |  Draft→StableICU 71
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_APPROXIMATELY_SIGN_FIELD |  Draft→StableICU 71

## Added

Added in ICU 73
  
| File | API | ICU 72 | ICU 73 |
|---|---|---|---|
| calendar.h | bool icu::Calendar::inTemporalLeapYear(UErrorCode&amp;) const |  (missing) | DraftICU 73
| calendar.h | const char* icu::Calendar::getTemporalMonthCode(UErrorCode&amp;) const |  (missing) | DraftICU 73
| calendar.h | void icu::Calendar::setTemporalMonthCode(const char*, UErrorCode&amp;) |  (missing) | DraftICU 73
| dcfmtsym.h | const char* icu::DecimalFormatSymbols::getNumberingSystemName() const |  (missing) | Internal
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBeaufort() |  (missing) | DraftICU 73
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBeaufort(UErrorCode&amp;) |  (missing) | DraftICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfCeiling |  (missing) | DraftICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfFloor |  (missing) | DraftICU 73
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfOdd |  (missing) | DraftICU 73
| platform.h | <tt>#define</tt> UPRV_NO_SANITIZE_UNDEFINED |  (missing) | Internal
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::format(SimpleNumber, UErrorCode&amp;) const |  (missing) | DraftICU 73
| simplenumberformatter.h | FormattedNumber icu::number::SimpleNumberFormatter::formatInt64(int64_t, UErrorCode&amp;) const |  (missing) | DraftICU 73
| simplenumberformatter.h | SimpleNumber&amp; icu::number::SimpleNumber::operator=(SimpleNumber&amp;&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | SimpleNumberFormatter&amp; icu::number::SimpleNumberFormatter::operator=(SimpleNumberFormatter&amp;&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber()=default |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::SimpleNumber(SimpleNumber&amp;&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumber::~SimpleNumber() |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter()=default |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::SimpleNumberFormatter(SimpleNumberFormatter&amp;&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | icu::number::SimpleNumberFormatter::~SimpleNumberFormatter() |  (missing) | DraftICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumber icu::number::SimpleNumber::forInt64(int64_t, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocale(const icu::Locale&amp;, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndGroupingStrategy(const icu::Locale&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndSymbolsAndGroupingStrategy(const icu::Locale&amp;, const DecimalFormatSymbols&amp;, UNumberGroupingStrategy, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::multiplyByPowerOfTen(int32_t, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::roundTo(int32_t, UNumberFormatRoundingMode, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumFractionDigits(uint32_t, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumIntegerDigits(uint32_t, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setSign(USimpleNumberSign, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) |  (missing) | DraftICU 73
| simplenumberformatter.h | void icu::number::SimpleNumberFormatter::formatImpl(impl::UFormattedNumberData*, USimpleNumberSign, UErrorCode&amp;) const |  (missing) | Internal
| ucal.h | <tt>enum</tt> UCalendarDateFields::UCAL_ORDINAL_MONTH |  (missing) | DraftICU 73
| uconfig.h | <tt>#define</tt> UCONFIG_USE_ML_PHRASE_BREAKING |  (missing) | Internal
| uset.h | <tt>enum</tt> (anonymous)::USET_SIMPLE_CASE_INSENSITIVE |  (missing) | DraftICU 73
| usimplenumberformatter.h | USimpleNumber* usnum_openForInt64(int64_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocale(const char*, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | USimpleNumberFormatter* usnumf_openForLocaleAndGroupingStrategy(const char*, UNumberGroupingStrategy, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_MINUS_SIGN |  (missing) | DraftICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_NO_SIGN |  (missing) | DraftICU 73
| usimplenumberformatter.h | <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_PLUS_SIGN |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_close(USimpleNumber*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_multiplyByPowerOfTen(USimpleNumber*, int32_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_roundTo(USimpleNumber*, int32_t, UNumberFormatRoundingMode, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_setMinimumFractionDigits(USimpleNumber*, int32_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_setMinimumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnumf_close(USimpleNumberFormatter*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnumf_format(const USimpleNumberFormatter*, USimpleNumber*, UFormattedNumber*, UErrorCode*) |  (missing) | DraftICU 73
| usimplenumberformatter.h | void usnumf_formatInt64(const USimpleNumberFormatter*, int64_t, UFormattedNumber*, UErrorCode*) |  (missing) | DraftICU 73

## Other

Other existing drafts in ICU 73

| File | API | ICU 72 | ICU 73 |
|---|---|---|---|
| displayoptions.h |  Builder icu::DisplayOptions::copyToBuilder() const | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setCapitalization(UDisplayOptionsCapitalization) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setDisplayLength(UDisplayOptionsDisplayLength) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setGrammaticalCase(UDisplayOptionsGrammaticalCase) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setNameStyle(UDisplayOptionsNameStyle) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setNounClass(UDisplayOptionsNounClass) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setPluralCategory(UDisplayOptionsPluralCategory) | DraftICU 72 | 
| displayoptions.h |  Builder&amp; icu::DisplayOptions::Builder::setSubstituteHandling(UDisplayOptionsSubstituteHandling) | DraftICU 72 | 
| displayoptions.h |  DisplayOptions icu::DisplayOptions::Builder::build() | DraftICU 72 | 
| displayoptions.h |  DisplayOptions&amp; icu::DisplayOptions::operator=(DisplayOptions&amp;&amp;)=default | DraftICU 72 | 
| displayoptions.h |  DisplayOptions&amp; icu::DisplayOptions::operator=(const DisplayOptions&amp;)=default | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsCapitalization icu::DisplayOptions::getCapitalization() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsDisplayLength icu::DisplayOptions::getDisplayLength() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsGrammaticalCase icu::DisplayOptions::getGrammaticalCase() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsNameStyle icu::DisplayOptions::getNameStyle() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsNounClass icu::DisplayOptions::getNounClass() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsPluralCategory icu::DisplayOptions::getPluralCategory() const | DraftICU 72 | 
| displayoptions.h |  UDisplayOptionsSubstituteHandling icu::DisplayOptions::getSubstituteHandling() const | DraftICU 72 | 
| displayoptions.h |  icu::DisplayOptions::DisplayOptions(const DisplayOptions&amp;)=default | DraftICU 72 | 
| displayoptions.h |  <tt>static</tt> Builder icu::DisplayOptions::builder() | DraftICU 72 | 
| formattednumber.h |  UDisplayOptionsNounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const | DraftICU 72 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuarter() | DraftICU 72 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTonne() | DraftICU 72 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuarter(UErrorCode&amp;) | DraftICU 72 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTonne(UErrorCode&amp;) | DraftICU 72 | 
| numberformatter.h |  Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;) const&amp; | DraftICU 72 | 
| numberformatter.h |  Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;)&amp;&amp; | DraftICU 72 | 
| udisplayoptions.h |  UDisplayOptionsGrammaticalCase udispopt_fromGrammaticalCaseIdentifier(const char*) | DraftICU 72 | 
| udisplayoptions.h |  UDisplayOptionsNounClass udispopt_fromNounClassIdentifier(const char*) | DraftICU 72 | 
| udisplayoptions.h |  UDisplayOptionsPluralCategory udispopt_fromPluralCategoryIdentifier(const char*) | DraftICU 72 | 
| udisplayoptions.h |  const char* udispopt_getGrammaticalCaseIdentifier(UDisplayOptionsGrammaticalCase) | DraftICU 72 | 
| udisplayoptions.h |  const char* udispopt_getNounClassIdentifier(UDisplayOptionsNounClass) | DraftICU 72 | 
| udisplayoptions.h |  const char* udispopt_getPluralCategoryIdentifier(UDisplayOptionsPluralCategory) | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_BEGINNING_OF_SENTENCE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_MIDDLE_OF_SENTENCE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_STANDALONE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UI_LIST_OR_MENU | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_FULL | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_SHORT | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ABLATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ACCUSATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_COMITATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_DATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ERGATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_GENITIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_INSTRUMENTAL | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE_COPULATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_NOMINATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_OBLIQUE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_PREPOSITIONAL | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_SOCIATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_VOCATIVE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_DIALECT_NAMES | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_STANDARD_NAMES | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_ANIMATE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_COMMON | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_FEMININE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_INANIMATE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_MASCULINE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_NEUTER | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_OTHER | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_PERSONAL | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_FEW | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_MANY | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ONE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_OTHER | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_TWO | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ZERO | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_NO_SUBSTITUTE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_SUBSTITUTE | DraftICU 72 | 
| udisplayoptions.h |  <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_UNDEFINED | DraftICU 72 | 
| unum.h |  bool unum_hasAttribute(const UNumberFormat*, UNumberFormatAttribute) | DraftICU 72 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.

- **`Appendable& icu::FormattedDateInterval::appendTo(Appendable&, UErrorCode&) const`**
  - `Appendable& icu::FormattedDateInterval::appendTo(Appendable&, UErrorCode&) const U_OVERRIDE`
  - `Appendable& icu::FormattedDateInterval::appendTo(Appendable&, UErrorCode&) const override`
- **`Appendable& icu::FormattedList::appendTo(Appendable&, UErrorCode&) const`**
  - `Appendable& icu::FormattedList::appendTo(Appendable&, UErrorCode&) const U_OVERRIDE`
  - `Appendable& icu::FormattedList::appendTo(Appendable&, UErrorCode&) const override`
- **`Appendable& icu::FormattedRelativeDateTime::appendTo(Appendable&, UErrorCode&) const`**
  - `Appendable& icu::FormattedRelativeDateTime::appendTo(Appendable&, UErrorCode&) const U_OVERRIDE`
  - `Appendable& icu::FormattedRelativeDateTime::appendTo(Appendable&, UErrorCode&) const override`
- **`Appendable& icu::number::FormattedNumber::appendTo(Appendable&, UErrorCode&) const`**
  - `Appendable& icu::number::FormattedNumber::appendTo(Appendable&, UErrorCode&) const U_OVERRIDE`
  - `Appendable& icu::number::FormattedNumber::appendTo(Appendable&, UErrorCode&) const override`
- **`Appendable& icu::number::FormattedNumberRange::appendTo(Appendable&, UErrorCode&) const`**
  - `Appendable& icu::number::FormattedNumberRange::appendTo(Appendable&, UErrorCode&) const U_OVERRIDE`
  - `Appendable& icu::number::FormattedNumberRange::appendTo(Appendable&, UErrorCode&) const override`
- **`Builder& icu::LocaleMatcher::Builder::operator=(Builder&&)`**
  - `Builder& icu::LocaleMatcher::Builder::operator=(Builder&&) U_NOEXCEPT`
  - `Builder& icu::LocaleMatcher::Builder::operator=(Builder&&) noexcept`
- **`CurrencyAmount* icu::CompactDecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const`**
  - `CurrencyAmount* icu::CompactDecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const U_OVERRIDE`
  - `CurrencyAmount* icu::CompactDecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const override`
- **`CurrencyAmount* icu::DecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const`**
  - `CurrencyAmount* icu::DecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const U_OVERRIDE`
  - `CurrencyAmount* icu::DecimalFormat::parseCurrency(const UnicodeString&, ParsePosition&) const override`
- **`ERoundingMode icu::DecimalFormat::getRoundingMode() const`**
  - `ERoundingMode icu::DecimalFormat::getRoundingMode() const U_OVERRIDE`
  - `ERoundingMode icu::DecimalFormat::getRoundingMode() const override`
- **`Edits& icu::Edits::operator=(Edits&&)`**
  - `Edits& icu::Edits::operator=(Edits&&) U_NOEXCEPT`
  - `Edits& icu::Edits::operator=(Edits&&) noexcept`
- **`FormattedDateInterval& icu::FormattedDateInterval::operator=(FormattedDateInterval&&)`**
  - `FormattedDateInterval& icu::FormattedDateInterval::operator=(FormattedDateInterval&&) U_NOEXCEPT`
  - `FormattedDateInterval& icu::FormattedDateInterval::operator=(FormattedDateInterval&&) noexcept`
- **`FormattedList& icu::FormattedList::operator=(FormattedList&&)`**
  - `FormattedList& icu::FormattedList::operator=(FormattedList&&) U_NOEXCEPT`
  - `FormattedList& icu::FormattedList::operator=(FormattedList&&) noexcept`
- **`FormattedNumber& icu::number::FormattedNumber::operator=(FormattedNumber&&)`**
  - `FormattedNumber& icu::number::FormattedNumber::operator=(FormattedNumber&&) U_NOEXCEPT`
  - `FormattedNumber& icu::number::FormattedNumber::operator=(FormattedNumber&&) noexcept`
- **`FormattedNumberRange& icu::number::FormattedNumberRange::operator=(FormattedNumberRange&&)`**
  - `FormattedNumberRange& icu::number::FormattedNumberRange::operator=(FormattedNumberRange&&) U_NOEXCEPT`
  - `FormattedNumberRange& icu::number::FormattedNumberRange::operator=(FormattedNumberRange&&) noexcept`
- **`FormattedRelativeDateTime& icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&&)`**
  - `FormattedRelativeDateTime& icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&&) U_NOEXCEPT`
  - `FormattedRelativeDateTime& icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&&) noexcept`
- **`LocalArray< T >& icu::LocalArray< T >::operator=(LocalArray< T >&&)`**
  - `LocalArray< T >& icu::LocalArray< T >::operator=(LocalArray< T >&&) U_NOEXCEPT`
  - `LocalArray< T >& icu::LocalArray< T >::operator=(LocalArray< T >&&) noexcept`
- **`LocalArray< T >& icu::LocalArray< T >::operator=(std::unique_ptr< T[]>&&)`**
  - `LocalArray< T >& icu::LocalArray< T >::operator=(std::unique_ptr< T[]>&&) U_NOEXCEPT`
  - `LocalArray< T >& icu::LocalArray< T >::operator=(std::unique_ptr< T[]>&&) noexcept`
- **`LocalPointer< T >& icu::LocalPointer< T >::operator=(LocalPointer< T >&&)`**
  - `LocalPointer< T >& icu::LocalPointer< T >::operator=(LocalPointer< T >&&) U_NOEXCEPT`
  - `LocalPointer< T >& icu::LocalPointer< T >::operator=(LocalPointer< T >&&) noexcept`
- **`LocalPointer< T >& icu::LocalPointer< T >::operator=(std::unique_ptr< T >&&)`**
  - `LocalPointer< T >& icu::LocalPointer< T >::operator=(std::unique_ptr< T >&&) U_NOEXCEPT`
  - `LocalPointer< T >& icu::LocalPointer< T >::operator=(std::unique_ptr< T >&&) noexcept`
- **`Locale& icu::Locale::operator=(Locale&&)`**
  - `Locale& icu::Locale::operator=(Locale&&) U_NOEXCEPT`
  - `Locale& icu::Locale::operator=(Locale&&) noexcept`
- **`LocaleMatcher& icu::LocaleMatcher::operator=(LocaleMatcher&&)`**
  - `LocaleMatcher& icu::LocaleMatcher::operator=(LocaleMatcher&&) U_NOEXCEPT`
  - `LocaleMatcher& icu::LocaleMatcher::operator=(LocaleMatcher&&) noexcept`
- **`LocalizedNumberFormatter& icu::number::LocalizedNumberFormatter::operator=(LocalizedNumberFormatter&&)`**
  - `LocalizedNumberFormatter& icu::number::LocalizedNumberFormatter::operator=(LocalizedNumberFormatter&&) U_NOEXCEPT`
  - `LocalizedNumberFormatter& icu::number::LocalizedNumberFormatter::operator=(LocalizedNumberFormatter&&) noexcept`
- **`LocalizedNumberRangeFormatter& icu::number::LocalizedNumberRangeFormatter::operator=(LocalizedNumberRangeFormatter&&)`**
  - `LocalizedNumberRangeFormatter& icu::number::LocalizedNumberRangeFormatter::operator=(LocalizedNumberRangeFormatter&&) U_NOEXCEPT`
  - `LocalizedNumberRangeFormatter& icu::number::LocalizedNumberRangeFormatter::operator=(LocalizedNumberRangeFormatter&&) noexcept`
- **`Result& icu::LocaleMatcher::Result::operator=(Result&&)`**
  - `Result& icu::LocaleMatcher::Result::operator=(Result&&) U_NOEXCEPT`
  - `Result& icu::LocaleMatcher::Result::operator=(Result&&) noexcept`
- **`Scale& icu::number::Scale::operator=(Scale&&)`**
  - `Scale& icu::number::Scale::operator=(Scale&&) U_NOEXCEPT`
  - `Scale& icu::number::Scale::operator=(Scale&&) noexcept`
- **`StringProp& icu::number::impl::StringProp::operator=(StringProp&&)`**
  - `StringProp& icu::number::impl::StringProp::operator=(StringProp&&) U_NOEXCEPT`
  - `StringProp& icu::number::impl::StringProp::operator=(StringProp&&) noexcept`
- **`SymbolsWrapper& icu::number::impl::SymbolsWrapper::operator=(SymbolsWrapper&&)`**
  - `SymbolsWrapper& icu::number::impl::SymbolsWrapper::operator=(SymbolsWrapper&&) U_NOEXCEPT`
  - `SymbolsWrapper& icu::number::impl::SymbolsWrapper::operator=(SymbolsWrapper&&) noexcept`
- **`UChar32 icu::FilteredNormalizer2::composePair(UChar32, UChar32) const`**
  - `UChar32 icu::FilteredNormalizer2::composePair(UChar32, UChar32) const U_OVERRIDE`
  - `UChar32 icu::FilteredNormalizer2::composePair(UChar32, UChar32) const override`
- **`UClassID icu::CompactDecimalFormat::getDynamicClassID() const`**
  - `UClassID icu::CompactDecimalFormat::getDynamicClassID() const U_OVERRIDE`
  - `UClassID icu::CompactDecimalFormat::getDynamicClassID() const override`
- **`UClassID icu::DecimalFormat::getDynamicClassID() const`**
  - `UClassID icu::DecimalFormat::getDynamicClassID() const U_OVERRIDE`
  - `UClassID icu::DecimalFormat::getDynamicClassID() const override`
- **`UNormalizationCheckResult icu::FilteredNormalizer2::quickCheck(const UnicodeString&, UErrorCode&) const`**
  - `UNormalizationCheckResult icu::FilteredNormalizer2::quickCheck(const UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `UNormalizationCheckResult icu::FilteredNormalizer2::quickCheck(const UnicodeString&, UErrorCode&) const override`
- **`UnicodeString icu::FormattedDateInterval::toString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedDateInterval::toString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedDateInterval::toString(UErrorCode&) const override`
- **`UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&) const override`
- **`UnicodeString icu::FormattedList::toString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedList::toString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedList::toString(UErrorCode&) const override`
- **`UnicodeString icu::FormattedList::toTempString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedList::toTempString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedList::toTempString(UErrorCode&) const override`
- **`UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&) const override`
- **`UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&) const`**
  - `UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&) const override`
- **`UnicodeString icu::number::FormattedNumber::toString(UErrorCode&) const`**
  - `UnicodeString icu::number::FormattedNumber::toString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::number::FormattedNumber::toString(UErrorCode&) const override`
- **`UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&) const`**
  - `UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&) const override`
- **`UnicodeString icu::number::FormattedNumberRange::toString(UErrorCode&) const`**
  - `UnicodeString icu::number::FormattedNumberRange::toString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::number::FormattedNumberRange::toString(UErrorCode&) const override`
- **`UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&) const`**
  - `UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&) const U_OVERRIDE`
  - `UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(StringPiece, UnicodeString&, FieldPositionIterator*, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(StringPiece, UnicodeString&, FieldPositionIterator*, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(StringPiece, UnicodeString&, FieldPositionIterator*, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPosition&, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPosition&, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPositionIterator*, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPositionIterator*, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(const number::impl::DecimalQuantity&, UnicodeString&, FieldPositionIterator*, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&) const`**
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&) const override`
- **`UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPosition&, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPositionIterator*, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPositionIterator*, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(double, UnicodeString&, FieldPositionIterator*, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPosition&, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int32_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPosition&, UErrorCode&) const override`
- **`UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const`**
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::DecimalFormat::format(int64_t, UnicodeString&, FieldPositionIterator*, UErrorCode&) const override`
- **`UnicodeString& icu::FilteredNormalizer2::append(UnicodeString&, const UnicodeString&, UErrorCode&) const`**
  - `UnicodeString& icu::FilteredNormalizer2::append(UnicodeString&, const UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::FilteredNormalizer2::append(UnicodeString&, const UnicodeString&, UErrorCode&) const override`
- **`UnicodeString& icu::FilteredNormalizer2::normalize(const UnicodeString&, UnicodeString&, UErrorCode&) const`**
  - `UnicodeString& icu::FilteredNormalizer2::normalize(const UnicodeString&, UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::FilteredNormalizer2::normalize(const UnicodeString&, UnicodeString&, UErrorCode&) const override`
- **`UnicodeString& icu::FilteredNormalizer2::normalizeSecondAndAppend(UnicodeString&, const UnicodeString&, UErrorCode&) const`**
  - `UnicodeString& icu::FilteredNormalizer2::normalizeSecondAndAppend(UnicodeString&, const UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `UnicodeString& icu::FilteredNormalizer2::normalizeSecondAndAppend(UnicodeString&, const UnicodeString&, UErrorCode&) const override`
- **`UnicodeString& icu::UnicodeString::operator=(UnicodeString&&)`**
  - `UnicodeString& icu::UnicodeString::operator=(UnicodeString&&) U_NOEXCEPT`
  - `UnicodeString& icu::UnicodeString::operator=(UnicodeString&&) noexcept`
- **`UnlocalizedNumberFormatter& icu::number::UnlocalizedNumberFormatter::operator=(UnlocalizedNumberFormatter&&)`**
  - `UnlocalizedNumberFormatter& icu::number::UnlocalizedNumberFormatter::operator=(UnlocalizedNumberFormatter&&) U_NOEXCEPT`
  - `UnlocalizedNumberFormatter& icu::number::UnlocalizedNumberFormatter::operator=(UnlocalizedNumberFormatter&&) noexcept`
- **`UnlocalizedNumberRangeFormatter& icu::number::UnlocalizedNumberRangeFormatter::operator=(UnlocalizedNumberRangeFormatter&&)`**
  - `UnlocalizedNumberRangeFormatter& icu::number::UnlocalizedNumberRangeFormatter::operator=(UnlocalizedNumberRangeFormatter&&) U_NOEXCEPT`
  - `UnlocalizedNumberRangeFormatter& icu::number::UnlocalizedNumberRangeFormatter::operator=(UnlocalizedNumberRangeFormatter&&) noexcept`
- **`bool icu::Calendar::inDaylightTime(UErrorCode&) const`**
  - `UBool icu::Calendar::inDaylightTime(UErrorCode&) const`
  - `UBool icu::Calendar::inDaylightTime(UErrorCode&) const=0`
- **`bool icu::DecimalFormat::operator==(const Format&) const`**
  - `bool icu::DecimalFormat::operator==(const Format&) const U_OVERRIDE`
  - `bool icu::DecimalFormat::operator==(const Format&) const override`
- **`bool icu::FilteredNormalizer2::getDecomposition(UChar32, UnicodeString&) const`**
  - `UBool icu::FilteredNormalizer2::getDecomposition(UChar32, UnicodeString&) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::getDecomposition(UChar32, UnicodeString&) const override`
- **`bool icu::FilteredNormalizer2::getRawDecomposition(UChar32, UnicodeString&) const`**
  - `UBool icu::FilteredNormalizer2::getRawDecomposition(UChar32, UnicodeString&) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::getRawDecomposition(UChar32, UnicodeString&) const override`
- **`bool icu::FilteredNormalizer2::hasBoundaryAfter(UChar32) const`**
  - `UBool icu::FilteredNormalizer2::hasBoundaryAfter(UChar32) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::hasBoundaryAfter(UChar32) const override`
- **`bool icu::FilteredNormalizer2::hasBoundaryBefore(UChar32) const`**
  - `UBool icu::FilteredNormalizer2::hasBoundaryBefore(UChar32) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::hasBoundaryBefore(UChar32) const override`
- **`bool icu::FilteredNormalizer2::isInert(UChar32) const`**
  - `UBool icu::FilteredNormalizer2::isInert(UChar32) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::isInert(UChar32) const override`
- **`bool icu::FilteredNormalizer2::isNormalized(const UnicodeString&, UErrorCode&) const`**
  - `UBool icu::FilteredNormalizer2::isNormalized(const UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::isNormalized(const UnicodeString&, UErrorCode&) const override`
- **`bool icu::FilteredNormalizer2::isNormalizedUTF8(StringPiece, UErrorCode&) const`**
  - `UBool icu::FilteredNormalizer2::isNormalizedUTF8(StringPiece, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::FilteredNormalizer2::isNormalizedUTF8(StringPiece, UErrorCode&) const override`
- **`bool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const`**
  - `UBool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const override`
- **`bool icu::FormattedList::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const`**
  - `UBool icu::FormattedList::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::FormattedList::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const override`
- **`bool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const`**
  - `UBool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const override`
- **`bool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const`**
  - `UBool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const override`
- **`bool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const`**
  - `UBool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const U_OVERRIDE`
  - `UBool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&, UErrorCode&) const override`
- **`icu::CompactDecimalFormat::~CompactDecimalFormat()`**
  - `icu::CompactDecimalFormat::~CompactDecimalFormat() U_OVERRIDE`
  - `icu::CompactDecimalFormat::~CompactDecimalFormat() override`
- **`icu::DecimalFormat::~DecimalFormat()`**
  - `icu::DecimalFormat::~DecimalFormat() U_OVERRIDE`
  - `icu::DecimalFormat::~DecimalFormat() override`
- **`icu::Edits::Edits(Edits&&)`**
  - `icu::Edits::Edits(Edits&&) U_NOEXCEPT`
  - `icu::Edits::Edits(Edits&&) noexcept`
- **`icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&&)`**
  - `icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&&) U_NOEXCEPT`
  - `icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&&) noexcept`
- **`icu::FormattedDateInterval::~FormattedDateInterval()`**
  - `icu::FormattedDateInterval::~FormattedDateInterval() U_OVERRIDE`
  - `icu::FormattedDateInterval::~FormattedDateInterval() override`
- **`icu::FormattedList::FormattedList(FormattedList&&)`**
  - `icu::FormattedList::FormattedList(FormattedList&&) U_NOEXCEPT`
  - `icu::FormattedList::FormattedList(FormattedList&&) noexcept`
- **`icu::FormattedList::~FormattedList()`**
  - `icu::FormattedList::~FormattedList() U_OVERRIDE`
  - `icu::FormattedList::~FormattedList() override`
- **`icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&&)`**
  - `icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&&) U_NOEXCEPT`
  - `icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&&) noexcept`
- **`icu::FormattedRelativeDateTime::~FormattedRelativeDateTime()`**
  - `icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() U_OVERRIDE`
  - `icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() override`
- **`icu::LocalArray< T >::LocalArray(LocalArray< T >&&)`**
  - `icu::LocalArray< T >::LocalArray(LocalArray< T >&&) U_NOEXCEPT`
  - `icu::LocalArray< T >::LocalArray(LocalArray< T >&&) noexcept`
- **`icu::LocalPointer< T >::LocalPointer(LocalPointer< T >&&)`**
  - `icu::LocalPointer< T >::LocalPointer(LocalPointer< T >&&) U_NOEXCEPT`
  - `icu::LocalPointer< T >::LocalPointer(LocalPointer< T >&&) noexcept`
- **`icu::Locale::Locale(Locale&&)`**
  - `icu::Locale::Locale(Locale&&) U_NOEXCEPT`
  - `icu::Locale::Locale(Locale&&) noexcept`
- **`icu::LocaleMatcher::Builder::Builder(Builder&&)`**
  - `icu::LocaleMatcher::Builder::Builder(Builder&&) U_NOEXCEPT`
  - `icu::LocaleMatcher::Builder::Builder(Builder&&) noexcept`
- **`icu::LocaleMatcher::LocaleMatcher(LocaleMatcher&&)`**
  - `icu::LocaleMatcher::LocaleMatcher(LocaleMatcher&&) U_NOEXCEPT`
  - `icu::LocaleMatcher::LocaleMatcher(LocaleMatcher&&) noexcept`
- **`icu::LocaleMatcher::Result::Result(Result&&)`**
  - `icu::LocaleMatcher::Result::Result(Result&&) U_NOEXCEPT`
  - `icu::LocaleMatcher::Result::Result(Result&&) noexcept`
- **`icu::UnicodeString::UnicodeString(UnicodeString&&)`**
  - `icu::UnicodeString::UnicodeString(UnicodeString&&) U_NOEXCEPT`
  - `icu::UnicodeString::UnicodeString(UnicodeString&&) noexcept`
- **`icu::number::FormattedNumber::FormattedNumber(FormattedNumber&&)`**
  - `icu::number::FormattedNumber::FormattedNumber(FormattedNumber&&) U_NOEXCEPT`
  - `icu::number::FormattedNumber::FormattedNumber(FormattedNumber&&) noexcept`
- **`icu::number::FormattedNumber::~FormattedNumber()`**
  - `icu::number::FormattedNumber::~FormattedNumber() U_OVERRIDE`
  - `icu::number::FormattedNumber::~FormattedNumber() override`
- **`icu::number::FormattedNumberRange::FormattedNumberRange(FormattedNumberRange&&)`**
  - `icu::number::FormattedNumberRange::FormattedNumberRange(FormattedNumberRange&&) U_NOEXCEPT`
  - `icu::number::FormattedNumberRange::FormattedNumberRange(FormattedNumberRange&&) noexcept`
- **`icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(LocalizedNumberFormatter&&)`**
  - `icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(LocalizedNumberFormatter&&) U_NOEXCEPT`
  - `icu::number::LocalizedNumberFormatter::LocalizedNumberFormatter(LocalizedNumberFormatter&&) noexcept`
- **`icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(LocalizedNumberRangeFormatter&&)`**
  - `icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(LocalizedNumberRangeFormatter&&) U_NOEXCEPT`
  - `icu::number::LocalizedNumberRangeFormatter::LocalizedNumberRangeFormatter(LocalizedNumberRangeFormatter&&) noexcept`
- **`icu::number::Scale::Scale(Scale&&)`**
  - `icu::number::Scale::Scale(Scale&&) U_NOEXCEPT`
  - `icu::number::Scale::Scale(Scale&&) noexcept`
- **`icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(UnlocalizedNumberFormatter&&)`**
  - `icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(UnlocalizedNumberFormatter&&) U_NOEXCEPT`
  - `icu::number::UnlocalizedNumberFormatter::UnlocalizedNumberFormatter(UnlocalizedNumberFormatter&&) noexcept`
- **`icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(UnlocalizedNumberRangeFormatter&&)`**
  - `icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(UnlocalizedNumberRangeFormatter&&) U_NOEXCEPT`
  - `icu::number::UnlocalizedNumberRangeFormatter::UnlocalizedNumberRangeFormatter(UnlocalizedNumberRangeFormatter&&) noexcept`
- **`icu::number::impl::StringProp::StringProp(StringProp&&)`**
  - `icu::number::impl::StringProp::StringProp(StringProp&&) U_NOEXCEPT`
  - `icu::number::impl::StringProp::StringProp(StringProp&&) noexcept`
- **`icu::number::impl::SymbolsWrapper::SymbolsWrapper(SymbolsWrapper&&)`**
  - `icu::number::impl::SymbolsWrapper::SymbolsWrapper(SymbolsWrapper&&) U_NOEXCEPT`
  - `icu::number::impl::SymbolsWrapper::SymbolsWrapper(SymbolsWrapper&&) noexcept`
- **`int32_t icu::FilteredNormalizer2::spanQuickCheckYes(const UnicodeString&, UErrorCode&) const`**
  - `int32_t icu::FilteredNormalizer2::spanQuickCheckYes(const UnicodeString&, UErrorCode&) const U_OVERRIDE`
  - `int32_t icu::FilteredNormalizer2::spanQuickCheckYes(const UnicodeString&, UErrorCode&) const override`
- **`uint8_t icu::FilteredNormalizer2::getCombiningClass(UChar32) const`**
  - `uint8_t icu::FilteredNormalizer2::getCombiningClass(UChar32) const U_OVERRIDE`
  - `uint8_t icu::FilteredNormalizer2::getCombiningClass(UChar32) const override`
- **`void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const`**
  - `void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const U_OVERRIDE`
  - `void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const override`
- **`void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, UErrorCode&) const`**
  - `void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, UErrorCode&) const U_OVERRIDE`
  - `void icu::CompactDecimalFormat::parse(const UnicodeString&, Formattable&, UErrorCode&) const override`
- **`void icu::DecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const`**
  - `void icu::DecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const U_OVERRIDE`
  - `void icu::DecimalFormat::parse(const UnicodeString&, Formattable&, ParsePosition&) const override`
- **`void icu::DecimalFormat::setCurrency(const char16_t*, UErrorCode&)`**
  - `void icu::DecimalFormat::setCurrency(const char16_t*, UErrorCode&) U_OVERRIDE`
  - `void icu::DecimalFormat::setCurrency(const char16_t*, UErrorCode&) override`
- **`void icu::DecimalFormat::setGroupingUsed(bool)`**
  - `void icu::DecimalFormat::setGroupingUsed(UBool) U_OVERRIDE`
  - `void icu::DecimalFormat::setGroupingUsed(UBool) override`
- **`void icu::DecimalFormat::setLenient(bool)`**
  - `void icu::DecimalFormat::setLenient(UBool) U_OVERRIDE`
  - `void icu::DecimalFormat::setLenient(UBool) override`
- **`void icu::DecimalFormat::setMaximumFractionDigits(int32_t)`**
  - `void icu::DecimalFormat::setMaximumFractionDigits(int32_t) U_OVERRIDE`
  - `void icu::DecimalFormat::setMaximumFractionDigits(int32_t) override`
- **`void icu::DecimalFormat::setMaximumIntegerDigits(int32_t)`**
  - `void icu::DecimalFormat::setMaximumIntegerDigits(int32_t) U_OVERRIDE`
  - `void icu::DecimalFormat::setMaximumIntegerDigits(int32_t) override`
- **`void icu::DecimalFormat::setMinimumFractionDigits(int32_t)`**
  - `void icu::DecimalFormat::setMinimumFractionDigits(int32_t) U_OVERRIDE`
  - `void icu::DecimalFormat::setMinimumFractionDigits(int32_t) override`
- **`void icu::DecimalFormat::setMinimumIntegerDigits(int32_t)`**
  - `void icu::DecimalFormat::setMinimumIntegerDigits(int32_t) U_OVERRIDE`
  - `void icu::DecimalFormat::setMinimumIntegerDigits(int32_t) override`
- **`void icu::DecimalFormat::setParseIntegerOnly(bool)`**
  - `void icu::DecimalFormat::setParseIntegerOnly(UBool) U_OVERRIDE`
  - `void icu::DecimalFormat::setParseIntegerOnly(UBool) override`
- **`void icu::DecimalFormat::setRoundingMode(ERoundingMode)`**
  - `void icu::DecimalFormat::setRoundingMode(ERoundingMode) U_OVERRIDE`
  - `void icu::DecimalFormat::setRoundingMode(ERoundingMode) override`
- **`void icu::Edits::reset()`**
  - `void icu::Edits::reset() U_NOEXCEPT`
  - `void icu::Edits::reset() noexcept`
- **`void icu::FilteredNormalizer2::normalizeUTF8(uint32_t, StringPiece, ByteSink&, Edits*, UErrorCode&) const`**
  - `void icu::FilteredNormalizer2::normalizeUTF8(uint32_t, StringPiece, ByteSink&, Edits*, UErrorCode&) const U_OVERRIDE`
  - `void icu::FilteredNormalizer2::normalizeUTF8(uint32_t, StringPiece, ByteSink&, Edits*, UErrorCode&) const override`
- **`void icu::LocalArray< T >::swap(LocalArray< T >&)`**
  - `void icu::LocalArray< T >::swap(LocalArray< T >&) U_NOEXCEPT`
  - `void icu::LocalArray< T >::swap(LocalArray< T >&) noexcept`
- **`void icu::LocalPointer< T >::swap(LocalPointer< T >&)`**
  - `void icu::LocalPointer< T >::swap(LocalPointer< T >&) U_NOEXCEPT`
  - `void icu::LocalPointer< T >::swap(LocalPointer< T >&) noexcept`
- **`void icu::UnicodeString::swap(UnicodeString&)`**
  - `void icu::UnicodeString::swap(UnicodeString&) U_NOEXCEPT`
  - `void icu::UnicodeString::swap(UnicodeString&) noexcept`
- **`void* icu::CompactDecimalFormat::clone() const`**
  - `CompactDecimalFormat* icu::CompactDecimalFormat::clone() const U_OVERRIDE`
  - `CompactDecimalFormat* icu::CompactDecimalFormat::clone() const override`
- **`void* icu::DecimalFormat::clone() const`**
  - `DecimalFormat* icu::DecimalFormat::clone() const U_OVERRIDE`
  - `DecimalFormat* icu::DecimalFormat::clone() const override`

## Colophon

Contents generated by StableAPI tool on Fri Mar 17 10:37:04 PDT 2023

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  