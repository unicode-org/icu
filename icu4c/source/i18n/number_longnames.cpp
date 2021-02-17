// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/simpleformatter.h"
#include "unicode/ures.h"
#include "ureslocs.h"
#include "charstr.h"
#include "uresimp.h"
#include "measunit_impl.h"
#include "number_longnames.h"
#include "number_microprops.h"
#include <algorithm>
#include "cstring.h"
#include "util.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;

namespace {

/**
 * Display Name (this format has no placeholder).
 *
 * Used as an index into the LongNameHandler::simpleFormats array. Units
 * resources cover the normal set of PluralRules keys, as well as `dnam` and
 * `per` forms.
 */
constexpr int32_t DNAM_INDEX = StandardPlural::Form::COUNT;
/**
 * "per" form (e.g. "{0} per day" is day's "per" form).
 *
 * Used as an index into the LongNameHandler::simpleFormats array. Units
 * resources cover the normal set of PluralRules keys, as well as `dnam` and
 * `per` forms.
 */
constexpr int32_t PER_INDEX = StandardPlural::Form::COUNT + 1;
/**
 * Gender of the word, in languages with grammatical gender.
 */
constexpr int32_t GENDER_INDEX = StandardPlural::Form::COUNT + 2;
// Number of keys in the array populated by PluralTableSink.
constexpr int32_t ARRAY_LENGTH = StandardPlural::Form::COUNT + 3;

// TODO(inflections): load this list from resources, after creating a "&set"
// function for use in ldml2icu rules.
const int32_t GENDER_COUNT = 7;
const char *gGenders[GENDER_COUNT] = {"animate",   "common", "feminine", "inanimate",
                                      "masculine", "neuter", "personal"};

const char *getGenderString(UnicodeString uGender, UErrorCode status) {
    CharString gender;
    gender.appendInvariantChars(uGender, status);
    if (U_FAILURE(status)) {
        return "";
    }
    int32_t first = 0;
    int32_t last = GENDER_COUNT;
    while (first < last) {
        int32_t mid = (first + last) / 2;
        int32_t cmp = uprv_strcmp(gender.data(), gGenders[mid]);
        if (cmp == 0) {
            return gGenders[mid];
        } else if (cmp > 0) {
            first = mid + 1;
        } else if (cmp < 0) {
            last = mid;
        }
    }
    return "";
}

static int32_t getIndex(const char* pluralKeyword, UErrorCode& status) {
    // pluralKeyword can also be "dnam", "per", or "gender"
    switch (*pluralKeyword) {
    case 'd':
        if (uprv_strcmp(pluralKeyword + 1, "nam") == 0) {
            return DNAM_INDEX;
        }
        break;
    case 'g':
        if (uprv_strcmp(pluralKeyword + 1, "ender") == 0) {
            return GENDER_INDEX;
        }
        break;
    case 'p':
        if (uprv_strcmp(pluralKeyword + 1, "er") == 0) {
            return PER_INDEX;
        }
        break;
    default:
        break;
    }
    StandardPlural::Form plural = StandardPlural::fromString(pluralKeyword, status);
    return plural;
}

// Selects a string out of the `strings` array which corresponds to the
// specified plural form, with fallback to the OTHER form.
//
// The `strings` array must have ARRAY_LENGTH items: one corresponding to each
// of the plural forms, plus a display name ("dnam") and a "per" form.
static UnicodeString getWithPlural(
        const UnicodeString* strings,
        StandardPlural::Form plural,
        UErrorCode& status) {
    UnicodeString result = strings[plural];
    if (result.isBogus()) {
        result = strings[StandardPlural::Form::OTHER];
    }
    if (result.isBogus()) {
        // There should always be data in the "other" plural variant.
        status = U_INTERNAL_PROGRAM_ERROR;
    }
    return result;
}


//////////////////////////
/// BEGIN DATA LOADING ///
//////////////////////////

class PluralTableSink : public ResourceSink {
  public:
    explicit PluralTableSink(UnicodeString *outArray) : outArray(outArray) {
        // Initialize the array to bogus strings.
        for (int32_t i = 0; i < ARRAY_LENGTH; i++) {
            outArray[i].setToBogus();
        }
    }

    void put(const char *key, ResourceValue &value, UBool /*noFallback*/, UErrorCode &status) U_OVERRIDE {
        ResourceTable pluralsTable = value.getTable(status);
        if (U_FAILURE(status)) { return; }
        for (int32_t i = 0; pluralsTable.getKeyAndValue(i, key, value); ++i) {
            if (uprv_strcmp(key, "case") == 0) {
                continue;
            }
            int32_t index = getIndex(key, status);
            if (U_FAILURE(status)) { return; }
            if (!outArray[index].isBogus()) {
                continue;
            }
            outArray[index] = value.getUnicodeString(status);
            if (U_FAILURE(status)) { return; }
        }
    }

