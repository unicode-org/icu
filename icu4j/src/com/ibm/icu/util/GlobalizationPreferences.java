//##header
/*
 *******************************************************************************
 * Copyright (C) 2004-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

//#ifndef FOUNDATION
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//#endif

import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * This convenience class provides a mechanism for bundling together different
 * globalization preferences. It includes:
 * <ul>
 * <li>A list of locales/languages in preference order</li>
 * <li>A territory</li>
 * <li>A currency</li>
 * <li>A timezone</li>
 * <li>A calendar</li>
 * <li>A collator (for language-sensitive sorting, searching, and matching).</li>
 * <li>Explicit overrides for date/time formats, etc.</li>
 * </ul>
 * The class will heuristically compute implicit, heuristic values for the above
 * based on available data if explicit values are not supplied. These implicit
 * values can be presented to users for confirmation, or replacement if the
 * values are incorrect.
 * <p>
 * To reset any explicit field so that it will get heuristic values, pass in
 * null. For example, myPreferences.setLocale(null);
 * <p>
 * All of the heuristics can be customized by subclasses, by overriding
 * getTerritory(), guessCollator(), etc.
 * <p>
 * The class also supplies display names for languages, scripts, territories,
 * currencies, timezones, etc. These are computed according to the
 * locale/language preference list. Thus, if the preference is Breton; French;
 * English, then the display name for a language will be returned in Breton if
 * available, otherwise in French if available, otherwise in English.
 * <p>
 * The codes used to reference territory, currency, etc. are as defined elsewhere in ICU,
 * and are taken from CLDR (which reflects RFC 3066bis usage, ISO 4217, and the 
 * TZ Timezone database identifiers).
 * <p>
 * <b>This is at a prototype stage, and has not incorporated all the design
 * changes that we would like yet; further feedback is welcome.</b></p>
 * <p>
 * TODO:<ul>
 * <li>Add Holidays</li>
 * <li>Add convenience to get/take Locale as well as ULocale.</li>
 * <li>Add Lenient datetime formatting when that is available.</li>
 * <li>Should this be serializable?</li>
 * <li>Other utilities?</li>
 * </ul>
 * Note:
 * <ul>
 * <li>to get the display name for the first day of the week, use the calendar +
 * display names.</li>
 * <li>to get the work days, ask the calendar (when that is available).</li>
 * <li>to get papersize / measurement system/bidi-orientation, ask the locale
 * (when that is available there)</li>
 * <li>to get the field order in a date, and whether a time is 24hour or not,
 * ask the DateFormat (when that is available there)</li>
 * <li>it will support HOST locale when it becomes available (it is a special
 * locale that will ask the services to use the host platform's values).</li>
 * </ul>
 *
 * @internal
 * @deprecated This API is ICU internal only.
 */
public class GlobalizationPreferences implements Freezable {
    /**
     * Number Format types
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int 
        NF_NUMBER = 0,      // NumberFormat.NUMBERSTYLE
        NF_CURRENCY = 1,    // NumberFormat.CURRENCYSTYLE
        NF_PERCENT = 2,     // NumberFormat.PERCENTSTYLE
        NF_SCIENTIFIC = 3,  // NumberFormat.SCIENTIFICSTYLE
        NF_INTEGER = 4;     // NumberFormat.INTEGERSTYLE

    private static final int NF_LIMIT = NF_INTEGER + 1;

    /**
     * Date Format types
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int
        DF_FULL = DateFormat.FULL,      // 0
        DF_LONG = DateFormat.LONG,      // 1
        DF_MEDIUM = DateFormat.MEDIUM,  // 2
        DF_SHORT = DateFormat.SHORT,    // 3
        DF_NONE = 4;

    private static final int DF_LIMIT = DF_NONE + 1;

    /**
     * For selecting a choice of display names
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int
        ID_LOCALE = 0,
        ID_LANGUAGE = 1,
        ID_SCRIPT = 2,
        ID_TERRITORY = 3,
        ID_VARIANT = 4, 
        ID_KEYWORD = 5,
        ID_KEYWORD_VALUE = 6,
        ID_CURRENCY = 7,
        ID_CURRENCY_SYMBOL = 8,
        ID_TIMEZONE = 9;
    
    private static final int ID_LIMIT = ID_TIMEZONE + 1;
    
    /**
     * Break iterator types
     * @internal
     * @deprecated This API is ICU internal only
     */
    public static final int
        BI_CHARACTER = BreakIterator.KIND_CHARACTER,    // 0
        BI_WORD = BreakIterator.KIND_WORD,              // 1
        BI_LINE = BreakIterator.KIND_LINE,              // 2
        BI_SENTENCE = BreakIterator.KIND_SENTENCE,      // 3
        BI_TITLE = BreakIterator.KIND_TITLE;            // 4

    private static final int BI_LIMIT = BI_TITLE + 1;
    
