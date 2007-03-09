/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

/**
 * Entry point for the ICUTZU tool.
 */
public class ICUTZUMain {
    /**
     * Entry point for the ICUTZU tool.
     * 
     * @param args
     *            The list of arguments.
     */
    public static void main(String[] args) {
        if ("true".equals(System.getProperty("nogui")))
            CLILoader.main(args);
        else
            GUILoader.main(args);
    }
}
