/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

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
    
    private ULocale[] availableLocales = null;
    
    public LocaleDataTest(){
        availableLocales = ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_BASE_NAME);
    }
    public void TestPaperSize(){
        for(int i = 0; i < availableLocales.length; i++){
            ULocale locale = availableLocales[i];
            LocaleData.PaperSize paperSize = LocaleData.getPaperSize(locale);
            // skip testing of "in" .. deprecated code for Indonesian
            String lang = locale.getLanguage();
            if(lang.equals("in")){
                continue;
            }
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
            ULocale locale = availableLocales[i];
            LocaleData.MeasurementSystem ms = LocaleData.getMeasurementSystem(locale);
            // skip testing of "in" .. deprecated code for Indonesian
            String lang = locale.getLanguage();
            if(lang.equals("in")){
                continue;
            }           
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
        int equalCount = 0;
        for(int i=0; i<availableLocales.length; i++){
            ULocale locale = availableLocales[i];
            UnicodeSet exemplarSets[] = new UnicodeSet[2];
            for (int k=0; k<2; ++k) {
                int option = (k==0) ? 0 : UnicodeSet.CASE;
                UnicodeSet exemplarSet = LocaleData.getExemplarSet(locale, option);
                exemplarSets[k] = exemplarSet;
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
                    while (!existsInScript && iter.nextRange()) {
                        if (iter.codepoint != UnicodeSetIterator.IS_STRING) {
                            for(int j=0; j<sets.length; j++){
                                if(sets[j].contains(iter.codepoint, iter.codepointEnd)){
                                    existsInScript = true;
                                    break;
                                }
                            }
                        } else {
                            for(int j=0; j<sets.length; j++){
                                if(sets[j].contains(iter.string)){
                                    existsInScript = true;
                                    break;
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
            // This is expensive, so only do it if it will be visible
            if (isVerbose()) {
                logln(locale.toString() + " exemplar " + exemplarSets[0]);
                logln(locale.toString() + " exemplar(case-folded) " + exemplarSets[1]);
            }
            assertTrue(locale.toString() + " case-folded is a superset",
                       exemplarSets[1].containsAll(exemplarSets[0]));
            if (exemplarSets[1].equals(exemplarSets[0])) {
                ++equalCount;
            }
        }
        // Note: The case-folded set should sometimes be a strict superset
        // and sometimes be equal.
        assertTrue("case-folded is sometimes a strict superset, and sometimes equal",
                   equalCount > 0 && equalCount < availableLocales.length);
    }
}