  private:
    UnicodeString *outArray;
};

// NOTE: outArray MUST have room for all StandardPlural values.  No bounds checking is performed.

/**
 * Populates outArray with `locale`-specific values for `unit` through use of
 * PluralTableSink. Only the set of basic units are supported!
 *
 * Reading from resources *unitsNarrow* and *unitsShort* (for width
 * UNUM_UNIT_WIDTH_NARROW), or just *unitsShort* (for width
 * UNUM_UNIT_WIDTH_SHORT). For other widths, it reads just "units".
 *
 * @param unit must be a built-in unit, i.e. must have a type and subtype,
 *     listed in gTypes and gSubTypes in measunit.cpp.
 * @param unitDisplayCase the empty string and "nominative" are treated the
 *     same. For other cases, strings for the requested case are used if found.
 *     (For any missing case-specific data, we fall back to nominative.)
 * @param outArray must be of fixed length ARRAY_LENGTH.
 */
void getMeasureData(const Locale &locale,
                    const MeasureUnit &unit,
                    const UNumberUnitWidth &width,
                    StringPiece unitDisplayCase,
                    UnicodeString *outArray,
                    UErrorCode &status) {
    PluralTableSink sink(outArray);
    LocalUResourceBundlePointer unitsBundle(ures_open(U_ICUDATA_UNIT, locale.getName(), &status));
    if (U_FAILURE(status)) { return; }

    // Map duration-year-person, duration-week-person, etc. to duration-year, duration-week, ...
    // TODO(ICU-20400): Get duration-*-person data properly with aliases.
    StringPiece subtypeForResource;
    int32_t subtypeLen = static_cast<int32_t>(uprv_strlen(unit.getSubtype()));
    if (subtypeLen > 7 && uprv_strcmp(unit.getSubtype() + subtypeLen - 7, "-person") == 0) {
        subtypeForResource = {unit.getSubtype(), subtypeLen - 7};
    } else {
        subtypeForResource = unit.getSubtype();
    }

    CharString key;
    key.append("units", status);
    // TODO(icu-units#140): support gender for other unit widths.
    if (width == UNUM_UNIT_WIDTH_NARROW) {
        key.append("Narrow", status);
    } else if (width == UNUM_UNIT_WIDTH_SHORT) {
        key.append("Short", status);
    }
    key.append("/", status);
    key.append(unit.getType(), status);
    key.append("/", status);
    key.append(subtypeForResource, status);

    // Grab desired case first, if available. Then grab no-case data to fill in
    // the gaps.
    if (width == UNUM_UNIT_WIDTH_FULL_NAME && !unitDisplayCase.empty()) {
        CharString caseKey;
        caseKey.append(key, status);
        caseKey.append("/case/", status);
        caseKey.append(unitDisplayCase, status);

        UErrorCode localStatus = U_ZERO_ERROR;
        ures_getAllItemsWithFallback(unitsBundle.getAlias(), caseKey.data(), sink, localStatus);
        // TODO(icu-units#138): our fallback logic is not spec-compliant: we
        // check the given case, then go straight to the no-case data. The spec
        // states we should first look for case="nominative". As part of #138,
        // either get the spec changed, or add unit tests that warn us if
        // case="nominative" data differs from no-case data?
    }

    UErrorCode localStatus = U_ZERO_ERROR;
    ures_getAllItemsWithFallback(unitsBundle.getAlias(), key.data(), sink, localStatus);
    if (width == UNUM_UNIT_WIDTH_SHORT) {
        if (U_FAILURE(localStatus)) {
            status = localStatus;
        }
        return;
    }

    // TODO(ICU-13353): The fallback to short does not work in ICU4C.
    // Manually fall back to short (this is done automatically in Java).
    key.clear();
    key.append("unitsShort/", status);
    key.append(unit.getType(), status);
    key.append("/", status);
    key.append(subtypeForResource, status);
    ures_getAllItemsWithFallback(unitsBundle.getAlias(), key.data(), sink, status);
}

void getCurrencyLongNameData(const Locale &locale, const CurrencyUnit &currency, UnicodeString *outArray,
                             UErrorCode &status) {
    // In ICU4J, this method gets a CurrencyData from CurrencyData.provider.
    // TODO(ICU4J): Implement this without going through CurrencyData, like in ICU4C?
    PluralTableSink sink(outArray);
    LocalUResourceBundlePointer unitsBundle(ures_open(U_ICUDATA_CURR, locale.getName(), &status));
    if (U_FAILURE(status)) { return; }
    ures_getAllItemsWithFallback(unitsBundle.getAlias(), "CurrencyUnitPatterns", sink, status);
    if (U_FAILURE(status)) { return; }
    for (int32_t i = 0; i < StandardPlural::Form::COUNT; i++) {
        UnicodeString &pattern = outArray[i];
        if (pattern.isBogus()) {
            continue;
        }
        int32_t longNameLen = 0;
        const char16_t *longName = ucurr_getPluralName(
                currency.getISOCurrency(),
                locale.getName(),
                nullptr /* isChoiceFormat */,
                StandardPlural::getKeyword(static_cast<StandardPlural::Form>(i)),
                &longNameLen,
                &status);
        // Example pattern from data: "{0} {1}"
        // Example output after find-and-replace: "{0} US dollars"
        pattern.findAndReplace(UnicodeString(u"{1}"), UnicodeString(longName, longNameLen));
    }
}

UnicodeString getPerUnitFormat(const Locale& locale, const UNumberUnitWidth &width, UErrorCode& status) {
    LocalUResourceBundlePointer unitsBundle(ures_open(U_ICUDATA_UNIT, locale.getName(), &status));
    if (U_FAILURE(status)) { return {}; }
    CharString key;
    key.append("units", status);
    if (width == UNUM_UNIT_WIDTH_NARROW) {
        key.append("Narrow", status);
    } else if (width == UNUM_UNIT_WIDTH_SHORT) {
        key.append("Short", status);
    }
    key.append("/compound/per", status);
    int32_t len = 0;
    const UChar* ptr = ures_getStringByKeyWithFallback(unitsBundle.getAlias(), key.data(), &len, &status);
    return UnicodeString(ptr, len);
}

/**
 * Loads and applies deriveComponent rules from CLDR's grammaticalFeatures.xml.
 *
 * Consider a deriveComponent rule that looks like this:
 *
 *     <deriveComponent feature="case" structure="per" value0="compound" value1="nominative"/>
 *
 * Instantiating an instance as follows:
 *
 *     DerivedComponents d(loc, "case", "per", "foo");
 *
 * Applying the rule in the XML element above, `d.value0()` will be "foo", and
 * `d.value1()` will be "nominative".
 *
 * In case of any kind of failure, value0() and value1() will simply return "".
 */
class DerivedComponents {
  public:
    /**
     * Constructor.
     *
     * The feature and structure parameters must be null-terminated. The string
     * referenced by compoundValue must exist for longer than the
     * DerivedComponents instance.
     */
    DerivedComponents(const Locale &locale,
                      const char *feature,
                      const char *structure,
                      const StringPiece compoundValue) {
        StackUResourceBundle derivationsBundle, stackBundle;
        ures_openDirectFillIn(derivationsBundle.getAlias(), NULL, "grammaticalFeatures", &status);
        ures_getByKey(derivationsBundle.getAlias(), "grammaticalData", derivationsBundle.getAlias(),
                      &status);
        ures_getByKey(derivationsBundle.getAlias(), "derivations", derivationsBundle.getAlias(),
                      &status);
        if (U_FAILURE(status)) {
            return;
        }
        UErrorCode localStatus = U_ZERO_ERROR;
        // TODO: use standard normal locale resolution algorithms rather than just grabbing language:
        ures_getByKey(derivationsBundle.getAlias(), locale.getLanguage(), stackBundle.getAlias(),
                      &localStatus);
        // TODO:
        // - code currently assumes if the locale exists, the rules are there -
        //   instead of falling back to root when the requested rule is missing.
        // - investigate ures.h functions, see if one that uses res_findResource()
        //   might be better (or use res_findResource directly), or maybe help
        //   improve ures documentation to guide function selection?
        if (localStatus == U_MISSING_RESOURCE_ERROR) {
            ures_getByKey(derivationsBundle.getAlias(), "root", stackBundle.getAlias(), &status);
        } else {
            status = localStatus;
        }
        ures_getByKey(stackBundle.getAlias(), "component", stackBundle.getAlias(), &status);
        ures_getByKey(stackBundle.getAlias(), feature, stackBundle.getAlias(), &status);
        ures_getByKey(stackBundle.getAlias(), structure, stackBundle.getAlias(), &status);
        UnicodeString val0 = ures_getUnicodeStringByIndex(stackBundle.getAlias(), 0, &status);
        UnicodeString val1 = ures_getUnicodeStringByIndex(stackBundle.getAlias(), 1, &status);
        if (U_SUCCESS(status)) {
            if (val0.compare(UnicodeString(u"compound")) == 0) {
                sp0 = compoundValue;
            } else {
                memory0.appendInvariantChars(val0, status);
                sp0 = memory0.toStringPiece();
            }
            if (val1.compare(UnicodeString(u"compound")) == 0) {
                sp1 = compoundValue;
            } else {
                memory1.appendInvariantChars(val1, status);
                sp1 = memory1.toStringPiece();
            }
        }
    }
    // The returned StringPiece is only valid as long as both the instance
    // exists, and the compoundValue passed to the constructor is valid.
    StringPiece value0() const {
        return sp0;
    }
    // The returned StringPiece is only valid as long as both the instance
    // exists, and the compoundValue passed to the constructor is valid.
    StringPiece value1() const {
        return sp1;
    }

