/*
 *******************************************************************************
 * Copyright (C) 1997-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/calendar/CalendarApp.java,v $ 
 * $Date: 2000/03/10 03:47:42 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */

package com.ibm.demo.calendar;

import com.ibm.demo.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

import java.text.DateFormat;
import java.util.SimpleTimeZone;

import com.ibm.util.*;
import com.ibm.text.*;

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
