//##header J2SE15
/*
 *******************************************************************************
 * Copyright (C) 2007, Google and  *
 * others. All Rights Reserved. *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.io.PrintWriter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TimeZoneFormatTest extends com.ibm.icu.dev.test.TestFmwk {

    public static void main(String[] args) throws Exception {
        new TimeZoneFormatTest().run(args);
    }

    public void TestTimeZones() {
        int testCount = 0;
        // test all combinations of the following
        String[] testLocales = { "en", "fr", "zh" };
        // pick one winter time, one summer time
        Date[] testDates = { new Date(107, 1, 15), new Date(107, 6, 15) };
        String[] zoneFormats = { "z", "zzzz", "Z", "ZZZZ", "v", "vvvv", "V", "VVVV" };
        Set mustRoundTrip = new HashSet(Arrays.asList(new String[] { "v", "vvvv", "VVVV" }));
        String[] zones = TimeZone.getAvailableIDs();

        // common objects
        ParsePosition inoutPosition = new ParsePosition(0);
        TimeZone unknownZone = new SimpleTimeZone(-31415, "Etc/Unknown");
        Calendar outputCalendar = Calendar.getInstance();
        ZoneStatus status = new ZoneStatus();
        
        // set up equivalents
        // could be optimized, but not worth the effort
        Map equivalentZones = new HashMap();
        for (int zoneIndex = 0; zoneIndex < zones.length; ++zoneIndex) {
            String zone = zones[zoneIndex];
            Set equivalents = new HashSet();
            equivalents.add(zone);
            for (int i = 0; i < TimeZone.countEquivalentIDs(zone); ++i) {
                equivalents.add(TimeZone.getEquivalentID(zone, i));
            }
            equivalentZones.put(zone, equivalents);
        }
        
       for (int testLocaleIndex = 0; testLocaleIndex < testLocales.length; ++testLocaleIndex) {
            ULocale locale = new ULocale(testLocales[testLocaleIndex]);

            // prepare the zoneFormats for the locale
            List zoneFormatList = new ArrayList();
            for (int zoneFormatsIndex = 0; zoneFormatsIndex < zoneFormats.length; ++zoneFormatsIndex) {
                String zoneFormat = zoneFormats[zoneFormatsIndex];
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(zoneFormat, locale);
                try {
                    // check once just to make sure the syntax is supported
                    simpleDateFormat.format(testDates[0]);
                } catch (RuntimeException e) {
                    errln("Unable to parse format: " + zoneFormat + "; " + e.getClass().getName() + ", " + e.getMessage());
                    bumpCount(status.formatFailures, zoneFormat, zones.length * testDates.length);
                    continue;
                }
                zoneFormatList.add(simpleDateFormat);
            }

            // loop over zones, formats, and test dates
            for (int zoneIndex = 0; zoneIndex < zones.length; ++zoneIndex) {
                TimeZone timezone = TimeZone.getTimeZone(zones[zoneIndex]);
                Set equivalents = (Set) equivalentZones.get(zones[zoneIndex]);
                
                for (Iterator formatIterator = zoneFormatList.iterator(); formatIterator.hasNext();) {
                    SimpleDateFormat format = (SimpleDateFormat) formatIterator.next();
                    format.setTimeZone(timezone);
                    
                    for (int dateIndex = 0; dateIndex < testDates.length; ++dateIndex) {
                        ++testCount;
                        Date date = testDates[dateIndex];

                        String formattedString = format.format(date);
                        inoutPosition.setIndex(0);
                        outputCalendar.setTimeZone(unknownZone);
                        int badDstOffset = -123;
                        int badZoneOffset = -2345;
                        outputCalendar.set(Calendar.DST_OFFSET, badDstOffset);
                        outputCalendar.set(Calendar.ZONE_OFFSET, badZoneOffset);
                        format.parse(formattedString, outputCalendar, inoutPosition);

//                        // now check for errors
//                        if (pos.getIndex() != formatted.length()) {
//                            // we failed to parse everything
//                            errln(status.getPrefix(locale, timezone, format, formatted)
//                                    + ", but can't parse it at all!");
//                            continue; // don't bother with other tests
//                        }
                        // we must not get "Etc/Unknown" -- that would mean
                        // that we didn't really get the timezone, and the old value was left
                        TimeZone parsedZone = outputCalendar.getTimeZone();
                        // parsedZone.getID().equals(unknownZone.getID())
                        if (outputCalendar.get(Calendar.DST_OFFSET) == badDstOffset
                                || outputCalendar.get(Calendar.ZONE_OFFSET) == badZoneOffset) {
                            errln(status.getPrefix(locale, timezone, format, formattedString)
                                    + ", but when parsed, the zone isn't retrieved.");
                            continue; // don't bother with other tests
                        }
                        if (mustRoundTrip.contains(format.toPattern())) {
                            // also make sure that we roundtrip when we must
                            if (parsedZone.getID().equals(unknownZone.getID())) { 
                                errln(status.getPrefix(locale, timezone, format, formattedString)
                                        + ", but when parsed, got no zone.");
                                continue; // don't bother with other tests
                            }
                            if (!equivalents.contains(parsedZone.getID())) { 
                                // also make sure that we roundtrip when we must
                                errln(status.getPrefix(locale, timezone, format, formattedString)
                                        + ", but when parsed, a non-equivalent zone is retrieved: "
                                        + parsedZone.getID()
                                        + "; equivalents are: " + equivalents);
                                continue; // don't bother with other tests
                            }
                        }   
                        status.succeed(locale, timezone, format);
                    }
                }
            }
        }
       logln("total tests: " + testCount);
       status.log(getLogPrintWriter());
    }

    static class ZoneStatus {
        Map localeFailures = new TreeMap();
        Map formatFailures = new TreeMap();
        Map zoneFailures = new TreeMap();
        Map localeOk = new TreeMap();
        Map formatOk = new TreeMap();
        Map zoneOk = new TreeMap();

        private String getPrefix(ULocale locale, TimeZone timezone,
                SimpleDateFormat format, String formatted) {
            bumpCount(localeFailures, locale.toString(), 1);
            bumpCount(formatFailures, format.toPattern(), 1);
            bumpCount(zoneFailures, timezone.getID(), 1);
            return locale + ": Created \"" + formatted + "\" with \"" + format.toPattern()
                    + "\" and " + timezone.getID();
        }
        
        void succeed(ULocale locale, TimeZone timezone, SimpleDateFormat format) {
            bumpCount(localeOk, locale.toString(), 1);
            bumpCount(formatOk, format.toPattern(), 1);
            bumpCount(zoneOk, timezone.getID(), 1);
        }
        
        void log(PrintWriter out) {
            writeCounts(out, "Locales", localeFailures, localeOk);
            writeCounts(out, "Formats", formatFailures, formatOk);
            writeCounts(out, "Zones", zoneFailures, zoneOk);
        }

        private void writeCounts(PrintWriter out, String title, Map fail, Map ok) {
            out.println("Succeed/Fail for types: " + title);
            Set all = new TreeSet();
            all.addAll(fail.keySet());
            all.addAll(ok.keySet());
            for (Iterator it = all.iterator(); it.hasNext();) {
                Object key = it.next();
                Object failCount = fail.get(key);
                Object okCount = ok.get(key);
                out.println("\t" + key + ":\tok:\t" + (okCount == null ? "0" : okCount) + "\tfail: " + (failCount == null ? "0" : failCount) );
            }
        }
    }
    
    static void bumpCount(Map map, Object key, int delta) {
        Integer count = (Integer) map.get(key);
        map.put(key, new Integer(count == null ? delta : count.intValue() + delta));
    }
}