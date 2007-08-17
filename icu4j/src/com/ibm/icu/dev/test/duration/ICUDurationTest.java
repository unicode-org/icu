/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.duration;

import java.util.Date;

// BEGIN JDK>1.5
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
// END JDK>1.5

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

 // BEGIN JDK>1.5

    public void TestSimpleXMLDuration() {
        DatatypeFactory factory = null;
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            errln("Error instantiating XML DatatypeFactory.");
            e.printStackTrace();
        }
        
        Duration d;
        DurationFormat df;
        String out;
        String expected;
        
        
        // test 1
        d = factory.newDuration("PT2H46M40S");
        df = DurationFormat.getInstance(new ULocale("en"));
        expected = "2 hours, 46 minutes, and 40 seconds";
        out = df.format(d);
        if(out.equals(expected)) {
            logln("out=expected: " + expected + " from " + d);
        } else {
            errln("FAIL: got " + out + " wanted " + expected + " from " + d);
        }
        
        // test 2
        d = factory.newDuration(10000);
        df = DurationFormat.getInstance(new ULocale("en"));
        expected = "10 seconds";
        out = df.format(d);
        if(out.equals(expected)) {
            logln("out=expected: " + expected + " from " + d);
        } else {
            errln("FAIL: got " + out + " wanted " + expected + " from " + d);
        }
    }
    
    
    public void TestXMLDuration() {
        DatatypeFactory factory = null;
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            errln("Error instantiating XML DatatypeFactory.");
            e.printStackTrace();
        }
        
        String cases[] = {
                "en",   "PT10.00099S",   "10 seconds",
                "en",   "#10000",   "10 seconds",
                "en",   "-PT10.00099S",   "10 seconds",
                "en",   "#-10000",   "10 seconds",
                
                // from BD req's
                "en",   "PT2H46M40S",   "2 hours, 46 minutes, and 40 seconds",
                "it",   "PT2H46M40S",   "due ore, 46 minuti e 40 secondi",
        };
        
        for(int n=0;n<cases.length;n+=3) {
            String loc = cases[n+0];
            String from = cases[n+1];
            String to = cases[n+2];
            
            ULocale locale = new ULocale(loc);
            Duration d;
            if(from.startsWith("#")) {
                d = factory.newDuration(Long.parseLong(from.substring(1)));
            } else {
                d = factory.newDuration(from);
            }
            
            DurationFormat df = DurationFormat.getInstance(locale);
            String output = df.format(d);
            
            if(output.equals(to)) {
                logln("SUCCESS: locale: " + loc + ", from " + from + " ["+d.toString()+"] " +" to " + to + "= " + output);
            } else {
                logln("FAIL: locale: " + loc + ", from " + from + " ["+d.toString()+"] " +": expected " + to + " got " + output);
            }
        }
    }
 // END JDK>1.5
}