  private:
    UErrorCode status = U_ZERO_ERROR;

    // Holds strings referred to by value0 and value1;
    CharString memory0, memory1;
    StringPiece sp0, sp1;
};

UnicodeString
getDeriveCompoundRule(Locale locale, const char *feature, const char *structure, UErrorCode &status) {
    StackUResourceBundle derivationsBundle, stackBundle;
    ures_openDirectFillIn(derivationsBundle.getAlias(), NULL, "grammaticalFeatures", &status);
    ures_getByKey(derivationsBundle.getAlias(), "grammaticalData", derivationsBundle.getAlias(),
                  &status);
    ures_getByKey(derivationsBundle.getAlias(), "derivations", derivationsBundle.getAlias(), &status);
    // TODO: use standard normal locale resolution algorithms rather than just grabbing language:
    ures_getByKey(derivationsBundle.getAlias(), locale.getLanguage(), stackBundle.getAlias(), &status);
    // TODO:
    // - code currently assumes if the locale exists, the rules are there -
    //   instead of falling back to root when the requested rule is missing.
    // - investigate ures.h functions, see if one that uses res_findResource()
    //   might be better (or use res_findResource directly), or maybe help
    //   improve ures documentation to guide function selection?
    if (status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        ures_getByKey(derivationsBundle.getAlias(), "root", stackBundle.getAlias(), &status);
    }
    ures_getByKey(stackBundle.getAlias(), "compound", stackBundle.getAlias(), &status);
    ures_getByKey(stackBundle.getAlias(), feature, stackBundle.getAlias(), &status);
    return ures_getUnicodeStringByKey(stackBundle.getAlias(), structure, &status);
}

////////////////////////
/// END DATA LOADING ///
////////////////////////

// TODO: promote this somewhere? It's based on patternprops.cpp' trimWhitespace
const UChar *trimSpaceChars(const UChar *s, int32_t &length) {
    if (length <= 0 || (!u_isJavaSpaceChar(s[0]) && !u_isJavaSpaceChar(s[length - 1]))) {
        return s;
    }
    int32_t start = 0;
    int32_t limit = length;
    while (start < limit && u_isJavaSpaceChar(s[start])) {
        ++start;
    }
    if (start < limit) {
        // There is non-white space at start; we will not move limit below that,
        // so we need not test start<limit in the loop.
        while (u_isJavaSpaceChar(s[limit - 1])) {
            --limit;
        }
    }
    length = limit - start;
    return s + start;
}

} // namespace

