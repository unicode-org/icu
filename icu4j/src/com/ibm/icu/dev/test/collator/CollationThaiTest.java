/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: 
 * $Date: 
 * $Revision: 
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : collate/CollationRegressionTest
 * Source File: $ICU4CRoot/source/test/intltest/regcoll.cpp
 **/
 
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;
import java.util.Locale;
import java.io.*;

public class CollationThaiTest extends TestFmwk {
    
    final int MAX_FAILURES_TO_SHOW = 8;
    
    public static void main(String[] args) throws Exception {
        new CollationThaiTest().run(args);
    }
    
    /**
     * Odd corner conditions taken from "How to Sort Thai Without Rewriting Sort",
     * by Doug Cooper, http://seasrc.th.net/paper/thaisort.zip
     */
    public void TestCornerCases() {
        String TESTS[] = {
            // Shorter words precede longer
            "\u0e01",                               "<",    "\u0e01\u0e01",
    
            // Tone marks are considered after letters (i.e. are primary ignorable)
            "\u0e01\u0e32",                        "<",    "\u0e01\u0e49\u0e32",
    
            // ditto for other over-marks
            "\u0e01\u0e32",                        "<",    "\u0e01\u0e32\u0e4c",
    
            // commonly used mark-in-context order.
            // In effect, marks are sorted after each syllable.
            "\u0e01\u0e32\u0e01\u0e49\u0e32",   "<",    "\u0e01\u0e48\u0e32\u0e01\u0e49\u0e32",
    
            // Hyphens and other punctuation follow whitespace but come before letters
            "\u0e01\u0e32",                        "<",    "\u0e01\u0e32-",
            "\u0e01\u0e32-",                       "<",    "\u0e01\u0e32\u0e01\u0e32",
    
            // Doubler follows an indentical word without the doubler
            "\u0e01\u0e32",                        "<",    "\u0e01\u0e32\u0e46",
            "\u0e01\u0e32\u0e46",                 "<",    "\u0e01\u0e32\u0e01\u0e32",
    
            // \u0e45 after either \u0e24 or \u0e26 is treated as a single
            // combining character, similar to "c < ch" in traditional spanish.
            // TODO: beef up this case
            "\u0e24\u0e29\u0e35",                 "<",    "\u0e24\u0e45\u0e29\u0e35",
            "\u0e26\u0e29\u0e35",                 "<",    "\u0e26\u0e45\u0e29\u0e35",
    
            // Vowels reorder, should compare \u0e2d and \u0e34
            "\u0e40\u0e01\u0e2d",                 "<",    "\u0e40\u0e01\u0e34",
    
            // Tones are compared after the rest of the word (e.g. primary ignorable)
            "\u0e01\u0e32\u0e01\u0e48\u0e32",   "<",    "\u0e01\u0e49\u0e32\u0e01\u0e32",
    
            // Periods are ignored entirely
            "\u0e01.\u0e01.",                      "<",    "\u0e01\u0e32",
        };
        
        Collator coll = null;
        try {
            coll = Collator.getInstance(new Locale("th", "TH", ""));
        } catch (Exception e) {
            errln("Error: could not construct Thai collator");
            return;
        }
        compareArray(coll, TESTS); 
    }
    
    void compareArray(Collator c, String[] tests) {
        for (int i = 0; i < tests.length; i += 3) {
            int expect = 0;
            if (tests[i+1].equals("<")) {
                expect = -1;
            } else if (tests[i+1].equals(">")) {
                expect = 1;
            } else if (tests[i+1].equals("=")) {
                expect = 0;
            } else {
                // expect = Integer.decode(tests[i+1]).intValue();
                errln("Error: unknown operator " + tests[i+1]);
                return;
            }
            String s1 = tests[i];
            String s2 = tests[i+2];
            int result = c.compare(s1, s2);
            if (sign(result) != sign(expect)) {
                errln("" + i/3 + ": compare(" + s1
                      + " , " + s2  + ") got " + result + "; expected " + expect);
    
                CollationKey k1, k2;
                try {
                    k1 = c.getCollationKey(s1);
                    k2 = c.getCollationKey(s2);
                } catch (Exception e) {
                    errln("Fail: getCollationKey returned ");
                    return;
                }
                errln("  key1: " + prettify(k1));
                errln("  key2: " + prettify(k2));
            } else {
                // Collator.compare worked OK; now try the collation keys
                CollationKey k1, k2;
                try {
                    k1 = c.getCollationKey(s1);
                    k2 = c.getCollationKey(s2);
                } catch (Exception e) {
                    //System.out.println(e);
                    errln("Fail: getCollationKey returned ");
                    return;
                }
    
                result = k1.compareTo(k2);
                if (sign(result) != sign(expect)) {
                    errln("" + i/3 + ": key(" + s1
                          + ").compareTo(key(" + s2
                          + ")) got " + result + "; expected " + expect);
                    
                    errln("  " + prettify(k1) + " vs. " + prettify(k2));
                }
            }
        }
    }
    
