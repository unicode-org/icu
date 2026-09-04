
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 78 with ICU 79

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 78](#removed)
- [Deprecated or Obsoleted in ICU 79](#deprecated)
- [Changed in  ICU 79](#changed)
- [Promoted to stable in ICU 79](#promoted)
- [Added in ICU 79](#added)
- [Other existing drafts in ICU 79](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 78
  
| File | API | ICU 78 | ICU 79 |
|---|---|---|---|
| messageformat2_formattable.h | FormattedPlaceholder&amp; icu::message2::FormattedPlaceholder::operator=(FormattedPlaceholder&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | FormattedValue&amp; icu::message2::FormattedValue::operator=(FormattedValue&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | FunctionOptions&amp; icu::message2::FunctionOptions::operator=(FunctionOptions&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | FunctionOptions&amp; icu::message2::FunctionOptions::operator=(const FunctionOptions&amp;)=delete |  InternalICU 75 | (missing)
| messageformat2_formattable.h | UnicodeString icu::message2::FormattedPlaceholder::formatToString(const Locale&amp;, UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::canFormat() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isEvaluated() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isFallback() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedPlaceholder::isNullOperand() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedValue::isNumber() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | bool icu::message2::FormattedValue::isString() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const FormattedValue &amp; icu::message2::FormattedPlaceholder::output() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const FunctionOptions &amp; icu::message2::FormattedPlaceholder::options() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const UnicodeString &amp; icu::message2::FormattedPlaceholder::getFallback() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const UnicodeString &amp; icu::message2::FormattedValue::getString() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const message2::Formattable &amp; icu::message2::FormattedPlaceholder::asFormattable() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | const number::FormattedNumber &amp; icu::message2::FormattedValue::getNumber() const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder() |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(FormattedPlaceholder&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const Formattable&amp;, const UnicodeString&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder&amp;, FormattedValue&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder&amp;, FunctionOptions&amp;&amp;, FormattedValue&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedPlaceholder::FormattedPlaceholder(const UnicodeString&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue() |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(FormattedValue&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(const UnicodeString&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedValue::FormattedValue(number::FormattedNumber&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FormattedValue::~FormattedValue() |  InternalICU 75 | (missing)
| messageformat2_formattable.h | icu::message2::FunctionOptions::FunctionOptions(FunctionOptions&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::adoptFormatter(const data_model::FunctionName&amp;, FormatterFactory*, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::adoptSelector(const data_model::FunctionName&amp;, SelectorFactory*, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | FormattedPlaceholder icu::message2::Formatter::format(FormattedPlaceholder&amp;&amp;, FunctionOptions&amp;&amp;, UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | Formatter* icu::message2::FormatterFactory::createFormatter(const Locale&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | FormatterFactory&amp; icu::message2::FormatterFactory::operator=(const FormatterFactory&amp;)=delete |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | FormatterFactory* icu::message2::MFFunctionRegistry::getFormatter(const FunctionName&amp;) const |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | Selector* icu::message2::SelectorFactory::createSelector(const Locale&amp;, UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | SelectorFactory&amp; icu::message2::SelectorFactory::operator=(const SelectorFactory&amp;)=delete |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | const SelectorFactory* icu::message2::MFFunctionRegistry::getSelector(const FunctionName&amp;) const |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | icu::message2::Formatter::~Formatter() |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | icu::message2::FormatterFactory::~FormatterFactory() |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | icu::message2::Selector::~Selector() |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | icu::message2::SelectorFactory::~SelectorFactory() |  InternalICU 75 | (missing)
| messageformat2_function_registry.h | void icu::message2::Selector::selectKey(FormattedPlaceholder&amp;&amp;, FunctionOptions&amp;&amp;, const UnicodeString*, int32_t, UnicodeString*, int32_t&amp;, UErrorCode&amp;) const |  InternalICU 75 | (missing)
| numberformatter.h | int16_t icu::number::impl::StringProp::length() const |  Internal | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 79
  
| File | API | ICU 78 | ICU 79 |
|---|---|---|---|

## Changed

Changed in  ICU 79 (old, new)


  
| File | API | ICU 78 | ICU 79 |
|---|---|---|---|
| measunit.h | MeasureUnit icu::MeasureUnit::withConstantDenominator(uint64_t, UErrorCode&amp;) const |  Draft→StableICU 77
| measunit.h | uint64_t icu::MeasureUnit::getConstantDenominator(UErrorCode&amp;) const |  Draft→StableICU 77
| messageformat2_data_model.h | bool icu::message2::data_model::Binding::isLocal() const |  InternalICU 78 | InternalICU 79
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::begin() const |  Draft→StableICU 77
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::end() const |  Draft→StableICU 77
| uset.h | USetElementIterator &amp; U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++() |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++(int) |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::begin() const |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::end() const |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElementIterator::USetElementIterator(const USetElementIterator&amp;)=default |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USet*) |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USetElements&amp;)=default |  Draft→StableICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator!=(const USetElementIterator&amp;) const |  Draft→StableICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator==(const USetElementIterator&amp;) const |  Draft→StableICU 77
| uset.h | std::u16string U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const |  Draft→StableICU 77

## Promoted

Promoted to stable in ICU 79
  
| File | API | ICU 78 | ICU 79 |
|---|---|---|---|
| measunit.h | MeasureUnit icu::MeasureUnit::withConstantDenominator(uint64_t, UErrorCode&amp;) const |  Draft→StableICU 77
| measunit.h | uint64_t icu::MeasureUnit::getConstantDenominator(UErrorCode&amp;) const |  Draft→StableICU 77
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARCHAIC_CUNEIFORM_NUMERALS |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_BENGALI_SUPPLEMENT |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_JURCHEN_RADICALS |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_JURCHEN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MISCELLANEOUS_SYMBOLS_AND_ARROWS_EXTENDED |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MUSICAL_SYMBOLS_SUPPLEMENT |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SEAL |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_AIN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_BEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_FEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_HAH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_HEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_KAF |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_MEEM |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_SAD |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_SEEN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_TAH |  (missing) | StableICU 79| *(Born Stable)* |
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::begin() const |  Draft→StableICU 77
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::end() const |  Draft→StableICU 77
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_PROTO_CUNEIFORM |  (missing) | StableICU 79| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SEAL |  (missing) | StableICU 79| *(Born Stable)* |
| uset.h | USetElementIterator &amp; U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++() |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++(int) |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::begin() const |  Draft→StableICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::end() const |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElementIterator::USetElementIterator(const USetElementIterator&amp;)=default |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USet*) |  Draft→StableICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USetElements&amp;)=default |  Draft→StableICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator!=(const USetElementIterator&amp;) const |  Draft→StableICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator==(const USetElementIterator&amp;) const |  Draft→StableICU 77
| uset.h | std::u16string U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const |  Draft→StableICU 77

## Added

Added in ICU 79
  
| File | API | ICU 78 | ICU 79 |
|---|---|---|---|
| dtfmtsym.h | const UnicodeString* icu::DateFormatSymbols::getDayOfMonthCardinalNames(int32_t&amp;, DtContextType, DtWidthType) const |  (missing) | Internal
| dtfmtsym.h | const UnicodeString* icu::DateFormatSymbols::getDayOfMonthOrdinalNames(DtContextType, DtWidthType) const |  (missing) | Internal
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getDyne() |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilliinch() |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPoundal() |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createDyne(UErrorCode&amp;) |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilliinch(UErrorCode&amp;) |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPoundal(UErrorCode&amp;) |  (missing) | DraftICU 79
| measunit.h | <tt>static</tt> bool icu::MeasureUnit::validateAndGet(StringPiece, StringPiece, MeasureUnit&amp;) |  (missing) | Internal
| messageformat2_formattable.h | FunctionOptions icu::message2::FunctionOptions::mergeOptions(const FunctionOptions&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 79
| messageformat2_formattable.h | FunctionOptions&amp; icu::message2::FunctionOptions::operator=(FunctionOptions) |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::FunctionOptions::FunctionOptions(const FunctionOptions&amp;) |  (missing) | InternalICU 75
| messageformat2_function_registry.h | Builder&amp; icu::message2::MFFunctionRegistry::Builder::adoptFunction(const data_model::FunctionName&amp;, Function*, UErrorCode&amp;) |  (missing) | InternalICU 79
| messageformat2_function_registry.h | FunctionContext icu::message2::FunctionContext::withLocale(const Locale&amp;) const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | LocalPointer&lt; FunctionValue &gt; icu::message2::Function::call(const FunctionContext&amp;, const FunctionValue&amp;, const FunctionOptions&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | UMFBidiOption icu::message2::FunctionContext::getDirection() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | UMFBidiOption icu::message2::FunctionValue::getDirectionAnnotation() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | UMFDirectionality icu::message2::FunctionValue::getDirection() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | UnicodeString icu::message2::FunctionValue::formatToString(UErrorCode&amp;) const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | bool icu::message2::FunctionValue::isNullOperand() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | bool icu::message2::FunctionValue::isSelectable() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const Formattable &amp; icu::message2::FunctionValue::unwrap() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const Function* icu::message2::MFFunctionRegistry::getFunction(const FunctionName&amp;) const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const FunctionName &amp; icu::message2::FunctionContext::getCalledFunctionName() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const FunctionName &amp; icu::message2::FunctionValue::getFunctionName() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const FunctionOptions &amp; icu::message2::FunctionValue::getResolvedOptions() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const Locale &amp; icu::message2::FunctionContext::getLocale() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const UnicodeString &amp; icu::message2::FunctionContext::getID() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | const UnicodeString &amp; icu::message2::FunctionValue::getFallback() const |  (missing) | InternalICU 79
| messageformat2_function_registry.h | icu::message2::Function::~Function() |  (missing) | InternalICU 79
| messageformat2_function_registry.h | icu::message2::FunctionValue::~FunctionValue() |  (missing) | InternalICU 79
| messageformat2_function_registry.h | <tt>static</tt> const MFFunctionRegistry* icu::message2::MFFunctionRegistry::getStandardFunctionsRegistry(UErrorCode&amp;) |  (missing) | InternalICU 79
| messageformat2_function_registry.h | void icu::message2::FunctionValue::selectKeys(const UnicodeString*, int32_t, int32_t*, int32_t&amp;, UErrorCode&amp;) const |  (missing) | InternalICU 79
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setBidiContext(UMFBidiContext) |  (missing) | InternalICU 79
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setBidiIsolationStrategy(UMFBidiIsolationStrategy) |  (missing) | InternalICU 79
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setBidiIsolationStyle(UMFBidiIsolationStyle) |  (missing) | InternalICU 79
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFBidiContext {} |  (missing) | InternalICU 79
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFBidiContext::U_MF_BIDI_CONTEXT_AUTO |  (missing) | InternalICU 79
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFBidiContext::U_MF_BIDI_CONTEXT_DEFAULT |  (missing) | InternalICU 79
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFBidiContext::U_MF_BIDI_CONTEXT_LTR |  (missing) | InternalICU 79
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFBidiContext::U_MF_BIDI_CONTEXT_RTL |  (missing) | InternalICU 79
| numberformatter.h | int32_t icu::number::impl::StringProp::length() const |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_HAS_SANITIZER |  (missing) | Internal
| platform.h | <tt>#define</tt> UPRV_NO_SANITIZE_FUNCTION |  (missing) | Internal
| platform.h | <tt>#define</tt> U_LIFETIME_BOUND |  (missing) | Internal
| rbbi.h | int32_t icu::RuleBasedBreakIterator::handleSafePrevious(int32_t) |  (missing) | Internal
| symtable.h | const UnicodeSet* icu::SymbolTable::lookupSet(const UnicodeString&amp;) const |  (missing) | Internal
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_ARCHAIC_CUNEIFORM_NUMERALS |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_BENGALI_SUPPLEMENT |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_JURCHEN_RADICALS |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_JURCHEN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MISCELLANEOUS_SYMBOLS_AND_ARROWS_EXTENDED |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MUSICAL_SYMBOLS_SUPPLEMENT |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SEAL |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::_INCB_COUNT |  (missing) | Internal
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_AIN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_BEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_FEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_HAH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_HEH |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_KAF |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_MEEM |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_SAD |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_SEEN |  (missing) | StableICU 79| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_CROWN_TAH |  (missing) | StableICU 79| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_PROTO_CUNEIFORM |  (missing) | StableICU 79| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SEAL |  (missing) | StableICU 79| *(Born Stable)* |
| utfiterator.h | U_FORCE_INLINE UnitIter U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::base() const |  (missing) | DraftICU 79
| utfiterator.h | U_FORCE_INLINE UnitIter U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::base() const |  (missing) | DraftICU 79
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::back() const |  (missing) | DraftICU 79
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::front() const |  (missing) | DraftICU 79
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::back() const |  (missing) | DraftICU 79
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::front() const |  (missing) | DraftICU 79
| utypes.h | <tt>#define</tt> U_MF_RECOVERABLE_ERROR |  (missing) | InternalICU 79

## Other

Other existing drafts in ICU 79

| File | API | ICU 78 | ICU 79 |
|---|---|---|---|
| dtfmtsym.h |  const UnicodeString* icu::DateFormatSymbols::getAmPmStrings(int32_t&amp;, DtContextType, DtWidthType) const | DraftICU 78 | 
| dtfmtsym.h |  void icu::DateFormatSymbols::setAmPmStrings(const UnicodeString*, int32_t, DtContextType, DtWidthType) | DraftICU 78 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBecquerel() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBritishThermalUnitIt() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBuJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCalorieIt() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getChain() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCho() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCoulomb() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupImperial() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFarad() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunceMetric() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFortnight() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getFun() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getGray() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getHenry() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getJoJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKatal() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKen() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilogramForce() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKoku() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getKosaji() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOfglucose() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOfhg() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getOsaji() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPart() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPer1E6() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPer1E9() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintImperial() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRankine() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRiJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRin() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getRod() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSai() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSeJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getShaku() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getShakuCloth() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getShakuLength() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSiemens() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSievert() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSlug() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSteradian() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getSun() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getTesla() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getToJp() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeber() | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBecquerel(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnitIt(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBuJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCalorieIt(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createChain(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCho(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCoulomb(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCupImperial(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCupJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFarad(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceMetric(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFortnight(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFun(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGray(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createHenry(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createJoJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKatal(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKen(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilogramForce(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKoku(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKosaji(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOfglucose(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOfhg(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOsaji(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPart(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPartPer1E6(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPartPer1E9(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPintImperial(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRankine(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRiJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRin(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRod(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSai(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSeJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShaku(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShakuCloth(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShakuLength(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSiemens(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSievert(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSlug(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSteradian(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSun(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTesla(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createToJp(UErrorCode&amp;) | DraftICU 78 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createWeber(UErrorCode&amp;) | DraftICU 78 | 
| udat.h |  <tt>enum</tt> UDateFormatSymbolType::UDAT_AM_PMS_NARROW | DraftICU 78 | 
| udat.h |  <tt>enum</tt> UDateFormatSymbolType::UDAT_AM_PMS_WIDE | DraftICU 78 | 
| unistr.h |  StringClass icu::UnicodeString::toUTF8String() const | DraftICU 78 | 
| unistr.h |  unspecified_iterator icu::UnicodeString::begin() const | DraftICU 78 | 
| unistr.h |  unspecified_iterator icu::UnicodeString::end() const | DraftICU 78 | 
| unistr.h |  unspecified_reverse_iterator icu::UnicodeString::rbegin() const | DraftICU 78 | 
| unistr.h |  unspecified_reverse_iterator icu::UnicodeString::rend() const | DraftICU 78 | 
| unistr.h |  void icu::UnicodeString::push_back(char16_t) | DraftICU 78 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| utf.h |  <tt>#define</tt> U_IS_CODE_POINT | DraftICU 78 | 
| utf.h |  <tt>#define</tt> U_IS_SCALAR_VALUE | DraftICU 78 | 
| utf8.h |  <tt>#define</tt> U8_LENGTH_FROM_LEAD_BYTE_UNSAFE | DraftICU 78 | 
| utf8.h |  <tt>#define</tt> U8_LENGTH_FROM_LEAD_BYTE | DraftICU 78 | 
| utfiterator.h |  CP32 U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::codePoint() const | DraftICU 78 | 
| utfiterator.h |  CodeUnits&amp; U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::operator=(const CodeUnits&amp;)=default | DraftICU 78 | 
| utfiterator.h |  UTFStringCodePoints&amp; U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::operator=(const UTFStringCodePoints&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE CodeUnits&lt; CP32, UnitIter &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator* () const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE Proxy U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator-&gt;() const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE Proxy U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator-&gt;() const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UTFIterator &amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator++() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UTFIterator U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator++(int) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator=(UTFIterator&amp;&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator=(const UTFIterator&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UTFIterator&amp;&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter, LimitIter) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter, UnitIter, LimitIter) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(const UTFIterator&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::UnsafeUTFIterator() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::UnsafeUTFIterator(UnitIter) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::UnsafeUTFIterator(UnsafeUTFIterator&amp;&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::UnsafeUTFIterator(const UnsafeUTFIterator&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UnsafeCodeUnits&lt; CP32, UnitIter &gt; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator* () const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UnsafeUTFIterator &amp; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator++() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UnsafeUTFIterator U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator++(int) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UnsafeUTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator=(UnsafeUTFIterator&amp;&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE UnsafeUTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator=(const UnsafeUTFIterator&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator!=(const UTFIterator&amp;) const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator==(const UTFIterator&amp;) const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator!=(const UnsafeUTFIterator&amp;) const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator==(const UnsafeUTFIterator&amp;) const | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UTFIterator &amp; &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator--() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UTFIterator &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator--(int) | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UnsafeUTFIterator &amp; &gt; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator--() | DraftICU 78 | 
| utfiterator.h |  U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UnsafeUTFIterator &gt; U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator&lt; CP32, UnitIter, typename &gt;::operator--(int) | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::AllCodePoints() | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::AllScalarValues() | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::CodeUnits(const CodeUnits&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::UTFStringCodePoints()=default | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::UTFStringCodePoints(Range) | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::UTFStringCodePoints(Range) | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::UTFStringCodePoints(const UTFStringCodePoints&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::UnsafeCodeUnits(const UnsafeCodeUnits&amp;)=default | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::UnsafeUTFStringCodePoints()=default | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::UnsafeUTFStringCodePoints(Range) | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::UnsafeUTFStringCodePoints(Range) | DraftICU 78 | 
| utfiterator.h |  U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::UnsafeUTFStringCodePoints(const UnsafeUTFStringCodePoints&amp;)=default | DraftICU 78 | 
| utfiterator.h |  UnitIter U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::begin() const | DraftICU 78 | 
| utfiterator.h |  UnitIter U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::end() const | DraftICU 78 | 
| utfiterator.h |  UnsafeCodeUnits&amp; U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::operator=(const UnsafeCodeUnits&amp;)=default | DraftICU 78 | 
| utfiterator.h |  UnsafeUTFStringCodePoints&amp; U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::operator=(const UnsafeUTFStringCodePoints&amp;)=default | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::begin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::end() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::begin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::end() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::begin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::begin() | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::end() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::end() | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::rbegin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints&lt; CP32, behavior, Range &gt;::rend() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::begin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::begin() | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::end() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::end() | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::rbegin() const | DraftICU 78 | 
| utfiterator.h |  auto U_HEADER_ONLY_NAMESPACE::UnsafeUTFStringCodePoints&lt; CP32, Range &gt;::rend() const | DraftICU 78 | 
| utfiterator.h |  bool U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::wellFormed() const | DraftICU 78 | 
| utfiterator.h |  <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_FFFD | DraftICU 78 | 
| utfiterator.h |  <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_NEGATIVE | DraftICU 78 | 
| utfiterator.h |  <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_SURROGATE | DraftICU 78 | 
| utfiterator.h |  std::enable_if_t&lt; std::is_pointer_v&lt; Iter &gt;||std::is_same_v&lt; Iter, typename std::basic_string&lt; Unit &gt;::iterator &gt;||std::is_same_v&lt; Iter, typename std::basic_string&lt; Unit &gt;::const_iterator &gt;||std::is_same_v&lt; Iter, typename std::basic_string_view&lt; Unit &gt;::iterator &gt;||std::is_same_v&lt; Iter, typename std::basic_string_view&lt; Unit &gt;::const_iterator &gt;, std::basic_string_view&lt; Unit &gt; &gt; U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::stringView() const | DraftICU 78 | 
| utfiterator.h |  uint8_t U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::length() const | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> U_DATA_API_CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> U_IO_API_CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> U_LAYOUTEX_API_CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> U_LAYOUT_API_CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> U_TOOLUTIL_API_CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> _CLASS | DraftICU 78 | 
| utypes.h |  <tt>#define</tt> _CLASS | DraftICU 78 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Fri Sep 04 15:28:07 PDT 2026

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  