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
 * Port From:   ICU4C v2.1 : collate/CollationIteratorTest
 * Source File: $ICU4CRoot/source/test/intltest/itercoll.cpp
 **/

package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;
import com.ibm.icu.lang.UCharacter;
import java.util.Locale;
import java.text.CharacterIterator;

public class CollationIteratorTest extends TestFmwk {
    
    String test1 = "What subset of all possible test cases?";
    String test2 = "has the highest probability of detecting";
   
    public static void main(String[] args) throws Exception {
        new CollationIteratorTest().run(args);
        // new CollationIteratorTest().TestUnicodeChar();
    }
    
    /*
     * @bug 4157299
     */
    public void TestClearBuffers(/* char* par */) {
        RuleBasedCollator c = null;
        try {
            c = new RuleBasedCollator("&a < b < c & ab = d");
        } catch (Exception e) {
            errln("Couldn't create a RuleBasedCollator.");
            return;
        }
    
        String source = "abcd";
        CollationElementIterator i = c.getCollationElementIterator(source);
        int e0 = 0;
        try {
            e0 = i.next();    // save the first collation element
        } catch (Exception e) {
            errln("call to i.next() failed.");
            return;
        }
            
        try {
            i.setOffset(3);        // go to the expanding character
        } catch (Exception e) {
            errln("call to i.setOffset(3) failed.");
            return;
        }
        
        try {
            i.next();                // but only use up half of it
        } catch (Exception e) {
            errln("call to i.next() failed.");
            return;
        }
            
        try {
            i.setOffset(0);        // go back to the beginning
        } catch (Exception e) {
            errln("call to i.setOffset(0) failed. ");
        }
        
        int e = 0;
        try {
            e = i.next();    // and get this one again
        } catch (Exception ee) {
            errln("call to i.next() failed. ");
            return;
        }
        
        if (e != e0) {
            errln("got 0x" + Integer.toHexString(e) + ", expected 0x" + Integer.toHexString(e0));
        }
    }
    
    /** @bug 4108762
     * Test for getMaxExpansion()
     */
    public void TestMaxExpansion(/* char* par */) {
        String rule = "&a < ab < c/aba < d < z < ch";
        RuleBasedCollator coll = null;
        try {
            coll = new RuleBasedCollator(rule);
        } catch (Exception e) {
            errln("Fail to create RuleBasedCollator");
            return;
        }
        char ch = 0;
        String str = String.valueOf(ch);
    
        CollationElementIterator iter   = coll.getCollationElementIterator(str);
    
        while (ch < 0xFFFF) {
            int count = 1;
            ch ++;
            str = String.valueOf(ch);
            iter.setText(str);
            int order = iter.previous();
    
            // thai management 
            if (order == 0) {
                order = iter.previous();
            }
    
            while (iter.previous() != CollationElementIterator.NULLORDER) {
                count ++; 
            }
    
            if (iter.getMaxExpansion(order) < count) {
                errln("Failure at codepoint " + ch + ", maximum expansion count < " + count);
            }
        }
    }
    
    /**
     * Test for getOffset() and setOffset()
     */
    public void TestOffset(/* char* par */) {
        RuleBasedCollator en_us;
        try {
            en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);    
        } catch (Exception e) {
            errln("ERROR: in creation of collator of ENGLISH locale");
            return;
        }

        CollationElementIterator iter = en_us.getCollationElementIterator(test1);
    
        // Run all the way through the iterator, then get the offset
        int[] orders = getOrders(iter);
        logln("orders.length = " + orders.length);
        
        int offset = iter.getOffset();
    
        if (offset != test1.length()) {
            String msg1 = "offset at end != length: ";
            String msg2 = " vs ";
            errln(msg1 + offset + msg2 + test1.length());
        }
    
        // Now set the offset back to the beginning and see if it works
        CollationElementIterator pristine = en_us.getCollationElementIterator(test1);
        
        try {
            iter.setOffset(0);
        } catch(Exception e) {
            errln("setOffset failed.");
        }
        assertEqual(iter, pristine);
    
