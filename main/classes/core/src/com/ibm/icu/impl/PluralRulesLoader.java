/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Loader for plural rules data.
 */
public class PluralRulesLoader {
    private final Map<String, PluralRules> rulesIdToRules;
    private Map<String, String> localeIdToRulesId; // lazy init, use
                                                   // getLocaleIdToRulesIdMap to
                                                   // access
    private Map<String, ULocale> rulesIdToEquivalentULocale; // lazy init, use
                                                             // getRulesIdToEquivalentULocaleMap
                                                             // to access

    /**
     * Access through singleton.
     */
    private PluralRulesLoader() {
        rulesIdToRules = new HashMap<String, PluralRules>();
    }

    /**
     * Returns the locales for which we have plurals data. Utility for testing.
     */
    public ULocale[] getAvailableULocales() {
        Set<String> keys = getLocaleIdToRulesIdMap().keySet();
        ULocale[] locales = new ULocale[keys.size()];
        int n = 0;
        for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
            locales[n++] = ULocale.createCanonical(iter.next());
        }
        return locales;
    }

    /**
     * Returns the functionally equivalent locale.
     */
    public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
        if (isAvailable != null && isAvailable.length > 0) {
            String localeId = ULocale.canonicalize(locale.getBaseName());
            Map<String, String> idMap = getLocaleIdToRulesIdMap();
            isAvailable[0] = idMap.containsKey(localeId);
        }

        String rulesId = getRulesIdForLocale(locale);
        if (rulesId == null || rulesId.trim().length() == 0) {
            return ULocale.ROOT; // ultimate fallback
        }

        ULocale result = getRulesIdToEquivalentULocaleMap().get(
                rulesId);
        if (result == null) {
            return ULocale.ROOT; // ultimate fallback
        }

        return result;
    }

    /**
     * Returns the lazily-constructed map.
     */
    private Map<String, String> getLocaleIdToRulesIdMap() {
        checkBuildRulesIdMaps();
        return localeIdToRulesId;
    }

    /**
     * Returns the lazily-constructed map.
     */
    private Map<String, ULocale> getRulesIdToEquivalentULocaleMap() {
        checkBuildRulesIdMaps();
        return rulesIdToEquivalentULocale;
    }

    /**
     * Lazily constructs the localeIdToRulesId and rulesIdToEquivalentULocale
     * maps if necessary. These exactly reflect the contents of the locales
     * resource in plurals.res.
     */
    private void checkBuildRulesIdMaps() {
        boolean haveMap;
        synchronized (this) {
            haveMap = localeIdToRulesId != null;
        }
        if (!haveMap) {
            Map<String, String> tempLocaleIdToRulesId;
            Map<String, ULocale> tempRulesIdToEquivalentULocale;
            try {
                UResourceBundle pluralb = getPluralBundle();
                UResourceBundle localeb = pluralb.get("locales");
                
                // sort for convenience of getAvailableULocales
                tempLocaleIdToRulesId = new TreeMap<String, String>();
                // not visible
                tempRulesIdToEquivalentULocale = new HashMap<String, ULocale>();
                
                for (int i = 0; i < localeb.getSize(); ++i) {
                    UResourceBundle b = localeb.get(i);
                    String id = b.getKey();
                    String value = b.getString().intern();
                    tempLocaleIdToRulesId.put(id, value);

                    if (!tempRulesIdToEquivalentULocale.containsKey(value)) {
                        tempRulesIdToEquivalentULocale.put(value, new ULocale(id));
                    }
                }
            } catch (MissingResourceException e) {
                // dummy so we don't try again
                tempLocaleIdToRulesId = Collections.emptyMap();
                tempRulesIdToEquivalentULocale = Collections.emptyMap();
            }
            
            synchronized(this) {
                if (localeIdToRulesId == null) {
                    localeIdToRulesId = tempLocaleIdToRulesId;
                    rulesIdToEquivalentULocale = tempRulesIdToEquivalentULocale;
                }
            }
        }
    }

    /**
     * Gets the rulesId from the locale,with locale fallback. If there is no
     * rulesId, return null. The rulesId might be the empty string if the rule
     * is the default rule.
     */
    public String getRulesIdForLocale(ULocale locale) {
        Map<String, String> idMap = getLocaleIdToRulesIdMap();
        String localeId = ULocale.canonicalize(locale.getBaseName());
        String rulesId = null;
        while (null == (rulesId = idMap.get(localeId))) {
            int ix = localeId.lastIndexOf("_");
            if (ix == -1) {
                break;
            }
            localeId = localeId.substring(0, ix);
        }
        return rulesId;
    }

    /**
     * Gets the rule from the rulesId. If there is no rule for this rulesId,
     * return null.
     */
    public PluralRules getRulesForRulesId(String rulesId) {
        // synchronize on the map.  release the lock temporarily while we build the rules.
        PluralRules rules = null;
        boolean hasRules;  // Separate boolean because stored rules can be null.
        synchronized (rulesIdToRules) {
            hasRules = rulesIdToRules.containsKey(rulesId);
            if (hasRules) {
                rules = rulesIdToRules.get(rulesId);  // can be null
            }
        }
        if (!hasRules) {
            try {
                UResourceBundle pluralb = getPluralBundle();
                UResourceBundle rulesb = pluralb.get("rules");
                UResourceBundle setb = rulesb.get(rulesId);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < setb.getSize(); ++i) {
                    UResourceBundle b = setb.get(i);
                    if (i > 0) {
                        sb.append("; ");
                    }
                    sb.append(b.getKey());
                    sb.append(": ");
                    sb.append(b.getString());
                }
                rules = PluralRules.parseDescription(sb.toString());
            } catch (ParseException e) {
            } catch (MissingResourceException e) {
            }
            synchronized (rulesIdToRules) {
                if (rulesIdToRules.containsKey(rulesId)) {
                    rules = rulesIdToRules.get(rulesId);
                } else {
                    rulesIdToRules.put(rulesId, rules);  // can be null
                }
            }
        }
        return rules;
    }

    /**
     * Return the plurals resource. Note MissingResourceException is unchecked,
     * listed here for clarity. Callers should handle this exception.
     */
    public UResourceBundle getPluralBundle() throws MissingResourceException {
        return ICUResourceBundle.getBundleInstance(
                ICUResourceBundle.ICU_BASE_NAME, "plurals",
                ICUResourceBundle.ICU_DATA_CLASS_LOADER, true);
    }

    /**
     * Returns the plural rules for the the locale. If we don't have data,
     * com.ibm.icu.text.PluralRules.DEFAULT is returned.
     */
    public PluralRules forLocale(ULocale locale) {
        String rulesId = getRulesIdForLocale(locale);
        if (rulesId == null || rulesId.trim().length() == 0) {
            return PluralRules.DEFAULT;
        }
        PluralRules rules = getRulesForRulesId(rulesId);
        if (rules == null) {
            rules = PluralRules.DEFAULT;
        }
        return rules;
    }

    /**
     * The only instance of the loader.
     */
    public static final PluralRulesLoader loader = new PluralRulesLoader();
}
