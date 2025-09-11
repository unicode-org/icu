// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

/**
 * <code>EraRules</code> represents calendar era rules specified
 * in supplementalData/calendarData.
 *
 * @author Yoshito Umaoka
 */
public class EraRules {
    private static final int MAX_ENCODED_START_YEAR = 32767;
    private static final int MIN_ENCODED_START_YEAR = -32768;

    public static final int MIN_ENCODED_START = encodeDate(MIN_ENCODED_START_YEAR, 1, 1);

    private static final int YEAR_MASK = 0xFFFF0000;
    private static final int MONTH_MASK = 0x0000FF00;
    private static final int DAY_MASK = 0x000000FF;

    private int[] startDates;
    private int minEra;  // minimum valid era code, for first entry in startDates[]
    private int numEras; // number of valid era codes (not necessarily the same as startDates.length)
    private int currentEra;

    private EraRules(int[] startDates, int minEra, int numEras) {
        this.startDates = startDates;
        this.minEra = minEra;
        this.numEras = numEras;
        initCurrentEra();
    }

    public static EraRules getInstance(CalType calType, boolean includeTentativeEra) {
        return getInstance(calType.getId(), includeTentativeEra);
    }

    public static EraRules getInstance(String calId, boolean includeTentativeEra) {
        UResourceBundle supplementalDataRes = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME,
                "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle calendarDataRes = supplementalDataRes.get("calendarData");
        UResourceBundle calendarTypeRes = calendarDataRes.get(calId);
        UResourceBundle erasRes = calendarTypeRes.get("eras");

        int numEras = erasRes.getSize();
        int firstTentativeIdx = Integer.MAX_VALUE; // first tentative era index
        ArrayList<Integer> eraStartDates = new ArrayList<>(numEras);

        UResourceBundleIterator itr = erasRes.getIterator();
        while (itr.hasNext()) {
            UResourceBundle eraRuleRes = itr.next();
            String eraIdxStr = eraRuleRes.getKey();
            int eraIdx = -1;
            try {
                eraIdx = Integer.parseInt(eraIdxStr);
            } catch (NumberFormatException e) {
                throw new ICUException("Invalid era rule key:" + eraIdxStr + " in era rule data for " + calId);
            }
            if (eraIdx < 0) {
                throw new ICUException("Era rule key:" + eraIdxStr + " in era rule data for " + calId
                        + " must be >= 0");
            }
            if (eraIdx + 1 > eraStartDates.size()) {
                eraStartDates.ensureCapacity(eraIdx + 1); // needed only to minimize expansions
                // Fill in zeros for all added slots
                while (eraStartDates.size() < eraIdx + 1) {
                    eraStartDates.add(0);
                }
            }
            // Now set the startDate that we just read
            if (isSet(eraStartDates.get(eraIdx).intValue())) {
                throw new ICUException(
                        "Duplicated era rule for rule key:" + eraIdxStr + " in era rule data for " + calId);
            }

            boolean hasName = true;
            boolean hasEnd = false;
            UResourceBundleIterator ruleItr = eraRuleRes.getIterator();
            while (ruleItr.hasNext()) {
                UResourceBundle res = ruleItr.next();
                String key = res.getKey();
                if (key.equals("start")) {
                    int[] fields = res.getIntVector();
                    if (fields.length != 3 || !isValidRuleStartDate(fields[0], fields[1], fields[2])) {
                        throw new ICUException(
                                "Invalid era rule date data:" + Arrays.toString(fields) + " in era rule data for "
                                + calId);
                    }
                    eraStartDates.set(eraIdx, encodeDate(fields[0], fields[1], fields[2]));
                } else if (key.equals("named")) {
                    String val = res.getString();
                    if (val.equals("false")) {
                        hasName = false;
                    }
                } else if (key.equals("end")) {
                    hasEnd = true;
                }
            }
            if (isSet(eraStartDates.get(eraIdx).intValue())) {
                if (hasEnd) {
                    // This implementation assumes either start or end is available, not both.
                    // For now, just ignore the end rule.
                }
            } else {
                if (hasEnd) {
                    // The islamic calendars now have an end-only rule for the
                    // second (and final) entry; basically they are in reverse order.
                    eraStartDates.set(eraIdx, MIN_ENCODED_START);
                } else {
                    throw new ICUException("Missing era start/end rule date for key:" + eraIdxStr + " in era rule data for "
                            + calId);
                }
            }

            if (hasName) {
                if (eraIdx >= firstTentativeIdx) {
                    throw new ICUException(
                            "Non-tentative era(" + eraIdx + ") must be placed before the first tentative era");
                }
            } else {
                if (eraIdx < firstTentativeIdx) {
                    firstTentativeIdx = eraIdx;
                }
            }
        }

        // Remove from eraStartDates any tentative eras if they should not be included
        // (these would be the last entries). Also reduce numEras appropriately.
        if (!includeTentativeEra) {
            while (firstTentativeIdx < eraStartDates.size()) {
                int lastEraIdx = eraStartDates.size() - 1;
                if (isSet(eraStartDates.get(lastEraIdx))) { // If there are multiple tentativeEras, some may be unset
                    numEras--;
                }
                eraStartDates.remove(lastEraIdx);
            }
            // Remove any remaining trailing unSet entries
            // (can only have these if tentativeEras have been removed)
            while (eraStartDates.size() > 0 && !isSet(eraStartDates.get(eraStartDates.size() - 1))) {
                eraStartDates.remove(eraStartDates.size() - 1);
            }
        }
        // Remove from eraStartDates any initial 0 entries, keeping the original index (eraCode)
        // of the first non-zero entry as minEra; then we can add that back to the offset in the
        // compressed array to get the correct eraCode.
        int minEra = 0;
        while (eraStartDates.size() > 0 && !isSet(eraStartDates.get(0))) {
            eraStartDates.remove(0);
            minEra++;
        }
        // Convert eraStartDates to int[] startDates and pass to EraRules constructor,
        // along with minEra and numEras (which may be different from startDates.length)
        int[] startDates = new int[eraStartDates.size()];
        for (int eraIdx = 0; eraIdx < eraStartDates.size(); eraIdx++) {
            startDates[eraIdx] = eraStartDates.get(eraIdx).intValue();
        };
        return new EraRules(startDates, minEra, numEras);
    }

