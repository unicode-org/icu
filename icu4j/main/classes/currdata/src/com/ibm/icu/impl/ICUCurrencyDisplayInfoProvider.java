/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfo;
import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfoProvider;
import com.ibm.icu.impl.CurrencyData.CurrencyFormatInfo;
import com.ibm.icu.impl.CurrencyData.CurrencySpacingInfo;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class ICUCurrencyDisplayInfoProvider implements CurrencyDisplayInfoProvider {
    public ICUCurrencyDisplayInfoProvider() {
    }

    public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(
                ICUResourceBundle.ICU_CURR_BASE_NAME, locale);
        if (!withFallback) {
            int status = rb.getLoadingStatus();
            if (status == ICUResourceBundle.FROM_DEFAULT || status == ICUResourceBundle.FROM_ROOT) {
                return CurrencyData.DefaultInfo.getWithFallback(false);
            }
        }
        return new ICUCurrencyDisplayInfo(rb, withFallback);
    }
    
    public boolean hasData() {
        return true;
    }

    static class ICUCurrencyDisplayInfo extends CurrencyDisplayInfo {
        private final boolean fallback;
        private final ICUResourceBundle rb;
        private final ICUResourceBundle currencies;
        private final ICUResourceBundle plurals;
        private SoftReference<Map<String, String>> _symbolMapRef;
        private SoftReference<Map<String, String>> _nameMapRef;
        
        public ICUCurrencyDisplayInfo(ICUResourceBundle rb, boolean fallback) {
            this.fallback = fallback;
            this.rb = rb;
            this.currencies = rb.findTopLevel("Currencies");
            this.plurals = rb.findTopLevel("CurrencyPlurals");
       }

        @Override
        public ULocale getLocale() {
            return rb.getULocale();
        }

        @Override
        public String getName(String isoCode) {
            return getName(isoCode, false);
        }

        @Override
        public String getSymbol(String isoCode) {
            return getName(isoCode, true);
        }
        
        private String getName(String isoCode, boolean symbolName) {
            if (currencies != null) {
                ICUResourceBundle result = currencies.findWithFallback(isoCode);
                if (result != null) {
                    if (!fallback) {
                        int status = result.getLoadingStatus();
                        if (status == ICUResourceBundle.FROM_DEFAULT || 
                                status == ICUResourceBundle.FROM_ROOT) {
                            return null;
                        }
                    }
                    return result.getString(symbolName ? 0 : 1);
                }
            }

            return fallback ? isoCode : null;
        }

        @Override
        public String getPluralName(String isoCode, String pluralKey ) {
            // See http://unicode.org/reports/tr35/#Currencies, especially the fallback rule.
            if (plurals != null) {
                ICUResourceBundle pluralsBundle = plurals.findWithFallback(isoCode);
                if (pluralsBundle != null) {
                    ICUResourceBundle pluralBundle = pluralsBundle.findWithFallback(pluralKey);
                    if (pluralBundle == null) {
                        if (!fallback) {
                            return null;
                        }
                        pluralBundle = pluralsBundle.findWithFallback("other");
                        if (pluralBundle == null) {
                            return getName(isoCode);
                        }
                    }
                    return pluralBundle.getString();
                }
            }
            
            return fallback ? getName(isoCode) : null;
        }
        
        @Override
        public Map<String, String> symbolMap() {
            Map<String, String> map = _symbolMapRef == null ? null : _symbolMapRef.get();
            if (map == null) {
                map = _createSymbolMap();
                // atomic and idempotent
                _symbolMapRef = new SoftReference<Map<String, String>>(map);
            }
            return map;
        }
        
        @Override
        public Map<String, String> nameMap() {
            Map<String, String> map = _nameMapRef == null ? null : _nameMapRef.get();
            if (map == null) {
                map = _createNameMap();
                // atomic and idempotent
                _nameMapRef = new SoftReference<Map<String, String>>(map);
            }
            return map;
        }
        
       @Override
        public Map<String, String> getUnitPatterns() {
            Map<String, String> result = new HashMap<String, String>();
            
            ULocale locale = rb.getULocale();
            for (;locale != null; locale = locale.getFallback()) {
                ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(
                        ICUResourceBundle.ICU_CURR_BASE_NAME, locale);
                if (r == null) {
                    continue;
                }
                ICUResourceBundle cr = r.findWithFallback("CurrencyUnitPatterns");
                if (cr == null) {
                    continue;
                }
                for (int index = 0, size = cr.getSize(); index < size; ++index) {
                    ICUResourceBundle b = (ICUResourceBundle) cr.get(index);
                    String key = b.getKey();
                    if (result.containsKey(key)) {
                        continue;
                    }
                    result.put(key, b.getString());
                }
            }
            
            // Default result is the empty map. Callers who require a pattern will have to
            // supply a default.
            return Collections.unmodifiableMap(result);
        }
        
        @Override
        public CurrencyFormatInfo getFormatInfo(String isoCode) {
            ICUResourceBundle crb = currencies.findWithFallback(isoCode);
            if (crb != null && crb.getSize() > 2) {
                crb = crb.at(2);
                if (crb != null) {
                  String pattern = crb.getString(0);
                  char separator = crb.getString(1).charAt(0);
                  char groupingSeparator = crb.getString(2).charAt(0);
                  return new CurrencyFormatInfo(pattern, separator, groupingSeparator);
                }
            }
            return null;
        }

        @Override
        public CurrencySpacingInfo getSpacingInfo() {
            ICUResourceBundle srb = rb.findWithFallback("currencySpacing");
            if (srb != null) {
                ICUResourceBundle brb = srb.findWithFallback("beforeCurrency");
                ICUResourceBundle arb = srb.findWithFallback("afterCurrency");
                if (brb != null && brb != null) {
                    String beforeCurrencyMatch = brb.findWithFallback("currencyMatch").getString();
                    String beforeContextMatch = brb.findWithFallback("surroundingMatch").getString();
                    String beforeInsert = brb.findWithFallback("insertBetween").getString();
                    String afterCurrencyMatch = arb.findWithFallback("currencyMatch").getString();
                    String afterContextMatch = arb.findWithFallback("surroundingMatch").getString();
                    String afterInsert = arb.findWithFallback("insertBetween").getString();
                    
                    return new CurrencySpacingInfo(
                            beforeCurrencyMatch, beforeContextMatch, beforeInsert,
                            afterCurrencyMatch, afterContextMatch, afterInsert);
                }
            }
            return fallback ? CurrencySpacingInfo.DEFAULT : null;
        }
    
        private Map<String, String> _createSymbolMap() {
            Map<String, String> result = new HashMap<String, String>();
            
            for (ULocale locale = rb.getULocale(); locale != null; locale = locale.getFallback()) {
                ICUResourceBundle bundle = (ICUResourceBundle)
                    UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_CURR_BASE_NAME, locale);
                ICUResourceBundle curr = bundle.findTopLevel("Currencies");
                if (curr == null) {
                    continue;
                }
                for (int i = 0; i < curr.getSize(); ++i) {
                    ICUResourceBundle item = curr.at(i);
                    String isoCode = item.getKey();
                    if (!result.containsKey(isoCode)) {
                        // put the code itself
                        result.put(isoCode, isoCode);
                        // 0 == symbol element
                        String symbol = item.getString(0);
                        result.put(symbol, isoCode);
                    }
                }
            }
            
            return Collections.unmodifiableMap(result);
        }
        
        private Map<String, String> _createNameMap() {
            // ignore case variants
            Map<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            
            Set<String> visited = new HashSet<String>();
            Map<String, Set<String>> visitedPlurals = new HashMap<String, Set<String>>();
            for (ULocale locale = rb.getULocale(); locale != null; locale = locale.getFallback()) {
                ICUResourceBundle bundle = (ICUResourceBundle)
                    UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_CURR_BASE_NAME, locale);
                ICUResourceBundle curr = bundle.findTopLevel("Currencies");
                if (curr != null) {
                    for (int i = 0; i < curr.getSize(); ++i) {
                        ICUResourceBundle item = curr.at(i);
                        String isoCode = item.getKey();
                        if (!visited.contains(isoCode)) {
                            visited.add(isoCode);
                            // 1 == name element
                            String name = item.getString(1);
                            result.put(name, isoCode);
                        }
                    }
                }
                
                ICUResourceBundle plurals = bundle.findTopLevel("CurrencyPlurals");
                if (plurals != null) {
                    for (int i = 0; i < plurals.getSize(); ++i) {
                        ICUResourceBundle item = plurals.at(i);
                        String isoCode = item.getKey();
                        Set<String> pluralSet = visitedPlurals.get(isoCode);
                        if (pluralSet == null) {
                            pluralSet = new HashSet<String>();
                            visitedPlurals.put(isoCode, pluralSet);
                        }
                        for (int j = 0; j < item.getSize(); ++j) {
                            ICUResourceBundle plural = item.at(j);
                            String pluralType = plural.getKey();
                            if (!pluralSet.contains(pluralType)) {
                                String pluralName = plural.getString();
                                result.put(pluralName, isoCode);
                                pluralSet.add(pluralType);
                            }
                        }
                    }
                }
            }
            
            return Collections.unmodifiableMap(result);
        }
    }
}
