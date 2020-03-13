
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 65 with ICU 66

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 65](#removed)
- [Deprecated or Obsoleted in ICU 66](#deprecated)
- [Changed in  ICU 66](#changed)
- [Promoted to stable in ICU 66](#promoted)
- [Added in ICU 66](#added)
- [Other existing drafts in ICU 66](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 65
  
| File | API | ICU 65 | ICU 66 |
|---|---|---|---|

## Deprecated

Deprecated or Obsoleted in ICU 66
  
| File | API | ICU 65 | ICU 66 |
|---|---|---|---|

## Changed

Changed in  ICU 66 (old, new)


  
| File | API | ICU 65 | ICU 66 |
|---|---|---|---|

## Promoted

Promoted to stable in ICU 66
  
| File | API | ICU 65 | ICU 66 |
|---|---|---|---|
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CHORASMIAN |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_DIVES_AKURU |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KHITAN_SMALL_SCRIPT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LISU_SUPPLEMENT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SYMBOLS_FOR_LEGACY_COMPUTING |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGUT_SUPPLEMENT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_YEZIDI |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicPositionalCategory::U_INPC_TOP_AND_BOTTOM_AND_LEFT |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_CHORASMIAN |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_DIVES_AKURU |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KHITAN_SMALL_SCRIPT |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_YEZIDI |  (missing) | StableICU 66| *(Born Stable)* |

## Added

Added in ICU 66
  
| File | API | ICU 65 | ICU 66 |
|---|---|---|---|
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CHORASMIAN |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_DIVES_AKURU |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KHITAN_SMALL_SCRIPT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_LISU_SUPPLEMENT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SYMBOLS_FOR_LEGACY_COMPUTING |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGUT_SUPPLEMENT |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_YEZIDI |  (missing) | StableICU 66| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicPositionalCategory::U_INPC_TOP_AND_BOTTOM_AND_LEFT |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_CHORASMIAN |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_DIVES_AKURU |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KHITAN_SMALL_SCRIPT |  (missing) | StableICU 66| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_YEZIDI |  (missing) | StableICU 66| *(Born Stable)* |

## Other

Other existing drafts in ICU 66

| File | API | ICU 65 | ICU 66 |
|---|---|---|---|
| bytestrie.h |  BytesTrie&amp; icu::BytesTrie::resetToState64(uint64_t) | DraftICU 65 | 
| bytestrie.h |  uint64_t icu::BytesTrie::getState64() const | DraftICU 65 | 
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
| localebuilder.h |  UBool icu::LocaleBuilder::copyErrorTo(UErrorCode&amp;) const | DraftICU 65 | 
| localebuilder.h |  icu::LocaleBuilder::LocaleBuilder() | DraftICU 64 | 
| localebuilder.h |  icu::LocaleBuilder::~LocaleBuilder() | DraftICU 64 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::addSupportedLocale(const Locale&amp;) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::operator=(Builder&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setDefaultLocale(const Locale*) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setDemotionPerDesiredLocale(ULocMatchDemotion) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setFavorSubtag(ULocMatchFavorSubtag) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocales(Iter, Iter) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocales(Locale::Iterator&amp;) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocalesFromListString(StringPiece) | DraftICU 65 | 
| localematcher.h |  Builder&amp; icu::LocaleMatcher::Builder::setSupportedLocalesViaConverter(Iter, Iter, Conv) | DraftICU 65 | 
| localematcher.h |  Locale icu::LocaleMatcher::Result::makeResolvedLocale(UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  LocaleMatcher icu::LocaleMatcher::Builder::build(UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  LocaleMatcher&amp; icu::LocaleMatcher::operator=(LocaleMatcher&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  Result icu::LocaleMatcher::getBestMatchResult(Locale::Iterator&amp;, UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  Result icu::LocaleMatcher::getBestMatchResult(const Locale&amp;, UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  Result&amp; icu::LocaleMatcher::Result::operator=(Result&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  UBool icu::LocaleMatcher::Builder::copyErrorTo(UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  const Locale* icu::LocaleMatcher::Result::getDesiredLocale() const | DraftICU 65 | 
| localematcher.h |  const Locale* icu::LocaleMatcher::Result::getSupportedLocale() const | DraftICU 65 | 
| localematcher.h |  const Locale* icu::LocaleMatcher::getBestMatch(Locale::Iterator&amp;, UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  const Locale* icu::LocaleMatcher::getBestMatch(const Locale&amp;, UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  const Locale* icu::LocaleMatcher::getBestMatchForListString(StringPiece, UErrorCode&amp;) const | DraftICU 65 | 
| localematcher.h |  <tt>enum</tt> ULocMatchDemotion::ULOCMATCH_DEMOTION_NONE | DraftICU 65 | 
| localematcher.h |  <tt>enum</tt> ULocMatchDemotion::ULOCMATCH_DEMOTION_REGION | DraftICU 65 | 
| localematcher.h |  <tt>enum</tt> ULocMatchFavorSubtag::ULOCMATCH_FAVOR_LANGUAGE | DraftICU 65 | 
| localematcher.h |  <tt>enum</tt> ULocMatchFavorSubtag::ULOCMATCH_FAVOR_SCRIPT | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::Builder::Builder() | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::Builder::Builder(Builder&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::Builder::~Builder() | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::LocaleMatcher(LocaleMatcher&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::Result::Result(Result&amp;&amp;) | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::Result::~Result() | DraftICU 65 | 
| localematcher.h |  icu::LocaleMatcher::~LocaleMatcher() | DraftICU 65 | 
| localematcher.h |  int32_t icu::LocaleMatcher::Result::getDesiredIndex() const | DraftICU 65 | 
| localematcher.h |  int32_t icu::LocaleMatcher::Result::getSupportedIndex() const | DraftICU 65 | 
| localpointer.h |  LocalArray&lt;T&gt;&amp; icu::LocalArray&lt; T &gt;::operator=(std::unique_ptr&lt; T[]&gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  LocalPointer&lt;T&gt;&amp; icu::LocalPointer&lt; T &gt;::operator=(std::unique_ptr&lt; T &gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalArray&lt; T &gt;::LocalArray(std::unique_ptr&lt; T[]&gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalArray&lt; T &gt;::operator std::unique_ptr&lt; T[]&gt;() &amp;&amp; | DraftICU 64 | 
| localpointer.h |  icu::LocalPointer&lt; T &gt;::LocalPointer(std::unique_ptr&lt; T &gt;&amp;&amp;) | DraftICU 64 | 
| localpointer.h |  icu::LocalPointer&lt; T &gt;::operator std::unique_ptr&lt; T &gt;() &amp;&amp; | DraftICU 64 | 
| locid.h |  UBool icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::hasNext() const override | DraftICU 65 | 
| locid.h |  UBool icu::Locale::Iterator::hasNext() const | DraftICU 65 | 
| locid.h |  UBool icu::Locale::RangeIterator&lt; Iter &gt;::hasNext() const override | DraftICU 65 | 
| locid.h |  const Locale&amp; icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::next() override | DraftICU 65 | 
| locid.h |  const Locale&amp; icu::Locale::Iterator::next() | DraftICU 65 | 
| locid.h |  const Locale&amp; icu::Locale::RangeIterator&lt; Iter &gt;::next() override | DraftICU 65 | 
| locid.h |  icu::Locale::ConvertingIterator&lt; Iter, Conv &gt;::ConvertingIterator(Iter, Iter, Conv) | DraftICU 65 | 
| locid.h |  icu::Locale::Iterator::~Iterator() | DraftICU 65 | 
| locid.h |  icu::Locale::RangeIterator&lt; Iter &gt;::RangeIterator(Iter, Iter) | DraftICU 65 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcre() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcreFoot() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAmpere() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcMinute() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcSecond() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAstronomicalUnit() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getAtmosphere() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBar() | DraftICU 65 | 
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
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecade() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDeciliter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecimeter() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDegree() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerCentimeter() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerInch() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDunam() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthMass() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getElectronvolt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getEm() | DraftICU 65 | 
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
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapixel() | DraftICU 65 | 
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
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPascal() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPercent() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermille() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermyriad() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPetabyte() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPicometer() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPint() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintMetric() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixel() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerCentimeter() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerInch() | DraftICU 65 | 
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
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getThermUs() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTon() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getVolt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWatt() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeek() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeekPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYard() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYear() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getYearPerson() | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBar(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBarrel(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnit(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDalton(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDayPerson(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDecade(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerCentimeter(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerInch(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDunam(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthMass(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createElectronvolt(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEm(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceImperial(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilopascal(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapascal(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapixel(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMole(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMonthPerson(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewton(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewtonMeter(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPascal(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermyriad(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixel(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerCentimeter(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerInch(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundFoot(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundForce(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarLuminosity(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarMass(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarRadius(UErrorCode&amp;) | DraftICU 64 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createThermUs(UErrorCode&amp;) | DraftICU 65 | 
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
| numberformatter.h |  StringClass icu::number::FormattedNumber::toDecimalNumber(UErrorCode&amp;) const | DraftICU 65 | 
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
| reldatefmt.h |  <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_HOUR | DraftICU 65 | 
| reldatefmt.h |  <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_MINUTE | DraftICU 65 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::FormattedRelativeDateTime() | DraftICU 64 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&amp;&amp;) | DraftICU 64 | 
| reldatefmt.h |  icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() | DraftICU 64 | 
| stringpiece.h |  icu::StringPiece::StringPiece(T) | DraftICU 65 | 
| ucal.h |  int32_t ucal_getHostTimeZone(UChar*, int32_t, UErrorCode*) | DraftICU 65 | 
| ucharstrie.h |  UCharsTrie&amp; icu::UCharsTrie::resetToState64(uint64_t) | DraftICU 65 | 
| ucharstrie.h |  uint64_t icu::UCharsTrie::getState64() const | DraftICU 65 | 
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
| uloc.h |  UEnumeration* uloc_openAvailableByType(ULocAvailableType, UErrorCode*) | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_DEFAULT | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_ONLY_LEGACY_ALIASES | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_WITH_LEGACY_ALIASES | DraftICU 65 | 
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
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_BUNDLE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_DATA_FILE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RESOURCE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RES_FILE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_START | DraftICU 65 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Wed Feb 19 10:40:39 PST 2020

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  
