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
import java.util.TreeMap;

//#ifndef FOUNDATION
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//#endif

import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.ZoneMeta;
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
 * <li>Separate out base class</li>
 * <li>Add BreakIterator</li>
 * <li>Add Holidays</li>
 * <li>Add convenience to get/take Locale as well as ULocale.</li>
 * <li>Add getResourceBundle(String baseName, ClassLoader loader);</li>
 * <li>Add getFallbackLocales();</li>
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
    public static final int CURRENCY = 0, NUMBER = 1, INTEGER = 2, SCIENTIFIC = 3, 
        PERCENT = 4, NUMBER_LIMIT = 5;

    /**
     * Supplement to DateFormat.FULL, LONG, MEDIUM, SHORT. Indicates
     * that no value for one of date or time is to be used.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int NONE = 4;

    /**
     * For selecting a choice of display names
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int
        LOCALEID = 0, LANGUAGEID = 1, SCRIPTID = 2, TERRITORYID = 3, VARIANTID = 4, 
        KEYWORDID = 5, KEYWORD_VALUEID = 6,
        CURRENCYID = 7, CURRENCY_SYMBOLID = 8, TIMEZONEID = 9, DISPLAYID_LIMIT = 10;
	
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
        if (locales.size() == 0) {
            this.locales = locales.get(0);
        } else {
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
            if (index != 0) throw new IllegalArgumentException("Out of bounds: " + index);
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
                if (qValueString != null) qValue = new Double(Double.parseDouble(qValueString));
            } catch (Exception e) {
                throw new IllegalArgumentException("element '" + pieces[i] + 
                    "' is not of the form '<locale>{;q=<number>}");
            }
            List items = (List)reorder.get(qValue);
            if (items == null) reorder.put(qValue, items = new LinkedList());
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
        if (territory == null) return guessTerritory();
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
        if (currency == null) return guessCurrency();
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
        this.calendar = calendar;
        return this;
    }

    /**
     * Get a copy of the calendar according to the settings. 
     * @return calendar explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Calendar getCalendar() {
        if (calendar == null) return guessCalendar();
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
        this.timezone = timezone;
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
        if (timezone == null) return guessTimeZone();
        return (TimeZone) timezone.clone(); // clone for safety
    }
	
    /**
     * Get a copy of the collator according to the settings. 
     * @return collator explicit or implicit.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Collator getCollator() {
        if (collator == null) return guessCollator();
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
        this.collator = collator;
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
            case LOCALEID: 
                result = ULocale.getDisplayName(id, locale); 
                break;
            case LANGUAGEID: 
                result = ULocale.getDisplayLanguage(id, locale); 
                break;
            case SCRIPTID: 
                result = ULocale.getDisplayScript("und-" + id, locale); 
                break;
            case TERRITORYID: 
                result = ULocale.getDisplayCountry("und-" + id, locale); 
                break;
            case VARIANTID: 
                // TODO fix variant parsing
                result = ULocale.getDisplayVariant("und-QQ-" + id, locale); 
                break;
            case KEYWORDID: 
                result = ULocale.getDisplayKeyword(id, locale); 
                break;
            case KEYWORD_VALUEID:
                String[] parts = new String[2];
                Utility.split(id,'=',parts);
                result = ULocale.getDisplayKeywordValue("und@"+id, parts[0], locale);
                // TODO fix to tell when successful
                if (result.equals(parts[1])) continue;
                break;
            case CURRENCY_SYMBOLID:
            case CURRENCYID:
                Currency temp = new Currency(id);
                result =temp.getName(locale, type==CURRENCYID 
                                     ? Currency.LONG_NAME 
                                     : Currency.SYMBOL_NAME, new boolean[1]);
                // TODO: have method that doesn't take parameter. Add
                // function to determine whether string is choice
                // format.  
                // TODO: have method that doesn't require us
                // to create a currency
                break;
            case TIMEZONEID:
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
        if (dateFormats == null) dateFormats = new Object[NONE+1][NONE+1];
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
        if (dateFormats == null) dateFormats = new Object[NONE+1][NONE+1];
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
                if (timeStyle == NONE) {
                    result = DateFormat.getDateInstance(getCalendar(), dateStyle, getDateLocale());
                } else if (dateStyle == NONE) {
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
                if (style == CURRENCY) {
                    result.setCurrency(getCurrency());
                }
                return result; 
            }
            // In the case of date formats, we don't have to look at more than one
            // locale. May be different for other cases
            switch (style) {
            case NUMBER: return NumberFormat.getInstance(getNumberLocale());
            case SCIENTIFIC: return NumberFormat.getScientificInstance(getNumberLocale());
            case INTEGER: return NumberFormat.getIntegerInstance(getNumberLocale());
            case PERCENT: return NumberFormat.getPercentInstance(getNumberLocale());
            case CURRENCY: result = NumberFormat.getCurrencyInstance(getNumberLocale());
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
        if (numberFormats == null) numberFormats = new Object[NUMBER_LIMIT];
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
        if (numberFormats == null) numberFormats = new Object[NUMBER_LIMIT];
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
        if (result == null) result = (String) language_territory_hack_map.get(language);
        if (result == null) result = "US"; // need *some* default
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
        {"wal", "ET"},	};
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