    int sign(int i ) {
        if (i < 0) return -1;
        if (i > 0) return 1;
        return 0;
    }
    
    /**
     * Read the external dictionary file, which is already in proper
     * sorted order, and confirm that the collator compares each line as
     * preceding the following line.
     */
    public void TestDictionary() {
        Collator coll = null;
        try {
            coll = Collator.getInstance(new Locale("th", "TH", ""));
        } catch (Exception e) {
            errln("Error: could not construct Thai collator");
            return;
        }
     
        // Read in a dictionary of Thai words
               DataInputStream in = null;
        String fileName = "th18057.txt";
        try {
            in = new DataInputStream(new FileInputStream(TestUtil.getDataFile(
                                                                   fileName)));
        } catch (Exception e) {
            try {
                in.close();
            } catch (IOException ioe) {}
            errln("Error: could not open test file: " + fileName);
            return;        
        }
    
        //
        // Loop through each word in the dictionary and compare it to the previous
        // word.  They should be in sorted order.
        //
        String lastWord = "";
        int line = 0;
        int failed = 0;
        int wordCount = 0;
        String word = readLine(in);
        while (word != null) {
            line++;
    
            // Skip comments and blank lines
            if (word.length() == 0 || word.charAt(0) == 0x23) {
                word = readLine(in);
                continue;
            }
    
            // Show the first 8 words being compared, so we can see what's happening
            ++wordCount;
            if (wordCount <= 8) {
                logln("Word " + wordCount + ": " + word);
            }
    
            if (lastWord.length() > 0) {
                int result = 0;
                try {
                    result = coll.compare(lastWord, word);
                } catch (Exception e) {
                    logln("line" + line + ":" + word);
                    logln("lastWord = " + lastWord);
                    logln(e.getMessage());
                }
        
                if (result >= 0) {
                    failed++;
                    if (MAX_FAILURES_TO_SHOW < 0 || failed <= MAX_FAILURES_TO_SHOW) {
                        String msg = "--------------------------------------------\n"
                                    + line
                                    + " compare(" + lastWord
                                    + ", " + word + ") returned " + result
                                    + ", expected -1\n";
                        CollationKey k1, k2;
                        try {
                            k1 = coll.getCollationKey(lastWord);
                            k2 = coll.getCollationKey(word);
                        } catch (Exception e) {
                            errln("Fail: getCollationKey returned ");
                            return;
                        }
                        msg += "key1: " + prettify(k1) + "\n"
                                    + "key2: " + prettify(k2);
                        errln(msg);
                    }
                }
            }
            lastWord = word;
            word = readLine(in);
        }
    
        if (failed != 0) {
            if (failed > MAX_FAILURES_TO_SHOW) {
                errln("Too many failures; only the first " +
                      MAX_FAILURES_TO_SHOW + " failures were shown");
            }
            errln("Summary: " + failed + " of " + (line - 1) +
                  " comparisons failed");
        }
    
        logln("Words checked: " + wordCount);
    }
    
    String readLine(DataInputStream in) {
        byte[] bytes = new byte[128];
        int i = 0;
        byte c = 0;
        while (i < 128) {
            try {
                c = in.readByte();
            } catch (EOFException ee) {
                return null;
            } catch (IOException e) {
                errln("Cannot read line from the file");
                return null;
            }
            if (c == 0xD || c == 0xA) {
                try {
                    c = in.readByte();
                } catch (EOFException ee) {
                    return null;
                } catch (IOException e) {
                    errln("Cannot read line from the file");
                    return null;
                }
                break;
            }
            bytes[i++] = c;
        }
        
        String line = null;
        try {
            line = new String(bytes, 0, i, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e);
        }
        return line;
    }
    
    String prettify(CollationKey sourceKey) {
        int i;
        byte[] bytes= sourceKey.toByteArray();
        String target = "[";
    
        for (i = 0; i < bytes.length; i++) {
            target += Integer.toHexString(bytes[i]);
            target += " ";
        }
        target += "]";
        return target;
    }
}