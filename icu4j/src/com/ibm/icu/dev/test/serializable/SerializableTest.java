//##header
/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.JavaTimeZone;
import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.impl.TimeZoneAdapter;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.util.AnnualTimeZoneRule;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.DateTimeRule;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.InitialTimeZoneRule;
import com.ibm.icu.util.RuleBasedTimeZone;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeArrayTimeZoneRule;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VTimeZone;

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

            if (!(zone_a.getDisplayName().equals(zone_b.getDisplayName()))) {
                return false;
            }

            int a_offsets[] = {0, 0};
            int b_offsets[] = {0, 0};

            boolean bSame = true;
            for (int i = 0; i < sampleTimes.length; i++) {
                zone_a.getOffset(sampleTimes[i], false, a_offsets);
                zone_b.getOffset(sampleTimes[i], false, b_offsets);
                if (a_offsets[0] != b_offsets[0] || a_offsets[1] != b_offsets[1]) {
                    bSame = false;
                    break;
                }
            }
            return bSame;
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

    private static class VTimeZoneHandler extends TimeZoneHandler {
        public Object[] getTestObjects() {
            //TODO
            VTimeZone[] vtzs = new VTimeZone[1];
            vtzs[0] = VTimeZone.create("America/New_York");
            return vtzs;
        }
    }

    private static final int HOUR = 60*60*1000;
    private static final AnnualTimeZoneRule[] TEST_US_EASTERN = {
        new AnnualTimeZoneRule("EST", -5*HOUR, 0,
                new DateTimeRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*HOUR, DateTimeRule.WALL_TIME),
                1967, 2006),
        
        new AnnualTimeZoneRule("EST", -5*HOUR, 0,
                new DateTimeRule(Calendar.NOVEMBER, 1, Calendar.SUNDAY, true, 2*HOUR, DateTimeRule.WALL_TIME),
                2007, AnnualTimeZoneRule.MAX_YEAR),
        
        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.APRIL, -1, Calendar.SUNDAY, 2*HOUR, DateTimeRule.WALL_TIME),
                1967, 1973),

        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.JANUARY, 6, 2*HOUR, DateTimeRule.WALL_TIME),
                1974, 1974),

        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.FEBRUARY, 23, 2*HOUR, DateTimeRule.WALL_TIME),
                1975, 1975),

        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.APRIL, -1, Calendar.SUNDAY, 2*HOUR, DateTimeRule.WALL_TIME),
                1976, 1986),

        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.APRIL, 1, Calendar.SUNDAY, true, 2*HOUR, DateTimeRule.WALL_TIME),
                1987, 2006),

        new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR,
                new DateTimeRule(Calendar.MARCH, 8, Calendar.SUNDAY, true, 2*HOUR, DateTimeRule.WALL_TIME),
                2007, AnnualTimeZoneRule.MAX_YEAR)
    };
    

    
    private static class RuleBasedTimeZoneHandler extends TimeZoneHandler
    {
        public Object[] getTestObjects()
        {
            RuleBasedTimeZone ruleBasedTimeZones[] = new RuleBasedTimeZone[2];

            InitialTimeZoneRule ir = new InitialTimeZoneRule("GMT-5", -5*HOUR, 0);

            // GMT-5, no transition
            ruleBasedTimeZones[0] = new RuleBasedTimeZone("GMT-5", ir);


            // US Eastern since 1967
            ruleBasedTimeZones[1] = new RuleBasedTimeZone("US_East", ir);
            for (int i = 0; i < TEST_US_EASTERN.length; i++) {
                ruleBasedTimeZones[1].addTransitionRule(TEST_US_EASTERN[i]);
            }
            return ruleBasedTimeZones;
        }
    }

    private static class DateTimeRuleHandler implements Handler {
        public Object[] getTestObjects() {
            DateTimeRule[] rules = new DateTimeRule[4];

            // DOM + UTC
            rules[0] = new DateTimeRule(Calendar.OCTOBER, 10, 13*HOUR, DateTimeRule.UTC_TIME);

            // DOW + WALL
            rules[1] = new DateTimeRule(Calendar.MARCH, 2, Calendar.SUNDAY, 2*HOUR, DateTimeRule.WALL_TIME);

            // DOW_GEQ_DOM + STD
            rules[2] = new DateTimeRule(Calendar.MAY, 1, Calendar.MONDAY, true, 0*HOUR, DateTimeRule.STANDARD_TIME);

            // DOW_LEQ_DOM + WALL
            rules[3] = new DateTimeRule(Calendar.AUGUST, 31, Calendar.SATURDAY, false, 1*HOUR, DateTimeRule.WALL_TIME);

            return rules;
        }

        public boolean hasSameBehavior(Object a, Object b) {
            return hasSameRule((DateTimeRule)a, (DateTimeRule)b);
        }

        static boolean hasSameRule(DateTimeRule dtra, DateTimeRule dtrb) {
            boolean bSame = false;
            if (dtra.getDateRuleType() == dtrb.getDateRuleType()
                    && dtra.getRuleMonth() == dtrb.getRuleMonth()
                    && dtra.getTimeRuleType() == dtrb.getTimeRuleType()
                    && dtra.getRuleMillisInDay() == dtrb.getRuleMillisInDay()) {
                switch (dtra.getDateRuleType()) {
                case DateTimeRule.DOM:
                    bSame = (dtra.getRuleDayOfMonth() == dtrb.getRuleDayOfMonth());
                    break;
                case DateTimeRule.DOW:
                    bSame = (dtra.getRuleDayOfWeek() == dtrb.getRuleDayOfWeek() &&
                                dtra.getRuleWeekInMonth() == dtrb.getRuleWeekInMonth());
                    break;
                case DateTimeRule.DOW_GEQ_DOM:
                case DateTimeRule.DOW_LEQ_DOM:
                    bSame = (dtra.getRuleDayOfMonth() == dtrb.getRuleDayOfMonth() &&
                                dtra.getRuleDayOfWeek() == dtrb.getRuleDayOfWeek());
                    break;
                }
            }
            return bSame;            
        }
    }

    private static boolean compareTimeZoneRules(TimeZoneRule ra, TimeZoneRule rb) {
        if (ra.getName().equals(rb.getName()) &&
                ra.getRawOffset() == rb.getRawOffset() &&
                ra.getDSTSavings() == rb.getDSTSavings()) {
            return true;
        }
        return false;        
    }

    private static class AnnualTimeZoneRuleHandler implements Handler {
        public Object[] getTestObjects() {
            return TEST_US_EASTERN;
        }

        public boolean hasSameBehavior(Object a, Object b) {
            AnnualTimeZoneRule ra = (AnnualTimeZoneRule)a;
            AnnualTimeZoneRule rb = (AnnualTimeZoneRule)b;
            if (DateTimeRuleHandler.hasSameRule(ra.getRule(), rb.getRule()) &&
                    ra.getStartYear() == rb.getStartYear() &&
                    ra.getEndYear() == rb.getEndYear()) {
                return compareTimeZoneRules(ra, rb);
            }
            return false;
        }
    }

    private static class InitialTimeZoneRuleHandler implements Handler {
        public Object[] getTestObjects() {
            TimeZoneRule[] rules = new TimeZoneRule[2];
            rules[0] = new InitialTimeZoneRule("EST", -5*HOUR, 0);
            rules[1] = new InitialTimeZoneRule("PST", -8*HOUR, 0);
            return rules;
        }

        public boolean hasSameBehavior(Object a, Object b) {
            return compareTimeZoneRules((TimeZoneRule)a, (TimeZoneRule)b);
        }
    }

    private static class TimeArrayTimeZoneRuleHandler implements Handler {
        public Object[] getTestObjects() {
            TimeArrayTimeZoneRule[] rules = new TimeArrayTimeZoneRule[1];
            long[] ttime = new long[] {-631152000000L, 0L, 946684800000L}; /* {1950-1-1, 1970-1-1, 2000-1-1} */
            rules[0] = new TimeArrayTimeZoneRule("Foo", 1*HOUR, 1*HOUR, ttime, DateTimeRule.UTC_TIME);

            return rules;
        }
        public boolean hasSameBehavior(Object a, Object b) {
            TimeArrayTimeZoneRule ra = (TimeArrayTimeZoneRule)a;
            TimeArrayTimeZoneRule rb = (TimeArrayTimeZoneRule)b;

            Date da = ra.getFirstStart(0, 0);
            Date db = rb.getFirstStart(0, 0);
            long t = da.getTime();
            if (da.equals(db)) {
                da = ra.getFinalStart(0, 0);
                db = rb.getFinalStart(0, 0);
                long end = da.getTime();
                if (da.equals(db)) {
                    while (t < end) {
                        da = ra.getNextStart(t, 0, 0, false);
                        db = ra.getNextStart(t, 0, 0, false);
                        if (da == null || db == null || !da.equals(db)) {
                            break;
                        }
                        t = da.getTime();
                    }
                    return compareTimeZoneRules(ra, rb);
                }
            }
            return false;
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
    
    public static class DateIntervalHandler implements Handler
    {
        private DateInterval dateInterval[] = {
                new DateInterval(0L, 1164931200000L/*20061201T000000Z*/)
        };
        public Object[] getTestObjects()
        {
            return dateInterval;
        }

        public boolean hasSameBehavior(Object a, Object b)
        {
            return a.equals(b);
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

    private static String zoneIDs[] = {
        "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
        "America/Chicago", "America/New_York", "Africa/Cairo", "Africa/Addis_Ababa", "Africa/Dar_es_Salaam",
        "Africa/Freetown", "Africa/Johannesburg", "Africa/Nairobi", "Asia/Bangkok", "Asia/Baghdad",
        "Asia/Calcutta", "Asia/Hong_Kong", "Asia/Jakarta", "Asia/Jerusalem", "Asia/Manila", "Asia/Tokyo",
        "Europe/Amsterdam", "Europe/Athens", "Europe/Berlin", "Europe/London", "Europe/Malta", "Europe/Moscow",
        "Europe/Paris", "Europe/Rome"
    };
        
    private static long sampleTimes[] = {
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

    private static class OlsonTimeZoneHandler implements Handler
    {
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

    private static class TimeZoneAdapterHandler implements Handler
    {
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

    private static class JavaTimeZoneHandler implements Handler {
        String[] ZONES = { "GMT", "America/New_York", "GMT+05:45" };

        public Object[] getTestObjects() {
            JavaTimeZone zones[] = new JavaTimeZone[ZONES.length];
            for(int z = 0; z < ZONES.length; z += 1) {
                zones[z] = new JavaTimeZone(ZONES[z]);
            }
            return zones;
        }
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            TimeZone zone_a = (TimeZone) a;
            TimeZone zone_b = (TimeZone) b;

            if (!(zone_a.getID().equals(zone_b.getID()))) {
                return false;
            }

            int a_offsets[] = {0, 0};
            int b_offsets[] = {0, 0};

            boolean bSame = true;
            for (int i = 0; i < sampleTimes.length; i++) {
                zone_a.getOffset(sampleTimes[i], false, a_offsets);
                zone_b.getOffset(sampleTimes[i], false, b_offsets);
                if (a_offsets[0] != b_offsets[0] || a_offsets[1] != b_offsets[1]) {
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

    private static HashMap map = new HashMap();
    
    static {
        map.put("com.ibm.icu.util.TimeZone", new TimeZoneHandler());
        map.put("com.ibm.icu.util.SimpleTimeZone", new SimpleTimeZoneHandler());
        map.put("com.ibm.icu.util.RuleBasedTimeZone", new RuleBasedTimeZoneHandler());
        map.put("com.ibm.icu.util.VTimeZone", new VTimeZoneHandler());
        map.put("com.ibm.icu.util.DateTimeRule", new DateTimeRuleHandler());
        map.put("com.ibm.icu.util.AnnualTimeZoneRule", new AnnualTimeZoneRuleHandler());
        map.put("com.ibm.icu.util.InitialTimeZoneRule", new InitialTimeZoneRuleHandler());
        map.put("com.ibm.icu.util.TimeArrayTimeZoneRule", new TimeArrayTimeZoneRuleHandler());
        map.put("com.ibm.icu.util.ULocale", new ULocaleHandler());
        map.put("com.ibm.icu.util.Currency", new CurrencyHandler());
        map.put("com.ibm.icu.impl.JavaTimeZone", new JavaTimeZoneHandler());
        map.put("com.ibm.icu.impl.OlsonTimeZone", new OlsonTimeZoneHandler());
        map.put("com.ibm.icu.impl.TimeZoneAdapter", new TimeZoneAdapterHandler());
        map.put("com.ibm.icu.math.BigDecimal", new BigDecimalHandler());
        map.put("com.ibm.icu.math.MathContext", new MathContextHandler());
        
        map.put("com.ibm.icu.text.NumberFormat", new FormatTests.NumberFormatHandler());
        map.put("com.ibm.icu.text.DecimalFormat", new FormatTests.DecimalFormatHandler());
        map.put("com.ibm.icu.text.RuleBasedNumberFormat", new FormatTests.RuleBasedNumberFormatHandler());
        map.put("com.ibm.icu.text.CurrencyPluralInfo", new FormatTests.CurrencyPluralInfoHandler());
        map.put("com.ibm.icu.text.DecimalFormatSymbols", new FormatTests.DecimalFormatSymbolsHandler());
        map.put("com.ibm.icu.text.MessageFormat", new FormatTests.MessageFormatHandler());
        map.put("com.ibm.icu.text.DateFormat", new FormatTests.DateFormatHandler());
        map.put("com.ibm.icu.text.DateFormatSymbols", new FormatTests.DateFormatSymbolsHandler());
        map.put("com.ibm.icu.util.DateInterval", new DateIntervalHandler());
        map.put("com.ibm.icu.text.DateIntervalFormat", new FormatTests.DateIntervalFormatHandler());
        map.put("com.ibm.icu.text.DateIntervalInfo", new FormatTests.DateIntervalInfoHandler());
        map.put("com.ibm.icu.text.DateIntervalInfo$PatternInfo", new FormatTests.PatternInfoHandler());
        map.put("com.ibm.icu.text.SimpleDateFormat", new FormatTests.SimpleDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormat", new FormatTests.ChineseDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormatSymbols", new FormatTests.ChineseDateFormatSymbolsHandler());
        map.put("com.ibm.icu.impl.DateNumberFormat", new FormatTests.DateNumberFormatHandler());
        map.put("com.ibm.icu.text.PluralFormat", new FormatTests.PluralFormatHandler());
        map.put("com.ibm.icu.text.PluralRules", new FormatTests.PluralRulesHandler());
        map.put("com.ibm.icu.text.TimeUnitFormat", new FormatTests.TimeUnitFormatHandler());

        map.put("com.ibm.icu.util.Calendar", new CalendarTests.CalendarHandler());
        map.put("com.ibm.icu.util.BuddhistCalendar", new CalendarTests.BuddhistCalendarHandler());
        map.put("com.ibm.icu.util.ChineseCalendar", new CalendarTests.ChineseCalendarHandler());
        map.put("com.ibm.icu.util.CopticCalendar", new CalendarTests.CopticCalendarHandler());
        map.put("com.ibm.icu.util.EthiopicCalendar", new CalendarTests.EthiopicCalendarHandler());
        map.put("com.ibm.icu.util.GregorianCalendar", new CalendarTests.GregorianCalendarHandler());
        map.put("com.ibm.icu.util.HebrewCalendar", new CalendarTests.HebrewCalendarHandler());
        map.put("com.ibm.icu.util.IndianCalendar", new CalendarTests.IndianCalendarHandler());
        map.put("com.ibm.icu.util.IslamicCalendar", new CalendarTests.IslamicCalendarHandler());
        map.put("com.ibm.icu.util.JapaneseCalendar", new CalendarTests.JapaneseCalendarHandler());
        map.put("com.ibm.icu.util.TaiwanCalendar", new CalendarTests.TaiwanCalendarHandler());
        
        map.put("com.ibm.icu.text.ArabicShapingException", new ExceptionTests.ArabicShapingExceptionHandler());
        map.put("com.ibm.icu.text.StringPrepParseException", new ExceptionTests.StringPrepParseExceptionHandler());
        map.put("com.ibm.icu.util.UResourceTypeMismatchException", new ExceptionTests.UResourceTypeMismatchExceptionHandler());
        map.put("com.ibm.icu.impl.InvalidFormatException", new ExceptionTests.InvalidFormatExceptionHandler());

//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
        map.put("com.ibm.icu.text.NumberFormat$Field", new FormatTests.NumberFormatFieldHandler());
        map.put("com.ibm.icu.text.DateFormat$Field", new FormatTests.DateFormatFieldHandler());
        map.put("com.ibm.icu.text.ChineseDateFormat$Field", new FormatTests.ChineseDateFormatFieldHandler());
        map.put("com.ibm.icu.text.MessageFormat$Field", new FormatTests.MessageFormatFieldHandler());
//#endif
        map.put("com.ibm.icu.impl.duration.BasicDurationFormat", new FormatTests.BasicDurationFormatHandler());
        map.put("com.ibm.icu.impl.RelativeDateFormat", new FormatTests.RelativeDateFormatHandler());
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
//eof
