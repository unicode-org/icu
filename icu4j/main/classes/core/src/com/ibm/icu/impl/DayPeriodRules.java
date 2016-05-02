/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;

public final class DayPeriodRules {
    public enum DayPeriod {
        MIDNIGHT,
        NOON,
        MORNING1,
        AFTERNOON1,
        EVENING1,
        NIGHT1,
        MORNING2,
        AFTERNOON2,
        EVENING2,
        NIGHT2,
        AM,
        PM;

        public static DayPeriod[] VALUES = DayPeriod.values();

        private static DayPeriod fromStringOrNull(CharSequence str) {
            if ("midnight".contentEquals(str)) { return MIDNIGHT; }
            if ("noon".contentEquals(str)) { return NOON; }
            if ("morning1".contentEquals(str)) { return MORNING1; }
            if ("afternoon1".contentEquals(str)) { return AFTERNOON1; }
            if ("evening1".contentEquals(str)) { return EVENING1; }
            if ("night1".contentEquals(str)) { return NIGHT1; }
            if ("morning2".contentEquals(str)) { return MORNING2; }
            if ("afternoon2".contentEquals(str)) { return AFTERNOON2; }
            if ("evening2".contentEquals(str)) { return EVENING2; }
            if ("night2".contentEquals(str)) { return NIGHT2; }
            if ("am".contentEquals(str)) { return AM; }
            if ("pm".contentEquals(str)) { return PM; }
            return null;
        }
    }

    private enum CutoffType {
        BEFORE,
        AFTER,  // TODO: AFTER is deprecated in CLDR 29. Remove.
        FROM,
        AT;

        private static CutoffType fromStringOrNull(CharSequence str) {
            if ("from".contentEquals(str)) { return CutoffType.FROM; }
            if ("before".contentEquals(str)) { return CutoffType.BEFORE; }
            if ("after".contentEquals(str)) { return CutoffType.AFTER; }
            if ("at".contentEquals(str)) { return CutoffType.AT; }
            return null;
        }
    }

    private static final class DayPeriodRulesData {
        Map<String, Integer> localesToRuleSetNumMap = new HashMap<String, Integer>();
        DayPeriodRules[] rules;
        int maxRuleSetNum = -1;
    }

    private static final class DayPeriodRulesDataSink extends UResource.TableSink {
        private DayPeriodRulesData data;

        private DayPeriodRulesDataSink(DayPeriodRulesData data) {
            this.data = data;
        }

        // Entry point.
        @Override
        public UResource.TableSink getOrCreateTableSink(UResource.Key key) {
            if (key.contentEquals("locales")) {
                return localesSink;
            } else if (key.contentEquals("rules")) {
                return rulesSink;
            }
            return null;
        }

        // Locales.
        private class LocalesSink extends UResource.TableSink {
            @Override
            public void put(UResource.Key key, UResource.Value value) {
                int setNum = parseSetNum(value.getString());
                data.localesToRuleSetNumMap.put(key.toString(), setNum);
            }
        }

        private LocalesSink localesSink = new LocalesSink();

        // Rules.
        private class RulesSink extends UResource.TableSink {
            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key) {
                ruleSetNum = parseSetNum(key.toString());
                data.rules[ruleSetNum] = new DayPeriodRules();
                return ruleSetSink;
            }
        }
        private RulesSink rulesSink = new RulesSink();

        // Rules -> "set10", e.g.
        private class RuleSetSink extends UResource.TableSink {
            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key) {
                period = DayPeriod.fromStringOrNull(key);
                if (period == null) { throw new ICUException("Unknown day period in data."); }
                return periodSink;
            }

