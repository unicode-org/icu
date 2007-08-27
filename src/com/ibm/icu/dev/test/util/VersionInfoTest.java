/*
*******************************************************************************
* Copyright (C) 1996-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/


package com.ibm.icu.dev.test.util;


import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.VersionInfo;


/**
* Testing class for VersionInfo
* @author Syn Wee Quek
* @since release 2.1 March 01 2002
*/
public final class VersionInfoTest extends TestFmwk 
{ 
    // constructor ---------------------------------------------------
  
    /**
    * Constructor
    */
    public VersionInfoTest()
    {
    }

    // public methods -----------------------------------------------
    
    public static void main(String arg[]) 
    {
        VersionInfoTest test = new VersionInfoTest();
        try {
            test.run(arg);
        } catch (Exception e) {
            test.errln("Error testing VersionInfo");
        }
    }
    
    /**
     * Test that the instantiation works
     */
    public void TestInstance()
    {
        for (int i = 0; i < INSTANCE_INVALID_STRING_.length; i ++) {
            try {
            	VersionInfo.getInstance(INSTANCE_INVALID_STRING_[i]);
                errln("\"" + INSTANCE_INVALID_STRING_[i] + 
                      "\" should produce an exception");
            } catch (RuntimeException e) {
            	logln("PASS: \"" + INSTANCE_INVALID_STRING_[i] + 
                      "\" failed as expected");
            }
        }
        for (int i = 0; i < INSTANCE_VALID_STRING_.length; i ++) {
            try {
            	VersionInfo.getInstance(INSTANCE_VALID_STRING_[i]);
            } catch (RuntimeException e) {
                errln("\"" + INSTANCE_VALID_STRING_[i] + 
                      "\" should produce an valid version");
            }
        }
        for (int i = 0; i < INSTANCE_INVALID_INT_.length; i ++) {
            try {
            	getInstance(INSTANCE_INVALID_INT_[i]);
                errln("invalid ints should produce an exception");
            } catch (RuntimeException e) {
            	logln("PASS: \"" + INSTANCE_INVALID_INT_[i] + 
                	  "\" failed as expected");
            }
        }
        for (int i = 0; i < INSTANCE_VALID_INT_.length; i ++) {
            try {
            	getInstance(INSTANCE_VALID_INT_[i]);
            } catch (RuntimeException e) {
                errln("valid ints should not produce an exception");
            }
        }
    }
    
    /**
     * Test that the comparison works
     */
    public void TestCompare()
    {
        for (int i = 0; i < COMPARE_NOT_EQUAL_STRING_.length; i += 2) {
            VersionInfo v1 = 
                        VersionInfo.getInstance(COMPARE_NOT_EQUAL_STRING_[i]);
            VersionInfo v2 = 
                    VersionInfo.getInstance(COMPARE_NOT_EQUAL_STRING_[i + 1]);
            if (v1.compareTo(v2) == 0) {
                errln(COMPARE_NOT_EQUAL_STRING_[i] + " should not equal " +
                      COMPARE_NOT_EQUAL_STRING_[i + 1]);
            }
        }
        for (int i = 0; i < COMPARE_NOT_EQUAL_INT_.length; i += 2) {
            VersionInfo v1 = getInstance(COMPARE_NOT_EQUAL_INT_[i]);
            VersionInfo v2 = getInstance(COMPARE_NOT_EQUAL_INT_[i + 1]);
            if (v1.compareTo(v2) == 0) {
                errln(COMPARE_NOT_EQUAL_INT_[i] + " should not equal " +
                      COMPARE_NOT_EQUAL_INT_[i + 1]);
            }
        }
        for (int i = 0; i < COMPARE_EQUAL_STRING_.length - 1; i ++) {
            VersionInfo v1 = 
                        VersionInfo.getInstance(COMPARE_EQUAL_STRING_[i]);
            VersionInfo v2 = 
                    VersionInfo.getInstance(COMPARE_EQUAL_STRING_[i + 1]);
            if (v1.compareTo(v2) != 0) {
                errln(COMPARE_EQUAL_STRING_[i] + " should equal " +
                      COMPARE_EQUAL_STRING_[i + 1]);
            }
        }
        for (int i = 0; i < COMPARE_EQUAL_INT_.length - 1; i ++) {
            VersionInfo v1 = getInstance(COMPARE_EQUAL_INT_[i]);
            VersionInfo v2 = getInstance(COMPARE_EQUAL_INT_[i + 1]);
            if (v1.compareTo(v2) != 0) {
                errln(COMPARE_EQUAL_INT_[i] + " should equal " +
                      COMPARE_EQUAL_INT_[i + 1]);
            }
        }
        for (int i = 0; i < COMPARE_LESS_.length - 1; i ++) {
            VersionInfo v1 = VersionInfo.getInstance(COMPARE_LESS_[i]);
            VersionInfo v2 = VersionInfo.getInstance(COMPARE_LESS_[i + 1]);
            if (v1.compareTo(v2) >= 0) {
                errln(COMPARE_LESS_[i] + " should be less than " + 
                      COMPARE_LESS_[i + 1]);
            }
            if (v2.compareTo(v1) <= 0) {
                errln(COMPARE_LESS_[i + 1] + " should be greater than " + 
                      COMPARE_LESS_[i]);
            }
        }
    }
    