    /**
     * Sets the language/locale priority list. If other information is
     * not (yet) available, this is used to to produce a default value
     * for the appropriate territory, currency, timezone, etc.  The
     * user should be given the opportunity to correct those defaults
     * in case they are incorrect.
     * @param locales list of locales in priority order, eg {"be", "fr"} 
     *     for Breton first, then French if that fails.
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setLocales(List locales) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (locales.size() == 1) {
            this.locales = locales.get(0);
        } else if (locales.size() > 1) {
            this.locales = new ArrayList(locales); // clone for safety
        }
        return this;
    }

    /**
     * Get a copy of the language/locale priority list
     * @return a copy of the language/locale priority list.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List getLocales() {
        List result = new ArrayList(); // clone for safety
        if (locales == null) {
            result = guessLocales();
        } else if (locales instanceof ULocale) {
            result.add(locales);
        } else {
            result.addAll((List)locales);
        }
        return result;
    }

    /**
     * Convenience function for getting the locales in priority order
     * @param index The index (0..n) of the desired item.
     * @return desired item.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ULocale getLocale(int index) {
        if (locales == null) {
            return (ULocale)guessLocales().get(index);
        } else if (locales instanceof ULocale) {
            if (index != 0) {
                throw new IllegalArgumentException("Out of bounds: " + index);
            }
            return (ULocale)locales;
        } else {
            return (ULocale)((List)locales).get(index);
        }
    }

    /**
     * Convenience routine for setting the language/locale priority
     * list from an array.
     * @see #setLocales(List locales)
     * @param uLocales list of locales in an array
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setLocales(ULocale[] uLocales) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        return setLocales(Arrays.asList(uLocales));
    }

    /**
     * Convenience routine for setting the language/locale priority
     * list from a single locale/language.
     * @see #setLocales(List locales)
     * @param uLocale single locale
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setLocale(ULocale uLocale) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        return setLocales(new ULocale[]{uLocale});
    }

//#ifndef FOUNDATION
    /**
     * Convenience routine for setting the locale priority list from
     * an Accept-Language string.
     * @see #setLocales(List locales)
     * @param acceptLanguageString Accept-Language list, as defined by 
     *     Section 14.4 of the RFC 2616 (HTTP 1.1)
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setLocales(String acceptLanguageString) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        /*
          Accept-Language = "Accept-Language" ":" 1#( language-range [ ";" "q" "=" qvalue ] )
          x matches x-...
        */
        // reorders in quality order
        // don't care that it is not very efficient right now
        Matcher acceptMatcher = Pattern.compile("\\s*([-_a-zA-Z]+)(;q=([.0-9]+))?\\s*").matcher("");
        Map reorder = new TreeMap();
        String[] pieces = acceptLanguageString.split(",");
        
        for (int i = 0; i < pieces.length; ++i) {
            Double qValue = new Double(1);
            try {
                if (!acceptMatcher.reset(pieces[i]).matches()) {
                    throw new IllegalArgumentException();
                }
                String qValueString = acceptMatcher.group(3);
                if (qValueString != null) {
                    qValue = new Double(Double.parseDouble(qValueString));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("element '" + pieces[i] + 
                    "' is not of the form '<locale>{;q=<number>}");
            }
            List items = (List)reorder.get(qValue);
            if (items == null) {
                reorder.put(qValue, items = new LinkedList());
            }
            items.add(0, acceptMatcher.group(1)); // reverse order, will reverse again
        }
        // now read out in reverse order
        List result = new ArrayList();
        for (Iterator it = reorder.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            List items = (List)reorder.get(key);
            for (Iterator it2 = items.iterator(); it2.hasNext();) {
                result.add(0, new ULocale((String)it2.next()));
            }
        }
        return setLocales(result);
    }
//#endif

    /**
     * Convenience routine for populating locale fallback order.
     * @return a copy of fallback locale priority list
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List getFallbackLocales() {
        ArrayList fallbacks = new ArrayList();
        List localeList = getLocales();

        /*
         * Step 1: Relocate later occurence of more specific locale
         * before earlier occurence of less specific locale.
         *
         * Example:
         *   Before - en_US, fr_FR, zh, en_US_Boston, zh_TW, zh_Hant, fr_CA
         *   After  - en_US_Boston, en_US, fr_FR, zh_TW, zh_Hant, zh, fr_CA
         */
        for (int i = 0; i < localeList.size(); i++) {
            ULocale uloc = (ULocale)localeList.get(i);

            String language = uloc.getLanguage();
            String script = uloc.getScript();
            String country = uloc.getCountry();
            String variant = uloc.getVariant();

            boolean bInserted = false;
            for (int j = 0; j < fallbacks.size(); j++) {
                // Check if this locale is more specific
                // than existing locale entries already inserted
                // in the destination list
                ULocale u = (ULocale)fallbacks.get(j);
                if (!u.getLanguage().equals(language)) {
                    continue;
                }
                String s = u.getScript();
                String c = u.getCountry();
                String v = u.getVariant();
                if (!s.equals(script)) {
                    if (s.length() == 0 && c.length() == 0 && v.length() == 0) {
                        fallbacks.add(j, uloc);
                        bInserted = true;
                        break;
                    } else if (s.length() == 0 && c.equals(country)) {
                        // We want to see zh_Hant_HK before zh_HK
                        fallbacks.add(j, uloc);
                        bInserted = true;
                        break;                      
                    } else if (script.length() == 0 && country.length() > 0 && c.length() == 0) {
                        // We want to see zh_HK before zh_Hant
                        fallbacks.add(j, uloc);
                        bInserted = true;
                        break;
                    }
                    continue;
                }
                if (!c.equals(country)) {
                    if (c.length() == 0 && v.length() == 0) {
                        fallbacks.add(j, uloc);
                        bInserted = true;
                        break;
                    }
                }
                if (!v.equals(variant) && v.length() == 0) {
                    fallbacks.add(j, uloc);
                    bInserted = true;
                    break;
                }
            }
            if (!bInserted) {
                // Add this locale at the end of the list
                fallbacks.add(uloc);
            }
        }

