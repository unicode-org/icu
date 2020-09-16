// © 2020 and later: Unicode, Inc. and others.
// License  &terms of use: http://www.unicode.org/copyright.html

#include "currencydisplaynames.h"
#include "charstr.h"
#include "cstring.h"
#include "uassert.h"
#include "uhash.h"
#include "umutex.h"
#include "unicode/uloc.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "uresimp.h"
#include "ureslocs.h"

struct UHashtable;

U_NAMESPACE_BEGIN

//------------------------------------------------------------
// Constants

constexpr int ISO_CURRENCY_CODE_LENGTH = 3;

//------------------------------------------------------------
// Resource tags

// Tag for localized display names (symbols) of currencies
static const char CURRENCIES[] = "Currencies";
static const char CURRENCIES_NARROW[] = "Currencies%narrow";
static const char CURRENCIES_FORMAL[] = "Currencies%formal";
static const char CURRENCIES_VARIANT[] = "Currencies%variant";
static const char CURRENCYPLURALS[] = "CurrencyPlurals";

//------------------------------------------------------------
// Variables
static CurrencyDisplayNames *currencyDisplayDataCache = nullptr;
static FormattingData *formattingDataCache = nullptr;
static VariantSymbol *variantSymbolCache = nullptr;
static PluralsData *pluralsDataCache = nullptr;

static UInitOnce initOnce = U_INITONCE_INITIALIZER;

//------------------------------------------------------------
// Code

struct FormattingData : public UMemory {
    const UChar *isoCode;
    UBool isFallback = false;
    UBool isDefault = false;
    UnicodeString displayName = UnicodeString();
    UnicodeString symbol = UnicodeString();
    // Only implementing Currency Display Names.
    // Handle Currency Formatting later.
    // CurrencyFormatInfo formatInfo = nullptr;

    FormattingData(const UChar *isoCode) : isoCode(isoCode) {}
};

struct VariantSymbol : public UMemory {
    const UChar *isoCode;
    const char *variant;
    UBool isFallback = false;
    UBool isDefault = false;
    UnicodeString symbol = UnicodeString();

    VariantSymbol(const UChar *isoCode, const char *variant) : isoCode(isoCode), variant(variant) {}
};

struct PluralsData : public UMemory {
    const UChar *isoCode;
    UBool isFallback = false;
    UBool isDefault = false;
    UHashtable *pluralsStringTable = nullptr;
    
    PluralsData(const UChar *isoCode) : isoCode(isoCode) {}
};

struct CurrencySink : public ResourceSink {

    enum EntrypointTable {
        TOP,
        CURRENCIES_TABLE,
        CURRENCY_PLURALS_TABLE,
        CURRENCY_VARIANT_TABLE,
        CURRENCY_SPACING_TABLE,
        CURRENCY_UNIT_PATTERNS_TABLE
    };

    const UBool noSubstitute;
    const EntrypointTable entrypointTable;
    FormattingData *formattingData = nullptr;
    VariantSymbol *variantSymbol = nullptr;
    PluralsData *pluralsData = nullptr;

    virtual ~CurrencySink();

    CurrencySink(UBool noSubstitute, EntrypointTable entrypointTable)
        : noSubstitute(noSubstitute), entrypointTable(entrypointTable) {}

    virtual void put(const char *key, ResourceValue &value, UBool isRoot, UErrorCode &errorCode) {
        if (noSubstitute && isRoot) {
            return;
        }

        switch (entrypointTable) {
        case CURRENCIES_TABLE:
            consumeCurrenciesEntry(key, value, isRoot, errorCode);
            break;
        case CURRENCY_VARIANT_TABLE:
            consumeCurrenciesVariantEntry(key, value, isRoot, errorCode);
            break;
        case CURRENCY_PLURALS_TABLE:
            consumeCurrencyPluralsEntry(key, value, isRoot, errorCode);
            break;
        default:
            errorCode = U_UNSUPPORTED_ERROR;
            return;
        }
    }

