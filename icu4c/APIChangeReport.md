
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 76 with ICU 77

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 76](#removed)
- [Deprecated or Obsoleted in ICU 77](#deprecated)
- [Changed in  ICU 77](#changed)
- [Promoted to stable in ICU 77](#promoted)
- [Added in ICU 77](#added)
- [Other existing drafts in ICU 77](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 76
  
| File | API | ICU 76 | ICU 77 |
|---|---|---|---|
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addSelector(Expression&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| resbund.h | const Locale icu::ResourceBundle::getLocale(ULocDataLocaleType, UErrorCode&amp;) const |  StableICU 2.8 | (missing)
| timezone.h | <tt>static</tt> const UnicodeString icu::TimeZone::getEquivalentID(const UnicodeString&amp;, int32_t) |  StableICU 2.0 | (missing)
| uset.h | UnicodeString U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const |  DraftICU 76 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 77
  
| File | API | ICU 76 | ICU 77 |
|---|---|---|---|

## Changed

Changed in  ICU 77 (old, new)


  
| File | API | ICU 76 | ICU 77 |
|---|---|---|---|
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() &amp;&amp; |  Draft→StableICU 75
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() const &amp; |  Draft→StableICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() &amp;&amp; |  Draft→StableICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() const &amp; |  Draft→StableICU 75
| simplenumberformatter.h | void icu::number::SimpleNumber::setMaximumIntegerDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 75
| uchar.h | bool u_hasIDType(UChar32, UIdentifierType) |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_ALLOWED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_RESTRICTED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEFAULT_IGNORABLE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEPRECATED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_EXCLUSION |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_INCLUSION |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_LIMITED_USE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_CHARACTER |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_NFKC |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_XID |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_OBSOLETE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_RECOMMENDED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_TECHNICAL |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_UNCOMMON_USE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_STATUS |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_TYPE |  Draft→StableICU 75
| uchar.h | int32_t u_getIDTypes(UChar32, UIdentifierType*, int32_t, UErrorCode*) |  Draft→StableICU 75
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::begin() const |  DraftICU 76 | DraftICU 77
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::end() const |  DraftICU 76 | DraftICU 77
| uset.h | USetElementIterator &amp; U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++() |  DraftICU 76 | DraftICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++(int) |  DraftICU 76 | DraftICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::begin() const |  DraftICU 76 | DraftICU 77
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::end() const |  DraftICU 76 | DraftICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElementIterator::USetElementIterator(const USetElementIterator&amp;)=default |  DraftICU 76 | DraftICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USet*) |  DraftICU 76 | DraftICU 77
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USetElements&amp;)=default |  DraftICU 76 | DraftICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator!=(const USetElementIterator&amp;) const |  DraftICU 76 | DraftICU 77
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator==(const USetElementIterator&amp;) const |  DraftICU 76 | DraftICU 77
| usimplenumberformatter.h | void usnum_setMaximumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 75

## Promoted

Promoted to stable in ICU 77
  
| File | API | ICU 76 | ICU 77 |
|---|---|---|---|
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() &amp;&amp; |  Draft→StableICU 75
| numberformatter.h | UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() const &amp; |  Draft→StableICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() &amp;&amp; |  Draft→StableICU 75
| numberrangeformatter.h | UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() const &amp; |  Draft→StableICU 75
| resbund.h | Locale icu::ResourceBundle::getLocale(ULocDataLocaleType, UErrorCode&amp;) const |  (missing) | StableICU 2.8
| simplenumberformatter.h | void icu::number::SimpleNumber::setMaximumIntegerDigits(uint32_t, UErrorCode&amp;) |  Draft→StableICU 75
| timezone.h | <tt>static</tt> UnicodeString icu::TimeZone::getEquivalentID(const UnicodeString&amp;, int32_t) |  (missing) | StableICU 2.0
| uchar.h | bool u_hasIDType(UChar32, UIdentifierType) |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_ALLOWED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_RESTRICTED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEFAULT_IGNORABLE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEPRECATED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_EXCLUSION |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_INCLUSION |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_LIMITED_USE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_CHARACTER |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_NFKC |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_XID |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_OBSOLETE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_RECOMMENDED |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_TECHNICAL |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UIdentifierType::U_ID_TYPE_UNCOMMON_USE |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_STATUS |  Draft→StableICU 75
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_TYPE |  Draft→StableICU 75
| uchar.h | int32_t u_getIDTypes(UChar32, UIdentifierType*, int32_t, UErrorCode*) |  Draft→StableICU 75
| usimplenumberformatter.h | void usnum_setMaximumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) |  Draft→StableICU 75

## Added

Added in ICU 77
  
| File | API | ICU 76 | ICU 77 |
|---|---|---|---|
| locid.h | <tt>static</tt> Locale icu::Locale::createFromName(StringPiece) |  (missing) | Internal
| measunit.h | MeasureUnit icu::MeasureUnit::withConstantDenominator(uint64_t, UErrorCode&amp;) const |  (missing) | DraftICU 77
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getPortionPer1E9() |  (missing) | DraftICU 77
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createPortionPer1E9(UErrorCode&amp;) |  (missing) | DraftICU 77
| measunit.h | uint64_t icu::MeasureUnit::getConstantDenominator(UErrorCode&amp;) const |  (missing) | DraftICU 77
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addSelector(VariableName&amp;&amp;, UErrorCode&amp;) |  (missing) | InternalICU 75
| resbund.h | Locale icu::ResourceBundle::getLocale(ULocDataLocaleType, UErrorCode&amp;) const |  (missing) | StableICU 2.8
| timezone.h | <tt>static</tt> UnicodeString icu::TimeZone::getEquivalentID(const UnicodeString&amp;, int32_t) |  (missing) | StableICU 2.0
| uset.h | std::u16string U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const |  (missing) | DraftICU 77
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_BAD_OPTION |  (missing) | InternalICU 77

## Other

Other existing drafts in ICU 77

| File | API | ICU 76 | ICU 77 |
|---|---|---|---|
| coll.h |  auto icu::Collator::equal_to() const | DraftICU 76 | 
| coll.h |  auto icu::Collator::greater() const | DraftICU 76 | 
| coll.h |  auto icu::Collator::greater_equal() const | DraftICU 76 | 
| coll.h |  auto icu::Collator::less() const | DraftICU 76 | 
| coll.h |  auto icu::Collator::less_equal() const | DraftICU 76 | 
| coll.h |  auto icu::Collator::not_equal_to() const | DraftICU 76 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getLightSpeed() | DraftICU 76 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getNight() | DraftICU 76 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createLightSpeed(UErrorCode&amp;) | DraftICU 76 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createNight(UErrorCode&amp;) | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UIndicConjunctBreak::U_INCB_CONSONANT | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UIndicConjunctBreak::U_INCB_EXTEND | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UIndicConjunctBreak::U_INCB_LINKER | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UIndicConjunctBreak::U_INCB_NONE | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_INDIC_CONJUNCT_BREAK | DraftICU 76 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_MODIFIER_COMBINING_MARK | DraftICU 76 | 
| uniset.h |  U_HEADER_NESTED_NAMESPACE::USetCodePoints icu::UnicodeSet::codePoints() const | DraftICU 76 | 
| uniset.h |  U_HEADER_NESTED_NAMESPACE::USetRanges icu::UnicodeSet::ranges() const | DraftICU 76 | 
| uniset.h |  U_HEADER_NESTED_NAMESPACE::USetStrings icu::UnicodeSet::strings() const | DraftICU 76 | 
| unistr.h |  UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const S&amp;) | DraftICU 76 | 
| unistr.h |  UnicodeString&amp; icu::UnicodeString::append(const S&amp;) | DraftICU 76 | 
| unistr.h |  UnicodeString&amp; icu::UnicodeString::operator+=(const S&amp;) | DraftICU 76 | 
| unistr.h |  UnicodeString&amp; icu::UnicodeString::operator=(const S&amp;) | DraftICU 76 | 
| unistr.h |  bool icu::UnicodeString::operator!=(const S&amp;) const | DraftICU 76 | 
| unistr.h |  bool icu::UnicodeString::operator==(const S&amp;) const | DraftICU 76 | 
| unistr.h |  icu::UnicodeString::operator std::u16string_view() const | DraftICU 76 | 
| unistr.h |  icu::UnicodeString::operator std::wstring_view() const | DraftICU 76 | 
| unistr.h |  <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const S&amp;) | DraftICU 76 | 
| unistr.h |  <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const UnicodeString&amp;) | DraftICU 76 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| uset.h |  CodePointRange U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator* () const | DraftICU 76 | 
| uset.h |  USetCodePointIterator &amp; U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++() | DraftICU 76 | 
| uset.h |  USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++(int) | DraftICU 76 | 
| uset.h |  USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::begin() const | DraftICU 76 | 
| uset.h |  USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::end() const | DraftICU 76 | 
| uset.h |  USetRangeIterator &amp; U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++() | DraftICU 76 | 
| uset.h |  USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++(int) | DraftICU 76 | 
| uset.h |  USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::begin() const | DraftICU 76 | 
| uset.h |  USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::end() const | DraftICU 76 | 
| uset.h |  USetStringIterator &amp; U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++() | DraftICU 76 | 
| uset.h |  USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++(int) | DraftICU 76 | 
| uset.h |  USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::begin() const | DraftICU 76 | 
| uset.h |  USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::end() const | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::USetCodePointIterator(const USetCodePointIterator&amp;)=default | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USet*) | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USetCodePoints&amp;)=default | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetRangeIterator::USetRangeIterator(const USetRangeIterator&amp;)=default | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USet*) | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USetRanges&amp;)=default | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetStringIterator::USetStringIterator(const USetStringIterator&amp;)=default | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USet*) | DraftICU 76 | 
| uset.h |  U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USetStrings&amp;)=default | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator!=(const USetCodePointIterator&amp;) const | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator==(const USetCodePointIterator&amp;) const | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator!=(const USetRangeIterator&amp;) const | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator==(const USetRangeIterator&amp;) const | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator!=(const USetStringIterator&amp;) const | DraftICU 76 | 
| uset.h |  bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator==(const USetStringIterator&amp;) const | DraftICU 76 | 
| uset.h |  UChar32 U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator* () const | DraftICU 76 | 
| uset.h |  const UChar* uset_getString(const USet*, int32_t, int32_t*) | DraftICU 76 | 
| uset.h |  int32_t uset_getStringCount(const USet*) | DraftICU 76 | 
| uset.h |  std::u16string_view U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator* () const | DraftICU 76 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Wed Feb 19 16:10:28 PST 2025

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  