    /**
     * Gets number of effective eras
     * @return  number of effective eras (not the same as max era code)
     */
    public int getNumberOfEras() {
        return numEras;
    }

    /**
     * Gets maximum defined era code for the current calendar
     * @return  maximum defined era code
     */
    public int getMaxEraCode() {
        return minEra + startDates.length - 1;
    }

    /**
     * Gets start date of an era
     * @param eraCode   Era code
     * @param fillIn    Receives date fields if supplied. If null, or size of array
     *                  is less than 3, then a new int[] will be newly allocated.
     * @return  An int array including values of year, month, day of month in this order.
     *          When an era has no start date, the result will be January 1st in year
     *          whose value is minimum integer.
     */
    public int[] getStartDate(int eraCode, int[] fillIn) {
        int startDate = 0;
        if (eraCode >= minEra) {
            int startIdx = eraCode - minEra;
            if (startIdx < startDates.length) {
                startDate = startDates[startIdx];
            }
        }
        if (isSet(startDate)) {
            return decodeDate(startDate, fillIn);
        }
        // We did not find the requested eraCode in our data
        throw new IllegalArgumentException("eraCode is not found in our data");
    }

    /**
     * Gets start year of an era
     * @param eraCode    Era code
     * @return  The first year of an era. When a era has no start date, minimum integer
     *          value is returned.
     */
    public int getStartYear(int eraCode) {
        int startDate = 0;
        if (eraCode >= minEra) {
            int startIdx = eraCode - minEra;
            if (startIdx < startDates.length) {
                startDate = startDates[startIdx];
            }
        }
        if (isSet(startDate)) {
            int[] fields = decodeDate(startDate, null);
            return fields[0];
        }
        // We did not find the requested eraCode in our data
        throw new IllegalArgumentException("eraCode is not found in our data");
    }

