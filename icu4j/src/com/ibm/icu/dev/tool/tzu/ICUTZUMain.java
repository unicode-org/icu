/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

public class ICUTZUMain {
    public static void main(String[] args) {
        if ("true".equals(System.getProperty("nogui")))
            CLILoader.main(args);
        else
            GUILoader.main(args);
    }
}