    /**
     * Test that the getter function works
     */
    public void TestGetter()
    {
        for (int i = 0; i < GET_STRING_.length; i ++) {
            VersionInfo v = VersionInfo.getInstance(GET_STRING_[i]);
            if (v.getMajor() != GET_RESULT_[i << 2] ||
                v.getMinor() != GET_RESULT_[(i << 2) + 1] ||
                v.getMilli() != GET_RESULT_[(i << 2) + 2] ||
                v.getMicro() != GET_RESULT_[(i << 2) + 3]) {
                errln(GET_STRING_[i] + " should return major=" + 
                      GET_RESULT_[i << 2] + " minor=" + 
                      GET_RESULT_[(i << 2) + 1] + " milli=" + 
                      GET_RESULT_[(i << 2) + 2] + " micro=" +
                      GET_RESULT_[(i << 2) + 3]);  
            }
            v = getInstance(GET_INT_[i]);
            if (v.getMajor() != GET_RESULT_[i << 2] ||
                v.getMinor() != GET_RESULT_[(i << 2) + 1] ||
                v.getMilli() != GET_RESULT_[(i << 2) + 2] ||
                v.getMicro() != GET_RESULT_[(i << 2) + 3]) {
                errln(GET_STRING_[i] + " should return major=" + 
                      GET_RESULT_[i << 2] + " minor=" + 
                      GET_RESULT_[(i << 2) + 1] + " milli=" + 
                      GET_RESULT_[(i << 2) + 2] + " micro=" +
                      GET_RESULT_[(i << 2) + 3]);  
            }
        }
    }
    
    /**
     * Test toString()
     */
    public void TesttoString() 
    {
        for (int i = 0; i < TOSTRING_STRING_.length; i ++) {
            VersionInfo v = VersionInfo.getInstance(TOSTRING_STRING_[i]);
            if (!v.toString().equals(TOSTRING_RESULT_[i])) {
                errln("toString() for " + TOSTRING_STRING_[i] + 
                      " should produce " + TOSTRING_RESULT_[i]);
            }
            v = getInstance(TOSTRING_INT_[i]);
            if (!v.toString().equals(TOSTRING_RESULT_[i])) {
                errln("toString() for " + TOSTRING_INT_[i] + 
                      " should produce " + TOSTRING_RESULT_[i]);
            }
        }
    }
    
    // private methods --------------------------------------------------
    
    /**
     * int array versioninfo creation
     */
    private static VersionInfo getInstance(int data[])
    {
        switch (data.length) {
            case 1:
                return VersionInfo.getInstance(data[0]);
            case 2:
                return VersionInfo.getInstance(data[0], data[1]);
            case 3:
                return VersionInfo.getInstance(data[0], data[1], data[2]);
            default:
                return VersionInfo.getInstance(data[0], data[1], data[2], 
                                               data[3]);
        }
    }
    
    // private data members --------------------------------------------
    
    /**
     * Test instance data
     */
    private static final String INSTANCE_INVALID_STRING_[] = {
        "a",
        "-1",
        "-1.0",
        "-1.0.0",
        "-1.0.0.0",
        "0.-1",
        "0.0.-1",
        "0.0.0.-1",
        "256",
        "256.0",
        "256.0.0",
        "256.0.0.0",
        "0.256",
        "0.0.256",
        "0.0.0.256",
        "1.2.3.4.5"
    };
    private static final String INSTANCE_VALID_STRING_[] = {
        "255",
        "255.255",
        "255.255.255",
        "255.255.255.255"
    };
    private static final int INSTANCE_INVALID_INT_[][] = {
        {-1},
        {-1, 0},
        {-1, 0, 0},
        {-1, 0, 0, 0},
        {0, -1},
        {0, 0, -1},
        {0, 0, 0, -1},
        {256},
        {256, 0},
        {256, 0, 0},
        {256, 0, 0, 0},
        {0, 256},
        {0, 0, 256},
        {0, 0, 0, 256},
    };
    private static final int INSTANCE_VALID_INT_[][] = {
        {255},
        {255, 255},
        {255, 255, 255},
        {255, 255, 255, 255}
    };
    
    /**
     * Test compare data
     */
    private static final String COMPARE_NOT_EQUAL_STRING_[] = {
        "2.0.0.0", "3.0.0.0"
    };
    private static final int COMPARE_NOT_EQUAL_INT_[][] = {
        {2, 0, 0, 0}, {3, 0, 0, 0}
    };
    private static final String COMPARE_EQUAL_STRING_[] = {
        "2.0.0.0", "2.0.0", "2.0", "2"
    };
    private static final int COMPARE_EQUAL_INT_[][] = {
        {2}, {2, 0}, {2, 0, 0}, {2, 0, 0, 0}    
    };
    private static final String COMPARE_LESS_[] = {
        "0", "0.0.0.1", "0.0.1", "0.1", "1", "2", "2.1", "2.1.1", "2.1.1.1"
    };
    
    /**
     * Test Getter data
     */
    private static final String GET_STRING_[] = {
        "0",
        "1.1",
        "2.1.255",
        "3.1.255.100"
    };
    private static final int GET_INT_[][] = {
        {0},
        {1, 1},
        {2, 1, 255},
        {3, 1, 255, 100}
    };
    private static final int GET_RESULT_[] = {
        0, 0, 0, 0,
        1, 1, 0, 0,
        2, 1, 255, 0,
        3, 1, 255, 100
    };
    
    /**
     * Test toString data
     */
    private static final String TOSTRING_STRING_[] = {
        "0",
        "1.1",
        "2.1.255",
        "3.1.255.100"    
    };
    private static final int TOSTRING_INT_[][] = {
        {0},
        {1, 1},
        {2, 1, 255},
        {3, 1, 255, 100}
    };
    private static final String TOSTRING_RESULT_[] = {
        "0.0.0.0",
        "1.1.0.0",
        "2.1.255.0",
        "3.1.255.100"    
    };
}