void LongNameHandler::forMeasureUnit(const Locale &loc,
                                     const MeasureUnit &unitRef,
                                     const UNumberUnitWidth &width,
                                     StringPiece unitDisplayCase,
                                     const PluralRules *rules,
                                     const MicroPropsGenerator *parent,
                                     LongNameHandler *fillIn,
                                     UErrorCode &status) {
    // Not valid for mixed units that aren't built-in units, and there should
    // not be any built-in mixed units!
    U_ASSERT(uprv_strcmp(unitRef.getType(), "") != 0 ||
             unitRef.getComplexity(status) != UMEASURE_UNIT_MIXED);
    U_ASSERT(fillIn != nullptr);

    if (uprv_strcmp(unitRef.getType(), "") == 0) {
        // Not a built-in unit. Split it up, since we can already format
        // "builtin-per-builtin".
        // TODO(ICU-20941): support more generic case than builtin-per-builtin.
        MeasureUnitImpl fullUnit = MeasureUnitImpl::forMeasureUnitMaybeCopy(unitRef, status);
        if (U_FAILURE(status)) {
            return;
        }
        MeasureUnitImpl unit;
        MeasureUnitImpl perUnit;
        for (int32_t i = 0; i < fullUnit.singleUnits.length(); i++) {
            SingleUnitImpl *subUnit = fullUnit.singleUnits[i];
            if (subUnit->dimensionality > 0) {
                unit.appendSingleUnit(*subUnit, status);
            } else {
                subUnit->dimensionality *= -1;
                perUnit.appendSingleUnit(*subUnit, status);
            }
        }
        forCompoundUnit(loc, std::move(unit).build(status), std::move(perUnit).build(status), width,
                        unitDisplayCase, rules, parent, fillIn, status);
        return;
    }

    UnicodeString simpleFormats[ARRAY_LENGTH];
    getMeasureData(loc, unitRef, width, unitDisplayCase, simpleFormats, status);
    if (U_FAILURE(status)) {
        return;
    }
    fillIn->rules = rules;
    fillIn->parent = parent;
    fillIn->simpleFormatsToModifiers(simpleFormats, {UFIELD_CATEGORY_NUMBER, UNUM_MEASURE_UNIT_FIELD},
                                     status);
    if (!simpleFormats[GENDER_INDEX].isBogus()) {
        fillIn->gender = getGenderString(simpleFormats[GENDER_INDEX], status);
    }
}

