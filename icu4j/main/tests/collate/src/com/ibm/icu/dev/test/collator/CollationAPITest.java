/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : collate/CollationAPITest
 * Source File: $ICU4CRoot/source/test/intltest/apicoll.cpp
 **/
 
package com.ibm.icu.dev.test.collator;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class CollationAPITest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new CollationAPITest().run(args);
        //new CollationAPITest().TestGetTailoredSet();
    }
    
    /**
     * This tests the collation key related APIs.
     * - constructor/destructor
     * - Collator.getCollationKey
     * - == and != operators
     * - comparison between collation keys
     * - creating collation key with a byte array and vice versa
     */
    public void TestCollationKey() {
        logln("testing CollationKey begins...");
        Collator col = Collator.getInstance();
        col.setStrength(Collator.TERTIARY);
    
        String test1 = "Abcda";
        String test2 = "abcda";
    
        logln("Testing weird arguments");
        CollationKey sortk1 = col.getCollationKey("");
        // key gets reset here
        byte[] bytes = sortk1.toByteArray();
        doAssert(bytes.length == 3 && bytes[0] == 1 && bytes[1] == 1 
                 && bytes[2] == 0, 
                 "Empty string should return an empty collation key");
        // bogus key returned here
        sortk1 = col.getCollationKey(null);
        doAssert(sortk1 == null, "Error code should return bogus collation key");

        logln("Use tertiary comparison level testing ....");
        sortk1 = col.getCollationKey(test1);
        CollationKey sortk2 = col.getCollationKey(test2);
        doAssert((sortk1.compareTo(sortk2)) > 0, "Result should be \"Abcda\" >>> \"abcda\"");

        CollationKey sortkNew;
        sortkNew = sortk1;
        doAssert(!(sortk1.equals(sortk2)), "The sort keys should be different");
        doAssert((sortk1.hashCode() != sortk2.hashCode()), "sort key hashCode() failed");
        doAssert((sortk1.equals(sortkNew)), "The sort keys assignment failed");
        doAssert((sortk1.hashCode() == sortkNew.hashCode()), "sort key hashCode() failed");

        // port from apicoll
        try {
            col = Collator.getInstance();
        } catch (Exception e) {
            errln("Collator.getInstance() failed");
        }
        if (col.getStrength() != Collator.TERTIARY){
            errln("Default collation did not have tertiary strength");
        }

        // Need to use identical strength
        col.setStrength(Collator.IDENTICAL);

        CollationKey key1 = col.getCollationKey(test1);
        CollationKey key2 = col.getCollationKey(test2);
        CollationKey key3 = col.getCollationKey(test2);
    
        doAssert(key1.compareTo(key2) > 0, 
                 "Result should be \"Abcda\" > \"abcda\"");
        doAssert(key2.compareTo(key1) < 0,
                "Result should be \"abcda\" < \"Abcda\"");
        doAssert(key2.compareTo(key3) == 0,
                "Result should be \"abcda\" ==  \"abcda\"");
     
        byte key2identical[] = key2.toByteArray();
    
        logln("Use secondary comparision level testing ...");
        col.setStrength(Collator.SECONDARY);
    
        key1 = col.getCollationKey(test1);
        key2 = col.getCollationKey(test2);
        key3 = col.getCollationKey(test2);
    
        doAssert(key1.compareTo(key2) == 0, 
                "Result should be \"Abcda\" == \"abcda\"");
        doAssert(key2.compareTo(key3) == 0,
                "Result should be \"abcda\" ==  \"abcda\"");
    
        byte tempkey[] = key2.toByteArray();
        byte subkey2compat[] = new byte[tempkey.length];
        System.arraycopy(key2identical, 0, subkey2compat, 0, tempkey.length);
        subkey2compat[subkey2compat.length - 1] = 0;
        doAssert(Arrays.equals(tempkey, subkey2compat),
                 "Binary format for 'abcda' sortkey different for secondary strength!");
    
        logln("testing sortkey ends...");
    }
    
    public void TestRawCollationKey()
    {
        // testing constructors
        RawCollationKey key = new RawCollationKey();
        if (key.bytes != null || key.size != 0) {
            errln("Empty default constructor expected to leave the bytes null "
                  + "and size 0"); 
        }
        byte array[] = new byte[128];
        key = new RawCollationKey(array);
        if (key.bytes != array || key.size != 0) {
            errln("Constructor taking an array expected to adopt it and "
                  + "retaining its size 0"); 
        }
        try {
            key = new RawCollationKey(array, 129);
            errln("Constructor taking an array and a size > array.length "
                  + "expected to throw an exception"); 
        } catch (IndexOutOfBoundsException e) {
                logln("PASS: Constructor failed as expected");
        }
        try {
            key = new RawCollationKey(array, -1);
            errln("Constructor taking an array and a size < 0 "
                  + "expected to throw an exception"); 
        } catch (IndexOutOfBoundsException e) {
                logln("PASS: Constructor failed as expected");
        }
        key = new RawCollationKey(array, array.length >> 1);
        if (key.bytes != array || key.size != (array.length >> 1)) {
            errln("Constructor taking an array and a size, "
                  + "expected to adopt it and take the size specified"); 
        }
        key = new RawCollationKey(10);
        if (key.bytes == null || key.bytes.length != 10 || key.size != 0) {
            errln("Constructor taking a specified capacity expected to "
                  + "create a new internal byte array with length 10 and "
                  + "retain size 0"); 
        }
    }
    
    void doAssert(boolean conditions, String message) {
        if (!conditions) {
            errln(message);
        }
    }
    
    /**
     * This tests the comparison convenience methods of a collator object.
     * - greater than
     * - greater than or equal to
     * - equal to
     */
    public void TestCompare() {
        logln("The compare tests begin : ");
        Collator col = Collator.getInstance(Locale.ENGLISH);

        String test1 = "Abcda";
        String test2 = "abcda";
        logln("Use tertiary comparison level testing ....");
        
        doAssert((!col.equals(test1, test2) ), "Result should be \"Abcda\" != \"abcda\"");
        doAssert((col.compare(test1, test2) > 0 ), "Result should be \"Abcda\" >>> \"abcda\"");

        col.setStrength(Collator.SECONDARY);
        logln("Use secondary comparison level testing ....");
                    
        doAssert((col.equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.compare(test1, test2) == 0), "Result should be \"Abcda\" == \"abcda\"");

        col.setStrength(Collator.PRIMARY);
        logln("Use primary comparison level testing ....");
        
        doAssert((col.equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.compare(test1, test2) == 0 ), "Result should be \"Abcda\" == \"abcda\"");
        logln("The compare tests end.");
    }
    
    /**
    * Tests decomposition setting
    */
    public void TestDecomposition() {
        Collator en_US = null, el_GR = null, vi_VN = null;

        en_US = Collator.getInstance(new Locale("en", "US"));
        el_GR = Collator.getInstance(new Locale("el", "GR"));
        vi_VN = Collator.getInstance(new Locale("vi", "VN"));

        
        // there is no reason to have canonical decomposition in en_US OR default locale */
        if (vi_VN.getDecomposition() != Collator.CANONICAL_DECOMPOSITION)
        {
            errln("vi_VN collation did not have cannonical decomposition for normalization!");
        }

        if (el_GR.getDecomposition() != Collator.CANONICAL_DECOMPOSITION)
        {
            errln("el_GR collation did not have cannonical decomposition for normalization!");
        }

        if (en_US.getDecomposition() != Collator.NO_DECOMPOSITION)
        {
            errln("en_US collation had cannonical decomposition for normalization!");
        }
    }
    
    /**
     * This tests the duplication of a collator object.
     */
    public void TestDuplicate() {
        //Clone does not be implemented 
        Collator col1 = Collator.getInstance(Locale.ENGLISH);
        
        // Collator col2 = (Collator)col1.clone();
        // doAssert(col1.equals(col2), "Cloned object is not equal to the orginal");
        String ruleset = "< a, A < b, B < c, C < d, D, e, E";
        RuleBasedCollator col3 = null;
        try {
            col3 = new RuleBasedCollator(ruleset);
        } catch (Exception e) {
            errln("Failure creating RuleBasedCollator with rule:" + ruleset);
            return;
        }
        doAssert(!col1.equals(col3), "Cloned object is equal to some dummy");
        col3 = (RuleBasedCollator)col1;
        doAssert(col1.equals(col3), "Copied object is not equal to the orginal");
        
    }
    
    /**
     * This tests the CollationElementIterator related APIs.
     * - creation of a CollationElementIterator object
     * - == and != operators
     * - iterating forward
     * - reseting the iterator index
     * - requesting the order properties(primary, secondary or tertiary)
     */
    public void TestElemIter() {
        // logln("testing sortkey begins...");
        Collator col = Collator.getInstance(Locale.ENGLISH);

           
        String testString1 = "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?";
        String testString2 = "Xf_ile What subset of all possible test cases has the lowest probability of detecting the least errors?";
        // logln("Constructors and comparison testing....");
        CollationElementIterator iterator1 = ((RuleBasedCollator)col).getCollationElementIterator(testString1);
        
        CharacterIterator chariter=new StringCharacterIterator(testString1);
        // copy ctor
        CollationElementIterator iterator2 = ((RuleBasedCollator)col).getCollationElementIterator(chariter);
        UCharacterIterator uchariter=UCharacterIterator.getInstance(testString2);
        CollationElementIterator iterator3 = ((RuleBasedCollator)col).getCollationElementIterator(uchariter);
    
        int offset = 0;
        offset = iterator1.getOffset();
        if (offset != 0) {
            errln("Error in getOffset for collation element iterator");
            return;
        }
        iterator1.setOffset(6);
        iterator1.setOffset(0);
        int order1, order2, order3;
        
        order1 = iterator1.next();
        doAssert(!(iterator1.equals(iterator2)), "The first iterator advance failed");
        order2 = iterator2.next();
        
        doAssert((iterator1.equals(iterator2)), "The second iterator advance failed"); 
        doAssert((order1 == order2), "The order result should be the same");
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be the same");
        doAssert((CollationElementIterator.secondaryOrder(order1) == 
            CollationElementIterator.secondaryOrder(order3)), "The secondary orders should be the same");
        doAssert((CollationElementIterator.tertiaryOrder(order1) == 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be the same");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be identical");
        doAssert((CollationElementIterator.tertiaryOrder(order1) != 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be different");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        // invalid test wrong in UCA
        // doAssert((CollationElementIterator.secondaryOrder(order1) != 
        //    CollationElementIterator.secondaryOrder(order3)), "The secondary orders should not be the same");
            
        doAssert((order1 != CollationElementIterator.NULLORDER), "Unexpected end of iterator reached");
    
        iterator1.reset(); 
        iterator2.reset(); 
        iterator3.reset();
        order1 = iterator1.next();
        
        doAssert(!(iterator1.equals(iterator2)), "The first iterator advance failed");
        
        order2 = iterator2.next();
        
        doAssert((iterator1.equals(iterator2)), "The second iterator advance failed");
        doAssert((order1 == order2), "The order result should be the same");
    
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be the same");
        doAssert((CollationElementIterator.secondaryOrder(order1) == 
            CollationElementIterator.secondaryOrder(order3)), "The secondary orders should be the same");
        doAssert((CollationElementIterator.tertiaryOrder(order1) == 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be the same");
    
        order1 = iterator1.next(); 
        order2 = iterator2.next(); 
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be identical");
        doAssert((CollationElementIterator.tertiaryOrder(order1) != 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be different");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        
        // obsolete invalid test, removed
        // doAssert((CollationElementIterator.secondaryOrder(order1) != 
        //    CollationElementIterator.secondaryOrder(order3)), "The secondary orders should not be the same");
        doAssert((order1 != CollationElementIterator.NULLORDER), "Unexpected end of iterator reached");
        doAssert(!(iterator2.equals(iterator3)), "The iterators should be different");
        logln("testing CollationElementIterator ends...");
    }
    
    /**
     * This tests the hashCode method of a collator object.
     */
    public void TestHashCode() {
        logln("hashCode tests begin.");
        Collator col1 = Collator.getInstance(Locale.ENGLISH);
    
        Collator col2 = null;
        Locale dk = new Locale("da", "DK", "");
        try {
            col2 = Collator.getInstance(dk);
        } catch (Exception e) {
            errln("Danish collation creation failed.");
            return;
        }
    
        Collator col3 = null;
        try {
            col3 = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("2nd default collation creation failed.");
            return;
        }
    
        logln("Collator.hashCode() testing ...");
        
        doAssert(col1.hashCode() != col2.hashCode(), "Hash test1 result incorrect" );                 
        doAssert(!(col1.hashCode() == col2.hashCode()), "Hash test2 result incorrect" );              
        doAssert(col1.hashCode() == col3.hashCode(), "Hash result not equal" );               
    
        logln("hashCode tests end.");
        
        String test1 = "Abcda";
        String test2 = "abcda";
        
        CollationKey sortk1, sortk2, sortk3;
                    
        sortk1 = col3.getCollationKey(test1);
        sortk2 = col3.getCollationKey(test2); 
        sortk3 = col3.getCollationKey(test2); 
        
        doAssert(sortk1.hashCode() != sortk2.hashCode(), "Hash test1 result incorrect");               
        doAssert(sortk2.hashCode() == sortk3.hashCode(), "Hash result not equal" );
    }
    
    /**
     * This tests the properties of a collator object.
     * - constructor
     * - factory method getInstance
     * - compare and getCollationKey
     * - get/set decomposition mode and comparison level
     */
    public void TestProperty() {
        /* 
          All the collations have the same version in an ICU
          version.
          ICU 2.0 currVersionArray = {0x18, 0xC0, 0x02, 0x02};
          ICU 2.1 currVersionArray = {0x19, 0x00, 0x03, 0x03};
          ICU 2.8 currVersionArray = {0x29, 0x80, 0x00, 0x04};          
        */    
        logln("The property tests begin : ");
        logln("Test ctors : ");
        Collator col = Collator.getInstance(Locale.ENGLISH);

        logln("Test getVersion");
        // Check for a version greater than some value rather than equality
        // so that we need not update the expected version each time.
        VersionInfo expectedVersion = VersionInfo.getInstance(0x31, 0xC0, 0x00, 0x05);  // from ICU 4.4/UCA 5.2
        doAssert(col.getVersion().compareTo(expectedVersion) >= 0, "Expected minimum version "+expectedVersion.toString()+" got "+col.getVersion().toString());

        logln("Test getUCAVersion");
        // Assume that the UCD and UCA versions are the same,
        // rather than hardcoding (and updating each time) a particular UCA version.
        VersionInfo ucdVersion = UCharacter.getUnicodeVersion();
        doAssert(col.getUCAVersion().equals(ucdVersion), "Expected UCA version "+ucdVersion.toString()+" got "+col.getUCAVersion().toString());

        doAssert((col.compare("ab", "abc") < 0), "ab < abc comparison failed");
        doAssert((col.compare("ab", "AB") < 0), "ab < AB comparison failed");
        doAssert((col.compare("blackbird", "black-bird") > 0), "black-bird > blackbird comparison failed");
        doAssert((col.compare("black bird", "black-bird") < 0), "black bird > black-bird comparison failed");
        doAssert((col.compare("Hello", "hello") > 0), "Hello > hello comparison failed");
    
        logln("Test ctors ends.");
        
        logln("testing Collator.getStrength() method ...");
        doAssert((col.getStrength() == Collator.TERTIARY), "collation object has the wrong strength");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
            
        logln("testing Collator.setStrength() method ...");
        col.setStrength(Collator.SECONDARY);
        doAssert((col.getStrength() != Collator.TERTIARY), "collation object's strength is secondary difference");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
        doAssert((col.getStrength() == Collator.SECONDARY), "collation object has the wrong strength");
    
        logln("testing Collator.setDecomposition() method ...");
        col.setDecomposition(Collator.NO_DECOMPOSITION);
        doAssert((col.getDecomposition() != Collator.CANONICAL_DECOMPOSITION), "Decomposition mode != Collator.CANONICAL_DECOMPOSITION");
        doAssert((col.getDecomposition() == Collator.NO_DECOMPOSITION), "Decomposition mode = Collator.NO_DECOMPOSITION");
        
        
        RuleBasedCollator rcol = (RuleBasedCollator)Collator.getInstance(new Locale("da", "DK"));
        doAssert(rcol.getRules().length() != 0, "da_DK rules does not have length 0");
        
        try {
            col = Collator.getInstance(Locale.FRENCH);
        } catch (Exception e) {
            errln("Creating French collation failed.");
            return;
        }
    
        col.setStrength(Collator.PRIMARY);
        logln("testing Collator.getStrength() method again ...");
        doAssert((col.getStrength() != Collator.TERTIARY), "collation object has the wrong strength");
        doAssert((col.getStrength() == Collator.PRIMARY), "collation object's strength is not primary difference");
            
        logln("testing French Collator.setStrength() method ...");
        col.setStrength(Collator.TERTIARY);
        doAssert((col.getStrength() == Collator.TERTIARY), "collation object's strength is not tertiary difference");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
        doAssert((col.getStrength() != Collator.SECONDARY), "collation object's strength is secondary difference");
    
    }
    public void TestJunkCollator(){
        logln("Create junk collation: ");
        Locale abcd = new Locale("ab", "CD", "");
        
        Collator junk = Collator.getInstance(abcd);
        Collator col = Collator.getInstance();

    
        String colrules = ((RuleBasedCollator)col).getRules();
        String junkrules = ((RuleBasedCollator)junk).getRules();
        doAssert(colrules == junkrules || colrules.equals(junkrules), 
                   "The default collation should be returned.");
        Collator frCol = null;
        try {
            frCol = Collator.getInstance(Locale.FRANCE);
        } catch (Exception e) {
            errln("Creating French collator failed.");
            return;
        }
    
        doAssert(!(frCol.equals(junk)), "The junk is the same as the French collator.");
        logln("Collator property test ended.");
    
    }
    /**
    * This tests the RuleBasedCollator
    * - constructor/destructor
    * - getRules
    */
    public void TestRuleBasedColl() {
        RuleBasedCollator col1 = null, col2 = null, col3 = null, col4 = null;
    
        String ruleset1 = "&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E"; 
        String ruleset2 = "&9 < a, A < b, B < c, C < d, D, e, E";
        String ruleset3 = "&";
        
        try {
            col1 = new RuleBasedCollator(ruleset1);
        } catch (Exception e) {
            // only first error needs to be a warning since we exit function 
            warnln("RuleBased Collator creation failed.");
            return;
        }
    
        try {
            col2 = new RuleBasedCollator(ruleset2);
        } catch (Exception e) {
            errln("RuleBased Collator creation failed.");
            return;
        }
    
        try {
            // empty rules fail
            col3 = new RuleBasedCollator(ruleset3);
            errln("Failure: Empty rules for the collator should fail");
            return;
        } catch (MissingResourceException e) {
            warnln(e.getMessage());
        } catch (Exception e) {
            logln("PASS: Empty rules for the collator failed as expected");
        }
        
        Locale locale = new Locale("aa", "AA");
        try {
            col3 = (RuleBasedCollator)Collator.getInstance(locale);
        } catch (Exception e) {
            errln("Fallback Collator creation failed.: %s");
            return;
        }
    
        try {
            col3 = (RuleBasedCollator)Collator.getInstance();
        } catch (Exception e) {
            errln("Default Collator creation failed.: %s");
            return;
        }
    
        String rule1 = col1.getRules(); 
        String rule2 = col2.getRules();
        String rule3 = col3.getRules();
    
        doAssert(!rule1.equals(rule2), "Default collator getRules failed");
        doAssert(!rule2.equals(rule3), "Default collator getRules failed");
        doAssert(!rule1.equals(rule3), "Default collator getRules failed");
        
        try {
            col4 = new RuleBasedCollator(rule2);
        } catch (Exception e) {
            errln("RuleBased Collator creation failed.");
            return;
        }
    
        String rule4 = col4.getRules();
        doAssert(rule2.equals(rule4), "Default collator getRules failed");
        // tests that modifier ! is always ignored
        String exclamationrules = "!&a<b";
        // java does not allow ! to be the start of the rule
        String thaistr = "\u0e40\u0e01\u0e2d";
        try {
            RuleBasedCollator col5 = new RuleBasedCollator(exclamationrules);
            RuleBasedCollator encol = (RuleBasedCollator)
                                        Collator.getInstance(Locale.ENGLISH);
            CollationElementIterator col5iter 
                                   = col5.getCollationElementIterator(thaistr);
            CollationElementIterator encoliter 
                                   = encol.getCollationElementIterator(
                                                                      thaistr);
            while (true) {
                // testing with en since thai has its own tailoring
                int ce = col5iter.next();
                int ce2 = encoliter.next();
                if (ce2 != ce) {
                    errln("! modifier test failed");
                }
                if (ce == CollationElementIterator.NULLORDER) {
                    break;
                }
            }
        } catch (Exception e) {
            errln("RuleBased Collator creation failed for ! modifier.");
            return;
        }
    }
    
    /**
    * This tests the RuleBasedCollator
    * - getRules
    */
    public void TestRules() {
        RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(new Locale("","","")); //root
            // logln("PASS: RuleBased Collator creation passed");

    
        String rules = coll.getRules();
        if (rules != null && rules.length() != 0) {
            errln("Root tailored rules failed");
        }
    }
    
    public void TestSafeClone() {
        String test1 = "abCda";
        String test2 = "abcda";
        
        // one default collator & two complex ones 
        RuleBasedCollator someCollators[] = {
            (RuleBasedCollator)Collator.getInstance(Locale.ENGLISH),
            (RuleBasedCollator)Collator.getInstance(Locale.KOREA),
            (RuleBasedCollator)Collator.getInstance(Locale.JAPAN)
        };
        RuleBasedCollator someClonedCollators[] = new RuleBasedCollator[3];
        
        // change orig & clone & make sure they are independent 
    
        for (int index = 0; index < someCollators.length; index ++)
        {
            try {
                someClonedCollators[index] 
                            = (RuleBasedCollator)someCollators[index].clone();
            } catch (CloneNotSupportedException e) {
                errln("Error cloning collator");
            }
    
            someClonedCollators[index].setStrength(Collator.TERTIARY);
            someCollators[index].setStrength(Collator.PRIMARY);
            someClonedCollators[index].setCaseLevel(false);
            someCollators[index].setCaseLevel(false);
            
            doAssert(someClonedCollators[index].compare(test1, test2) > 0, 
                     "Result should be \"abCda\" >>> \"abcda\" ");
            doAssert(someCollators[index].compare(test1, test2) == 0, 
                     "Result should be \"abCda\" == \"abcda\" ");
        }
    }

    public void TestGetTailoredSet() 
    {
        logln("testing getTailoredSet...");
        String rules[] = {
            "&a < \u212b",             
            "& S < \u0161 <<< \u0160",
        };
        String data[][] = {
            { "\u212b", "A\u030a", "\u00c5" },        
            { "\u0161", "s\u030C", "\u0160", "S\u030C" }            
        };
    
        int i = 0, j = 0;
        
        RuleBasedCollator coll;
        UnicodeSet set;
        
        for(i = 0; i < rules.length; i++) {
            try {
                logln("Instantiating a collator from "+rules[i]);
                coll = new RuleBasedCollator(rules[i]);
                set = coll.getTailoredSet();
                logln("Got set: "+set.toPattern(true));
                if(set.size() != data[i].length) {
                    errln("Tailored set size different ("+set.size()+") than expected ("+data[i].length+")");
                }
                for(j = 0; j < data[i].length; j++) {
                    logln("Checking to see whether "+data[i][j]+" is in set");
                    if(!set.contains(data[i][j])) {
                        errln("Tailored set doesn't contain "+data[i][j]+"... It should");
                    }
                }
            } catch (Exception e) {
                warnln("Couldn't open collator with rules "+ rules[i]);
            }
        }
    }

    /** 
     * Simple test to see if Collator is subclassable
     */
    public void TestSubClass() 
    {
        class TestCollator extends Collator
        {
            public boolean equals(Object that) {
                return this == that;
            }
    
            public int hashCode() {
                return 0;
            }
            
            public int compare(String source, String target) {
                return source.compareTo(target);
            }
            
            public CollationKey getCollationKey(String source)
            {   return new CollationKey(source, 
                          getRawCollationKey(source, new RawCollationKey()));
            }
            
            public RawCollationKey getRawCollationKey(String source, 
                                                      RawCollationKey key)
            {
                byte temp1[] = source.getBytes();
                byte temp2[] = new byte[temp1.length + 1];
                System.arraycopy(temp1, 0, temp2, 0, temp1.length);
                temp2[temp1.length] = 0;
                if (key == null) {
                    key = new RawCollationKey();
                }
                key.bytes = temp2; 
                key.size = temp2.length;
                return key;
            }
            
            public void setVariableTop(int ce)
            {
            }
            
            public int setVariableTop(String str) 
            {
                return 0;
            }
            
            public int getVariableTop()
            {
                return 0;
            }
            public VersionInfo getVersion()
            {
                return VersionInfo.getInstance(0);
            }
            public VersionInfo getUCAVersion()
            {
                return VersionInfo.getInstance(0);
            }
        }
 
        Collator col1 = new TestCollator();
        Collator col2 = new TestCollator();
        if (col1.equals(col2)) {
            errln("2 different instance of TestCollator should fail");
        }
        if (col1.hashCode() != col2.hashCode()) {
            errln("Every TestCollator has the same hashcode");
        }
        String abc = "abc";
        String bcd = "bcd";
        if (col1.compare(abc, bcd) != abc.compareTo(bcd)) {
            errln("TestCollator compare should be the same as the default " +
                  "string comparison");
        }
        CollationKey key = col1.getCollationKey(abc);
        byte temp1[] = abc.getBytes();
        byte temp2[] = new byte[temp1.length + 1];
        System.arraycopy(temp1, 0, temp2, 0, temp1.length);
        temp2[temp1.length] = 0;
        if (!java.util.Arrays.equals(key.toByteArray(), temp2) 
            || !key.getSourceString().equals(abc)) {
            errln("TestCollator collationkey API is returning wrong values");
        }
        UnicodeSet set = col1.getTailoredSet();
        if (!set.equals(new UnicodeSet(0, 0x10FFFF))) {
            errln("Error getting default tailored set");
        }
    }
    
     /** 
     * Simple test the collator setter and getters
     */
    public void TestSetGet() 
    {
        RuleBasedCollator collator = (RuleBasedCollator)Collator.getInstance();
        int decomp = collator.getDecomposition();
        int strength = collator.getStrength();
        boolean alt = collator.isAlternateHandlingShifted();
        boolean caselevel = collator.isCaseLevel();
        boolean french = collator.isFrenchCollation();
        boolean hquart = collator.isHiraganaQuaternary();
        boolean lowercase = collator.isLowerCaseFirst();
        boolean uppercase = collator.isUpperCaseFirst();
        
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        if (collator.getDecomposition() != Collator.CANONICAL_DECOMPOSITION) {
            errln("Setting decomposition failed");
        }
        collator.setStrength(Collator.QUATERNARY);
        if (collator.getStrength() != Collator.QUATERNARY) {
            errln("Setting strength failed");
        }
        collator.setAlternateHandlingShifted(!alt);
        if (collator.isAlternateHandlingShifted() == alt) {
            errln("Setting alternate handling failed");
        }
        collator.setCaseLevel(!caselevel);
        if (collator.isCaseLevel() == caselevel) {
            errln("Setting case level failed");
        }
        collator.setFrenchCollation(!french);
        if (collator.isFrenchCollation() == french) {
            errln("Setting french collation failed");
        }
        collator.setHiraganaQuaternary(!hquart);
        if (collator.isHiraganaQuaternary() == hquart) {
            errln("Setting hiragana quartenary failed");
        }
        collator.setLowerCaseFirst(!lowercase);
        if (collator.isLowerCaseFirst() == lowercase) {
            errln("Setting lower case first failed");
        }
        collator.setUpperCaseFirst(!uppercase);
        if (collator.isUpperCaseFirst() == uppercase) {
            errln("Setting upper case first failed");
        }   
        collator.setDecompositionDefault();
        if (collator.getDecomposition() != decomp) {
            errln("Setting decomposition default failed");
        }
        collator.setStrengthDefault();
        if (collator.getStrength() != strength) {
            errln("Setting strength default failed");
        }
        collator.setAlternateHandlingDefault();
        if (collator.isAlternateHandlingShifted() != alt) {
            errln("Setting alternate handling default failed");
        }
        collator.setCaseLevelDefault();
        if (collator.isCaseLevel() != caselevel) {
            errln("Setting case level default failed");
        }
        collator.setFrenchCollationDefault();
        if (collator.isFrenchCollation() != french) {
            errln("Setting french handling default failed");
        }
        collator.setHiraganaQuaternaryDefault();
        if (collator.isHiraganaQuaternary() != hquart) {
            errln("Setting Hiragana Quartenary default failed");
        }
        collator.setCaseFirstDefault();
        if (collator.isLowerCaseFirst() != lowercase 
            || collator.isUpperCaseFirst() != uppercase) {
            errln("Setting case first handling default failed");
        }
    }
    
    public void TestBounds() 
    {
        Collator coll = Collator.getInstance(new Locale("sh", ""));
          
        String test[] = { "John Smith", "JOHN SMITH", 
                          "john SMITH", "j\u00F6hn sm\u00EFth",
                          "J\u00F6hn Sm\u00EFth", "J\u00D6HN SM\u00CFTH",
                          "john smithsonian", "John Smithsonian",
        };
      
        String testStr[] = {
                          "\u010CAKI MIHALJ", 
                          "\u010CAKI MIHALJ",
                          "\u010CAKI PIRO\u0160KA", 
                          "\u010CABAI ANDRIJA",
                          "\u010CABAI LAJO\u0160", 
                          "\u010CABAI MARIJA",
                          "\u010CABAI STEVAN", 
                          "\u010CABAI STEVAN",
                          "\u010CABARKAPA BRANKO", 
                          "\u010CABARKAPA MILENKO",
                          "\u010CABARKAPA MIROSLAV", 
                          "\u010CABARKAPA SIMO",
                          "\u010CABARKAPA STANKO", 
                          "\u010CABARKAPA TAMARA",
                          "\u010CABARKAPA TOMA\u0160", 
                          "\u010CABDARI\u0106 NIKOLA",
                          "\u010CABDARI\u0106 ZORICA",
                          "\u010CABI NANDOR",
                          "\u010CABOVI\u0106 MILAN",
                          "\u010CABRADI AGNEZIJA",
                          "\u010CABRADI IVAN",
                          "\u010CABRADI JELENA",
                          "\u010CABRADI LJUBICA",
                          "\u010CABRADI STEVAN",
                          "\u010CABRDA MARTIN",
                          "\u010CABRILO BOGDAN",
                          "\u010CABRILO BRANISLAV",
                          "\u010CABRILO LAZAR",
                          "\u010CABRILO LJUBICA",
                          "\u010CABRILO SPASOJA",
                          "\u010CADE\u0160 ZDENKA",
                          "\u010CADESKI BLAGOJE",
                          "\u010CADOVSKI VLADIMIR",
                          "\u010CAGLJEVI\u0106 TOMA",
                          "\u010CAGOROVI\u0106 VLADIMIR",
                          "\u010CAJA VANKA",
                          "\u010CAJI\u0106 BOGOLJUB",
                          "\u010CAJI\u0106 BORISLAV",
                          "\u010CAJI\u0106 RADOSLAV",
                          "\u010CAK\u0160IRAN MILADIN",
                          "\u010CAKAN EUGEN",
                          "\u010CAKAN EVGENIJE",
                          "\u010CAKAN IVAN",
                          "\u010CAKAN JULIJAN",
                          "\u010CAKAN MIHAJLO",
                          "\u010CAKAN STEVAN",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKARA ANA",
                          "\u010CAKAREVI\u0106 MOMIR",
                          "\u010CAKAREVI\u0106 NEDELJKO",
                          "\u010CAKI \u0160ANDOR",
                          "\u010CAKI AMALIJA",
                          "\u010CAKI ANDRA\u0160",
                          "\u010CAKI LADISLAV",
                          "\u010CAKI LAJO\u0160",
                          "\u010CAKI LASLO" }; 
        
        CollationKey testKey[] = new CollationKey[testStr.length];
        for (int i = 0; i < testStr.length; i ++) {
            testKey[i] = coll.getCollationKey(testStr[i]);
        }
        
        Arrays.sort(testKey);
        for(int i = 0; i < testKey.length - 1; i ++) {
            CollationKey lower 
                           = testKey[i].getBound(CollationKey.BoundMode.LOWER,
                                                 Collator.SECONDARY);
            for (int j = i + 1; j < testKey.length; j ++) {
                CollationKey upper 
                           = testKey[j].getBound(CollationKey.BoundMode.UPPER,
                                                 Collator.SECONDARY);
                for (int k = i; k <= j; k ++) {
                    if (lower.compareTo(testKey[k]) > 0) {
                        errln("Problem with lower bound at i = " + i + " j = "
                              + j + " k = " + k);
                    }
                    if (upper.compareTo(testKey[k]) <= 0) {
                        errln("Problem with upper bound at i = " + i + " j = "
                              + j + " k = " + k);
                    }
                }
            }
        }
        
        for (int i = 0; i < test.length; i ++) 
        {
            CollationKey key = coll.getCollationKey(test[i]);
            CollationKey lower = key.getBound(CollationKey.BoundMode.LOWER,
                                              Collator.SECONDARY);
            CollationKey upper = key.getBound(CollationKey.BoundMode.UPPER_LONG,
                                              Collator.SECONDARY);
            for (int j = i + 1; j < test.length; j ++) {
                key = coll.getCollationKey(test[j]);
                if (lower.compareTo(key) > 0) {
                    errln("Problem with lower bound i = " + i + " j = " + j);
                }
                if (upper.compareTo(key) <= 0) {
                    errln("Problem with upper bound i = " + i + " j = " + j);
                }
            }
        }
    }
    
    public final void TestGetAll() {
        Locale[] list = Collator.getAvailableLocales();
        int errorCount = 0;
        for (int i = 0; i < list.length; ++i) {
            log("Locale name: ");
            log(list[i].toString());
            log(" , the display name is : ");
            logln(list[i].getDisplayName());
            try{
                logln("     ...... Or display as: " + Collator.getDisplayName(list[i]));
                logln("     ...... and display in Chinese: " + 
                      Collator.getDisplayName(list[i],Locale.CHINA)); 
            }catch(MissingResourceException ex){
                errorCount++;
                logln("could not get displayName for " + list[i]);
            }           
        }
        if(errorCount>0){
          warnln("Could not load the locale data.");
        }
    }    

    private boolean
    doSetsTest(UnicodeSet ref, UnicodeSet set, String inSet, String outSet) {
        boolean ok = true;
        set.clear();
        set.applyPattern(inSet);
        
        if(!ref.containsAll(set)) {
            err("Some stuff from "+inSet+" is not present in the set.\nMissing:"+
                set.removeAll(ref).toPattern(true)+"\n");
            ok = false;
        }

        set.clear();
        set.applyPattern(outSet);
        if(!ref.containsNone(set)) {
            err("Some stuff from "+outSet+" is present in the set.\nUnexpected:"+
                set.retainAll(ref).toPattern(true)+"\n");
            ok = false;
        }
        return ok;
    }
    
    public void TestGetContractions()throws Exception {
        /*        static struct {
         const char* locale;
         const char* inConts;
         const char* outConts;
         const char* inExp;
         const char* outExp;
         const char* unsafeCodeUnits;
         const char* safeCodeUnits;
         }
         */
        String tests[][] = {
                { "ru", 
                    "[{\u0418\u0306}{\u0438\u0306}]",
                    "[\u0439\u0457]",
                    "[\u00e6]",
                    "[ae]",
                    "[\u0418\u0438]",
                    "[aAbBxv]"
                },
                { "uk",
                    "[{\u0406\u0308}{\u0456\u0308}{\u0418\u0306}{\u0438\u0306}]",
                    "[\u0407\u0419\u0439\u0457]",
                    "[\u00e6]",
                    "[ae]",
                    "[\u0406\u0456\u0418\u0438]",
                    "[aAbBxv]"
                },
                { "sh",
                    "[{C\u0301}{C\u030C}{C\u0341}{DZ\u030C}{Dz\u030C}{D\u017D}{D\u017E}{lj}{nj}]",
                    "[{\u309d\u3099}{\u30fd\u3099}]",
                    "[\u00e6]",
                    "[a]",
                    "[nlcdzNLCDZ]",
                    "[jabv]"
                },
                { "ja",
                    "[{\u3053\u3099\u309D}{\u3053\u3099\u309D\u3099}{\u3053\u3099\u309E}{\u3053\u3099\u30FC}{\u3053\u309D}{\u3053\u309D\u3099}{\u3053\u309E}{\u3053\u30FC}{\u30B3\u3099\u30FC}{\u30B3\u3099\u30FD}{\u30B3\u3099\u30FD\u3099}{\u30B3\u3099\u30FE}{\u30B3\u30FC}{\u30B3\u30FD}{\u30B3\u30FD\u3099}{\u30B3\u30FE}]",
                    "[{\u30FD\u3099}{\u309D\u3099}{\u3053\u3099}{\u30B3\u3099}{lj}{nj}]",
                    "[\u30FE\u00e6]",
                    "[a]",
                    "[\u3099]",
                    "[]"
                }
        };
        
        
        
        
        RuleBasedCollator coll = null;
        int i = 0;
        UnicodeSet conts = new UnicodeSet();
        UnicodeSet exp = new UnicodeSet();
        UnicodeSet set = new UnicodeSet();
        
        for(i = 0; i < tests.length; i++) {
            logln("Testing locale: "+ tests[i][0]);
            coll = (RuleBasedCollator)Collator.getInstance(new ULocale(tests[i][0]));
            coll.getContractionsAndExpansions(conts, exp, true);
            boolean ok = true;
            logln("Contractions "+conts.size()+":\n"+conts.toPattern(true));
            ok &= doSetsTest(conts, set, tests[i][1], tests[i][2]);
            logln("Expansions "+exp.size()+":\n"+exp.toPattern(true));
            ok &= doSetsTest(exp, set, tests[i][3], tests[i][4]);
            if(!ok) {
                // In case of failure, log the rule string for better diagnostics.
                String rules = coll.getRules(false);
                logln("Collation rules (getLocale()="+
                        coll.getLocale(ULocale.ACTUAL_LOCALE).toString()+"): "+
                        Utility.escape(rules));
            }

            // No unsafe set in ICU4J
            //noConts = ucol_getUnsafeSet(coll, conts, &status);
            //doSetsTest(conts, set, tests[i][5], tests[i][6]);
            //log_verbose("Unsafes "+conts.size()+":\n"+conts.toPattern(true)+"\n");
        }
    }
    private static final String bigone = "One";
    private static final String littleone = "one";
    
    public void TestClone() {
        logln("\ninit c0");
        RuleBasedCollator c0 = (RuleBasedCollator)Collator.getInstance();
        c0.setStrength(Collator.TERTIARY);
        dump("c0", c0);

        logln("\ninit c1");
        RuleBasedCollator c1 = (RuleBasedCollator)Collator.getInstance();
        c1.setStrength(Collator.TERTIARY);
        c1.setUpperCaseFirst(!c1.isUpperCaseFirst());
        dump("c0", c0);
        dump("c1", c1);
        try{
            logln("\ninit c2");
            RuleBasedCollator c2 = (RuleBasedCollator)c1.clone();
            c2.setUpperCaseFirst(!c2.isUpperCaseFirst());
            dump("c0", c0);
            dump("c1", c1);
            dump("c2", c2);
            if(c1.equals(c2)){
                errln("The cloned objects refer to same data");
            }
        }catch(CloneNotSupportedException ex){
            errln("Could not clone the collator");
        }
    }

    private void dump(String msg, RuleBasedCollator c) {
        logln(msg + " " + c.compare(bigone, littleone) +
                           " s: " + c.getStrength() +
                           " u: " + c.isUpperCaseFirst());
    }
    
    /*
     * Tests the method public void setStrength(int newStrength)
     */
    public void TestSetStrength() {
        // Tests when if ((newStrength != PRIMARY) && ... ) is true
        int[] cases = { -1, 4, 5 };
        for (int i = 0; i < cases.length; i++) {
            try {
                // Assuming -1 is not one of the values
                Collator c = Collator.getInstance();
                c.setStrength(cases[i]);
                errln("Collator.setStrength(int) is suppose to return "
                        + "an exception for an invalid newStrength value of " + cases[i]);
            } catch (Exception e) {
            }
        }
    }

    /*
     * Tests the method public void setDecomposition(int decomposition)
     */
    public void TestSetDecomposition() {
        // Tests when if ((decomposition != NO_DECOMPOSITION) && ...) is true
        int[] cases = { 0, 1, 14, 15, 18, 19 };
        for (int i = 0; i < cases.length; i++) {
            try {
                // Assuming -1 is not one of the values
                Collator c = Collator.getInstance();
                c.setDecomposition(cases[i]);
                errln("Collator.setDecomposition(int) is suppose to return "
                        + "an exception for an invalid decomposition value of " + cases[i]);
            } catch (Exception e) {
            }
        }
    }
    
    /*
     * Tests the class CollatorFactory
     */
    public void TestCreateCollator() {
        // The following class override public Collator createCollator(Locale loc)
        class TestCreateCollator extends CollatorFactory {
            public Set<String> getSupportedLocaleIDs() {
                return new HashSet<String>();
            }

            public TestCreateCollator() {
                super();
            }

            public Collator createCollator(ULocale c) {
                return null;
            }
        }
        // The following class override public Collator createCollator(ULocale loc)
        class TestCreateCollator1 extends CollatorFactory {
            public Set<String> getSupportedLocaleIDs() {
                return new HashSet<String>();
            }

            public TestCreateCollator1() {
                super();
            }

            public Collator createCollator(Locale c) {
                return null;
            }
            public boolean visible(){
                return false;
            }
        }

        /*
         * Tests the method public Collator createCollator(Locale loc) using TestCreateCollator1 class
         */
        try {
            TestCreateCollator tcc = new TestCreateCollator();
            tcc.createCollator(new Locale("en_US"));
        } catch (Exception e) {
            errln("Collator.createCollator(Locale) was not suppose to " + "return an exception.");
        }

        /*
         * Tests the method public Collator createCollator(ULocale loc) using TestCreateCollator1 class
         */
        try {
            TestCreateCollator1 tcc = new TestCreateCollator1();
            tcc.createCollator(new ULocale("en_US"));
        } catch (Exception e) {
            errln("Collator.createCollator(ULocale) was not suppose to " + "return an exception.");
        }
        
        /*
         * Tests the method gpublic String getDisplayName(Locale objectLocale, Locale displayLocale) using TestCreateCollator1 class
         */
        try {
            TestCreateCollator tcc = new TestCreateCollator();
            tcc.getDisplayName(new Locale("en_US"), new Locale("jp_JP"));
        } catch (Exception e) {
            errln("Collator.getDisplayName(Locale,Locale) was not suppose to return an exception.");
        }
        
        /*
         * Tests the method public String getDisplayName(ULocale objectLocale, ULocale displayLocale) using TestCreateCollator1 class
         */
        try {
            TestCreateCollator1 tcc = new TestCreateCollator1();
            tcc.getDisplayName(new ULocale("en_US"), new ULocale("jp_JP"));
        } catch (Exception e) {
            errln("Collator.getDisplayName(ULocale,ULocale) was not suppose to return an exception.");
        }
    }
    /* Tests the method
     * public static final String[] getKeywordValues(String keyword)
     */
    @SuppressWarnings("static-access")
    public void TestGetKeywordValues(){
        // Tests when "if (!keyword.equals(KEYWORDS[0]))" is true
        String[] cases = {"","dummy"};
        for(int i=0; i<cases.length; i++){
            try{
                Collator c = Collator.getInstance();
                @SuppressWarnings("unused")
                String[] s = c.getKeywordValues(cases[i]);
                errln("Collator.getKeywordValues(String) is suppose to return " +
                        "an exception for an invalid keyword.");
            } catch(Exception e){}
        }
    }
}
