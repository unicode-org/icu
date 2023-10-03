
  
<!--
 Copyright © 2019 and later: Unicode, Inc. and others.
 License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C API Comparison: ICU 73 with ICU 74

> _Note_ Markdown format of this document is new for ICU 65.

- [Removed from ICU 73](#removed)
- [Deprecated or Obsoleted in ICU 74](#deprecated)
- [Changed in  ICU 74](#changed)
- [Promoted to stable in ICU 74](#promoted)
- [Added in ICU 74](#added)
- [Other existing drafts in ICU 74](#other)
- [Signature Simplifications](#simplifications)

## Removed

Removed from ICU 73
  
| File | API | ICU 73 | ICU 74 |
|---|---|---|---|

## Deprecated

Deprecated or Obsoleted in ICU 74
  
| File | API | ICU 73 | ICU 74 |
|---|---|---|---|

## Changed

Changed in  ICU 74 (old, new)


  
| File | API | ICU 73 | ICU 74 |
|---|---|---|---|
| displayoptions.h | Builder icu::DisplayOptions::copyToBuilder() const |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setCapitalization(UDisplayOptionsCapitalization) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setDisplayLength(UDisplayOptionsDisplayLength) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setGrammaticalCase(UDisplayOptionsGrammaticalCase) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setNameStyle(UDisplayOptionsNameStyle) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setNounClass(UDisplayOptionsNounClass) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setPluralCategory(UDisplayOptionsPluralCategory) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setSubstituteHandling(UDisplayOptionsSubstituteHandling) |  Draft→StableICU 72
| displayoptions.h | DisplayOptions icu::DisplayOptions::Builder::build() |  Draft→StableICU 72
| displayoptions.h | DisplayOptions&amp; icu::DisplayOptions::operator=(DisplayOptions&amp;&amp;)=default |  Draft→StableICU 72
| displayoptions.h | DisplayOptions&amp; icu::DisplayOptions::operator=(const DisplayOptions&amp;)=default |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsCapitalization icu::DisplayOptions::getCapitalization() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsDisplayLength icu::DisplayOptions::getDisplayLength() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsGrammaticalCase icu::DisplayOptions::getGrammaticalCase() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsNameStyle icu::DisplayOptions::getNameStyle() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsNounClass icu::DisplayOptions::getNounClass() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsPluralCategory icu::DisplayOptions::getPluralCategory() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsSubstituteHandling icu::DisplayOptions::getSubstituteHandling() const |  Draft→StableICU 72
| displayoptions.h | icu::DisplayOptions::DisplayOptions(const DisplayOptions&amp;)=default |  Draft→StableICU 72
| displayoptions.h | <tt>static</tt> Builder icu::DisplayOptions::builder() |  Draft→StableICU 72
| formattednumber.h | UDisplayOptionsNounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuarter() |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTonne() |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuarter(UErrorCode&amp;) |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTonne(UErrorCode&amp;) |  Draft→StableICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;) const&amp; |  Draft→StableICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;)&amp;&amp; |  Draft→StableICU 72
| udisplayoptions.h | UDisplayOptionsGrammaticalCase udispopt_fromGrammaticalCaseIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | UDisplayOptionsNounClass udispopt_fromNounClassIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | UDisplayOptionsPluralCategory udispopt_fromPluralCategoryIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getGrammaticalCaseIdentifier(UDisplayOptionsGrammaticalCase) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getNounClassIdentifier(UDisplayOptionsNounClass) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getPluralCategoryIdentifier(UDisplayOptionsPluralCategory) |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_BEGINNING_OF_SENTENCE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_MIDDLE_OF_SENTENCE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_STANDALONE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UI_LIST_OR_MENU |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_FULL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_SHORT |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ABLATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ACCUSATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_COMITATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_DATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ERGATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_GENITIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_INSTRUMENTAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE_COPULATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_NOMINATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_OBLIQUE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_PREPOSITIONAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_SOCIATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_VOCATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_DIALECT_NAMES |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_STANDARD_NAMES |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_ANIMATE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_COMMON |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_FEMININE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_INANIMATE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_MASCULINE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_NEUTER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_OTHER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_PERSONAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_FEW |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_MANY |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ONE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_OTHER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_TWO |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ZERO |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_NO_SUBSTITUTE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_SUBSTITUTE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_UNDEFINED |  Draft→StableICU 72
| unum.h | bool unum_hasAttribute(const UNumberFormat*, UNumberFormatAttribute) |  Draft→StableICU 72

## Promoted

Promoted to stable in ICU 74
  
| File | API | ICU 73 | ICU 74 |
|---|---|---|---|
| displayoptions.h | Builder icu::DisplayOptions::copyToBuilder() const |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setCapitalization(UDisplayOptionsCapitalization) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setDisplayLength(UDisplayOptionsDisplayLength) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setGrammaticalCase(UDisplayOptionsGrammaticalCase) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setNameStyle(UDisplayOptionsNameStyle) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setNounClass(UDisplayOptionsNounClass) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setPluralCategory(UDisplayOptionsPluralCategory) |  Draft→StableICU 72
| displayoptions.h | Builder&amp; icu::DisplayOptions::Builder::setSubstituteHandling(UDisplayOptionsSubstituteHandling) |  Draft→StableICU 72
| displayoptions.h | DisplayOptions icu::DisplayOptions::Builder::build() |  Draft→StableICU 72
| displayoptions.h | DisplayOptions&amp; icu::DisplayOptions::operator=(DisplayOptions&amp;&amp;)=default |  Draft→StableICU 72
| displayoptions.h | DisplayOptions&amp; icu::DisplayOptions::operator=(const DisplayOptions&amp;)=default |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsCapitalization icu::DisplayOptions::getCapitalization() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsDisplayLength icu::DisplayOptions::getDisplayLength() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsGrammaticalCase icu::DisplayOptions::getGrammaticalCase() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsNameStyle icu::DisplayOptions::getNameStyle() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsNounClass icu::DisplayOptions::getNounClass() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsPluralCategory icu::DisplayOptions::getPluralCategory() const |  Draft→StableICU 72
| displayoptions.h | UDisplayOptionsSubstituteHandling icu::DisplayOptions::getSubstituteHandling() const |  Draft→StableICU 72
| displayoptions.h | icu::DisplayOptions::DisplayOptions(const DisplayOptions&amp;)=default |  Draft→StableICU 72
| displayoptions.h | <tt>static</tt> Builder icu::DisplayOptions::builder() |  Draft→StableICU 72
| formattednumber.h | UDisplayOptionsNounClass icu::number::FormattedNumber::getNounClass(UErrorCode&amp;) const |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getQuarter() |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getTonne() |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createQuarter(UErrorCode&amp;) |  Draft→StableICU 72
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createTonne(UErrorCode&amp;) |  Draft→StableICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;) const&amp; |  Draft→StableICU 72
| numberformatter.h | Derived icu::number::NumberFormatterSettings&lt; Derived &gt;::displayOptions(const DisplayOptions&amp;)&amp;&amp; |  Draft→StableICU 72
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_I |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA_PREBASE |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA_START |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_VIRAMA_FINAL |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_VIRAMA |  (missing) | StableICU 74| *(Born Stable)* |
| udisplayoptions.h | UDisplayOptionsGrammaticalCase udispopt_fromGrammaticalCaseIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | UDisplayOptionsNounClass udispopt_fromNounClassIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | UDisplayOptionsPluralCategory udispopt_fromPluralCategoryIdentifier(const char*) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getGrammaticalCaseIdentifier(UDisplayOptionsGrammaticalCase) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getNounClassIdentifier(UDisplayOptionsNounClass) |  Draft→StableICU 72
| udisplayoptions.h | const char* udispopt_getPluralCategoryIdentifier(UDisplayOptionsPluralCategory) |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_BEGINNING_OF_SENTENCE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_MIDDLE_OF_SENTENCE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_STANDALONE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UI_LIST_OR_MENU |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_FULL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_SHORT |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ABLATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ACCUSATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_COMITATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_DATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_ERGATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_GENITIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_INSTRUMENTAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE_COPULATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_LOCATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_NOMINATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_OBLIQUE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_PREPOSITIONAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_SOCIATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_VOCATIVE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_DIALECT_NAMES |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_STANDARD_NAMES |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_ANIMATE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_COMMON |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_FEMININE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_INANIMATE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_MASCULINE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_NEUTER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_OTHER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_PERSONAL |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_FEW |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_MANY |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ONE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_OTHER |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_TWO |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_ZERO |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_NO_SUBSTITUTE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_SUBSTITUTE |  Draft→StableICU 72
| udisplayoptions.h | <tt>enum</tt> UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_UNDEFINED |  Draft→StableICU 72
| unum.h | bool unum_hasAttribute(const UNumberFormat*, UNumberFormatAttribute) |  Draft→StableICU 72

## Added

Added in ICU 74
  
| File | API | ICU 73 | ICU 74 |
|---|---|---|---|
| measunit.h | <tt>static</tt> MeasureUnit icu::MeasureUnit::getGasolineEnergyDensity() |  (missing) | DraftICU 74
| measunit.h | <tt>static</tt> MeasureUnit* icu::MeasureUnit::createGasolineEnergyDensity(UErrorCode&amp;) |  (missing) | DraftICU 74
| measure.h | bool icu::Measure::operator!=(const UObject&amp;) const |  (missing) | DraftICU 74
| normalizer2.h | <tt>static</tt> const Normalizer2* icu::Normalizer2::getNFKCSimpleCasefoldInstance(UErrorCode&amp;) |  (missing) | DraftICU 74
| rbbi.h | bool icu::ExternalBreakEngine::handles(UChar32) const |  (missing) | InternalICU 74
| rbbi.h | bool icu::ExternalBreakEngine::isFor(UChar32, const char*) const |  (missing) | InternalICU 74
| rbbi.h | icu::ExternalBreakEngine::~ExternalBreakEngine() |  (missing) | InternalICU 74
| rbbi.h | int32_t icu::ExternalBreakEngine::fillBreaks(UText*, int32_t, int32_t, int32_t*, int32_t, UErrorCode&amp;) const |  (missing) | InternalICU 74
| rbbi.h | <tt>static</tt> void icu::RuleBasedBreakIterator::registerExternalBreakEngine(ExternalBreakEngine*, UErrorCode&amp;) |  (missing) | InternalICU 74
| timezone.h | <tt>static</tt> UnicodeString&amp; icu::TimeZone::getIanaID(const UnicodeString&amp;, UnicodeString&amp;, UErrorCode&amp;) |  (missing) | DraftICU 74
| ucal.h | int32_t ucal_getIanaTimeZoneID(const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| uchar.h | <tt>enum</tt> UBlockCode::UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_I |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA_PREBASE |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA_START |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_AKSARA |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_VIRAMA_FINAL |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> ULineBreak::U_LB_VIRAMA |  (missing) | StableICU 74| *(Born Stable)* |
| uchar.h | <tt>enum</tt> UProperty::UCHAR_IDS_UNARY_OPERATOR |  (missing) | DraftICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_CONTINUE |  (missing) | DraftICU 74
| uchar.h | <tt>enum</tt> UProperty::UCHAR_ID_COMPAT_MATH_START |  (missing) | DraftICU 74
| ulocale.h | UEnumeration* ulocale_getKeywords(const ULocale*, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | UEnumeration* ulocale_getUnicodeKeywords(const ULocale*, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | ULocale* ulocale_openForLanguageTag(const char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | ULocale* ulocale_openForLocaleID(const char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | bool ulocale_isBogus(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getBaseName(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getLanguage(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getLocaleID(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getRegion(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getScript(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | const char* ulocale_getVariant(const ULocale*) |  (missing) | DraftICU 74
| ulocale.h | int32_t ulocale_getKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | int32_t ulocale_getUnicodeKeywordValue(const ULocale*, const char*, int32_t, char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocale.h | void ulocale_close(ULocale*) |  (missing) | DraftICU 74
| ulocbuilder.h | ULocale* ulocbld_buildULocale(ULocaleBuilder*, UErrorCode*) |  (missing) | DraftICU 74
| ulocbuilder.h | ULocaleBuilder* ulocbld_open() |  (missing) | DraftICU 74
| ulocbuilder.h | bool ulocbld_copyErrorTo(const ULocaleBuilder*, UErrorCode*) |  (missing) | DraftICU 74
| ulocbuilder.h | int32_t ulocbld_buildLanguageTag(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocbuilder.h | int32_t ulocbld_buildLocaleID(ULocaleBuilder*, char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_addUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_adoptULocale(ULocaleBuilder*, ULocale*) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_clear(ULocaleBuilder*) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_clearExtensions(ULocaleBuilder*) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_close(ULocaleBuilder*) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_removeUnicodeLocaleAttribute(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setExtension(ULocaleBuilder*, char, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setLanguage(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setLanguageTag(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setLocale(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setRegion(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setScript(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setUnicodeLocaleKeyword(ULocaleBuilder*, const char*, int32_t, const char*, int32_t) |  (missing) | DraftICU 74
| ulocbuilder.h | void ulocbld_setVariant(ULocaleBuilder*, const char*, int32_t) |  (missing) | DraftICU 74
| unorm2.h | const UNormalizer2* unorm2_getNFKCSimpleCasefoldInstance(UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | icu::UnicodeString&amp; uspoof_getBidiSkeletonUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, icu::UnicodeString&amp;, UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | int32_t uspoof_getBidiSkeleton(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, UChar*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | int32_t uspoof_getBidiSkeletonUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusable(const USpoofChecker*, UBiDiDirection, const UChar*, int32_t, const UChar*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUTF8(const USpoofChecker*, UBiDiDirection, const char*, int32_t, const char*, int32_t, UErrorCode*) |  (missing) | DraftICU 74
| uspoof.h | uint32_t uspoof_areBidiConfusableUnicodeString(const USpoofChecker*, UBiDiDirection, const icu::UnicodeString&amp;, const icu::UnicodeString&amp;, UErrorCode*) |  (missing) | DraftICU 74

## Other

Other existing drafts in ICU 74

| File | API | ICU 73 | ICU 74 |
|---|---|---|---|
| calendar.h |  bool icu::Calendar::inTemporalLeapYear(UErrorCode&amp;) const | DraftICU 73 | 
| calendar.h |  const char* icu::Calendar::getTemporalMonthCode(UErrorCode&amp;) const | DraftICU 73 | 
| calendar.h |  void icu::Calendar::setTemporalMonthCode(const char*, UErrorCode&amp;) | DraftICU 73 | 
| measfmt.h |  void icu::MeasureFormat::parseObject(const UnicodeString&amp;, Formattable&amp;, ParsePosition&amp;) const | DraftICU 53 | 
| measunit.h |  <tt>static</tt> MeasureUnit icu::MeasureUnit::getBeaufort() | DraftICU 73 | 
| measunit.h |  <tt>static</tt> MeasureUnit* icu::MeasureUnit::createBeaufort(UErrorCode&amp;) | DraftICU 73 | 
| numfmt.h |  <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfCeiling | DraftICU 73 | 
| numfmt.h |  <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfFloor | DraftICU 73 | 
| numfmt.h |  <tt>enum</tt>  							icu::NumberFormat::ERoundingMode::kRoundHalfOdd | DraftICU 73 | 
| simplenumberformatter.h |  FormattedNumber icu::number::SimpleNumberFormatter::format(SimpleNumber, UErrorCode&amp;) const | DraftICU 73 | 
| simplenumberformatter.h |  FormattedNumber icu::number::SimpleNumberFormatter::formatInt64(int64_t, UErrorCode&amp;) const | DraftICU 73 | 
| simplenumberformatter.h |  SimpleNumber&amp; icu::number::SimpleNumber::operator=(SimpleNumber&amp;&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  SimpleNumberFormatter&amp; icu::number::SimpleNumberFormatter::operator=(SimpleNumberFormatter&amp;&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumber::SimpleNumber()=default | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumber::SimpleNumber(SimpleNumber&amp;&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumber::~SimpleNumber() | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumberFormatter::SimpleNumberFormatter()=default | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumberFormatter::SimpleNumberFormatter(SimpleNumberFormatter&amp;&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  icu::number::SimpleNumberFormatter::~SimpleNumberFormatter() | DraftICU 73 | 
| simplenumberformatter.h |  <tt>static</tt> SimpleNumber icu::number::SimpleNumber::forInt64(int64_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocale(const icu::Locale&amp;, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndGroupingStrategy(const icu::Locale&amp;, UNumberGroupingStrategy, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  <tt>static</tt> SimpleNumberFormatter icu::number::SimpleNumberFormatter::forLocaleAndSymbolsAndGroupingStrategy(const icu::Locale&amp;, const DecimalFormatSymbols&amp;, UNumberGroupingStrategy, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::multiplyByPowerOfTen(int32_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::roundTo(int32_t, UNumberFormatRoundingMode, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setMinimumFractionDigits(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setMinimumIntegerDigits(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::setSign(USimpleNumberSign, UErrorCode&amp;) | DraftICU 73 | 
| simplenumberformatter.h |  void icu::number::SimpleNumber::truncateStart(uint32_t, UErrorCode&amp;) | DraftICU 73 | 
| ucal.h |  <tt>enum</tt> UCalendarDateFields::UCAL_ORDINAL_MONTH | DraftICU 73 | 
| uregex.h |  <tt>enum</tt> URegexpFlag::UREGEX_CANON_EQ | DraftICU 2.4 | 
| uset.h |  <tt>enum</tt> (anonymous)::USET_SIMPLE_CASE_INSENSITIVE | DraftICU 73 | 
| usimplenumberformatter.h |  USimpleNumber* usnum_openForInt64(int64_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  USimpleNumberFormatter* usnumf_openForLocale(const char*, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  USimpleNumberFormatter* usnumf_openForLocaleAndGroupingStrategy(const char*, UNumberGroupingStrategy, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_MINUS_SIGN | DraftICU 73 | 
| usimplenumberformatter.h |  <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_NO_SIGN | DraftICU 73 | 
| usimplenumberformatter.h |  <tt>enum</tt> USimpleNumberSign::UNUM_SIMPLE_NUMBER_PLUS_SIGN | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_close(USimpleNumber*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_multiplyByPowerOfTen(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_roundTo(USimpleNumber*, int32_t, UNumberFormatRoundingMode, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setMinimumFractionDigits(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setMinimumIntegerDigits(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setSign(USimpleNumber*, USimpleNumberSign, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_setToInt64(USimpleNumber*, int64_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnum_truncateStart(USimpleNumber*, int32_t, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnumf_close(USimpleNumberFormatter*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnumf_format(const USimpleNumberFormatter*, USimpleNumber*, UFormattedNumber*, UErrorCode*) | DraftICU 73 | 
| usimplenumberformatter.h |  void usnumf_formatInt64(const USimpleNumberFormatter*, int64_t, UFormattedNumber*, UErrorCode*) | DraftICU 73 | 

## Simplifications

This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    all possible variations in "original" form.


## Colophon

Contents generated by StableAPI tool on Mon Oct 02 17:52:32 PDT 2023

Copyright © 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
  