void LongNameHandler::forCompoundUnit(const Locale &loc,
                                      const MeasureUnit &unit,
                                      const MeasureUnit &perUnit,
                                      const UNumberUnitWidth &width,
                                      StringPiece unitDisplayCase,
                                      const PluralRules *rules,
                                      const MicroPropsGenerator *parent,
                                      LongNameHandler *fillIn,
                                      UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    if (uprv_strcmp(unit.getType(), "") == 0 || uprv_strcmp(perUnit.getType(), "") == 0) {
        // TODO(ICU-20941): Unsanctioned unit. Not yet fully supported. Set an
        // error code. Once we support not-built-in units here, unitRef may be
        // anything, but if not built-in, perUnit has to be "none".
        status = U_UNSUPPORTED_ERROR;
        return;
    }
    if (fillIn == nullptr) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return;
    }

    DerivedComponents derivedPerCases(loc, "case", "per", unitDisplayCase);

    UnicodeString primaryData[ARRAY_LENGTH];
    getMeasureData(loc, unit, width, derivedPerCases.value0(), primaryData, status);
    if (U_FAILURE(status)) {
        return;
    }
    UnicodeString secondaryData[ARRAY_LENGTH];
    getMeasureData(loc, perUnit, width, derivedPerCases.value1(), secondaryData, status);
    if (U_FAILURE(status)) {
        return;
    }

    // TODO(icu-units#139): implement these rules:
    // <deriveComponent feature="plural" structure="per" ...>
    // This has impact on multiSimpleFormatsToModifiers(...) below too.
    // These rules are currently (ICU 69) all the same and hard-coded below.
    UnicodeString perUnitFormat;
    if (!secondaryData[PER_INDEX].isBogus()) {
        perUnitFormat = secondaryData[PER_INDEX];
    } else {
        UnicodeString rawPerUnitFormat = getPerUnitFormat(loc, width, status);
        if (U_FAILURE(status)) {
            return;
        }
        // rawPerUnitFormat is something like "{0} per {1}"; we need to substitute in the secondary unit.
        SimpleFormatter compiled(rawPerUnitFormat, 2, 2, status);
        if (U_FAILURE(status)) {
            return;
        }
        UnicodeString secondaryFormat = getWithPlural(secondaryData, StandardPlural::Form::ONE, status);
        if (U_FAILURE(status)) {
            return;
        }
        // Some "one" pattern may not contain "{0}". For example in "ar" or "ne" locale.
        SimpleFormatter secondaryCompiled(secondaryFormat, 0, 1, status);
        if (U_FAILURE(status)) {
            return;
        }
        UnicodeString secondaryFormatString = secondaryCompiled.getTextWithNoArguments();
        int32_t trimmedSecondaryLen = secondaryFormatString.length();
        const UChar *trimmedSecondaryString =
            trimSpaceChars(secondaryFormatString.getBuffer(), trimmedSecondaryLen);
        UnicodeString secondaryString(false, trimmedSecondaryString, trimmedSecondaryLen);
        // TODO: Why does UnicodeString need to be explicit in the following line?
        compiled.format(UnicodeString(u"{0}"), secondaryString, perUnitFormat, status);
        if (U_FAILURE(status)) {
            return;
        }
    }
    fillIn->rules = rules;
    fillIn->parent = parent;
    fillIn->multiSimpleFormatsToModifiers(primaryData, perUnitFormat,
                                          {UFIELD_CATEGORY_NUMBER, UNUM_MEASURE_UNIT_FIELD}, status);

    // Gender
    UnicodeString uVal = getDeriveCompoundRule(loc, "gender", "per", status);
    if (U_FAILURE(status)) {
        return;
    }
    U_ASSERT(!uVal.isBogus() && uVal.length() == 1);
    switch (uVal[0]) {
    case u'0':
        fillIn->gender = getGenderString(primaryData[GENDER_INDEX], status);
        break;
    case u'1':
        fillIn->gender = getGenderString(secondaryData[GENDER_INDEX], status);
        break;
    default:
        // Data error. Assert-fail in debug mode, else return no gender.
        U_ASSERT(false);
    }
}

