/*
 *******************************************************************************
 * Copyright (C) 1997-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/calendar/CalendarApp.java,v $ 
 * $Date: 2002/02/19 04:10:23 $ 
 * $Revision: 1.7 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.dev.demo.calendar;

import com.ibm.icu.dev.demo.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

import com.ibm.icu.util.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
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
