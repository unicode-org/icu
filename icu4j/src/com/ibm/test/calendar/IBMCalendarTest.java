/*
 *******************************************************************************
 * Copyright (C) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/calendar/Attic/IBMCalendarTest.java,v $ 
 * $Date: 2000/10/17 18:32:50 $ 
 * $Revision: 1.3 $
 *******************************************************************************
 */
package com.ibm.test.calendar;
import com.ibm.test.TestFmwk;
import com.ibm.util.*;
import java.text.*;
import java.util.Date;
import java.util.Locale;

/**
 * @test
 * @summary Tests of new functionality in IBMCalendar
 */
public class IBMCalendarTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new IBMCalendarTest().run(args);
    }

    /**
     * Test weekend support in IBMCalendar.
     *
     * NOTE: This test will have to be updated when the isWeekend() etc.
     *       API is finalized later.
     *
     *       In particular, the test will have to be rewritten to instantiate
     *       a Calendar in the given locale (using getInstance()) and call
     *       that Calendar's isWeekend() etc. methods.
     */
    public void TestWeekend() {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM dd yyyy G HH:mm:ss.SSS");
        
        // NOTE
        // This test tests for specific locale data.  This is probably okay
        // as far as US data is concerned, but if the Arabic/Bahrain data
        // changes, this test will have to be updated.

        // Test specific days
        Object[] DATA1 = {
            Locale.US, new int[] { // Saturday:Sunday
                2000, Calendar.MARCH, 17, 23,  0, 0, // Fri 23:00
                2000, Calendar.MARCH, 18,  0, -1, 0, // Fri 23:59:59.999
                2000, Calendar.MARCH, 18,  0,  0, 1, // Sat 00:00
                2000, Calendar.MARCH, 18, 15,  0, 1, // Sat 15:00
                2000, Calendar.MARCH, 19, 23,  0, 1, // Sun 23:00
                2000, Calendar.MARCH, 20,  0, -1, 1, // Sun 23:59:59.999
                2000, Calendar.MARCH, 20,  0,  0, 0, // Mon 00:00
                2000, Calendar.MARCH, 20,  8,  0, 0, // Mon 08:00
            },
            new Locale("ar", "BH"), new int[] { // Thursday:Friday
                2000, Calendar.MARCH, 15, 23,  0, 0, // Wed 23:00
                2000, Calendar.MARCH, 16,  0, -1, 0, // Wed 23:59:59.999
                2000, Calendar.MARCH, 16,  0,  0, 1, // Thu 00:00
                2000, Calendar.MARCH, 16, 15,  0, 1, // Thu 15:00
                2000, Calendar.MARCH, 17, 23,  0, 1, // Fri 23:00
                2000, Calendar.MARCH, 18,  0, -1, 1, // Fri 23:59:59.999
                2000, Calendar.MARCH, 18,  0,  0, 0, // Sat 00:00
                2000, Calendar.MARCH, 18,  8,  0, 0, // Sat 08:00
            },
        };

        // Test days of the week
        Object[] DATA2 = {
            Locale.US, new int[] {
                Calendar.MONDAY,   Calendar.WEEKDAY,
                Calendar.FRIDAY,   Calendar.WEEKDAY,
                Calendar.SATURDAY, Calendar.WEEKEND,
                Calendar.SUNDAY,   Calendar.WEEKEND,
            },
            new Locale("ar", "BH"), new int[] { // Thursday:Friday
                Calendar.WEDNESDAY,Calendar.WEEKDAY,
                Calendar.SATURDAY, Calendar.WEEKDAY,
                Calendar.THURSDAY, Calendar.WEEKEND,
                Calendar.FRIDAY,   Calendar.WEEKEND,
            },
        };

        // We only test the getDayOfWeekType() and isWeekend() APIs.
        // The getWeekendTransition() API is tested indirectly via the
        // isWeekend() API, which calls it.

        for (int i1=0; i1<DATA1.length; i1+=2) {
            Locale loc = (Locale)DATA1[i1];
            int[] data = (int[]) DATA1[i1+1];
            Calendar cal = Calendar.getInstance(loc);
            logln("Locale: " + loc);
            for (int i=0; i<data.length; i+=6) {
                cal.clear();
                cal.set(data[i], data[i+1], data[i+2], data[i+3], 0, 0);
                if (data[i+4] != 0) {
                    cal.setTime(new Date(cal.getTime().getTime() + data[i+4]));
                }
                boolean isWeekend = cal.isWeekend();
                boolean ok = isWeekend == (data[i+5] != 0);
                if (ok) {
                    logln("Ok:   " + fmt.format(cal.getTime()) + " isWeekend=" + isWeekend);
                } else {
                    errln("FAIL: " + fmt.format(cal.getTime()) + " isWeekend=" + isWeekend +
                          ", expected=" + (!isWeekend));
                }
            }
        }

        for (int i2=0; i2<DATA2.length; i2+=2) {
            Locale loc = (Locale)DATA2[i2];
            int[] data = (int[]) DATA2[i2+1];
            logln("Locale: " + loc);
            Calendar cal = Calendar.getInstance(loc);
            for (int i=0; i<data.length; i+=2) {
                int type = cal.getDayOfWeekType(data[i]);
                int exp  = data[i+1];
                if (type == exp) {
                    logln("Ok:   DOW " + data[i] + " type=" + type);
                } else {
                    errln("FAIL: DOW " + data[i] + " type=" + type +
                          ", expected=" + exp);
                }
            }
        }
    }
}
