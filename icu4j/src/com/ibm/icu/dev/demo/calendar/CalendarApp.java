/*
 *******************************************************************************
 * Copyright (C) 1997-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/calendar/CalendarApp.java,v $ 
 * $Date: 2003/05/14 18:35:53 $ 
 * $Revision: 1.8 $
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