        // TODO: try iterating halfway through a messy string.
    }
    
    /**
     * Return an integer array containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    int[] getOrders(CollationElementIterator iter) {
        int maxSize = 100;
        int size = 0;
        int[] orders = new int[maxSize];
    
        int order;
        while ((order = iter.next()) != CollationElementIterator.NULLORDER) {
            if (size == maxSize) {
                maxSize *= 2;
                int[] temp = new int[maxSize];
                System.arraycopy(orders, 0, temp,  0, size);
                orders = temp;
            }
            orders[size++] = order;
        }
    
        if (maxSize > size) {
            int[] temp = new int[size];
            System.arraycopy(orders, 0, temp,  0, size);
            orders = temp;
        }
        return orders;
    }

    void assertEqual(CollationElementIterator i1, CollationElementIterator i2) {
        int c1, c2, count = 0;
        do {
            c1 = i1.next();
            c2 = i2.next();
            if (c1 != c2) {
                errln("    " + count + ": strength(0x" + 
                    Integer.toHexString(c1) + ") != strength(0x" + Integer.toHexString(c2) + ")");
                break;
            }
            count += 1;
        } while (c1 != CollationElementIterator.NULLORDER);
        backAndForth(i1);
        backAndForth(i2);
    }
    
    /**
     * Test for CollationElementIterator.previous()
     *
     * @bug 4108758 - Make sure it works with contracting characters
     * 
     */
    public void TestPrevious(/* char* par */) {
        RuleBasedCollator en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);
        CollationElementIterator iter = en_us.getCollationElementIterator(test1);
    
        // A basic test to see if it's working at all
        backAndForth(iter);
    
        // Test with a contracting character sequence
        String source;
        RuleBasedCollator c1 = null;
        try {
            c1 = new RuleBasedCollator("&a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH");
        } catch (Exception e) {
            errln("Couldn't create a RuleBasedCollator with a contracting sequence.");
            return;
        }
    
        source = "abchdcba";
        iter = c1.getCollationElementIterator(source);
        backAndForth(iter);
    
        // Test with an expanding character sequence
        RuleBasedCollator c2 = null;
        try {
            c2 = new RuleBasedCollator("&a < b < c/abd < d");
        } catch (Exception e ) {
            errln("Couldn't create a RuleBasedCollator with an expanding sequence.");
            return;
        }
    
        source = "abcd";
        iter = c2.getCollationElementIterator(source);
        backAndForth(iter);
    
        // Now try both
        RuleBasedCollator c3 = null;
        try {
            c3 = new RuleBasedCollator("&a < b < c/aba < d < z < ch");
        } catch (Exception e) {
            errln("Couldn't create a RuleBasedCollator with both an expanding and a contracting sequence.");
            return;
        }
        
        source = "abcdbchdc";
        iter = c3.getCollationElementIterator(source);
        backAndForth(iter);
    
        source= "\u0e41\u0e02\u0e41\u0e02\u0e27abc";
        Collator c4 = null;
        try {
            c4 = Collator.getInstance(new Locale("th", "TH", ""));
        } catch (Exception e) {
            errln("Couldn't create a collator");
            return;
        }
        
        iter = ((RuleBasedCollator)c4).getCollationElementIterator(source);
        backAndForth(iter);
       
        source= "\u0061\u30CF\u3099\u30FC";
        Collator c5 = null;
        try {
            c5 = Collator.getInstance(new Locale("ja", "JP", ""));
        } catch (Exception e) {
            errln("Couldn't create Japanese collator\n");
        }
        iter = ((RuleBasedCollator)c5).getCollationElementIterator(source);
        
        backAndForth(iter);
    }
    
    void backAndForth(CollationElementIterator iter) {
        // Run through the iterator forwards and stick it into an array
        iter.reset();
        int[] orders = getOrders(iter);
    
        // Now go through it backwards and make sure we get the same values
        int index = orders.length;
        int o;
    
        // reset the iterator
        iter.reset();
    
        while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
            if (o != orders[--index]) {
                if (o == 0) {
                    index ++;
                } else {
                    while (index > 0 && orders[--index] == 0) {
                    } if (o != orders[index]) {
                        errln("Mismatch at index " + index + ": 0x" 
                            + Integer.toHexString(orders[index]) + " vs 0x" + Integer.toHexString(o));
                        break;
                    }
                }
            }
        }
    
        while (index != 0 && orders[index - 1] == 0) {
          index --;
        }
    
        if (index != 0) {
            String msg = "Didn't get back to beginning - index is ";
            errln(msg + index);
    
            iter.reset();
            err("next: ");
            while ((o = iter.next()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Integer.toHexString(o) + " ";
                err(hexString);
            }
            errln("");
            err("prev: ");
            while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Integer.toHexString(o) + " ";
                 err(hexString);
            }
            errln("");
        }
    }
    
    /**
     * Test for setText()
     */
    public void TestSetText(/* char* par */) {
        RuleBasedCollator en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);
        CollationElementIterator iter1 = en_us.getCollationElementIterator(test1);
        CollationElementIterator iter2 = en_us.getCollationElementIterator(test2);
    
        // Run through the second iterator just to exercise it
        int c = iter2.next();
        int i = 0;
    
        while ( ++i < 10 && c != CollationElementIterator.NULLORDER) {
            try {
                c = iter2.next();
            } catch (Exception e) {
                errln("iter2.next() returned an error.");
                break;
            }
        }
    
        // Now set it to point to the same string as the first iterator
        try {
            iter2.setText(test1);
        } catch (Exception e) {
            errln("call to iter2->setText(test1) failed.");
            return;
        }
        assertEqual(iter1, iter2);
        
        iter1.reset();
        //now use the overloaded setText(ChracterIterator&, UErrorCode) function to set the text
        CharacterIterator chariter = new StringCharacterIterator(test1);
        try {
        iter2.setText(chariter);
        } catch (Exception e ) {
            errln("call to iter2->setText(chariter(test1)) failed.");
            return;
        }
        assertEqual(iter1, iter2);
    }

    /**
     * Test for CollationElementIterator previous and next for the whole set of
     * unicode characters.
     */
    public void TestUnicodeChar() {
        RuleBasedCollator en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);
        CollationElementIterator iter;
        char codepoint;
        StringBuffer source = new StringBuffer();
        for (codepoint = 0xe40; codepoint < 0xe42; codepoint ++) {
            if (UCharacter.isDefined(codepoint)) {
                source.append(codepoint);
            }
        }
        iter = en_us.getCollationElementIterator(source.toString());
        // A basic test to see if it's working at all 
        backAndForth(iter);
        for (codepoint = 1; codepoint < 0xFFFE;) {
            source.delete(0, source.length());
            while (codepoint % 0xFF != 0) {
                if (UCharacter.isDefined(codepoint)) {
                    source.append(codepoint);
                }
                codepoint ++;
            }
            
            if (UCharacter.isDefined(codepoint)) {
                source.append(codepoint);
            }
            
            if (codepoint != 0xFFFF) {
                codepoint ++;
            }
            iter = en_us.getCollationElementIterator(source.toString());
            // A basic test to see if it's working at all 
            backAndForth(iter);
        }
    }
    
    /**
    * Testing the discontiguous contractions
    */
    public void TestDiscontiguous() 
    {
        String rulestr ="&z < AB < X\u0300 < ABC < X\u0300\u0315";
        String src[] = {"ADB", "ADBC", "A\u0315B", "A\u0315BC",
                        // base character blocked
                        "XD\u0300", "XD\u0300\u0315",
                        // non blocking combining character
                        "X\u0319\u0300", "X\u0319\u0300\u0315",
                        // blocking combining character
                        "X\u0314\u0300", "X\u0314\u0300\u0315",
                        // contraction prefix
                        "ABDC", "AB\u0315C","X\u0300D\u0315", 
                        "X\u0300\u0319\u0315", "X\u0300\u031A\u0315",
                        // ends not with a contraction character
                        "X\u0319\u0300D", "X\u0319\u0300\u0315D", 
                        "X\u0300D\u0315D", "X\u0300\u0319\u0315D", 
                        "X\u0300\u031A\u0315D"
        };
        String tgt[] = {// non blocking combining character
                        "A D B", "A D BC", "A \u0315 B", "A \u0315 BC",
                        // base character blocked
                        "X D \u0300", "X D \u0300\u0315",
                        // non blocking combining character
                        "X\u0300 \u0319", "X\u0300\u0315 \u0319",
                        // blocking combining character
                        "X \u0314 \u0300", "X \u0314 \u0300\u0315",
                        // contraction prefix
                        "AB DC", "AB \u0315 C","X\u0300 D \u0315", 
                        "X\u0300\u0315 \u0319", "X\u0300 \u031A \u0315",
                        // ends not with a contraction character
                        "X\u0300 \u0319D", "X\u0300\u0315 \u0319D", 
                        "X\u0300 D\u0315D", "X\u0300\u0315 \u0319D", 
                        "X\u0300 \u031A\u0315D"
        };
        int count = 0;
        try {
            RuleBasedCollator coll = new RuleBasedCollator(rulestr);
            CollationElementIterator iter 
                                        = coll.getCollationElementIterator("");
            CollationElementIterator resultiter 
                                        = coll.getCollationElementIterator("");    
            while (count < src.length) {
                iter.setText(src[count]);
                int s = 0;
                while (s < tgt[count].length()) {
                    int e = tgt[count].indexOf(' ', s);
                    if (e < 0) {
                        e = tgt[count].length();
                    }
                    String resultstr = tgt[count].substring(s, e);
                    resultiter.setText(resultstr);
                    int ce = resultiter.next();
                    while (ce != CollationElementIterator.NULLORDER) {
                        if (ce != iter.next()) {
                            errln("Discontiguos contraction test mismatch at" 
                                  + count);
                            return;
                        }
                        ce = resultiter.next();
                    }
                    s = e + 1;
                }
                iter.reset();
                backAndForth(iter);
                count ++;
            }
        }
        catch (Exception e) {
            errln("Error running discontiguous tests " + e.toString());
        }
    }

}