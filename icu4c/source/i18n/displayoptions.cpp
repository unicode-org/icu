// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "cstring.h"
#include "unicode/displayoptions.h"
#include "unicode/udisplayoptions.h"

icu::DisplayOptions::Builder icu::DisplayOptions::builder() { return icu::DisplayOptions::Builder(); }

icu::DisplayOptions::Builder icu::DisplayOptions::copyToBuilder() const { return Builder(*this); }

icu::DisplayOptions::DisplayOptions(const Builder &builder) {
    this->grammaticalCase = builder.grammaticalCase;
    this->nounClass = builder.nounClass;
    this->pluralCategory = builder.pluralCategory;
    this->capitalization = builder.capitalization;
    this->nameStyle = builder.nameStyle;
    this->displayLength = builder.displayLength;
    this->substituteHandling = builder.substituteHandling;
}

icu::DisplayOptions::Builder::Builder() {
    // Sets default values.
    this->grammaticalCase = UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED;
    this->nounClass = UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED;
    this->pluralCategory = UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED;
    this->capitalization = UDisplayOptionsCapitalization::UDISPOPT_CAPITALIZATION_UNDEFINED;
    this->nameStyle = UDisplayOptionsNameStyle::UDISPOPT_NAME_STYLE_UNDEFINED;
    this->displayLength = UDisplayOptionsDisplayLength::UDISPOPT_DISPLAY_LENGTH_UNDEFINED;
    this->substituteHandling = UDisplayOptionsSubstituteHandling::UDISPOPT_SUBSTITUTE_HANDLING_UNDEFINED;
}

icu::DisplayOptions::Builder::Builder(const DisplayOptions &displayOptions) {
    this->grammaticalCase = displayOptions.grammaticalCase;
    this->nounClass = displayOptions.nounClass;
    this->pluralCategory = displayOptions.pluralCategory;
    this->capitalization = displayOptions.capitalization;
    this->nameStyle = displayOptions.nameStyle;
    this->displayLength = displayOptions.displayLength;
    this->substituteHandling = displayOptions.substituteHandling;
}

namespace {

const char *grammaticalCasesIds[] = {
    "undefined",           // 0
    "ablative",            // 1
    "accusative",          // 2
    "comitative",          // 3
    "dative",              // 4
    "ergative",            // 5
    "genitive",            // 6
    "instrumental",        // 7
    "locative",            // 8
    "locative_copulative", // 9
    "nominative",          // 10
    "oblique",             // 11
    "prepositional",       // 12
    "sociative",           // 13
    "vocative",            // 14
};

const int32_t grammaticalCasesCount = 15;

} // namespace

const char *udispopt_getGrammaticalCaseIdentifier(UDisplayOptionsGrammaticalCase grammaticalCase) {
    if (grammaticalCase >= 0 && grammaticalCase < grammaticalCasesCount) {
        return grammaticalCasesIds[grammaticalCase];
    }

    return grammaticalCasesIds[0];
}

UDisplayOptionsGrammaticalCase udispopt_fromGrammaticalCaseIdentifier(const char *identifier) {
    for (int32_t i = 0; i < grammaticalCasesCount; i++) {
        if (uprv_strcmp(identifier, grammaticalCasesIds[i]) == 0) {
            return static_cast<UDisplayOptionsGrammaticalCase>(i);
        }
    }

    return UDisplayOptionsGrammaticalCase::UDISPOPT_GRAMMATICAL_CASE_UNDEFINED;
}

namespace {

const char *pluralCategoriesIds[] = {
    "undefined", // 0
    "zero",      // 1
    "one",       // 2
    "two",       // 3
    "few",       // 4
    "many",      // 5
    "other",     // 6
};

} // namespace

const int32_t pluralCategoriesCount = 7;

const char *udispopt_getPluralCategoryIdentifier(UDisplayOptionsPluralCategory pluralCategory) {
    if (pluralCategory >= 0 && pluralCategory < pluralCategoriesCount) {
        return pluralCategoriesIds[pluralCategory];
    }

    return pluralCategoriesIds[0];
}

UDisplayOptionsPluralCategory udispopt_fromPluralCategoryIdentifier(const char *identifier) {
    for (int32_t i = 0; i < pluralCategoriesCount; i++) {
        if (uprv_strcmp(identifier, pluralCategoriesIds[i]) == 0) {
            return static_cast<UDisplayOptionsPluralCategory>(i);
        }
    }

    return UDisplayOptionsPluralCategory::UDISPOPT_PLURAL_CATEGORY_UNDEFINED;
}

namespace {

const char *nounClassesIds[] = {
    "undefined", // 0
    "other",     // 1
    "neuter",    // 2
    "feminine",  // 3
    "masculine", // 4
    "animate",   // 5
    "inanimate", // 6
    "personal",  // 7
    "common",    // 8
};

const int32_t nounClassesCount = 9;

} // namespace

const char *udispopt_getNounClassIdentifier(UDisplayOptionsNounClass nounClass) {
    if (nounClass >= 0 && nounClass < nounClassesCount) {
        return nounClassesIds[nounClass];
    }

    return nounClassesIds[0];
}

UDisplayOptionsNounClass udispopt_fromNounClassIdentifier(const char *identifier) {
    for (int32_t i = 0; i < nounClassesCount; i++) {
        if (uprv_strcmp(identifier, nounClassesIds[i]) == 0) {
            return static_cast<UDisplayOptionsNounClass>(i);
        }
    }

    return UDisplayOptionsNounClass::UDISPOPT_NOUN_CLASS_UNDEFINED;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