    void consumeCurrenciesEntry(const char *key, ResourceValue &value, UBool isRoot,
                                UErrorCode &errorCode) {
        U_ASSERT(formattingData != nullptr);

        if (value.getType() != UResType::RES_ARRAY) {
            errorCode = U_INVALID_FORMAT_ERROR;
            return;
        }
        ResourceArray resourceArray = value.getArray(errorCode);

        if (formattingData->symbol.isEmpty()) {
            resourceArray.getValue(0, value);
            formattingData->symbol = value.getUnicodeString(errorCode);
            if (errorCode == U_USING_FALLBACK_WARNING) {
                formattingData->isFallback = true;
            }
            if (isRoot) {
                formattingData->isDefault = true;
            }
        }
        if (formattingData->displayName.isEmpty()) {
            resourceArray.getValue(1, value);
            formattingData->displayName = value.getUnicodeString(errorCode);
            if (errorCode == U_USING_FALLBACK_WARNING) {
                formattingData->isFallback = true;
            }
            if (isRoot) {
                formattingData->isDefault = true;
            }
        }
        // If present, the third element is the currency format info.
    }

    void consumeCurrenciesVariantEntry(const char *key, ResourceValue &value, UBool isRoot,
                                       UErrorCode &errorCode) {
        U_ASSERT(variantSymbol != nullptr);

        if (variantSymbol->symbol.isEmpty()) {
            variantSymbol->symbol = value.getUnicodeString(errorCode);
            if (errorCode == U_USING_FALLBACK_WARNING) {
                variantSymbol->isFallback = true;
            }
            if (isRoot) {
                variantSymbol->isDefault = true;
            }
        }
    }

    void consumeCurrencyPluralsEntry(const char *key, ResourceValue &value, UBool isRoot,
                                     UErrorCode &errorCode) {
        U_ASSERT(pluralsData != nullptr);

        if (value.getType() != UResType::RES_TABLE) {
            errorCode = U_INVALID_FORMAT_ERROR;
            return;
        }
        if (pluralsData->pluralsStringTable == nullptr) {
            pluralsData->pluralsStringTable =
                uhash_openSize(uhash_hashLong, uhash_compareLong, uhash_compareUnicodeString,
                               (int32_t)PluralMapBase::CATEGORY_COUNT, &errorCode);
            ResourceTable pluralsTable = value.getTable(errorCode);
            for (int j = 0; pluralsTable.getKeyAndValue(j, key, value); j++) {
                PluralMapBase::Category pluralCategory = PluralMapBase::toCategory(key);
                if (pluralCategory == PluralMapBase::NONE) {
                    errorCode = U_UNSUPPORTED_ERROR;
                }
                if (uhash_get(pluralsData->pluralsStringTable, (int32_t *)pluralCategory) == NULL) {
                    UnicodeString *pluralString = new UnicodeString(value.getUnicodeString(errorCode));
                    uhash_put(pluralsData->pluralsStringTable, (int32_t *)pluralCategory, pluralString,
                              &errorCode);
                }
            }
        }
        if (errorCode == U_USING_FALLBACK_WARNING) {
            pluralsData->isFallback = true;
        }
        if (isRoot) {
            pluralsData->isDefault = true;
        }
    }
};

CurrencySink::~CurrencySink() {}

CurrencyDisplayNames::CurrencyDisplayNames(Locale *locale, UBool noSubstitute)
    : locale(locale), noSubstitute(noSubstitute) {}

CurrencyDisplayNames::~CurrencyDisplayNames() {}

const CurrencyDisplayNames *CurrencyDisplayNames::getInstance(Locale *loc, UErrorCode &errorCode) {
    return getInstance(loc, false, errorCode);
}

