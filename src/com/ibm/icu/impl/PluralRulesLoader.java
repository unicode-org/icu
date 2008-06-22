/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

/**
 * Loader for plural rules data.
 */
public class PluralRulesLoader {
  private final Map rulesIdToRules;
  private Map localeIdToRulesId; // lazy init, use getLocaleIdToRulesIdMap to access

  /**
   * Access through singleton.
   */
  private PluralRulesLoader() {
    rulesIdToRules = new HashMap();
  }

  /**
   * Returns the locales for which we have plurals data.
   * Utility for testing.
   */
  public ULocale[] getAvailableULocales() {
    Set keys = getLocaleIdToRulesIdMap().keySet();
    ULocale[] locales = new ULocale[keys.size()];
    int n = 0;
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      locales[n++] = ULocale.createCanonical((String) iter.next());
    }
    return locales;
  }

  /**
   * Lazily construct the map from localeIds to rulesIds.  This
   * map exactly reflects the contents of the locales resource
   * in plurals.res.
   */
  private Map getLocaleIdToRulesIdMap() {
    if (localeIdToRulesId == null) {
      try {
        UResourceBundle pluralb = getPluralBundle();
        UResourceBundle localeb = pluralb.get("locales");
        localeIdToRulesId = new TreeMap();  // sort for convenience of getAvailableULocales
        for (int i = 0; i < localeb.getSize(); ++i) {
          UResourceBundle b = localeb.get(i);
          String id = b.getKey();
          String value = b.getString().intern();
          localeIdToRulesId.put(id, value);
        }
      }
      catch (MissingResourceException e) {
        localeIdToRulesId = new HashMap(); // dummy so we don't try again
      }
    }
    return localeIdToRulesId;
  }

  /**
   * Gets the rulesId from the locale,with locale fallback.  If there is no
   * rulesId, return null.  The rulesId might be the empty string if the
   * rule is the default rule.
   */
  public String getRulesIdForLocale(ULocale locale) {
    Map idMap = getLocaleIdToRulesIdMap();
    String localeId = ULocale.canonicalize(locale.getBaseName());
    String rulesId = null;
    while (null == (rulesId = (String) idMap.get(localeId))) {
      int ix = localeId.lastIndexOf("_");
      if (ix == -1) {
        break;
      }
      localeId = localeId.substring(0, ix);
    }
    return rulesId;
  }

  /**
   * Gets the rule from the rulesId.  If there is no rule for this rulesId,
   * return null.
   */
  public PluralRules getRulesForRulesId(String rulesId) {
    PluralRules rules = (PluralRules) rulesIdToRules.get(rulesId);
    if (rules == null) {
      try {
        UResourceBundle pluralb = getPluralBundle();
        UResourceBundle rulesb = pluralb.get("rules");
        UResourceBundle setb = rulesb.get(rulesId);

        StringBuffer sb = new StringBuffer();
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
      rulesIdToRules.put(rulesId, rules); // put even if null
    }
    return rules;
  }

  /**
   * Return the plurals resource.
   * Note MissingResourceException is unchecked, listed here for clarity.
   * Callers should handle this exception.
   */
  public UResourceBundle getPluralBundle() throws MissingResourceException {
      return ICUResourceBundle.getBundleInstance(
          ICUResourceBundle.ICU_BASE_NAME,
          "plurals",
          ICUResourceBundle.ICU_DATA_CLASS_LOADER,
          true);
  }

  /**
   * Returns the plural rules for the the locale.
   * If we don't have data,
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
