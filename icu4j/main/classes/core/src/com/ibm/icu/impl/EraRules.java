// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

/**
 * <code>EraRules</code> represents calendar era rules specified
 * in supplementalData/calendarData.
 *
 * @author Yoshito Umaoka
 */
public class EraRules {

    public static class RuleDate implements Comparable<RuleDate> {
        static final RuleDate MIN_DATE = new RuleDate(Integer.MIN_VALUE, 1, 1);
        static final RuleDate MAX_DATE = new RuleDate(Integer.MAX_VALUE, 12, 31);

        private final int year;
        private final int month;
        private final int day;

        RuleDate(int year, int month, int day) {
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                throw new IllegalArgumentException("Invalid rule date ["
                        + year + "," + month + "," + day + "]");
            }
            this.year = year;
            this.month = month;
            this.day = day;
        }

        /**
         * @return year, a value could be negative
         */
        public int getYear() {
            return year;
        }

        /**
         * @return month, 1-base (1 to 12)
         */
        public int getMonth() {
            return month;
        }

        /**
         * @return day of month (1 to 31)
         */
        public int getDay() {
            return day;
        }

        /**
         * Compare the rule date held by this <code>RuleDate</code> object to
         * the specified year.
         *
         * @param year  Year
         * @param month Month (1-based)
         * @param day   Day of month
         * @return  comparison result as below:
         *          <ul>
         *              <li>-1: This RuleDate is before the specified date</li>
         *              <li>0: This RuleDate is on the specified date</li>
         *              <li>1: This RuleDate is after the specified date</li>
         *          </ul>
         */
        public int compareTo(int year, int month, int day) {
            if (this.year < year) {
                return -1;
            } else if (this.year > year) {
                return 1;
            }

            if (this.month < month) {
                return -1;
            } else if (this.month > month) {
                return 1;
            }

            if (this.day < day) {
                return -1;
            } else if (this.day > day) {
                return 1;
            }

            return 0;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(RuleDate other) {
            return compareTo(other.getYear(), other.getMonth(), other.getDay());
        }
    }

    private static class EraRule {
        private RuleDate start;
        private RuleDate end;

        EraRule(RuleDate start, RuleDate end) {
            this.start = start;
            this.end = end;
        }

        RuleDate getStart() {
            return start;
        }

        RuleDate getEnd() {
            return end;
        }
    }

    private EraRule[] rules;
    private int numRules;

    public static EraRules getInstance(CalType calType) {
        return getInstance(calType, false);
    }

