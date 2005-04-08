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

    private void CheckAssert(boolean exp) {
        if (exp == false) {
            String msg;
            try {
                throw new Exception();
            }
            catch (Exception e) {
                StackTraceElement failPoint = e.getStackTrace()[1];
                msg = "Test failure in file " + failPoint.getFileName() +
                             " at line " + failPoint.getLineNumber();
            }
            errln(msg);
        }
        
    }
    public void TestConstruction() {
        int i;
        CharsetDetector  det = new CharsetDetector();
        
        String [] charsetNames = CharsetDetector.getAllDetectableCharsets();
        CheckAssert(charsetNames.length != 0);
        for (i=0; i<charsetNames.length; i++) {
            CheckAssert(charsetNames[i].equals("") == false); 
            // System.out.println("\"" + charsetNames[i] + "\"");
        }
     }
    
    public void TestUTF8() throws Exception {
        
        String  s = "This is a string with some non-ascii characters that will " +
                    "be converted to UTF-8, then shoved through the detection process.  " +
                    "\u0391\u0392\u0393\u0394\u0395" +
                    "Sure would be nice if our source could contain Unicode directly!";
        byte [] bytes = s.getBytes("UTF-8");
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        CheckAssert(m.getName().equals("UTF-8"));
        String retrievedS = m.getString();
        CheckAssert(s == retrievedS);
    }
}
