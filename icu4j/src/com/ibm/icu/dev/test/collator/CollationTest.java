/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/collator/CollationTest.java,v $
 * $Date: 2002/08/31 04:55:10 $
 * $Revision: 1.5 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.ModuleTest;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.LocaleUtility;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class CollationTest extends ModuleTest 
{
    // public methods --------------------------------------------------------
    
    public static void main(String[] args) throws Exception 
    {
        new CollationTest().run(args);
        /* CollationTest test = new CollationTest();
        if (test.validate()) {
            test.TestCIgnorableContraction();
        }*/
    }

    public CollationTest() {
        super("processModules");
    }

    public void processModules() {
        while (nextSettings()) {
            processTest();
        }
    }
    
    // private data members --------------------------------------------------
    
    private String m_sequence_;
    private int m_sequenceIndex_;
    private String m_source_;
    private StringBuffer m_target_ = new StringBuffer();
    private int m_nextRelation_;
    private int m_relation_;
    
    // private methods -------------------------------------------------------
    
    private void processTest() {
        RuleBasedCollator col = null;
        // ok i have to be careful here since it seems like we can have
        // multiple locales for each test 
        String locale = settings.getString("TestLocale");
        if (locale != null) {
            // this is a case where we have locale 
            try {
                if (locale.equalsIgnoreCase("root")) {
                    col = (RuleBasedCollator)Collator.getInstance(
                                                               Locale.ENGLISH);
                }
                else {
                    Locale l = LocaleUtility.getLocaleFromName(locale);
                    col = (RuleBasedCollator)Collator.getInstance(l);          
                }
            } catch (Exception e) {
                errln("Error creating collator for locale " + locale);
            }
            logln("Testing collator for locale %s\n" + locale);
            processCollatorTests(col);
        }
        String rules = settings.getString("Rules");
        // ok i have to be careful here since it seems like we can have
        // multiple rules for each test 
        if (rules != null) {
            // here we deal with rules
            try {
                col = new RuleBasedCollator(rules);
            } catch (Exception e) {
                errln("Error creating collator for rules " + rules);
            }
            processCollatorTests(col);
        }
    }
    
    private void processCollatorTests(RuleBasedCollator col) 
    {
        
        // ok i have to be careful here since it seems like we can have
        // multiple rules for each test 
        String arguments = settings.getString("Arguments");
        if (arguments != null) {
            processArguments(col, arguments);
        }
        processReadyCollator(col);      
    }
    
    /** 
     * Reads the options string and sets appropriate attributes in collator 
     */
    private void processArguments(RuleBasedCollator col, String argument) {
        int i = 0;
        while (i < argument.length()) { 
            if (!UCharacter.isWhitespace(argument.charAt(i))) { 
                // eat whitespace
                break;
            }
            i ++;
        }
        while (i < argument.length()) {
            // skip opening '['
            if (argument.charAt(i) == '[') {
                i ++;
            } 
            else {
                errln("Error in collation arguments, missing ["); // no opening '['
                return;
            }
            
            int value = argument.indexOf(' ', i);
            String option = argument.substring(i, value);
            i = argument.indexOf(']', value);
            String optionvalue = argument.substring(value + 1, i);
            i ++;
            // some options are not added because they have no public apis yet
            // TODO add the rest of the options
            if (option.equalsIgnoreCase("alternate")) {
                if (optionvalue.equalsIgnoreCase("non-ignorable")) {
                    col.setAlternateHandlingShifted(false);
                }
                else {
                    col.setAlternateHandlingShifted(true);
                }
            }
            else if (option.equals("strength")) {
                if (optionvalue.equalsIgnoreCase("1")) {
                    col.setStrength(Collator.PRIMARY);
                }
                else if (optionvalue.equalsIgnoreCase("2")) {
                    col.setStrength(Collator.SECONDARY);
                }
                else if (optionvalue.equalsIgnoreCase("3")) {
                    col.setStrength(Collator.TERTIARY);
                }
                else if (optionvalue.equalsIgnoreCase("4")) {
                    col.setStrength(Collator.QUATERNARY);
                }
            }
        }
    }

    private void processReadyCollator(RuleBasedCollator col) {
       while (nextCase()) {
            // this is very sad, it is alittle awkward to write the c rb 
            // to have an object array of an object array of a 1 element 
            // string array. so now we have an object array of a 1 element 
            // object array of string arrays.
            String sequence[] = testcase.getStringArray("sequence");
            for (int i = 0; i < sequence.length; i ++) {
                processSequence(col, sequence[i]);
            }
       }
    }
    
    private void processSequence(RuleBasedCollator col, String sequence) {
        // TODO: have a smarter tester that remembers the sequence and ensures 
        // that the complete sequence is in order. That is why I have made a 
        // constraint in the sequence format.     
        m_sequence_ = sequence;
        m_sequenceIndex_ = 0;
        m_nextRelation_ = -1;
        m_target_.delete(0, m_target_.length());
        Vector vector = new Vector();
    
        while (getNextInSequence()) {    
            String target = m_target_.toString();
            doTest(col, m_source_, target, m_relation_);
            int vsize = vector.size();
            for (int i = 0; i < vsize; i ++) {
                String source = (String)vector.elementAt(i);   
                doTest(col, source, target, m_relation_);
            }
            vector.addElement(target);
        }
    }
    
    /** 
     * Parses the sequence to be tested 
     */
    private boolean getNextInSequence() {
        if (m_sequenceIndex_ >= m_sequence_.length()) {
            return false;
        }
        
        boolean quoted = false;
        boolean quotedsingle = false;
        boolean done = false;
        int i = m_sequenceIndex_;
        int offset = 0;
        m_source_ = m_target_.toString();
        m_relation_ = m_nextRelation_;
        m_target_.delete(0, m_target_.length());
        while (i < m_sequence_.length() && !done) {
            int ch = UTF16.charAt(m_sequence_, i);
            if (UCharacter.isSupplementary(ch)) {
                i += 2;
            }
            else {
                i ++;
            }
            if (!quoted) {
                if (UCharacter.isWhitespace(ch)) {
                    continue;
                }
                switch (ch) {
                    case 0x003C : // <
                        m_nextRelation_ = -1;
                        done = true;
                        break;
                    case 0x003D : // = 
                        m_nextRelation_ = 0;
                        done = true;
                        break;
                    case 0x003E : // >
                        m_nextRelation_ = 1;
                        done = true;
                        break;
                    case 0x0027 : // ' very basic quoting
                        quoted = true;
                        quotedsingle = false;
                        break;
                    case 0x005c : // \ single quote
                        quoted = true;
                        quotedsingle = true;
                        break;
                    default:
                        UTF16.insert(m_target_, offset, ch);
                        if (UCharacter.isSupplementary(ch)) {
                            offset += 2;
                        }
                        else {
                            offset ++;
                        }
                    }
                } 
                else {
                      if (ch == 0x0027) {
                          quoted = false;
                      } 
                      else {
                          UTF16.insert(m_target_, offset, ch);
                          if (UCharacter.isSupplementary(ch)) {
                              offset += 2;
                          }
                          else {
                              offset ++;
                          }
                      }
                      if (quotedsingle) {
                          quoted = false;
                      }
                }
          }
          if (quoted == true) {
              errln("Quote in sequence not closed!");
              return false;
          }
        
        
          m_sequenceIndex_ = i;
          return true;
    }
    
    private void doTestVariant(RuleBasedCollator myCollation, 
                               String source, String target, int result)
    {
        int compareResult  = myCollation.compare(source, target);
        if (compareResult != result) {
            errln("Comparing \"" + Utility.hex(source) + "\" with \"" 
                  + Utility.hex(target) + "\" expected " + result 
                  + " but got " + compareResult);
        }
        CollationKey ssk = myCollation.getCollationKey(source);
        CollationKey tsk = myCollation.getCollationKey(target);
        compareResult = ssk.compareTo(tsk);
        if (compareResult != result) {
            errln("Comparing sortkeys of \"" + Utility.hex(source) + "\" with \"" 
                  + Utility.hex(target) + "\" expected " + result 
                  + " but got " + compareResult);
        } 
    }
    
    private void doTest(RuleBasedCollator myCollation, 
                               String source, String target, int result)
    {
        doTestVariant(myCollation, source, target, result);
        if (result == 0) {
            doTestVariant(myCollation, target, source, result);            
        }
        else if (result < 0) {
            doTestVariant(myCollation, target, source, 1); 
        }
        else {
            doTestVariant(myCollation, target, source, -1); 
        }
    }
}
