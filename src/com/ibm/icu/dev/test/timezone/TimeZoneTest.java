/**
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * @test 1.22 99/09/21
 * @bug 4028006 4044013 4096694 4107276 4107570 4112869 4130885
 * @summary test TimeZone
 * @build TimeZoneTest
 */

package com.ibm.icu.dev.test.timezone;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.lang.reflect.InvocationTargetException;

public class TimeZoneTest extends TestFmwk
{
    static final int millisPerHour = 3600000;

    public static void main(String[] args) throws Exception {
        new TimeZoneTest().run(args);
    }

    /**
     * NOTE: As of ICU 2.8, the mapping of 3-letter legacy aliases
     * to `real' Olson IDs is under control of the underlying JDK.
     * This test may fail on one JDK and pass on another; don't be
     * too concerned.  Alan
     *
     * Bug 4130885
     * Certain short zone IDs, used since 1.1.x, are incorrect.
     *  
     * The worst of these is:
     *
     * "CAT" (Central African Time) should be GMT+2:00, but instead returns a
     * zone at GMT-1:00. The zone at GMT-1:00 should be called EGT, CVT, EGST,
     * or AZOST, depending on which zone is meant, but in no case is it CAT.
     *
     * Other wrong zone IDs:
     *
     * ECT (European Central Time) GMT+1:00: ECT is Ecuador Time,
     * GMT-5:00. European Central time is abbreviated CEST.
     *
     * SST (Solomon Island Time) GMT+11:00. SST is actually Samoa Standard Time,
     * GMT-11:00. Solomon Island time is SBT.
     *
     * NST (New Zealand Time) GMT+12:00. NST is the abbreviation for
     * Newfoundland Standard Time, GMT-3:30. New Zealanders use NZST.
     *
     * AST (Alaska Standard Time) GMT-9:00. [This has already been noted in
     * another bug.] It should be "AKST". AST is Atlantic Standard Time,
     * GMT-4:00.
     *
     * PNT (Phoenix Time) GMT-7:00. PNT usually means Pitcairn Time,
     * GMT-8:30. There is no standard abbreviation for Phoenix time, as distinct
     * from MST with daylight savings.
     *
     * In addition to these problems, a number of zones are FAKE. That is, they
     * don't match what people use in the real world.
     *
     * FAKE zones:
     *
     * EET (should be EEST)
     * ART (should be EEST)
     * MET (should be IRST)
     * NET (should be AMST)
     * PLT (should be PKT)
     * BST (should be BDT)
     * VST (should be ICT)
     * CTT (should be CST) +
     * ACT (should be CST) +
     * AET (should be EST) +
     * MIT (should be WST) +
     * IET (should be EST) +
     * PRT (should be AST) +
     * CNT (should be NST)
     * AGT (should be ARST)
     * BET (should be EST) +
     *
     * + A zone with the correct name already exists and means something
     * else. E.g., EST usually indicates the US Eastern zone, so it cannot be
     * used for Brazil (BET).
     */
    public void TestShortZoneIDs() throws Exception {

        ZoneDescriptor[] JDK_116_REFERENCE_LIST = {
            new ZoneDescriptor("MIT", -660, false),
            new ZoneDescriptor("HST", -600, false),
            new ZoneDescriptor("AST", -540, true),
            new ZoneDescriptor("PST", -480, true),
            new ZoneDescriptor("PNT", -420, false),
            new ZoneDescriptor("MST", -420, true),
            new ZoneDescriptor("CST", -360, true),
            new ZoneDescriptor("IET", -300, false),
            new ZoneDescriptor("EST", -300, true),
            new ZoneDescriptor("PRT", -240, false),
            new ZoneDescriptor("CNT", -210, true),
            new ZoneDescriptor("AGT", -180, false),
            new ZoneDescriptor("BET", -180, true),
            // new ZoneDescriptor("CAT", -60, false), // Wrong:
            // As of bug 4130885, fix CAT (Central Africa)
            new ZoneDescriptor("CAT", 120, false), // Africa/Harare
            new ZoneDescriptor("GMT", 0, false),
            new ZoneDescriptor("UTC", 0, false),
            new ZoneDescriptor("ECT", 60, true),
            new ZoneDescriptor("ART", 120, true),
            new ZoneDescriptor("EET", 120, true),
            new ZoneDescriptor("EAT", 180, false),
            // new ZoneDescriptor("MET", 210, true),
            // This is a standard Unix zone, so don't remap it - Liu 3Jan01
            // new ZoneDescriptor("NET", 240, false);
            // As of bug 4191164, fix NET
            new ZoneDescriptor("NET", 240, true),
            // PLT behaves differently under different JDKs, so we don't check it
            // new ZoneDescriptor("PLT", 300, false), // updated Oct 2003 aliu
            new ZoneDescriptor("IST", 330, false),
            new ZoneDescriptor("BST", 360, false),
            new ZoneDescriptor("VST", 420, false),
            new ZoneDescriptor("CTT", 480, false), // updated Oct 2003 aliu
            new ZoneDescriptor("JST", 540, false),
            new ZoneDescriptor("ACT", 570, false), // updated Oct 2003 aliu
            new ZoneDescriptor("AET", 600, true),
            new ZoneDescriptor("SST", 660, false),
            // new ZoneDescriptor("NST", 720, false),
            // As of bug 4130885, fix NST (New Zealand)
            new ZoneDescriptor("NST", 720, true), // Pacific/Auckland

            // [3Jan01 Liu] Three of these zones have been updated.
            // The CTT and ACT zones just remap to Asia/Shanghai
            // and Australia/Darwin.  Since those zones have changed,
            // I have updated the table.  The MET zone used to be mapped
            // to Asia/Tehran but since MET is a standard Unix zone named
            // in the source data we no longer do this in icu or icu4j.
        };

        Hashtable hash = new Hashtable();

        String[] ids = TimeZone.getAvailableIDs();
        for (int i=0; i<ids.length; ++i) {
            String id = ids[i];
            if (id.length() == 3) {
                hash.put(id, new ZoneDescriptor(TimeZone.getTimeZone(id)));
            }
        }

        for (int i=0; i<JDK_116_REFERENCE_LIST.length; ++i) {
            ZoneDescriptor referenceZone = JDK_116_REFERENCE_LIST[i];
            ZoneDescriptor currentZone = (ZoneDescriptor)hash.get(referenceZone.getID());
            if (referenceZone.equals(currentZone)) {
                logln("ok " + referenceZone);
            }
            else {
                errln("Fail: Expected " + referenceZone +
                      "; got " + currentZone);
            }
        }
    }

