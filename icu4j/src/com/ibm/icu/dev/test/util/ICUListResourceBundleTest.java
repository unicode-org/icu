/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/ICUListResourceBundleTest.java,v $
 * $Date: 2002/08/13 22:10:20 $
 * $Revision: 1.4 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ResourceBundle;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICULocaleData;


public final class ICUListResourceBundleTest extends TestFmwk 
{
    public static void main(String args[]) throws Exception {
		ICUListResourceBundleTest test = new ICUListResourceBundleTest();
	    test.run(args);

    }

    public void TestReferences() {
		ResourceBundle rb = ICULocaleData.getLocaleElements("th");
		logln("got rb:" + rb);
	
		byte[] binaryData = null;//(byte[])rb.getObject("%%CollationBin");
		Object colElem = rb.getObject("CollationElements");
        if(colElem instanceof Object[][]){
            Object[][] colElemArr = (Object[][])colElem;
            if(((String)colElemArr[0][0]).equals("%%CollationBin")){   
	           binaryData = (byte[]) colElemArr[0][1];
            }
        }
        logln("got binaryData: " + binaryData + " length: " + (binaryData == null ? 0 : binaryData.length));
		Object[] stringArrayData = (Object[])rb.getObject("CollationElements");
		//String[] collationData = new String[] {
		 //   (String)stringArrayData[0],
		 //   (String)stringArrayData[0]
		//};
		logln("got stringData: " + stringArrayData + " length: " + stringArrayData.length);
		logln("got stringDataElement: " + stringArrayData[0] + " length: " + stringArrayData.length);
		//System.out.println("got sdee: " + collationData[0]);
		//	System.out.println("char data length: " + stringArrayData.length());
    }
    public void TestAliases(){
        ResourceBundle rb = ICULocaleData.getResourceBundle("com.ibm.icu.dev.data","TestDataElements","testaliases");
        Object o = rb.getObject("CollationElements");
        Object o1 = rb.getObject("zoneTests");
        
    }
    
    public void TestCircularAliases(){
        try{
	        ResourceBundle rb = ICULocaleData.getResourceBundle("com.ibm.icu.dev.data","TestDataElements","testcircularalias");
	        Object o = rb.getObject("aaa");
        }catch(java.util.MissingResourceException e){
            if(e.toString().indexOf("ircular")==-1){
                errln("Did not get the expected Exception for circular aliases");
            }
            return;
        }
        errln("Did not get the expected Exception for circular aliases");
    }

}
