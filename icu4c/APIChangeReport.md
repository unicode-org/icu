
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 75 with ICU 76

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 75](#removed)
- [Deprecated or Obsoleted in ICU 76](#deprecated)
- [Changed in  ICU 76](#changed)
- [Promoted to stable in ICU 76](#promoted)
- [Added in ICU 76](#added)
- [Other existing drafts in ICU 76](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 75
  
| File | API | ICU 75 | ICU 76 |
|---|---|---|---|
| messageformat2_data_model.h | Builder&amp; icu::message2::MFDataModel::Builder::addUnsupportedStatement(UnsupportedStatement&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Operator::Builder::setReserved(Reserved&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Reserved::Builder::add(Literal&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Reserved::Builder::operator=(Builder&amp;&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::Reserved::Builder::operator=(const Builder&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::addExpression(Expression&amp;&amp;, UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::operator=(Builder&amp;&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::operator=(const Builder&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::setBody(Reserved&amp;&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Builder&amp; icu::message2::data_model::UnsupportedStatement::Builder::setKeyword(const UnicodeString&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Reserved icu::message2::data_model::Reserved::Builder::build(UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | Reserved&amp; icu::message2::data_model::Reserved::operator=(Reserved) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | UnsupportedStatement icu::message2::data_model::UnsupportedStatement::Builder::build(UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | UnsupportedStatement&amp; icu::message2::data_model::UnsupportedStatement::operator=(UnsupportedStatement) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | bool icu::message2::data_model::Expression::isReserved() const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | const Literal&amp; icu::message2::data_model::Reserved::getPart(int32_t) const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | const Reserved* icu::message2::data_model::UnsupportedStatement::getBody(UErrorCode&amp;) const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | const UnicodeString &amp; icu::message2::data_model::UnsupportedStatement::getKeyword() const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::Builder(Builder&amp;&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::Builder(UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::Builder(const Builder&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Builder::~Builder() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Reserved() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::Reserved(const Reserved&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::Reserved::~Reserved() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::Builder(Builder&amp;&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::Builder(UErrorCode&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::Builder(const Builder&amp;)=delete |   _untagged _  | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::Builder::~Builder() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::UnsupportedStatement() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::UnsupportedStatement(const UnsupportedStatement&amp;) |  InternalICU 75 | (missing)
| messageformat2_data_model.h | icu::message2::data_model::UnsupportedStatement::~UnsupportedStatement() |  InternalICU 75 | (missing)
| messageformat2_data_model.h | int32_t icu::message2::data_model::Reserved::numParts() const |  InternalICU 75 | (missing)
| messageformat2_data_model.h | std::vector&lt; Expression &gt; icu::message2::data_model::UnsupportedStatement::getExpressions() const |  InternalICU 75 | (missing)
| simplenumberformatter.h | void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) |  DeprecatedICU 75 | (missing)
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const char16_t*) |  StableICU 2.0 | (missing)
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const uint16_t*) |  StableICU 59 | (missing)
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const wchar_t*) |  StableICU 59 | (missing)
| unistr.h | UnicodeString&amp; icu::UnicodeString::remove(int32_t, int32_t length=(int32_t)) |  StableICU 2.0 | (missing)
| unistr.h | UnicodeString&amp; icu::UnicodeString::removeBetween(int32_t, int32_t limit=(int32_t)) |  StableICU 2.0 | (missing)
| usimplenumberformatter.h | void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) |  DeprecatedICU 75 | (missing)
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNSUPPORTED_EXPRESSION_ERROR |  InternalICU 75 | (missing)
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_UNSUPPORTED_STATEMENT_ERROR |  InternalICU 75 | (missing)

## Deprecated

Deprecated or Obsoleted in ICU 76
  
| File | API | ICU 75 | ICU 76 |
|---|---|---|---|

## Changed

Changed in  ICU 76 (old, new)


  
| File | API | ICU 75 | ICU 76 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGasolineEnergyDensity() |  Draft→StableICU 74
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGasolineEnergyDensity(UErrorCode&amp;) |  Draft→StableICU 74
| measure.h | bool icu::Measure::operator!=(const UObject&amp;) const |  Draft→StableICU 74
| normalizer2.h | <tt>static</tt> const Normalizer2* icu::Normalizer2::getNFKCSimpleCasefoldInstance(UErrorCode&amp;) |  Draft→StableICU 74
| timezone.h | <tt>static</tt> UnicodeString&amp; icu::TimeZone::getIanaID(const UnicodeString&amp;, UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 74
| ucal.h | int32_t ucal_getIanaTimeZoneID(const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDS_UNARY_OPERATOR |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_CONTINUE |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_START |  Draft→StableICU 74
| ulocale.h | UEnumeration* ulocale_getKeywords(const ULocale*, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | UEnumeration* ulocale_getUnicodeKeywords(const ULocale*, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | ULocale* ulocale_openForLanguageTag(const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | ULocale* ulocale_openForLocaleID(const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | bool ulocale_isBogus(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getBaseName(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getLanguage(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getLocaleID(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getRegion(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getScript(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getVariant(const ULocale*) |  Draft→StableICU 74
| ulocale.h | int32_t ulocale_getKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | int32_t ulocale_getUnicodeKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | void ulocale_close(ULocale*) |  Draft→StableICU 74
| ulocbuilder.h | ULocale* ulocbld_buildULocale(ULocaleBuilder*, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | ULocaleBuilder* ulocbld_open() |  Draft→StableICU 74
| ulocbuilder.h | bool ulocbld_copyErrorTo(const ULocaleBuilder*, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | int32_t ulocbld_buildLanguageTag(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | int32_t ulocbld_buildLocaleID(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_addUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_adoptULocale(ULocaleBuilder*, ULocale*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_clear(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_clearExtensions(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_close(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_removeUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setExtension(ULocaleBuilder*, char, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLanguage(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLanguageTag(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLocale(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setRegion(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setScript(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setUnicodeLocaleKeyword(ULocaleBuilder*, const char*, int32_t, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setVariant(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| unorm2.h | const UNormalizer2* unorm2_getNFKCSimpleCasefoldInstance(UErrorCode*) |  Draft→StableICU 74
| uspoof.h | icu::UnicodeString&amp; uspoof_getBidiSkeletonUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, icu::UnicodeString&amp;, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | int32_t uspoof_getBidiSkeleton(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | int32_t uspoof_getBidiSkeletonUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusable(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, const UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, const icu::UnicodeString&amp;, UErrorCode*) |  Draft→StableICU 74

## Promoted

Promoted to stable in ICU 76
  
| File | API | ICU 75 | ICU 76 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGasolineEnergyDensity() |  Draft→StableICU 74
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGasolineEnergyDensity(UErrorCode&amp;) |  Draft→StableICU 74
| measure.h | bool icu::Measure::operator!=(const UObject&amp;) const |  Draft→StableICU 74
| normalizer2.h | <tt>static</tt> const Normalizer2* icu::Normalizer2::getNFKCSimpleCasefoldInstance(UErrorCode&amp;) |  Draft→StableICU 74
| timezone.h | <tt>static</tt> UnicodeString&amp; icu::TimeZone::getIanaID(const UnicodeString&amp;, UnicodeString&amp;, UErrorCode&amp;) |  Draft→StableICU 74
| ucal.h | int32_t ucal_getIanaTimeZoneID(const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_EGYPTIAN_HIEROGLYPHS_EXTENDED_A |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_GARAY |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_GURUNG_KHEMA |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KIRAT_RAI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MYANMAR_EXTENDED_C |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_OL_ONAL |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SUNUWAR |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SYMBOLS_FOR_LEGACY_COMPUTING_SUPPLEMENT |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TODHRI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TULU_TIGALARI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicSyllabicCategory::U_INSC_REORDERING_KILLER |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_KASHMIRI_YEH |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDS_UNARY_OPERATOR |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_CONTINUE |  Draft→StableICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_START |  Draft→StableICU 74
| ulocale.h | UEnumeration* ulocale_getKeywords(const ULocale*, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | UEnumeration* ulocale_getUnicodeKeywords(const ULocale*, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | ULocale* ulocale_openForLanguageTag(const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | ULocale* ulocale_openForLocaleID(const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | bool ulocale_isBogus(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getBaseName(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getLanguage(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getLocaleID(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getRegion(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getScript(const ULocale*) |  Draft→StableICU 74
| ulocale.h | const char* ulocale_getVariant(const ULocale*) |  Draft→StableICU 74
| ulocale.h | int32_t ulocale_getKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | int32_t ulocale_getUnicodeKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocale.h | void ulocale_close(ULocale*) |  Draft→StableICU 74
| ulocbuilder.h | ULocale* ulocbld_buildULocale(ULocaleBuilder*, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | ULocaleBuilder* ulocbld_open() |  Draft→StableICU 74
| ulocbuilder.h | bool ulocbld_copyErrorTo(const ULocaleBuilder*, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | int32_t ulocbld_buildLanguageTag(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | int32_t ulocbld_buildLocaleID(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_addUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_adoptULocale(ULocaleBuilder*, ULocale*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_clear(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_clearExtensions(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_close(ULocaleBuilder*) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_removeUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setExtension(ULocaleBuilder*, char, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLanguage(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLanguageTag(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setLocale(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setRegion(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setScript(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setUnicodeLocaleKeyword(ULocaleBuilder*, const char*, int32_t, const char*, int32_t) |  Draft→StableICU 74
| ulocbuilder.h | void ulocbld_setVariant(ULocaleBuilder*, const char*, int32_t) |  Draft→StableICU 74
| unistr.h | UnicodeString&amp; icu::UnicodeString::remove(int32_t, int32_t length=static_cast&lt; int32_t &gt;(INT32_MAX)) |  (missing) | StableICU 2.0
| unistr.h | UnicodeString&amp; icu::UnicodeString::removeBetween(int32_t, int32_t limit=static_cast&lt; int32_t &gt;(INT32_MAX)) |  (missing) | StableICU 2.0
| unorm2.h | const UNormalizer2* unorm2_getNFKCSimpleCasefoldInstance(UErrorCode*) |  Draft→StableICU 74
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_GARAY |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_GURUNG_KHEMA |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KIRAT_RAI |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_OL_ONAL |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SUNUWAR |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TODHRI |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TULU_TIGALARI |  (missing) | StableICU 76| *(Born Stable)* |
| uspoof.h | icu::UnicodeString&amp; uspoof_getBidiSkeletonUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, icu::UnicodeString&amp;, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | int32_t uspoof_getBidiSkeleton(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | int32_t uspoof_getBidiSkeletonUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusable(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, const UChar*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, const char*, int32_t, UErrorCode*) |  Draft→StableICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, const icu::UnicodeString&amp;, UErrorCode*) |  Draft→StableICU 74

## Added

Added in ICU 76
  
| File | API | ICU 75 | ICU 76 |
|---|---|---|---|
| coll.h | auto icu::Collator::equal_to() const |  (missing) | DraftICU 76
| coll.h | auto icu::Collator::greater() const |  (missing) | DraftICU 76
| coll.h | auto icu::Collator::greater_equal() const |  (missing) | DraftICU 76
| coll.h | auto icu::Collator::less() const |  (missing) | DraftICU 76
| coll.h | auto icu::Collator::less_equal() const |  (missing) | DraftICU 76
| coll.h | auto icu::Collator::not_equal_to() const |  (missing) | DraftICU 76
| messageformat2.h | Builder&amp; icu::message2::MessageFormatter::Builder::setErrorHandlingBehavior(UMFErrorHandlingBehavior) |  (missing) | InternalICU 76
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFErrorHandlingBehavior {} |  (missing) | InternalICU 76
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFErrorHandlingBehavior::U_MF_BEST_EFFORT |  (missing) | InternalICU 76
| messageformat2.h | <tt>enum</tt>  							icu::message2::MessageFormatter::UMFErrorHandlingBehavior::U_MF_STRICT |  (missing) | InternalICU 76
| stringpiece.h | icu::StringPiece::operator std::string_view() const |  (missing) | Internal
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_EGYPTIAN_HIEROGLYPHS_EXTENDED_A |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_GARAY |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_GURUNG_KHEMA |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_KIRAT_RAI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_MYANMAR_EXTENDED_C |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_OL_ONAL |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SUNUWAR |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_SYMBOLS_FOR_LEGACY_COMPUTING_SUPPLEMENT |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TODHRI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_TULU_TIGALARI |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_CONSONANT |  (missing) | DraftICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_EXTEND |  (missing) | DraftICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_LINKER |  (missing) | DraftICU 76
| uchar.h | <tt>enum</tt> UIndicConjunctBreak::U_INCB_NONE |  (missing) | DraftICU 76
| uchar.h | <tt>enum</tt> UIndicSyllabicCategory::U_INSC_REORDERING_KILLER |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UJoiningGroup::U_JG_KASHMIRI_YEH |  (missing) | StableICU 76| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_INDIC_CONJUNCT_BREAK |  (missing) | DraftICU 76
| uchar.h | <tt>enum</tt> UProperty::UCHAR_MODIFIER_COMBINING_MARK |  (missing) | DraftICU 76
| ucol.h | U_HEADER_ONLY_NAMESPACE::collator::internal::Predicate&lt; Compare, result &gt;::Predicate(const UCollator*) |  (missing) | Internal
| ucol.h | bool U_HEADER_ONLY_NAMESPACE::collator::internal::Predicate&lt; Compare, result &gt;::operator()(const T&amp;, const U&amp;) const |  (missing) | Internal
| ucol.h | bool U_HEADER_ONLY_NAMESPACE::collator::internal::Predicate&lt; Compare, result &gt;::operator()(std::string_view, std::string_view) const |  (missing) | Internal
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetCodePoints icu::UnicodeSet::codePoints() const |  (missing) | DraftICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::begin() const |  (missing) | DraftICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetElementIterator icu::UnicodeSet::end() const |  (missing) | DraftICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetRanges icu::UnicodeSet::ranges() const |  (missing) | DraftICU 76
| uniset.h | U_HEADER_NESTED_NAMESPACE::USetStrings icu::UnicodeSet::strings() const |  (missing) | DraftICU 76
| unistr.h | UNISTR_FROM_STRING_EXPLICIT icu::UnicodeString::UnicodeString(const S&amp;) |  (missing) | DraftICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::append(const S&amp;) |  (missing) | DraftICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator+=(const S&amp;) |  (missing) | DraftICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::operator=(const S&amp;) |  (missing) | DraftICU 76
| unistr.h | UnicodeString&amp; icu::UnicodeString::remove(int32_t, int32_t length=static_cast&lt; int32_t &gt;(INT32_MAX)) |  (missing) | StableICU 2.0
| unistr.h | UnicodeString&amp; icu::UnicodeString::removeBetween(int32_t, int32_t limit=static_cast&lt; int32_t &gt;(INT32_MAX)) |  (missing) | StableICU 2.0
| unistr.h | bool icu::UnicodeString::operator!=(const S&amp;) const |  (missing) | DraftICU 76
| unistr.h | bool icu::UnicodeString::operator==(const S&amp;) const |  (missing) | DraftICU 76
| unistr.h | icu::UnicodeString::operator std::u16string_view() const |  (missing) | DraftICU 76
| unistr.h | icu::UnicodeString::operator std::wstring_view() const |  (missing) | DraftICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const S&amp;) |  (missing) | DraftICU 76
| unistr.h | <tt>static</tt> UnicodeString icu::UnicodeString::readOnlyAlias(const UnicodeString&amp;) |  (missing) | DraftICU 76
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_GARAY |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_GURUNG_KHEMA |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_KIRAT_RAI |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_OL_ONAL |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_SUNUWAR |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TODHRI |  (missing) | StableICU 76| *(Born Stable)* |
| uscript.h | <tt>enum</tt> UScriptCode::USCRIPT_TULU_TIGALARI |  (missing) | StableICU 76| *(Born Stable)* |
| uset.h | CodePointRange U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator* () const |  (missing) | DraftICU 76
| uset.h | USetCodePointIterator &amp; U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++() |  (missing) | DraftICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator++(int) |  (missing) | DraftICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::begin() const |  (missing) | DraftICU 76
| uset.h | USetCodePointIterator U_HEADER_ONLY_NAMESPACE::USetCodePoints::end() const |  (missing) | DraftICU 76
| uset.h | USetElementIterator &amp; U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++() |  (missing) | DraftICU 76
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator++(int) |  (missing) | DraftICU 76
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::begin() const |  (missing) | DraftICU 76
| uset.h | USetElementIterator U_HEADER_ONLY_NAMESPACE::USetElements::end() const |  (missing) | DraftICU 76
| uset.h | USetRangeIterator &amp; U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++() |  (missing) | DraftICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator++(int) |  (missing) | DraftICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::begin() const |  (missing) | DraftICU 76
| uset.h | USetRangeIterator U_HEADER_ONLY_NAMESPACE::USetRanges::end() const |  (missing) | DraftICU 76
| uset.h | USetStringIterator &amp; U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++() |  (missing) | DraftICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator++(int) |  (missing) | DraftICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::begin() const |  (missing) | DraftICU 76
| uset.h | USetStringIterator U_HEADER_ONLY_NAMESPACE::USetStrings::end() const |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::USetCodePointIterator(const USetCodePointIterator&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USet*) |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetCodePoints::USetCodePoints(const USetCodePoints&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElementIterator::USetElementIterator(const USetElementIterator&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USet*) |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetElements::USetElements(const USetElements&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRangeIterator::USetRangeIterator(const USetRangeIterator&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USet*) |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetRanges::USetRanges(const USetRanges&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStringIterator::USetStringIterator(const USetStringIterator&amp;)=default |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USet*) |  (missing) | DraftICU 76
| uset.h | U_HEADER_ONLY_NAMESPACE::USetStrings::USetStrings(const USetStrings&amp;)=default |  (missing) | DraftICU 76
| uset.h | UnicodeString U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator* () const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator!=(const USetCodePointIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator==(const USetCodePointIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator!=(const USetElementIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetElementIterator::operator==(const USetElementIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator!=(const USetRangeIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetRangeIterator::operator==(const USetRangeIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator!=(const USetStringIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | bool U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator==(const USetStringIterator&amp;) const |  (missing) | DraftICU 76
| uset.h | UChar32 U_HEADER_ONLY_NAMESPACE::USetCodePointIterator::operator* () const |  (missing) | DraftICU 76
| uset.h | const UChar* uset_getString(const USet*, int32_t, int32_t*) |  (missing) | DraftICU 76
| uset.h | int32_t uset_getStringCount(const USet*) |  (missing) | DraftICU 76
| uset.h | std::u16string_view U_HEADER_ONLY_NAMESPACE::USetStringIterator::operator* () const |  (missing) | DraftICU 76
| utypes.h | <tt>#define</tt> U_SHOW_CPLUSPLUS_HEADER_API |  (missing) | Internal
| utypes.h | <tt>enum</tt> UErrorCode::U_MF_DUPLICATE_VARIANT_ERROR |  (missing) | InternalICU 76

## Other

Other existing drafts in ICU 76

| File | API | ICU 75 | ICU 76 |
|---|---|---|---|
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| numberformatter.h |  UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() &amp;&amp; | DraftICU 75 | 
| numberformatter.h |  UnlocalizedNumberFormatter icu::number::LocalizedNumberFormatter::withoutLocale() const &amp; | DraftICU 75 | 
| numberrangeformatter.h |  UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() &amp;&amp; | DraftICU 75 | 
| numberrangeformatter.h |  UnlocalizedNumberRangeFormatter icu::number::LocalizedNumberRangeFormatter::withoutLocale() const &amp; | DraftICU 75 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setMaximumIntegerDigits(uint32_t, UErrorCode&amp;) | DraftICU 75 | 
| uchar.h |  bool u_hasIDType(UChar32, UIdentifierType) | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_ALLOWED | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierStatus::U_ID_STATUS_RESTRICTED | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEFAULT_IGNORABLE | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_DEPRECATED | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_EXCLUSION | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_INCLUSION | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_LIMITED_USE | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_CHARACTER | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_NFKC | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_NOT_XID | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_OBSOLETE | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_RECOMMENDED | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_TECHNICAL | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UIdentifierType::U_ID_TYPE_UNCOMMON_USE | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_STATUS | DraftICU 75 | 
| uchar.h |  <tt>enum</tt> UProperty::UCHAR_IDENTIFIER_TYPE | DraftICU 75 | 
| uchar.h |  int32_t u_getIDTypes(UChar32, UIdentifierType*, int32_t, UErrorCode*) | DraftICU 75 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| usimplenumberformatter.h |  void usnum_setMaximumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 75 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Tue Sep 24 13:19:44 PDT 2024

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  