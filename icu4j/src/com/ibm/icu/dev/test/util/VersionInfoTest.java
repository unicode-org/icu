/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/VersionInfoTest.java,v $ 
* $Date: 2002/03/23 00:58:20 $ 
* $Revision: 1.4 $
*
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
    	VersionInfo version;
    	try {
    		version = VersionInfo.getInstance("a");
    		errln("\"a\" should produce an exception");
    	} catch (RuntimeException e) {
    	}
    	try {
    		version = VersionInfo.getInstance("2");
    	} catch (RuntimeException e) {
    		errln("\"2\" should produce a valid version");
    	}
    	try {
    		version = VersionInfo.getInstance("2.0");
    	} catch (RuntimeException e) {
    		errln("\"2.0\" should produce a valid version");
    	}
    	try {
    		version = VersionInfo.getInstance("2.0.0");
    	} catch (RuntimeException e) {
    		errln("\"2.0.0\" should produce a valid version");
    	}
    	try {
    		version = VersionInfo.getInstance("-2.0.0");
    		errln("\"-2.0.0\" should produce an exception");
    	} catch (RuntimeException e) {
    	}
    	try {
    		version = VersionInfo.getInstance("2.300.0");
    		errln("\"2.300.0\" should produce an exception");
    	} catch (RuntimeException e) {
    	}
    	try {
    		version = VersionInfo.getInstance("2.0.0.0");
    	} catch (RuntimeException e) {
    		errln("\"2.0.0.0\" should produce an valid version");
    	}
    	try {
    		version = VersionInfo.getInstance("2.100.100.100");
    	} catch (RuntimeException e) {
    		errln("\"2.100.100.100\" should produce an valid version");
    	}
    	try {
    		version = VersionInfo.getInstance(-2, 0, 0, 0);
    		errln("-2.0.0.0 should produce an exception");
    	} catch (RuntimeException e) {
    	}
    	try {
    		version = VersionInfo.getInstance(2, 300, 0, 0);
    		errln("2.300.0.0 should produce an exception");
    	} catch (RuntimeException e) {
    	}
    	try {
    		version = VersionInfo.getInstance(2, 0, 0, 0);
    	} catch (RuntimeException e) {
    		errln("\"2.0.0.0\" should produce an valid version");
    	}
    }
    
    /**
     * Test that the comparison works
     */
    public void TestCompare()
    {
    	VersionInfo unicode10 = VersionInfo.getInstance("1.0.0.0");
    	VersionInfo unicode10again = VersionInfo.getInstance(1, 0, 0, 0);
    	if (unicode10 != unicode10again || !unicode10.equals(unicode10again) ||
    	    unicode10.compareTo(unicode10again) != 0) {
    	    errln("Creation by string and int should produce the same VersionInfo 1.0.0.0");
    	}
    	VersionInfo unicode1 = VersionInfo.getInstance("1");
    	VersionInfo unicode1again = VersionInfo.getInstance("1.0");
    	if (unicode1 != unicode10 || !unicode1.equals(unicode1again) ||
    	    unicode1.compareTo(unicode1again) != 0) {
    	    errln("Creation by string and int should produce the same VersionInfo 1.0.0.0");
    	}
    	VersionInfo unicode20 = VersionInfo.getInstance(2, 0, 0, 0);
    	VersionInfo unicode20again = VersionInfo.getInstance(2, 0, 0, 0);
    	if (unicode20 != unicode20again || !unicode20.equals(unicode20again) ||
    	    unicode20.compareTo(unicode20again) != 0) {
    	    errln("Creation by int should produce the same VersionInfo 2.0.0.0");
    	}
    	VersionInfo unicode2 = VersionInfo.getInstance(2);
    	VersionInfo unicode2again = VersionInfo.getInstance(2, 0);
    	if (unicode20 != unicode2 || !unicode2.equals(unicode2again) ||
    	    unicode2.compareTo(unicode2again) != 0) {
    	    errln("Creation by int should produce the same VersionInfo 2.0.0.0");
    	}
    	VersionInfo unicode30 = VersionInfo.getInstance("3.0.0.0");
    	VersionInfo unicode30again = VersionInfo.getInstance("3.0.0.0");
    	if (unicode30 != unicode30again || !unicode30.equals(unicode30again) ||
    	    unicode30.compareTo(unicode30again) != 0) {
    	    errln("Creation by string should produce the same VersionInfo 3.0.0.0");
    	}
    	VersionInfo unicode3 = VersionInfo.getInstance("3");
    	VersionInfo unicode3again = VersionInfo.getInstance(3, 0);
    	if (unicode3 != unicode30 || !unicode3.equals(unicode3again) ||
    	    unicode3.compareTo(unicode3again) != 0) {
    	    errln("Creation by string should produce the same VersionInfo 3.0.0.0");
    	}
    	if (unicode10 == unicode20 || unicode10.equals(unicode20) ||
    	    unicode10.compareTo(unicode20) >= 0) {
    	    errln("1.0.0.0 should be less than 2.0.0.0");
    	}
    	if (unicode20 == unicode30 || unicode20.equals(unicode30) ||
    	    unicode20.compareTo(unicode30) >= 0) {
    	    errln("2.0.0.0 should be less than 3.0.0.0");
    	}
    }
    
    /**
     * Test that the getter function works
     */
    public void TestGetter()
    {
    	VersionInfo unicode10 = VersionInfo.getInstance("1.0.0.0");
    	if (unicode10.getMajor() != 1 || unicode10.getMinor() != 0 ||
    	    unicode10.getMilli() != 0 || unicode10.getMicro() != 0) {
    	    errln("Getter methods for 1.0.0.0 should produce 1.0.0.0");
    	}
    	VersionInfo unicode31 = VersionInfo.getInstance("3.1.1.0");
    	if (unicode31.getMajor() != 3 || unicode31.getMinor() != 1 ||
    	    unicode31.getMilli() != 1 || unicode31.getMicro() != 0) {
    	    errln("Getter methods for 3.1.1.0 should produce 3.1.1.0");
    	}
    	VersionInfo unicode43 = VersionInfo.getInstance(4, 3, 2, 1);
    	if (unicode43.getMajor() != 4 || unicode43.getMinor() != 3 ||
    	    unicode43.getMilli() != 2 || unicode43.getMicro() != 1) {
    	    errln("Getter methods for 4.3.2.1 should produce 4.3.2.1");
    	}
    	VersionInfo unicode54 = VersionInfo.getInstance(54, 43, 32, 21);
    	if (unicode54.getMajor() != 54 || unicode54.getMinor() != 43 ||
    	    unicode54.getMilli() != 32 || unicode54.getMicro() != 21) {
    	    errln("Getter methods for 54.43.32.21 should produce 54.43.32.21");
    	}
    }
    
    /**
     * Test toString()
     */
	public void TesttoString() 
	{
    	VersionInfo unicode31 = VersionInfo.getInstance("3.1.1.0");
    	if (!unicode31.toString().equals("3.1.1.0")) {
    	    errln("toString() for 3.1.1.0 should produce 3.1.1.0");
    	}
    	VersionInfo unicode54 = VersionInfo.getInstance(54, 43, 32, 21);
    	if (!unicode54.toString().equals("54.43.32.21")) {
    	    errln("toString() for 54.43.32.21 should produce 54.43.32.21");
    	}
	}
}