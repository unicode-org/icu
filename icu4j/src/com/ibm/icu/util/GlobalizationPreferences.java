/*
 *******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.util;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xml.utils.UnImplNode;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;

public class GlobalizationPreferences {
	/**
	 * Number Format types
	 */
	public static final int CURRENCY = 0, NUMBER = 1, INTEGER = 2, SCIENTIFIC = 3, PERCENT = 4, NUMBER_LIMIT = 5;
	/**
	 * Supplement to DateFormat.FULL, LONG, MEDIUM, SHORT. Indicates that no value for one of date or time is to be used.
	 */
	public static final int NONE = 4;
	/**
	 * Display Name Item (TODO: flesh out)
	 */
	public static final int
		LOCALEID = 0, LANGUAGEID = 1, SCRIPTID = 2, TERRITORYID = 3, VARIANTID = 4, 
		KEYWORDID = 5, KEYWORD_VALUEID = 6,
		CURRENCYID = 7, CURRENCY_SYMBOLID = 8, TIMEZONEID = 9, DISPLAYID_LIMIT = 10;
	
	/**
	 * Just for testing right now. Will remove later.
	 */
	private static final String[] TYPENAMES = {
		"locale", "language", "script", "territory", "variant",
		"keyword", "keyword=value",
		"currency", "currency-symbol", "timezone"
	};
	private static final String[] ContextNames = {"format", "standalone"};
	private static final String[] WidthNames = {"abbreviated", "wide", "narrow"};
	
	public static void main(String[] args) throws IOException {
		PrintWriter out = BagFormatter.openUTF8Writer("c:/", "tempFile.txt");
		try {
			Date now = new Date();
			
			GlobalizationPreferences lPreferences = new GlobalizationPreferences();
			out.println("Date Formatting");
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, NONE).format(now));
			
			out.println("setting locale to Germany");
			lPreferences.setULocales(ULocale.GERMANY);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, NONE).format(now));
			
			out.println("setting date locale to France");
			lPreferences.setDateLocale(ULocale.FRANCE);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, NONE).format(now));
			
			out.println("setting date format to yyyy-MMM-dd (Italy)");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd",ULocale.ITALY);
			lPreferences.setDateFormat(DateFormat.FULL, NONE, sdf);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, NONE).format(now));
			
			out.println();
			out.println("Various named date fields");
			SimpleDateFormat df = (SimpleDateFormat)lPreferences.getDateFormat(DateFormat.FULL, NONE);
			DateFormatSymbols dfs = df.getDateFormatSymbols();
			
			for (int context = dfs.FORMAT; context <= dfs.STANDALONE; ++context) {
				out.println("context: " + ContextNames[context-dfs.FORMAT]);
				for (int width = dfs.ABBREVIATED; width <= dfs.NARROW; ++width) {
					out.println("\twidth: " + WidthNames[width-dfs.ABBREVIATED]);
					out.println("\t\tgetAmPmStrings:\t" + Arrays.asList(dfs.getAmPmStrings()));
					out.println("\t\tgetEras\t" + Arrays.asList((width > 0 ? dfs.getEraNames() : dfs.getEras())));
					try {
						out.println("\t\tgetMonths:\t" + Arrays.asList(dfs.getMonths(context, width)));
					} catch (RuntimeException e) {
						out.println("\t\tgetMonths:\t" + "none");
					}	
					try {
						out.println("\t\tgetWeekdays:\t" + Arrays.asList(dfs.getWeekdays(context, width)));
					} catch (RuntimeException e) {
						out.println("\t\tgetWeekdays:\t" + "none");
					}	
				}
			}
			
			// now show currencies
			out.println();
			out.println("Currency Formatting");
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(CURRENCY).format(1234.567));
			
			out.println("setting number locale to Canada");
			lPreferences.setNumberLocale(ULocale.CANADA);
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(CURRENCY).format(1234.567));
			
			out.println("setting currency to INR"); 
			lPreferences.setCurrency(Currency.getInstance("INR"));
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(CURRENCY).format(1234.567));
			
			out.println("setting number locale to Hindi-India"); 
			lPreferences.setNumberLocale(new ULocale("hi-IN"));
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(CURRENCY).format(1234.567));
			
			// now try a fallback within locales
			out.println();
			out.println("Display Names");
			lPreferences.setULocales(new ULocale[]{new ULocale("as"),new ULocale("pl"),new ULocale("fr")});
			out.println("Trying fallback for multiple locales: " + lPreferences.getULocales());
			String[][] testItems = {
					{LOCALEID+"", "as_FR", "en_RU","haw_CA","se_Cyrl_AT"},
					{LANGUAGEID+"", "as", "en","haw","se","kok"},
					{SCRIPTID+"", "Arab", "Cyrl", "Hant"},
					{TERRITORYID+"", "US", "FR", "AU", "RU","IN"},
					{VARIANTID+"","REVISED"},
					{KEYWORDID+"","calendar", "collation", "currency"},
					{KEYWORD_VALUEID+"", "calendar=buddhist", "calendar=gregorian", 
						"collation=phonebook", "collation=traditional"},
					{CURRENCYID+"", "USD", "GBP", "EUR", "JPY","INR"},
					{CURRENCY_SYMBOLID+"", "USD", "GBP", "EUR", "JPY","INR"},
					{TIMEZONEID+"", "America/Mexico_City", "Asia/Shanghai", "Europe/London", "Europe/Berlin"},
					};
			for (int i = 0; i < testItems.length; ++i) {
				int type = Integer.parseInt(testItems[i][0]);
				String typeName = TYPENAMES[type];
				for (int j = 1; j < testItems[i].length; ++j) {
					String item = testItems[i][j];
					out.println(typeName + " for " + item + ": \t"
							+ lPreferences.getDisplayName(item, type));
				}
			}
		} finally {
			out.close();
			System.out.println("done");
		}
	}
	/**
	 * Sets the language priority list. If other information is not (yet) available, this is used to
	 * to produce a default value for the appropriate territory, currency, timezone, etc.
	 * The user should be given the opportunity to correct those defaults in case they are incorrect.
	 * @param locales list of locales in priority order, eg {"be", "fr"} for Breton first, then French if that fails.
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setULocales(List locales) {
		this.locales = new ArrayList(locales);
		explicitLocales = true;
		setTerritoryFromLocales();
		setCurrencyFromTerritory();
		setTimeZoneFromTerritory();
		setCalendarFromTerritory();
		return this;
	}
	/**
	 * @return a copy of the language priority list.
	 */
	public List getULocales() {
		return new ArrayList(locales); // clone for safety
	}

	/**
	 * Convenience routine for setting the locale priority list
	 * @see setULocales(List locales)
	 * @param locales list of locales in an array
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setULocales(ULocale[] locales) {
		return setULocales(Arrays.asList(locales));
	}
	/**
	 * Convenience routine for setting the locale priority list
	 * @see setULocales(List locales)
	 * @param ulocale single locale
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setULocales(ULocale uLocale) {
		return setULocales(new ULocale[]{uLocale});
	}
	/**
	 * Convenience routine for setting the locale priority list
	 * @see setULocales(List locales)
	 * @param ulocale Accept-Language list, as defined by Section 14.4 of the RFC 2616 (HTTP 1.1)
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setULocales(String acceptLanguageString) {
		// change Accept-Language list into ordered list of ULocales
		// then call setULocales with that list
		if (true) throw new UnsupportedOperationException("not yet implemented");
		return this;
	}
	/**
	 * Sets the territory, which is a valid territory according to for RFC 3066 (or successor).
	 * If not otherwise set, default currency and timezone values will be set from this.
	 * The user should be given the opportunity to correct those defaults in case they are incorrect.
	 * @param territory code
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setTerritory(String territory) {
		this.territory = territory;
		explicitTerritory = true;
		setCurrencyFromTerritory();
		setTimeZoneFromTerritory();
		setCalendarFromTerritory();
		return this;
	}
	/**
	 * @return territory code, explicit or implicit.
	 */
	public String getTerritory() {
		return territory; // immutable, so don't need to clone
	}

	/**
	 * Sets the currency code. If this has not been set, uses default for territory.
	 * @param currency Valid ISO 4217 currency code.
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setCurrency(Currency currency) {
		this.currency = currency;
		explicitCurrency = true;
		return this;
	}
	/**
	 * @return currency code, explicit or implicit.
	 */
	public Currency getCurrency() {
		return currency; // immutable, so don't have to clone
	}

	/**
	 * Sets the calendar. If this has not been set, uses default for territory.
	 * @param calendar arbitrary calendar
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setCalendar(Calendar calendar) {
		this.calendar = calendar;
		explicitCalendar = true;
		return this;
	}
	/**
	 * @return currency code, explicit or implicit.
	 */
	public Calendar getCalendar() {
		return (Calendar) calendar.clone(); // clone for safety
	}

	/**
	 * Sets the timezone ID.  If this has not been set, uses default for territory.
	 * @param timezone a valid TZID (see UTS#35).
	 * @return
	 */
	public GlobalizationPreferences setTimezone(TimeZone timezone) {
		this.timezone = timezone;
		explicitTimezone = true;
		return this;
	}
	/**
	 * @return timezone, either implicitly or explicitly set
	 */
	public TimeZone getTimezone() {
		return (TimeZone) timezone.clone(); // clone for safety
	}

	/**
	 * Set the date locale. If not null, overrides the locale priority list for all the date formats.
	 * @param dateLocale
	 * @return
	 */
	public GlobalizationPreferences setDateLocale(ULocale dateLocale) {
		this.dateLocale = dateLocale;
		return this;
	}
	/**
	 * @return date locale. Null if none was set.
	 */
	public ULocale getDateLocale() {
		return dateLocale;
	}
	
	/**
	 * Set an explicit date format. Overrides both the date locale, and the locale priority list
	 * for a particular combination of dateStyle and timeStyle. NONE should be used if for the style,
	 * where only the date or time format individually is being set.
	 * @param dateStyle
	 * @param timeStyle
	 * @param format
	 * @return this, for chaining
	 */
	public GlobalizationPreferences setDateFormat(int dateStyle, int timeStyle, DateFormat format) {
		dateFormats[dateStyle][timeStyle] = (DateFormat) format.clone(); // for safety
		return this;
	}
	
	/**
	 * Gets a date format according to the current settings. If there is an explicit (non-null) date/time
	 * format set, a copy of that is returned. Otherwise, if there is a non-null date locale, that is used.
	 * Otherwise, the language priority list is used. NONE should be used if for the style,
	 * where only the date or time format individually is being gotten.
	 * @param dateStyle
	 * @param timeStyle
	 * @return a DateFormat, according to the above description
	 */
	public DateFormat getDateFormat(int dateStyle, int timeStyle) {
		try {
			DateFormat result = dateFormats[dateStyle][timeStyle];
			if (result != null) {
				result = (DateFormat) result.clone(); // clone for safety
				result.setCalendar(calendar);
			} else {
				// In the case of date formats, we don't have to look at more than one
				// locale. May be different for other cases
				ULocale currentLocale = dateLocale != null ? dateLocale : (ULocale)locales.get(0);
				// TODO Make this one function.
				if (timeStyle == NONE) {
					result = DateFormat.getDateInstance(calendar, dateStyle, currentLocale);
				} else if (dateStyle == NONE) {
					result = DateFormat.getTimeInstance(calendar, timeStyle, currentLocale);
				} else {
					result = DateFormat.getDateTimeInstance(calendar, dateStyle, timeStyle, currentLocale);
				}
			}
			return result;
		} catch (RuntimeException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Cannot create DateFormat").initCause(e);	
		}
	}
	
	/**
	 * Get the display name for an ID: language, script, territory, currency, timezone...
	 * Uses the language priority list to do so.
	 * @param id language code, script code, ...
	 * @param type specifies the type of the ID: LANGUAGE, etc.
	 * @param length specifies the style (only currently for currency).
	 * @return the display name
	 */
	public String getDisplayName(String id, int type) {
		String result = id;
		for (Iterator it = locales.iterator(); it.hasNext();) {
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
				result =temp.getName(locale, type==CURRENCYID ? Currency.LONG_NAME : Currency.SYMBOL_NAME, new boolean[1]);
				// TODO, have method that doesn't take parameter. Add function to determine whether string is choice format.
				// TODO, have method that doesn't require us to create a currency
				break;
			case TIMEZONEID:
				SimpleDateFormat dtf = new SimpleDateFormat("vvvv",locale);
				dtf.setTimeZone(TimeZone.getTimeZone(id));
				result = dtf.format(new Date());
				// TODO, have method that doesn't require us to create a timezone
				// fix other hacks
				// hack for couldn't match
				if (badTimezone.reset(result).matches()) continue;
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
	// TODO remove need for this
	private static final Matcher badTimezone = Pattern.compile("[A-Z]{2}|.*\\s\\([A-Z]{2}\\)").matcher("");

	/**
	 * TBD
	 */
	public ULocale getNumberLocale() {
		return numberLocale;
	}
	
	/**
	 * TBD
	 */
	public GlobalizationPreferences setNumberLocale(ULocale numberLocale) {
		this.numberLocale = numberLocale;
		return this;
	}
	
	/**
	 * TBD
	 */
	public NumberFormat getNumberFormat(int style) {
		try {
			NumberFormat result = numberFormats[style];
			if (result != null) {
				result = (NumberFormat) result.clone(); // clone for safety
				if (style == CURRENCY) {
					result.setCurrency(currency);
				}
				return result; 
			}
			// In the case of date formats, we don't have to look at more than one
			// locale. May be different for other cases
			ULocale currentLocale = numberLocale != null ? numberLocale : (ULocale)locales.get(0);
			switch (style) {
			case NUMBER: return NumberFormat.getInstance(currentLocale);
			case SCIENTIFIC: return NumberFormat.getScientificInstance(currentLocale);
			case INTEGER: return NumberFormat.getIntegerInstance(currentLocale);
			case PERCENT: return NumberFormat.getPercentInstance(currentLocale);
			case CURRENCY: result = NumberFormat.getCurrencyInstance(currentLocale);
			result.setCurrency(currency);
			return result;
			}
		} catch (RuntimeException e) {}
		throw new IllegalArgumentException(); // fix later
	}
	
	/**
	 * TBD
	 */
	public GlobalizationPreferences setNumberFormat(int style, DateFormat format) {
		numberFormats[style] = (NumberFormat) format.clone(); // for safety
		return this;
	}
	
	
	// PRIVATES
	
	private ArrayList locales = new ArrayList();
	private String territory;
	private Currency currency;
	private TimeZone timezone;
	private Calendar calendar;
	private boolean explicitLocales;
	private boolean explicitTerritory;
	private boolean explicitCurrency;
	private boolean explicitTimezone;
	private boolean explicitCalendar;
	
	private ULocale dateLocale;
	private DateFormat[][] dateFormats = new DateFormat[NONE+1][NONE+1];
	private ULocale numberLocale;
	private NumberFormat[] numberFormats = new NumberFormat[NUMBER_LIMIT];
	
	{
		locales.add(ULocale.getDefault());
		setTerritoryFromLocales();
		setCurrencyFromTerritory();
		setTimeZoneFromTerritory();
		setCalendarFromTerritory();
	}
	
	// helper functions
	private void setTerritoryFromLocales() {
		if (explicitTerritory) return;
		// pass through locales to see if there is a territory.
		for (Iterator it = locales.iterator(); it.hasNext();) {
			ULocale locale = (ULocale)it.next();
			String temp = locale.getCountry();
			if (temp.length() == 0) {
				territory = temp;
				return;
			}
		}
		// if not, guess from the first language tag, or maybe from intersection of languages, eg nl + fr => BE
		// TODO fix using real data
		// for now, just use fixed value
		territory = "CN";
	}
	private void setCurrencyFromTerritory() {
		if (explicitCurrency) return;
		// TODO fix using real data
		// for now, just use what we've got
		currency = Currency.getInstance((ULocale)locales.get(0));
	}
	private void setTimeZoneFromTerritory() {
		if (explicitTimezone) return;
		// TODO fix using real data
		// for single-zone countries, pick that zone
		// for others, pick the most populous zone
		// for now, just use fixed value
		timezone = TimeZone.getTimeZone("Europe/London");
	}
	private void setCalendarFromTerritory() {
		if (explicitCalendar) return;
		// TODO fix using real data
		calendar = Calendar.getInstance((ULocale)locales.get(0));
	}
	
}