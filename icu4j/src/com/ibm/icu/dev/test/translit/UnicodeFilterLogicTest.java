/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/Attic/UnicodeFilterLogicTest.java,v $ 
 * $Date: 2003/06/03 18:49:31 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.text.*;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.impl.Utility;


/**
 * @test
 * @summary UnicodeFilterLogic test 
 */
public class UnicodeFilterLogicTest extends TestFmwk {

   public static void main(String[] args) throws Exception {
        new UnicodeFilterLogicTest().run(args);
   }


   UnicodeFilter Filter1=new UnicodeFilter() {
        public boolean contains(int c) {
            if(c == 0x0061 || c == 0x0041 || c == 0x0063 || c == 0x0043)
                return false;
            else
                return true;
        }
        public String toPattern(boolean escapeUnprintable) {
            return "";
        }
        public boolean matchesIndexValue(int v) {
            return false;
        }
        public void addMatchSetTo(UnicodeSet toUnionTo) {}
   };
   UnicodeFilter Filter2=new UnicodeFilter() {
        public boolean contains(int c) {
            if(c == 0x0079 || c == 0x0059 || c == 0x007a || c == 0x005a  || c == 0x0061 || c == 0x0063)
                return false;
            else
                return true;
        }
        public String toPattern(boolean escapeUnprintable) {
            return "";
        }
        public boolean matchesIndexValue(int v) {
            return false;
        }
        public void addMatchSetTo(UnicodeSet toUnionTo) {}
   };

   public void TestAllFilters() {

        Transliterator t1 = Transliterator.getInstance("Any-Hex");
        String source="abcdABCDyzYZ";

        //sanity testing wihtout any filter
        expect(t1, "without any Filter", source, "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        //sanity testing using the Filter1(acAC) and Filter2(acyzYZ)
        t1.setFilter(Filter1);
        expect(t1, "with Filter(acAC)", source, "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        t1.setFilter(Filter2);
        expect(t1, "with Filter2(acyzYZ)", source, "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");

        UnicodeFilter filterNOT=UnicodeFilterLogic.not(Filter1);
        UnicodeFilter filterAND=UnicodeFilterLogic.and(Filter1, Filter2);
        UnicodeFilter filterOR=UnicodeFilterLogic.or(Filter1, Filter2);

        checkNOT(t1, Filter1, "Filter(acAC)",  source, "\u0061b\u0063d\u0041B\u0043DyzYZ");
        checkNOT(t1, Filter2, "Filter(acyzYZ)", source, "\u0061b\u0063dABCD\u0079\u007A\u0059\u005A");
       // checkNOT(t1, null, "NULL", source, "abcdABCDyzYZ");
        checkNOT(t1, filterNOT, "FilterNOT(Fitler1(acAC))", source, 
                 "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkNOT(t1, filterAND, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ))",  source, 
                  "\u0061b\u0063d\u0041B\u0043D\u0079\u007A\u0059\u005A");
        checkNOT(t1, filterOR, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ))",  source, 
                 "\u0061b\u0063dABCDyzYZ");

        checkAND(t1, Filter1, Filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)",  source, 
                 "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, Filter2, Filter1, "Filter2(acyzYZ), Filter1(a,c,A,C), ", source, 
                 "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, Filter1, null, "Filter1(a,c,A,C), NULL",  source, 
                 "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkAND(t1, null, Filter2, "NULL, Filter2(acyzYZ)",   source, 
                 "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkAND(t1, null, null, "NULL, NULL",  source, 
                 "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkAND(t1, filterAND, null, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL",  source,
                  "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, filterAND, Filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)", source, 
                 "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, filterAND, Filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)", source, 
                 "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, Filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))", source,
                  "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, Filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))", source, 
                 "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkAND(t1, filterOR, null, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), NULL",  source, 
                 "a\u0062c\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkAND(t1, filterOR, Filter1, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler1(acAC)", source,
                 "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkAND(t1, filterOR, Filter2, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler2(acyzYZ)", source, 
                 "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkAND(t1, filterNOT, Filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)", source, 
                 "abcdABCDyzYZ");
        checkAND(t1, Filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))", source, 
                 "abcdABCDyzYZ");
        checkAND(t1, filterNOT, Filter2, "FilterNOT(Fitler1(acAC)), Fitler2(acyzYZ)", source, 
                 "abcd\u0041B\u0043DyzYZ");
        checkAND(t1, Filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))", source, 
                 "abcd\u0041B\u0043DyzYZ");

        checkOR(t1, Filter1, Filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)",  source,
                "a\u0062c\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, Filter2, Filter1, "Filter2(acyzYZ), Filter1(a,c,A,C)", source, 
                "a\u0062c\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, Filter1, null, "Filter1(a,c,A,C), NULL", source, 
                "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, null, Filter2, "NULL, Filter2(acyzYZ)",  source, 
                "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkOR(t1, null, null, "NULL, NULL",   source, 
                "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, filterAND, null, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL", source, 
                "a\u0062c\u0064A\u0042C\u0044yzYZ");
        checkOR(t1, filterAND, Filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)", source, 
                "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, filterAND, Filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)", source, 
                "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkOR(t1, Filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))", source, 
                "a\u0062c\u0064A\u0042C\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, Filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))", source, 
                "a\u0062c\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkOR(t1, filterNOT, Filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)", source, 
                "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, Filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))",  source, 
                "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044\u0079\u007A\u0059\u005A");
        checkOR(t1, filterNOT, Filter2, "FilterNOT(Fitler1(acAC)), Fitler1(acyzYZ)",  source, 
                "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044yzYZ");
        checkOR(t1, Filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))", source, 
                "\u0061\u0062\u0063\u0064\u0041\u0042\u0043\u0044yzYZ");

  
}
    //======================================================================
    // Support methods
    //======================================================================

    void checkOR(Transliterator t,
                   UnicodeFilter f1, 
                   UnicodeFilter f2, 
                   String message,
                   String source,
                   String expected){
        UnicodeFilter filter=UnicodeFilterLogic.or(f1, f2);
        t.setFilter(filter);
        expect(t, "with FilterOR(" + message + ")", source, expected);

    }
    void checkAND(Transliterator t,
                   UnicodeFilter f1, 
                   UnicodeFilter f2, 
                   String message,
                   String source,
                   String expected){
        UnicodeFilter filter=UnicodeFilterLogic.and(f1, f2);
        t.setFilter(filter);
        expect(t, "with FilterAND(" + message + ")", source, expected);

    }
    void checkNOT(Transliterator t,
                   UnicodeFilter f1, 
                   String message,
                   String source,
                   String expected){
        UnicodeFilter filter=UnicodeFilterLogic.not(f1);
        t.setFilter(filter);
        expect(t, "with FilterNOT(" + message + ")", source, expected);

    }  
    void expect(Transliterator t,  String message,  String source,  String expectedResult) {
        String rsource = source;
        t.transliterate(rsource);
        expectAux(t.getID() + ":Replaceable " + message, source + "->" + rsource, expectedResult.equals(rsource), expectedResult);

    }
    void expectAux(String tag, String summary, boolean pass, String expectedResult) {
        if (pass) {
            logln("(" + tag+ ") " + Utility.escape(summary));
        } else {
            errln("FAIL: ("+ tag+ ") "
                + Utility.escape(summary)
                + ", expected " + Utility.escape(expectedResult));
        }
    }

}



