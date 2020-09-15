// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2009-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfo;
import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfoProvider;
import com.ibm.icu.impl.CurrencyData.CurrencyFormatInfo;
import com.ibm.icu.impl.CurrencyData.CurrencySpacingInfo;
import com.ibm.icu.impl.ICUResourceBundle.OpenType;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class ICUCurrencyDisplayInfoProvider implements CurrencyDisplayInfoProvider {
    public ICUCurrencyDisplayInfoProvider() {
    }

    /**
     * Single-item cache for ICUCurrencyDisplayInfo keyed by locale.
     */
    private volatile ICUCurrencyDisplayInfo currencyDisplayInfoCache = null;

    @Override
    public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
        // Make sure the locale is non-null (this can happen during deserialization):
        if (locale == null) { locale = ULocale.ROOT; }
        ICUCurrencyDisplayInfo instance = currencyDisplayInfoCache;
        if (instance == null || !instance.locale.equals(locale) || instance.fallback != withFallback) {
            ICUResourceBundle rb;
            if (withFallback) {
                rb = ICUResourceBundle.getBundleInstance(
                        ICUData.ICU_CURR_BASE_NAME, locale, OpenType.LOCALE_DEFAULT_ROOT);
            } else {
                try {
                    rb = ICUResourceBundle.getBundleInstance(
                            ICUData.ICU_CURR_BASE_NAME, locale, OpenType.LOCALE_ONLY);
                } catch (MissingResourceException e) {
                    return null;
                }
            }
            instance = new ICUCurrencyDisplayInfo(locale, rb, withFallback);
            currencyDisplayInfoCache = instance;
        }
        return instance;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    /**
     * This class performs data loading for currencies and keeps data in lightweight cache.
     */
    static class ICUCurrencyDisplayInfo extends CurrencyDisplayInfo {
        final ULocale locale;
        final boolean fallback;
        private final ICUResourceBundle rb;

        /**
         * Single-item cache for getName(), getSymbol(), and getFormatInfo().
         * Holds data for only one currency. If another currency is requested, the old cache item is overwritten.
         */
        private volatile FormattingData formattingDataCache = null;

        /**
         * Single-item cache for variant symbols.
         * Holds data for only one currency. If another currency is requested, the old cache item is overwritten.
         */
        private volatile VariantSymbol variantSymbolCache = null;

        /**
         * Single-item cache for getPluralName().
         *
         * <p>
         * array[0] is the ISO code.<br>
         * array[1+p] is the plural name where p=standardPlural.ordinal().
         *
         * <p>
         * Holds data for only one currency. If another currency is requested, the old cache item is overwritten.
         */
        private volatile String[] pluralsDataCache = null;

        /**
         * Cache for symbolMap() and nameMap().
         */
        private volatile SoftReference<ParsingData> parsingDataCache = new SoftReference<>(null);

        /**
         * Cache for getUnitPatterns().
         */
        private volatile Map<String, String> unitPatternsCache = null;

        /**
         * Cache for getSpacingInfo().
         */
        private volatile CurrencySpacingInfo spacingInfoCache = null;

        static class FormattingData {
            final String isoCode;
            String displayName = null;
            String symbol = null;
            CurrencyFormatInfo formatInfo = null;

            FormattingData(String isoCode) { this.isoCode = isoCode; }
        }

        static class VariantSymbol {
            final String isoCode;
            final String variant;
            String symbol = null;

            VariantSymbol(String isoCode, String variant) {
                this.isoCode = isoCode;
                this.variant = variant;
            }
        }

        static class ParsingData {
            Map<String, String> symbolToIsoCode = new HashMap<>();
            Map<String, String> nameToIsoCode = new HashMap<>();
        }

        ////////////////////////
        /// START PUBLIC API ///
        ////////////////////////

        public ICUCurrencyDisplayInfo(ULocale locale, ICUResourceBundle rb, boolean fallback) {
            this.locale = locale;
            this.fallback = fallback;
            this.rb = rb;
        }

        @Override
        public ULocale getULocale() {
            return rb.getULocale();
        }

        @Override
        public String getName(String isoCode) {
            FormattingData formattingData = fetchFormattingData(isoCode);

            // Fall back to ISO Code
            if (formattingData.displayName == null && fallback) {
                return isoCode;
            }
            return formattingData.displayName;
        }

        @Override
        public String getSymbol(String isoCode) {
            FormattingData formattingData = fetchFormattingData(isoCode);

            // Fall back to ISO Code
            if (formattingData.symbol == null && fallback) {
                return isoCode;
            }
            return formattingData.symbol;
        }

        @Override
        public String getNarrowSymbol(String isoCode) {
            VariantSymbol variantSymbol = fetchVariantSymbol(isoCode, "narrow");

            // Fall back to regular symbol
            if (variantSymbol.symbol == null && fallback) {
                return getSymbol(isoCode);
            }
            return variantSymbol.symbol;
        }

        @Override
        public String getFormalSymbol(String isoCode) {
            VariantSymbol variantSymbol = fetchVariantSymbol(isoCode, "formal");

            // Fall back to regular symbol
            if (variantSymbol.symbol == null && fallback) {
                return getSymbol(isoCode);
            }
            return variantSymbol.symbol;
        }

        @Override
        public String getVariantSymbol(String isoCode) {
            VariantSymbol variantSymbol = fetchVariantSymbol(isoCode, "variant");

            // Fall back to regular symbol
            if (variantSymbol.symbol == null && fallback) {
                return getSymbol(isoCode);
            }
            return variantSymbol.symbol;
        }

        @Override
        public String getPluralName(String isoCode, String pluralKey ) {
            StandardPlural plural = StandardPlural.orNullFromString(pluralKey);
            String[] pluralsData = fetchPluralsData(isoCode);

            // See http://unicode.org/reports/tr35/#Currencies, especially the fallback rule.
            String result = null;
            if (plural != null) {
                result = pluralsData[1 + plural.ordinal()];
            }
            if (result == null && fallback) {
                // First fall back to the "other" plural variant
                // Note: If plural is already "other", this fallback is benign
                result = pluralsData[1 + StandardPlural.OTHER.ordinal()];
            }
            if (result == null && fallback) {
                // If that fails, fall back to the display name
                FormattingData formattingData = fetchFormattingData(isoCode);
                result = formattingData.displayName;
            }
            if (result == null && fallback) {
                // If all else fails, return the ISO code
                result = isoCode;
            }
            return result;
        }

        @Override
        public Map<String, String> symbolMap() {
            ParsingData parsingData = fetchParsingData();
            return parsingData.symbolToIsoCode;
        }

        @Override
        public Map<String, String> nameMap() {
            ParsingData parsingData = fetchParsingData();
            return parsingData.nameToIsoCode;
        }

        @Override
        public Map<String, String> getUnitPatterns() {
            // Default result is the empty map. Callers who require a pattern will have to
            // supply a default.
            Map<String,String> unitPatterns = fetchUnitPatterns();
            return unitPatterns;
        }

        @Override
        public CurrencyFormatInfo getFormatInfo(String isoCode) {
            FormattingData formattingData = fetchFormattingData(isoCode);
            return formattingData.formatInfo;
        }

        @Override
        public CurrencySpacingInfo getSpacingInfo() {
            CurrencySpacingInfo spacingInfo = fetchSpacingInfo();

            // Fall back to DEFAULT
            if ((!spacingInfo.hasBeforeCurrency || !spacingInfo.hasAfterCurrency) && fallback) {
                return CurrencySpacingInfo.DEFAULT;
            }
            return spacingInfo;
        }

        /////////////////////////////////////////////
        /// END PUBLIC API -- START DATA FRONTEND ///
        /////////////////////////////////////////////

        FormattingData fetchFormattingData(String isoCode) {
            FormattingData result = formattingDataCache;
            if (result == null || !result.isoCode.equals(isoCode)) {
                result = new FormattingData(isoCode);
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.CURRENCIES);
                sink.formattingData = result;
                rb.getAllItemsWithFallbackNoFail("Currencies/" + isoCode, sink);
                formattingDataCache = result;
            }
            return result;
        }

        VariantSymbol fetchVariantSymbol(String isoCode, String variant) {
            VariantSymbol result = variantSymbolCache;
            if (result == null || !result.isoCode.equals(isoCode) || !result.variant.equals(variant)) {
                result = new VariantSymbol(isoCode, variant);
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.CURRENCY_VARIANT);
                sink.variantSymbol = result;
                rb.getAllItemsWithFallbackNoFail("Currencies%" + variant + "/" + isoCode, sink);
                variantSymbolCache = result;
            }
            return result;
        }

        String[] fetchPluralsData(String isoCode) {
            String[] result = pluralsDataCache;
            if (result == null || !result[0].equals(isoCode)) {
                result = new String[1 + StandardPlural.COUNT];
                result[0] = isoCode;
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.CURRENCY_PLURALS);
                sink.pluralsData = result;
                rb.getAllItemsWithFallbackNoFail("CurrencyPlurals/" + isoCode, sink);
                pluralsDataCache = result;
            }
            return result;
        }

        ParsingData fetchParsingData() {
            ParsingData result = parsingDataCache.get();
            if (result == null) {
                result = new ParsingData();
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.TOP);
                sink.parsingData = result;
                rb.getAllItemsWithFallback("", sink);
                parsingDataCache = new SoftReference<>(result);
            }
            return result;
        }

        Map<String, String> fetchUnitPatterns() {
            Map<String, String> result = unitPatternsCache;
            if (result == null) {
                result = new HashMap<>();
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.CURRENCY_UNIT_PATTERNS);
                sink.unitPatterns = result;
                rb.getAllItemsWithFallback("CurrencyUnitPatterns", sink);
                unitPatternsCache = result;
            }
            return result;
        }

        CurrencySpacingInfo fetchSpacingInfo() {
            CurrencySpacingInfo result = spacingInfoCache;
            if (result == null) {
                result = new CurrencySpacingInfo();
                CurrencySink sink = new CurrencySink(!fallback, CurrencySink.EntrypointTable.CURRENCY_SPACING);
                sink.spacingInfo = result;
                rb.getAllItemsWithFallback("currencySpacing", sink);
                spacingInfoCache = result;
            }
            return result;
        }

        ////////////////////////////////////////////
        /// END DATA FRONTEND -- START DATA SINK ///
        ////////////////////////////////////////////

        private static final class CurrencySink extends UResource.Sink {
            final boolean noRoot;
            final EntrypointTable entrypointTable;

            // The fields to be populated on this run of the data sink will be non-null.
            FormattingData formattingData = null;
            String[] pluralsData = null;
            ParsingData parsingData = null;
            Map<String, String> unitPatterns = null;
            CurrencySpacingInfo spacingInfo = null;
            VariantSymbol variantSymbol = null;

            enum EntrypointTable {
                // For Parsing:
                TOP,

                // For Formatting:
                CURRENCIES,
                CURRENCY_PLURALS,
                CURRENCY_VARIANT,
                CURRENCY_SPACING,
                CURRENCY_UNIT_PATTERNS
            }

            CurrencySink(boolean noRoot, EntrypointTable entrypointTable) {
                this.noRoot = noRoot;
                this.entrypointTable = entrypointTable;
            }

            /**
             * The entrypoint method delegates to helper methods for each of the types of tables
             * found in the currency data.
             */
            @Override
            public void put(UResource.Key key, UResource.Value value, boolean isRoot) {
                if (noRoot && isRoot) {
                    // Don't consume the root bundle
                    return;
                }

                switch (entrypointTable) {
                case TOP:
                    consumeTopTable(key, value);
                    break;
                case CURRENCIES:
                    consumeCurrenciesEntry(key, value);
                    break;
                case CURRENCY_PLURALS:
                    consumeCurrencyPluralsEntry(key, value);
                    break;
                case CURRENCY_VARIANT:
                    consumeCurrenciesVariantEntry(key, value);
                    break;
                case CURRENCY_SPACING:
                    consumeCurrencySpacingTable(key, value);
                    break;
                case CURRENCY_UNIT_PATTERNS:
                    consumeCurrencyUnitPatternsTable(key, value);
                    break;
                }
            }

            private void consumeTopTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    if (key.contentEquals("Currencies")) {
                        consumeCurrenciesTable(key, value);
                    } else if (key.contentEquals("Currencies%variant")) {
                        consumeCurrenciesVariantTable(key, value);
                    } else if (key.contentEquals("CurrencyPlurals")) {
                        consumeCurrencyPluralsTable(key, value);
                    }
                }
            }

            /*
             *  Currencies{
             *      ...
             *      USD{
             *          "US$",        => symbol
             *          "US Dollar",  => display name
             *      }
             *      ...
             *      ESP{
             *          "₧",                  => symbol
             *          "pesseta espanyola",  => display name
             *          {
             *              "¤ #,##0.00",     => currency-specific pattern
             *              ",",              => currency-specific grouping separator
             *              ".",              => currency-specific decimal separator
             *          }
             *      }
             *      ...
             *  }
             */
            void consumeCurrenciesTable(UResource.Key key, UResource.Value value) {
                // The full Currencies table is consumed for parsing only.
                assert parsingData != null;
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    if (value.getType() != UResourceBundle.ARRAY) {
                        throw new ICUException("Unexpected data type in Currencies table for " + isoCode);
                    }
                    UResource.Array array = value.getArray();

                    parsingData.symbolToIsoCode.put(isoCode, isoCode); // Add the ISO code itself as a symbol
                    array.getValue(0, value);
                    parsingData.symbolToIsoCode.put(value.getString(), isoCode);
                    array.getValue(1, value);
                    parsingData.nameToIsoCode.put(value.getString(), isoCode);
                }
            }

            void consumeCurrenciesEntry(UResource.Key key, UResource.Value value) {
                assert formattingData != null;
                String isoCode = key.toString();
                if (value.getType() != UResourceBundle.ARRAY) {
                    throw new ICUException("Unexpected data type in Currencies table for " + isoCode);
                }
                UResource.Array array = value.getArray();

                if (formattingData.symbol == null) {
                    array.getValue(0, value);
                    formattingData.symbol = value.getString();
                }
                if (formattingData.displayName == null) {
                    array.getValue(1, value);
                    formattingData.displayName = value.getString();
                }

                // If present, the third element is the currency format info.
                // TODO: Write unit test to ensure that this data is being used by number formatting.
                if (array.getSize() > 2 && formattingData.formatInfo == null) {
                    array.getValue(2, value);
                    UResource.Array formatArray = value.getArray();
                    formatArray.getValue(0, value);
                    String formatPattern = value.getString();
                    formatArray.getValue(1, value);
                    String decimalSeparator = value.getString();
                    formatArray.getValue(2, value);
                    String groupingSeparator = value.getString();
                    formattingData.formatInfo = new CurrencyFormatInfo(
                            isoCode, formatPattern, decimalSeparator, groupingSeparator);
                }
            }

            /*
             *  Currencies%narrow{
             *      AOA{"Kz"}
             *      ARS{"$"}
             *      ...
             *  }
             */
            void consumeCurrenciesVariantEntry(UResource.Key key, UResource.Value value) {
                assert variantSymbol != null;
                // No extra structure to traverse.
                if (variantSymbol.symbol == null) {
                    variantSymbol.symbol = value.getString();
                }
            }

            /*
             *  Currencies%variant{
             *      TRY{"TL"}
             *  }
             */
            void consumeCurrenciesVariantTable(UResource.Key key, UResource.Value value) {
                // Note: This data is used for parsing but not formatting.
                assert parsingData != null;
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    parsingData.symbolToIsoCode.put(value.getString(), isoCode);
                }
            }

            /*
             *  CurrencyPlurals{
             *      BYB{
             *          one{"Belarusian new rouble (1994–1999)"}
             *          other{"Belarusian new roubles (1994–1999)"}
             *      }
             *      ...
             *  }
             */
            void consumeCurrencyPluralsTable(UResource.Key key, UResource.Value value) {
                // The full CurrencyPlurals table is consumed for parsing only.
                assert parsingData != null;
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    UResource.Table pluralsTable = value.getTable();
                    for (int j=0; pluralsTable.getKeyAndValue(j, key, value); j++) {
                        StandardPlural plural = StandardPlural.orNullFromString(key.toString());
                        if (plural == null) {
                            throw new ICUException("Could not make StandardPlural from keyword " + key);
                        }

                        parsingData.nameToIsoCode.put(value.getString(), isoCode);
                    }
                }
            }

            void consumeCurrencyPluralsEntry(UResource.Key key, UResource.Value value) {
                assert pluralsData != null;
                UResource.Table pluralsTable = value.getTable();
                for (int j=0; pluralsTable.getKeyAndValue(j, key, value); j++) {
                    StandardPlural plural = StandardPlural.orNullFromString(key.toString());
                    if (plural == null) {
                        throw new ICUException("Could not make StandardPlural from keyword " + key);
                    }

                    if (pluralsData[1 + plural.ordinal()] == null) {
                        pluralsData[1 + plural.ordinal()] = value.getString();
                    }
                }
            }

            /*
             *  currencySpacing{
             *      afterCurrency{
             *          currencyMatch{"[:^S:]"}
             *          insertBetween{" "}
             *          surroundingMatch{"[:digit:]"}
             *      }
             *      beforeCurrency{
             *          currencyMatch{"[:^S:]"}
             *          insertBetween{" "}
             *          surroundingMatch{"[:digit:]"}
             *      }
             *  }
             */
            void consumeCurrencySpacingTable(UResource.Key key, UResource.Value value) {
                assert spacingInfo != null;
                UResource.Table spacingTypesTable = value.getTable();
                for (int i = 0; spacingTypesTable.getKeyAndValue(i, key, value); ++i) {
                    CurrencySpacingInfo.SpacingType type;
                    if (key.contentEquals("beforeCurrency")) {
                        type = CurrencySpacingInfo.SpacingType.BEFORE;
                        spacingInfo.hasBeforeCurrency = true;
                    } else if (key.contentEquals("afterCurrency")) {
                        type = CurrencySpacingInfo.SpacingType.AFTER;
                        spacingInfo.hasAfterCurrency = true;
                    } else {
                        continue;
                    }

                    UResource.Table patternsTable = value.getTable();
                    for (int j = 0; patternsTable.getKeyAndValue(j, key, value); ++j) {
                        CurrencySpacingInfo.SpacingPattern pattern;
                        if (key.contentEquals("currencyMatch")) {
                            pattern = CurrencySpacingInfo.SpacingPattern.CURRENCY_MATCH;
                        } else if (key.contentEquals("surroundingMatch")) {
                            pattern = CurrencySpacingInfo.SpacingPattern.SURROUNDING_MATCH;
                        } else if (key.contentEquals("insertBetween")) {
                            pattern = CurrencySpacingInfo.SpacingPattern.INSERT_BETWEEN;
                        } else {
                            continue;
                        }

                        spacingInfo.setSymbolIfNull(type, pattern, value.getString());
                    }
                }
            }

            /*
             *  CurrencyUnitPatterns{
             *      other{"{0} {1}"}
             *      ...
             *  }
             */
            void consumeCurrencyUnitPatternsTable(UResource.Key key, UResource.Value value) {
                assert unitPatterns != null;
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String pluralKeyword = key.toString();
                    if (unitPatterns.get(pluralKeyword) == null) {
                        unitPatterns.put(pluralKeyword, value.getString());
                    }
                }
            }
        }
    }
}
