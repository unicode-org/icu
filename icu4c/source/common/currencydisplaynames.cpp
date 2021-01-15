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

CurrencyDisplayNames::CurrencyDisplayNames(Locale *locale,
                                           UResourceBundle *rb, UBool noSubstitute)
    : locale(locale), rb(rb), noSubstitute(noSubstitute) {}

CurrencyDisplayNames::~CurrencyDisplayNames() {}

const CurrencyDisplayNames *CurrencyDisplayNames::getInstance(const Locale *loc, UErrorCode &errorCode) {
    return getInstance(loc, false, errorCode);
}

const CurrencyDisplayNames *CurrencyDisplayNames::getInstance(const Locale *loc, UBool noSubstitute,
                                                              UErrorCode &errorCode) {
    CurrencyDisplayNames *instance = currencyDisplayDataCache;
    if (instance == nullptr || strcmp((instance->locale)->getName(), loc->getName()) != 0 ||
        instance->noSubstitute != noSubstitute) {
        Locale *internalLocale;
        if (loc->isBogus()) {
            if (noSubstitute) {
                return nullptr;
            }
            internalLocale = new Locale();
            errorCode = U_USING_DEFAULT_WARNING;
        } else {
            internalLocale = new Locale(*loc);
        }
        UErrorCode ec2 = U_ZERO_ERROR;
        UResourceBundle *currencyResources = ures_open(U_ICUDATA_CURR, internalLocale->getName(), &ec2);
        if (U_SUCCESS(ec2)) {
            if ((ec2 == U_USING_DEFAULT_WARNING || ec2 == U_USING_FALLBACK_WARNING) && noSubstitute) {
                // If we fall back and noSubstitute is set then return null.
                return nullptr;
            }
            instance = new CurrencyDisplayNames(internalLocale, currencyResources, noSubstitute);
            currencyDisplayDataCache = instance;
            formattingDataCache = nullptr;
            variantSymbolCache = nullptr;
            pluralsDataCache = nullptr;
            if (ec2 == U_USING_DEFAULT_WARNING) {
                instance->isDefault = true;
            }
            if (ec2 == U_USING_FALLBACK_WARNING) {
                instance->isFallback = true;
            }
            errorCode = ec2;
        }
    } else {
        if (instance->isDefault) {
            errorCode = U_USING_DEFAULT_WARNING;
        } else if (instance->isFallback) {
            errorCode = U_USING_FALLBACK_WARNING;
        }
    }     
    return instance;
}

const Locale *CurrencyDisplayNames::getLocale() { return locale; }

const UChar *CurrencyDisplayNames::getName(const UChar *isoCode, UErrorCode &errorCode) const {
    FormattingData *formattingData = fetchFormattingData(isoCode, errorCode);

   if (formattingData->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
   } else if (formattingData->isDefault || isDefault) {
       errorCode = U_USING_DEFAULT_WARNING;
   } else if (formattingData->isFallback || isFallback) {
       errorCode = U_USING_FALLBACK_WARNING;
   }

   if (formattingData->displayName.isEmpty() && !noSubstitute) {
       return isoCode;
   }
   return (&formattingData->displayName)->getBuffer();
}

const UChar *CurrencyDisplayNames::getSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    FormattingData *formattingData = fetchFormattingData(isoCode, errorCode);

   if (formattingData->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
    } else if (formattingData->isDefault || isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (formattingData->isFallback || isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }

    if (formattingData->symbol.isEmpty() && !noSubstitute) {
        return isoCode;
    }
    return (&formattingData->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getNarrowSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_NARROW, errorCode);

   if (variantSymbol->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
    } else if (variantSymbol->isDefault || isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback || isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }

    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        errorCode = U_USING_FALLBACK_WARNING;
        return getSymbol(isoCode, errorCode);
    }
    return (&variantSymbol->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getFormalSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_FORMAL, errorCode);

   if (variantSymbol->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
    } else if (variantSymbol->isDefault || isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback || isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }

    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        errorCode = U_USING_FALLBACK_WARNING;
        return getSymbol(isoCode, errorCode);
    }
    return (&variantSymbol->symbol)->getBuffer();
}

