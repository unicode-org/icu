// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.ibm.icu.dev.test.format.MeasureUnitTest;
import com.ibm.icu.dev.test.format.PluralRulesTest;
import com.ibm.icu.dev.test.number.NumberFormatterApiTest;
import com.ibm.icu.dev.test.number.PropertiesTest;
import com.ibm.icu.impl.JavaTimeZone;
import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.impl.TimeZoneAdapter;
import com.ibm.icu.impl.URLHandler;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.message2.Mf2DataModel;
import com.ibm.icu.util.AnnualTimeZoneRule;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.DateTimeRule;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.ICUCloneNotSupportedException;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUInputTooLongException;
import com.ibm.icu.util.ICUUncheckedIOException;
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
public class SerializableTestUtility {
    private static Class serializable;
    static {
        try {
            serializable = Class.forName("java.io.Serializable");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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

        @Override
        public Object[] getTestObjects()
        {
            TimeZone zones[] = new TimeZone[ZONES.length];

            for(int z = 0; z < ZONES.length; z += 1) {
                zones[z] = TimeZone.getTimeZone(ZONES[z]);
            }

            return zones;
        }

        @Override
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
        @Override
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
        @Override
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
        @Override
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
        @Override
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

        @Override
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
        @Override
        public Object[] getTestObjects() {
            return TEST_US_EASTERN;
        }

        @Override
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
        @Override
        public Object[] getTestObjects() {
            TimeZoneRule[] rules = new TimeZoneRule[2];
            rules[0] = new InitialTimeZoneRule("EST", -5*HOUR, 0);
            rules[1] = new InitialTimeZoneRule("PST", -8*HOUR, 0);
            return rules;
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            return compareTimeZoneRules((TimeZoneRule)a, (TimeZoneRule)b);
        }
    }

    private static class TimeArrayTimeZoneRuleHandler implements Handler {
        @Override
        public Object[] getTestObjects() {
            TimeArrayTimeZoneRule[] rules = new TimeArrayTimeZoneRule[1];
            long[] ttime = new long[] {-631152000000L, 0L, 946684800000L}; /* {1950-1-1, 1970-1-1, 2000-1-1} */
            rules[0] = new TimeArrayTimeZoneRule("Foo", 1*HOUR, 1*HOUR, ttime, DateTimeRule.UTC_TIME);

            return rules;
        }
        @Override
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
        @Override
        public Object[] getTestObjects()
        {
            ULocale uLocales[] = new ULocale[locales.length];

            for (int i = 0; i < locales.length; i += 1) {
                uLocales[i] = ULocale.forLocale(locales[i]);
            }

            return uLocales;
        }

        @Override
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
        @Override
        public Object[] getTestObjects()
        {
            return dateInterval;
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            return a.equals(b);
        }
    }

    private static class CurrencyHandler implements Handler
    {
        @Override
        public Object[] getTestObjects()
        {
            Currency currencies[] = new Currency[places.length];

            for (int i = 0; i < places.length; i += 1) {
                currencies[i] = Currency.getInstance(places[i]);
            }

            return currencies;
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {

            Currency curr_a = (Currency) a;
            Currency curr_b = (Currency) b;

            return a == b
                    || a != null && b != null
                    && curr_a.getCurrencyCode() != null
                    && curr_a.getCurrencyCode().equals(curr_b.getCurrencyCode());

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
        @Override
        public Object[] getTestObjects()
        {
            OlsonTimeZone timeZones[] = new OlsonTimeZone[zoneIDs.length];

            for (int i = 0; i < zoneIDs.length; i += 1) {
                timeZones[i] = new OlsonTimeZone(zoneIDs[i]);
            }

            return timeZones;

        }

        @Override
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
        @Override
        public Object[] getTestObjects()
        {
            TimeZoneAdapter timeZones[] = new TimeZoneAdapter[zoneIDs.length];

            for (int i = 0; i < zoneIDs.length; i += 1) {
                timeZones[i] = new TimeZoneAdapter(TimeZone.getTimeZone(zoneIDs[i]));
            }

            return timeZones;

        }

        @Override
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

        @Override
        public Object[] getTestObjects() {
            JavaTimeZone zones[] = new JavaTimeZone[ZONES.length];
            for(int z = 0; z < ZONES.length; z += 1) {
                java.util.TimeZone tz = java.util.TimeZone.getTimeZone(ZONES[z]);
                zones[z] = new JavaTimeZone(tz, ZONES[z]);
            }
            return zones;
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            TimeZone zone_a = (TimeZone) a;
            TimeZone zone_b = (TimeZone) b;

            if (!(TimeZone.getCanonicalID(zone_a.getID()).equals(TimeZone.getCanonicalID(zone_b.getID())))) {
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

        @Override
        public Object[] getTestObjects()
        {
            BigDecimal bds[] = new BigDecimal[values.length];

            for (int i = 0; i < values.length; i += 1) {
                bds[i] = new BigDecimal(values[i]);
            }

            return bds;
        }

        @Override
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

        @Override
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

        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            MathContext mca = (MathContext) a;
            MathContext mcb = (MathContext) b;

            return mca.toString().equals(mcb.toString());
        }
    }

    private static abstract class ExceptionHandlerBase implements Handler {
        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            return sameThrowable((Exception) a, (Exception) b);
        }

        // Exception.equals() does not seem to work.
        private static final boolean sameThrowable(Throwable a, Throwable b) {
            return a == null ? b == null :
                    b == null ? false :
                            a.getClass().equals(b.getClass()) &&
                            Objects.equals(a.getMessage(), b.getMessage()) &&
                            sameThrowable(a.getCause(), b.getCause());
        }
    }

    private static class ICUExceptionHandler extends ExceptionHandlerBase {
        @Override
        public Object[] getTestObjects() {
            return new ICUException[] {
                    new ICUException(),
                    new ICUException("msg1"),
                    new ICUException(new RuntimeException("rte1")),
                    new ICUException("msg2", new RuntimeException("rte2"))
            };
        }
    }

    private static class ICUUncheckedIOExceptionHandler extends ExceptionHandlerBase {
        @Override
        public Object[] getTestObjects() {
            return new ICUUncheckedIOException[] {
                    new ICUUncheckedIOException(),
                    new ICUUncheckedIOException("msg1"),
                    new ICUUncheckedIOException(new RuntimeException("rte1")),
                    new ICUUncheckedIOException("msg2", new RuntimeException("rte2"))
            };
        }
    }

    private static class ICUCloneNotSupportedExceptionHandler extends ExceptionHandlerBase {
        @Override
        public Object[] getTestObjects() {
            return new ICUCloneNotSupportedException[] {
                    new ICUCloneNotSupportedException(),
                    new ICUCloneNotSupportedException("msg1"),
                    new ICUCloneNotSupportedException(new RuntimeException("rte1")),
                    new ICUCloneNotSupportedException("msg2", new RuntimeException("rte2"))
            };
        }
    }

    private static class ICUInputTooLongExceptionHandler extends ExceptionHandlerBase {
        @Override
        public Object[] getTestObjects() {
            return new ICUInputTooLongException[] {
                    new ICUInputTooLongException(),
                    new ICUInputTooLongException("msg1"),
                    new ICUInputTooLongException(new RuntimeException("rte1")),
                    new ICUInputTooLongException("msg2", new RuntimeException("rte2"))
            };
        }
    }

    private static class Mf2DataModelOrderedMapHandler implements Handler {
        @Override
        public Object[] getTestObjects() {
            Mf2DataModel.OrderedMap<String, Object> mapWithContent = new Mf2DataModel.OrderedMap<>();
            mapWithContent.put("number", Double.valueOf(3.1416));
            mapWithContent.put("date", new Date(1664582400000L /* 20221001T000000Z */));
            mapWithContent.put("string", "testing");
            return new Mf2DataModel.OrderedMap[] {
                    new Mf2DataModel.OrderedMap(),
                    mapWithContent
            };
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            // OrderedMap extends LinkedHashMap, without adding any functionality, nothing to test.
            Mf2DataModel.OrderedMap ra = (Mf2DataModel.OrderedMap)a;
            Mf2DataModel.OrderedMap rb = (Mf2DataModel.OrderedMap)b;
            return ra.equals(rb);
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

        map.put("com.ibm.icu.text.NumberFormat", new FormatHandler.NumberFormatHandler());
        map.put("com.ibm.icu.text.DecimalFormat", new FormatHandler.DecimalFormatHandler());
        map.put("com.ibm.icu.text.CompactDecimalFormat", new FormatHandler.CompactDecimalFormatHandler());
        map.put("com.ibm.icu.text.RuleBasedNumberFormat", new FormatHandler.RuleBasedNumberFormatHandler());
        map.put("com.ibm.icu.text.CurrencyPluralInfo", new FormatHandler.CurrencyPluralInfoHandler());
        map.put("com.ibm.icu.text.DecimalFormatSymbols", new FormatHandler.DecimalFormatSymbolsHandler());
        map.put("com.ibm.icu.text.MessageFormat", new FormatHandler.MessageFormatHandler());
        map.put("com.ibm.icu.text.DateFormat", new FormatHandler.DateFormatHandler());
        map.put("com.ibm.icu.text.DateFormatSymbols", new FormatHandler.DateFormatSymbolsHandler());
        map.put("com.ibm.icu.util.DateInterval", new DateIntervalHandler());
        map.put("com.ibm.icu.text.DateIntervalFormat", new FormatHandler.DateIntervalFormatHandler());
        map.put("com.ibm.icu.text.DateIntervalInfo", new FormatHandler.DateIntervalInfoHandler());
        map.put("com.ibm.icu.text.DateIntervalInfo$PatternInfo", new FormatHandler.PatternInfoHandler());
        map.put("com.ibm.icu.text.SimpleDateFormat", new FormatHandler.SimpleDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormat", new FormatHandler.ChineseDateFormatHandler());
        map.put("com.ibm.icu.text.ChineseDateFormatSymbols", new FormatHandler.ChineseDateFormatSymbolsHandler());
        map.put("com.ibm.icu.impl.DateNumberFormat", new FormatHandler.DateNumberFormatHandler());
        map.put("com.ibm.icu.text.PluralFormat", new FormatHandler.PluralFormatHandler());
        map.put("com.ibm.icu.text.PluralRules", new FormatHandler.PluralRulesHandler());
        map.put("com.ibm.icu.text.PluralRulesSerialProxy", new FormatHandler.PluralRulesSerialProxyHandler());
        map.put("com.ibm.icu.text.TimeUnitFormat", new FormatHandler.TimeUnitFormatHandler());
        map.put("com.ibm.icu.text.SelectFormat", new FormatHandler.SelectFormatHandler());
        map.put("com.ibm.icu.impl.TimeZoneNamesImpl", new FormatHandler.TimeZoneNamesHandler());
        map.put("com.ibm.icu.text.TimeZoneFormat", new FormatHandler.TimeZoneFormatHandler());
        map.put("com.ibm.icu.impl.TimeZoneGenericNames", new FormatHandler.TimeZoneGenericNamesHandler());
        map.put("com.ibm.icu.impl.TZDBTimeZoneNames", new FormatHandler.TZDBTimeZoneNamesHandler());

        map.put("com.ibm.icu.util.Calendar", new CalendarHandler.BasicCalendarHandler());
        map.put("com.ibm.icu.util.BuddhistCalendar", new CalendarHandler.BuddhistCalendarHandler());
        map.put("com.ibm.icu.util.ChineseCalendar", new CalendarHandler.ChineseCalendarHandler());
        map.put("com.ibm.icu.util.CopticCalendar", new CalendarHandler.CopticCalendarHandler());
        map.put("com.ibm.icu.util.DangiCalendar", new CalendarHandler.DangiCalendarHandler());
        map.put("com.ibm.icu.util.EthiopicCalendar", new CalendarHandler.EthiopicCalendarHandler());
        map.put("com.ibm.icu.util.GregorianCalendar", new CalendarHandler.GregorianCalendarHandler());
        map.put("com.ibm.icu.util.HebrewCalendar", new CalendarHandler.HebrewCalendarHandler());
        map.put("com.ibm.icu.util.IndianCalendar", new CalendarHandler.IndianCalendarHandler());
        map.put("com.ibm.icu.util.IslamicCalendar", new CalendarHandler.IslamicCalendarHandler());
        map.put("com.ibm.icu.util.JapaneseCalendar", new CalendarHandler.JapaneseCalendarHandler());
        map.put("com.ibm.icu.util.PersianCalendar", new CalendarHandler.PersianCalendarHandler());
        map.put("com.ibm.icu.util.TaiwanCalendar", new CalendarHandler.TaiwanCalendarHandler());

        map.put("com.ibm.icu.text.ArabicShapingException", new ExceptionHandler.ArabicShapingExceptionHandler());
        map.put("com.ibm.icu.text.StringPrepParseException", new ExceptionHandler.StringPrepParseExceptionHandler());
        map.put("com.ibm.icu.util.UResourceTypeMismatchException", new ExceptionHandler.UResourceTypeMismatchExceptionHandler());
        map.put("com.ibm.icu.impl.InvalidFormatException", new ExceptionHandler.InvalidFormatExceptionHandler());

        map.put("com.ibm.icu.text.NumberFormat$Field", new FormatHandler.NumberFormatFieldHandler());
        map.put("com.ibm.icu.text.DateFormat$Field", new FormatHandler.DateFormatFieldHandler());
        map.put("com.ibm.icu.text.ChineseDateFormat$Field", new FormatHandler.ChineseDateFormatFieldHandler());
        map.put("com.ibm.icu.text.MessageFormat$Field", new FormatHandler.MessageFormatFieldHandler());
        map.put("com.ibm.icu.text.RelativeDateTimeFormatter$Field", new FormatHandler.RelativeDateTimeFormatterFieldHandler());
        map.put("com.ibm.icu.text.DateIntervalFormat$SpanField", new FormatHandler.DateIntervalSpanFieldHandler());
        map.put("com.ibm.icu.text.ListFormatter$Field", new FormatHandler.ListFormatterFieldHandler());
        map.put("com.ibm.icu.text.ListFormatter$SpanField", new FormatHandler.ListFormatterSpanFieldHandler());
        map.put("com.ibm.icu.number.NumberRangeFormatter$SpanField", new FormatHandler.NumberRangeFormatterSpanFieldHandler());

        map.put("com.ibm.icu.impl.duration.BasicDurationFormat", new FormatHandler.BasicDurationFormatHandler());
        map.put("com.ibm.icu.impl.RelativeDateFormat", new FormatHandler.RelativeDateFormatHandler());
        map.put("com.ibm.icu.util.IllformedLocaleException", new ExceptionHandler.IllformedLocaleExceptionHandler());
        map.put("com.ibm.icu.impl.locale.LocaleSyntaxException", new ExceptionHandler.LocaleSyntaxExceptionHandler());
        map.put("com.ibm.icu.impl.IllegalIcuArgumentException", new ExceptionHandler.IllegalIcuArgumentExceptionHandler());

        map.put("com.ibm.icu.text.PluralRules$FixedDecimal", new PluralRulesTest.FixedDecimalHandler());
        map.put("com.ibm.icu.util.MeasureUnit", new MeasureUnitTest.MeasureUnitHandler());
        map.put("com.ibm.icu.util.TimeUnit", new MeasureUnitTest.MeasureUnitHandler());
        map.put("com.ibm.icu.util.NoUnit", new MeasureUnitTest.MeasureUnitHandler());
        map.put("com.ibm.icu.text.MeasureFormat", new MeasureUnitTest.MeasureFormatHandler());
        map.put("com.ibm.icu.impl.number.Properties", new PropertiesTest.ICU59PropertiesHandler());
        map.put("com.ibm.icu.impl.number.DecimalFormatProperties", new PropertiesTest.PropertiesHandler());
        map.put("com.ibm.icu.impl.number.CustomSymbolCurrency", new CurrencyHandler());
        map.put("com.ibm.icu.number.SkeletonSyntaxException", new ExceptionHandler.SkeletonSyntaxExceptionHandler());
        map.put("com.ibm.icu.impl.number.LocalizedNumberFormatterAsFormat", new NumberFormatterApiTest.FormatHandler());

        map.put("com.ibm.icu.util.ICUException", new ICUExceptionHandler());
        map.put("com.ibm.icu.util.ICUUncheckedIOException", new ICUUncheckedIOExceptionHandler());
        map.put("com.ibm.icu.util.ICUCloneNotSupportedException", new ICUCloneNotSupportedExceptionHandler());
        map.put("com.ibm.icu.util.ICUInputTooLongException", new ICUInputTooLongExceptionHandler());

        map.put("com.ibm.icu.message2.Mf2DataModel$OrderedMap", new Mf2DataModelOrderedMapHandler());
    }

    /*
     * Serialization Helpers
     */
    static Object[] getSerializedObjects(byte[] serializedBytes) throws ClassNotFoundException, IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object inputObjects[] = (Object[]) ois.readObject();

        ois.close();
        return inputObjects;
    }

    static byte[] getSerializedBytes(Object[] objectsOut) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(objectsOut);

        byte[] serializedBytes = bos.toByteArray();
        oos.close();
        return serializedBytes;
    }

    static Object[] getSerializedObjects(File testFile) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(testFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object[] objects = (Object[]) ois.readObject();
        fis.close();
        return objects;
    }

    static byte[] copyStreamBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        while((len = is.read(buffer, 0, buffer.length)) >= 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }

    static List<String> getSerializationClassList(Object caller) throws IOException {
        List<String> classList = new ArrayList();
        Enumeration<URL> urlEnum = caller.getClass().getClassLoader().getResources("com/ibm/icu");
        while (urlEnum.hasMoreElements()) {
            URL url = urlEnum.nextElement();
            URLHandler handler  = URLHandler.get(url);
            if (handler == null) {
                System.out.println("Unsupported URL: " + url);
                continue;
            }
            CoverageClassVisitor visitor = new CoverageClassVisitor(classList);
            handler.guide(visitor, true, false);
        }
        return classList;
    }

    private static class CoverageClassVisitor implements URLHandler.URLVisitor {
        private List<String> classNames;

        public CoverageClassVisitor(List<String> classNamesList) {
            this.classNames = classNamesList;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.impl.URLHandler.URLVisitor#visit(java.lang.String)
         */
        @Override
        public void visit(String classPath) {
            int ix = classPath.lastIndexOf(".class");
            if (ix < 0) {
                return;
            }
            String className = "com.ibm.icu" + classPath.substring(0, ix).replace('/', '.');

            // Skip things in com.ibm.icu.dev; they're not relevant.
            if (className.startsWith("com.ibm.icu.dev.")) {
                return;
            }
            Class c;
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
                return;
            }
            int m = c.getModifiers();

            if (className.equals("com.ibm.icu.text.PluralRules$FixedDecimal")) {
                // Known Issue: "10268", "Serializable interface is not implemented in PluralRules$FixedDecimal"
                return;
            }

            if (className.equals("com.ibm.icu.text.DecimalFormat_ICU58")) {
                // Do not test the legacy DecimalFormat class in ICU 59
                return;
            }

            if (c.isEnum() || !serializable.isAssignableFrom(c)) {
                //System.out.println("@@@ Skipping: " + className);
                return;
            }
            if (!Modifier.isPublic(m) || Modifier.isInterface(m)) {
                //System.out.println("@@@ Skipping: " + className);
                return;
            }

            this.classNames.add(className);
        }
    }

    public static void serializeObjects(File oof, Object[] objectsOut) throws IOException {
        FileOutputStream fos = new FileOutputStream(oof);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(objectsOut);

        oos.close();
    }
}