    /**
     * A descriptor for a zone; used to regress the short zone IDs.
     */
    static class ZoneDescriptor {
        String id;
        int offset; // In minutes
        boolean daylight;

        ZoneDescriptor(TimeZone zone) {
            this.id = zone.getID();
            this.offset = zone.getRawOffset() / 60000;
            this.daylight = zone.useDaylightTime();
        }

        ZoneDescriptor(String id, int offset, boolean daylight) {
            this.id = id;
            this.offset = offset;
            this.daylight = daylight;
        }

        public String getID() { return id; }

        public boolean equals(Object o) {
            ZoneDescriptor that = (ZoneDescriptor)o;
            return that != null &&
                id.equals(that.id) &&
                offset == that.offset &&
                daylight == that.daylight;
        }

        public String toString() {
            int min = offset;
            char sign = '+';
            if (min < 0) { sign = '-'; min = -min; }

            return "Zone[\"" + id + "\", GMT" + sign + (min/60) + ':' +
                (min%60<10?"0":"") + (min%60) + ", " +
                (daylight ? "Daylight" : "Standard") + "]";
        }

        public static int compare(Object o1, Object o2) {
            ZoneDescriptor i1 = (ZoneDescriptor)o1;
            ZoneDescriptor i2 = (ZoneDescriptor)o2;
            if (i1.offset > i2.offset) return 1;
            if (i1.offset < i2.offset) return -1;
            if (i1.daylight && !i2.daylight) return 1;
            if (!i1.daylight && i2.daylight) return -1;
            return i1.id.compareTo(i2.id);
        }
    }