    /**
     * Returns era code for the specified year/month/day.
     * @param year  Year
     * @param month Month (1-base)
     * @param day   Day of month
     * @return  era code (or code of earliest era when date is before that era)
     */
    public int getEraCode(int year, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Illegal date - year:" + year + "month:" + month + "day:" + day);
        }
        if (numEras > 1 && startDates[startDates.length-1] == MIN_ENCODED_START) {
            // Multiple eras in reverse order, linear search from beginning.
            // Currently only for islamic.
            for (int startIdx = 0; startIdx < startDates.length; startIdx++) {
                if (!isSet(startDates[startIdx])) {
                    continue;
                }
                if (compareEncodedDateWithYMD(startDates[startIdx], year, month, day) <= 0) {
                    return minEra + startIdx;
                }
            }
        }
        // Linear search from the end, which should hit the most likely eras first.
        // Also this is the most efficient for any era if we have < 8 or so eras, so only less
        // efficient for early eras in Japanese calendar (while we still have them). Formerly
        // this used binary search which would only be better for those early Japanese eras,
        // but now that is much more difficult since there may be holes in the sorted list.
        // Note with this change, this no longer uses or depends on currentEra.
        for (int startIdx = startDates.length; startIdx > 0;) {
            if (!isSet(startDates[--startIdx])) {
                continue;
            }
            if (compareEncodedDateWithYMD(startDates[startIdx], year, month, day) <= 0) {
                return minEra + startIdx;
            }
        }
        return minEra;
    }

    /**
     * Gets the current era code. This is calculated only once for an instance of
     * EraRules. The current era calculation is based on the default time zone at
     * the time of instantiation.
     *
     * @return era index of current era (or era code of earliest era when current date is before any era)
     */
    public int getCurrentEraCode() {
        return currentEra;
    }

    private void initCurrentEra() {
        long localMillis = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        localMillis += zone.getOffset(localMillis);

        int[] fields = Grego.timeToFields(localMillis, null);
        // Now that getEraCode no longer depends on currentEra, we can just do this:
        currentEra = getEraCode(fields[0], fields[1] + 1 /* changes to 1-base */, fields[2]);
    }

    //
    // private methods
    //

    private static boolean isSet(int startDate) {
        return startDate != 0;
    }

    private static boolean isValidRuleStartDate(int year, int month, int day) {
        return year >= MIN_ENCODED_START_YEAR && year <= MAX_ENCODED_START_YEAR
                && month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    /**
     * Encode year/month/date to a single integer.
     * year is high 16 bits (-32768 to 32767), month is
     * next 8 bits and day of month is last 8 bits.
     *
     * @param year  year
     * @param month month (1-base)
     * @param day   day of month
     * @return  an encoded date.
     */
    private static int encodeDate(int year, int month, int day) {
        return year << 16 | month << 8 | day;
    }

    private static int[] decodeDate(int encodedDate, int[] fillIn) {
        int year, month, day;
        if (encodedDate == MIN_ENCODED_START) {
            year = Integer.MIN_VALUE;
            month = 1;
            day = 1;
        } else {
            year = (encodedDate & YEAR_MASK) >> 16;
            month = (encodedDate & MONTH_MASK) >> 8;
            day = encodedDate & DAY_MASK;
        }

        if (fillIn != null && fillIn.length >= 3) {
            fillIn[0] = year;
            fillIn[1] = month;
            fillIn[2] = day;
            return fillIn;
        }

        int[] result = {year, month, day};
        return result;
    }

    /**
     * Compare an encoded date with another date specified by year/month/day.
     * @param encoded   An encoded date
     * @param year      Year of another date
     * @param month     Month of another date
     * @param day       Day of another date
     * @return -1 when encoded date is earlier, 0 when two dates are same,
     *          and 1 when encoded date is later.
     */
    private static int compareEncodedDateWithYMD(int encoded, int year, int month, int day) {
        if (year < MIN_ENCODED_START_YEAR) {
            if (encoded == MIN_ENCODED_START) {
                if (year > Integer.MIN_VALUE || month > 1 || day > 1) {
                    return -1;
                }
                return 0;
            } else {
                return 1;
            }
        } else if (year > MAX_ENCODED_START_YEAR) {
            return -1;
        } else {
            int tmp = encodeDate(year, month, day);
            if (encoded < tmp) {
                return -1;
            } else if (encoded == tmp) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
