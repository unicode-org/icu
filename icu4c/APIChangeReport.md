
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 77 with ICU 78

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 77](#removed)
- [Deprecated or Obsoleted in ICU 78](#deprecated)
- [Changed in  ICU 78](#changed)
- [Promoted to stable in ICU 78](#promoted)
- [Added in ICU 78](#added)
- [Other existing drafts in ICU 78](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 77
  
| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| fmtable.h | CharString* icu::Formattable::internalGetCharString(UErrorCode&amp;) |  Internal | (missing)
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPortionPer1E9() |  DraftICU 77 | (missing)
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPortionPer1E9(UErrorCode&amp;) |  DraftICU 77 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addBinding(Binding&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addSelector(VariableName&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addVariant(SelectorKeys&amp;&amp;, Pattern&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::operator=(Builder&amp;&amp;)=delete |   *untagged*  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::operator=(const Builder&amp;)=delete |   *untagged*  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::setPattern(Pattern&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | MFDataModel icu::message2::MFDataModel::Builder::build(UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::Builder(Builder&amp;&amp;)=delete |   *untagged*  | (missing)
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::Builder(UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::Builder(const Builder&amp;)=delete |   *untagged*  | (missing)
| messageformat2_data_model.h | icu::message2::MFDataModel::Builder::~Builder() |  InternalICU 75 | (missing)
| messageformat2_formattable.h | UDate icu::message2::Formattable::getDate(UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_formattable.h | <tt>static</tt> Formattable icu::message2::Formattable::forDate(UDate) |  InternalICU 75 | (missing)
| platform.h | <tt>#define</tt> U_HAVE_PLACEMENT_NEW |  StableICU 2.6 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 78
  
| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMetricTon() |  StableICU 64 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramPerDeciliter() |  StableICU 64 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMetricTon(UErrorCode&amp;) |  StableICU 54 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramPerDeciliter(UErrorCode&amp;) |  StableICU 57 | DeprecatedICU 78

## Changed

Changed in  ICU 78 (old, new)


  
| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| coll.h | auto icu::Collator::equal_to() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::greater() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::greater_equal() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::less() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::less_equal() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::not_equal_to() const |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightSpeed() |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMetricTon() |  StableICU 64 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getMilligramPerDeciliter() |  StableICU 64 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNight() |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLightSpeed(UErrorCode&amp;) |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMetricTon(UErrorCode&amp;) |  StableICU 54 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createMilligramPerDeciliter(UErrorCode&amp;) |  StableICU 57 | DeprecatedICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNight(UErrorCode&amp;) |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_CONSONANT |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_EXTEND |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_LINKER |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_NONE |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UProperty::UCHAR_INDIC_CONJUNCT_BREAK |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UProperty::UCHAR_MODIFIER_COMBINING_MARK |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetCodePoints icu::UnicodeSet::codePoints() const |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetRanges icu::UnicodeSet::ranges() const |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetStrings icu::UnicodeSet::strings() const |  Draft→StableICU 76
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::append(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator+=(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator=(const S&amp;) |  Draft→StableICU 76
| unistr.h | bool icu::UnicodeString::operator!=(const S&amp;) const |  Draft→StableICU 76
| unistr.h | bool icu::UnicodeString::operator==(const S&amp;) const |  Draft→StableICU 76
| unistr.h | icu::UnicodeString::operator std::u16string_view() const |  Draft→StableICU 76
| unistr.h | icu::UnicodeString::operator std::wstring_view() const |  Draft→StableICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const S&amp;) |  Draft→StableICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const UnicodeString&amp;) |  Draft→StableICU 76
| uset.h | CodePointRange U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator* () const |  Draft→StableICU 76
| uset.h | USetCodePointIterator &amp; U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++() |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::begin() const |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::end() const |  Draft→StableICU 76
| uset.h | USetRangeIterator &amp; U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++() |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::begin() const |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::end() const |  Draft→StableICU 76
| uset.h | USetStringIterator &amp; U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++() |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::begin() const |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::end() const |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::USetCodePointIterator(const USetCodePointIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USetCodePoints&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRangeIterator::USetRangeIterator(const USetRangeIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USetRanges&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStringIterator::USetStringIterator(const USetStringIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USetStrings&amp;)=default |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator!=(const USetCodePointIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator==(const USetCodePointIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator!=(const USetRangeIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator==(const USetRangeIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator!=(const USetStringIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator==(const USetStringIterator&amp;) const |  Draft→StableICU 76
| uset.h | UChar32 U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator* () const |  Draft→StableICU 76
| uset.h | const UChar* uset_getString(const USet*, int32_t, int32_t*) |  Draft→StableICU 76
| uset.h | int32_t uset_getStringCount(const USet*) |  Draft→StableICU 76
| uset.h | std::u16string_view U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator* () const |  Draft→StableICU 76

## Promoted

Promoted to stable in ICU 78
  
| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| coll.h | auto icu::Collator::equal_to() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::greater() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::greater_equal() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::less() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::less_equal() const |  Draft→StableICU 76
| coll.h | auto icu::Collator::not_equal_to() const |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightSpeed() |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getNight() |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLightSpeed(UErrorCode&amp;) |  Draft→StableICU 76
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNight(UErrorCode&amp;) |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_BERIA_ERFE |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_J |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MISCELLANEOUS_SYMBOLS_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SHARADA_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SIDETIC |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TAI_YO |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGUT_COMPONENTS_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TOLONG_SIKI |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_CONSONANT |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_EXTEND |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_LINKER |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_NONE |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_THIN_NOON |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_UNAMBIGUOUS_HYPHEN |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_INDIC_CONJUNCT_BREAK |  Draft→StableICU 76
| uchar.h | <tt>enum</tt> UProperty::UCHAR_MODIFIER_COMBINING_MARK |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetCodePoints icu::UnicodeSet::codePoints() const |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetRanges icu::UnicodeSet::ranges() const |  Draft→StableICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetStrings icu::UnicodeSet::strings() const |  Draft→StableICU 76
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::append(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator+=(const S&amp;) |  Draft→StableICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator=(const S&amp;) |  Draft→StableICU 76
| unistr.h | bool icu::UnicodeString::operator!=(const S&amp;) const |  Draft→StableICU 76
| unistr.h | bool icu::UnicodeString::operator==(const S&amp;) const |  Draft→StableICU 76
| unistr.h | icu::UnicodeString::operator std::u16string_view() const |  Draft→StableICU 76
| unistr.h | icu::UnicodeString::operator std::wstring_view() const |  Draft→StableICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const S&amp;) |  Draft→StableICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const UnicodeString&amp;) |  Draft→StableICU 76
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_BERIA_ERFE |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SIDETIC |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TAI_YO |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TOLONG_SIKI |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TRADITIONAL_HAN_WITH_LATIN |  (missing) | StableICU 78| *(Born Stable)* |
| uset.h | CodePointRange U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator* () const |  Draft→StableICU 76
| uset.h | USetCodePointIterator &amp; U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++() |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::begin() const |  Draft→StableICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::end() const |  Draft→StableICU 76
| uset.h | USetRangeIterator &amp; U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++() |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::begin() const |  Draft→StableICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::end() const |  Draft→StableICU 76
| uset.h | USetStringIterator &amp; U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++() |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++(int) |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::begin() const |  Draft→StableICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::end() const |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::USetCodePointIterator(const USetCodePointIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USetCodePoints&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRangeIterator::USetRangeIterator(const USetRangeIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USetRanges&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStringIterator::USetStringIterator(const USetStringIterator&amp;)=default |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USet*) |  Draft→StableICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USetStrings&amp;)=default |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator!=(const USetCodePointIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator==(const USetCodePointIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator!=(const USetRangeIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator==(const USetRangeIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator!=(const USetStringIterator&amp;) const |  Draft→StableICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator==(const USetStringIterator&amp;) const |  Draft→StableICU 76
| uset.h | UChar32 U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator* () const |  Draft→StableICU 76
| uset.h | const UChar* uset_getString(const USet*, int32_t, int32_t*) |  Draft→StableICU 76
| uset.h | int32_t uset_getStringCount(const USet*) |  Draft→StableICU 76
| uset.h | std::u16string_view U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator* () const |  Draft→StableICU 76

## Added

Added in ICU 78
  
| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| dtfmtsym.h | const UnicodeString* icu::DateFormatSymbols::getAmPmStrings(int32_t&amp;, DtContextType, DtWidthType) const |  (missing) | DraftICU 78
| dtfmtsym.h | void icu::DateFormatSymbols::setAmPmStrings(const UnicodeString*, int32_t, DtContextType, DtWidthType) |  (missing) | DraftICU 78
| dtptngen.h | UDateTimePatternConflict icu::DateTimePatternGenerator::addPatternWithSkeleton(const UnicodeString&amp;, const UnicodeString&amp;, bool, UnicodeString&amp;, UErrorCode&amp;) |  (missing) | InternalICU 78
| fmtable.h | FixedString* icu::Formattable::internalGetFixedString(UErrorCode&amp;) |  (missing) | Internal
| localpointer.h | <tt>static</tt> void* icu::LocalPointerBase&lt; T &gt;::operator new(size_t, void*)=delete |  (missing) | *untagged*
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBecquerel() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBritishThermalUnitIt() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getBuJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCalorieIt() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getChain() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCho() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCoulomb() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupImperial() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getCupJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFarad() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFluidOunceMetric() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFortnight() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getFun() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGray() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getHenry() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getJoJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKatal() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKen() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKilogramForce() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKoku() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getKosaji() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOfglucose() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOfhg() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getOsaji() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPart() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPer1E6() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPartPer1E9() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPintImperial() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRankine() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRiJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRin() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getRod() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSai() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSeJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getShaku() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getShakuCloth() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getShakuLength() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSiemens() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSievert() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSlug() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSteradian() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getSun() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTesla() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getToJp() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getWeber() |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBecquerel(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBritishThermalUnitIt(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBuJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCalorieIt(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createChain(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCho(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCoulomb(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCupImperial(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createCupJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFarad(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFluidOunceMetric(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFortnight(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createFun(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGray(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createHenry(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createJoJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKatal(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKen(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKilogramForce(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKoku(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createKosaji(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOfglucose(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOfhg(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createOsaji(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPart(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPartPer1E6(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPartPer1E9(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPintImperial(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRankine(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRiJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRin(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createRod(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSai(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSeJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShaku(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShakuCloth(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createShakuLength(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSiemens(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSievert(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSlug(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSteradian(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createSun(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTesla(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createToJp(UErrorCode&amp;) |  (missing) | DraftICU 78
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createWeber(UErrorCode&amp;) |  (missing) | DraftICU 78
| messageformat2_data_model.h | Binding&amp; icu::message2::data_model::Binding::operator=(Binding) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::addBinding(Binding&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::addSelector(VariableName&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::addVariant(SelectorKeys&amp;&amp;, Pattern&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::operator=(Builder&amp;&amp;)=delete |  (missing) | *untagged*
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::operator=(const Builder&amp;)=delete |  (missing) | *untagged*
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::MFDataModel::Builder::setPattern(Pattern&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Iterator icu::message2::data_model::Pattern::begin() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | Iterator icu::message2::data_model::Pattern::end() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | Key&amp; icu::message2::data_model::Key::operator=(Key) |  (missing) | InternalICU 75
| messageformat2_data_model.h | MFDataModel icu::message2::data_model::MFDataModel::Builder::build(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | MFDataModel&amp; icu::message2::data_model::MFDataModel::operator=(MFDataModel) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Operand&amp; icu::message2::data_model::Operand::operator=(Operand) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Operator&amp; icu::message2::data_model::Operator::operator=(Operator) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Pattern&amp; icu::message2::data_model::Pattern::operator=(Pattern) |  (missing) | InternalICU 75
| messageformat2_data_model.h | SelectorKeys&amp; icu::message2::data_model::SelectorKeys::operator=(SelectorKeys) |  (missing) | InternalICU 75
| messageformat2_data_model.h | Variant&amp; icu::message2::data_model::Variant::operator=(Variant) |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Binding::isLocal() const |  (missing) | InternalICU 78
| messageformat2_data_model.h | bool icu::message2::data_model::Key::isWildcard() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Key::operator&lt;(const Key&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Key::operator==(const Key&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Operand::isLiteral() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Operand::isNull() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::Operand::isVariable() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | bool icu::message2::data_model::SelectorKeys::operator&lt;(const SelectorKeys&amp;) const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Expression &amp; icu::message2::data_model::Binding::getValue() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const FunctionName &amp; icu::message2::data_model::Operator::getFunctionName() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Literal &amp; icu::message2::data_model::Key::asLiteral() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Literal &amp; icu::message2::data_model::Operand::asLiteral() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Pattern &amp; icu::message2::data_model::MFDataModel::getPattern() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const Pattern &amp; icu::message2::data_model::Variant::getPattern() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const SelectorKeys &amp; icu::message2::data_model::Variant::getKeys() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::Operand::asVariable() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | const VariableName &amp; icu::message2::data_model::Binding::getVariable() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Binding::Binding() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Binding::Binding(const Binding&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Binding::Binding(const VariableName&amp;, Expression&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Binding::~Binding() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Key::Key() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Key::Key(const Key&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Key::Key(const Literal&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Key::~Key() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::Builder::Builder(Builder&amp;&amp;)=delete |  (missing) | *untagged*
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::Builder::Builder(UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::Builder::Builder(const Builder&amp;)=delete |  (missing) | *untagged*
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::Builder::~Builder() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::MFDataModel() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::MFDataModel(const MFDataModel&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::MFDataModel::~MFDataModel() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operand::Operand() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operand::Operand(const Literal&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operand::Operand(const Operand&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operand::Operand(const UnicodeString&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operand::~Operand() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operator::Operator() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operator::Operator(const Operator&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Operator::~Operator() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Pattern::Pattern() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Pattern::Pattern(const Pattern&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Pattern::~Pattern() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::SelectorKeys::SelectorKeys() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::SelectorKeys::SelectorKeys(const SelectorKeys&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::SelectorKeys::~SelectorKeys() |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Variant::Variant()=default |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Variant::Variant(const SelectorKeys&amp;, Pattern&amp;&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Variant::Variant(const Variant&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | icu::message2::data_model::Variant::~Variant() |  (missing) | InternalICU 75
| messageformat2_data_model.h | <tt>static</tt> Binding icu::message2::data_model::Binding::input(UnicodeString&amp;&amp;, Expression&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Binding &gt; icu::message2::data_model::MFDataModel::getLocalVariables() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Key &gt; icu::message2::data_model::SelectorKeys::getKeys() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Option &gt; icu::message2::data_model::Operator::getOptions() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; VariableName &gt; icu::message2::data_model::MFDataModel::getSelectors() const |  (missing) | InternalICU 75
| messageformat2_data_model.h | std::vector&lt; Variant &gt; icu::message2::data_model::MFDataModel::getVariants() const |  (missing) | InternalICU 75
| messageformat2_formattable.h | const DateInfo* icu::message2::Formattable::getDate(UErrorCode&amp;) const |  (missing) | InternalICU 75
| messageformat2_formattable.h | icu::message2::Formattable::Formattable(DateInfo&amp;&amp;) |  (missing) | InternalICU 75
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_BERIA_ERFE |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_J |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MISCELLANEOUS_SYMBOLS_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SHARADA_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SIDETIC |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TAI_YO |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TANGUT_COMPONENTS_SUPPLEMENT |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TOLONG_SIKI |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_THIN_NOON |  (missing) | StableICU 78| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_UNAMBIGUOUS_HYPHEN |  (missing) | StableICU 78| *(Born Stable)* |
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_AM_PMS_NARROW |  (missing) | DraftICU 78
| udat.h | <tt>enum</tt> UDateFormatSymbolType::UDAT_AM_PMS_WIDE |  (missing) | DraftICU 78
| umachine.h | <tt>#define</tt> U_FORCE_INLINE |  (missing) | Internal
| unistr.h | StringClass icu::UnicodeString::toUTF8String() const |  (missing) | DraftICU 78
| unistr.h | unspecified_iterator icu::UnicodeString::begin() const |  (missing) | DraftICU 78
| unistr.h | unspecified_iterator icu::UnicodeString::end() const |  (missing) | DraftICU 78
| unistr.h | unspecified_reverse_iterator icu::UnicodeString::rbegin() const |  (missing) | DraftICU 78
| unistr.h | unspecified_reverse_iterator icu::UnicodeString::rend() const |  (missing) | DraftICU 78
| unistr.h | void icu::UnicodeString::push_back(char16_t) |  (missing) | DraftICU 78
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_BERIA_ERFE |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SIDETIC |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TAI_YO |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TOLONG_SIKI |  (missing) | StableICU 78| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TRADITIONAL_HAN_WITH_LATIN |  (missing) | StableICU 78| *(Born Stable)* |
| utf.h | <tt>#define</tt> U_IS_CODE_POINT |  (missing) | DraftICU 78
| utf.h | <tt>#define</tt> U_IS_SCALAR_VALUE |  (missing) | DraftICU 78
| utf8.h | <tt>#define</tt> U8_LENGTH_FROM_LEAD_BYTE_UNSAFE |  (missing) | DraftICU 78
| utf8.h | <tt>#define</tt> U8_LENGTH_FROM_LEAD_BYTE |  (missing) | DraftICU 78
| utfiterator.h | CP32 U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::codePoint() const |  (missing) | DraftICU 78
| utfiterator.h | CP32 U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::operator* () const |  (missing) | Internal
| utfiterator.h | CodePointsIterator &amp; U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::operator++() |  (missing) | Internal
| utfiterator.h | CodePointsIterator U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::operator++(int) |  (missing) | Internal
| utfiterator.h | CodeUnits&amp; U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::operator=(const CodeUnits&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE CodeUnits&lt; CP32, UnitIter &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator* () const |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE Proxy U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator-&gt;() const |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE UTFIterator &amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator++() |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE UTFIterator U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator++(int) |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE UTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator=(UTFIterator&amp;&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE UTFIterator&amp; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator=(const UTFIterator&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator() |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UTFIterator&amp;&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter) |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter, LimitIter) |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(UnitIter, UnitIter, LimitIter) |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::UTFIterator(const UTFIterator&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator!=(const UTFIterator&amp;) const |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE bool U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator==(const UTFIterator&amp;) const |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UTFIterator &amp; &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator--() |  (missing) | DraftICU 78
| utfiterator.h | U_FORCE_INLINE std::enable_if_t&lt; prv::bidirectional_iterator&lt; Iter &gt;, UTFIterator &gt; U_HEADER_ONLY_NAMESPACE::UTFIterator&lt; CP32, behavior, UnitIter, LimitIter, typename &gt;::operator--(int) |  (missing) | DraftICU 78
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::AllCodePoints() |  (missing) | DraftICU 78
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::AllScalarValues() |  (missing) | DraftICU 78
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::CodeUnits(CP32, uint8_t, bool, UnitIter, UnitIter) |  (missing) | Internal
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::CodeUnits(const CodeUnits&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::UnsafeCodeUnits(CP32, uint8_t, UnitIter, UnitIter) |  (missing) | Internal
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::UnsafeCodeUnits(const UnsafeCodeUnits&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::CodePointsIterator(CP32) |  (missing) | Internal
| utfiterator.h | UnitIter U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::begin() const |  (missing) | DraftICU 78
| utfiterator.h | UnitIter U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::end() const |  (missing) | DraftICU 78
| utfiterator.h | UnsafeCodeUnits&amp; U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::operator=(const UnsafeCodeUnits&amp;)=default |  (missing) | DraftICU 78
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::begin() const |  (missing) | DraftICU 78
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::AllCodePoints&lt; CP32 &gt;::end() const |  (missing) | DraftICU 78
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::begin() const |  (missing) | DraftICU 78
| utfiterator.h | auto U_HEADER_ONLY_NAMESPACE::AllScalarValues&lt; CP32 &gt;::end() const |  (missing) | DraftICU 78
| utfiterator.h | bool U_HEADER_ONLY_NAMESPACE::CodeUnits&lt; CP32, UnitIter, typename &gt;::wellFormed() const |  (missing) | DraftICU 78
| utfiterator.h | bool U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::operator!=(const CodePointsIterator&amp;) const |  (missing) | Internal
| utfiterator.h | bool U_HEADER_ONLY_NAMESPACE::prv::CodePointsIterator&lt; CP32, skipSurrogates &gt;::operator==(const CodePointsIterator&amp;) const |  (missing) | Internal
| utfiterator.h | <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_FFFD |  (missing) | DraftICU 78
| utfiterator.h | <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_NEGATIVE |  (missing) | DraftICU 78
| utfiterator.h | <tt>enum</tt> UTFIllFormedBehavior::UTF_BEHAVIOR_SURROGATE |  (missing) | DraftICU 78
| utfiterator.h | std::enable_if_t&lt; std::is_pointer_v&lt; Iter &gt;|| std::is_same_v&lt; Iter, typename std::basic_string&lt; Unit &gt;::iterator &gt;|| std::is_same_v&lt; Iter, typename std::basic_string&lt; Unit &gt;::const_iterator &gt;|| std::is_same_v&lt; Iter, typename std::basic_string_view&lt; Unit &gt;::iterator &gt;|| std::is_same_v&lt; Iter, typename std::basic_string_view&lt; Unit &gt;::const_iterator &gt;, std::basic_string_view&lt; Unit &gt; &gt; U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::stringView() const |  (missing) | DraftICU 78
| utfiterator.h | uint8_t U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits&lt; CP32, UnitIter, typename &gt;::length() const |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> U_DATA_API_CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> U_IO_API_CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> U_LAYOUTEX_API_CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> U_LAYOUT_API_CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> U_TOOLUTIL_API_CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> _CLASS |  (missing) | DraftICU 78
| utypes.h | <tt>#define</tt> _CLASS |  (missing) | DraftICU 78

## Other

Other existing drafts in ICU 78

| File | API | ICU 77 | ICU 78 |
|---|---|---|---|
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  MeasureUnit icu::MeasureUnit::withConstantDenominator(uint64_t, UErrorCode&amp;) const | DraftICU 77 | 
| measunit.h |  uint64_t icu::MeasureUnit::getConstantDenominator(UErrorCode&amp;) const | DraftICU 77 | 
| uniset.h |  U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::begin() const | DraftICU 77 | 
| uniset.h |  U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::end() const | DraftICU 77 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| uset.h |  USetElementIterator &amp; U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++() | DraftICU 77 | 
| uset.h |  USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++(int) | DraftICU 77 | 
| uset.h |  USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::begin() const | DraftICU 77 | 
| uset.h |  USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::end() const | DraftICU 77 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetElementIterator::USetElementIterator(const USetElementIterator&amp;)=default | DraftICU 77 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USet*) | DraftICU 77 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USetElements&amp;)=default | DraftICU 77 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator!=(const USetElementIterator&amp;) const | DraftICU 77 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator==(const USetElementIterator&amp;) const | DraftICU 77 | 
| uset.h |  std::u16string U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const | DraftICU 77 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Fri Oct 24 17:48:52 PDT 2025

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  