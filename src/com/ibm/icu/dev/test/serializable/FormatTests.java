/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Date;
import java.util.Locale;

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
import com.ibm.icu.util.TimeZone;
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
        
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DecimalFormatSymbols dfs[] = new DecimalFormatSymbols[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                dfs[i] = new DecimalFormatSymbols(locales[i]);
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

            // To absorb potential time zone rule differences...
            TimeZone tza = dfa.getTimeZone();
            TimeZone tzb = dfb.getTimeZone();
            String tzid = tza.getID();
            if (!tzid.equals(tzb.getID())) {
                return false;
            }
            TimeZone tz = TimeZone.getTimeZone(tzid);
            dfa.setTimeZone(tz);
            dfb.setTimeZone(tz);
            return dfa.format(date).equals(dfb.format(date));
        }
        
    }
    
    public static class DateFormatSymbolsHandler implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            DateFormatSymbols dfs[] = new DateFormatSymbols[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                dfs[i] = new DateFormatSymbols(GregorianCalendar.class, ULocale.forLocale(locales[i]));
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
                cdfs[i] = new ChineseDateFormatSymbols(locales[i]);
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
