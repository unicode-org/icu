/*
 *******************************************************************************
 * Copyright (C) 1997-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextComponent;
import java.util.Locale;

public class DemoUtility
{
    public static final Font titleFont = new Font("TimesRoman",Font.BOLD,18);
    public static final Font labelFont = new Font("TimesRoman",Font.BOLD,14);
    public static final Font choiceFont = new Font("Helvetica",Font.BOLD,12);
    public static final Font editFont = new Font("Helvetica",Font.PLAIN,14);
    public static final Font creditFont = new Font("Helvetica",Font.PLAIN,10);
    public static final Font numberFont = new Font("sansserif", Font.PLAIN, 14);

    public static final Color bgColor = Color.lightGray;
    public static final Color choiceColor = Color.white;

    public static final String copyright1 =
        "Copyright (C) IBM Corp and others. 1997 - 2002 All Rights Reserved";

    /**
    Provides easy way to use basic functions of GridBagLayout, without
    the complications. After building a panel, and inserting all the
    * subcomponents, call this to lay it out in the desired number of columns.
    */
    public static void fixGrid(Container cont, int columns) {
        GridBagLayout gridbag = new GridBagLayout();
        cont.setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 1.0;
        c.insets = new Insets(2,2,2,2);

        Component[] components = cont.getComponents();
        for (int i = 0; i < components.length; ++i) {
            // not used int colNumber = i%columns;
            c.gridwidth = 1;    // default
            if ((i%columns) == columns - 1)
                c.gridwidth = GridBagConstraints.REMAINDER;    // last in grid
            if (components[i] instanceof Label) {
                switch (((Label)components[i]).getAlignment()) {
                case Label.CENTER: c.anchor = GridBagConstraints.CENTER; break;
                case Label.LEFT: c.anchor = GridBagConstraints.WEST; break;
                case Label.RIGHT: c.anchor = GridBagConstraints.EAST; break;
                }
            }
            gridbag.setConstraints(components[i], c);
        }

    }

    /**
    Provides easy way to change the spacing around an object in a GridBagLayout.
    Call AFTER fixGridBag, passing in the container, the component, and the
    new insets.
    */
    public static void setInsets(Container cont, Component comp, Insets insets) {
        GridBagLayout gbl = (GridBagLayout)cont.getLayout();
        GridBagConstraints g = gbl.getConstraints(comp);
        g.insets = insets;
        gbl.setConstraints(comp,g);
    }

    public static Panel createSpacer() {
        Panel spacer = new Panel();
        spacer.setLayout(null);
        spacer.setSize(1000, 1);
        return spacer;
    }

    // to avoid goofy updates and misplaced cursors
    public static void setText(TextComponent area, String newText) {
        String foo = area.getText();
        if (foo.equals(newText)) return;
        area.setText(newText);
    }
    
    /**
     * Compares two locals. Return value is negative
     * if they're different, and more positive the more
     * fields that match.
     */
     
    public static int compareLocales(Locale l1, Locale l2)
    {
        int result = -1;
        
        if (l1.getLanguage().equals(l2.getLanguage())) {
            result += 1;
            
            if (l1.getCountry().equals(l2.getCountry())) {
                result += 1;
                
                if (l1.getVariant().equals(l2.getVariant())) {
                    result += 1;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get the G7 locale list for demos.
     */
    public static Locale[] getG7Locales() {
        return localeList;
    }
    private static Locale[] localeList = {
        new Locale("DA", "DK", ""),
        new Locale("EN", "US", ""),
        new Locale("EN", "GB", ""),
        new Locale("EN", "CA", ""),
        new Locale("FR", "FR", ""),
        new Locale("FR", "CA", ""),
        new Locale("DE", "DE", ""),
        new Locale("IT", "IT", ""),
    //new Locale("JA", "JP", ""),
    };
}
