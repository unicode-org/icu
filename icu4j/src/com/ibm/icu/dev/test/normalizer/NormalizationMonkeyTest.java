/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: 
 * $Date: 
 * $Revision: 
 *
 *****************************************************************************************
 */

package com.ibm.icu.dev.test.normalizer;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import java.util.Random;

public class NormalizationMonkeyTest extends TestFmwk {
    int loopCount = 100;
    int maxCharCount = 20;
    int maxCodePoint = 0x10ffff;
    Random random = null;
    UnicodeNormalizer unicode_NFD;
    UnicodeNormalizer unicode_NFC;
    UnicodeNormalizer unicode_NFKD;
    UnicodeNormalizer unicode_NFKC;
    
    public NormalizationMonkeyTest() {
        random = new Random();
        unicode_NFD = new UnicodeNormalizer(UnicodeNormalizer.D, true);
        unicode_NFC = new UnicodeNormalizer(UnicodeNormalizer.C, true);
        unicode_NFKD = new UnicodeNormalizer(UnicodeNormalizer.KD, true);
        unicode_NFKC = new UnicodeNormalizer(UnicodeNormalizer.KC, true);
    }
    
    public static void main(String[] args) throws Exception {
        new NormalizationMonkeyTest().run(args);
    }
    
    public void TestNormalize() {
        int i = 0;
        while (i < loopCount) {
            String source = getTestSource();
            logln("Test source:" + source);
            //NFD
            String uncodeNorm = unicode_NFD.normalize(source);
            String icuNorm = Normalizer.normalize(source, Normalizer.NFD);
            logln("\tNFD(Unicode): " + uncodeNorm);
            logln("\tNFD(icu4j)  : " + icuNorm);
            if (!uncodeNorm.equals(icuNorm)) {
                errln("NFD: Unicode sample output => " + uncodeNorm + "; icu4j output=> " + icuNorm);
            }
            //NFC
            uncodeNorm = unicode_NFC.normalize(source);
            icuNorm = Normalizer.normalize(source, Normalizer.NFC);
            logln("\tNFC(Unicode): " + uncodeNorm);
            logln("\tNFC(icu4j)  : " + icuNorm);
            if (!uncodeNorm.equals(icuNorm)) {
                errln("NFC: Unicode sample output => " + uncodeNorm + "; icu4j output=> " + icuNorm);
            }
            //NFKD
            uncodeNorm = unicode_NFKD.normalize(source);
            icuNorm = Normalizer.normalize(source, Normalizer.NFKD);
            logln("\tNFKD(Unicode): " + uncodeNorm);
            logln("\tNFKD(icu4j)  : " + icuNorm);
            if (!uncodeNorm.equals(icuNorm)) {
                errln("NFKD: Unicode sample output => " + uncodeNorm + "; icu4j output=> " + icuNorm);
            }
            //NFKC
            uncodeNorm = unicode_NFKC.normalize(source);
            icuNorm = Normalizer.normalize(source, Normalizer.NFKC);
            logln("\tNFKC(Unicode): " + uncodeNorm);
            logln("\tNFKC(icu4j)  : " + icuNorm);
            if (!uncodeNorm.equals(icuNorm)) {
                errln("NFKC: Unicode sample output => " + uncodeNorm + "; icu4j output=> " + icuNorm);
            }
            
            i++;
        }
    }
    
    String getTestSource() {
        String source = "";
        int i = 0;
        while (i < (random.nextInt(maxCharCount) + 1)) {
            int codepoint = random.nextInt(maxCodePoint);
            //Elimate unassigned characters
            while (UCharacter.getType(codepoint) == UCharacterCategory.UNASSIGNED) {
                codepoint = random.nextInt(maxCodePoint);
            }
            source = source + UTF16.toString(codepoint);
            i++;
        }
        return source;
    }
}