    public static EraRules getInstance(CalType calType, boolean includeTentativeEra) {
        UResourceBundle supplementalDataRes = UResourceBundle.getBundleInstance(
                ICUData.ICU_BASE_NAME, "supplementalData",
                ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle calendarDataRes = supplementalDataRes.get("calendarData");
        UResourceBundle calendarTypeRes = calendarDataRes.get(calType.getId());
        UResourceBundle erasRes = calendarTypeRes.get("eras");

        int numEras = erasRes.getSize();
        int firstTentativeIdx = Integer.MAX_VALUE;   // first tentative era index
        EraRule[] rules = new EraRule[numEras];
        UResourceBundleIterator itr = erasRes.getIterator();
        while (itr.hasNext()) {
            UResourceBundle eraRuleRes = itr.next();
            String eraIdxStr = eraRuleRes.getKey();
            int eraIdx = -1;
            try {
                eraIdx = Integer.parseInt(eraIdxStr);
            } catch (NumberFormatException e) {
                throw new ICUException("Invald era rule key:" + eraIdxStr + " in era rule data for "
                        + calType.getId());
            }
            if (eraIdx < 0 || eraIdx >= numEras) {
                throw new ICUException("Era rule key:" + eraIdxStr + " in era rule data for "
                        + calType.getId() + " must be in range [0, " + (numEras - 1) + "]");
            }
            if (rules[eraIdx] != null) {
                throw new ICUException("Dupulicated era rule for rule key:" + eraIdxStr
                        + " in era rule data for " + calType.getId());
            }

            RuleDate start = null;
            RuleDate end = null;
            boolean hasName = true;
            UResourceBundleIterator ruleItr = eraRuleRes.getIterator();
            while (ruleItr.hasNext()) {
                UResourceBundle res = ruleItr.next();
                String key = res.getKey();
                if (key.equals("start") || key.equals("end")) {
                    int[] fields = res.getIntVector();
                    if (fields.length != 3) {
                        throw new ICUException("Invalid era rule date data:" + fields
                                + " in era rule data for " + calType.getId());
                    }
                    if (key.equals("start")) {
                        start = new RuleDate(fields[0], fields[1], fields[2]);
                    } else {
                        end = new RuleDate(fields[0], fields[1], fields[2]);
                    }
                } else if (key.equals("named")) {
                    String val = res.getString();
                    if (val.equals("false")) {
                        hasName = false;
                    }
                }
            }
            if (start == null && end == null) {
                throw new ICUException("Missing era start/end rule date for key:" + eraIdxStr
                        + " in era rule data for " + calType.getId());
            }
            if (start == null && eraIdx != 0) {
                // start date must be available if era index is not 0.
                throw new ICUException("Missing start rule date for key:" + eraIdxStr
                        + " in era rule data for " + calType.getId());
            }
            if (hasName) {
                if (eraIdx >= firstTentativeIdx) {
                    throw new ICUException("Non-tentative era("
                            + eraIdx+ ") must be placed before the first tentative era");
                }
            } else {
                if (eraIdx < firstTentativeIdx) {
                    firstTentativeIdx = eraIdx;
                }
            }
            rules[eraIdx] = new EraRule(start, end);
        }

        if (firstTentativeIdx < Integer.MAX_VALUE && !includeTentativeEra) {
            return new EraRules(rules, firstTentativeIdx);
        }

        return new EraRules(rules);
    }

    private EraRules(EraRule[] rules) {
        this(rules, rules.length);
    }

    private EraRules(EraRule[] rules, int numRules) {
        this.rules = rules;
        this.numRules = numRules;
    }

    public int getNumberOfEras() {
        return numRules;
    }

    public RuleDate getStartDate(int eraIdx) {
        if (eraIdx < 0 || eraIdx >= numRules) {
            throw new IllegalArgumentException("eraIdx is out of range");
        }
        RuleDate date = rules[eraIdx].getStart();
        if (date == null) {
            // Start date must be available if era index is not 0.
            // This is already checked in the factory method.
            assert eraIdx == 0;
            date = RuleDate.MIN_DATE;
        }
        return date;
    }

    public int getStartYear(int eraIdx) {
        return getStartDate(eraIdx).getYear();
    }

    public RuleDate getEndDate(int eraIdx) {
        if (eraIdx < 0 || eraIdx >= numRules) {
            throw new IllegalArgumentException("eraIdx is out of range");
        }
        RuleDate date = rules[eraIdx].getEnd();
        if (date == null) {
            if (eraIdx == rules.length - 1) {
                // Last rule
                date = RuleDate.MAX_DATE;
            } else {
                // Use one day before the next start date
                RuleDate nextStart = rules[eraIdx + 1].start;
                assert nextStart != null;
                // Use Grego to calculate a day before.
                // Strictly speaking, if next start day is March 1st, previous day could be
                // different between Julian and proleptic Gregorian.
                long t = Grego.fieldsToDay(nextStart.getYear(), nextStart.getMonth(), nextStart.getDay());
                t -= Grego.MILLIS_PER_DAY;
                int[] fields = Grego.dayToFields(t, null);
                date = new RuleDate(fields[0], fields[1] + 1, fields[2]);
            }
        }
        return date;
    }

    public int getEndYear(int eraIdx) {
        return getEndDate(eraIdx).getYear();
    }
}