/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Date;
import java.util.Locale;
import java.util.HashMap;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.JDKTimeZone;
import com.ibm.icu.impl.LinkedHashMap;
import com.ibm.icu.impl.LRUMap;
import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.impl.TimeZoneAdapter;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SerializableTest extends TestFmwk.TestGroup
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
    
    private static class TimeZoneHandler implements Handler
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

    private static class SimpleTimeZoneHandler extends TimeZoneHandler
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
    
    private static class ULocaleHandler implements Handler
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
    
    private static class CurrencyHandler implements Handler
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
    
    private static class OlsonTimeZoneHandler implements Handler
    {
        String zoneIDs[] = {
            "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
            "America/Chicago", "America/New_York", "Africa/Cairo", "Africa/Addis_Ababa", "Africa/Dar_es_Salaam",
            "Africa/Freetown", "Africa/Johannesburg", "Africa/Nairobi", "Asia/Bangkok", "Asia/Baghdad",
            "Asia/Calcutta", "Asia/Hong_Kong", "Asia/Jakarta", "Asia/Jerusalem", "Asia/Manila", "Asia/Tokyo",
            "Europe/Amsterdam", "Europe/Athens", "Europe/Berlin", "Europe/London", "Europe/Malta", "Europe/Moscow",
            "Europe/Paris", "Europe/Rome"
        };

        long sampleTimes[] = {
            1136073600000L, // 20060101T000000Z
            1138752000000L, // 20060201T000000Z
            1141171200000L, // 20060301T000000Z
            1143849600000L, // 20060401T000000Z
            1146441600000L, // 20060501T000000Z
            1149120000000L, // 20060601T000000Z
            1151712000000L, // 20060701T000000Z
            1154390400000L, // 20060801T000000Z
            1157068800000L, // 20060901T000000Z
            1159660800000L, // 20061001T000000Z
            1162339200000L, // 20061101T000000Z
            1164931200000L, // 20061201T000000Z
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
            int a_offsets[] = {0, 0};
            int b_offsets[] = {0, 0};

            boolean bSame = true;
            for (int i = 0; i < sampleTimes.length; i++) {
                otz_a.getOffset(sampleTimes[i], false, a_offsets);
                otz_b.getOffset(sampleTimes[i], false, b_offsets);
                if (a_offsets[0] != b_offsets[0] || a_offsets[1] != b_offsets[1]) {
                    bSame = false;
                    break;
                }
            }
            return bSame;
        }
    }
    
    private static class JDKTimeZoneHandler implements Handler
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
            JDKTimeZone timeZones[] = new JDKTimeZone[zoneIDs.length];
            
            for (int i = 0; i < zoneIDs.length; i += 1) {
                timeZones[i] = new JDKTimeZone(java.util.TimeZone.getTimeZone(zoneIDs[i]));
            }
            
            return timeZones;
                
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            JDKTimeZone jtz_a = (JDKTimeZone) a;
            JDKTimeZone jtz_b = (JDKTimeZone) b;
            long now = System.currentTimeMillis();
            int a_offsets[] = {0, 0};
            int b_offsets[] = {0, 0};
            
            jtz_a.getOffset(now, false, a_offsets);
            jtz_b.getOffset(now, false, b_offsets);
            
            return a_offsets[0] == b_offsets[0] && a_offsets[1] == b_offsets[1];
        }
    }
    
    private static class TimeZoneAdapterHandler implements Handler
    {
        String zoneIDs[] = {
            "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
            "America/Chicago", "America/New_York", "Africa/Cairo", "Africa/Addis_Ababa", "Africa/Dar_es_Salaam",
            "Africa/Freetown", "Africa/Johannesburg", "Africa/Nairobi", "Asia/Bangkok", "Asia/Baghdad",
            "Asia/Calcutta", "Asia/Hong_Kong", "Asia/Jakarta", "Asia/Jerusalem", "Asia/Manila", "Asia/Tokyo",
            "Europe/Amsterdam", "Europe/Athens", "Europe/Berlin", "Europe/London", "Europe/Malta", "Europe/Moscow",
            "Europe/Paris", "Europe/Rome"
        };

        long sampleTimes[] = {
            1136073600000L, // 20060101T000000Z
            1138752000000L, // 20060201T000000Z
            1141171200000L, // 20060301T000000Z
            1143849600000L, // 20060401T000000Z
            1146441600000L, // 20060501T000000Z
            1149120000000L, // 20060601T000000Z
            1151712000000L, // 20060701T000000Z
            1154390400000L, // 20060801T000000Z
            1157068800000L, // 20060901T000000Z
            1159660800000L, // 20061001T000000Z
            1162339200000L, // 20061101T000000Z
            1164931200000L, // 20061201T000000Z
        };

        public Object[] getTestObjects()
        {
            TimeZoneAdapter timeZones[] = new TimeZoneAdapter[zoneIDs.length];
            
            for (int i = 0; i < zoneIDs.length; i += 1) {
                timeZones[i] = new TimeZoneAdapter(TimeZone.getTimeZone(zoneIDs[i]));
            }
            
            return timeZones;
                
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            GregorianCalendar cal = new GregorianCalendar();
            TimeZoneAdapter tza_a = (TimeZoneAdapter) a;
            TimeZoneAdapter tza_b = (TimeZoneAdapter) b;
            
            int a_offset, b_offset;
            boolean a_dst, b_dst;
            boolean bSame = true;
            for (int i = 0; i < sampleTimes.length; i++) {
                cal.setTimeInMillis(sampleTimes[i]);
                int era = cal.get(Calendar.ERA);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                int mid = cal.get(Calendar.MILLISECONDS_IN_DAY);
                a_offset = tza_a.getOffset(era, year, month, day, dow, mid);
                b_offset = tza_b.getOffset(era, year, month, day, dow, mid);
                Date d = new Date(sampleTimes[i]);
                a_dst = tza_a.inDaylightTime(d);
                b_dst = tza_b.inDaylightTime(d);
                if (a_offset != b_offset || a_dst != b_dst) {
                    bSame = false;
                    break;
                }
            }
            return bSame;
        }
    }
    
    private static class BigDecimalHandler implements Handler
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
    
    private static class MathContextHandler implements Handler
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
    
    private static class LRUMapHandler implements Handler
    {
        public Object[] getTestObjects()
        {
            LRUMap[] maps = new LRUMap[1];
            maps[0] = new LRUMap();
            maps[0].put("1", "a");
            maps[0].put("2", "b");
            return maps;
        }
        public boolean hasSameBehavior(Object a, Object b)
        {
            LRUMap mapA = (LRUMap) a;
            LRUMap mapB = (LRUMap) b;
            return mapA.equals(mapB);
        }
    }

    private static class LinkedHashMapHandler implements Handler
    {
        public Object[] getTestObjects()
        {
            LinkedHashMap[] maps = new LinkedHashMap[2];
            maps[0] = new LinkedHashMap();
            maps[1] = new LinkedHashMap(16, 0.75F, true);
            for (int i = 0; i < 2; i++) {
                maps[i].put("1", "a");
                maps[i].put("2", "b");
            }
            return maps;
        }
        public boolean hasSameBehavior(Object a, Object b)
        {
            LinkedHashMap mapA = (LinkedHashMap) a;
            LinkedHashMap mapB = (LinkedHashMap) b;
            return mapA.equals(mapB);
        }
    }

    private static HashMap map = new HashMap();
    
    static {
        map.put("com.ibm.icu.util.TimeZone", new TimeZoneHandler());
        map.put("com.ibm.icu.util.SimpleTimeZone", new SimpleTimeZoneHandler());
        map.put("com.ibm.icu.util.ULocale", new ULocaleHandler());
        map.put("com.ibm.icu.util.Currency", new CurrencyHandler());
        map.put("com.ibm.icu.impl.JDKTimeZone", new JDKTimeZoneHandler());
        map.put("com.ibm.icu.impl.LinkedHashMap", new LinkedHashMapHandler());
        map.put("com.ibm.icu.impl.LRUMap", new LRUMapHandler());
        map.put("com.ibm.icu.impl.OlsonTimeZone", new OlsonTimeZoneHandler());
        map.put("com.ibm.icu.impl.TimeZoneAdapter", new TimeZoneAdapterHandler());
        map.put("com.ibm.icu.math.BigDecimal", new BigDecimalHandler());
        map.put("com.ibm.icu.math.MathContext", new MathContextHandler());
        
        map.put("com.ibm.icu.text.NumberFormat", new FormatTests.NumberFormatHandler());
        map.put("com.ibm.icu.text.DecimalFormat", new FormatTests.DecimalFormatHandler());
        map.put("com.ibm.icu.text.RuleBasedNumberFormat", new FormatTests.RuleBasedNumberFormatHandler());
        map.put("com.ibm.icu.text.DecimalFormatSymbols", new FormatTests.DecimalFormatSymbolsHandler());
        map.put("com.ibm.icu.text.MessageFormat", new FormatTests.MessageFormatHandler());
        map.put("com.ibm.icu.text.DateFormat", new FormatTests.DateFormatHandler());
        map.put("com.ibm.icu.text.DateFormatSymbols", new FormatTests.DateFormatSymbolsHandler());
        map.put("com.ibm.icu.text.SimpleDateFormat", new FormatTests.SimpleDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormat", new FormatTests.ChineseDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormatSymbols", new FormatTests.ChineseDateFormatSymbolsHandler());

        map.put("com.ibm.icu.util.Calendar", new CalendarTests.CalendarHandler());
        map.put("com.ibm.icu.util.BuddhistCalendar", new CalendarTests.BuddhistCalendarHandler());
        map.put("com.ibm.icu.util.ChineseCalendar", new CalendarTests.ChineseCalendarHandler());
        map.put("com.ibm.icu.util.CopticCalendar", new CalendarTests.CopticCalendarHandler());
        map.put("com.ibm.icu.util.EthiopicCalendar", new CalendarTests.EthiopicCalendarHandler());
        map.put("com.ibm.icu.util.GregorianCalendar", new CalendarTests.GregorianCalendarHandler());
        map.put("com.ibm.icu.util.HebrewCalendar", new CalendarTests.HebrewCalendarHandler());
        map.put("com.ibm.icu.util.IslamicCalendar", new CalendarTests.IslamicCalendarHandler());
        map.put("com.ibm.icu.util.JapaneseCalendar", new CalendarTests.JapaneseCalendarHandler());
        
        map.put("com.ibm.icu.text.ArabicShapingException", new ExceptionTests.ArabicShapingExceptionHandler());
        map.put("com.ibm.icu.text.StringPrepParseException", new ExceptionTests.StringPrepParseExceptionHandler());
        map.put("com.ibm.icu.util.UResourceTypeMismatchException", new ExceptionTests.UResourceTypeMismatchExceptionHandler());
    }
    
    public SerializableTest()
    {
        super(
            new String[] {
                "com.ibm.icu.dev.test.serializable.CoverageTest",
                "com.ibm.icu.dev.test.serializable.CompatibilityTest"},
                "All Serializable Tests"
        );
    }

    public static final String CLASS_TARGET_NAME  = "Serializable";

    public static void main(String[] args)
    {
        SerializableTest test = new SerializableTest();
        
        test.run(args);
    }
}
