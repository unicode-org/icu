/*
 *******************************************************************************
 * Copyright (C) 1996-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Date;
import java.util.Locale;
import java.util.HashMap;

import com.ibm.icu.text.ChineseDateFormat;
import com.ibm.icu.text.ChineseDateFormatSymbols;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.ULocale;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FormatTests
{

    public static class NumberFormatHandler implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            NumberFormat formats[] = {
                NumberFormat.getInstance(Locale.US),
                NumberFormat.getCurrencyInstance(Locale.US),
                NumberFormat.getPercentInstance(Locale.US),
                NumberFormat.getScientificInstance(Locale.US)
               
            };
            
            return formats;
        }

        public boolean hasSameBehavior(Object a, Object b)
        {
            NumberFormat format_a = (NumberFormat) a;
            NumberFormat format_b = (NumberFormat) b;
            double number = 1234.56;
            
            return format_a.format(number).equals(format_b.format(number));
        }
    }
    
    public static class DecimalFormatHandler extends NumberFormatHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DecimalFormat formats[] = new DecimalFormat[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                formats[i] = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(locales[i]));
            }
            
            return formats;
        }
    }
    
    public static class RuleBasedNumberFormatHandler extends NumberFormatHandler
    {
        int types[] = {RuleBasedNumberFormat.SPELLOUT, RuleBasedNumberFormat.ORDINAL, RuleBasedNumberFormat.DURATION};
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            RuleBasedNumberFormat formats[] = new RuleBasedNumberFormat[types.length * locales.length];
            int i = 0;
            
            for (int t = 0; t < types.length; t += 1) {
                for (int l = 0; l < locales.length; l += 1) {
                    formats[i++] = new RuleBasedNumberFormat(locales[l], types[t]);
                }
            }
            
            return formats;
        }
    }
    
    public static class DecimalFormatSymbolsHandler implements SerializableTest.Handler
    {
        /*
         * The serialized form of a normally created DecimalFormatSymbols object
         * will have locale-specific data in it that might change from one version
         * of ICU4J to another. To guard against this, we store the following canned
         * data into the test objects we create.
         */
        static HashMap cannedData = new HashMap();
        
        static String en_CA_StringSymbols[] = {
            "$", 
            "E", 
            "\u221E", 
            "CAD", 
            "\uFFFD", 
        };

        static String fr_CA_StringSymbols[] = {
            "$", 
            "E", 
            "\u221E", 
            "CAD", 
            "\uFFFD", 
        };

        static String zh_CN_StringSymbols[] = {
            "\uFFE5", 
            "E", 
            "\u221E", 
            "CNY", 
            "\uFFFD", 
        };

        static String zh_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String en_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String fr_FR_StringSymbols[] = {
            "\u20AC", 
            "E", 
            "\u221E", 
            "EUR", 
            "\uFFFD", 
        };

        static String fr_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String de_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String de_DE_StringSymbols[] = {
            "\u20AC", 
            "E", 
            "\u221E", 
            "EUR", 
            "\uFFFD", 
        };

        static String it_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String it_IT_StringSymbols[] = {
            "\u20AC", 
            "E", 
            "\u221E", 
            "EUR", 
            "\uFFFD", 
        };

        static String ja_JP_StringSymbols[] = {
            "\uFFE5", 
            "E", 
            "\u221E", 
            "JPY", 
            "\uFFFD", 
        };

        static String ja_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String ko_KR_StringSymbols[] = {
            "\uFFE6", 
            "E", 
            "\u221E", 
            "KRW", 
            "\uFFFD", 
        };

        static String ko_StringSymbols[] = {
            "\u00A4", 
            "E", 
            "\u221E", 
            "XXX", 
            "\uFFFD", 
        };

        static String zh_Hans_CN_StringSymbols[] = {
            "\uFFE5", 
            "E", 
            "\u221E", 
            "CNY", 
            "\uFFFD", 
        };

        static String zh_Hant_TW_StringSymbols[] = {
            "NT$", 
            "E", 
            "\u221E", 
            "TWD", 
            "\uFFFD", 
        };

        static String zh_TW_StringSymbols[] = {
            "NT$", 
            "E", 
            "\u221E", 
            "TWD", 
            "\uFFFD", 
        };

        static String en_GB_StringSymbols[] = {
            "\u00A3", 
            "E", 
            "\u221E", 
            "GBP", 
            "\uFFFD", 
        };

        static String en_US_StringSymbols[] = {
            "$", 
            "E", 
            "\u221E", 
            "USD", 
            "\uFFFD", 
        };
        
        {
            cannedData.put("en_CA",      en_CA_StringSymbols);
            cannedData.put("fr_CA",      fr_CA_StringSymbols);
            cannedData.put("zh_CN",      zh_CN_StringSymbols);
            cannedData.put("zh",         zh_StringSymbols);
            cannedData.put("en",         en_StringSymbols);
            cannedData.put("fr_FR",      fr_FR_StringSymbols);
            cannedData.put("fr",         fr_StringSymbols);
            cannedData.put("de",         de_StringSymbols);
            cannedData.put("de_DE",      de_DE_StringSymbols);
            cannedData.put("it",         it_StringSymbols);
            cannedData.put("it_IT",      it_IT_StringSymbols);
            cannedData.put("ja_JP",      ja_JP_StringSymbols);
            cannedData.put("ja",         ja_StringSymbols);
            cannedData.put("ko_KR",      ko_KR_StringSymbols);
            cannedData.put("ko",         ko_StringSymbols);
            cannedData.put("zh_Hans_CN", zh_Hans_CN_StringSymbols);
            cannedData.put("zh_Hant_TW", zh_Hant_TW_StringSymbols);
            cannedData.put("zh_TW",      zh_TW_StringSymbols);
            cannedData.put("en_GB",      en_GB_StringSymbols);
            cannedData.put("en_US",      en_US_StringSymbols);
        }
        
        private char[] getCharSymbols(DecimalFormatSymbols dfs)
        {
            char symbols[] = {
                dfs.getDecimalSeparator(),
                dfs.getDigit(),
                dfs.getGroupingSeparator(),
                dfs.getMinusSign(),
                dfs.getMonetaryDecimalSeparator(),
                dfs.getPadEscape(),
                dfs.getPatternSeparator(),
                dfs.getPercent(),
                dfs.getPerMill(),
                dfs.getPlusSign(),
                dfs.getSignificantDigit(),
                dfs.getZeroDigit()
            };
            
            return symbols;
        }
        
        private String[] getStringSymbols(DecimalFormatSymbols dfs)
        {
            String symbols[] = {
                dfs.getCurrencySymbol(),
                dfs.getExponentSeparator(),
                dfs.getInfinity(),
                dfs.getInternationalCurrencySymbol(),
                dfs.getNaN()
            };
            
            return symbols;
        }
        
        private void setStringSymbols(DecimalFormatSymbols dfs, String symbols[])
        {
            dfs.setCurrencySymbol(symbols[0]);
            dfs.setExponentSeparator(symbols[1]);
            dfs.setInfinity(symbols[2]);
            dfs.setInternationalCurrencySymbol(symbols[3]);
            dfs.setNaN(symbols[4]);
        }
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DecimalFormatSymbols dfs[] = new DecimalFormatSymbols[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                ULocale uloc = ULocale.forLocale(locales[i]);

                dfs[i] = new DecimalFormatSymbols(uloc);
                setStringSymbols(dfs[i], (String[]) cannedData.get(uloc.toString()));
            }
            
            return dfs;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            DecimalFormatSymbols dfs_a = (DecimalFormatSymbols) a;
            DecimalFormatSymbols dfs_b = (DecimalFormatSymbols) b;
            String strings_a[] = getStringSymbols(dfs_a);
            String strings_b[] = getStringSymbols(dfs_b);
            char chars_a[] = getCharSymbols(dfs_a);
            char chars_b[] = getCharSymbols(dfs_b);

            return SerializableTest.compareStrings(strings_a, strings_b) && SerializableTest.compareChars(chars_a, chars_b);
        }
    }
    
    public static class MessageFormatHandler implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            MessageFormat formats[] = {new MessageFormat("pattern{0}")};
            
            return formats;
        }

        public boolean hasSameBehavior(Object a, Object b)
        {
            MessageFormat mfa = (MessageFormat) a;
            MessageFormat mfb = (MessageFormat) b;
            Object arguments[] = {new Integer(123456)};
            
            return mfa.format(arguments) != mfb.format(arguments);
        }
    }
    
    public static class DateFormatHandler implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DateFormat formats[] = new DateFormat[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                formats[i] = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locales[i]);
            }
            
            return formats;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            DateFormat dfa = (DateFormat) a;
            DateFormat dfb = (DateFormat) b;
            Date date = new Date(System.currentTimeMillis());
            String sfa = dfa.format(date);
            String sfb = dfa.format(date);
            
           return sfa.equals(sfb);
        }
        
    }
    
    public static class DateFormatSymbolsHandler implements SerializableTest.Handler
    {
        /*
         * The serialized form of a normally created DateFormatSymbols object
         * will have locale-specific data in it that might change from one version
         * of ICU4J to another. To guard against this, we store the following canned
         * data into the test objects we create.
         */
        static HashMap cannedData = new HashMap();

        static String en_CA_MonthNames[] = {
            "January", 
            "February", 
            "March", 
            "April", 
            "May", 
            "June", 
            "July", 
            "August", 
            "September", 
            "October", 
            "November", 
            "December", 
        };

        static String fr_CA_MonthNames[] = {
            "janvier", 
            "f\u00E9vrier", 
            "mars", 
            "avril", 
            "mai", 
            "juin", 
            "juillet", 
            "ao\u00FBt", 
            "septembre", 
            "octobre", 
            "novembre", 
            "d\u00E9cembre", 
        };

        static String zh_Hans_CN_MonthNames[] = {
            "\u4E00\u6708", 
            "\u4E8C\u6708", 
            "\u4E09\u6708", 
            "\u56DB\u6708", 
            "\u4E94\u6708", 
            "\u516D\u6708", 
            "\u4E03\u6708", 
            "\u516B\u6708", 
            "\u4E5D\u6708", 
            "\u5341\u6708", 
            "\u5341\u4E00\u6708", 
            "\u5341\u4E8C\u6708", 
        };

        static String zh_CN_MonthNames[] = {
            "\u4E00\u6708", 
            "\u4E8C\u6708", 
            "\u4E09\u6708", 
            "\u56DB\u6708", 
            "\u4E94\u6708", 
            "\u516D\u6708", 
            "\u4E03\u6708", 
            "\u516B\u6708", 
            "\u4E5D\u6708", 
            "\u5341\u6708", 
            "\u5341\u4E00\u6708", 
            "\u5341\u4E8C\u6708", 
        };

        static String zh_MonthNames[] = {
            "\u4E00\u6708", 
            "\u4E8C\u6708", 
            "\u4E09\u6708", 
            "\u56DB\u6708", 
            "\u4E94\u6708", 
            "\u516D\u6708", 
            "\u4E03\u6708", 
            "\u516B\u6708", 
            "\u4E5D\u6708", 
            "\u5341\u6708", 
            "\u5341\u4E00\u6708", 
            "\u5341\u4E8C\u6708", 
        };

        static String en_MonthNames[] = {
            "January", 
            "February", 
            "March", 
            "April", 
            "May", 
            "June", 
            "July", 
            "August", 
            "September", 
            "October", 
            "November", 
            "December", 
        };

        static String fr_FR_MonthNames[] = {
            "janvier", 
            "f\u00E9vrier", 
            "mars", 
            "avril", 
            "mai", 
            "juin", 
            "juillet", 
            "ao\u00FBt", 
            "septembre", 
            "octobre", 
            "novembre", 
            "d\u00E9cembre", 
        };

        static String fr_MonthNames[] = {
            "janvier", 
            "f\u00E9vrier", 
            "mars", 
            "avril", 
            "mai", 
            "juin", 
            "juillet", 
            "ao\u00FBt", 
            "septembre", 
            "octobre", 
            "novembre", 
            "d\u00E9cembre", 
        };

        static String de_MonthNames[] = {
            "Januar", 
            "Februar", 
            "M\u00E4rz", 
            "April", 
            "Mai", 
            "Juni", 
            "Juli", 
            "August", 
            "September", 
            "Oktober", 
            "November", 
            "Dezember", 
        };

        static String de_DE_MonthNames[] = {
            "Januar", 
            "Februar", 
            "M\u00E4rz", 
            "April", 
            "Mai", 
            "Juni", 
            "Juli", 
            "August", 
            "September", 
            "Oktober", 
            "November", 
            "Dezember", 
        };

        static String it_MonthNames[] = {
            "gennaio", 
            "febbraio", 
            "marzo", 
            "aprile", 
            "maggio", 
            "giugno", 
            "luglio", 
            "agosto", 
            "settembre", 
            "ottobre", 
            "novembre", 
            "dicembre", 
        };

        static String it_IT_MonthNames[] = {
            "gennaio", 
            "febbraio", 
            "marzo", 
            "aprile", 
            "maggio", 
            "giugno", 
            "luglio", 
            "agosto", 
            "settembre", 
            "ottobre", 
            "novembre", 
            "dicembre", 
        };

        static String ja_JP_MonthNames[] = {
            "1\u6708", 
            "2\u6708", 
            "3\u6708", 
            "4\u6708", 
            "5\u6708", 
            "6\u6708", 
            "7\u6708", 
            "8\u6708", 
            "9\u6708", 
            "10\u6708", 
            "11\u6708", 
            "12\u6708", 
        };

        static String ja_MonthNames[] = {
            "1\u6708", 
            "2\u6708", 
            "3\u6708", 
            "4\u6708", 
            "5\u6708", 
            "6\u6708", 
            "7\u6708", 
            "8\u6708", 
            "9\u6708", 
            "10\u6708", 
            "11\u6708", 
            "12\u6708", 
        };

        static String ko_KR_MonthNames[] = {
            "1\uC6D4", 
            "2\uC6D4", 
            "3\uC6D4", 
            "4\uC6D4", 
            "5\uC6D4", 
            "6\uC6D4", 
            "7\uC6D4", 
            "8\uC6D4", 
            "9\uC6D4", 
            "10\uC6D4", 
            "11\uC6D4", 
            "12\uC6D4", 
        };

        static String ko_MonthNames[] = {
            "1\uC6D4", 
            "2\uC6D4", 
            "3\uC6D4", 
            "4\uC6D4", 
            "5\uC6D4", 
            "6\uC6D4", 
            "7\uC6D4", 
            "8\uC6D4", 
            "9\uC6D4", 
            "10\uC6D4", 
            "11\uC6D4", 
            "12\uC6D4", 
        };

        static String zh_Hant_TW_MonthNames[] = {
            "\u4E00\u6708", 
            "\u4E8C\u6708", 
            "\u4E09\u6708", 
            "\u56DB\u6708", 
            "\u4E94\u6708", 
            "\u516D\u6708", 
            "\u4E03\u6708", 
            "\u516B\u6708", 
            "\u4E5D\u6708", 
            "\u5341\u6708", 
            "\u5341\u4E00\u6708", 
            "\u5341\u4E8C\u6708", 
        };

        static String zh_TW_MonthNames[] = {
            "\u4E00\u6708", 
            "\u4E8C\u6708", 
            "\u4E09\u6708", 
            "\u56DB\u6708", 
            "\u4E94\u6708", 
            "\u516D\u6708", 
            "\u4E03\u6708", 
            "\u516B\u6708", 
            "\u4E5D\u6708", 
            "\u5341\u6708", 
            "\u5341\u4E00\u6708", 
            "\u5341\u4E8C\u6708", 
            };

        static String en_GB_MonthNames[] = {
            "January", 
            "February", 
            "March", 
            "April", 
            "May", 
            "June", 
            "July", 
            "August", 
            "September", 
            "October", 
            "November", 
            "December", 
        };

        static String en_US_MonthNames[] = {
            "January", 
            "February", 
            "March", 
            "April", 
            "May", 
            "June", 
            "July", 
            "August", 
            "September", 
            "October", 
            "November", 
            "December", 
        };

        {
            cannedData.put("en_CA",      en_CA_MonthNames);
            cannedData.put("fr_CA",      fr_CA_MonthNames);
            cannedData.put("zh_Hans_CN", zh_Hans_CN_MonthNames);
            cannedData.put("zh_CN",      zh_CN_MonthNames);
            cannedData.put("zh",         zh_MonthNames);
            cannedData.put("en",         en_MonthNames);
            cannedData.put("fr_FR",      fr_FR_MonthNames);
            cannedData.put("fr",         fr_MonthNames);
            cannedData.put("de",         de_MonthNames);
            cannedData.put("de_DE",      de_DE_MonthNames);
            cannedData.put("it",         it_MonthNames);
            cannedData.put("it_IT",      it_IT_MonthNames);
            cannedData.put("ja_JP",      ja_JP_MonthNames);
            cannedData.put("ja",         ja_MonthNames);
            cannedData.put("ko_KR",      ko_KR_MonthNames);
            cannedData.put("ko",         ko_MonthNames);
            cannedData.put("zh_Hant_TW", zh_Hant_TW_MonthNames);
            cannedData.put("zh_TW",      zh_TW_MonthNames);
            cannedData.put("en_GB",      en_GB_MonthNames);
            cannedData.put("en_US",      en_US_MonthNames);
        }
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DateFormatSymbols dfs[] = new DateFormatSymbols[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                ULocale uloc = ULocale.forLocale(locales[i]);
                
                dfs[i] = new DateFormatSymbols(GregorianCalendar.class, uloc);
                dfs[i].setMonths((String[]) cannedData.get(uloc.toString()));
            }
            
            return dfs;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            DateFormatSymbols dfs_a = (DateFormatSymbols) a;
            DateFormatSymbols dfs_b = (DateFormatSymbols) b;
            String months_a[] = dfs_a.getMonths();
            String months_b[] = dfs_b.getMonths();
            
            return SerializableTest.compareStrings(months_a, months_b);
        }
    }
    
    public static class SimpleDateFormatHandler extends DateFormatHandler
    {
        String patterns[] = {
            "EEEE, yyyy MMMM dd",
            "yyyy MMMM d",
            "yyyy MMM d",
            "yy/MM/dd"
        };
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            SimpleDateFormat dateFormats[] = new SimpleDateFormat[patterns.length * locales.length];
            int i = 0;
            
            for (int p = 0; p < patterns.length; p += 1) {
                for (int l = 0; l < locales.length; l += 1) {
                    dateFormats[i++] = new SimpleDateFormat(patterns[p], ULocale.forLocale(locales[l]));
                }
            }
            
            return dateFormats;
        }
    }
    
    public static class ChineseDateFormatHandler extends DateFormatHandler
    {
        String patterns[] = {
            "EEEE y'x'G-Ml-d",
            "y'x'G-Ml-d",
            "y'x'G-Ml-d",
            "y'x'G-Ml-d"
        };
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            ChineseDateFormat dateFormats[] = new ChineseDateFormat[patterns.length * locales.length];
            int i = 0;
            
            for (int p = 0; p < patterns.length; p += 1) {
                for (int l = 0; l < locales.length; l += 1) {
                    ULocale locale = new ULocale(locales[l].toString() + "@calendar=chinese");
                    
                    dateFormats[i++] = new ChineseDateFormat(patterns[p], locale);
                }
            }
            
            return dateFormats;
        }
    }
    
    public static class ChineseDateFormatSymbolsHandler extends DateFormatSymbolsHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            ChineseDateFormatSymbols cdfs[] = new ChineseDateFormatSymbols[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                ULocale uloc = ULocale.forLocale(locales[i]);
                
                cdfs[i] = new ChineseDateFormatSymbols(uloc);
                cdfs[i].setMonths((String[]) cannedData.get(uloc.toString()));
            }
            
            return cdfs;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            if (! super.hasSameBehavior(a, b)) {
                return false;
            }
            
            ChineseDateFormatSymbols cdfs_a = (ChineseDateFormatSymbols) a;
            ChineseDateFormatSymbols cdfs_b = (ChineseDateFormatSymbols) b;
            
            return cdfs_a.getLeapMonth(0).equals(cdfs_b.getLeapMonth(0)) &&
                   cdfs_a.getLeapMonth(1).equals(cdfs_b.getLeapMonth(1));
        }
    }
    
    public static void main(String[] args)
    {
        // nothing needed...
    }
}
