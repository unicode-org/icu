/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/Attic/RangeDateRule.java,v $ 
 * $Date: 2000/03/10 04:17:59 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */

package com.ibm.util;

import java.util.Date;
import java.util.Vector;

public class RangeDateRule implements DateRule
{
    public RangeDateRule()
    {
    }

    public RangeDateRule(Range[] ranges)
    {
        for (int i = 0; i < ranges.length; i++) {
            this.ranges.addElement(ranges[i]);
        }
    }

    public void add(DateRule rule)
    {
        add(new Date(Long.MIN_VALUE), rule);
    }

    public void add(Date start, DateRule rule)
    {
        // TODO: Insert in the right place
        // System.out.println("Add: " + start.toString());
        ranges.addElement(new Range(start, rule));
    }

    //-----------------------------------------------------------------------

    public Date firstAfter(Date start)
    {
        // Find the range that I should look at
        int index = startIndex(start);
        Date result = null;

        Range r = rangeAt(index);
        Range e = rangeAt(index+1);

        if (r != null && r.rule != null)
        {
            if (e != null) {
                result = r.rule.firstBetween(start, e.start);
            } else {
                result = r.rule.firstAfter(start);
            }
        }
        return result;
    }

    public Date firstBetween(Date start, Date end)
    {
        // Find the range that I should look at
        int index = startIndex(start);
        Date result = null;

        Range next = rangeAt(index);

        while (result == null && next != null && ! next.start.after(end))
        {
            Range r = next;
            next = rangeAt(index+1);

            if (r.rule != null) {
                Date e = (next != null && next.start.before(end)) ? next.start
                                                                  : end;
                result = r.rule.firstBetween(start, e);
            }
        }
        return result;
    }

    public boolean isOn(Date date)
    {
        return false;
    }

    /**
     * Check whether this event occurs at least once between the two
     * dates given.
     */
    public boolean isBetween(Date start, Date end)
    {
        return firstBetween(start,end) == null;
    }

    /*
     * find the index of the last range whose start date is before "start"
     * returns an index >= ranges.size() if there is none
     */
    private int startIndex(Date start)
    {
        int lastIndex = ranges.size();

        for (int i = 0; i < ranges.size(); i++) {
            Range r = (Range) ranges.elementAt(i);
            if (start.before(r.start)) {
                break;
            }
            lastIndex = i;
        }
        return lastIndex;
    }

    private Range rangeAt(int index)
    {
       return (index < ranges.size()) ? (Range) ranges.elementAt(index)
                                      : null;
    }

    Vector ranges = new Vector(2,2);
};

//-----------------------------------------------------------------------
// Privates
//

class Range {
    public Range(Date start, DateRule rule) {
        this.start = start;
        this.rule = rule;
    }
    public Date     start;
    public DateRule rule;
}

