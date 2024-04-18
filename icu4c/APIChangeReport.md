
  
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
| gregocal.h | UDate icu::GregorianCalendar::defaultCenturyStart() const |  Internal | (missing)
| gregocal.h | bool icu::GregorianCalendar::haveDefaultCentury() const |  Internal | (missing)
| gregocal.h | int32_t icu::GregorianCalendar::defaultCenturyStartYear() const |  Internal | (missing)
| platform.h | <tt>#define</tt> U_HAVE_INTTYPES_H |  Internal | (missing)
| platform.h | <tt>#define</tt> U_HAVE_STDINT_H |  Internal | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 75
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| simplenumberformatter.h | void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) |  DraftICU 73 | DeprecatedICU 75
| usimplenumberformatter.h | void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) |  DraftICU 73 | DeprecatedICU 75

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
| simplenumberformatter.h | void icu::number::SimpleNumber::roundTo(int32_t, UNumberFormatRoundingMode, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumFractionDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumIntegerDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setSign(USimpleNumberSign, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) |  DraftICU 73 | DeprecatedICU 75
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
| usimplenumberformatter.h | void usnum_roundTo(USimpleNumber*, int32_t, UNumberFormatRoundingMode, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setMinimumFractionDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setMinimumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) |  DraftICU 73 | DeprecatedICU 75
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
| simplenumberformatter.h | void icu::number::SimpleNumber::roundTo(int32_t, UNumberFormatRoundingMode, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumFractionDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 73
| simplenumberformatter.h | void icu::number::SimpleNumber::setMinimumIntegerDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 73
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
| usimplenumberformatter.h | void usnum_roundTo(USimpleNumber*, int32_t, UNumberFormatRoundingMode, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setMinimumFractionDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setMinimumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_close(USimpleNumberFormatter*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_format(const USimpleNumberFormatter*, USimpleNumber*, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73
| usimplenumberformatter.h | void usnumf_formatInt64(const USimpleNumberFormatter*, int64_t, UFormattedNumber*, UErrorCode*) |  Draft→StableICU 73

## Added

Added in ICU 75
  
| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| calendar.h | <tt>#define</tt> DECLARE_OVERRIDE_SYSTEM_DEFAULT_CENTURY |  (missing) | Internal
| caniter.h | <tt>static</tt> void icu::CanonicalIterator::permute(UnicodeString&amp;, bool, Hashtable*, UErrorCode&amp;, int32_t depth=) |  (missing) | Internal
| messageformat2_arguments.h | MessageArguments&amp; icu::message2::MessageArguments::operator=(MessageArguments&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_arguments.h | icu::message2::MessageArguments::MessageArguments()=default |  (missing) | InternalICU 75
| messageformat2_arguments.h | icu::message2::MessageArguments::MessageArguments(const std::map&lt;, Formattable &gt;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_arguments.h | icu::message2::MessageArguments::~MessageArguments() |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder &amp; icu::message2::data_model::Markup::Builder::setClose() |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder &amp; icu::message2::data_model::Markup::Builder::setOpen() |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder &amp; icu::message2::data_model::Markup::Builder::setStandalone() |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addBinding(Binding&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addSelector(Expression&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addUnsupportedStatement(UnsupportedStatement&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addVariant(SelectorKeys&amp;&amp;, Pattern&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::setPattern(Pattern&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Expression::Builder::addAttribute(const UnicodeString&amp;, Operand&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Expression::Builder::setOperand(Operand&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Expression::Builder::setOperator(Operator&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Markup::Builder::addAttribute(const UnicodeString&amp;, Operand&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Markup::Builder::addOption(const UnicodeString&amp;, Operand&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Markup::Builder::setName(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Operator::Builder::addOption(const UnicodeString&amp;, Operand&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Operator::Builder::setFunctionName(FunctionName&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Operator::Builder::setReserved(Reserved&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Pattern::Builder::add(Expression&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Pattern::Builder::add(Markup&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Pattern::Builder::add(UnicodeString&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Reserved::Builder::add(Literal&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::SelectorKeys::Builder::add(Key&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::addExpression(Expression&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::setBody(Reserved&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::setKeyword(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Expression icu::message2::data_model::Expression::Builder::build(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Expression&amp; icu::message2::data_model::Expression::operator=(Expression) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Literal&amp; icu::message2::data_model::Literal::operator=(Literal) |  (missing) | InternalICU 75
| messageformat2_data_model.h | MFDataModel icu::message2::MFDataModel::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | Markup icu::message2::data_model::Markup::Builder::build(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Operator icu::message2::data_model::Operator::Builder::build(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Option&amp; icu::message2::data_model::Option::operator=(Option) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Pattern icu::message2::data_model::Pattern::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | PatternPart&amp; icu::message2::data_model::PatternPart::operator=(PatternPart) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Reserved icu::message2::data_model::Reserved::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | Reserved&amp; icu::message2::data_model::Reserved::operator=(Reserved) |  (missing) | InternalICU 75
| messageformat2_data_model.h | SelectorKeys icu::message2::data_model::SelectorKeys::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | UnicodeString icu::message2::data_model::Literal::quoted() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | UnsupportedStatement icu::message2::data_model::UnsupportedStatement::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | UnsupportedStatement&amp; icu::message2::data_model::UnsupportedStatement::operator=(UnsupportedStatement) |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Expression::isFunctionCall() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Expression::isReserved() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Expression::isStandaloneAnnotation() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Literal::isQuoted() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Literal::operator&lt;(const Literal&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Literal::operator==(const Literal&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Markup::isClose() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Markup::isOpen() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Markup::isStandalone() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::PatternPart::isExpression() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::PatternPart::isMarkup() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::PatternPart::isText() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Expression &amp; icu::message2::data_model::PatternPart::contents() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Literal&amp; icu::message2::data_model::Reserved::getPart(int32_t) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Markup &amp; icu::message2::data_model::PatternPart::asMarkup() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Operand &amp; icu::message2::data_model::Expression::getOperand() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Operand &amp; icu::message2::data_model::Option::getValue() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Operator* icu::message2::data_model::Expression::getOperator(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Reserved* icu::message2::data_model::UnsupportedStatement::getBody(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::Literal::unquoted() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::Markup::getName() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::Option::getName() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::PatternPart::asText() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::UnsupportedStatement::getKeyword() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Expression::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Expression::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Expression::Expression() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Expression::Expression(const Expression&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Expression::~Expression() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Literal::Literal()=default |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Literal::Literal(bool, const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Literal::Literal(const Literal&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Literal::~Literal() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Markup::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Markup::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Markup::Markup() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Markup::~Markup() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operator::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operator::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Option::Option() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Option::Option(const Option&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Option::Option(const UnicodeString&amp;, Operand&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Option::~Option() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Pattern::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Pattern::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::PatternPart()=default |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::PatternPart(Expression&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::PatternPart(Markup&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::PatternPart(const PatternPart&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::PatternPart(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::PatternPart::~PatternPart() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Reserved() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Reserved(const Reserved&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Reserved::~Reserved() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::SelectorKeys::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::SelectorKeys::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::UnsupportedStatement() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::UnsupportedStatement(const UnsupportedStatement&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::~UnsupportedStatement() |  (missing) | InternalICU 75
| messageformat2_data_model.h | int32_t icu::message2::data_model::Reserved::numParts() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Expression &gt; icu::message2::data_model::UnsupportedStatement::getExpressions() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Option &gt; icu::message2::data_model::Expression::getAttributes() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Option &gt; icu::message2::data_model::Markup::getAttributes() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Option &gt; icu::message2::data_model::Markup::getOptions() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | Appendable&amp; icu::message2::FormattedMessage::appendTo(Appendable&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | CharacterIterator* icu::message2::FormattedMessage::toCharacterIterator(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | Formattable&amp; icu::message2::Formattable::operator=(Formattable) |  (missing) | InternalICU 75
| messageformat2_formattable.h | FormattedPlaceholder&amp; icu::message2::FormattedPlaceholder::operator=(FormattedPlaceholder&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | FormattedValue&amp; icu::message2::FormattedValue::operator=(FormattedValue&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | FunctionOptions&amp; icu::message2::FunctionOptions::operator=(FunctionOptions&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | FunctionOptions&amp; icu::message2::FunctionOptions::operator=(const FunctionOptions&amp;)=delete |  (missing) | InternalICU 75
| messageformat2_formattable.h | FunctionOptionsMap icu::message2::FunctionOptions::getOptions() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | StringPiece icu::message2::FormattedMessage::subSequence(int32_t, int32_t, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | UDate icu::message2::Formattable::getDate(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | UFormattableType icu::message2::Formattable::getType() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | UnicodeString icu::message2::FormattedMessage::toString(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | UnicodeString icu::message2::FormattedMessage::toTempString(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | UnicodeString icu::message2::FormattedPlaceholder::formatToString(const Locale&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::Formattable::isNumeric() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedMessage::nextPosition(ConstrainedFieldPosition&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::canFormat() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isEvaluated() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isFallback() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isNullOperand() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedValue::isNumber() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | bool icu::message2::FormattedValue::isString() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | char16_t icu::message2::FormattedMessage::charAt(int32_t, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const Formattable* icu::message2::Formattable::getArray(int32_t&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const FormattableObject* icu::message2::Formattable::getObject(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const FormattedValue &amp; icu::message2::FormattedPlaceholder::output() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const FunctionOptions &amp; icu::message2::FormattedPlaceholder::options() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const UnicodeString &amp; icu::message2::FormattableObject::tag() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const UnicodeString &amp; icu::message2::FormattedPlaceholder::getFallback() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const UnicodeString &amp; icu::message2::FormattedValue::getString() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const UnicodeString&amp; icu::message2::Formattable::getString(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const message2::Formattable &amp; icu::message2::FormattedPlaceholder::asFormattable() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const number::FormattedNumber &amp; icu::message2::FormattedValue::getNumber() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | double icu::message2::Formattable::getDouble(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::Formattable icu::message2::Formattable::asICUFormattable(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(const Formattable&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(const Formattable*, int32_t) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(const FormattableObject*) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(double) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(int64_t) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::~Formattable() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattableObject::~FormattableObject() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedMessage::FormattedMessage(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedMessage::~FormattedMessage() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(FormattedPlaceholder&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const Formattable&amp;, const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder&amp;, FormattedValue&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder&amp;, FunctionOptions&amp;&amp;, FormattedValue&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(FormattedValue&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(number::FormattedNumber&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FormattedValue::~FormattedValue() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FunctionOptions::FunctionOptions() |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FunctionOptions::FunctionOptions(FunctionOptions&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FunctionOptions::~FunctionOptions() |  (missing) | InternalICU 75
| messageformat2_formattable.h | int32_t icu::message2::Formattable::getLong(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | int32_t icu::message2::FormattedMessage::length(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | int64_t icu::message2::Formattable::getInt64(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | int64_t icu::message2::Formattable::getInt64Value(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | <tt>static</tt> Formattable icu::message2::Formattable::forDate(UDate) |  (missing) | InternalICU 75
| messageformat2_formattable.h | <tt>static</tt> Formattable icu::message2::Formattable::forDecimal(std::string_view, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::adoptFormatter(const data_model::FunctionName&amp;, FormatterFactory*, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::adoptSelector(const data_model::FunctionName&amp;, SelectorFactory*, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::setDefaultFormatterNameByType(const UnicodeString&amp;, const data_model::FunctionName&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | FormattedPlaceholder icu::message2::Formatter::format(FormattedPlaceholder&amp;&amp;, FunctionOptions&amp;&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Formatter* icu::message2::FormatterFactory::createFormatter(const Locale&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | FormatterFactory&amp; icu::message2::FormatterFactory::operator=(const FormatterFactory&amp;)=delete |  (missing) | InternalICU 75
| messageformat2_function_registry.h | FormatterFactory* icu::message2::MFFunctionRegistry::getFormatter(const FunctionName&amp;) const |  (missing) | InternalICU 75
| messageformat2_function_registry.h | MFFunctionRegistry icu::message2::MFFunctionRegistry::Builder::build() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | MFFunctionRegistry&amp; icu::message2::MFFunctionRegistry::operator=(MFFunctionRegistry&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Selector* icu::message2::SelectorFactory::createSelector(const Locale&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_function_registry.h | SelectorFactory&amp; icu::message2::SelectorFactory::operator=(const SelectorFactory&amp;)=delete |  (missing) | InternalICU 75
| messageformat2_function_registry.h | bool icu::message2::MFFunctionRegistry::getDefaultFormatterNameByType(const UnicodeString&amp;, FunctionName&amp;) const |  (missing) | InternalICU 75
| messageformat2_function_registry.h | const SelectorFactory* icu::message2::MFFunctionRegistry::getSelector(const FunctionName&amp;) const |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::Formatter::~Formatter() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::FormatterFactory::~FormatterFactory() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::MFFunctionRegistry::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::MFFunctionRegistry::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::MFFunctionRegistry::MFFunctionRegistry(MFFunctionRegistry&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::MFFunctionRegistry::~MFFunctionRegistry() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::Selector::~Selector() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | icu::message2::SelectorFactory::~SelectorFactory() |  (missing) | InternalICU 75
| messageformat2_function_registry.h | void icu::message2::Selector::selectKey(FormattedPlaceholder&amp;&amp;, FunctionOptions&amp;&amp;, const UnicodeString*, int32_t, UnicodeString*, int32_t&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setDataModel(MFDataModel&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setFunctionRegistry(const MFFunctionRegistry&amp;) |  (missing) | InternalICU 75
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setLocale(const Locale&amp;) |  (missing) | InternalICU 75
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setPattern(const UnicodeString&amp;, UParseError&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2.h | FormattedMessage icu::message2::MessageFormatter::format(const MessageArguments&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2.h | MessageFormatter icu::message2::MessageFormatter::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2.h | MessageFormatter&amp; icu::message2::MessageFormatter::operator=(MessageFormatter&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2.h | UnicodeString icu::message2::MessageFormatter::formatToString(const MessageArguments&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2.h | UnicodeString icu::message2::MessageFormatter::getPattern() const |  (missing) | InternalICU 75
| messageformat2.h | const Locale &amp; icu::message2::MessageFormatter::getLocale() const |  (missing) | InternalICU 75
| messageformat2.h | const MFDataModel &amp; icu::message2::MessageFormatter::getDataModel() const |  (missing) | InternalICU 75
| messageformat2.h | const UnicodeString &amp; icu::message2::MessageFormatter::getNormalizedPattern() const |  (missing) | InternalICU 75
| messageformat2.h | icu::message2::MessageFormatter::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2.h | icu::message2::MessageFormatter::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2.h | icu::message2::MessageFormatter::~MessageFormatter() |  (missing) | InternalICU 75
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() &amp;&amp; |  (missing) | DraftICU 75
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() const &amp; |  (missing) | DraftICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() &amp;&amp; |  (missing) | DraftICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() const &amp; |  (missing) | DraftICU 75
| simplenumberformatter.h | void icu::number::SimpleNumber::setMaximumIntegerDigits(uint32_t, UErrorCode&amp;) |  (missing) | DraftICU 75
| uchar.h | bool u_hasIDType(UChar32, UIdentifierType) |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_ALLOWED |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_RESTRICTED |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEFAULT_IGNORABLE |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEPRECATED |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_EXCLUSION |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_INCLUSION |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_LIMITED_USE |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_CHARACTER |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_NFKC |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_XID |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_OBSOLETE |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_RECOMMENDED |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_TECHNICAL |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_UNCOMMON_USE |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_STATUS |  (missing) | DraftICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_TYPE |  (missing) | DraftICU 75
| uchar.h | int32_t u_getIDTypes(UChar32, UIdentifierType*, int32_t, UErrorCode*) |  (missing) | DraftICU 75
| uconfig.h | <tt>#define</tt> UCONFIG_NO_MF2 |  (missing) | InternalICU 75
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_ARABIC_NASTALIQ |  (missing) | StableICU 75| *(Born Stable)* |
| usimplenumberformatter.h | void usnum_setMaximumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  (missing) | DraftICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_DUPLICATE_DECLARATION_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_DUPLICATE_OPTION_NAME_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_FORMATTING_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_MISSING_SELECTOR_ANNOTATION_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_NONEXHAUSTIVE_PATTERN_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_OPERAND_MISMATCH_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_SELECTOR_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_SYNTAX_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNKNOWN_FUNCTION_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNRESOLVED_VARIABLE_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNSUPPORTED_EXPRESSION_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNSUPPORTED_STATEMENT_ERROR |  (missing) | InternalICU 75
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_VARIANT_KEY_MISMATCH_ERROR |  (missing) | InternalICU 75

## Other

Other existing drafts in ICU 75

| File | API | ICU 74 | ICU 75 |
|---|---|---|---|
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGasolineEnergyDensity() | DraftICU 74 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGasolineEnergyDensity(UErrorCode&amp;) | DraftICU 74 | 
| measure.h |  bool icu::Measure::operator!=(const UObject&amp;) const | DraftICU 74 | 
| normalizer2.h |  <tt>static</tt> const Normalizer2* icu::Normalizer2::getNFKCSimpleCasefoldInstance(UErrorCode&amp;) | DraftICU 74 | 
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

Contents generated by StableAPI tool on Thu Mar 28 15:48:25 PDT 2024

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  