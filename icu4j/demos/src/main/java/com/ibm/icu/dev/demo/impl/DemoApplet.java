// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1997-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.impl;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class DemoApplet extends java.applet.Applet {
    private static final long serialVersionUID = -8983602961925702071L;
    private Button   demoButton;
    private Frame    demoFrame;
    private static int demoFrameCount = 0;

    protected abstract Frame createDemoFrame(DemoApplet applet);
    protected Dimension getDefaultFrameSize(DemoApplet applet, Frame f) {
        return new Dimension(700, 550);
    }

    //Create a button that will display the demo
    public void init()
    {
        setBackground(Color.white);
        demoButton = new Button("Demo");
        demoButton.setBackground(Color.yellow);
        add( demoButton );

        demoButton.addActionListener( new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                if (e.getID() == ActionEvent.ACTION_PERFORMED) {
                    demoButton.setLabel("loading");

                    if (demoFrame == null) {
                       demoFrame = createDemoFrame(DemoApplet.this);
                       showDemo();
                    }

                    demoButton.setLabel("Demo");
                }
             }
        } );
    }

    public void showDemo()
    {
        demoFrame = createDemoFrame(this);
        demoFrame.doLayout();
        Dimension d = getDefaultFrameSize(this, demoFrame);
        demoFrame.setSize(d.width, d.height);
        demoFrame.show();
        demoFrameOpened();
    }

    public void demoClosed()
    {
        demoFrame = null;
        demoFrameClosed();
    }

    public static void demoFrameOpened() {
        demoFrameCount++;
        System.err.println("DemoFrameOpened, now at:"+demoFrameCount);
    }
    public static void demoFrameClosed() {
        if (--demoFrameCount == 0) {
            System.err.println("DemoFrameClosed, now at:"+demoFrameCount + " - quitting");
            System.exit(0);
        }
        System.err.println("DemoFrameClosed, now at:"+demoFrameCount);
    }
}