UnicodeString LongNameHandler::getUnitDisplayName(
        const Locale& loc,
        const MeasureUnit& unit,
        UNumberUnitWidth width,
        UErrorCode& status) {
    if (U_FAILURE(status)) {
        return ICU_Utility::makeBogusString();
    }
    UnicodeString simpleFormats[ARRAY_LENGTH];
    getMeasureData(loc, unit, width, "", simpleFormats, status);
    return simpleFormats[DNAM_INDEX];
}

UnicodeString LongNameHandler::getUnitPattern(
        const Locale& loc,
        const MeasureUnit& unit,
        UNumberUnitWidth width,
        StandardPlural::Form pluralForm,
        UErrorCode& status) {
    if (U_FAILURE(status)) {
        return ICU_Utility::makeBogusString();
    }
    UnicodeString simpleFormats[ARRAY_LENGTH];
    getMeasureData(loc, unit, width, "", simpleFormats, status);
    // The above already handles fallback from other widths to short
    if (U_FAILURE(status)) {
        return ICU_Utility::makeBogusString();
    }
    // Now handle fallback from other plural forms to OTHER
    return (!(simpleFormats[pluralForm]).isBogus())? simpleFormats[pluralForm]:
            simpleFormats[StandardPlural::Form::OTHER];
}

LongNameHandler* LongNameHandler::forCurrencyLongNames(const Locale &loc, const CurrencyUnit &currency,
                                                      const PluralRules *rules,
                                                      const MicroPropsGenerator *parent,
                                                      UErrorCode &status) {
    auto* result = new LongNameHandler(rules, parent);
    if (result == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    UnicodeString simpleFormats[ARRAY_LENGTH];
    getCurrencyLongNameData(loc, currency, simpleFormats, status);
    if (U_FAILURE(status)) { return nullptr; }
    result->simpleFormatsToModifiers(simpleFormats, {UFIELD_CATEGORY_NUMBER, UNUM_CURRENCY_FIELD}, status);
    // TODO(inflections): currency gender?
    return result;
}

void LongNameHandler::simpleFormatsToModifiers(const UnicodeString *simpleFormats, Field field,
                                               UErrorCode &status) {
    for (int32_t i = 0; i < StandardPlural::Form::COUNT; i++) {
        StandardPlural::Form plural = static_cast<StandardPlural::Form>(i);
        UnicodeString simpleFormat = getWithPlural(simpleFormats, plural, status);
        if (U_FAILURE(status)) { return; }
        SimpleFormatter compiledFormatter(simpleFormat, 0, 1, status);
        if (U_FAILURE(status)) { return; }
        fModifiers[i] = SimpleModifier(compiledFormatter, field, false, {this, SIGNUM_POS_ZERO, plural});
    }
}

void LongNameHandler::multiSimpleFormatsToModifiers(const UnicodeString *leadFormats, UnicodeString trailFormat,
                                                    Field field, UErrorCode &status) {
    SimpleFormatter trailCompiled(trailFormat, 1, 1, status);
    if (U_FAILURE(status)) { return; }
    for (int32_t i = 0; i < StandardPlural::Form::COUNT; i++) {
        StandardPlural::Form plural = static_cast<StandardPlural::Form>(i);
        UnicodeString leadFormat = getWithPlural(leadFormats, plural, status);
        if (U_FAILURE(status)) { return; }
        UnicodeString compoundFormat;
        trailCompiled.format(leadFormat, compoundFormat, status);
        if (U_FAILURE(status)) { return; }
        SimpleFormatter compoundCompiled(compoundFormat, 0, 1, status);
        if (U_FAILURE(status)) { return; }
        fModifiers[i] = SimpleModifier(compoundCompiled, field, false, {this, SIGNUM_POS_ZERO, plural});
    }
}

void LongNameHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                      UErrorCode &status) const {
    if (parent != NULL) {
        parent->processQuantity(quantity, micros, status);
    }
    StandardPlural::Form pluralForm = utils::getPluralSafe(micros.rounder, rules, quantity, status);
    micros.modOuter = &fModifiers[pluralForm];
    micros.gender = gender;
}

const Modifier* LongNameHandler::getModifier(Signum /*signum*/, StandardPlural::Form plural) const {
    return &fModifiers[plural];
}