const CurrencyDisplayNames *CurrencyDisplayNames::getInstance(Locale *loc, UBool noSubstitute,
                                                              UErrorCode &errorCode) {
    CurrencyDisplayNames *instance = currencyDisplayDataCache;
    if (instance == nullptr || strcmp((instance->locale)->getName(), loc->getName()) != 0 ||
        instance->noSubstitute != noSubstitute) {
        Locale *internalLocale;
        if (loc->isBogus()) {
            internalLocale = new Locale();
        } else {
            internalLocale = new Locale(*loc);
        }
        instance = new CurrencyDisplayNames(internalLocale, noSubstitute);

        currencyDisplayDataCache = instance;
        formattingDataCache = nullptr;
        variantSymbolCache = nullptr;
    }
    return instance;
}

Locale *CurrencyDisplayNames::getLocale() { return locale; }

const UChar *CurrencyDisplayNames::getName(const UChar *isoCode, UErrorCode &errorCode) const {
    FormattingData *formattingData = fetchFormattingData(isoCode, errorCode);

    if (formattingData->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (formattingData->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    if (formattingData->displayName.isEmpty() && !noSubstitute) {
        return isoCode;
    }
    return (&formattingData->displayName)->getBuffer();
}

const UChar *CurrencyDisplayNames::getSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    FormattingData *formattingData = fetchFormattingData(isoCode, errorCode);

    if (formattingData->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (formattingData->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    if (formattingData->symbol.isEmpty() && !noSubstitute) {
        return isoCode;
    }
    return (&formattingData->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getNarrowSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_NARROW, errorCode);

    if (variantSymbol->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        return getSymbol(isoCode, errorCode);
    }
    return (&variantSymbol->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getFormalSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_FORMAL, errorCode);

    if (variantSymbol->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        return getSymbol(isoCode, errorCode);
    }
    return (&variantSymbol->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getVariantSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_VARIANT, errorCode);

    if (variantSymbol->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        return getSymbol(isoCode, errorCode);
    }
    return (&variantSymbol->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getName(const UChar *isoCode, UCurrNameStyle nameStyle,
                                           UErrorCode &errorCode) const {
    switch (nameStyle) {
    case UCURR_SYMBOL_NAME:
        return getSymbol(isoCode, errorCode);
    case UCURR_LONG_NAME:
        return getName(isoCode, errorCode);
    case UCURR_NARROW_SYMBOL_NAME:
        return getNarrowSymbol(isoCode, errorCode);
    case UCURR_FORMAL_SYMBOL_NAME:
        return getFormalSymbol(isoCode, errorCode);
    case UCURR_VARIANT_SYMBOL_NAME:
        return getVariantSymbol(isoCode, errorCode);
    default:
        errorCode = U_UNSUPPORTED_ERROR;
        return nullptr;
    }
}

const UChar *CurrencyDisplayNames::getPluralName(const UChar *isoCode,
                                                 const PluralMapBase::Category pluralCategory,
                                                 UErrorCode &errorCode) const {
    PluralsData *pluralsData = fetchPluralsData(isoCode, errorCode);
    if (pluralsData->isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (pluralsData->isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }
    void *temp = uhash_get(pluralsData->pluralsStringTable, (int32_t *)pluralCategory);
    if (temp == NULL) {
        return nullptr;
    } else {
        return ((UnicodeString *)temp)->getBuffer();
    }
}

FormattingData *CurrencyDisplayNames::fetchFormattingData(const UChar *isoCode,
                                                          UErrorCode &errorCode) const {
    FormattingData *result = formattingDataCache;
    if (result == nullptr || u_strcmp(result->isoCode, isoCode) != 0) {
        UErrorCode ec2 = U_ZERO_ERROR;
        result = new FormattingData(isoCode);
        LocalUResourceBundlePointer currencyResources(
            ures_open(U_ICUDATA_CURR, locale->getName(), &ec2));
        if (U_SUCCESS(ec2)) {
            if ((ec2 == U_USING_DEFAULT_WARNING || ec2 == U_USING_FALLBACK_WARNING) && noSubstitute) {
                // If we fall back and noSubstitute is set then return null.
                errorCode = ec2;
                return nullptr;
            } else {
                errorCode = ec2;
            }
        }

        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCIES_TABLE);
        sink.formattingData = result;
        CharString path;
        path.append(CURRENCIES, errorCode)
            .append('/', errorCode)
            .appendInvariantChars(isoCode, u_strlen(isoCode), errorCode);
        ures_getAllItemsWithFallback(currencyResources.getAlias(), path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            if (noSubstitute) {
                errorCode = ec2;
                result = nullptr;
            } else {
                errorCode = U_USING_FALLBACK_WARNING;
            }
        } else {
            errorCode = ec2;
        }
        formattingDataCache = result;
    }
    return result;
}

VariantSymbol *CurrencyDisplayNames::fetchVariantSymbol(const UChar *isoCode, const char *variant,
                                                        UErrorCode &errorCode) const {
    VariantSymbol *result = variantSymbolCache;
    if (result == nullptr || u_strcmp(result->isoCode, isoCode) != 0 ||
        strcmp(result->variant, variant) != 0) {
        UErrorCode ec2 = U_ZERO_ERROR;
        LocalUResourceBundlePointer currencyResources(
            ures_open(U_ICUDATA_CURR, locale->getName(), &ec2));
        if (U_SUCCESS(ec2)) {
            if ((ec2 == U_USING_DEFAULT_WARNING || ec2 == U_USING_FALLBACK_WARNING) && noSubstitute) {
                // If we fall back and noSubstitute is set then return null.
                errorCode = ec2;
                return nullptr;
            } else {
                errorCode = ec2;
            }
        }

        result = new VariantSymbol(isoCode, variant);
        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCY_VARIANT_TABLE);
        sink.variantSymbol = result;
        CharString path;
        path.append(variant, errorCode)
            .append('/', errorCode)
            .appendInvariantChars(isoCode, u_strlen(isoCode), errorCode);
        ures_getAllItemsWithFallback(currencyResources.getAlias(), path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            if (noSubstitute) {
                errorCode = ec2;
                result = nullptr;
            } else {
                errorCode = U_USING_FALLBACK_WARNING;
            }
        } else {
            errorCode = ec2;
        }
        variantSymbolCache = result;
    }
    return result;
}

PluralsData *CurrencyDisplayNames::fetchPluralsData(const UChar *isoCode, UErrorCode &errorCode) const {
    PluralsData *result = pluralsDataCache;
    if (result == nullptr || u_strcmp(result->isoCode, isoCode) != 0) {
        UErrorCode ec2 = U_ZERO_ERROR;
        result = new PluralsData(isoCode);
        LocalUResourceBundlePointer currencyResources(
            ures_open(U_ICUDATA_CURR, locale->getName(), &ec2));
        if (U_SUCCESS(ec2)) {
            if ((ec2 == U_USING_DEFAULT_WARNING || ec2 == U_USING_FALLBACK_WARNING) && noSubstitute) {
                // If we fall back and noSubstitute is set then return null.
                errorCode = ec2;
                return nullptr;
            } else {
                errorCode = ec2;
            }
        }

        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCY_PLURALS_TABLE);
        sink.pluralsData = result;
        CharString path;
        path.append(CURRENCYPLURALS, errorCode)
            .append('/', errorCode)
            .appendInvariantChars(isoCode, u_strlen(isoCode), errorCode);
        ures_getAllItemsWithFallback(currencyResources.getAlias(), path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            if (noSubstitute) {
                errorCode = ec2;
                result = nullptr;
            } else {
                errorCode = U_USING_FALLBACK_WARNING;
            }
        } else {
            errorCode = ec2;
        }
        pluralsDataCache = result;
    }
    return result;
}
U_NAMESPACE_END
