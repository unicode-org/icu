/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.translit;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import com.ibm.icu.dev.demo.impl.AppletFrame;

/**
 * A simple Applet that shows a button.  When pressed, the button
 * shows the DemoAppletFrame.  This Applet is meant to be embedded
 * in a web page.
 *
 * <p>Copyright (c) IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 */
public class DemoApplet extends Applet {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 8214879807740061678L;
    Demo frame = null;
    
    public static void main(String args[]) {
        final DemoApplet applet = new DemoApplet();
        new AppletFrame("Transliteration Demo", applet, 640, 480);
    }

    public void init() {

        Button button = new Button("Transliteration Demo");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (frame == null) {
                    frame = new Demo(600, 200);
                    frame.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent we) {
                            frame = null;
                        }
                    });
                }
                frame.setVisible(true);
                frame.toFront();
            }
        });

        add(button);

        Dimension size = button.getPreferredSize();
        size.width += 10;
        size.height += 10;

        resize(size);
    }
    
    public void stop() {
        if (frame != null) {
            frame.dispose();
        }
        frame = null;
    }
}
