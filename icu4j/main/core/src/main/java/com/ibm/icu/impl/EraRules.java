// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

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
    private int numEras;
    private int currentEra;

    private EraRules(int[] startDates, int numEras) {
        this.startDates = startDates;
        this.numEras = numEras;
        initCurrentEra();
    }

    public static EraRules getInstance(CalType calType, boolean includeTentativeEra) {
        UResourceBundle supplementalDataRes = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME,
                "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle calendarDataRes = supplementalDataRes.get("calendarData");
        UResourceBundle calendarTypeRes = calendarDataRes.get(calType.getId());
        UResourceBundle erasRes = calendarTypeRes.get("eras");

        int numEras = erasRes.getSize();
        int firstTentativeIdx = Integer.MAX_VALUE; // first tentative era index
        int[] startDates = new int[numEras];

        UResourceBundleIterator itr = erasRes.getIterator();
        while (itr.hasNext()) {
            UResourceBundle eraRuleRes = itr.next();
            String eraIdxStr = eraRuleRes.getKey();
            int eraIdx = -1;
            try {
                eraIdx = Integer.parseInt(eraIdxStr);
            } catch (NumberFormatException e) {
                throw new ICUException("Invalid era rule key:" + eraIdxStr + " in era rule data for " + calType.getId());
            }
            if (eraIdx < 0 || eraIdx >= numEras) {
                throw new ICUException("Era rule key:" + eraIdxStr + " in era rule data for " + calType.getId()
                        + " must be in range [0, " + (numEras - 1) + "]");
            }
            if (isSet(startDates[eraIdx])) {
                throw new ICUException(
                        "Duplicated era rule for rule key:" + eraIdxStr + " in era rule data for " + calType.getId());
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
                                + calType.getId());
                    }
                    startDates[eraIdx] = encodeDate(fields[0], fields[1], fields[2]);
                } else if (key.equals("named")) {
                    String val = res.getString();
                    if (val.equals("false")) {
                        hasName = false;
                    }
                } else if (key.equals("end")) {
                    hasEnd = true;
                }
            }
            if (isSet(startDates[eraIdx])) {
                if (hasEnd) {
                    // This implementation assumes either start or end is available, not both.
                    // For now, just ignore the end rule.
                }
            } else {
                if (hasEnd) {
                    if (eraIdx != 0) {
                        // This implementation does not support end only rule for eras other than
                        // the first one.
                        throw new ICUException(
                                "Era data for " + eraIdxStr + " in era rule data for " + calType.getId()
                                + " has only end rule.");
                    }
                    startDates[eraIdx] = MIN_ENCODED_START;
                } else {
                    throw new ICUException("Missing era start/end rule date for key:" + eraIdxStr + " in era rule data for "
                            + calType.getId());
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

        if (firstTentativeIdx < Integer.MAX_VALUE && !includeTentativeEra) {
            return new EraRules(startDates, firstTentativeIdx);
        }

        return new EraRules(startDates, numEras);
    }

    /**
     * Gets number of effective eras
     * @return  number of effective eras
     */
    public int getNumberOfEras() {
        return numEras;
    }

    /**
     * Gets start date of an era
     * @param eraIdx    Era index
     * @param fillIn    Receives date fields if supplied. If null, or size of array
     *                  is less than 3, then a new int[] will be newly allocated.
     * @return  An int array including values of year, month, day of month in this order.
     *          When an era has no start date, the result will be January 1st in year
     *          whose value is minimum integer.
     */
    public int[] getStartDate(int eraIdx, int[] fillIn) {
        if (eraIdx < 0 || eraIdx >= numEras) {
            throw new IllegalArgumentException("eraIdx is out of range");
        }
        return decodeDate(startDates[eraIdx], fillIn);
    }

    /**
     * Gets start year of an era
     * @param eraIdx    Era index
     * @return  The first year of an era. When a era has no start date, minimum integer
     *          value is returned.
     */
    public int getStartYear(int eraIdx) {
        if (eraIdx < 0 || eraIdx >= numEras) {
            throw new IllegalArgumentException("eraIdx is out of range");
        }
        int[] fields = decodeDate(startDates[eraIdx], null);
        return fields[0];
    }

    /**
     * Returns era index for the specified year/month/day.
     * @param year  Year
     * @param month Month (1-base)
     * @param day   Day of month
     * @return  era index (or 0, when the specified date is before the first era)
     */
    public int getEraIndex(int year, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Illegal date - year:" + year + "month:" + month + "day:" + day);
        }
        int high = numEras; // last index + 1
        int low;

        // Short circuit for recent years.  Most modern computations will
        // occur in the last few eras.
        if (compareEncodedDateWithYMD(startDates[getCurrentEraIndex()], year, month, day) <= 0) {
            low = getCurrentEraIndex();
        } else {
            low = 0;
        }

        // Do binary search
        while (low < high - 1) {
            int i = (low + high) / 2;
            if (compareEncodedDateWithYMD(startDates[i], year, month, day) <= 0) {
                low = i;
            } else {
                high = i;
            }
        }
        return low;
    }

    /**
     * Gets the current era index. This is calculated only once for an instance of
     * EraRules. The current era calculation is based on the default time zone at
     * the time of instantiation.
     *
     * @return era index of current era (or 0, when current date is before the first era)
     */
    public int getCurrentEraIndex() {
        return currentEra;
    }

    private void initCurrentEra() {
        long localMillis = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        localMillis += zone.getOffset(localMillis);

        int[] fields = Grego.timeToFields(localMillis, null);
        int currentEncodedDate = encodeDate(fields[0], fields[1] + 1 /* changes to 1-base */, fields[2]);
        int eraIdx = numEras - 1;
        while (eraIdx > 0) {
            if (currentEncodedDate >= startDates[eraIdx]) {
                break;
            }
            eraIdx--;
        }
        // Note: current era could be before the first era.
        // In this case, this implementation returns the first era index (0).
        currentEra = eraIdx;
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
