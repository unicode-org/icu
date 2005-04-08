/**
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.charsetdet;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.*;


/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestCharsetDetector extends TestFmwk {

    
    /**
     * Constructor
     */
    public TestCharsetDetector()
    {
    }

    public static void main(String[] args) {
        try
        {
            TestCharsetDetector test = new TestCharsetDetector();
            test.run(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    
    public void TestConstruction() {
        int i;
        CharsetDetector  det = new CharsetDetector();
        
        String [] charsetNames = CharsetDetector.getAllDetectableCharsets();
        if (charsetNames.length == 0) {
            errln("TestCharsetDetector TestConstruction #0001");
        }
        for (i=0; i<charsetNames.length; i++) {
            if (charsetNames[i].equals("")) {
                errln("TestCharsetDetector TestConstruction #0002.  i=" + i);                
            }
            // System.out.println("\"" + charsetNames[i] + "\"");
        }
        
     }
}