const UChar *CurrencyDisplayNames::getVariantSymbol(const UChar *isoCode, UErrorCode &errorCode) const {
    VariantSymbol *variantSymbol = fetchVariantSymbol(isoCode, CURRENCIES_VARIANT, errorCode);

   if (variantSymbol->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
    } else if (variantSymbol->isDefault || isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (variantSymbol->isFallback || isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }

    if (variantSymbol->symbol.isEmpty() && !noSubstitute) {
        errorCode = U_USING_FALLBACK_WARNING;
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

    if (pluralsData->isMissing && noSubstitute) {
        errorCode = U_MISSING_RESOURCE_ERROR;
        return nullptr;
    } else if (pluralsData->isDefault || isDefault) {
        errorCode = U_USING_DEFAULT_WARNING;
    } else if (pluralsData->isFallback || isFallback) {
        errorCode = U_USING_FALLBACK_WARNING;
    }

    void *result = nullptr;
    if (!pluralsData->isMissing) {
        result = uhash_get(pluralsData->pluralsStringTable, (int32_t *)pluralCategory);
    }
    if (result == nullptr && noSubstitute) {
        return nullptr;
    }
    if (result == nullptr && !pluralsData->isMissing) {
        // First fall back to the "other" plural variant
        // Note: If plural is already "other", this fallback is benign
        result = uhash_get(pluralsData->pluralsStringTable, (int32_t *)PluralMapBase::Category::OTHER);
    }
    if (result == nullptr) {
        // If that fails, fall back to the display name
        FormattingData *formattingData = fetchFormattingData(isoCode, errorCode);
        if (!formattingData->isMissing) {
            result = &formattingData->displayName;
        }
    }
    if (result == nullptr) {
        // If all else fails, return the ISO code
        return isoCode;
    }
    return ((UnicodeString *)result)->getBuffer();
}

FormattingData *CurrencyDisplayNames::fetchFormattingData(const UChar *isoCode,
                                                          UErrorCode &errorCode) const {
    FormattingData *result = formattingDataCache;
    if (result == nullptr || u_strcmp(result->isoCode, isoCode) != 0) {
        UErrorCode ec2 = errorCode;
        result = new FormattingData(isoCode);
        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCIES_TABLE);
        sink.formattingData = result;
        CharString path;
        path.append(CURRENCIES, ec2)
            .append('/', ec2)
            .appendInvariantChars(isoCode, u_strlen(isoCode), ec2);
        ures_getAllItemsWithFallback(rb, path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            result->isMissing = true;
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

        UErrorCode ec2 = errorCode;
        result = new VariantSymbol(isoCode, variant);
        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCY_VARIANT_TABLE);
        sink.variantSymbol = result;
        CharString path;
        path.append(variant, ec2)
            .append('/', ec2)
            .appendInvariantChars(isoCode, u_strlen(isoCode), ec2);
        ures_getAllItemsWithFallback(rb, path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            result->isMissing = true;
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
        result = new PluralsData(isoCode);

        UErrorCode ec2 = errorCode;
        CurrencySink sink(noSubstitute, CurrencySink::EntrypointTable::CURRENCY_PLURALS_TABLE);
        sink.pluralsData = result;
        CharString path;
        path.append(CURRENCYPLURALS, ec2)
            .append('/', ec2)
            .appendInvariantChars(isoCode, u_strlen(isoCode), ec2);
        ures_getAllItemsWithFallback(rb, path.data(), sink, ec2);
        if (ec2 == U_MISSING_RESOURCE_ERROR) {
            result->isMissing = true;
        } else {
            errorCode = ec2;
        }
        pluralsDataCache = result;
    }
    return result;
}
U_NAMESPACE_END
