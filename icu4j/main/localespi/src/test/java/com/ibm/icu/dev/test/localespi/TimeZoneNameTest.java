// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.TimeZoneNames;
import com.ibm.icu.text.TimeZoneNames.NameType;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class TimeZoneNameTest extends TestFmwk {

    private static final Set<String> ProblematicZones = new HashSet<>();
    static {
        // Since tzdata2013e, Pacific/Johnston is defined as below:
        //
        //     Link Pacific/Honolulu Pacific/Johnston
        //
        // JDK TimeZone.getDisplayName no longer passes Pacific/Johnston to a
        // TimeZoneNameProvider implementation. As of CLDR 25M1, Pacific/Johnston
        // has a different set of names from Pacific/Honolulu. This test case
        // expects JRE calls a TimeZoneNameProvider without such normalization
        // (and I believe it's a JDK bug). For now, we ignore the test failure
        // caused by Pacific/Johnston with this the JDK problem.
        ProblematicZones.add("Pacific/Johnston");
    }

    @Test
    public void TestTimeZoneNames() {
        Locale[] locales = Locale.getAvailableLocales();
        String[] tzids = TimeZone.getAvailableIDs();

        for (Locale loc : locales) {
            boolean warningOnly = false;
            if (TestUtil.isExcluded(loc)) {
                warningOnly = true;
            }

            for (String tzid : tzids) {
                // Java has a problem when a provider does not supply all 4 names
                // for a zone. For this reason, ICU TimeZoneName provider does not return
                // localized names unless these 4 names are available.

                String icuStdLong = getIcuDisplayName(tzid, false, TimeZone.LONG, loc);
                String icuDstLong = getIcuDisplayName(tzid, true, TimeZone.LONG, loc);
                String icuStdShort = getIcuDisplayName(tzid, false, TimeZone.SHORT, loc);
                String icuDstShort = getIcuDisplayName(tzid, true, TimeZone.SHORT, loc);

                if (icuStdLong != null && icuDstLong != null && icuStdShort != null && icuDstShort != null) {
                    checkDisplayNamePair(TimeZone.SHORT, tzid, loc, warningOnly || ProblematicZones.contains(tzid));
                    checkDisplayNamePair(TimeZone.LONG, tzid, loc, warningOnly || ProblematicZones.contains(tzid));
                } else {
                    logln("Localized long standard name is not available for "
                            + tzid + " in locale " + loc + " in ICU");
                }
            }
        }
    }

    private void checkDisplayNamePair(int style, String tzid, Locale loc, boolean warnOnly) {
        /* Note: There are two problems here.
         *
         * It looks Java 6 requires a TimeZoneNameProvider to return both standard name and daylight name
         * for a zone.  If the provider implementation only returns either of them, Java 6 also ignore
         * the other.  In ICU, there are zones which do not have daylight names, especially zones which
         * do not use daylight time.  This test case does not check a standard name if its daylight name
         * is not available because of the Java 6 implementation problem.
         *
         * Another problem is that ICU always use a standard name for a zone which does not use daylight
         * saving time even daylight name is requested.
         */

        String icuStdName = getIcuDisplayName(tzid, false, style, loc);
        String icuDstName = getIcuDisplayName(tzid, true, style, loc);
        if (icuStdName != null && icuDstName != null && !icuStdName.equals(icuDstName)) {
            checkDisplayName(false, style, tzid, loc, icuStdName, warnOnly);
            checkDisplayName(true, style, tzid, loc, icuDstName, warnOnly);
        }
    }

    private String getIcuDisplayName(String tzid, boolean daylight, int style, Locale loc) {
        String icuName = null;
        boolean[] isSystemID = new boolean[1];
        String canonicalID = com.ibm.icu.util.TimeZone.getCanonicalID(tzid, isSystemID);
        if (isSystemID[0]) {
            long date = System.currentTimeMillis();
            TimeZoneNames tznames = TimeZoneNames.getInstance(ULocale.forLocale(loc));
            switch (style) {
            case TimeZone.LONG:
                icuName = daylight ?
                        tznames.getDisplayName(canonicalID, NameType.LONG_DAYLIGHT, date) :
                        tznames.getDisplayName(canonicalID, NameType.LONG_STANDARD, date);
                break;
            case TimeZone.SHORT:
                icuName = daylight ?
                        tznames.getDisplayName(canonicalID, NameType.SHORT_DAYLIGHT, date) :
                        tznames.getDisplayName(canonicalID, NameType.SHORT_STANDARD, date);
                break;
            }
        }
        return icuName;
    }

    private void checkDisplayName(boolean daylight, int style,  String tzid, Locale loc, String icuname, boolean warnOnly) {
        String styleStr = (style == TimeZone.SHORT) ? "SHORT" : "LONG";
        TimeZone tz = TimeZone.getTimeZone(tzid);
        String name = tz.getDisplayName(daylight, style, loc);

        if (TestUtil.isICUExtendedLocale(loc)) {
            // The name should be taken from ICU
            if (!name.equals(icuname)) {
                if (warnOnly) {
                    logln("WARNING: TimeZone name by ICU is " + icuname + ", but got " + name
                            + " for time zone " + tz.getID() + " in locale " + loc
                            + " (daylight=" + daylight + ", style=" + styleStr + ")");

                } else {
                    errln("FAIL: TimeZone name by ICU is " + icuname + ", but got " + name
                            + " for time zone " + tz.getID() + " in locale " + loc
                            + " (daylight=" + daylight + ", style=" + styleStr + ")");
                }
            }
        } else {
            if (!name.equals(icuname)) {
                logln("INFO: TimeZone name by ICU is " + icuname + ", but got " + name
                        + " for time zone " + tz.getID() + " in locale " + loc
                        + " (daylight=" + daylight + ", style=" + styleStr + ")");
            }
            // Try explicit ICU locale (xx_yy_ICU)
            Locale icuLoc = TestUtil.toICUExtendedLocale(loc);
            name = tz.getDisplayName(daylight, style, icuLoc);
            if (!name.equals(icuname)) {
                if (warnOnly) {
                    logln("WARNING: TimeZone name by ICU is " + icuname + ", but got " + name
                            + " for time zone " + tz.getID() + " in locale " + icuLoc
                            + " (daylight=" + daylight + ", style=" + styleStr + ")");
                } else {
                    errln("FAIL: TimeZone name by ICU is " + icuname + ", but got " + name
                            + " for time zone " + tz.getID() + " in locale " + icuLoc
                            + " (daylight=" + daylight + ", style=" + styleStr + ")");
                }
            }
        }
    }

    @Test
    public void testGetInstance_Locale() {
        TimeZoneNames uLocaleInstance = TimeZoneNames.getInstance(ULocale.CANADA);
        TimeZoneNames localeInstance = TimeZoneNames.getInstance(Locale.CANADA);

        Set<String> uLocaleAvailableIds = uLocaleInstance.getAvailableMetaZoneIDs();
        Set<String> localeAvailableIds = localeInstance.getAvailableMetaZoneIDs();
        assertEquals("Available ids", uLocaleAvailableIds, localeAvailableIds);

        for (String availableId : uLocaleAvailableIds) {
            long date = 1458385200000L;
            TimeZoneNames.NameType nameType = TimeZoneNames.NameType.SHORT_GENERIC;
            String uLocaleName = uLocaleInstance.getDisplayName(availableId, nameType, date);
            String localeName = localeInstance.getDisplayName(availableId, nameType, date);
            assertEquals("Id: " + availableId, uLocaleName, localeName);
        }
    }

    @Test
    public void testGetAvailableMetaZoneIDs() {
        TimeZoneNames japaneseNames = TimeZoneNames.getInstance(ULocale.JAPANESE);
        Set<String> allJapan = japaneseNames.getAvailableMetaZoneIDs();

        TimeZoneNames tzdbNames = TimeZoneNames.getTZDBInstance(ULocale.CHINESE);
        Set<String> tzdbAll = tzdbNames.getAvailableMetaZoneIDs();

        // The data is the same in the current implementation.
        assertEquals("MetaZone IDs different between locales", allJapan, tzdbAll);

        // Make sure that there is something.
        assertTrue("count of zone ids is less than 100", allJapan.size() >= 180);
    }

    @Test
    public void testGetAvailableMetaZoneIDs_String() {
        TimeZoneNames japaneseNames = TimeZoneNames.getInstance(ULocale.JAPANESE);
        assertEquals("Timezone name mismatch", Collections.singleton("America_Pacific"),
                japaneseNames.getAvailableMetaZoneIDs("America/Los_Angeles"));

        TimeZoneNames tzdbNames = TimeZoneNames.getTZDBInstance(ULocale.CHINESE);
        assertEquals("Timezone name mismatch", Collections.singleton("Taipei"),
                tzdbNames.getAvailableMetaZoneIDs("Asia/Taipei"));
    }

    @Test
    public void testGetMetaZoneDisplayName() {
        TimeZoneNames usNames = TimeZoneNames.getInstance(ULocale.US);

        String europeanCentralName = usNames.getMetaZoneDisplayName("Europe_Central",
                TimeZoneNames.NameType.LONG_STANDARD);
        assertEquals("Timezone name mismatch", "Central European Standard Time",
                europeanCentralName);

        TimeZoneNames tzdbNames = TimeZoneNames.getTZDBInstance(ULocale.CHINESE);
        String americaPacificName = tzdbNames.getMetaZoneDisplayName("America_Pacific",
                TimeZoneNames.NameType.SHORT_DAYLIGHT);
        assertEquals("Timezone name mismatch", "PDT", americaPacificName);
    }

    @Test
    public void testGetMetaZoneID() {
        TimeZoneNames usNames = TimeZoneNames.getInstance(ULocale.US);

        String europeanCentralName = usNames.getMetaZoneID("Europe/Paris", 0);
        assertEquals("Timezone name mismatch", "Europe_Central", europeanCentralName);

        TimeZoneNames tzdbNames = TimeZoneNames.getTZDBInstance(ULocale.KOREAN);
        String seoulName = tzdbNames.getMetaZoneID("Asia/Seoul", 0);
        assertEquals("Timezone name mismatch", "Korea", seoulName);

        // Now try Jan 1st 1945 GMT
        seoulName = tzdbNames.getMetaZoneID("Asia/Seoul", -786240000000L);
        assertNull("Timezone name mismatch", seoulName);
    }

    @Test
    public void testGetTimeZoneDisplayName() {
        TimeZoneNames frenchNames = TimeZoneNames.getInstance(ULocale.FRENCH);
        String dublinName = frenchNames.getTimeZoneDisplayName("Europe/Dublin",
                TimeZoneNames.NameType.LONG_DAYLIGHT);
        assertEquals("Timezone name mismatch", "heure d’été irlandaise", dublinName);

        String dublinLocation = frenchNames.getTimeZoneDisplayName("Europe/Dublin",
                TimeZoneNames.NameType.EXEMPLAR_LOCATION);
        assertEquals("Timezone name mismatch", "Dublin", dublinLocation);

        // All the names returned by this are null.
        TimeZoneNames tzdbNames = TimeZoneNames.getTZDBInstance(ULocale.KOREAN);
        for (String tzId : TimeZone.getAvailableIDs()) {
            for (TimeZoneNames.NameType nameType : TimeZoneNames.NameType.values()) {
                String name = tzdbNames.getTimeZoneDisplayName(tzId, nameType);
                assertNull("TZ:" + tzId + ", NameType: " + nameType + ", value: " + name, name);
            }
        }
    }
}
