/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateCaseTest.java,v $
* $Date: 2002/10/05 01:28:58 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

abstract public class GenerateCaseTest implements UCD_Types {
    
    public static void main(String[] args) throws IOException {
        System.out.println("Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        Default.setUCD();

        PrintWriter out = Utility.openPrintWriter("CaseTest.txt", Utility.UTF8_WINDOWS);

        out.println("# CaseTest");
        out.println("# Generated: " + Default.getDate() + ", MED");
        Utility.appendFile("CaseTestHeader.txt", Utility.LATIN1, out);
        
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd.isAllocated(cp)) continue;
            if (Default.ucd.isHangulSyllable(cp)) continue;
            byte cat = Default.ucd.getCategory(cp);
            if (cp == PRIVATE_USE) continue;
            
            String lower = Default.ucd.getCase(cp, FULL, LOWER); 
            String upper = Default.ucd.getCase(cp, FULL, UPPER); 
            String title = Default.ucd.getCase(cp, FULL, TITLE); 
            String fold = Default.ucd.getCase(cp, FULL, FOLD);
            if (lower.equals(upper) 
                && lower.equals(title) 
                && lower.equals(fold)) continue;
            
            String s = UTF16.valueOf(cp);
            write(out, s, true);
            
            // if (cp == '\u0345') continue; // don't add combining for this special case
            
            s = s + testChar;
            
            String s2 = Default.nfd.normalize(s);
            
            String lower1 = Default.nfc.normalize(Default.ucd.getCase(s2, FULL, LOWER)); 
            String upper1 = Default.nfc.normalize(Default.ucd.getCase(s2, FULL, UPPER)); 
            String title1 = Default.nfc.normalize(Default.ucd.getCase(s2, FULL, TITLE)); 
            String fold1 = Default.nfc.normalize(Default.ucd.getCase(s2, FULL, FOLD));
            
            if (lower1.equals(Default.nfc.normalize(lower+testChar))
                && upper1.equals(Default.nfc.normalize(upper+testChar))
                && title1.equals(Default.nfc.normalize(title+testChar))
                && fold1.equals(Default.nfc.normalize(fold+testChar))
            ) continue;
            
            write(out, s, true);
        }
        out.println("# total lines: " + counter);
        out.close();
    }
    
    static final char testChar = '\u0316';
    static int counter = 0;
    
    static void write(PrintWriter out, String ss, boolean doComment) {
        String s = Default.nfd.normalize(ss);
        String lower = Default.nfc.normalize(Default.ucd.getCase(s, FULL, LOWER)); 
        String upper = Default.nfc.normalize(Default.ucd.getCase(s, FULL, UPPER)); 
        String title = Default.nfc.normalize(Default.ucd.getCase(s, FULL, TITLE)); 
        String fold = Default.nfc.normalize(Default.ucd.getCase(s, FULL, FOLD));
        out.println(Utility.hex(ss) + "; "
            + Utility.hex(lower) + "; "
            + Utility.hex(upper) + "; "
            + Utility.hex(title) + "; "
            + Utility.hex(fold)
            + (doComment ?  "\t# " + Default.ucd.getName(ss) : "")
        );
        counter++;
    }
}