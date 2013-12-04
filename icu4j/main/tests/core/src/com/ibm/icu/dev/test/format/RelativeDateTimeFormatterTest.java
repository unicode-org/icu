/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit;
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction;
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit;
import com.ibm.icu.util.ULocale;

public class RelativeDateTimeFormatterTest extends TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new RelativeDateTimeFormatterTest().run(args);
    }
    
    public void TestRelativeDateWithQuantity() {
        Object[][] data = {
                {0.0, Direction.NEXT, RelativeUnit.SECONDS, "in 0 seconds"},
                {0.5, Direction.NEXT, RelativeUnit.SECONDS, "in 0.5 seconds"},
                {1.0, Direction.NEXT, RelativeUnit.SECONDS, "in 1 second"},
                {2.0, Direction.NEXT, RelativeUnit.SECONDS, "in 2 seconds"},
                {0.0, Direction.NEXT, RelativeUnit.MINUTES, "in 0 minutes"},
                {0.5, Direction.NEXT, RelativeUnit.MINUTES, "in 0.5 minutes"},
                {1.0, Direction.NEXT, RelativeUnit.MINUTES, "in 1 minute"},
                {2.0, Direction.NEXT, RelativeUnit.MINUTES, "in 2 minutes"},
                {0.0, Direction.NEXT, RelativeUnit.HOURS, "in 0 hours"},
                {0.5, Direction.NEXT, RelativeUnit.HOURS, "in 0.5 hours"},
                {1.0, Direction.NEXT, RelativeUnit.HOURS, "in 1 hour"},
                {2.0, Direction.NEXT, RelativeUnit.HOURS, "in 2 hours"},
                {0.0, Direction.NEXT, RelativeUnit.DAYS, "in 0 days"},
                {0.5, Direction.NEXT, RelativeUnit.DAYS, "in 0.5 days"},
                {1.0, Direction.NEXT, RelativeUnit.DAYS, "in 1 day"},
                {2.0, Direction.NEXT, RelativeUnit.DAYS, "in 2 days"},
                {0.0, Direction.NEXT, RelativeUnit.WEEKS, "in 0 weeks"},
                {0.5, Direction.NEXT, RelativeUnit.WEEKS, "in 0.5 weeks"},
                {1.0, Direction.NEXT, RelativeUnit.WEEKS, "in 1 week"},
                {2.0, Direction.NEXT, RelativeUnit.WEEKS, "in 2 weeks"},
                {0.0, Direction.NEXT, RelativeUnit.MONTHS, "in 0 months"},
                {0.5, Direction.NEXT, RelativeUnit.MONTHS, "in 0.5 months"},
                {1.0, Direction.NEXT, RelativeUnit.MONTHS, "in 1 month"},
                {2.0, Direction.NEXT, RelativeUnit.MONTHS, "in 2 months"},
                {0.0, Direction.NEXT, RelativeUnit.YEARS, "in 0 years"},
                {0.5, Direction.NEXT, RelativeUnit.YEARS, "in 0.5 years"},
                {1.0, Direction.NEXT, RelativeUnit.YEARS, "in 1 year"},
                {2.0, Direction.NEXT, RelativeUnit.YEARS, "in 2 years"},
                
                {0.0, Direction.LAST, RelativeUnit.SECONDS, "0 seconds ago"},
                {0.5, Direction.LAST, RelativeUnit.SECONDS, "0.5 seconds ago"},
                {1.0, Direction.LAST, RelativeUnit.SECONDS, "1 second ago"},
                {2.0, Direction.LAST, RelativeUnit.SECONDS, "2 seconds ago"},
                {0.0, Direction.LAST, RelativeUnit.MINUTES, "0 minutes ago"},
                {0.5, Direction.LAST, RelativeUnit.MINUTES, "0.5 minutes ago"},
                {1.0, Direction.LAST, RelativeUnit.MINUTES, "1 minute ago"},
                {2.0, Direction.LAST, RelativeUnit.MINUTES, "2 minutes ago"},
                {0.0, Direction.LAST, RelativeUnit.HOURS, "0 hours ago"},
                {0.5, Direction.LAST, RelativeUnit.HOURS, "0.5 hours ago"},
                {1.0, Direction.LAST, RelativeUnit.HOURS, "1 hour ago"},
                {2.0, Direction.LAST, RelativeUnit.HOURS, "2 hours ago"},
                {0.0, Direction.LAST, RelativeUnit.DAYS, "0 days ago"},
                {0.5, Direction.LAST, RelativeUnit.DAYS, "0.5 days ago"},
                {1.0, Direction.LAST, RelativeUnit.DAYS, "1 day ago"},
                {2.0, Direction.LAST, RelativeUnit.DAYS, "2 days ago"},
                {0.0, Direction.LAST, RelativeUnit.WEEKS, "0 weeks ago"},
                {0.5, Direction.LAST, RelativeUnit.WEEKS, "0.5 weeks ago"},
                {1.0, Direction.LAST, RelativeUnit.WEEKS, "1 week ago"},
                {2.0, Direction.LAST, RelativeUnit.WEEKS, "2 weeks ago"},
                {0.0, Direction.LAST, RelativeUnit.MONTHS, "0 months ago"},
                {0.5, Direction.LAST, RelativeUnit.MONTHS, "0.5 months ago"},
                {1.0, Direction.LAST, RelativeUnit.MONTHS, "1 month ago"},
                {2.0, Direction.LAST, RelativeUnit.MONTHS, "2 months ago"},
                {0.0, Direction.LAST, RelativeUnit.YEARS, "0 years ago"},
                {0.5, Direction.LAST, RelativeUnit.YEARS, "0.5 years ago"},
                {1.0, Direction.LAST, RelativeUnit.YEARS, "1 year ago"},
                {2.0, Direction.LAST, RelativeUnit.YEARS, "2 years ago"},      
        };
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("en_US"));
        for (Object[] row : data) {
            String actual = fmt.format(
                    ((Double) row[0]).doubleValue(), (Direction) row[1], (RelativeUnit) row[2]);
            assertEquals("Relative date with quantity", row[3], actual);
        }
    }
    
    public void TestRelativeDateWithQuantitySr() {
        Object[][] data = {
                {0.0, Direction.NEXT, RelativeUnit.MONTHS, "за 0 месеци"},
                {1.2, Direction.NEXT, RelativeUnit.MONTHS, "за 1,2 месеца"},
                {21.0, Direction.NEXT, RelativeUnit.MONTHS, "за 21 месец"},      
        };
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("sr"));
        for (Object[] row : data) {
            String actual = fmt.format(
                    ((Double) row[0]).doubleValue(), (Direction) row[1], (RelativeUnit) row[2]);
            assertEquals("Relative date with quantity", row[3], actual);
        }
    }
    
    public void TestRelativeDateWithoutQuantity() {
        Object[][] data = {
                {Direction.NEXT_2, AbsoluteUnit.DAY, null},
                
                {Direction.NEXT, AbsoluteUnit.DAY, "tomorrow"},
                {Direction.NEXT, AbsoluteUnit.WEEK, "next week"},
                {Direction.NEXT, AbsoluteUnit.MONTH, "next month"},
                {Direction.NEXT, AbsoluteUnit.YEAR, "next year"},
                {Direction.NEXT, AbsoluteUnit.MONDAY, "next Monday"},
                {Direction.NEXT, AbsoluteUnit.TUESDAY, "next Tuesday"},
                {Direction.NEXT, AbsoluteUnit.WEDNESDAY, "next Wednesday"},
                {Direction.NEXT, AbsoluteUnit.THURSDAY, "next Thursday"},
                {Direction.NEXT, AbsoluteUnit.FRIDAY, "next Friday"},
                {Direction.NEXT, AbsoluteUnit.SATURDAY, "next Saturday"},
                {Direction.NEXT, AbsoluteUnit.SUNDAY, "next Sunday"},
                
                {Direction.LAST_2, AbsoluteUnit.DAY, null},
                
                {Direction.LAST, AbsoluteUnit.DAY, "yesterday"},
                {Direction.LAST, AbsoluteUnit.WEEK, "last week"},
                {Direction.LAST, AbsoluteUnit.MONTH, "last month"},
                {Direction.LAST, AbsoluteUnit.YEAR, "last year"},
                {Direction.LAST, AbsoluteUnit.MONDAY, "last Monday"},
                {Direction.LAST, AbsoluteUnit.TUESDAY, "last Tuesday"},
                {Direction.LAST, AbsoluteUnit.WEDNESDAY, "last Wednesday"},
                {Direction.LAST, AbsoluteUnit.THURSDAY, "last Thursday"},
                {Direction.LAST, AbsoluteUnit.FRIDAY, "last Friday"},
                {Direction.LAST, AbsoluteUnit.SATURDAY, "last Saturday"},
                {Direction.LAST, AbsoluteUnit.SUNDAY, "last Sunday"},
                 
                {Direction.THIS, AbsoluteUnit.DAY, "today"},
                {Direction.THIS, AbsoluteUnit.WEEK, "this week"},
                {Direction.THIS, AbsoluteUnit.MONTH, "this month"},
                {Direction.THIS, AbsoluteUnit.YEAR, "this year"},
                {Direction.THIS, AbsoluteUnit.MONDAY, "this Monday"},
                {Direction.THIS, AbsoluteUnit.TUESDAY, "this Tuesday"},
                {Direction.THIS, AbsoluteUnit.WEDNESDAY, "this Wednesday"},
                {Direction.THIS, AbsoluteUnit.THURSDAY, "this Thursday"},
                {Direction.THIS, AbsoluteUnit.FRIDAY, "this Friday"},
                {Direction.THIS, AbsoluteUnit.SATURDAY, "this Saturday"},
                {Direction.THIS, AbsoluteUnit.SUNDAY, "this Sunday"},
                
                {Direction.PLAIN, AbsoluteUnit.DAY, "day"},
                {Direction.PLAIN, AbsoluteUnit.WEEK, "week"},
                {Direction.PLAIN, AbsoluteUnit.MONTH, "month"},
                {Direction.PLAIN, AbsoluteUnit.YEAR, "year"},
                {Direction.PLAIN, AbsoluteUnit.MONDAY, "Monday"},
                {Direction.PLAIN, AbsoluteUnit.TUESDAY, "Tuesday"},
                {Direction.PLAIN, AbsoluteUnit.WEDNESDAY, "Wednesday"},
                {Direction.PLAIN, AbsoluteUnit.THURSDAY, "Thursday"},
                {Direction.PLAIN, AbsoluteUnit.FRIDAY, "Friday"},
                {Direction.PLAIN, AbsoluteUnit.SATURDAY, "Saturday"},
                {Direction.PLAIN, AbsoluteUnit.SUNDAY, "Sunday"},
                
                {Direction.PLAIN, AbsoluteUnit.NOW, "now"},
        };
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("en_US"));
        for (Object[] row : data) {
            String actual = fmt.format((Direction) row[0], (AbsoluteUnit) row[1]);
            assertEquals("Relative date without quantity", row[2], actual);
        }
    }
    
    public void TestTwoBeforeTwoAfter() {
        Object[][] data = {
                {Direction.NEXT_2, AbsoluteUnit.DAY, "pasado ma\u00F1ana"},
                {Direction.LAST_2, AbsoluteUnit.DAY, "antes de ayer"},
        };
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("es"));
        for (Object[] row : data) {
            String actual = fmt.format((Direction) row[0], (AbsoluteUnit) row[1]);
            assertEquals("Two before two after", row[2], actual);
        }
    }
    
    public void TestFormatWithQuantityIllegalArgument() {
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("en_US"));
        try {
            fmt.format(1.0, Direction.PLAIN, RelativeUnit.DAYS);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            fmt.format(1.0, Direction.THIS, RelativeUnit.DAYS);
            fail("Expected IllegalArgumentException."); 
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    public void TestFormatWithoutQuantityIllegalArgument() {
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("en_US"));
        try {
            fmt.format(Direction.LAST, AbsoluteUnit.NOW);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            fmt.format(Direction.NEXT, AbsoluteUnit.NOW);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            fmt.format(Direction.THIS, AbsoluteUnit.NOW);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    public void TestCustomNumberFormat() {
        ULocale loc = new ULocale("en_US");
        NumberFormat nf = NumberFormat.getInstance(loc);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(loc, nf);
        
        // Change nf after the fact to prove that we made a defensive copy
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(3);
        
        // Change getNumberFormat to prove we made defensive copy going out.
        fmt.getNumberFormat().setMinimumFractionDigits(5);
        assertEquals(
                "TestCustomNumberformat", 1, fmt.getNumberFormat().getMinimumFractionDigits());
        
        Object[][] data = {
            {0.0, Direction.NEXT, RelativeUnit.SECONDS, "in 0.0 seconds"},
            {0.5, Direction.NEXT, RelativeUnit.SECONDS, "in 0.5 seconds"},
            {1.0, Direction.NEXT, RelativeUnit.SECONDS, "in 1.0 seconds"},
            {2.0, Direction.NEXT, RelativeUnit.SECONDS, "in 2.0 seconds"},
        };
        for (Object[] row : data) {
            String actual = fmt.format(
                    ((Double) row[0]).doubleValue(), (Direction) row[1], (RelativeUnit) row[2]);
            assertEquals("Relative date with quantity special NumberFormat", row[3], actual);
        }
    }
    
    public void TestCombineDateAndTime() {
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(new ULocale("en_US"));
        assertEquals("TestcombineDateAndTime", "yesterday, 3:50", fmt.combineDateAndTime("yesterday", "3:50"));
    }
    
}
