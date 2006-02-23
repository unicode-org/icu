//##header
/*
 *******************************************************************************
 * Copyright (C) 2004-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/

package com.ibm.icu.dev.test.format;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.GlobalizationPreferences;
import com.ibm.icu.util.ULocale;


public class GlobalizationPreferencesTest {
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
		PrintStream out = System.out;
		//PrintWriter out = BagFormatter.openUTF8Writer("c:/", "tempFile.txt");
		try {
			Date now = new Date();
			
			GlobalizationPreferences lPreferences = new GlobalizationPreferences();

			out.println("Samples from Globalization Preferences prototype");
			out.println("\tWarning: some of this is just mockup -- real data will be accessed later.");
			out.println();

//#ifndef FOUNDATION
			out.println("Check defaulting");
			String[] localeList = {"fr_BE;q=0.5,de", "fr_BE,de", "fr", "en_NZ", "en", "en-TH", "zh-Hant", "zh-MO", "zh", "it", "as", "haw", "ar-EG", "ar", "qqq"};
			for (int i = 0; i < localeList.length; ++i) {
				lPreferences.setLocales(localeList[i]);
				out.println("\tdefaults for: \t" + localeList[i] + "\t"
						+ lPreferences.getLocales()
						+ ", \t" + lPreferences.getTerritory()
						+ ", \t" + lPreferences.getCurrency()
						+ ", \t" + lPreferences.getCalendar().getClass()
						+ ", \t" + lPreferences.getTimeZone().getID()
				);
			}
			
			out.println();
//#endif

			out.println("Date Formatting");
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE).format(now));
			
			out.println("setting locale to Germany");
			lPreferences.setLocale(ULocale.GERMANY);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE).format(now));
			
			out.println("setting date locale to France");
			lPreferences.setDateLocale(ULocale.FRANCE);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE).format(now));
			
			out.println("setting explicit pattern");
			lPreferences.setDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE, "GGG yyyy+MMM+DD vvvv");
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE).format(now));
			
			out.println("setting date format to yyyy-MMM-dd (Italy)");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd",ULocale.ITALY);
			lPreferences.setDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE, sdf);
			out.println("\tdate: \t" + lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE).format(now));
			
			out.println();
			out.println("Various named date fields");
			SimpleDateFormat df = (SimpleDateFormat)lPreferences.getDateFormat(DateFormat.FULL, GlobalizationPreferences.NONE);
			DateFormatSymbols dfs = df.getDateFormatSymbols();
			
			for (int context = dfs.FORMAT; context <= dfs.STANDALONE; ++context) {
				out.println("context: " + ContextNames[context-dfs.FORMAT]);
				for (int width = dfs.ABBREVIATED; width <= dfs.NARROW; ++width) {
					out.println("\twidth: " + WidthNames[width-dfs.ABBREVIATED]);
					out.println("\t\tgetAmPmStrings:\t" + Arrays.asList(dfs.getAmPmStrings()));
					out.println("\t\tgetEras\t" + Arrays.asList((width > 0 ? dfs.getEraNames() : dfs.getEras())));
					out.println("\t\tgetMonths:\t" + Arrays.asList(dfs.getMonths(context, width)));
					out.println("\t\tgetWeekdays:\t" + Arrays.asList(dfs.getWeekdays(context, width)));
				}
			}
			
			// now show currencies
			out.println();
			out.println("Currency Formatting");
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(GlobalizationPreferences.CURRENCY).format(1234.567));
			
			out.println("setting number locale to Canada");
			lPreferences.setNumberLocale(ULocale.CANADA);
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(GlobalizationPreferences.CURRENCY).format(1234.567));
			
			out.println("setting currency to INR"); 
			lPreferences.setCurrency(Currency.getInstance("INR"));
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(GlobalizationPreferences.CURRENCY).format(1234.567));
			
			out.println("setting number locale to Hindi-India"); 
			lPreferences.setNumberLocale(new ULocale("hi-IN"));
			out.println("\tcurrency: \t" + lPreferences.getNumberFormat(GlobalizationPreferences.CURRENCY).format(1234.567));
			
			out.println();
			out.println("Comparison");
			out.println("setting number locale to Germany");
			lPreferences.setLocale(ULocale.GERMANY);
			out.println("\tcompare: \u00e4 & z \t" + lPreferences.getCollator().compare("\u00e4", "z"));

			out.println("setting number locale to Swedish");
			lPreferences.setLocale(new ULocale("sv"));
			out.println("\tcompare: \u00e4 & z \t" + lPreferences.getCollator().compare("\u00e4", "z"));

			// now try a fallback within locales
			out.println();
			out.println("Display Names");
			lPreferences.setLocales(new ULocale[]{new ULocale("as"),new ULocale("pl"),new ULocale("fr")});
			out.println("Trying fallback for multiple locales: " + lPreferences.getLocales());
			String[][] testItems = {
					{GlobalizationPreferences.LOCALEID+"", "as_FR", "en_RU","haw_CA","se_Cyrl_AT"},
					{GlobalizationPreferences.LANGUAGEID+"", "as", "en","haw","se","kok"},
					{GlobalizationPreferences.SCRIPTID+"", "Arab", "Cyrl", "Hant"},
					{GlobalizationPreferences.TERRITORYID+"", "US", "FR", "AU", "RU","IN"},
					{GlobalizationPreferences.VARIANTID+"","REVISED"},
					{GlobalizationPreferences.KEYWORDID+"","calendar", "collation", "currency"},
					{GlobalizationPreferences.KEYWORD_VALUEID+"", "calendar=buddhist", "calendar=gregorian", 
						"collation=phonebook", "collation=traditional"},
					{GlobalizationPreferences.CURRENCYID+"", "USD", "GBP", "EUR", "JPY","INR"},
					{GlobalizationPreferences.CURRENCY_SYMBOLID+"", "USD", "GBP", "EUR", "JPY","INR"},
					{GlobalizationPreferences.TIMEZONEID+"", "America/Mexico_City", "Asia/Shanghai", "Europe/London", "Europe/Berlin"},
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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.close();
			System.out.println("done");
		}
	}
}
