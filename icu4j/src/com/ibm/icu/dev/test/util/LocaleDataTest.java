/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/LocaleDataTest.java,v $
 * $Date: 2003/11/14 19:15:14 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.util;

import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;

/**
 * @author ram
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LocaleDataTest extends TestFmwk{

    public static void main(String[] args) throws Exception{
        new LocaleDataTest().run(args);
    }
    
    private Locale[] availableLocales = null;
    
    LocaleDataTest(){
        availableLocales = ICULocaleData.getAvailableLocales();
    }
    public void TestPaperSize(){
        for(int i = 0; i < availableLocales.length; i++){
            Locale locale = availableLocales[i];
            LocaleData.PaperSize paperSize = LocaleData.getPaperSize(locale);
            if(locale.toString().indexOf("_US") >= 0){
                if(paperSize.getHeight()!= 279 || paperSize.getWidth() != 216 ){
                    errln("PaperSize did not return the expected value for locale "+ locale+
                          " Expected height: 279 width: 216."+
                          " Got height: "+paperSize.getHeight()+" width: "+paperSize.getWidth()
                           );
                }else{
                    logln("PaperSize returned the expected values for locale " + locale);
                }
            }else{
                if(paperSize.getHeight()!= 297 || paperSize.getWidth() != 210 ){
                    errln("PaperSize did not return the expected value for locale "+ locale +
                          " Expected height: 297 width: 210."+
                          " Got height: "+paperSize.getHeight() +" width: "+paperSize.getWidth() 
                           );
                }else{
                    logln("PaperSize returned the expected values for locale " + locale);
                }
            }
        }
    }
    public void TestMeasurementSystem(){
        for(int i=0; i<availableLocales.length; i++){
            Locale locale = availableLocales[i];
            LocaleData.MeasurementSystem ms = LocaleData.getMeasurementSystem(locale);
            if(locale.toString().indexOf("_US") >= 0){
                if(ms == LocaleData.MeasurementSystem.US){
                    logln("Got the expected measurement system for locale: " + locale);
                }else{
                    errln("Did not get the expected measurement system for locale: "+ locale);
                }
            }else{
                if(ms == LocaleData.MeasurementSystem.SI){
                    logln("Got the expected measurement system for locale: " + locale);
                }else{
                    errln("Did not get the expected measurement system for locale: "+ locale);
                } 
            }
        }
    }
    public void TestExemplarSet(){
        for(int i=0; i<availableLocales.length; i++){
            Locale locale = availableLocales[i];
            UnicodeSet exemplarSet = LocaleData.getExemplarSet(locale);
            int[] code = UScript.getCode(locale);
            if(code != null){
                UnicodeSet[] sets = new UnicodeSet[code.length];
                // create the UnicodeSets for the script
                for(int j=0; j < code.length; j++){
                    sets[j] = new UnicodeSet("[:" + UScript.getShortName(code[j]) + ":]");
                }
                boolean existsInScript = false;
                UnicodeSetIterator iter = new UnicodeSetIterator(exemplarSet);
                // iterate over the 
                while (iter.nextRange()) {
                   if (iter.codepoint != UnicodeSetIterator.IS_STRING) {
                       for(int j=0; j<sets.length; j++){
                           if(sets[j].contains(iter.codepoint, iter.codepointEnd)){
                               existsInScript = true;
                           }
                       }
                   } else {
                       for(int j=0; j<sets.length; j++){
                           if(sets[j].contains(iter.string)){
                               existsInScript = true;
                           }
                       }
                   }
                }
                if(existsInScript == false){
                    errln("ExemplarSet containment failed for locale : "+ locale);
                }
            }else{
                // I hate the JDK's solution for deprecated language codes.
                // Why does the Locale constructor change the string I passed to it ?
                // such a broken hack !!!!!
                // so in effect I can never test the script code for Indonesian :(
                if(locale.toString().indexOf(("in"))<0){
                    errln("UScript.getCode returned null for locale: " + locale); 
                }
            }
        }  
    }
}