/**
 * $RCSfile: CalendarApp.java,v $ $Revision: 1.2 $ $Date: 2000/02/28 04:09:23 $
 *
 * (C) Copyright IBM Corp. 1998.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package com.ibm.demo.calendar;

import com.ibm.demo.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.text.DateFormatSymbols;
import java.util.SimpleTimeZone;
//import java.util.*;

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
