// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2004-2010, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime;

import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class IMETest {
    public static void main(String[] args) {
    String sampleText = "This is a sample\nto put into the field.";
    Rectangle loc = new Rectangle(100, 100, 300, 300);
    for (int i = 0; i < 2; ++i) {
        JFrame jf = new JFrame("Test Window " + i);
        jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Box box = Box.createVerticalBox();
        box.add(new JTextField(sampleText));
        box.add(new JTextField(sampleText));
        jf.getContentPane().add(box);
        jf.setBounds(loc);
        
        jf.setVisible(true);
        
        loc.x += 50;
        loc.y += 50;
    }
    }
}
