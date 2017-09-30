// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 2009-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.Collections;
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

    @Override
    public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
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
        return new ICUCurrencyDisplayInfo(rb, withFallback);
    }

    @Override
    public boolean hasData() {
        return true;
    }

    static class ICUCurrencyDisplayInfo extends CurrencyDisplayInfo {
        private final boolean fallback;
        private final ICUResourceBundle rb;
        private volatile SoftReference<RawCurrencyData> rawDataCache;

        /**
         * The primary data structure is isoCodeToCurrencyStrings. In that structure,
         * the String arrays contain the following elements:
         *
         * [DISPLAY_NAME] => display name
         * [SYMBOL] => symbol
         * [NARROW_SYMBOL] => narrow symbol
         * [FORMAT_PATTERN] => currency format pattern
         * [DECIMAL_SEPARATOR] => currency decimal separator
         * [GROUPING_SEPARATOR] => currency grouping separator
         * [PLURALS_OFFSET+p] => plural name where p=standardPlural.ordinal()
         */
        private static class RawCurrencyData {
            static final int DISPLAY_NAME = 0;
            static final int SYMBOL = 1;
            static final int NARROW_SYMBOL = 2;
            static final int FORMAT_PATTERN = 3;
            static final int DECIMAL_SEPARATOR = 4;
            static final int GROUPING_SEPARATOR = 5;
            static final int PLURALS_OFFSET = 6;
            static final int CURRENCY_STRINGS_LENGTH = 6 + StandardPlural.COUNT;

            Map<String, String[]> isoCodeToCurrencyStrings = new HashMap<String, String[]>();

            // The following maps are redundant data with the above map, but the API for CurrencyDisplayNames
            // restricts us to using these data structures.
            Map<String, String> symbolToIsoCode = new HashMap<String, String>();
            Map<String, String> nameToIsoCode = new HashMap<String, String>();

            // Other currency-related data
            CurrencySpacingInfo spacingInfo = new CurrencySpacingInfo();
            Map<String, String> currencyUnitPatterns = new HashMap<String, String>();

            /**
             * Gets an entry out of isoCodeToCurrencyStrings or creates it if it does not exist yet.
             */
            String[] getOrCreateCurrencyStrings(String isoCode) {
                String[] currencyStrings = isoCodeToCurrencyStrings.get(isoCode);
                if (currencyStrings == null) {
                    currencyStrings = new String[CURRENCY_STRINGS_LENGTH];
                    isoCodeToCurrencyStrings.put(isoCode, currencyStrings);
                }
                return currencyStrings;
            }

            /**
             * Called after all data is loaded to convert the externally visible Maps to Unmodifiable.
             */
            void freezeMaps() {
                symbolToIsoCode = Collections.unmodifiableMap(symbolToIsoCode);
                nameToIsoCode = Collections.unmodifiableMap(nameToIsoCode);
                currencyUnitPatterns = Collections.unmodifiableMap(currencyUnitPatterns);
            }
        }

        public ICUCurrencyDisplayInfo(ICUResourceBundle rb, boolean fallback) {
            this.fallback = fallback;
            this.rb = rb;
            rawDataCache = new SoftReference<RawCurrencyData>(null);
        }

        @Override
        public ULocale getULocale() {
            return rb.getULocale();
        }

        @Override
        public String getName(String isoCode) {
            return getName(isoCode, RawCurrencyData.DISPLAY_NAME);
        }

        @Override
        public String getSymbol(String isoCode) {
            return getName(isoCode, RawCurrencyData.SYMBOL);
        }

        @Override
        public String getNarrowSymbol(String isoCode) {
            // TODO: Should this fall back to the regular symbol instead of the ISO code?
            return getName(isoCode, RawCurrencyData.NARROW_SYMBOL);
        }

        private String getName(String isoCode, int index) {
            String[] currencyStrings = getRawCurrencyData().isoCodeToCurrencyStrings.get(isoCode);
            String result = null;
            if (currencyStrings != null) {
                result = currencyStrings[index];
            }
            // If fallback is true, don't return null; return the ISO code
            if (result == null && fallback) {
                result = isoCode;
            }
            return result;
        }

        @Override
        public String getPluralName(String isoCode, String pluralKey ) {
            StandardPlural plural = StandardPlural.orNullFromString(pluralKey);
            String[] currencyStrings = getRawCurrencyData().isoCodeToCurrencyStrings.get(isoCode);
            String result = null;
            if (currencyStrings != null && plural != null) {
                result = currencyStrings[RawCurrencyData.PLURALS_OFFSET + plural.ordinal()];
            }
            // See http://unicode.org/reports/tr35/#Currencies, especially the fallback rule.
            if (result == null && currencyStrings != null && fallback) {
                // First fall back to the "other" plural variant
                // Note: If plural is already "other", this fallback is benign
                result = currencyStrings[RawCurrencyData.PLURALS_OFFSET + StandardPlural.OTHER.ordinal()];
            }
            if (result == null && currencyStrings != null && fallback) {
                // If that fails, fall back to the display name
                result = currencyStrings[0];
            }
            if (result == null && fallback) {
                // If all else fails, return the ISO code
                result = isoCode;
            }
            return result;
        }

        @Override
        public Map<String, String> symbolMap() {
            return getRawCurrencyData().symbolToIsoCode;
        }

        @Override
        public Map<String, String> nameMap() {
            return getRawCurrencyData().nameToIsoCode;
        }

        @Override
        public Map<String, String> getUnitPatterns() {
            // Default result is the empty map. Callers who require a pattern will have to
            // supply a default.
            return getRawCurrencyData().currencyUnitPatterns;
        }

        @Override
        public CurrencyFormatInfo getFormatInfo(String isoCode) {
            String[] currencyStrings = getRawCurrencyData().isoCodeToCurrencyStrings.get(isoCode);
            if (currencyStrings == null || currencyStrings[RawCurrencyData.FORMAT_PATTERN] == null) {
                return null;
            }
            String pattern = currencyStrings[RawCurrencyData.FORMAT_PATTERN];
            String decimalSeparator = currencyStrings[RawCurrencyData.DECIMAL_SEPARATOR];
            String groupingSeparator = currencyStrings[RawCurrencyData.GROUPING_SEPARATOR];
            return new CurrencyFormatInfo(pattern, decimalSeparator, groupingSeparator);
        }

        @Override
        public CurrencySpacingInfo getSpacingInfo() {
            CurrencySpacingInfo result = getRawCurrencyData().spacingInfo;
            if (result != null && (!result.hasBeforeCurrency || !result.hasAfterCurrency) && fallback) {
                result = CurrencySpacingInfo.DEFAULT;
            }
            if (result == null && fallback) {
                result = CurrencySpacingInfo.DEFAULT;
            }
            return result;
        }

        /**
         * If the soft cache is populated, returns the data stored there.
         * Otherwise, computes the data, stores it in the cache, and returns it.
         * Never returns null.
         */
        private RawCurrencyData getRawCurrencyData() {
            RawCurrencyData data = rawDataCache.get();
            if (data == null) {
                data = new RawCurrencyData();
                RawCurrencyDataSink sink = new RawCurrencyDataSink(data, !fallback);
                rb.getAllItemsWithFallback("", sink);
                data.freezeMaps();
                rawDataCache = new SoftReference<RawCurrencyData>(data);
            }
            return data;
        }

        private static final class RawCurrencyDataSink extends UResource.Sink {
            private final RawCurrencyData data;
            private final boolean noRoot;

            RawCurrencyDataSink(RawCurrencyData data, boolean noRoot) {
                this.data = data;
                this.noRoot = noRoot;
            }

            /**
             * The entrypoint method delegates to helper methods for each of the types of tables
             * found in the currency data.
             */
            @Override
            public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
                if (noRoot && noFallback) {
                    // Don't consume the root bundle
                    return;
                }

                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    if (key.contentEquals("Currencies")) {
                        consumeCurrencies(key, value, noFallback);
                    } else if (key.contentEquals("Currencies%narrow")) {
                        consumeCurrenciesNarrow(key, value, noFallback);
                    } else if (key.contentEquals("Currencies%variant")) {
                        consumeCurrenciesVariant(key, value, noFallback);
                    } else if (key.contentEquals("CurrencyPlurals")) {
                        consumeCurrencyPlurals(key, value, noFallback);
                    } else if (key.contentEquals("currencySpacing")) {
                        consumeCurrencySpacing(key, value, noFallback);
                    } else if (key.contentEquals("CurrencyUnitPatterns")) {
                        consumeCurrencyUnitPatterns(key, value, noFallback);
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
             *  ESP{
             *      "₧",                  => symbol
             *      "pesseta espanyola",  => display name
             *      {
             *          "¤ #,##0.00",     => currency-specific pattern
             *          ",",              => currency-specific grouping separator
             *          ".",              => currency-specific decimal separator
             *      }
             *  }
             *      ...
             *  }
             */
            private void consumeCurrencies(UResource.Key key, UResource.Value value, boolean noFallback) {

                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    String[] currencyStrings = data.getOrCreateCurrencyStrings(isoCode);
                    if (value.getType() != UResourceBundle.ARRAY) {
                        throw new ICUException("Unexpected data type in Currencies table for " + isoCode);
                    }
                    UResource.Array array = value.getArray();

                    // First element is the symbol.
                    array.getValue(0, value);
                    String symbol = value.getString();
                    if (currencyStrings[RawCurrencyData.SYMBOL] == null) {
                        currencyStrings[RawCurrencyData.SYMBOL] = symbol;
                    }

                    // Second element is the display name.
                    array.getValue(1, value);
                    String name = value.getString();
                    if (currencyStrings[RawCurrencyData.DISPLAY_NAME] == null) {
                        currencyStrings[RawCurrencyData.DISPLAY_NAME] = name;
                    }

                    // If present, the third element is the currency format info.
                    // TODO: Write unit test to ensure that this data is being used by number formatting.
                    if (array.getSize() > 2 && currencyStrings[RawCurrencyData.FORMAT_PATTERN] == null) {
                        array.getValue(2, value);
                        UResource.Array formatArray = value.getArray();
                        formatArray.getValue(0, value);
                        currencyStrings[RawCurrencyData.FORMAT_PATTERN] = value.getString();
                        assert currencyStrings[RawCurrencyData.DECIMAL_SEPARATOR] == null;
                        formatArray.getValue(1, value);
                        currencyStrings[RawCurrencyData.DECIMAL_SEPARATOR] = value.getString();
                        assert currencyStrings[RawCurrencyData.GROUPING_SEPARATOR] == null;
                        formatArray.getValue(2, value);
                        currencyStrings[RawCurrencyData.GROUPING_SEPARATOR] = value.getString();
                    }

                    // Add the name and symbols to the other two maps (used for parsing).
                    data.nameToIsoCode.put(name, isoCode);
                    data.symbolToIsoCode.put(isoCode, isoCode); // Add the ISO code itself as a symbol
                    data.symbolToIsoCode.put(symbol, isoCode);
                }
            }

            /*
             *  Currencies%narrow{
             *      AOA{"Kz"}
             *      ARS{"$"}
             *      ...
             *  }
             */
            private void consumeCurrenciesNarrow(UResource.Key key, UResource.Value value, boolean noFallback) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    String[] currencyStrings = data.getOrCreateCurrencyStrings(isoCode);
                    if (currencyStrings[RawCurrencyData.NARROW_SYMBOL] == null) {
                        currencyStrings[RawCurrencyData.NARROW_SYMBOL] = value.getString();
                    }

                    // Note: This data is used for formatting but not parsing.
                }
            }

            /*
             *  Currencies%variant{
             *      TRY{"TL"}
             *  }
             */
            private void consumeCurrenciesVariant(UResource.Key key, UResource.Value value, boolean noFallback) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();

                    // Note: This data is used for parsing but not formatting.
                    data.symbolToIsoCode.put(value.getString(), isoCode);
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
            private void consumeCurrencyPlurals(UResource.Key key, UResource.Value value, boolean noFallback) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    String[] currencyStrings = data.getOrCreateCurrencyStrings(isoCode);
                    UResource.Table pluralsTable = value.getTable();
                    for (int j=0; pluralsTable.getKeyAndValue(j, key, value); j++) {
                        StandardPlural plural = StandardPlural.orNullFromString(key.toString());
                        if (plural == null) {
                            throw new ICUException("Could not make StandardPlural from keyword " + key);
                        }
                        String valueString = value.getString();
                        if (currencyStrings[RawCurrencyData.PLURALS_OFFSET + plural.ordinal()] == null) {
                            currencyStrings[RawCurrencyData.PLURALS_OFFSET + plural.ordinal()] = valueString;
                        }

                        // Add the name to the name-to-currency map (used for parsing)
                        data.nameToIsoCode.put(valueString, isoCode);
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
            private void consumeCurrencySpacing(UResource.Key key, UResource.Value value, boolean noFallback) {
                UResource.Table spacingTypesTable = value.getTable();
                for (int i = 0; spacingTypesTable.getKeyAndValue(i, key, value); ++i) {
                    CurrencySpacingInfo.SpacingType type;
                    if (key.contentEquals("beforeCurrency")) {
                        type = CurrencySpacingInfo.SpacingType.BEFORE;
                        data.spacingInfo.hasBeforeCurrency = true;
                    } else if (key.contentEquals("afterCurrency")) {
                        type = CurrencySpacingInfo.SpacingType.AFTER;
                        data.spacingInfo.hasAfterCurrency = true;
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

                        data.spacingInfo.setSymbolIfNull(type, pattern, value.getString());
                    }
                }
            }

            /*
             *  CurrencyUnitPatterns{
             *      other{"{0} {1}"}
             *      ...
             *  }
             */
            private void consumeCurrencyUnitPatterns(UResource.Key key, UResource.Value value, boolean noFallback) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String pluralKeyword = key.toString();
                    if (data.currencyUnitPatterns.get(pluralKeyword) == null) {
                        data.currencyUnitPatterns.put(pluralKeyword, value.getString());
                    }
                }
            }
        }
    }
}
