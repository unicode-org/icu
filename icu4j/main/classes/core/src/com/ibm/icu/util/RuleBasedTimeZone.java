/*
 *******************************************************************************
 * Copyright (C) 2007-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import com.ibm.icu.impl.Grego;

/**
 * <code>RuleBasedTimeZone</code> is a concrete subclass of <code>TimeZone</code> that allows users to define
 * custom historic time transition rules.
 * 
 * @see com.ibm.icu.util.TimeZoneRule
 * 
 * @stable ICU 3.8
 */
public class RuleBasedTimeZone extends BasicTimeZone {

    private static final long serialVersionUID = 7580833058949327935L;

    private final InitialTimeZoneRule initialRule;
    private List<TimeZoneRule> historicRules;
    private AnnualTimeZoneRule[] finalRules;

    private transient List<TimeZoneTransition> historicTransitions;
    private transient boolean upToDate;

    /**
     * Constructs a <code>RuleBasedTimeZone</code> object with the ID and the
     * <code>InitialTimeZoneRule</code>
     * 
     * @param id                The time zone ID.
     * @param initialRule       The initial time zone rule.
     * 
     * @stable ICU 3.8
     */
    public RuleBasedTimeZone(String id, InitialTimeZoneRule initialRule) {
        super.setID(id);
        this.initialRule = initialRule;
    }

