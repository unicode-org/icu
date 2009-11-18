/*
 *******************************************************************************
 * Copyright (C) 2003-2009, International Business Machines Corporation and    *
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
import java.util.HashSet;
import java.util.Arrays;

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
    }
    protected void init(){
        availableLocales = ICUResourceBundle.getAvailableULocales();
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
            if(locale.toString().indexOf("_US") >= 0 || locale.toString().indexOf("_CA") >= 0 ||
               locale.toString().indexOf("_PH") >= 0 || locale.toString().indexOf("_CL") >= 0 ||
               locale.toString().indexOf("_PR") >= 0 || locale.toString().indexOf("_VE") >= 0 ||
               locale.toString().indexOf("_CO") >= 0 || locale.toString().indexOf("_MX") >= 0 ){
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
    
    // Bundle together a UnicodeSet (of expemplars) and ScriptCode combination.
    //   We keep a set of combinations that have already been tested, to
    //   avoid repeated (time consuming) retesting of the same data.
    //   Instances of this class must be well behaved as members of a set.
    static class ExemplarGroup {
        private int[] scs;
        private UnicodeSet set;
        
        ExemplarGroup(UnicodeSet s, int[] scriptCodes) {
            set = s;
            scs = scriptCodes;
        }
        public int hashCode() {
            int hash = 0;
            for (int i=0; i<scs.length && i<4; i++) {
                hash = (hash<<8)+scs[i];
            }
            return hash;
        }        
        public boolean equals(Object other) {
            ExemplarGroup o = (ExemplarGroup)other;
            boolean r = Arrays.equals(scs, o.scs) &&
                         set.equals(o.set);
            return r;
        }
    }
    
    public void TestExemplarSet(){
        HashSet  testedExemplars = new HashSet();
        int equalCount = 0;
        for(int i=0; i<availableLocales.length; i++){
            ULocale locale = availableLocales[i];
            int[] scriptCodes = UScript.getCode(locale);
            if (scriptCodes==null) {
                // I hate the JDK's solution for deprecated language codes.
                // Why does the Locale constructor change the string I passed to it ?
                // such a broken hack !!!!!
                // so in effect I can never test the script code for Indonesian :(
                if(locale.toString().indexOf(("in"))<0){
                    errln("UScript.getCode returned null for locale: " + locale); 
                }
                continue;
            }
            UnicodeSet exemplarSets[] = new UnicodeSet[2];
            for (int k=0; k<2; ++k) {   // for casing option in (normal, caseInsensitive)
                int option = (k==0) ? 0 : UnicodeSet.CASE;
                UnicodeSet exemplarSet = LocaleData.getExemplarSet(locale, option);
                exemplarSets[k] = exemplarSet;
                ExemplarGroup exGrp = new ExemplarGroup(exemplarSet, scriptCodes);
                if (!testedExemplars.contains(exGrp)) {
                    testedExemplars.add(exGrp);
                    UnicodeSet[] sets = new UnicodeSet[scriptCodes.length];
                    // create the UnicodeSets for the script
                    for(int j=0; j < scriptCodes.length; j++){
                        sets[j] = new UnicodeSet("[:" + UScript.getShortName(scriptCodes[j]) + ":]");
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
    public void TestExemplarSet2(){
        int equalCount = 0;
        HashSet  testedExemplars = new HashSet();
        for(int i=0; i<availableLocales.length; i++){
            ULocale locale = availableLocales[i];
            LocaleData ld = LocaleData.getInstance(locale);
            int[] scriptCodes = UScript.getCode(locale);
            if (scriptCodes==null) {
                if(locale.toString().indexOf(("in"))<0){
                    errln("UScript.getCode returned null for locale: "+ locale); 
                }
                continue;
            }
            UnicodeSet exemplarSets[] = new UnicodeSet[4];

            for (int k=0; k<2; ++k) {  // for casing option in (normal, uncased)
                int option = (k==0) ? 0 : UnicodeSet.CASE;
                for(int h=0; h<2; ++h){  
                    int type = (h==0) ? LocaleData.ES_STANDARD : LocaleData.ES_AUXILIARY;

                    UnicodeSet exemplarSet = ld.getExemplarSet(option, type);
                    exemplarSets[k*2+h] = exemplarSet;

                    ExemplarGroup exGrp = new ExemplarGroup(exemplarSet, scriptCodes);
                    if (!testedExemplars.contains(exGrp)) {
                        testedExemplars.add(exGrp);
                        UnicodeSet[] sets = new UnicodeSet[scriptCodes.length];
                        // create the UnicodeSets for the script
                        for(int j=0; j < scriptCodes.length; j++){
                            sets[j] = new UnicodeSet("[:" + UScript.getShortName(scriptCodes[j]) + ":]");
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
                        // TODO: How to verify LocaleData.ES_AUXILIARY ???
                        if(existsInScript == false && h == 0){
                            errln("ExemplarSet containment failed for locale,option,type : "+ locale + ", " + option + ", " + type);
                        }
                    }
                }
            }
            // This is expensive, so only do it if it will be visible
            if (isVerbose()) {
                logln(locale.toString() + " exemplar(ES_STANDARD)" + exemplarSets[0]);
                logln(locale.toString() + " exemplar(ES_AUXILIARY) " + exemplarSets[1]);
                logln(locale.toString() + " exemplar(case-folded,ES_STANDARD) " + exemplarSets[2]);
                logln(locale.toString() + " exemplar(case-folded,ES_AUXILIARY) " + exemplarSets[3]);
            }
            assertTrue(locale.toString() + " case-folded is a superset",
                    exemplarSets[2].containsAll(exemplarSets[0]));
            assertTrue(locale.toString() + " case-folded is a superset",
                    exemplarSets[3].containsAll(exemplarSets[1]));
            if (exemplarSets[2].equals(exemplarSets[0])) {
                ++equalCount;
            }
            if (exemplarSets[3].equals(exemplarSets[1])) {
                ++equalCount;
            }
        }
        // Note: The case-folded set should sometimes be a strict superset
        // and sometimes be equal.
        assertTrue("case-folded is sometimes a strict superset, and sometimes equal",
                equalCount > 0 && equalCount < availableLocales.length * 2);
    }
    public void TestCoverage(){
        LocaleData ld = LocaleData.getInstance();
        boolean t = ld.getNoSubstitute();
        ld.setNoSubstitute(t);
        assertEquals("LocaleData get/set NoSubstitute",
                t,
                ld.getNoSubstitute());
    
        logln(ld.getDelimiter(LocaleData.QUOTATION_START));
        logln(ld.getDelimiter(LocaleData.QUOTATION_END));
        logln(ld.getDelimiter(LocaleData.ALT_QUOTATION_START));
        logln(ld.getDelimiter(LocaleData.ALT_QUOTATION_END));
    }

    public void TestLocaleDisplayPattern(){
        LocaleData ld = LocaleData.getInstance();
        logln("Default locale "+ " LocaleDisplayPattern:" + ld.getLocaleDisplayPattern());
        logln("Default locale "+ " LocaleSeparator:" + ld.getLocaleSeparator());
        for(int i = 0; i < availableLocales.length; i++){
          ULocale locale = availableLocales[i];
          ld = LocaleData.getInstance(locale);
          logln(locale.toString() + " LocaleDisplayPattern:" + ld.getLocaleDisplayPattern());
          logln(locale.toString() + " LocaleSeparator:" + ld.getLocaleSeparator());
        }
    }
}
