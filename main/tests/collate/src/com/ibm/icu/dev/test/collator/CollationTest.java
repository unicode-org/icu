/**
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.dev.test.ModuleTest;
import com.ibm.icu.dev.test.TestDataModule.DataMap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;

public class CollationTest extends ModuleTest{
    // public methods --------------------------------------------------------

    public static void main(String[] args) throws Exception{
        new CollationTest().run(args);
    }

    public CollationTest() {
        super("com/ibm/icu/dev/data/testdata/", "DataDrivenCollationTest");
    }
    
    public void processModules() {
        for (Iterator iter = t.getSettingsIterator(); iter.hasNext();) {
            DataMap setting = (DataMap) iter.next();
            processSetting(setting);
        }
    }
    
    // package private methods ----------------------------------------------
    
    static void doTest(TestFmwk test, RuleBasedCollator col, String source, 
                       String target, int result)
    {
        doTestVariant(test, col, source, target, result);
        if (result == -1) {
            doTestVariant(test, col, target, source, 1);
        } 
        else if (result == 1) {
            doTestVariant(test, col, target, source, -1);
        }
        else {
            doTestVariant(test, col, target, source, 0);
        }

        CollationElementIterator iter = col.getCollationElementIterator(source);
        backAndForth(test, iter);
        iter.setText(target);
        backAndForth(test, iter);
    }
    
    /**
     * Return an integer array containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    static int[] getOrders(CollationElementIterator iter) 
    {
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
    
    static void backAndForth(TestFmwk test, CollationElementIterator iter) 
    {
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
                    while (index > 0 && orders[index] == 0) {
                        index --;
                    } 
                    if (o != orders[index]) {
                        test.errln("Mismatch at index " + index + ": 0x" 
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
            test.errln(msg + index);
    
            iter.reset();
            test.err("next: ");
            while ((o = iter.next()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Integer.toHexString(o) + " ";
                test.err(hexString);
            }
            test.errln("");
            test.err("prev: ");
            while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Integer.toHexString(o) + " ";
                 test.err(hexString);
            }
            test.errln("");
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

    private void processSetting(DataMap settings) {
        RuleBasedCollator col = null;
        // ok i have to be careful here since it seems like we can have
        // multiple locales for each test
        String locale = settings.getString("TestLocale");
        
        if (locale != null) {
            // this is a case where we have locale
            try {
                Locale l = LocaleUtility.getLocaleFromName(locale);
                col = (RuleBasedCollator)Collator.getInstance(l);
            }catch (MissingResourceException e){
                warnln("Could not load the locale data for locale " + locale);
            }catch (Exception e) {
                errln("Error creating collator for locale " + locale);
            }
            logln("Testing collator for locale " + locale);
            processSetting2(settings, col);
        }
        String rules = settings.getString("Rules");
        // ok i have to be careful here since it seems like we can have
        // multiple rules for each test
        if (rules != null) {
            // here we deal with rules
            try {
                col = new RuleBasedCollator(rules);
            }catch (MissingResourceException e){
        warnln("Could not load the locale data: " + e.getMessage());
            } catch (Exception e) {
                errln("Error creating collator for rules " + rules);
            }
            processSetting2(settings, col);
        }
    }

    private void processSetting2(DataMap settings,RuleBasedCollator col)
    {

        // ok i have to be careful here since it seems like we can have
        // multiple rules for each test
        String arguments = settings.getString("Arguments");
        if (arguments != null) {
            handleArguments(col, arguments);
        }
        processTestCases(col);
    }

    /**
     * Reads the options string and sets appropriate attributes in collator
     */
    private void handleArguments(RuleBasedCollator col, String argument) {
        int i = 0;
        boolean printInfo = false;
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
                if(!isModularBuild()){
                    errln("Error in collation arguments, missing ["); // no opening '['
                }
                // !!! following line has no effect
                printInfo=true;
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
        if (printInfo) {
            warnln("Could not load the locale data. Skipping...");
        }
        // !!! effect is odd, if no modular build, this emits no
        // message at all.  How come?  Hmmm.  printInfo is never
        // true if we get here, so this code is never executed.
        /*
        if(printInfo == true && isModularBuild()){
            infoln("Could not load the locale data. Skipping...");
        }
        */
    }

    private void processTestCases(RuleBasedCollator col) {
        for (Iterator iter = t.getDataIterator(); iter.hasNext();) {
            DataMap e1 =  (DataMap) iter.next();
            processSequence(col, e1.getString("sequence"));
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
        List vector = new ArrayList();
        int lastsmallerthanindex = -1;
        getNextInSequence();
        while (getNextInSequence()) {
            String target = m_target_.toString();
            doTest(this, col, m_source_, target, m_relation_);
            int vsize = vector.size();
            for (int i = vsize - 1; i >= 0; i --) {
                String source = (String)vector.get(i);
                if (i > lastsmallerthanindex) {
                    doTest(this, col, source, target, m_relation_);
                }
                else {
                    doTest(this, col, source, target, -1);
                }
            }
            vector.add(target);
            if (m_relation_ < 0) {
                lastsmallerthanindex = vsize - 1;
            }
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

    private static void doTestVariant(TestFmwk test, 
                                      RuleBasedCollator myCollation,
                                      String source, String target, int result)
    {
        boolean printInfo = false;
        int compareResult  = myCollation.compare(source, target);
        if (compareResult != result) {
            
            // !!! if not mod build, error, else nothing.
            // warnln if not build, error, else always print warning.
            // do we need a 'quiet warning?' (err or log).  Hmmm,
            // would it work to have the 'verbose' flag let you 
            // suppress warnings?  Are there ever some warnings you
            // want to suppress, and others you don't?
            if(!test.isModularBuild()){
                test.errln("Comparing \"" + Utility.hex(source) + "\" with \""
                           + Utility.hex(target) + "\" expected " + result
                           + " but got " + compareResult);
            }else{
                printInfo = true;
            }
        }
        CollationKey ssk = myCollation.getCollationKey(source);
        CollationKey tsk = myCollation.getCollationKey(target);
        compareResult = ssk.compareTo(tsk);
        if (compareResult != result) {
            
            if(!test.isModularBuild()){
                test.errln("Comparing CollationKeys of \"" + Utility.hex(source) 
                           + "\" with \"" + Utility.hex(target) 
                           + "\" expected " + result + " but got " 
                           + compareResult);
           }else{
               printInfo = true;
           }
        }
        RawCollationKey srsk = new RawCollationKey();
        myCollation.getRawCollationKey(source, srsk);
        RawCollationKey trsk = new RawCollationKey();
        myCollation.getRawCollationKey(target, trsk);
        compareResult = ssk.compareTo(tsk);
        if (compareResult != result) {
            
            if(!test.isModularBuild()){
                test.errln("Comparing RawCollationKeys of \"" 
                           + Utility.hex(source) 
                           + "\" with \"" + Utility.hex(target) 
                           + "\" expected " + result + " but got " 
                           + compareResult);
           }else{
               printInfo = true;
           }
        }
        // hmmm, but here we issue a warning
        // only difference is, one warning or two, and detailed info or not?
        // hmmm, does seem preferable to omit detail if we know it is due to missing resource data.
        // well, if we label the errors as warnings, we can let people know the details, but
        // also know they may be due to missing resource data.  basically this code is asserting
        // that the errors are due to missing resource data, which may or may not be true.
        if (printInfo) {
            test.warnln("Could not load locale data skipping.");
        }
    }
}