    /**
     * Adds the <code>TimeZoneRule</code> which represents time transitions.
     * The <code>TimeZoneRule</code> must have start times, that is, the result
     * of {@link com.ibm.icu.util.TimeZoneRule#isTransitionRule()} must be true.
     * Otherwise, <code>IllegalArgumentException</code> is thrown.
     * 
     * @param rule The <code>TimeZoneRule</code>.
     * 
     * @stable ICU 3.8
     */
    public void addTransitionRule(TimeZoneRule rule) {
        if (!rule.isTransitionRule()) {
            throw new IllegalArgumentException("Rule must be a transition rule");
        }
        if (rule instanceof AnnualTimeZoneRule
                && ((AnnualTimeZoneRule)rule).getEndYear() == AnnualTimeZoneRule.MAX_YEAR) {
            // One of the final rules applicable in future forever
            if (finalRules == null) {
                finalRules = new AnnualTimeZoneRule[2];
                finalRules[0] = (AnnualTimeZoneRule)rule;
            } else if (finalRules[1] == null) {
                finalRules[1] = (AnnualTimeZoneRule)rule;
            } else {
                // Only a pair of AnnualTimeZoneRule is allowed.
                throw new IllegalStateException("Too many final rules");
            }
        } else {
            // If this is not a final rule, add it to the historic rule list 
            if (historicRules == null) {
                historicRules = new ArrayList<TimeZoneRule>();
            }
            historicRules.add(rule);
        }
        // Mark dirty, so transitions are recalculated when offset information is
        // accessed next time.
        upToDate = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
            int milliseconds) {
        if (era == GregorianCalendar.BC) {
            // Convert to extended year
            year = 1 - year;
        }
        long time = Grego.fieldsToDay(year, month, day) * Grego.MILLIS_PER_DAY + milliseconds;
        int[] offsets = new int[2];
        getOffset(time, true, LOCAL_DST, LOCAL_STD, offsets);
        return (offsets[0] + offsets[1]);
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public void getOffset(long time, boolean local, int[] offsets) {
        getOffset(time, local, LOCAL_FORMER, LOCAL_LATTER, offsets);
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public void getOffsetFromLocal(long date,
            int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        getOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public int getRawOffset() {
        // Note: This implementation returns standard GMT offset
        // as of current time.
        long now = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(now, false, offsets);
        return offsets[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public boolean inDaylightTime(Date date) {
        int[] offsets = new int[2];
        getOffset(date.getTime(), false, offsets);
        return (offsets[1] != 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    ///CLOVER:OFF
    public void setRawOffset(int offsetMillis) {
        // TODO: Do nothing for now..
        throw new UnsupportedOperationException("setRawOffset in RuleBasedTimeZone is not supported.");
    }
    ///CLOVER:ON

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public boolean useDaylightTime() {
        // Note: This implementation returns true when
        // daylight saving time is used as of now or
        // after the next transition.
        long now = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(now, false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        // If DST is not used now, check if DST is used after the next transition
        TimeZoneTransition tt = getNextTransition(now, false);
        if (tt != null && tt.getTo().getDSTSavings() != 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public boolean hasSameRules(TimeZone other) {
        if (!(other instanceof RuleBasedTimeZone)) {
            // We cannot reasonably compare rules in different types
            return false;
        }
        RuleBasedTimeZone otherRBTZ = (RuleBasedTimeZone)other;

        // initial rule
        if (!initialRule.isEquivalentTo(otherRBTZ.initialRule)) {
            return false;
        }

        // final rules
        if (finalRules != null && otherRBTZ.finalRules != null) {
            for (int i = 0; i < finalRules.length; i++) {
                if (finalRules[i] == null && otherRBTZ.finalRules[i] == null) {
                    continue;
                }
                if (finalRules[i] != null && otherRBTZ.finalRules[i] != null
                        && finalRules[i].isEquivalentTo(otherRBTZ.finalRules[i])) {
                    continue;
                    
                }
                return false;
            }
        } else if (finalRules != null || otherRBTZ.finalRules != null) {
            return false;
        }

        // historic rules
        if (historicRules != null && otherRBTZ.historicRules != null) {
            if (historicRules.size() != otherRBTZ.historicRules.size()) {
                return false;
            }
            for (TimeZoneRule rule : historicRules) {
                boolean foundSameRule = false;
                for (TimeZoneRule orule : otherRBTZ.historicRules) {
                    if (rule.isEquivalentTo(orule)) {
                        foundSameRule = true;
                        break;
                    }
                }
                if (!foundSameRule) {
                    return false;
                }
            }
        } else if (historicRules != null || otherRBTZ.historicRules != null) {
            return false;
        }
        return true;
    }

    // BasicTimeZone methods

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public TimeZoneRule[] getTimeZoneRules() {
        int size = 1;
        if (historicRules != null) {
            size += historicRules.size();
        }

        if (finalRules != null) {
            if (finalRules[1] != null) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] rules = new TimeZoneRule[size];
        rules[0] = initialRule;
        
        int idx = 1;
        if (historicRules != null) {
            for (; idx < historicRules.size() + 1; idx++) {
                rules[idx] = historicRules.get(idx - 1);
            }
        }
        if (finalRules != null) {
            rules[idx++] = finalRules[0];
            if (finalRules[1] != null) {
                rules[idx] = finalRules[1];
            }
        }
        return rules;
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        complete();
        if (historicTransitions == null) {
            return null;
        }
        boolean isFinal = false;
        TimeZoneTransition result = null;
        TimeZoneTransition tzt = historicTransitions.get(0);
        long tt = tzt.getTime();
        if (tt > base || (inclusive && tt == base)) {
            result = tzt;
        } else {
            int idx = historicTransitions.size() - 1;        
            tzt = historicTransitions.get(idx);
            tt = tzt.getTime();
            if (inclusive && tt == base) {
                result = tzt;
            } else if (tt <= base) {
                if (finalRules != null) {
                    // Find a transion time with finalRules
                    Date start0 = finalRules[0].getNextStart(base,
                            finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(), inclusive);
                    Date start1 = finalRules[1].getNextStart(base,
                            finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(), inclusive);

                    if (start1.after(start0)) {
                        tzt = new TimeZoneTransition(start0.getTime(), finalRules[1], finalRules[0]);
                    } else {
                        tzt = new TimeZoneTransition(start1.getTime(), finalRules[0], finalRules[1]);
                    }
                    result = tzt;
                    isFinal = true;
                } else {
                    return null;
                }
            } else {
                // Find a transition within the historic transitions
                idx--;
                TimeZoneTransition prev = tzt;
                while (idx > 0) {
                    tzt = historicTransitions.get(idx);
                    tt = tzt.getTime();
                    if (tt < base || (!inclusive && tt == base)) {
                        break;
                    }
                    idx--;
                    prev = tzt;
                }
                result = prev;
            }
        }
        if (result != null) {
            // For now, this implementation ignore transitions with only zone name changes.
            TimeZoneRule from = result.getFrom();
            TimeZoneRule to = result.getTo();
            if (from.getRawOffset() == to.getRawOffset()
                    && from.getDSTSavings() == to.getDSTSavings()) {
                // No offset changes.  Try next one if not final
                if (isFinal) {
                    return null;
                } else {
                    result = getNextTransition(result.getTime(), false /* always exclusive */);
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @stable ICU 3.8
     */
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        complete();
        if (historicTransitions == null) {
            return null;
        }
        TimeZoneTransition result = null;
        TimeZoneTransition tzt = historicTransitions.get(0);
        long tt = tzt.getTime();
        if (inclusive && tt == base) {
            result = tzt;
        } else if (tt >= base) {
            return null;
        } else {
            int idx = historicTransitions.size() - 1;        
            tzt = historicTransitions.get(idx);
            tt = tzt.getTime();
            if (inclusive && tt == base) {
                result = tzt;
            } else if (tt < base) {
                if (finalRules != null) {
                    // Find a transion time with finalRules
                    Date start0 = finalRules[0].getPreviousStart(base,
                            finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(), inclusive);
                    Date start1 = finalRules[1].getPreviousStart(base,
                            finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(), inclusive);

                    if (start1.before(start0)) {
                        tzt = new TimeZoneTransition(start0.getTime(), finalRules[1], finalRules[0]);
                    } else {
                        tzt = new TimeZoneTransition(start1.getTime(), finalRules[0], finalRules[1]);
                    }
                }
                result = tzt;
            } else {
                // Find a transition within the historic transitions
                idx--;
                while (idx >= 0) {
                    tzt = historicTransitions.get(idx);
                    tt = tzt.getTime();
                    if (tt < base || (inclusive && tt == base)) {
                        break;
                    }
                    idx--;
                }
                result = tzt;                
            }
        }
        if (result != null) {
            // For now, this implementation ignore transitions with only zone name changes.
            TimeZoneRule from = result.getFrom();
            TimeZoneRule to = result.getTo();
            if (from.getRawOffset() == to.getRawOffset()
                    && from.getDSTSavings() == to.getDSTSavings()) {
                // No offset changes.  Try previous one
                result = getPreviousTransition(result.getTime(), false /* always exclusive */);
            }
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public Object clone() {
        RuleBasedTimeZone other = (RuleBasedTimeZone)super.clone();
        if (historicRules != null) {
            other.historicRules = new ArrayList<TimeZoneRule>(historicRules); // rules are immutable
        }
        if (finalRules != null) {
            other.finalRules = finalRules.clone();
        }
        return other;
    }

    // private stuff

    /*
     * Resolve historic transition times and update fields used for offset
     * calculation.
     */
    private void complete() {
        if (upToDate) {
            // No rules were added since last time.
            return;
        }

        // Make sure either no final rules or a pair of AnnualTimeZoneRules
        // are available.
        if (finalRules != null && finalRules[1] == null) {
            throw new IllegalStateException("Incomplete final rules");
        }

        // Create a TimezoneTransition and add to the list
        if (historicRules != null || finalRules != null) {
            TimeZoneRule curRule = initialRule;
            long lastTransitionTime = Grego.MIN_MILLIS;

            // Build the transition array which represents historical time zone
            // transitions.
            if (historicRules != null) {
                BitSet done = new BitSet(historicRules.size()); // for skipping rules already processed

                while (true) {
                    int curStdOffset = curRule.getRawOffset();
                    int curDstSavings = curRule.getDSTSavings();
                    long nextTransitionTime = Grego.MAX_MILLIS;
                    TimeZoneRule nextRule = null;
                    Date d;
                    long tt;

                    for (int i = 0; i < historicRules.size(); i++) {
                        if (done.get(i)) {
                            continue;
                        }
                        TimeZoneRule r = historicRules.get(i);
                        d = r.getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false);
                        if (d == null) {
                            // No more transitions from this rule - skip this rule next time
                            done.set(i);
                        } else {
                            if (r == curRule ||
                                    (r.getName().equals(curRule.getName())
                                            && r.getRawOffset() == curRule.getRawOffset()
                                            && r.getDSTSavings() == curRule.getDSTSavings())) {
                                continue;
                            }
                            tt = d.getTime();
                            if (tt < nextTransitionTime) {
                                nextTransitionTime = tt;
                                nextRule = r;
                            }
                        }
                    }

                    if (nextRule ==  null) {
                        // Check if all historic rules are done
                        boolean bDoneAll = true;
                        for (int j = 0; j < historicRules.size(); j++) {
                            if (!done.get(j)) {
                                bDoneAll = false;
                                break;
                            }
                        }
                        if (bDoneAll) {
                            break;
                        }
                    }

                    if (finalRules != null) {
                        // Check if one of final rules has earlier transition date
                        for (int i = 0; i < 2 /* finalRules.length */; i++) {
                            if (finalRules[i] == curRule) {
                                continue;
                            }
                            d = finalRules[i].getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false);
                            if (d != null) {
                                tt = d.getTime();
                                if (tt < nextTransitionTime) {
                                    nextTransitionTime = tt;
                                    nextRule = finalRules[i];
                                }
                            }
                        }
                    }

                    if (nextRule == null) {
                        // Nothing more
                        break;
                    }

                    if (historicTransitions == null) {
                        historicTransitions = new ArrayList<TimeZoneTransition>();
                    }
                    historicTransitions.add(new TimeZoneTransition(nextTransitionTime, curRule, nextRule));
                    lastTransitionTime = nextTransitionTime;
                    curRule = nextRule;
                }
            }
            if (finalRules != null) {
                if (historicTransitions == null) {
                    historicTransitions = new ArrayList<TimeZoneTransition>();
                }
                // Append the first transition for each
                Date d0 = finalRules[0].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                Date d1 = finalRules[1].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                if (d1.after(d0)) {
                    historicTransitions.add(new TimeZoneTransition(d0.getTime(), curRule, finalRules[0]));
                    d1 = finalRules[1].getNextStart(d0.getTime(), finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(), false);
                    historicTransitions.add(new TimeZoneTransition(d1.getTime(), finalRules[0], finalRules[1]));
                } else {
                    historicTransitions.add(new TimeZoneTransition(d1.getTime(), curRule, finalRules[1]));
                    d0 = finalRules[0].getNextStart(d1.getTime(), finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(), false);
                    historicTransitions.add(new TimeZoneTransition(d0.getTime(), finalRules[1], finalRules[0]));
                }
            }
        }
        upToDate = true;
    }

    /*
     * getOffset internal implementation
     */
    private void getOffset(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        complete();
        TimeZoneRule rule;
        if (historicTransitions == null) {
            rule = initialRule;
        } else {
            long tstart = getTransitionTime(historicTransitions.get(0),
                    local, NonExistingTimeOpt, DuplicatedTimeOpt);
            if (time < tstart) {
                rule = initialRule;
            } else {
                int idx = historicTransitions.size() - 1;
                long tend = getTransitionTime(historicTransitions.get(idx),
                        local, NonExistingTimeOpt, DuplicatedTimeOpt);
                if (time > tend) {
                    if (finalRules != null) {
                        rule = findRuleInFinal(time, local, NonExistingTimeOpt, DuplicatedTimeOpt);
                    } else {
                        // no final rule, use the last rule
                        rule = (historicTransitions.get(idx)).getTo();
                    }
                } else {
                    // Find a historical transition
                    while (idx >= 0) {
                        if (time >= getTransitionTime(historicTransitions.get(idx),
                                local, NonExistingTimeOpt, DuplicatedTimeOpt)) {
                            break;
                        }
                        idx--;
                    }
                    rule = (historicTransitions.get(idx)).getTo();
                }
            }
        }
        offsets[0] = rule.getRawOffset();
        offsets[1] = rule.getDSTSavings();
    }
    
    /*
     * Find a time zone rule applicable to the specified time
     */
    private TimeZoneRule findRuleInFinal(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        if (finalRules == null || finalRules.length != 2 || finalRules[0] == null || finalRules[1] == null) {
            return null;
        }

        Date start0, start1;
        long base;
        int localDelta;

        base = time;
        if (local) {
            localDelta = getLocalDelta(finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(),
                    finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(),
                    NonExistingTimeOpt, DuplicatedTimeOpt);
            base -= localDelta;
        }
        start0 = finalRules[0].getPreviousStart(base, finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(), true);

        base = time;
        if (local) {
            localDelta = getLocalDelta(finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(),
                    finalRules[1].getRawOffset(), finalRules[1].getDSTSavings(),
                    NonExistingTimeOpt, DuplicatedTimeOpt);
            base -= localDelta;
        }
        start1 = finalRules[1].getPreviousStart(base, finalRules[0].getRawOffset(), finalRules[0].getDSTSavings(), true);

        return start0.after(start1) ? finalRules[0] : finalRules[1];
    }

    /*
     * Get the transition time in local wall clock
     */
    private static long getTransitionTime(TimeZoneTransition tzt, boolean local,
            int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        long time = tzt.getTime();
        if (local) {
            time += getLocalDelta(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings(),
                                tzt.getTo().getRawOffset(), tzt.getTo().getDSTSavings(),
                                NonExistingTimeOpt, DuplicatedTimeOpt);
        }
        return time;
    }

    /*
     * Returns amount of local time adjustment used for checking rule transitions
     */
    private static int getLocalDelta(int rawBefore, int dstBefore, int rawAfter, int dstAfter,
            int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        int delta = 0;

        int offsetBefore = rawBefore + dstBefore;
        int offsetAfter = rawAfter + dstAfter;

        boolean dstToStd = (dstBefore != 0) && (dstAfter == 0);
        boolean stdToDst = (dstBefore == 0) && (dstAfter != 0);

        if (offsetAfter - offsetBefore >= 0) {
            // Positive transition, which makes a non-existing local time range
            if (((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_STD && dstToStd)
                    || ((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_DST && stdToDst)) {
                delta = offsetBefore;
            } else if (((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_STD && stdToDst)
                    || ((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_DST && dstToStd)) {
                delta = offsetAfter;
            } else if ((NonExistingTimeOpt & FORMER_LATTER_MASK) == LOCAL_LATTER) {
                delta = offsetBefore;
            } else {
                // Interprets the time with rule before the transition,
                // default for non-existing time range
                delta = offsetAfter;
            }
        } else {
            // Negative transition, which makes a duplicated local time range
            if (((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_STD && dstToStd)
                    || ((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_DST && stdToDst)) {
                delta = offsetAfter;
            } else if (((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_STD && stdToDst)
                    || ((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_DST && dstToStd)) {
                delta = offsetBefore;
            } else if ((DuplicatedTimeOpt & FORMER_LATTER_MASK) == LOCAL_FORMER) {
                delta = offsetBefore;
            } else {
                // Interprets the time with rule after the transition,
                // default for duplicated local time range
                delta = offsetAfter;
            }
        }
        return delta;
    }
}

