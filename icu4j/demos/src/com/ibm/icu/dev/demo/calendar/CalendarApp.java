// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1997-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.calendar;

import java.awt.Frame;

import com.ibm.icu.dev.demo.impl.DemoApplet;

/**
 * CalendarApp demonstrates how Calendar works.
 */
public class CalendarApp extends DemoApplet
{
    /**
     * For serialization
     */
    private static final long serialVersionUID = -4270137898405840825L;

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
