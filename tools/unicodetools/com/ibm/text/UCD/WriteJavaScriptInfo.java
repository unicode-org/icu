/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/WriteJavaScriptInfo.java,v $
* $Date: 2001/08/31 00:29:50 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
//import java.text.*;
import com.ibm.text.utility.*;

public class WriteJavaScriptInfo {
    /* TODO: fix enumeration of compositions

    static public void writeJavascriptInfo() throws IOException {
        System.err.println("Writing Javascript data");
        UCD ucd = UCD.make();
        Normalizer normKD = new Normalizer(Normalizer.NFKD);
        Normalizer normD = new Normalizer(Normalizer.NFD);
        PrintWriter log = new PrintWriter(new FileOutputStream("Normalization_data.js"));

        int count = 0;
        int datasize = 0;
        int max = 0;
        int over7 = 0;
        log.println("var KD = new Object(); // NFKD compatibility decomposition mappings");
        log.println("// NOTE: Hangul is done in code!");
        CompactShortArray csa = new CompactShortArray((short)0);

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) System.err.println(Utility.hex(c));
            if (0xAC00 <= c && c <= 0xD7A3) continue;
            if (normKD.hasDecomposition(c)) {
                ++count;
                String decomp = normKD.normalize(c);
                datasize += decomp.length();
                if (max < decomp.length()) max = decomp.length();
                if (decomp.length() > 7) ++over7;
                csa.setElementAt(c, (short)count);
                log.println("\t KD[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp,"\\u") + "';");
            }
        }
        csa.compact();
        log.println("// " + count + " NFKD mappings total");
        log.println("// " + datasize + " total characters of results");
        log.println("// " + max + " string length, maximum");
        log.println("// " + over7 + " result strings with length > 7");
        log.println("// " + csa.storage() + " trie length (doesn't count string size)");
        log.println();

        count = 0;
        datasize = 0;
        max = 0;
        log.println("var D = new Object();  // NFD canonical decomposition mappings");
        log.println("// NOTE: Hangul is done in code!");
        csa = new CompactShortArray((short)0);

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) System.err.println(Utility.hex(c));
            if (0xAC00 <= c && c <= 0xD7A3) continue;
            if (normD.hasDecomposition(c)) {
                ++count;
                String decomp = normD.normalize(c);
                datasize += decomp.length();
                if (max < decomp.length()) max = decomp.length();
                csa.setElementAt(c, (short)count);
                log.println("\t D[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp,"\\u") + "';");
            }
        }
        csa.compact();

        log.println("// " + count + " NFD mappings total");
        log.println("// " + datasize + " total characters of results");
        log.println("// " + max + " string length, maximum");
        log.println("// " + csa.storage() + " trie length (doesn't count string size)");
        log.println();

        count = 0;
        datasize = 0;
        log.println("var CC = new Object(); // canonical class mappings");
        CompactByteArray cba = new CompactByteArray();

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) System.err.println(Utility.hex(c));
            int canClass = normKD.getCanonicalClass(c);
            if (canClass != 0) {
                ++count;

                log.println("\t CC[0x" + Utility.hex(c) + "]=" + canClass + ";");
            }
        }
        cba.compact();
        log.println("// " + count + " canonical class mappings total");
        log.println("// " + cba.storage() + " trie length");
        log.println();

        count = 0;
        datasize = 0;
        log.println("var C = new Object();  // composition mappings");
        log.println("// NOTE: Hangul is done in code!");

        IntHashtable.IntEnumeration enum = normKD.getD getComposition();
        while (enum.hasNext()) {
            int key = enum.next();
            char val = (char) enum.value();
            if (0xAC00 <= val && val <= 0xD7A3) continue;
            ++count;
            log.println("\tC[0x" + Utility.hex(key) + "]=0x" + Utility.hex(val) + ";");
        }
        log.println("// " + count + " composition mappings total");
        log.println();

        log.close();
        System.err.println("Done writing Javascript data");
    }

    */

}