/*********************************************************************
 * Copyright (C) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *********************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/ChineseDateFormat.java,v $
 * $Date: 2000/11/21 20:19:08 $
 * $Revision: 1.3 $
 */
package com.ibm.text;
import com.ibm.util.*;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * A concrete {@link DateFormat} for {@link com.ibm.util.ChineseCalendar}.
 * This class handles a <code>ChineseCalendar</code>-specific field,
 * <code>ChineseCalendar.IS_LEAP_MONTH</code>.  It also redefines the
 * handling of two fields, <code>ERA</code> and <code>YEAR</code>.  The
 * former is displayed numerically, instead of symbolically, since it is
 * the numeric cycle number in <code>ChineseCalendar</code>.  The latter is
 * numeric, as before, but has no special 2-digit Y2K behavior.
 *
 * <p>With regard to <code>ChineseCalendar.IS_LEAP_MONTH</code>, this
 * class handles parsing specially.  If no string symbol is found at all,
 * this is taken as equivalent to an <code>IS_LEAP_MONTH</code> value of
 * zero.  This allows formats to display a special string (e.g., "*") for
 * leap months, but no string for normal months.
 *
 * <p>Summary of field changes vs. {@link SimpleDateFormat}:<pre>
 * Symbol   Meaning                 Presentation        Example
 * ------   -------                 ------------        -------
 * G        cycle                   (Number)            78
 * y        year of cycle (1..60)   (Number)            17
 * l        is leap month           (Text)              4637
 * </pre>
 *
 * @see com.ibm.util.ChineseCalendar
 * @see ChineseDateFormatSymbols
 * @author Alan Liu
 */
public class ChineseDateFormat extends SimpleDateFormat {

    // TODO Finish the constructors

    public ChineseDateFormat(String pattern, Locale locale) {
        super(pattern, new ChineseDateFormatSymbols(locale));
    }

    protected String subFormat(char ch, int count, int beginOffset,
                               FieldPosition pos, DateFormatSymbols formatData)  {
        switch (ch) {
        case 'G': // 'G' - ERA
            return zeroPaddingNumber(calendar.get(Calendar.ERA), 1, 9);
        case 'l': // 'l' - IS_LEAP_MONTH
            {
                ChineseDateFormatSymbols symbols =
                    (ChineseDateFormatSymbols) formatData;
                return symbols.getLeapMonth(calendar.get(
                               ChineseCalendar.IS_LEAP_MONTH));
            }
        default:
            return super.subFormat(ch, count, beginOffset, pos, formatData);
        }
    }    

    protected int subParse(String text, int start, char ch, int count,
                           boolean obeyCount, boolean[] ambiguousYear) {
        if (ch != 'G' && ch != 'l' && ch != 'y') {
            return super.subParse(text, start, ch, count, obeyCount, ambiguousYear);
        }

        ParsePosition pos = new ParsePosition(start);

        // If there are any spaces here, skip over them.  If we hit the end
        // of the string, then fail.
        for (;;) {
            if (pos.getIndex() >= text.length()) {
                return -start;
            }
            char c = text.charAt(pos.getIndex());
            if (c != ' ' && c != '\t') { // TODO Shouldn't this be isWhitespace?
                break;
            }
            pos.setIndex(pos.getIndex()+1);
        }

        switch (ch) {
        case 'G': // 'G' - ERA
        case 'y': // 'y' - YEAR, but without the 2-digit Y2K adjustment
            {
                Number number = null;
                if (obeyCount) {
                    if ((start+count) > text.length()) {
                        return -start;
                    }
                    number = numberFormat.parse(text.substring(0, start+count), pos);
                } else {
                    number = numberFormat.parse(text, pos);
                }
                if (number == null) {
                    return -start;
                }
                int value = number.intValue();
                calendar.set(ch == 'G' ? Calendar.ERA : Calendar.YEAR, value);
                return pos.getIndex();
            }
        case 'l': // 'l' - IS_LEAP_MONTH
            {
                ChineseDateFormatSymbols symbols =
                    (ChineseDateFormatSymbols) getSymbols();
                int result = matchString(text, start, ChineseCalendar.IS_LEAP_MONTH,
                                         symbols.isLeapMonth);
                // Treat the absence of any matching string as setting
                // IS_LEAP_MONTH to false.
                if (result<0) {
                    calendar.set(ChineseCalendar.IS_LEAP_MONTH, 0);
                    result = start;
                }
                return result;
            }
        default:
            return 0; // This can never happen
        }
    }
}