            @Override
            public void leave() {
                for (DayPeriod period : data.rules[ruleSetNum].dayPeriodForHour) {
                    if (period == null) {
                        throw new ICUException("Rules in data don't cover all 24 hours (they should).");
                    }
                }
            }
        }
        private RuleSetSink ruleSetSink = new RuleSetSink();

        // Rules -> "set10" -> "morning1", e.g.
        // If multiple times exist in a cutoff (such as before{6:00, 24:00})
        // they'll be sent to the next sink.
        private class PeriodSink extends UResource.TableSink {
            @Override
            public void put(UResource.Key key, UResource.Value value) {
                cutoffType = CutoffType.fromStringOrNull(key);
                addCutoff(cutoffType, value.getString());
            }

            @Override
            public UResource.ArraySink getOrCreateArraySink(UResource.Key key) {
                cutoffType = CutoffType.fromStringOrNull(key);
                return cutoffSink;
            }

            @Override
            public void leave() {
                setDayPeriodForHoursFromCutoffs();
                for (int i = 0; i < cutoffs.length; ++i) {
                    cutoffs[i] = 0;
                }
            }
        }
        private PeriodSink periodSink = new PeriodSink();

        // Rules -> "set10" -> "morning1" -> "before", e.g.
        // Will only enter this sink if more than one time is present for this cutoff.
        private class CutoffSink extends UResource.ArraySink {
            @Override
            public void put(int index, UResource.Value value) {
                addCutoff(cutoffType, value.getString());
            }
        }
        private CutoffSink cutoffSink = new CutoffSink();

        // Members.
        private int cutoffs[] = new int[25];  // [0] thru [24]; 24 is allowed is "before 24".

        // "Path" to data.
        private int ruleSetNum;
        private DayPeriod period;
        private CutoffType cutoffType;

        // Helpers.
        private void addCutoff(CutoffType type, String hourStr) {
            if (type == null) { throw new ICUException("Cutoff type not recognized."); }
            int hour = parseHour(hourStr);
            cutoffs[hour] |= 1 << type.ordinal();
        }

        private void setDayPeriodForHoursFromCutoffs() {
            DayPeriodRules rule = data.rules[ruleSetNum];
            for (int startHour = 0; startHour <= 24; ++startHour) {
                // AT cutoffs must be either midnight or noon.
                if ((cutoffs[startHour] & (1 << CutoffType.AT.ordinal())) > 0) {
                    if (startHour == 0 && period == DayPeriod.MIDNIGHT) {
                        rule.hasMidnight = true;
                    } else if (startHour == 12 && period == DayPeriod.NOON) {
                        rule.hasNoon = true;
                    } else {
                        throw new ICUException("AT cutoff must only be set for 0:00 or 12:00.");
                    }
                }

                // FROM/AFTER and BEFORE must come in a pair.
                if ((cutoffs[startHour] & (1 << CutoffType.FROM.ordinal())) > 0 ||
                        (cutoffs[startHour] & (1 << CutoffType.AFTER.ordinal())) > 0) {
                    for (int hour = startHour + 1;; ++hour) {
                        if (hour == startHour) {
                            // We've gone around the array once and can't find a BEFORE.
                            throw new ICUException(
                                    "FROM/AFTER cutoffs must have a matching BEFORE cutoff.");
                        }
                        if (hour == 25) { hour = 0; }
                        if ((cutoffs[hour] & (1 << CutoffType.BEFORE.ordinal())) > 0) {
                            rule.add(startHour, hour, period);
                            break;
                        }
                    }
                }
            }
        }

        private static int parseHour(String str) {
            int firstColonPos = str.indexOf(':');
            if (firstColonPos < 0 || !str.substring(firstColonPos).equals(":00")) {
                throw new ICUException("Cutoff time must end in \":00\".");
            }

            String hourStr = str.substring(0, firstColonPos);
            if (firstColonPos != 1 && firstColonPos != 2) {
                throw new ICUException("Cutoff time must begin with h: or hh:");
            }

            int hour = Integer.parseInt(hourStr);
            // parseInt() throws NumberFormatException if hourStr isn't proper.

            if (hour < 0 || hour > 24) {
                throw new ICUException("Cutoff hour must be between 0 and 24, inclusive.");
            }

            return hour;
        }
    }  // DayPeriodRulesDataSink

    private static class DayPeriodRulesCountSink extends UResource.TableSink {
        private DayPeriodRulesData data;

        private DayPeriodRulesCountSink(DayPeriodRulesData data) {
            this.data = data;
        }

        @Override
        public UResource.TableSink getOrCreateTableSink(UResource.Key key) {
            int setNum = parseSetNum(key.toString());
            if (setNum > data.maxRuleSetNum) {
                data.maxRuleSetNum = setNum;
            }

            return null;
        }
    }

    private static final DayPeriodRulesData DATA = loadData();

    private boolean hasMidnight;
    private boolean hasNoon;
    private DayPeriod[] dayPeriodForHour;

    private DayPeriodRules() {
        hasMidnight = false;
        hasNoon = false;
        dayPeriodForHour = new DayPeriod[24];
    }

    /**
     * Get a DayPeriodRules object given a locale.
     * If data hasn't been loaded, it will be loaded for all locales at once.
     * @param locale locale for which the DayPeriodRules object is requested.
     * @return a DayPeriodRules object for `locale`.
     */
    public static DayPeriodRules getInstance(ULocale locale) {
        String localeCode = locale.getName();
        if (localeCode.isEmpty()) { localeCode = "root"; }

        Integer ruleSetNum = null;
        while (ruleSetNum == null) {
            ruleSetNum = DATA.localesToRuleSetNumMap.get(localeCode);
            if (ruleSetNum == null) {
                localeCode = ULocale.getFallback(localeCode);
                if (localeCode.isEmpty()) {
                    // Saves a lookup in the map.
                    break;
                }
            } else {
                break;
            }
        }

        if (ruleSetNum == null || DATA.rules[ruleSetNum] == null) {
            // Data doesn't exist for the locale requested.
            return null;
        }

        return DATA.rules[ruleSetNum];
    }

    public double getMidPointForDayPeriod(DayPeriod dayPeriod) {
        int startHour = getStartHourForDayPeriod(dayPeriod);
        int endHour = getEndHourForDayPeriod(dayPeriod);

        double midPoint = (startHour + endHour) / 2.0;

        if (startHour > endHour) {
            // dayPeriod wraps around midnight. Shift midPoint by 12 hours, in the direction that
            // lands it in [0, 24).
            midPoint += 12;
            if (midPoint >= 24) {
                midPoint -= 24;
            }
        }

        return midPoint;
    }

    private static DayPeriodRulesData loadData() {
        DayPeriodRulesData data = new DayPeriodRulesData();
        ICUResourceBundle rb = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(
                ICUData.ICU_BASE_NAME,
                "dayPeriods",
                ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                true);

        DayPeriodRulesCountSink countSink = new DayPeriodRulesCountSink(data);
        rb.getAllTableItemsWithFallback("rules", countSink);

        data.rules = new DayPeriodRules[data.maxRuleSetNum + 1];
        DayPeriodRulesDataSink sink = new DayPeriodRulesDataSink(data);
        rb.getAllTableItemsWithFallback("", sink);

        return data;
    }

    private int getStartHourForDayPeriod(DayPeriod dayPeriod) throws IllegalArgumentException {
        if (dayPeriod == DayPeriod.MIDNIGHT) { return 0; }
        if (dayPeriod == DayPeriod.NOON) { return 12; }

        if (dayPeriodForHour[0] == dayPeriod && dayPeriodForHour[23] == dayPeriod) {
            // dayPeriod wraps around midnight. Start hour is later than end hour.
            for (int i = 22; i >= 1; --i) {
                if (dayPeriodForHour[i] != dayPeriod) {
                    return (i + 1);
                }
            }
        } else {
            for (int i = 0; i <= 23; ++i) {
                if (dayPeriodForHour[i] == dayPeriod) {
                    return i;
                }
            }
        }

        // dayPeriod doesn't exist in rule set; throw exception.
        throw new IllegalArgumentException();
    }

    private int getEndHourForDayPeriod(DayPeriod dayPeriod) {
        if (dayPeriod == DayPeriod.MIDNIGHT) { return 0; }
        if (dayPeriod == DayPeriod.NOON) { return 12; }

        if (dayPeriodForHour[0] == dayPeriod && dayPeriodForHour[23] == dayPeriod) {
            // dayPeriod wraps around midnight. End hour is before start hour.
            for (int i = 1; i <= 22; ++i) {
                if (dayPeriodForHour[i] != dayPeriod) {
                    // i o'clock is when a new period starts, therefore when the old period ends.
                    return i;
                }
            }
        } else {
            for (int i = 23; i >= 0; --i) {
                if (dayPeriodForHour[i] == dayPeriod) {
                    return (i + 1);
                }
            }
        }

        // dayPeriod doesn't exist in rule set; throw exception.
        throw new IllegalArgumentException();
    }

    // Getters.
    public boolean hasMidnight() { return hasMidnight; }
    public boolean hasNoon() { return hasNoon; }
    public DayPeriod getDayPeriodForHour(int hour) { return dayPeriodForHour[hour]; }

    // Helpers.
    private void add(int startHour, int limitHour, DayPeriod period) {
        for (int i = startHour; i != limitHour; ++i) {
            if (i == 24) { i = 0; }
            dayPeriodForHour[i] = period;
        }
    }

    private static int parseSetNum(String setNumStr) {
        if (!setNumStr.startsWith("set")) {
            throw new ICUException("Set number should start with \"set\".");
        }

        String numStr = setNumStr.substring(3);  // e.g. "set17" -> "17"
        return Integer.parseInt(numStr);  // This throws NumberFormatException if numStr isn't a proper number.
    }
}
