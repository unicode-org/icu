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
		System.out.println("got rb:" + rb);
	
		byte[] binaryData = null;//(byte[])rb.getObject("%%CollationBin");
		Object colElem = rb.getObject("CollationElements");
        if(colElem instanceof Object[][]){
            Object[][] colElemArr = (Object[][])colElem;
            if(((String)colElemArr[0][0]).equals("%%CollationBin")){   
	           binaryData = (byte[]) colElemArr[0][1];
            }
        }
        System.out.println("got binaryData: " + binaryData + " length: " + (binaryData == null ? 0 : binaryData.length));
		Object[] stringArrayData = (Object[])rb.getObject("CollationElements");
		//String[] collationData = new String[] {
		 //   (String)stringArrayData[0],
		 //   (String)stringArrayData[0]
		//};
		System.out.println("got stringData: " + stringArrayData + " length: " + stringArrayData.length);
		System.out.println("got stringDataElement: " + stringArrayData[0] + " length: " + stringArrayData.length);
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