    static final String EXPECTED_CUSTOM_ID = "Custom";
    static final String formatMinutes(int min) {
        char sign = '+';
        if (min < 0) { sign = '-'; min = -min; }
        int h = min/60;
        min = min%60;
        return "" + sign + (h<10?"0":"") + h + ":" + (min<10?"0":"") + min;
    }
    /**
     * As part of the VM fix (see CCC approved RFE 4028006, bug
     * 4044013), TimeZone.getTimeZone() has been modified to recognize
     * generic IDs of the form GMT[+-]hh:mm, GMT[+-]hhmm, and
     * GMT[+-]hh.  Test this behavior here.
     *
     * Bug 4044013
     */
    public void TestCustomParse() {
        Object[] DATA = {
            // ID        Expected offset in minutes
            "GMT",       null,
            // "GMT0",      null, // This is parsed by some JDKs (Sun 1.4.1), but not by others
            "GMT+0",     new Integer(0),
            "GMT+1",     new Integer(60),
            "GMT-0030",  new Integer(-30),
            // Parsed in 1.3, parse failure in 1.4:
            //"GMT+15:99", new Integer(15*60+99),
            "GMT+",      null,
            "GMT-",      null,
            "GMT+0:",    null,
            "GMT-:",     null,
            "GMT+0010",  new Integer(10), // Interpret this as 00:10
            "GMT-10",    new Integer(-10*60),
            // Parsed in 1.3, parse failure in 1.4:
            //"GMT+30",    new Integer(30),
            "GMT-3:30",  new Integer(-(3*60+30)),
            "GMT-230",   new Integer(-(2*60+30)),
        };
        for (int i=0; i<DATA.length; i+=2) {
            String id = (String)DATA[i];
            Integer exp = (Integer)DATA[i+1];
            TimeZone zone = TimeZone.getTimeZone(id);
            if (zone.getID().equals("GMT")) {
                logln(id + " -> generic GMT");
                // When TimeZone.getTimeZone() can't parse the id, it
                // returns GMT -- a dubious practice, but required for
                // backward compatibility.
                if (exp != null) {
                    errln("Expected offset of " + formatMinutes(exp.intValue()) +
                          " for " + id + ", got parse failure");
                }
            }
            else {
                int ioffset = zone.getRawOffset()/60000;
                String offset = formatMinutes(ioffset);
                String genID = "GMT"+ offset;
                logln(id + " -> " + zone.getID() + " " + genID);
                if (exp == null) {
                    errln("Expected parse failure for " + id +
                          ", got offset of " + offset +
                          ", id " + zone.getID());
                }
                // JDK 1.3 creates custom zones with the ID "Custom"
                // JDK 1.4 creates custom zones with IDs of the form "GMT+02:00"
                else if (ioffset != exp.intValue() ||
                         !(zone.getID().equals(EXPECTED_CUSTOM_ID) ||
                           zone.getID().equals(genID))) {
                    errln("Expected offset of " + formatMinutes(exp.intValue()) +
                          ", id Custom, for " + id +
                          ", got offset of " + offset +
                          ", id " + zone.getID());
                }
            }
        }
    }