void MixedUnitLongNameHandler::forMeasureUnit(const Locale &loc,
                                              const MeasureUnit &mixedUnit,
                                              const UNumberUnitWidth &width,
                                              StringPiece unitDisplayCase,
                                              const PluralRules *rules,
                                              const MicroPropsGenerator *parent,
                                              MixedUnitLongNameHandler *fillIn,
                                              UErrorCode &status) {
    U_ASSERT(mixedUnit.getComplexity(status) == UMEASURE_UNIT_MIXED);
    U_ASSERT(fillIn != nullptr);

    MeasureUnitImpl temp;
    const MeasureUnitImpl &impl = MeasureUnitImpl::forMeasureUnit(mixedUnit, temp, status);
    fillIn->fMixedUnitCount = impl.singleUnits.length();
    fillIn->fMixedUnitData.adoptInstead(new UnicodeString[fillIn->fMixedUnitCount * ARRAY_LENGTH]);
    for (int32_t i = 0; i < fillIn->fMixedUnitCount; i++) {
        // Grab data for each of the components.
        UnicodeString *unitData = &fillIn->fMixedUnitData[i * ARRAY_LENGTH];
        // TODO(CLDR-14502): check from the CLDR-14502 ticket whether this
        // propagation of unitDisplayCase is correct:
        getMeasureData(loc, impl.singleUnits[i]->build(status), width, unitDisplayCase, unitData,
                       status);
    }

    // TODO(icu-units#120): Make sure ICU doesn't output zero-valued
    // high-magnitude fields
    // * for mixed units count N, produce N listFormatters, one for each subset
    //   that might be formatted.
    UListFormatterWidth listWidth = ULISTFMT_WIDTH_SHORT;
    if (width == UNUM_UNIT_WIDTH_NARROW) {
        listWidth = ULISTFMT_WIDTH_NARROW;
    } else if (width == UNUM_UNIT_WIDTH_FULL_NAME) {
        // This might be the same as SHORT in most languages:
        listWidth = ULISTFMT_WIDTH_WIDE;
    }
    fillIn->fListFormatter.adoptInsteadAndCheckErrorCode(
        ListFormatter::createInstance(loc, ULISTFMT_TYPE_UNITS, listWidth, status), status);
    // TODO(ICU-21494): grab gender of each unit, calculate the gender
    // associated with this list formatter, save it for later.
    fillIn->rules = rules;
    fillIn->parent = parent;

    // We need a localised NumberFormatter for the numbers of the bigger units
    // (providing Arabic numerals, for example).
    fillIn->fNumberFormatter = NumberFormatter::withLocale(loc);
}

void MixedUnitLongNameHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                               UErrorCode &status) const {
    U_ASSERT(fMixedUnitCount > 1);
    if (parent != nullptr) {
        parent->processQuantity(quantity, micros, status);
    }
    micros.modOuter = getMixedUnitModifier(quantity, micros, status);
}

const Modifier *MixedUnitLongNameHandler::getMixedUnitModifier(DecimalQuantity &quantity,
                                                               MicroProps &micros,
                                                               UErrorCode &status) const {
    if (micros.mixedMeasuresCount == 0) {
        U_ASSERT(micros.mixedMeasuresCount > 0); // Mixed unit: we must have more than one unit value
        status = U_UNSUPPORTED_ERROR;
        return &micros.helpers.emptyWeakModifier;
    }

    // Algorithm:
    //
    // For the mixed-units measurement of: "3 yard, 1 foot, 2.6 inch", we should
    // find "3 yard" and "1 foot" in micros.mixedMeasures.
    //
    // Obtain long-names with plural forms corresponding to measure values:
    //   * {0} yards, {0} foot, {0} inches
    //
    // Format the integer values appropriately and modify with the format
    // strings:
    //   - 3 yards, 1 foot
    //
    // Use ListFormatter to combine, with one placeholder:
    //   - 3 yards, 1 foot and {0} inches
    //
    // Return a SimpleModifier for this pattern, letting the rest of the
    // pipeline take care of the remaining inches.

    LocalArray<UnicodeString> outputMeasuresList(new UnicodeString[fMixedUnitCount], status);
    if (U_FAILURE(status)) {
        return &micros.helpers.emptyWeakModifier;
    }

    StandardPlural::Form quantityPlural = StandardPlural::Form::OTHER;
    for (int32_t i = 0; i < micros.mixedMeasuresCount; i++) {
        DecimalQuantity fdec;

        // If numbers are negative, only the first number needs to have its
        // negative sign formatted.
        int64_t number = i > 0 ? std::abs(micros.mixedMeasures[i]) : micros.mixedMeasures[i];

        if (micros.indexOfQuantity == i) { // Insert placeholder for `quantity`
            // If quantity is not the first value and quantity is negative
            if (micros.indexOfQuantity > 0 && quantity.isNegative()) {
                quantity.negate();
            }

            StandardPlural::Form quantityPlural =
                utils::getPluralSafe(micros.rounder, rules, quantity, status);
            UnicodeString quantityFormatWithPlural =
                getWithPlural(&fMixedUnitData[i * ARRAY_LENGTH], quantityPlural, status);
            SimpleFormatter quantityFormatter(quantityFormatWithPlural, 0, 1, status);
            quantityFormatter.format(UnicodeString(u"{0}"), outputMeasuresList[i], status);
        } else {
            fdec.setToLong(number);
            StandardPlural::Form pluralForm = utils::getStandardPlural(rules, fdec);
            UnicodeString simpleFormat =
                getWithPlural(&fMixedUnitData[i * ARRAY_LENGTH], pluralForm, status);
            SimpleFormatter compiledFormatter(simpleFormat, 0, 1, status);
            UnicodeString num;
            auto appendable = UnicodeStringAppendable(num);

            fNumberFormatter.formatDecimalQuantity(fdec, status).appendTo(appendable, status);
            compiledFormatter.format(num, outputMeasuresList[i], status);
        }
    }

    // TODO(ICU-21494): implement gender for lists of mixed units. Presumably we
    // can set micros.gender to the gender associated with the list formatter in
    // use below (once we have correct support for that). And then document this
    // appropriately? "getMixedUnitModifier" doesn't sound like it would do
    // something like this.

    // Combine list into a "premixed" pattern
    UnicodeString premixedFormatPattern;
    fListFormatter->format(outputMeasuresList.getAlias(), fMixedUnitCount, premixedFormatPattern,
                           status);
    SimpleFormatter premixedCompiled(premixedFormatPattern, 0, 1, status);
    if (U_FAILURE(status)) {
        return &micros.helpers.emptyWeakModifier;
    }

    micros.helpers.mixedUnitModifier =
        SimpleModifier(premixedCompiled, kUndefinedField, false, {this, SIGNUM_POS_ZERO, quantityPlural});
    return &micros.helpers.mixedUnitModifier;
}

