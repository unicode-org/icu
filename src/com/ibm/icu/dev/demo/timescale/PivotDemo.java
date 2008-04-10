/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.demo.timescale;

import java.util.Locale;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.UniversalTimeScale;

/**
 * This class demonstrates how to use <code>UniversalTimeScale</code> to
 * convert from one local time scale to another.
 * 
 * @see UniversalTimeScale
 * 
 * @draft ICU 3.2
 */
public class PivotDemo {

    /**
     * The default constructor.
     * 
     * @draft ICU 3.2
     */
    public PivotDemo()
    {
    }

    /**
     * The <code>main()</code> method uses <code>UniversalTimeScale</code> to
     * convert from the Java and Unix time scales to the ICU time scale. It uses
     * a <code>Calendar</code> object to display the ICU time values.
     * 
     * @param args the command line arguments.
     * 
     * @draft ICU 3.2
     */
    public static void main(String[] args)
    {
        TimeZone utc = new SimpleTimeZone(0, "UTC");
        Calendar cal = Calendar.getInstance(utc, Locale.ENGLISH);
        MessageFormat fmt = new MessageFormat("{1} = {0, date, full} {0, time, full}");
        Object arguments[] = {cal, null};
        
        arguments[0] = cal;
        
        System.out.println("\nJava test:");
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(0, UniversalTimeScale.JAVA_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = " 000000000000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(-62164684800000L, UniversalTimeScale.JAVA_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = "-62164684800000L";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(-62135769600000L, UniversalTimeScale.JAVA_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = "-62135769600000L";
        System.out.println(fmt.format(arguments));
        
        System.out.println("\nUnix test:");
        
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(0x80000000, UniversalTimeScale.UNIX_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = "0x80000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(0, UniversalTimeScale.UNIX_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = "0x00000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(UniversalTimeScale.toLong(UniversalTimeScale.from(0x7FFFFFFF, UniversalTimeScale.UNIX_TIME), UniversalTimeScale.ICU4C_TIME));
        arguments[1] = "0x7FFFFFFF";
        System.out.println(fmt.format(arguments));
        
    }
}