    /**
     * Test the basic functionality of the getDisplayName() API.
     *
     * Bug 4112869
     * Bug 4028006
     *
     * See also API change request A41.
     *
     * 4/21/98 - make smarter, so the test works if the ext resources
     * are present or not.
     */
    public void TestDisplayName() {
        TimeZone zone = TimeZone.getTimeZone("PST");
        String name = zone.getDisplayName(Locale.ENGLISH);
        logln("PST->" + name);
        if (!name.equals("Pacific Standard Time"))
            errln("Fail: Expected \"Pacific Standard Time\", got " + name +
                  " for " + zone);

        //*****************************************************************
        // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
        // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
        // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
        //*****************************************************************
        Object[] DATA = {
            new Boolean(false), new Integer(TimeZone.SHORT), "PST",
            new Boolean(true),  new Integer(TimeZone.SHORT), "PDT",
            new Boolean(false), new Integer(TimeZone.LONG),  "Pacific Standard Time",
            new Boolean(true),  new Integer(TimeZone.LONG),  "Pacific Daylight Time",
        };

        for (int i=0; i<DATA.length; i+=3) {
            name = zone.getDisplayName(((Boolean)DATA[i]).booleanValue(),
                                       ((Integer)DATA[i+1]).intValue(),
                                       Locale.ENGLISH);
            if (!name.equals(DATA[i+2]))
                errln("Fail: Expected " + DATA[i+2] + "; got " + name);
        }

        // Make sure that we don't display the DST name by constructing a fake
        // PST zone that has DST all year long.
        SimpleTimeZone zone2 = new SimpleTimeZone(0, "PST");
        zone2.setStartRule(Calendar.JANUARY, 1, 0);
        zone2.setEndRule(Calendar.DECEMBER, 31, 0);
        logln("Modified PST inDaylightTime->" + zone2.inDaylightTime(new Date()));
        name = zone2.getDisplayName(Locale.ENGLISH);
        logln("Modified PST->" + name);
        if (!name.equals("Pacific Standard Time"))
            errln("Fail: Expected \"Pacific Standard Time\"");

        // Make sure we get the default display format for Locales
        // with no display name data.
        Locale mt_MT = new Locale("mt", "MT");
        name = zone.getDisplayName(mt_MT);
        //*****************************************************************
        // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
        // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
        // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
        //*****************************************************************
        logln("PST(mt_MT)->" + name);

        // Now be smart -- check to see if zh resource is even present.
        // If not, we expect the en fallback behavior.

        // in icu4j 2.1 we know we have the zh_CN locale data, though it's incomplete
//    /"DateFormatZoneData", 
        UResourceBundle enRB = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,Locale.ENGLISH);
        UResourceBundle mtRB = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, mt_MT);
        boolean noZH = enRB == mtRB;

        if (noZH) {
            logln("Warning: Not testing the mt_MT behavior because resource is absent");
            if (!name.equals("Pacific Standard Time"))
                errln("Fail: Expected Pacific Standard Time for PST in mt_MT but got ");
        }
        else if(!name.equals("Pacific Standard Time") &&
            !name.equals("GMT-08:00") &&
            !name.equals("GMT-8:00") &&
            !name.equals("GMT-0800") &&
            !name.equals("GMT-800")) {

            errln("Fail: Expected GMT-08:00 or something similar");
            errln("************************************************************");
            errln("THE ABOVE FAILURE MAY JUST MEAN THE LOCALE DATA HAS CHANGED");
            errln("************************************************************");
        }
        
        // Now try a non-existent zone
        zone2 = new SimpleTimeZone(90*60*1000, "xyzzy");
        name = zone2.getDisplayName(Locale.ENGLISH);
        logln("GMT+90min->" + name);
        if (!name.equals("GMT+01:30") &&
            !name.equals("GMT+1:30") &&
            !name.equals("GMT+0130") &&
            !name.equals("GMT+130"))
            errln("Fail: Expected GMT+01:30 or something similar");
        
        // cover getDisplayName() - null arg
        ULocale save = ULocale.getDefault();
        ULocale.setDefault(ULocale.US);
        name = zone2.getDisplayName();
        logln("GMT+90min->" + name + "for default display locale");
        if (!name.equals("GMT+01:30") &&
            !name.equals("GMT+1:30") &&
            !name.equals("GMT+0130") &&
            !name.equals("GMT+130"))
            errln("Fail: Expected GMT+01:30 or something similar");        
        ULocale.setDefault(save);
    }

    public void TestGenericAPI() {
        String id = "NewGMT";
        int offset = 12345;

        SimpleTimeZone zone = new SimpleTimeZone(offset, id);
        if (zone.useDaylightTime()) errln("FAIL: useDaylightTime should return false");

        TimeZone zoneclone = (TimeZone)zone.clone();
        if (!zoneclone.equals(zone)) errln("FAIL: clone or operator== failed");
        zoneclone.setID("abc");
        if (zoneclone.equals(zone)) errln("FAIL: clone or operator!= failed");
        // delete zoneclone;

        zoneclone = (TimeZone)zone.clone();
        if (!zoneclone.equals(zone)) errln("FAIL: clone or operator== failed");
        zoneclone.setRawOffset(45678);
        if (zoneclone.equals(zone)) errln("FAIL: clone or operator!= failed");

        // C++ only
        /*
          SimpleTimeZone copy(*zone);
          if (!(copy == *zone)) errln("FAIL: copy constructor or operator== failed");
          copy = *(SimpleTimeZone*)zoneclone;
          if (!(copy == *zoneclone)) errln("FAIL: assignment operator or operator== failed");
          */

        TimeZone saveDefault = TimeZone.getDefault();
        TimeZone.setDefault(zone);
        TimeZone defaultzone = TimeZone.getDefault();
        if (defaultzone == zone) errln("FAIL: Default object is identical, not clone");
        if (!defaultzone.equals(zone)) errln("FAIL: Default object is not equal");
        TimeZone.setDefault(saveDefault);
        // delete defaultzone;
        // delete zoneclone;

//      // ICU 2.6 Coverage
//      logln(zone.toString());
//      logln(zone.getDisplayName());
//      SimpleTimeZoneAdapter stza = new SimpleTimeZoneAdapter((SimpleTimeZone) TimeZone.getTimeZone("GMT"));
//      stza.setID("Foo");
//      if (stza.hasSameRules(java.util.TimeZone.getTimeZone("GMT"))) {
//          errln("FAIL: SimpleTimeZoneAdapter.hasSameRules");
//      }
//      stza.setRawOffset(3000);
//      offset = stza.getOffset(GregorianCalendar.BC, 2001, Calendar.DECEMBER,
//                              25, Calendar.TUESDAY, 12*60*60*1000);
//      if (offset != 3000) {
//          errln("FAIL: SimpleTimeZoneAdapter.getOffset");
//      }
//      SimpleTimeZoneAdapter dup = (SimpleTimeZoneAdapter) stza.clone();
//      if (stza.hashCode() != dup.hashCode()) {
//          errln("FAIL: SimpleTimeZoneAdapter.hashCode");
//      }
//      if (!stza.equals(dup)) {
//          errln("FAIL: SimpleTimeZoneAdapter.equals");
//      }
//      logln(stza.toString());
    }

    public void TestRuleAPI()
    {
        // ErrorCode status = ZERO_ERROR;

        int offset = (int)(60*60*1000*1.75); // Pick a weird offset
        SimpleTimeZone zone = new SimpleTimeZone(offset, "TestZone");
        if (zone.useDaylightTime()) errln("FAIL: useDaylightTime should return false");

        // Establish our expected transition times.  Do this with a non-DST
        // calendar with the (above) declared local offset.
        GregorianCalendar gc = new GregorianCalendar(zone);
        gc.clear();
        gc.set(1990, Calendar.MARCH, 1);
        long marchOneStd = gc.getTime().getTime(); // Local Std time midnight
        gc.clear();
        gc.set(1990, Calendar.JULY, 1);
        long julyOneStd = gc.getTime().getTime(); // Local Std time midnight

        // Starting and ending hours, WALL TIME
        int startHour = (int)(2.25 * 3600000);
        int endHour   = (int)(3.5  * 3600000);

        zone.setStartRule(Calendar.MARCH, 1, 0, startHour);
        zone.setEndRule  (Calendar.JULY,  1, 0, endHour);

        gc = new GregorianCalendar(zone);
        // if (failure(status, "new GregorianCalendar")) return;

        long marchOne = marchOneStd + startHour;
        long julyOne = julyOneStd + endHour - 3600000; // Adjust from wall to Std time

        long expMarchOne = 636251400000L;
        if (marchOne != expMarchOne)
        {
            errln("FAIL: Expected start computed as " + marchOne +
                  " = " + new Date(marchOne));
            logln("      Should be                  " + expMarchOne +
                  " = " + new Date(expMarchOne));
        }

        long expJulyOne = 646793100000L;
        if (julyOne != expJulyOne)
        {
            errln("FAIL: Expected start computed as " + julyOne +
                  " = " + new Date(julyOne));
            logln("      Should be                  " + expJulyOne +
                  " = " + new Date(expJulyOne));
        }

        Calendar cal1 = Calendar.getInstance();
        cal1.set(1990, Calendar.JANUARY, 1);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(1990, Calendar.JUNE, 1);
        _testUsingBinarySearch(zone, cal1.getTimeInMillis(),
                               cal2.getTimeInMillis(), marchOne);
        cal1.set(1990, Calendar.JUNE, 1);
        cal2.set(1990, Calendar.DECEMBER, 31);
        _testUsingBinarySearch(zone, cal1.getTimeInMillis(),
                               cal2.getTimeInMillis(), julyOne);

        if (zone.inDaylightTime(new Date(marchOne - 1000)) ||
            !zone.inDaylightTime(new Date(marchOne)))
            errln("FAIL: Start rule broken");
        if (!zone.inDaylightTime(new Date(julyOne - 1000)) ||
            zone.inDaylightTime(new Date(julyOne)))
            errln("FAIL: End rule broken");

        zone.setStartYear(1991);
        if (zone.inDaylightTime(new Date(marchOne)) ||
            zone.inDaylightTime(new Date(julyOne - 1000)))
            errln("FAIL: Start year broken");

        // failure(status, "TestRuleAPI");
        // delete gc;
        // delete zone;
    }

    void _testUsingBinarySearch(SimpleTimeZone tz, long min, long max, long expectedBoundary)
    {
        // ErrorCode status = ZERO_ERROR;
        boolean startsInDST = tz.inDaylightTime(new Date(min));
        // if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
        if (tz.inDaylightTime(new Date(max)) == startsInDST) {
            logln("Error: inDaylightTime(" + new Date(max) + ") != " + (!startsInDST));
            return;
        }
        // if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
        while ((max - min) > INTERVAL) {
            long mid = (min + max) / 2;
            if (tz.inDaylightTime(new Date(mid)) == startsInDST) {
                min = mid;
            }
            else {
                max = mid;
            }
            // if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
        }
        logln("Binary Search Before: " + min + " = " + new Date(min));
        logln("Binary Search After:  " + max + " = " + new Date(max));
        long mindelta = expectedBoundary - min;
        // not used long maxdelta = max - expectedBoundary;
        if (mindelta >= 0 &&
            mindelta <= INTERVAL &&
            mindelta >= 0 &&
            mindelta <= INTERVAL)
            logln("PASS: Expected bdry:  " + expectedBoundary + " = " + new Date(expectedBoundary));
        else
            errln("FAIL: Expected bdry:  " + expectedBoundary + " = " + new Date(expectedBoundary));
    }

    static final int INTERVAL = 100;

    // Bug 006; verify the offset for a specific zone.
    public void TestPRTOffset()
    {
        TimeZone tz = TimeZone.getTimeZone( "PRT" );
        if( tz == null ) {
            errln( "FAIL: TimeZone(PRT) is null" );
        }
        else{
            if (tz.getRawOffset() != (-4*millisPerHour))
                errln("FAIL: Offset for PRT should be -4, got " +
                      tz.getRawOffset() / (double)millisPerHour);
        }

    }

    // Test various calls
    public void TestVariousAPI518()
    {
        TimeZone time_zone = TimeZone.getTimeZone("PST");
        Calendar cal = Calendar.getInstance();
        cal.set(1997, Calendar.APRIL, 30);
        Date d = cal.getTime();

        logln("The timezone is " + time_zone.getID());

        if (time_zone.inDaylightTime(d) != true)
            errln("FAIL: inDaylightTime returned false");

        if (time_zone.useDaylightTime() != true)
            errln("FAIL: useDaylightTime returned false");

        if (time_zone.getRawOffset() != -8*millisPerHour)
            errln( "FAIL: getRawOffset returned wrong value");

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        if (time_zone.getOffset(GregorianCalendar.AD, gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH),
                                gc.get(GregorianCalendar.DAY_OF_MONTH),
                                gc.get(GregorianCalendar.DAY_OF_WEEK), 0)
            != -7*millisPerHour)
            errln("FAIL: getOffset returned wrong value");
    }

    // Test getAvailableID API
    public void TestGetAvailableIDs913()
    {
        StringBuffer buf = new StringBuffer("TimeZone.getAvailableIDs() = { ");
        String[] s = TimeZone.getAvailableIDs();
        for (int i=0; i<s.length; ++i)
        {
            if (i > 0) buf.append(", ");
            buf.append(s[i]);
        }
        buf.append(" };");
        logln(buf.toString());

        buf.setLength(0);
        buf.append("TimeZone.getAvailableIDs(GMT+02:00) = { ");
        s = TimeZone.getAvailableIDs(+2 * 60 * 60 * 1000);
        for (int i=0; i<s.length; ++i)
        {
            if (i > 0) buf.append(", ");
            buf.append(s[i]);
        }
        buf.append(" };");
        logln(buf.toString());

        TimeZone tz = TimeZone.getTimeZone("PST");
        if (tz != null)
            logln("getTimeZone(PST) = " + tz.getID());
        else
            errln("FAIL: getTimeZone(PST) = null");

        tz = TimeZone.getTimeZone("America/Los_Angeles");
        if (tz != null)
            logln("getTimeZone(America/Los_Angeles) = " + tz.getID());
        else
            errln("FAIL: getTimeZone(PST) = null");

        // Bug 4096694
        tz = TimeZone.getTimeZone("NON_EXISTENT");
        if (tz == null)
            errln("FAIL: getTimeZone(NON_EXISTENT) = null");
        else if (!tz.getID().equals("GMT"))
            errln("FAIL: getTimeZone(NON_EXISTENT) = " + tz.getID());
    }

    /**
     * Bug 4107276
     */
    public void TestDSTSavings() {
        // It might be better to find a way to integrate this test into the main TimeZone
        // tests above, but I don't have time to figure out how to do this (or if it's
        // even really a good idea).  Let's consider that a future.  --rtg 1/27/98
        SimpleTimeZone tz = new SimpleTimeZone(-5 * millisPerHour, "dstSavingsTest",
                                               Calendar.MARCH, 1, 0, 0, Calendar.SEPTEMBER, 1, 0, 0,
                                               (int)(0.5 * millisPerHour));

        if (tz.getRawOffset() != -5 * millisPerHour)
            errln("Got back a raw offset of " + (tz.getRawOffset() / millisPerHour) +
                  " hours instead of -5 hours.");
        if (!tz.useDaylightTime())
            errln("Test time zone should use DST but claims it doesn't.");
        if (tz.getDSTSavings() != 0.5 * millisPerHour)
            errln("Set DST offset to 0.5 hour, but got back " + (tz.getDSTSavings() /
                                                                 millisPerHour) + " hours instead.");

        int offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.JANUARY, 1,
                                  Calendar.THURSDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10 AM, 1/1/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.JUNE, 1, Calendar.MONDAY,
                              10 * millisPerHour);
        if (offset != -4.5 * millisPerHour)
            errln("The offset for 10 AM, 6/1/98 should have been -4.5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        tz.setDSTSavings(millisPerHour);
        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.JANUARY, 1,
                              Calendar.THURSDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10 AM, 1/1/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.JUNE, 1, Calendar.MONDAY,
                              10 * millisPerHour);
        if (offset != -4 * millisPerHour)
            errln("The offset for 10 AM, 6/1/98 (with a 1-hour DST offset) should have been -4 hours, but we got "
                  + (offset / millisPerHour) + " hours.");
    }

    /**
     * Bug 4107570
     */
    public void TestAlternateRules() {
        // Like TestDSTSavings, this test should probably be integrated somehow with the main
        // test at the top of this class, but I didn't have time to figure out how to do that.
        //                      --rtg 1/28/98

        SimpleTimeZone tz = new SimpleTimeZone(-5 * millisPerHour, "alternateRuleTest");

        // test the day-of-month API
        tz.setStartRule(Calendar.MARCH, 10, 12 * millisPerHour);
        tz.setEndRule(Calendar.OCTOBER, 20, 12 * millisPerHour);

        int offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.MARCH, 5,
                                  Calendar.THURSDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10AM, 3/5/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.MARCH, 15,
                              Calendar.SUNDAY, 10 * millisPerHour);
        if (offset != -4 * millisPerHour)
            errln("The offset for 10AM, 3/15/98 should have been -4 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.OCTOBER, 15,
                              Calendar.THURSDAY, 10 * millisPerHour);
        if (offset != -4 * millisPerHour)
            errln("The offset for 10AM, 10/15/98 should have been -4 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.OCTOBER, 25,
                              Calendar.SUNDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10AM, 10/25/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        // test the day-of-week-after-day-in-month API
        tz.setStartRule(Calendar.MARCH, 10, Calendar.FRIDAY, 12 * millisPerHour, true);
        tz.setEndRule(Calendar.OCTOBER, 20, Calendar.FRIDAY, 12 * millisPerHour, false);

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.MARCH, 11,
                              Calendar.WEDNESDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10AM, 3/11/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.MARCH, 14,
                              Calendar.SATURDAY, 10 * millisPerHour);
        if (offset != -4 * millisPerHour)
            errln("The offset for 10AM, 3/14/98 should have been -4 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.OCTOBER, 15,
                              Calendar.THURSDAY, 10 * millisPerHour);
        if (offset != -4 * millisPerHour)
            errln("The offset for 10AM, 10/15/98 should have been -4 hours, but we got "
                  + (offset / millisPerHour) + " hours.");

        offset = tz.getOffset(GregorianCalendar.AD, 1998, Calendar.OCTOBER, 17,
                              Calendar.SATURDAY, 10 * millisPerHour);
        if (offset != -5 * millisPerHour)
            errln("The offset for 10AM, 10/17/98 should have been -5 hours, but we got "
                  + (offset / millisPerHour) + " hours.");
    }

    public void TestEquivalencyGroups() {
        String id = "America/Los_Angeles";
        int n = TimeZone.countEquivalentIDs(id);
        if (n < 2) {
            errln("FAIL: countEquivalentIDs(" + id + ") returned " + n +
                  ", expected >= 2");
        }
        for (int i=0; i<n; ++i) {
            String s = TimeZone.getEquivalentID(id, i);
            if (s.length() == 0) {
                errln("FAIL: getEquivalentID(" + id + ", " + i +
                      ") returned \"" + s + "\", expected valid ID");
            } else {
                logln("" + i + ":" + s);
            }
        }
    }

    public void TestCountries() {
        // Make sure America/Los_Angeles is in the "US" group, and
        // Asia/Tokyo isn't.  Vice versa for the "JP" group.

        String[] s = TimeZone.getAvailableIDs("US");
        boolean la = false, tokyo = false;
        String laZone = "America/Los_Angeles", tokyoZone = "Asia/Tokyo";

        for (int i=0; i<s.length; ++i) {
            if (s[i].equals(laZone)) {
                la = true;
            }
            if (s[i].equals(tokyoZone)) {
                tokyo = true;
            }
        }
        if (!la || tokyo) {
            errln("FAIL: " + laZone + " in US = " + la);
            errln("FAIL: " + tokyoZone + " in US = " + tokyo);
        }

        s = TimeZone.getAvailableIDs("JP");
        la = false; tokyo = false;

        for (int i=0; i<s.length; ++i) {
            if (s[i].equals(laZone)) {
                la = true;
            }
            if (s[i].equals(tokyoZone)) {
                tokyo = true;
            }
        }
        if (la || !tokyo) {
            errln("FAIL: " + laZone + " in JP = " + la);
            errln("FAIL: " + tokyoZone + " in JP = " + tokyo);
        }
    }

    public void TestFractionalDST() {
    String tzName = "Australia/Lord_Howe"; // 30 min offset
    java.util.TimeZone tz_java = java.util.TimeZone.getTimeZone(tzName);
    int dst_java = 0;
    try {
        // hack so test compiles and runs in both JDK 1.3 and JDK 1.4
        final Object[] args = new Object[0];
        final Class[] argtypes = new Class[0];
        java.lang.reflect.Method m = tz_java.getClass().getMethod("getDSTSavings", argtypes); 
        dst_java = ((Integer) m.invoke(tz_java, args)).intValue();
        if (dst_java <= 0 || dst_java >= 3600000) { // didn't get the fractional time zone we wanted
        errln("didn't get fractional time zone!");
        }
    } catch (NoSuchMethodException e) {
        // see JDKTimeZone for the reason for this code
        dst_java = 3600000;
    } catch (IllegalAccessException e) {
        // see JDKTimeZone for the reason for this code
        errln(e.getMessage());
        dst_java = 3600000;
    } catch (InvocationTargetException e) {
        // see JDKTimeZone for the reason for this code
        errln(e.getMessage());
        dst_java = 3600000;
    } catch (SecurityException e) {
        warnln(e.getMessage());
        return;
    }
    
    com.ibm.icu.util.TimeZone tz_icu = com.ibm.icu.util.TimeZone.getTimeZone(tzName);
    int dst_icu = tz_icu.getDSTSavings();

    if (dst_java != dst_icu) {
        errln("java reports dst savings of " + dst_java +
          " but icu reports " + dst_icu + 
          " for tz " + tz_icu.getID());
    } else {
        logln("both java and icu report dst savings of " + dst_java + " for tz " + tz_icu.getID());
    }
    }

    public void TestGetOffsetDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, Calendar.JANUARY, 30);
        long date = cal.getTimeInMillis();

    TimeZone tz_icu = TimeZone.getTimeZone("America/Los_Angeles");
    int offset = tz_icu.getOffset(date);
    if (offset != -28800000) {
        errln("expected offset -28800000, got: " + offset);
    }

    cal.set(1997, Calendar.JULY, 30);
    date = cal.getTimeInMillis();
    offset = tz_icu.getOffset(date);
    if (offset != -25200000) {
        errln("expected offset -25200000, got: " + offset);
    }
    }
}

//eof
