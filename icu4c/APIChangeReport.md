
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 66 with ICU 67

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 66](#removed)
- [Deprecated or Obsoleted in ICU 67](#deprecated)
- [Changed in  ICU 67](#changed)
- [Promoted to stable in ICU 67](#promoted)
- [Added in ICU 67](#added)
- [Other existing drafts in ICU 67](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 66
  
| File | API | ICU 66 | ICU 67 |
|---|---|---|---|
| listformatter.h | UnicodeString&amp; icu::ListFormatter::format(const UnicodeString items[], int32_t, UnicodeString&amp;, FieldPositionIterator*, UErrorCode&amp;) const |  DraftICU 63 | (missing)
| numberformatter.h | UBool icu::number::FormattedNumber::nextFieldPosition(FieldPosition&amp;, UErrorCode&amp;) const |  DraftICU 62 | (missing)
| numberformatter.h | void icu::number::FormattedNumber::getAllFieldPositions(FieldPositionIterator&amp;, UErrorCode&amp;) const |  DraftICU 62 | (missing)
| numberrangeformatter.h | UBool icu::number::FormattedNumberRange::nextFieldPosition(FieldPosition&amp;, UErrorCode&amp;) const |  DraftICU 63 | (missing)
| numberrangeformatter.h | void icu::number::FormattedNumberRange::getAllFieldPositions(FieldPositionIterator&amp;, UErrorCode&amp;) const |  DraftICU 63 | (missing)
| udateintervalformat.h | void udtitvfmt_formatToResult(const UDateIntervalFormat*, UFormattedDateInterval*, UDate, UDate, UErrorCode*) |  DraftICU 64 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 67
  
| File | API | ICU 66 | ICU 67 |
|---|---|---|---|

## Changed

Changed in  ICU 67 (old, new)


  
| File | API | ICU 66 | ICU 67 |
|---|---|---|---|
| currunit.h | icu::CurrencyUnit::CurrencyUnit(StringPiece, UErrorCode&amp;) |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isFormatFailIfMoreThanMaxDigits() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isParseCaseSensitive() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isParseNoExponent() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isSignAlwaysShown() const |  Draft→StableICU 64
| decimfmt.h | const number::LocalizedNumberFormatter* icu::DecimalFormat::toNumberFormatter(UErrorCode&amp;) const |  Draft→StableICU 64
| decimfmt.h | int32_t icu::DecimalFormat::getMinimumGroupingDigits() const |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setFormatFailIfMoreThanMaxDigits(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setMinimumGroupingDigits(int32_t) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setParseCaseSensitive(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setParseNoExponent(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setSignAlwaysShown(UBool) |  Draft→StableICU 64
| dtitvfmt.h | Appendable&amp; icu::FormattedDateInterval::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval icu::DateIntervalFormat::formatToValue(Calendar&amp;, Calendar&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval icu::DateIntervalFormat::formatToValue(const DateInterval&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval&amp; icu::FormattedDateInterval::operator=(FormattedDateInterval&amp;&amp;) |  Draft→StableICU 64
| dtitvfmt.h | UBool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | UnicodeString icu::FormattedDateInterval::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::FormattedDateInterval() |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&amp;&amp;) |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::~FormattedDateInterval() |  Draft→StableICU 64
| formattedvalue.h | Appendable&amp; icu::FormattedValue::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UBool icu::ConstrainedFieldPosition::matchesField(int32_t, int32_t) const |  Draft→StableICU 64
| formattedvalue.h | UBool icu::FormattedValue::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UnicodeString icu::FormattedValue::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UnicodeString icu::FormattedValue::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | icu::ConstrainedFieldPosition::ConstrainedFieldPosition() |  Draft→StableICU 64
| formattedvalue.h | icu::ConstrainedFieldPosition::~ConstrainedFieldPosition() |  Draft→StableICU 64
| formattedvalue.h | icu::FormattedValue::~FormattedValue() |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getCategory() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getField() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getLimit() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getStart() const |  Draft→StableICU 64
| formattedvalue.h | int64_t icu::ConstrainedFieldPosition::getInt64IterationContext() const |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::constrainCategory(int32_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::constrainField(int32_t, int32_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::reset() |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::setInt64IterationContext(int64_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::setState(int32_t, int32_t, int32_t, int32_t) |  Draft→StableICU 64
| listformatter.h | Appendable&amp; icu::FormattedList::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | FormattedList icu::ListFormatter::formatStringsToValue(const UnicodeString items[], int32_t, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | FormattedList&amp; icu::FormattedList::operator=(FormattedList&amp;&amp;) |  Draft→StableICU 64
| listformatter.h | UBool icu::FormattedList::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | UnicodeString icu::FormattedList::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | UnicodeString icu::FormattedList::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::FormattedList() |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::FormattedList(FormattedList&amp;&amp;) |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::~FormattedList() |  Draft→StableICU 64
| localebuilder.h | Locale icu::LocaleBuilder::build(UErrorCode&amp;) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::addUnicodeLocaleAttribute(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::clear() |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::clearExtensions() |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::removeUnicodeLocaleAttribute(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setExtension(char, StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLanguage(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLanguageTag(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLocale(const Locale&amp;) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setRegion(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setScript(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setUnicodeLocaleKeyword(StringPiece, StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setVariant(StringPiece) |  Draft→StableICU 64
| localebuilder.h | icu::LocaleBuilder::LocaleBuilder() |  Draft→StableICU 64
| localebuilder.h | icu::LocaleBuilder::~LocaleBuilder() |  Draft→StableICU 64
| localpointer.h | LocalArray&lt;T&gt;&amp; icu::LocalArray&lt; T &gt;::operator=(std::unique_ptr&lt; T[]&gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | LocalPointer&lt;T&gt;&amp; icu::LocalPointer&lt; T &gt;::operator=(std::unique_ptr&lt; T &gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalArray&lt; T &gt;::LocalArray(std::unique_ptr&lt; T[]&gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalArray&lt; T &gt;::operator std::unique_ptr&lt; T[]&gt;() &amp;&amp; |  Draft→StableICU 64
| localpointer.h | icu::LocalPointer&lt; T &gt;::LocalPointer(std::unique_ptr&lt; T &gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalPointer&lt; T &gt;::operator std::unique_ptr&lt; T &gt;() &amp;&amp; |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcre() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcreFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAmpere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcMinute() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAstronomicalUnit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAtmosphere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBarrel() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBritishThermalUnit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBushel() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getByte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCarat() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCelsius() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentiliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentury() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCup() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupMetric() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDalton() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDay() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDayPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDeciliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDegree() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDunam() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthMass() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getElectronvolt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFahrenheit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFathom() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunceImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoodcalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFurlong() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGForce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallonImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGenericTemperature() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigahertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigawatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectare() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectoliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectopascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHorsepower() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getInchHg() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getJoule() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKarat() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKelvin() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilocalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilogram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilohertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilojoule() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometerPerHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilopascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKnot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightYear() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPer100Kilometers() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPerKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLux() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegahertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegaliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegawatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecondSquared() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMetricTon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrogram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrosecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallonImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMileScandinavian() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliampere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillibar() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramPerDeciliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeterOfMercury() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimolePerLiter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillisecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliwatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMinute() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMole() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonth() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonthPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanosecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNauticalMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewton() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewtonMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOhm() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunceTroy() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getParsec() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPerMillion() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPercent() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermille() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermyriad() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPetabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPicometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPint() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintMetric() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoint() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPound() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundForce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundPerSquareInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuart() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRadian() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRevolutionAngle() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarLuminosity() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarMass() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarRadius() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getStone() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTablespoon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTeaspoon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getVolt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeek() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeekPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYear() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYearPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBarrel(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnit(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDalton(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDayPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDunam(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthMass(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createElectronvolt(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceImperial(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilopascal(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapascal(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMole(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMonthPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewton(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewtonMeter(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermyriad(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundFoot(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundForce(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarLuminosity(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarMass(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarRadius(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createWeekPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createYearPerson(UErrorCode&amp;) |  Draft→StableICU 64
| numberformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; |  Draft→StableICU 64
| numberformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() const &amp; |  Draft→StableICU 64
| numberformatter.h | UBool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| numberformatter.h | UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| numberformatter.h | icu::number::FormattedNumber::FormattedNumber() |  Draft→StableICU 64
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::forSkeleton(const UnicodeString&amp;, UParseError&amp;, UErrorCode&amp;) |  Draft→StableICU 64
| numberrangeformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; |  Draft→StableICU 64
| numberrangeformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() const &amp; |  Draft→StableICU 64
| numberrangeformatter.h | UBool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kCompactField |  Draft→StableICU 64
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kMeasureUnitField |  Draft→StableICU 64
| plurrule.h | UnicodeString icu::PluralRules::select(const number::FormattedNumber&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | Appendable&amp; icu::FormattedRelativeDateTime::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatNumericToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(UDateDirection, UDateAbsoluteUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, UDateDirection, UDateRelativeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime&amp; icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&amp;&amp;) |  Draft→StableICU 64
| reldatefmt.h | UBool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::FormattedRelativeDateTime() |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&amp;&amp;) |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() |  Draft→StableICU 64
| udateintervalformat.h | UFormattedDateInterval* udtitvfmt_openResult(UErrorCode*) |  Draft→StableICU 64
| udateintervalformat.h | const UFormattedValue* udtitvfmt_resultAsValue(const UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 64
| udateintervalformat.h | void udtitvfmt_closeResult(UFormattedDateInterval*) |  Draft→StableICU 64
| uformattedvalue.h | UBool ucfpos_matchesField(const UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | UBool ufmtval_nextPosition(const UFormattedValue*, UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | UConstrainedFieldPosition* ucfpos_open(UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | const UChar* ufmtval_getString(const UFormattedValue*, int32_t*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE_INTERVAL_SPAN |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST_SPAN |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_RELATIVE_DATETIME |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_UNDEFINED |  Draft→StableICU 64
| uformattedvalue.h | int32_t ucfpos_getCategory(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | int32_t ucfpos_getField(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | int64_t ucfpos_getInt64IterationContext(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_close(UConstrainedFieldPosition*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_constrainCategory(UConstrainedFieldPosition*, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_constrainField(UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_getIndexes(const UConstrainedFieldPosition*, int32_t*, int32_t*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_reset(UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_setInt64IterationContext(UConstrainedFieldPosition*, int64_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_setState(UConstrainedFieldPosition*, int32_t, int32_t, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | UFormattedList* ulistfmt_openResult(UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | const UFormattedValue* ulistfmt_resultAsValue(const UFormattedList*, UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | <tt>enum</tt> UListFormatterField::ULISTFMT_ELEMENT_FIELD |  Draft→StableICU 63
| ulistformatter.h | <tt>enum</tt> UListFormatterField::ULISTFMT_LITERAL_FIELD |  Draft→StableICU 63
| ulistformatter.h | void ulistfmt_closeResult(UFormattedList*) |  Draft→StableICU 64
| ulistformatter.h | void ulistfmt_formatStringsToResult(const UListFormatter*, const UChar* const strings[], const int32_t*, int32_t, UFormattedList*, UErrorCode*) |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_MINIMUM_GROUPING_DIGITS |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_PARSE_CASE_SENSITIVE |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_SIGN_ALWAYS_SHOWN |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_COMPACT_FIELD |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_MEASURE_UNIT_FIELD |  Draft→StableICU 64
| unumberformatter.h | UNumberFormatter* unumf_openForSkeletonAndLocaleWithError(const UChar*, int32_t, const char*, UParseError*, UErrorCode*) |  Draft→StableICU 64
| unumberformatter.h | const UFormattedValue* unumf_resultAsValue(const UFormattedNumber*, UErrorCode*) |  Draft→StableICU 64
| upluralrules.h | int32_t uplrules_selectFormatted(const UPluralRules*, const struct UFormattedNumber*, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | UFormattedRelativeDateTime* ureldatefmt_openResult(UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | const UFormattedValue* ureldatefmt_resultAsValue(const UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_LITERAL_FIELD |  Draft→StableICU 64
| ureldatefmt.h | <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_NUMERIC_FIELD |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_closeResult(UFormattedRelativeDateTime*) |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_formatNumericToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_formatToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64

## Promoted

Promoted to stable in ICU 67
  
| File | API | ICU 66 | ICU 67 |
|---|---|---|---|
| currunit.h | icu::CurrencyUnit::CurrencyUnit(StringPiece, UErrorCode&amp;) |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isFormatFailIfMoreThanMaxDigits() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isParseCaseSensitive() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isParseNoExponent() const |  Draft→StableICU 64
| decimfmt.h | UBool icu::DecimalFormat::isSignAlwaysShown() const |  Draft→StableICU 64
| decimfmt.h | const number::LocalizedNumberFormatter* icu::DecimalFormat::toNumberFormatter(UErrorCode&amp;) const |  Draft→StableICU 64
| decimfmt.h | int32_t icu::DecimalFormat::getMinimumGroupingDigits() const |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setFormatFailIfMoreThanMaxDigits(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setMinimumGroupingDigits(int32_t) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setParseCaseSensitive(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setParseNoExponent(UBool) |  Draft→StableICU 64
| decimfmt.h | void icu::DecimalFormat::setSignAlwaysShown(UBool) |  Draft→StableICU 64
| dtitvfmt.h | Appendable&amp; icu::FormattedDateInterval::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval icu::DateIntervalFormat::formatToValue(Calendar&amp;, Calendar&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval icu::DateIntervalFormat::formatToValue(const DateInterval&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | FormattedDateInterval&amp; icu::FormattedDateInterval::operator=(FormattedDateInterval&amp;&amp;) |  Draft→StableICU 64
| dtitvfmt.h | UBool icu::FormattedDateInterval::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | UnicodeString icu::FormattedDateInterval::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | UnicodeString icu::FormattedDateInterval::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::FormattedDateInterval() |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::FormattedDateInterval(FormattedDateInterval&amp;&amp;) |  Draft→StableICU 64
| dtitvfmt.h | icu::FormattedDateInterval::~FormattedDateInterval() |  Draft→StableICU 64
| formattedvalue.h | Appendable&amp; icu::FormattedValue::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UBool icu::ConstrainedFieldPosition::matchesField(int32_t, int32_t) const |  Draft→StableICU 64
| formattedvalue.h | UBool icu::FormattedValue::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UnicodeString icu::FormattedValue::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | UnicodeString icu::FormattedValue::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| formattedvalue.h | icu::ConstrainedFieldPosition::ConstrainedFieldPosition() |  Draft→StableICU 64
| formattedvalue.h | icu::ConstrainedFieldPosition::~ConstrainedFieldPosition() |  Draft→StableICU 64
| formattedvalue.h | icu::FormattedValue::~FormattedValue() |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getCategory() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getField() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getLimit() const |  Draft→StableICU 64
| formattedvalue.h | int32_t icu::ConstrainedFieldPosition::getStart() const |  Draft→StableICU 64
| formattedvalue.h | int64_t icu::ConstrainedFieldPosition::getInt64IterationContext() const |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::constrainCategory(int32_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::constrainField(int32_t, int32_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::reset() |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::setInt64IterationContext(int64_t) |  Draft→StableICU 64
| formattedvalue.h | void icu::ConstrainedFieldPosition::setState(int32_t, int32_t, int32_t, int32_t) |  Draft→StableICU 64
| listformatter.h | Appendable&amp; icu::FormattedList::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | FormattedList icu::ListFormatter::formatStringsToValue(const UnicodeString items[], int32_t, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | FormattedList&amp; icu::FormattedList::operator=(FormattedList&amp;&amp;) |  Draft→StableICU 64
| listformatter.h | UBool icu::FormattedList::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | UnicodeString icu::FormattedList::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | UnicodeString icu::FormattedList::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::FormattedList() |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::FormattedList(FormattedList&amp;&amp;) |  Draft→StableICU 64
| listformatter.h | icu::FormattedList::~FormattedList() |  Draft→StableICU 64
| localebuilder.h | Locale icu::LocaleBuilder::build(UErrorCode&amp;) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::addUnicodeLocaleAttribute(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::clear() |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::clearExtensions() |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::removeUnicodeLocaleAttribute(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setExtension(char, StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLanguage(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLanguageTag(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setLocale(const Locale&amp;) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setRegion(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setScript(StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setUnicodeLocaleKeyword(StringPiece, StringPiece) |  Draft→StableICU 64
| localebuilder.h | LocaleBuilder&amp; icu::LocaleBuilder::setVariant(StringPiece) |  Draft→StableICU 64
| localebuilder.h | icu::LocaleBuilder::LocaleBuilder() |  Draft→StableICU 64
| localebuilder.h | icu::LocaleBuilder::~LocaleBuilder() |  Draft→StableICU 64
| localpointer.h | LocalArray&lt;T&gt;&amp; icu::LocalArray&lt; T &gt;::operator=(std::unique_ptr&lt; T[]&gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | LocalPointer&lt;T&gt;&amp; icu::LocalPointer&lt; T &gt;::operator=(std::unique_ptr&lt; T &gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalArray&lt; T &gt;::LocalArray(std::unique_ptr&lt; T[]&gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalArray&lt; T &gt;::operator std::unique_ptr&lt; T[]&gt;() &amp;&amp; |  Draft→StableICU 64
| localpointer.h | icu::LocalPointer&lt; T &gt;::LocalPointer(std::unique_ptr&lt; T &gt;&amp;&amp;) |  Draft→StableICU 64
| localpointer.h | icu::LocalPointer&lt; T &gt;::operator std::unique_ptr&lt; T &gt;() &amp;&amp; |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcre() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAcreFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAmpere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcMinute() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getArcSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAstronomicalUnit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getAtmosphere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBarrel() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBritishThermalUnit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBushel() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getByte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCarat() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCelsius() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentiliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCentury() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCubicYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCup() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupMetric() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDalton() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDay() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDayPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDeciliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDegree() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDunam() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getEarthMass() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getElectronvolt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFahrenheit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFathom() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunceImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoodcalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFurlong() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGForce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGallonImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGenericTemperature() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigahertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGigawatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectare() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectoliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHectopascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHorsepower() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getInchHg() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getJoule() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKarat() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKelvin() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilobyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilocalorie() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilogram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilohertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilojoule() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilometerPerHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilopascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilowattHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKnot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightYear() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPer100Kilometers() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLiterPerKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLux() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegahertz() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegaliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapascal() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegawatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMeterPerSecondSquared() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMetricTon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrogram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMicrosecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerGallonImperial() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilePerHour() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMileScandinavian() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliampere() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillibar() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligram() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramPerDeciliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliliter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimeterOfMercury() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillimolePerLiter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMillisecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliwatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMinute() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMole() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonth() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMonthPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNanosecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNauticalMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewton() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNewtonMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOhm() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOunceTroy() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getParsec() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPerMillion() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPercent() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermille() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPermyriad() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPetabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPicometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPint() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintMetric() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoint() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPound() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundForce() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundPerSquareInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuart() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRadian() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRevolutionAngle() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSecond() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarLuminosity() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarMass() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSolarRadius() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareCentimeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareFoot() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareInch() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareKilometer() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMeter() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareMile() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSquareYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getStone() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTablespoon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTeaspoon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabit() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTerabyte() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTon() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getVolt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWatt() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeek() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeekPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYard() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYear() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getYearPerson() |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBarrel(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnit(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDalton(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDayPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDunam(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEarthMass(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createElectronvolt(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceImperial(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilopascal(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapascal(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMole(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMonthPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewton(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNewtonMeter(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPermyriad(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundFoot(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundForce(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarLuminosity(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarMass(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSolarRadius(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createWeekPerson(UErrorCode&amp;) |  Draft→StableICU 64
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createYearPerson(UErrorCode&amp;) |  Draft→StableICU 64
| numberformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; |  Draft→StableICU 64
| numberformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberFormatterSettings&lt; Derived &gt;::clone() const &amp; |  Draft→StableICU 64
| numberformatter.h | UBool icu::number::FormattedNumber::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| numberformatter.h | UnicodeString icu::number::FormattedNumber::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| numberformatter.h | icu::number::FormattedNumber::FormattedNumber() |  Draft→StableICU 64
| numberformatter.h | <tt>static</tt> UnlocalizedNumberFormatter icu::number::NumberFormatter::forSkeleton(const UnicodeString&amp;, UParseError&amp;, UErrorCode&amp;) |  Draft→StableICU 64
| numberrangeformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() &amp;&amp; |  Draft→StableICU 64
| numberrangeformatter.h | LocalPointer&lt;Derived&gt; icu::number::NumberRangeFormatterSettings&lt; Derived &gt;::clone() const &amp; |  Draft→StableICU 64
| numberrangeformatter.h | UBool icu::number::FormattedNumberRange::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| numberrangeformatter.h | UnicodeString icu::number::FormattedNumberRange::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kCompactField |  Draft→StableICU 64
| numfmt.h | <tt>enum</tt>  							icu::NumberFormat::EAlignmentFields::kMeasureUnitField |  Draft→StableICU 64
| numsys.h | NumberingSystem&amp; icu::NumberingSystem::operator=(const NumberingSystem&amp;)=default |  (missing) | StableICU 4.2
| plurrule.h | UnicodeString icu::PluralRules::select(const number::FormattedNumber&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | Appendable&amp; icu::FormattedRelativeDateTime::appendTo(Appendable&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatNumericToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(UDateDirection, UDateAbsoluteUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, UDateDirection, UDateRelativeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime icu::RelativeDateTimeFormatter::formatToValue(double, URelativeDateTimeUnit, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | FormattedRelativeDateTime&amp; icu::FormattedRelativeDateTime::operator=(FormattedRelativeDateTime&amp;&amp;) |  Draft→StableICU 64
| reldatefmt.h | UBool icu::FormattedRelativeDateTime::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | UnicodeString icu::FormattedRelativeDateTime::toString(UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | UnicodeString icu::FormattedRelativeDateTime::toTempString(UErrorCode&amp;) const |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::FormattedRelativeDateTime() |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::FormattedRelativeDateTime(FormattedRelativeDateTime&amp;&amp;) |  Draft→StableICU 64
| reldatefmt.h | icu::FormattedRelativeDateTime::~FormattedRelativeDateTime() |  Draft→StableICU 64
| udateintervalformat.h | UFormattedDateInterval* udtitvfmt_openResult(UErrorCode*) |  Draft→StableICU 64
| udateintervalformat.h | const UFormattedValue* udtitvfmt_resultAsValue(const UFormattedDateInterval*, UErrorCode*) |  Draft→StableICU 64
| udateintervalformat.h | void udtitvfmt_closeResult(UFormattedDateInterval*) |  Draft→StableICU 64
| uformattedvalue.h | UBool ucfpos_matchesField(const UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | UBool ufmtval_nextPosition(const UFormattedValue*, UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | UConstrainedFieldPosition* ucfpos_open(UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | const UChar* ufmtval_getString(const UFormattedValue*, int32_t*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE_INTERVAL_SPAN |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_DATE |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST_SPAN |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_LIST |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_NUMBER |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_RELATIVE_DATETIME |  Draft→StableICU 64
| uformattedvalue.h | <tt>enum</tt> UFieldCategory::UFIELD_CATEGORY_UNDEFINED |  Draft→StableICU 64
| uformattedvalue.h | int32_t ucfpos_getCategory(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | int32_t ucfpos_getField(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | int64_t ucfpos_getInt64IterationContext(const UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_close(UConstrainedFieldPosition*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_constrainCategory(UConstrainedFieldPosition*, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_constrainField(UConstrainedFieldPosition*, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_getIndexes(const UConstrainedFieldPosition*, int32_t*, int32_t*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_reset(UConstrainedFieldPosition*, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_setInt64IterationContext(UConstrainedFieldPosition*, int64_t, UErrorCode*) |  Draft→StableICU 64
| uformattedvalue.h | void ucfpos_setState(UConstrainedFieldPosition*, int32_t, int32_t, int32_t, int32_t, UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | UFormattedList* ulistfmt_openResult(UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | const UFormattedValue* ulistfmt_resultAsValue(const UFormattedList*, UErrorCode*) |  Draft→StableICU 64
| ulistformatter.h | <tt>enum</tt> UListFormatterField::ULISTFMT_ELEMENT_FIELD |  Draft→StableICU 63
| ulistformatter.h | <tt>enum</tt> UListFormatterField::ULISTFMT_LITERAL_FIELD |  Draft→StableICU 63
| ulistformatter.h | void ulistfmt_closeResult(UFormattedList*) |  Draft→StableICU 64
| ulistformatter.h | void ulistfmt_formatStringsToResult(const UListFormatter*, const UChar* const strings[], const int32_t*, int32_t, UFormattedList*, UErrorCode*) |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_MINIMUM_GROUPING_DIGITS |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_PARSE_CASE_SENSITIVE |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatAttribute::UNUM_SIGN_ALWAYS_SHOWN |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_COMPACT_FIELD |  Draft→StableICU 64
| unum.h | <tt>enum</tt> UNumberFormatFields::UNUM_MEASURE_UNIT_FIELD |  Draft→StableICU 64
| unumberformatter.h | UNumberFormatter* unumf_openForSkeletonAndLocaleWithError(const UChar*, int32_t, const char*, UParseError*, UErrorCode*) |  Draft→StableICU 64
| unumberformatter.h | const UFormattedValue* unumf_resultAsValue(const UFormattedNumber*, UErrorCode*) |  Draft→StableICU 64
| upluralrules.h | int32_t uplrules_selectFormatted(const UPluralRules*, const struct UFormattedNumber*, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | UFormattedRelativeDateTime* ureldatefmt_openResult(UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | const UFormattedValue* ureldatefmt_resultAsValue(const UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_LITERAL_FIELD |  Draft→StableICU 64
| ureldatefmt.h | <tt>enum</tt> URelativeDateTimeFormatterField::UDAT_REL_NUMERIC_FIELD |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_closeResult(UFormattedRelativeDateTime*) |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_formatNumericToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64
| ureldatefmt.h | void ureldatefmt_formatToResult(const URelativeDateTimeFormatter*, double, URelativeDateTimeUnit, UFormattedRelativeDateTime*, UErrorCode*) |  Draft→StableICU 64

## Added

Added in ICU 67
  
| File | API | ICU 66 | ICU 67 |
|---|---|---|---|
| bytestream.h | void icu::ByteSink::AppendU8(const char*, int32_t) |  (missing) | DraftICU 67
| bytestream.h | void icu::ByteSink::AppendU8(const char8_t*, int32_t) |  (missing) | DraftICU 67
| dcfmtsym.h | void icu::DecimalFormatSymbols::setCurrency(const UChar*, UErrorCode&amp;) |  (missing) | Internal
| dtptngen.h | UDateFormatHourCycle icu::DateTimePatternGenerator::getDefaultHourCycle(UErrorCode&amp;) const |  (missing) | DraftICU 67
| listformatter.h | <tt>static</tt> ListFormatter* icu::ListFormatter::createInstance(const Locale&amp;, UListFormatterType, UListFormatterWidth, UErrorCode&amp;) |  (missing) | DraftICU 67
| localematcher.h | Builder&amp; icu::LocaleMatcher::Builder::setDirection(ULocMatchDirection) |  (missing) | DraftICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_ONLY_TWO_WAY |  (missing) | DraftICU 67
| localematcher.h | <tt>enum</tt> ULocMatchDirection::ULOCMATCH_DIRECTION_WITH_ONE_WAY |  (missing) | DraftICU 67
| locid.h | void icu::Locale::canonicalize(UErrorCode&amp;) |  (missing) | DraftICU 67
| measunit.h | LocalArray&lt;MeasureUnit&gt; icu::MeasureUnit::splitToSingleUnits(int32_t&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::product(const MeasureUnit&amp;, UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::reciprocal(UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::withDimensionality(int32_t, UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | MeasureUnit icu::MeasureUnit::withSIPrefix(UMeasureSIPrefix, UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | MeasureUnit&amp; icu::MeasureUnit::operator=(MeasureUnit&amp;&amp;) noexcept |  (missing) | DraftICU 67
| measunit.h | UMeasureSIPrefix icu::MeasureUnit::getSIPrefix(UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | UMeasureUnitComplexity icu::MeasureUnit::getComplexity(UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | const char* icu::MeasureUnit::getIdentifier() const |  (missing) | DraftICU 67
| measunit.h | icu::MeasureUnit::MeasureUnit(MeasureUnit&amp;&amp;) noexcept |  (missing) | DraftICU 67
| measunit.h | int32_t icu::MeasureUnit::getDimensionality(UErrorCode&amp;) const |  (missing) | DraftICU 67
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::forIdentifier(StringPiece, UErrorCode&amp;) |  (missing) | DraftICU 67
| numsys.h | NumberingSystem&amp; icu::NumberingSystem::operator=(const NumberingSystem&amp;)=default |  (missing) | StableICU 4.2
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*) |  (missing) | DraftICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const char8_t*, int32_t) |  (missing) | DraftICU 67
| stringpiece.h | icu::StringPiece::StringPiece(const std::u8string&amp;) |  (missing) | DraftICU 67
| stringpiece.h | icu::StringPiece::StringPiece(std::nullptr_t) |  (missing) | DraftICU 67
| stringpiece.h | int32_t icu::StringPiece::compare(StringPiece) |  (missing) | DraftICU 67
| stringpiece.h | int32_t icu::StringPiece::find(StringPiece, int32_t) |  (missing) | DraftICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*) |  (missing) | DraftICU 67
| stringpiece.h | void icu::StringPiece::set(const char8_t*, int32_t) |  (missing) | DraftICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_11 |  (missing) | DraftICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_12 |  (missing) | DraftICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_23 |  (missing) | DraftICU 67
| udat.h | <tt>enum</tt> UDateFormatHourCycle::UDAT_HOUR_CYCLE_24 |  (missing) | DraftICU 67
| udateintervalformat.h | void udtitvfmt_formatCalendarToResult(const UDateIntervalFormat*, UCalendar*, UCalendar*, UFormattedDateInterval*, UErrorCode*) |  (missing) | DraftICU 67
| udateintervalformat.h | void udtitvfmt_formatToResult(const UDateIntervalFormat*, UDate, UDate, UFormattedDateInterval*, UErrorCode*) |  (missing) | DraftICU 67
| udatpg.h | UDateFormatHourCycle udatpg_getDefaultHourCycle(const UDateTimePatternGenerator*, UErrorCode*) |  (missing) | DraftICU 67
| ulistformatter.h | UListFormatter* ulistfmt_openForType(const char*, UListFormatterType, UListFormatterWidth, UErrorCode*) |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterType::ULISTFMT_TYPE_AND |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterType::ULISTFMT_TYPE_OR |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterType::ULISTFMT_TYPE_UNITS |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterWidth::ULISTFMT_WIDTH_NARROW |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterWidth::ULISTFMT_WIDTH_SHORT |  (missing) | DraftICU 67
| ulistformatter.h | <tt>enum</tt> UListFormatterWidth::ULISTFMT_WIDTH_WIDE |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_BREAK_ENGINE |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_CHARACTER |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_LINE |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_SENTENCE |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_TITLE |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_CREATE_WORD |  (missing) | DraftICU 67
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_LIMIT |  (missing) | Internal
| utrace.h | <tt>enum</tt> UTraceFunctionNumber::UTRACE_UBRK_START |  (missing) | DraftICU 67

## Other

Other existing drafts in ICU 67

| File | API | ICU 66 | ICU 67 |
|---|---|---|---|
| bytestrie.h |  BytesTrie&amp; icu::BytesTrie::resetToState64(uint64_t) | DraftICU 65 | 
| bytestrie.h |  uint64_t icu::BytesTrie::getState64() const | DraftICU 65 | 
| localebuilder.h |  UBool icu::LocaleBuilder::copyErrorTo(UErrorCode&amp;) const | DraftICU 65 | 
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
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBar() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDecade() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerCentimeter() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getDotPerInch() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getEm() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getMegapixel() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPascal() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixel() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerCentimeter() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPixelPerInch() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getThermUs() | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBar(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDecade(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerCentimeter(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDotPerInch(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createEm(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMegapixel(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPascal(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixel(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerCentimeter(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPixelPerInch(UErrorCode&amp;) | DraftICU 65 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createThermUs(UErrorCode&amp;) | DraftICU 65 | 
| nounit.h |  UClassID icu::NoUnit::getDynamicClassID() const | DraftICU 60 | 
| nounit.h |  icu::NoUnit::NoUnit(const NoUnit&amp;) | DraftICU 60 | 
| nounit.h |  icu::NoUnit::~NoUnit() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::base() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::percent() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> NoUnit icu::NoUnit::permille() | DraftICU 60 | 
| nounit.h |  <tt>static</tt> UClassID icu::NoUnit::getStaticClassID() | DraftICU 60 | 
| nounit.h |  void* icu::NoUnit::clone() const | DraftICU 60 | 
| numberformatter.h |  StringClass icu::number::FormattedNumber::toDecimalNumber(UErrorCode&amp;) const | DraftICU 65 | 
| numberrangeformatter.h |  UnicodeString icu::number::FormattedNumberRange::getFirstDecimal(UErrorCode&amp;) const | DraftICU 63 | 
| numberrangeformatter.h |  UnicodeString icu::number::FormattedNumberRange::getSecondDecimal(UErrorCode&amp;) const | DraftICU 63 | 
| reldatefmt.h |  <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_HOUR | DraftICU 65 | 
| reldatefmt.h |  <tt>enum</tt> UDateAbsoluteUnit::UDAT_ABSOLUTE_MINUTE | DraftICU 65 | 
| stringpiece.h |  icu::StringPiece::StringPiece(T) | DraftICU 65 | 
| ucal.h |  int32_t ucal_getHostTimeZone(UChar*, int32_t, UErrorCode*) | DraftICU 65 | 
| ucharstrie.h |  UCharsTrie&amp; icu::UCharsTrie::resetToState64(uint64_t) | DraftICU 65 | 
| ucharstrie.h |  uint64_t icu::UCharsTrie::getState64() const | DraftICU 65 | 
| uloc.h |  UEnumeration* uloc_openAvailableByType(ULocAvailableType, UErrorCode*) | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_DEFAULT | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_ONLY_LEGACY_ALIASES | DraftICU 65 | 
| uloc.h |  <tt>enum</tt> ULocAvailableType::ULOC_AVAILABLE_WITH_LEGACY_ALIASES | DraftICU 65 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_BUNDLE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_DATA_FILE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RESOURCE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_RES_FILE | DraftICU 65 | 
| utrace.h |  <tt>enum</tt> UTraceFunctionNumber::UTRACE_UDATA_START | DraftICU 65 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Fri Apr 03 07:26:48 PDT 2020

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  