        // TODO: Locale aliases should be resolved here
        // For example, zh_Hant_TW = zh_TW

        /*
         * Step 2: Append fallback locales for each entry
         *
         * Example:
         *   Before - en_US_Boston, en_US, fr_FR, zh_TW, zh_Hant, zh, fr_CA
         *   After  - en_US_Boston, en_US, en, en_US, en, fr_FR, fr,
         *            zh_TW, zn, zh_Hant, zh, zh, fr_CA, fr
         */
        int index = 0;
        while (index < fallbacks.size()) {
            ULocale uloc = (ULocale)fallbacks.get(index);
            while (true) {
                uloc = uloc.getFallback();
                if (uloc.getLanguage().length() == 0) {
                    break;
                }
                index++;
                fallbacks.add(index, uloc);
            }
            index++;
        }

        /*
         * Step 3: Remove earlier occurence of duplicated locales
         * 
         * Example:
         *   Before - en_US_Boston, en_US, en, en_US, en, fr_FR, fr,
         *            zh_TW, zn, zh_Hant, zh, zh, fr_CA, fr
         *   After  - en_US_Boston, en_US, en, fr_FR, zh_TW, zh_Hant,
         *            zh, fr_CA, fr
         */
        index = 0;
        while (index < fallbacks.size() - 1) {
            ULocale uloc = (ULocale)fallbacks.get(index);
            boolean bRemoved = false;
            for (int i = index + 1; i < fallbacks.size(); i++) {
                if (uloc.equals((ULocale)fallbacks.get(i))) {
                    // Remove ealier one
                    fallbacks.remove(index);
                    bRemoved = true;
                    break;
                }
            }
            if (!bRemoved) {
                index++;
            }
        }
        return fallbacks;
    }

    /**
     * Convenience function to get a ResourceBundle instance using
     * the specified base name based on the language/locale priority list
     * stored in this object.
     *  
     * @param baseName the base name of the resource bundle, a fully qualified
     * class name
     * @return a resource bundle for the given base name and locale based on the
     * language/locale priority list stored in this object
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ResourceBundle getResourceBundle(String baseName) {
        return getResourceBundle(baseName, null);
    }
    
    /**
     * Convenience function to get a ResourceBundle instance using
     * the specified base name and class loader based on the language/locale
     * priority list stored in this object.
     *  
     * @param baseName the base name of the resource bundle, a fully qualified
     * class name
     * @param loader the class object from which to load the resource bundle
     * @return a resource bundle for the given base name and locale based on the
     * language/locale priority list stored in this object
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ResourceBundle getResourceBundle(String baseName, ClassLoader loader) {
        UResourceBundle urb = null;
        UResourceBundle candidate = null;
        String actualLocaleName = null;
        List fallbacks = getFallbackLocales();
        for (int i = 0; i < fallbacks.size(); i++) {
            String localeName = ((ULocale)fallbacks.get(i)).toString();
            if (actualLocaleName != null && localeName.equals(actualLocaleName)) {
                // Actual locale name in the previous round may exactly matches
                // with the next fallback locale
                urb = candidate;
                break;
            }
            try {
                if (loader == null) {
                    candidate = UResourceBundle.getBundleInstance(baseName, localeName);
                }
                else {
                    candidate = UResourceBundle.getBundleInstance(baseName, localeName, loader);
                }
                if (candidate != null) {
                    actualLocaleName = candidate.getULocale().getName();
                    if (actualLocaleName.equals(localeName)) {
                        urb = candidate;
                        break;
                    }
                    if (urb == null) {
                        // Preserve the available bundle as the last resort
                        urb = candidate;
                    }
                }
            } catch (MissingResourceException mre) {
                actualLocaleName = null;
                continue;
            }
        }
        if (urb == null) {
            throw new MissingResourceException("Can't find bundle for base name "
                    + baseName, baseName, "");
        }
        return urb;
    }
    
    /**
     * Sets the territory, which is a valid territory according to for
     * RFC 3066 (or successor).  If not otherwise set, default
     * currency and timezone values will be set from this.  The user
     * should be given the opportunity to correct those defaults in
     * case they are incorrect.
     * @param territory code
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setTerritory(String territory) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.territory = territory;
        return this;
    }

    /**
     * Gets the territory setting. If it wasn't explicitly set, it is
     * computed from the general locale setting.
     * @return territory code, explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getTerritory() {
        if (territory == null) {
            return guessTerritory();
        }
        return territory; // immutable, so don't need to clone
    }

    /**
     * Sets the currency code. If this has not been set, uses default for territory.
     * @param currency Valid ISO 4217 currency code.
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setCurrency(Currency currency) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.currency = currency;
        return this;
    }

    /**
     * Get a copy of the currency computed according to the settings. 
     * @return currency code, explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Currency getCurrency() {
        if (currency == null) {
            return guessCurrency();
        }
        return currency; // immutable, so don't have to clone
    }

    /**
     * Sets the calendar. If this has not been set, uses default for territory.
     * @param calendar arbitrary calendar
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setCalendar(Calendar calendar) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.calendar = (Calendar) calendar.clone(); // clone for safety
        return this;
    }

    /**
     * Get a copy of the calendar according to the settings. 
     * @return calendar explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Calendar getCalendar() {
        if (calendar == null) {
            return guessCalendar();
        }
        Calendar temp = (Calendar) calendar.clone(); // clone for safety
        temp.setTimeZone(getTimeZone());
        return temp;
    }

    /**
     * Sets the timezone ID.  If this has not been set, uses default for territory.
     * @param timezone a valid TZID (see UTS#35).
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setTimeZone(TimeZone timezone) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.timezone = (TimeZone) timezone.clone(); // clone for safety;
        return this;
    }

    /**
     * Get the timezone. It was either explicitly set, or is
     * heuristically computed from other settings.
     * @return timezone, either implicitly or explicitly set
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public TimeZone getTimeZone() {
        if (timezone == null) {
            return guessTimeZone();
        }
        return (TimeZone) timezone.clone(); // clone for safety
    }

    /**
     * Get a copy of the collator according to the settings. 
     * @return collator explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Collator getCollator() {
        if (collator == null) {
            return guessCollator();
        }
        try {
            return (Collator) collator.clone();  // clone for safety
        } catch (CloneNotSupportedException e) {
        	throw new IllegalStateException("Error in cloning collator");
        }
    }

    /**
     * Explicitly set the collator for this object.
     * @param collator
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setCollator(Collator collator) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        try {
            this.collator = (Collator) collator.clone();            
        } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Error in cloning collator");
        }
        return this;
    }

    /**
     * Get a copy of the break iterator for the specified type according to the
     * settings.
     * 
     * @param type
     *          break type - CHARACTER or TITLE, WORD, LINE, SENTENC
     * @return break iterator explicit or implicit
     */
    public BreakIterator getBreakIterator(int type) {
        if (breakIterators == null || breakIterators[type] == null) {
            return guessBreakIterator(type);
        }
        return (BreakIterator) breakIterators[type].clone(); // clone for safety
    }

    /**
     * Explicitly set the break iterator for this object.
     * 
     * @param type
     *          break type - CHARACTER or TITLE, WORD, LINE, SENTENC
     * @param iterator a break iterator
     * @return
     */
    public GlobalizationPreferences setBreakIterator(int type, BreakIterator iterator) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (breakIterators == null)
            breakIterators = new BreakIterator[BI_LIMIT];
        breakIterators[type] = (BreakIterator) iterator.clone(); // clone for safety
        return this;
    }

    
    /**
     * Set the date locale. 
     * @param dateLocale If not null, overrides the locale priority list for all the date formats.
     * @return this, for chaining
     */
    public GlobalizationPreferences setDateLocale(ULocale dateLocale) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.dateLocale = dateLocale;
        return this;
    }

    /**
     * Gets the date locale, to be used in computing date formats. Overrides the general locale setting.
     * @return date locale. Null if none was set explicitly.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ULocale getDateLocale() {
        return dateLocale != null ? dateLocale : getLocale(0);
    }
    
    /**
     * Set the number locale. 
     * @param numberLocale If not null, overrides the locale priority list for all the date formats.
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setNumberLocale(ULocale numberLocale) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.numberLocale = numberLocale;
        return this;
    }

    /**
     * Get the current number locale setting used for getNumberFormat.
     * @return number locale. Null if none was set explicitly.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ULocale getNumberLocale() {
        return numberLocale != null ? numberLocale : getLocale(0);
    }
    
    /**
     * Get the display name for an ID: language, script, territory, currency, timezone...
     * Uses the language priority list to do so.
     * @param id language code, script code, ...
     * @param type specifies the type of the ID: LANGUAGE, etc.
     * @return the display name
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getDisplayName(String id, int type) {
        String result = id;
        for (Iterator it = getLocales().iterator(); it.hasNext();) {
            ULocale locale = (ULocale) it.next();
            switch (type) {
            case ID_LOCALE:
                result = ULocale.getDisplayName(id, locale); 
                break;
            case ID_LANGUAGE:
                result = ULocale.getDisplayLanguage(id, locale); 
                break;
            case ID_SCRIPT:
                result = ULocale.getDisplayScript("und-" + id, locale); 
                break;
            case ID_TERRITORY:
                result = ULocale.getDisplayCountry("und-" + id, locale); 
                break;
            case ID_VARIANT:
                // TODO fix variant parsing
                result = ULocale.getDisplayVariant("und-QQ-" + id, locale); 
                break;
            case ID_KEYWORD:
                result = ULocale.getDisplayKeyword(id, locale); 
                break;
            case ID_KEYWORD_VALUE:
                String[] parts = new String[2];
                Utility.split(id,'=',parts);
                result = ULocale.getDisplayKeywordValue("und@"+id, parts[0], locale);
                // TODO fix to tell when successful
                if (result.equals(parts[1])) continue;
                break;
            case ID_CURRENCY_SYMBOL:
            case ID_CURRENCY:
                Currency temp = new Currency(id);
                result =temp.getName(locale, type==ID_CURRENCY 
                                     ? Currency.LONG_NAME 
                                     : Currency.SYMBOL_NAME, new boolean[1]);
                // TODO: have method that doesn't take parameter. Add
                // function to determine whether string is choice
                // format.  
                // TODO: have method that doesn't require us
                // to create a currency
                break;
            case ID_TIMEZONE:
                SimpleDateFormat dtf = new SimpleDateFormat("vvvv",locale);
                dtf.setTimeZone(TimeZone.getTimeZone(id));
                result = dtf.format(new Date());
                // TODO, have method that doesn't require us to create a timezone
                // fix other hacks
                // hack for couldn't match
                // note, compiling with FOUNDATION omits this check for now
//#ifndef FOUNDATION
                if (badTimeZone.reset(result).matches()) continue;
//#endif
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
            }
            if (!id.equals(result)) return result;
            // TODO need better way of seeing if we fell back to root!!
            // This will not work at all for lots of stuff
        }
        return result;
    }
//#ifndef FOUNDATION
    // TODO remove need for this
    private static final Matcher badTimeZone = Pattern.compile("[A-Z]{2}|.*\\s\\([A-Z]{2}\\)").matcher("");
//#endif


    /**
     * Set an explicit date format. Overrides both the date locale,
     * and the locale priority list for a particular combination of
     * dateStyle and timeStyle. NONE should be used if for the style,
     * where only the date or time format individually is being set.
     * @param dateStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @param timeStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @param format
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setDateFormat(int dateStyle, int timeStyle, DateFormat format) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (dateFormats == null) {
            dateFormats = new Object[DF_LIMIT][DF_LIMIT];
        }
        dateFormats[dateStyle][timeStyle] = (DateFormat) format.clone(); // for safety
        return this;
    }

    /**
     * Set an explicit date format. Overrides both the date locale,
     * and the locale priority list for a particular combination of
     * dateStyle and timeStyle. NONE should be used if for the style,
     * where only the date or time format individually is being set.
     * @param dateStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @param timeStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @param formatPattern date pattern, eg "yyyy-MMM-dd"
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setDateFormat(int dateStyle, int timeStyle, String formatPattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (dateFormats == null) {
            dateFormats = new Object[DF_LIMIT][DF_LIMIT];
        }
        // test the format to make sure it won't throw an error later
        new SimpleDateFormat(formatPattern, getDateLocale());
        dateFormats[dateStyle][timeStyle] = formatPattern; // for safety
        return this;
    }

    /**
     * Gets a date format according to the current settings. If there
     * is an explicit (non-null) date/time format set, a copy of that
     * is returned. Otherwise, if there is a non-null date locale,
     * that is used.  Otherwise, the language priority list is
     * used. NONE should be used for the style, where only the date or
     * time format individually is being gotten.
     * @param dateStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @param timeStyle NONE, or DateFormat.FULL, LONG, MEDIUM, SHORT
     * @return a DateFormat, according to the above description
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public DateFormat getDateFormat(int dateStyle, int timeStyle) {
        try {
            DateFormat result = null;
            if (dateFormats != null) { // and override can either be a string or a pattern
                Object temp = dateFormats[dateStyle][timeStyle];
                if (temp instanceof DateFormat) {
                    result = (DateFormat) temp;
                } else {
                    result = new SimpleDateFormat((String)temp, getDateLocale());
                }   
            }
            if (result != null) {
                result = (DateFormat) result.clone(); // clone for safety
                result.setCalendar(getCalendar());
            } else {
                // In the case of date formats, we don't have to look at more than one
                // locale. May be different for other cases
                // TODO Make this one function.
                if (timeStyle == DF_NONE) {
                    result = DateFormat.getDateInstance(getCalendar(), dateStyle, getDateLocale());
                } else if (dateStyle == DF_NONE) {
                    result = DateFormat.getTimeInstance(getCalendar(), timeStyle, getDateLocale());
                } else {
                    result = DateFormat.getDateTimeInstance(getCalendar(), dateStyle, timeStyle, getDateLocale());
                }
            }
            return result;
        } catch (RuntimeException e) {
            IllegalArgumentException ex = new IllegalArgumentException("Cannot create DateFormat");
//#ifndef FOUNDATION
            ex.initCause(e);
//#endif
            throw ex;
        }
    }
    
    /**
     * Gets a number format according to the current settings.  If
     * there is an explicit (non-null) number format set, a copy of
     * that is returned. Otherwise, if there is a non-null number
     * locale, that is used.  Otherwise, the language priority list is
     * used. NONE should be used for the style, where only the date or
     * time format individually is being gotten.
     * @param style CURRENCY, NUMBER, INTEGER, SCIENTIFIC, PERCENT
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public NumberFormat getNumberFormat(int style) {
        try {
            NumberFormat result = null;
            if (numberFormats != null) {
                Object temp = numberFormats[style];
                if (temp instanceof NumberFormat) {
                    result = (NumberFormat) temp;
                } else {
                    result = new DecimalFormat((String)temp, new DecimalFormatSymbols(getDateLocale()));
                }   
            }
            if (result != null) {
                result = (NumberFormat) result.clone(); // clone for safety (later optimize)
                if (style == NF_CURRENCY) {
                    result.setCurrency(getCurrency());
                }
                return result; 
            }
            // In the case of date formats, we don't have to look at more than one
            // locale. May be different for other cases
            switch (style) {
            case NF_NUMBER: return NumberFormat.getInstance(getNumberLocale());
            case NF_SCIENTIFIC: return NumberFormat.getScientificInstance(getNumberLocale());
            case NF_INTEGER: return NumberFormat.getIntegerInstance(getNumberLocale());
            case NF_PERCENT: return NumberFormat.getPercentInstance(getNumberLocale());
            case NF_CURRENCY: result = NumberFormat.getCurrencyInstance(getNumberLocale());
                result.setCurrency(getCurrency());
                return result;
            }
        } catch (RuntimeException e) {}
        throw new IllegalArgumentException(); // fix later
    }
    
    /**
     * Sets a number format explicitly. Overrides the number locale
     * and the general locale settings.
     * @param style CURRENCY, NUMBER, INTEGER, SCIENTIFIC, PERCENT
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setNumberFormat(int style, DateFormat format) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (numberFormats == null) {
            numberFormats = new Object[NF_LIMIT];
        }
        numberFormats[style] = (NumberFormat) format.clone(); // for safety
        return this;
    }
    
    /**
     * Sets a number format explicitly. Overrides the number locale
     * and the general locale settings.
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences setNumberFormat(int style, String formatPattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (numberFormats == null) {
            numberFormats = new Object[NF_LIMIT];
        }
        // check to make sure it compiles
        new DecimalFormat((String)formatPattern, new DecimalFormatSymbols(getDateLocale()));
        numberFormats[style] = formatPattern; // for safety
        return this;
    }

    /**
     * Restore the object to the initial state.
     * @return this, for chaining
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public GlobalizationPreferences reset() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        territory = null;
        calendar = null;
        collator = null;
        breakIterators = null;
        timezone = null;
        currency = null;
        dateFormats = null;
        numberFormats = null;
        dateLocale = null;
        numberLocale = null;
        locales = null;
        return this;
    }
    
    /**
     * This function can be overridden by subclasses to use different heuristics.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected String guessTerritory() {
        String result;
        // pass through locales to see if there is a territory.
        for (Iterator it = getLocales().iterator(); it.hasNext();) {
            ULocale locale = (ULocale)it.next();
            result = locale.getCountry();
            if (result.length() != 0) {
                return result;
            }
        }
        // if not, guess from the first language tag, or maybe from
        // intersection of languages, eg nl + fr => BE
        // TODO: fix using real data
        // for now, just use fixed values
        ULocale firstLocale = getLocale(0);
        String language = firstLocale.getLanguage();
        String script = firstLocale.getScript();
        result = null;
        if (script.length() != 0) {
            result = (String) language_territory_hack_map.get(language + "_" + script);
        }
        if (result == null) {
            result = (String) language_territory_hack_map.get(language);
        }
        if (result == null) {
            result = "US"; // need *some* default
        }
        return result;
    }

    /**
     * This function can be overridden by subclasses to use different heuristics
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected Currency guessCurrency() {
        return Currency.getInstance(new ULocale("und-" + getTerritory()));
    }

    /**
     * This function can be overridden by subclasses to use different heuristics
     * <b>It MUST return a 'safe' value,
     * one whose modification will not affect this object.</b>
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected List guessLocales() {
        List result = new ArrayList(0);
        result.add(ULocale.getDefault());
        return result;
    }

    /**
     * This function can be overridden by subclasses to use different heuristics.
     * <b>It MUST return a 'safe' value,
     * one whose modification will not affect this object.</b>
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected Collator guessCollator() {
        return Collator.getInstance(getLocale(0));
    }

    protected BreakIterator guessBreakIterator(int type) {
        BreakIterator bitr = null;
        switch (type) {
        case BI_CHARACTER:
            bitr = BreakIterator.getCharacterInstance(getLocale(0));
            break;
        case BI_TITLE:
            bitr = BreakIterator.getTitleInstance(getLocale(0));
            break;
        case BI_WORD:
            bitr = BreakIterator.getWordInstance(getLocale(0));
            break;
        case BI_LINE:
            bitr = BreakIterator.getLineInstance(getLocale(0));
            break;
        case BI_SENTENCE:
            bitr = BreakIterator.getSentenceInstance(getLocale(0));
            break;
        default:
            throw new IllegalArgumentException("Unknown break iterator type");
        }
        return bitr;
    }

    /**
     * This function can be overridden by subclasses to use different heuristics.
     * <b>It MUST return a 'safe' value,
     * one whose modification will not affect this object.</b>
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected TimeZone guessTimeZone() {
        // TODO fix using real data
        // for single-zone countries, pick that zone
        // for others, pick the most populous zone
        // for now, just use fixed value
        // NOTE: in a few cases can do better by looking at language. 
        // Eg haw+US should go to Pacific/Honolulu
        // fr+CA should go to America/Montreal
        String timezoneString = (String) territory_tzid_hack_map.get(getTerritory());
        if (timezoneString == null) {
            String[] attempt = ZoneMeta.getAvailableIDs(getTerritory());
            if (attempt.length == 0) {
                timezoneString = "Etc/GMT"; // gotta do something
            } else {
                int i;
                // this all needs to be fixed to use real data. But for now, do slightly better by skipping cruft
                for (i = 0; i < attempt.length; ++i) {
                    if (attempt[i].indexOf("/") >= 0) break;
                }
                if (i > attempt.length) i = 0;
                timezoneString = attempt[i];
            }
        }
        return TimeZone.getTimeZone(timezoneString);
    }

    /**
     * This function can be overridden by subclasses to use different heuristics.
     * <b>It MUST return a 'safe' value,
     * one whose modification will not affect this object.</b>
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected Calendar guessCalendar() {
        // TODO add better API
        return Calendar.getInstance(new ULocale("und-" + getTerritory()));
    }
    
    // PRIVATES
    
    private Object locales;
    private String territory;
    private Currency currency;
    private TimeZone timezone;
    private Calendar calendar;
    private Collator collator;
    private BreakIterator[] breakIterators;
    
    private ULocale dateLocale;
    private Object[][] dateFormats;
    private ULocale numberLocale;
    private Object[] numberFormats;
    
    {
        reset();
    }
    
    /** WARNING: All of this data is temporary, until we start importing from CLDR!!!
     * 
     */
    private static final Map language_territory_hack_map = new HashMap();
    private static final String[][] language_territory_hack = {
        {"af", "ZA"},
        {"am", "ET"},
        {"ar", "SA"},
        {"as", "IN"},
        {"ay", "PE"},
        {"az", "AZ"},
        {"bal", "PK"},
        {"be", "BY"},
        {"bg", "BG"},
        {"bn", "IN"},
        {"bs", "BA"},
        {"ca", "ES"},
        {"ch", "MP"},
        {"cpe", "SL"},
        {"cs", "CZ"},
        {"cy", "GB"},
        {"da", "DK"},
        {"de", "DE"},
        {"dv", "MV"},
        {"dz", "BT"},
        {"el", "GR"},
        {"en", "US"},
        {"es", "ES"},
        {"et", "EE"},
        {"eu", "ES"},
        {"fa", "IR"},
        {"fi", "FI"},
        {"fil", "PH"},
        {"fj", "FJ"},
        {"fo", "FO"},
        {"fr", "FR"},
        {"ga", "IE"},
        {"gd", "GB"},
        {"gl", "ES"},
        {"gn", "PY"},
        {"gu", "IN"},
        {"gv", "GB"},
        {"ha", "NG"},
        {"he", "IL"},
        {"hi", "IN"},
        {"ho", "PG"},
        {"hr", "HR"},
        {"ht", "HT"},
        {"hu", "HU"},
        {"hy", "AM"},
        {"id", "ID"},
        {"is", "IS"},
        {"it", "IT"},
        {"ja", "JP"},
        {"ka", "GE"},
        {"kk", "KZ"},
        {"kl", "GL"},
        {"km", "KH"},
        {"kn", "IN"},
        {"ko", "KR"},
        {"kok", "IN"},
        {"ks", "IN"},
        {"ku", "TR"},
        {"ky", "KG"},
        {"la", "VA"},
        {"lb", "LU"},
        {"ln", "CG"},
        {"lo", "LA"},
        {"lt", "LT"},
        {"lv", "LV"},
        {"mai", "IN"},
        {"men", "GN"},
        {"mg", "MG"},
        {"mh", "MH"},
        {"mk", "MK"},
        {"ml", "IN"},
        {"mn", "MN"},
        {"mni", "IN"},
        {"mo", "MD"},
        {"mr", "IN"},
        {"ms", "MY"},
        {"mt", "MT"},
        {"my", "MM"},
        {"na", "NR"},
        {"nb", "NO"},
        {"nd", "ZA"},
        {"ne", "NP"},
        {"niu", "NU"},
        {"nl", "NL"},
        {"nn", "NO"},
        {"no", "NO"},
        {"nr", "ZA"},
        {"nso", "ZA"},
        {"ny", "MW"},
        {"om", "KE"},
        {"or", "IN"},
        {"pa", "IN"},
        {"pau", "PW"},
        {"pl", "PL"},
        {"ps", "PK"},
        {"pt", "BR"},
        {"qu", "PE"},
        {"rn", "BI"},
        {"ro", "RO"},
        {"ru", "RU"},
        {"rw", "RW"},
        {"sd", "IN"},
        {"sg", "CF"},
        {"si", "LK"},
        {"sk", "SK"},
        {"sl", "SI"},
        {"sm", "WS"},
        {"so", "DJ"},
        {"sq", "CS"},
        {"sr", "CS"},
        {"ss", "ZA"},
        {"st", "ZA"},
        {"sv", "SE"},
        {"sw", "KE"},
        {"ta", "IN"},
        {"te", "IN"},
        {"tem", "SL"},
        {"tet", "TL"},
        {"th", "TH"},
        {"ti", "ET"},
        {"tg", "TJ"},
        {"tk", "TM"},
        {"tkl", "TK"},
        {"tvl", "TV"},
        {"tl", "PH"},
        {"tn", "ZA"},
        {"to", "TO"},
        {"tpi", "PG"},
        {"tr", "TR"},
        {"ts", "ZA"},
        {"uk", "UA"},
        {"ur", "IN"},
        {"uz", "UZ"},
        {"ve", "ZA"},
        {"vi", "VN"},
        {"wo", "SN"},
        {"xh", "ZA"},
        {"zh", "CN"},
        {"zh_Hant", "TW"},
        {"zu", "ZA"},
        {"aa", "ET"},
        {"byn", "ER"},
        {"eo", "DE"},
        {"gez", "ET"},
        {"haw", "US"},
        {"iu", "CA"},
        {"kw", "GB"},
        {"sa", "IN"},
        {"sh", "HR"},
        {"sid", "ET"},
        {"syr", "SY"},
        {"tig", "ER"},
        {"tt", "RU"},
        {"wal", "ET"},  };
    static {
        for (int i = 0; i < language_territory_hack.length; ++i) {
            language_territory_hack_map.put(language_territory_hack[i][0],language_territory_hack[i][1]);
        }
    }
    
    static final Map territory_tzid_hack_map = new HashMap();
    static final String[][] territory_tzid_hack = {
        {"AQ", "Antarctica/McMurdo"},
        {"AR", "America/Buenos_Aires"},
        {"AU", "Australia/Sydney"},
        {"BR", "America/Sao_Paulo"},
        {"CA", "America/Toronto"},
        {"CD", "Africa/Kinshasa"},
        {"CL", "America/Santiago"},
        {"CN", "Asia/Shanghai"},
        {"EC", "America/Guayaquil"},
        {"ES", "Europe/Madrid"},
        {"GB", "Europe/London"},
        {"GL", "America/Godthab"},
        {"ID", "Asia/Jakarta"},
        {"ML", "Africa/Bamako"},
        {"MX", "America/Mexico_City"},
        {"MY", "Asia/Kuala_Lumpur"},
        {"NZ", "Pacific/Auckland"},
        {"PT", "Europe/Lisbon"},
        {"RU", "Europe/Moscow"},
        {"UA", "Europe/Kiev"},
        {"US", "America/New_York"},
        {"UZ", "Asia/Tashkent"},
        {"PF", "Pacific/Tahiti"},
        {"FM", "Pacific/Kosrae"},
        {"KI", "Pacific/Tarawa"},
        {"KZ", "Asia/Almaty"},
        {"MH", "Pacific/Majuro"},
        {"MN", "Asia/Ulaanbaatar"},
        {"SJ", "Arctic/Longyearbyen"},
        {"UM", "Pacific/Midway"},   
    };
    static {
        for (int i = 0; i < territory_tzid_hack.length; ++i) {
            territory_tzid_hack_map.put(territory_tzid_hack[i][0],territory_tzid_hack[i][1]);
        }
    }

    private boolean frozen;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Object freeze() {
        frozen = true;
        return this;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Object cloneAsThawed() {
        try {
            GlobalizationPreferences result = (GlobalizationPreferences) clone();
            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            // will always work
            return null;
        }
    }
}