const Modifier *MixedUnitLongNameHandler::getModifier(Signum /*signum*/,
                                                      StandardPlural::Form /*plural*/) const {
    // TODO(units): investigate this method when investigating where
    // ModifierStore::getModifier() gets used. To be sure it remains
    // unreachable:
    UPRV_UNREACHABLE;
    return nullptr;
}

LongNameMultiplexer *LongNameMultiplexer::forMeasureUnits(const Locale &loc,
                                                          const MaybeStackVector<MeasureUnit> &units,
                                                          const UNumberUnitWidth &width,
                                                          StringPiece unitDisplayCase,
                                                          const PluralRules *rules,
                                                          const MicroPropsGenerator *parent,
                                                          UErrorCode &status) {
    LocalPointer<LongNameMultiplexer> result(new LongNameMultiplexer(parent), status);
    if (U_FAILURE(status)) {
        return nullptr;
    }
    U_ASSERT(units.length() > 0);
    if (result->fHandlers.resize(units.length()) == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    result->fMeasureUnits.adoptInstead(new MeasureUnit[units.length()]);
    for (int32_t i = 0, length = units.length(); i < length; i++) {
        const MeasureUnit &unit = *units[i];
        result->fMeasureUnits[i] = unit;
        if (unit.getComplexity(status) == UMEASURE_UNIT_MIXED) {
            MixedUnitLongNameHandler *mlnh = result->fMixedUnitHandlers.createAndCheckErrorCode(status);
            MixedUnitLongNameHandler::forMeasureUnit(loc, unit, width, unitDisplayCase, rules, NULL,
                                                     mlnh, status);
            result->fHandlers[i] = mlnh;
        } else {
            LongNameHandler *lnh = result->fLongNameHandlers.createAndCheckErrorCode(status);
            LongNameHandler::forMeasureUnit(loc, unit, width, unitDisplayCase, rules, NULL, lnh, status);
            result->fHandlers[i] = lnh;
        }
        if (U_FAILURE(status)) {
            return nullptr;
        }
    }
    return result.orphan();
}

void LongNameMultiplexer::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                          UErrorCode &status) const {
    // We call parent->processQuantity() from the Multiplexer, instead of
    // letting LongNameHandler handle it: we don't know which LongNameHandler to
    // call until we've called the parent!
    fParent->processQuantity(quantity, micros, status);

    // Call the correct LongNameHandler based on outputUnit
    for (int i = 0; i < fHandlers.getCapacity(); i++) {
        if (fMeasureUnits[i] == micros.outputUnit) {
            fHandlers[i]->processQuantity(quantity, micros, status);
            return;
        }
    }
    if (U_FAILURE(status)) {
        return;
    }
    // We shouldn't receive any outputUnit for which we haven't already got a
    // LongNameHandler:
    status = U_INTERNAL_PROGRAM_ERROR;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
