/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.duration;

import java.util.Date;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class ICUDurationTest extends TestFmwk {

    /**
     * 
     */
    public ICUDurationTest() {
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new ICUDurationTest().run(args);
    }
    
    
    /**
     * Basic test
     */
    public void TestBasics() {
        DurationFormat df;
        String expect;
        String formatted;
        
        df = DurationFormat.getInstance(new ULocale("it"));
        formatted = df.formatDurationFromNow(4096);
        expect = "fra quattro secondi";
        if(!expect.equals(formatted)) {
            errln("Expected " + expect + " but got " + formatted);
        } else {
            logln("format duration -> " + formatted);
        }
        
        formatted = df.formatDurationFromNowTo(new Date(0));
        expect = "fra 37 anni"; // will break next year.
        if(!expect.equals(formatted)) {
            errln("Expected " + expect + " but got " + formatted);
        } else {
            logln("format date  -> " + formatted);
        }
        
        formatted = df.formatDurationFrom(1000*3600*24, new Date(0).getTime());
        expect = "fra un giorno";
        if(!expect.equals(formatted)) {
            errln("Expected " + expect + " but got " + formatted);
        } else {
            logln("format date from -> " + formatted);
        }

        formatted = df.format(new Long(1000*3600*24*2));
        expect = "fra due giorni";
        if(!expect.equals(formatted)) {
            errln("Expected " + expect + " but got " + formatted);
        } else {
            logln("format long obj -> " + formatted);
        }

    
    }
}
