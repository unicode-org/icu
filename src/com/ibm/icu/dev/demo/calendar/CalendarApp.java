/*
 *******************************************************************************
 * Copyright (C) 1997-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/calendar/CalendarApp.java,v $ 
 * $Date: 2003/06/03 18:49:27 $ 
 * $Revision: 1.9 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.dev.demo.calendar;

import java.awt.Frame;

import com.ibm.icu.dev.demo.impl.*;

/**
 * CalendarApp demonstrates how Calendar works.
 */
public class CalendarApp extends DemoApplet
{
    /**
     * The main function which defines the behavior of the CalendarDemo
     * applet when an applet is started.
     */
    public static void main(String argv[]) {

        new CalendarApp().showDemo();
    }

    /* This creates a CalendarFrame for the demo applet. */
    public Frame createDemoFrame(DemoApplet applet) {
        return new CalendarFrame(applet);
    }
}
