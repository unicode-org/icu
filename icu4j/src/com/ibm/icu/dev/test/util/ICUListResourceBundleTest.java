package com.ibm.icu.dev.test.util;

import java.util.Enumeration;
import java.util.ResourceBundle;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICULocaleData;

public final class ICUListResourceBundleTest extends TestFmwk 
{
    public static void main(String args[]) {
	ICUListResourceBundleTest test = new ICUListResourceBundleTest();
	test.TestReferences();
	/**
	try {
	    test.run(args);
	} catch (Exception e) {
	    test.errln("Error testing ICUListResourceBundle: " + e);
	}
	*/
    }
    
    public void TestReferences() {
	ResourceBundle rb = ICULocaleData.getLocaleElements("th");
	System.out.println("got rb:" + rb);

	byte[] binaryData = (byte[])rb.getObject("%%CollationBin");
	System.out.println("got binaryData: " + binaryData + " length: " + (binaryData == null ? 0 : binaryData.length));
	Object[][] stringArrayData = (Object[][])rb.getObject("CollationElements");
	String[] collationData = new String[] {
	    (String)stringArrayData[0][0],
	    (String)stringArrayData[0][1]
	};
	System.out.println("got stringData: " + stringArrayData + " length: " + stringArrayData.length);
	System.out.println("got stringDataElement: " + stringArrayData[0] + " length: " + stringArrayData[0].length);
	System.out.println("got sdee: " + collationData[0]);
	//	System.out.println("char data length: " + stringArrayData.length());
    }
}
