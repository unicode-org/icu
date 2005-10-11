/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SerializableTest
{
    public interface Handler
    {
        public Object[] getTestObjects();
        
        public boolean hasSameBehavior(Object a, Object b);
    }
    
    public static Handler getHandler(String className)
    {
        return (Handler) map.get(className);
    }
    
    private static class TimeZoneTest implements Handler
    {
        String[] ZONES = { "GMT", "MET", "IST" };

        public Object[] getTestObjects()
        {
            TimeZone zones[] = new TimeZone[ZONES.length];
            
            for(int z = 0; z < ZONES.length; z += 1) {
                zones[z] = TimeZone.getTimeZone(ZONES[z]);
            }
            
            return zones;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            TimeZone zone_a = (TimeZone) a;
            TimeZone zone_b = (TimeZone) b;
            
            return zone_a.getDisplayName().equals(zone_b.getDisplayName()) && zone_a.hasSameRules(zone_b);
        }
    }
    
    private static Locale locales[] = {
        Locale.CANADA, Locale.CANADA_FRENCH, Locale.CHINA, 
        Locale.CHINESE, Locale.ENGLISH, Locale.FRANCE, Locale.FRENCH, 
        Locale.GERMAN, Locale.GERMANY, Locale.ITALIAN, Locale.ITALY, 
        Locale.JAPAN, Locale.JAPANESE, Locale.KOREA, Locale.KOREAN, 
        Locale.PRC, Locale.SIMPLIFIED_CHINESE, Locale.TAIWAN, 
        Locale.TRADITIONAL_CHINESE, Locale.UK, Locale.US
    };
    
    private static Locale places[] = {
        Locale.CANADA, Locale.CANADA_FRENCH, Locale.CHINA, 
        Locale.FRANCE, Locale.GERMANY, Locale.ITALY, 
        Locale.JAPAN, Locale.KOREA, Locale.PRC, Locale.TAIWAN, 
        Locale.UK, Locale.US
    };
    
    public static Locale[] getLocales()
    {
        return locales;
    }
    
    public static boolean compareStrings(String a[], String b[])
    {
        if (a.length != b.length) {
            return false;
        }
        
        for (int i = 0; i < a.length; i += 1) {
            if (! a[i].equals(b[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean compareChars(char a[], char b[])
    {
        if (a.length != b.length) {
            return false;
        }
        
        for (int i = 0; i < a.length; i += 1) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        
        return true;
    }

    private static class SimpleTimeZoneTest extends TimeZoneTest
    {
        public Object[] getTestObjects()
        {
            SimpleTimeZone simpleTimeZones[] = new SimpleTimeZone[6];
            
            simpleTimeZones[0] = new SimpleTimeZone(32400000, "MyTimeZone");
            
            simpleTimeZones[1] = new SimpleTimeZone(32400000, "Asia/Tokyo");
            
            simpleTimeZones[2] = new SimpleTimeZone(32400000, "Asia/Tokyo");
            simpleTimeZones[2].setRawOffset(0);
            
            simpleTimeZones[3] = new SimpleTimeZone(32400000, "Asia/Tokyo");
            simpleTimeZones[3].setStartYear(100);
            
            simpleTimeZones[4] = new SimpleTimeZone(32400000, "Asia/Tokyo");
            simpleTimeZones[4].setStartYear(1000);
            simpleTimeZones[4].setDSTSavings(1800000);
            simpleTimeZones[4].setStartRule(3, 4, 180000);
            simpleTimeZones[4].setEndRule(6, 3, 4, 360000);
            
            simpleTimeZones[5] = new SimpleTimeZone(32400000, "Asia/Tokyo");
            simpleTimeZones[5].setStartRule(2, 3, 4, 360000);
            simpleTimeZones[5].setEndRule(6, 3, 4, 360000);
            
            return simpleTimeZones;
        }
    }
    
    private static class ULocaleTest implements Handler
    {
        public Object[] getTestObjects()
        {
            ULocale uLocales[] = new ULocale[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                uLocales[i] = ULocale.forLocale(locales[i]);
            }
            
            return uLocales;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            ULocale uloc_a = (ULocale) a;
            ULocale uloc_b = (ULocale) b;
            
            return uloc_a.getName().equals(uloc_b.getName());
        }
    }
    
    private static class CurrencyTest implements Handler
    {
        public Object[] getTestObjects()
        {
            Currency currencies[] = new Currency[places.length];
            
            for (int i = 0; i < places.length; i += 1) {
                currencies[i] = Currency.getInstance(places[i]);
            }
            
            return currencies;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            Currency curr_a = (Currency) a;
            Currency curr_b = (Currency) b;
            
            return curr_a.getCurrencyCode().equals(curr_b.getCurrencyCode());
            
        }
    }
    
    private static class OlsonTimeZoneTest implements Handler
    {
        String zoneIDs[] = {
            "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
            "America/Chicago", "America/New_York", "Africa/Cairo", "Africa/Addis_Ababa", "Africa/Dar_es_Salaam",
            "Africa/Freetown", "Africa/Johannesburg", "Africa/Nairobi", "Asia/Bangkok", "Asia/Baghdad",
            "Asia/Calcutta", "Asia/Hong_Kong", "Asia/Jakarta", "Asia/Jerusalem", "Asia/Manila", "Asia/Tokyo",
            "Europe/Amsterdam", "Europe/Athens", "Europe/Berlin", "Europe/London", "Europe/Malta", "Europe/Moscow",
            "Europe/Paris", "Europe/Rome"
        };
        
        public Object[] getTestObjects()
        {
            OlsonTimeZone timeZones[] = new OlsonTimeZone[zoneIDs.length];
            
            for (int i = 0; i < zoneIDs.length; i += 1) {
                timeZones[i] = new OlsonTimeZone(zoneIDs[i]);
            }
            
            return timeZones;
                
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            OlsonTimeZone otz_a = (OlsonTimeZone) a;
            OlsonTimeZone otz_b = (OlsonTimeZone) b;
            long now = System.currentTimeMillis();
            int a_offsets[] = {0, 0};
            int b_offsets[] = {0, 0};
            
            otz_a.getOffset(now, false, a_offsets);
            otz_b.getOffset(now, false, b_offsets);
            
            return a_offsets[0] == b_offsets[0] && a_offsets[1] == b_offsets[1];
        }
    }
    
    private static class BigDecimalTest implements Handler
    {
        String values[] = {
            "1234567890.",
            "123456789.0",
            "12345678.90",
            "1234567.890",
            "123456.7890",
            "12345.67890",
            "1234.567890",
            "123.4567890",
            "12.34567890",
            "1.234567890",
            ".1234567890"};
        
        public Object[] getTestObjects()
        {
            BigDecimal bds[] = new BigDecimal[values.length];
            
            for (int i = 0; i < values.length; i += 1) {
                bds[i] = new BigDecimal(values[i]);
            }
            
            return bds;
        }
        
        public boolean hasSameBehavior(Object a, Object b) {
            BigDecimal bda = (BigDecimal) a;
            BigDecimal bdb = (BigDecimal) b;
            
            return bda.toString().equals(bdb.toString());
        }
    }
    
    private static class MathContextTest implements Handler
    {
        int forms[] = {MathContext.PLAIN, MathContext.ENGINEERING, MathContext.SCIENTIFIC};
        int rounds[] = {
            MathContext.ROUND_CEILING, MathContext.ROUND_DOWN, MathContext.ROUND_FLOOR,
            MathContext.ROUND_HALF_DOWN, MathContext.ROUND_HALF_EVEN, MathContext.ROUND_HALF_UP,
            MathContext.ROUND_UNNECESSARY, MathContext.ROUND_UP};

        public Object[] getTestObjects()
        {
            int objectCount = forms.length * rounds.length;
            MathContext contexts[] = new MathContext[objectCount];
            int i = 0;
            
            for (int f = 0; f < forms.length; f += 1) {
                for (int r = 0; r < rounds.length; r += 1) {
                    int digits = f * r;
                    boolean lostDigits = (r & 1) != 0;
                    
                    contexts[i++] = new MathContext(digits, forms[f], lostDigits, rounds[r]);
                }
            }
            
            return contexts;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            MathContext mca = (MathContext) a;
            MathContext mcb = (MathContext) b;
            
            return mca.toString().equals(mcb.toString());
        }
    }
    
    private static HashMap map = new HashMap();
    
    static {
        map.put("com.ibm.icu.util.TimeZone", new TimeZoneTest());
        map.put("com.ibm.icu.util.SimpleTimeZone", new SimpleTimeZoneTest());
        map.put("com.ibm.icu.util.ULocale", new ULocaleTest());
        map.put("com.ibm.icu.util.Currency", new CurrencyTest());
        map.put("com.ibm.icu.impl.OlsonTimeZone", new OlsonTimeZoneTest());
        map.put("com.ibm.icu.math.BigDecimal", new BigDecimalTest());
        map.put("com.ibm.icu.math.MathContext", new MathContextTest());
        
        map.put("com.ibm.icu.text.NumberFormat", new FormatTests.NumberFormatTest());
        map.put("com.ibm.icu.text.DecimalFormat", new FormatTests.DecimalFormatTest());
        map.put("com.ibm.icu.text.RuleBasedNumberFormat", new FormatTests.RuleBasedNumberFormatTest());
        map.put("com.ibm.icu.text.DecimalFormatSymbols", new FormatTests.DecimalFormatSymbolsTest());
        map.put("com.ibm.icu.text.MessageFormat", new FormatTests.MessageFormatTest());
        map.put("com.ibm.icu.text.DateFormat", new FormatTests.DateFormatTest());
        map.put("com.ibm.icu.text.DateFormatSymbols", new FormatTests.DateFormatSymbolsTest());
        map.put("com.ibm.icu.text.SimpleDateFormat", new FormatTests.SimpleDateFormatTest());
        map.put("com.ibm.icu.text.ChineseDateFormat", new FormatTests.ChineseDateFormatTest());
        map.put("com.ibm.icu.text.ChineseDateFormatSymbols", new FormatTests.ChineseDateFormatSymbolsTest());

        map.put("com.ibm.icu.util.Calendar", new CalendarTests.CalendarTest());
        map.put("com.ibm.icu.util.BuddhistCalendar", new CalendarTests.BuddhistCalendarTest());
        map.put("com.ibm.icu.util.ChineseCalendar", new CalendarTests.ChineseCalendarTest());
        map.put("com.ibm.icu.util.CopticCalendar", new CalendarTests.CopticCalendarTest());
        map.put("com.ibm.icu.util.EthiopicCalendar", new CalendarTests.EthiopicCalendarTest());
        map.put("com.ibm.icu.util.GregorianCalendar", new CalendarTests.GregorianCalendarTest());
        map.put("com.ibm.icu.util.HebrewCalendar", new CalendarTests.HebrewCalendarTest());
        map.put("com.ibm.icu.util.IslamicCalendar", new CalendarTests.IslamicCalendarTest());
        map.put("com.ibm.icu.util.JapaneseCalendar", new CalendarTests.JapaneseCalendarTest());
        
        map.put("com.ibm.icu.text.ArabicShapingException", new ExceptionTests.ArabicShapingExceptionTest());
        map.put("com.ibm.icu.text.StringPrepParseException", new ExceptionTests.StringPrepParseExceptionTest());
        map.put("com.ibm.icu.util.UResourceTypeMismatchException", new ExceptionTests.UResourceTypeMismatchExceptionTest());
    }
    
    public void testDirectory(File dir)
    {
        File files[] = dir.listFiles();
        
        for (int i = 0; i < files.length; i += 1) {
            check(files[i]);
        }
    }
    
    public void check(File file)
    {
        String filename = file.getName();
        int ix = filename.lastIndexOf(".dat");
        
        if (ix < 0) {
            return;
        }
        
        String className = filename.substring(0, ix);
        Handler handler = getHandler(className);

        System.out.print(className + " - ");
        
        if (handler == null) {
            System.out.println("no test.");
            return;
        }
        
        try {
            FileInputStream fs = new FileInputStream(file);
            
            ObjectInputStream in = new ObjectInputStream(fs);
            Object inputObjects[] = (Object[]) in.readObject();
            Object testObjects[] = handler.getTestObjects();
            boolean passed = true;
            
            in.close();
            fs.close();
            
            // TODO: add equality test...
            for (int i = 0; i < testObjects.length; i += 1) {
                if (! handler.hasSameBehavior(inputObjects[i], testObjects[i])) {
                    passed = false;
                    System.out.println("Input object " + i + " failed behavior test.");
                }
            }
            
            if (passed) {
                System.out.println("test passed.");
            }
        } catch (Exception e) {
            System.out.println("Error processing test object: " + e.toString());
        }
    }
    
    public static void main(String[] args)
    {
        SerializableTest test = new SerializableTest();
        List argList = Arrays.asList(args);
        boolean write = false;
        
        for (Iterator it = argList.iterator(); it.hasNext(); /*anything?*/) {
            String arg = (String) it.next();
            
            try {
                File dir = new File(arg);
                
                if (! dir.isDirectory()) {
                    System.out.println(dir + " is not a directory.");
                    continue;
                }
                
                System.out.println("Checking test data from " + arg + ":");
                test.testDirectory(dir);
            } catch (Exception e) {
                System.out.println("Error processing " + arg + ": " + e.getMessage());
            }
        }
    }
}
