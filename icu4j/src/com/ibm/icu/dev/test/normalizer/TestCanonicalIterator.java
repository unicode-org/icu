/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/normalizer/TestCanonicalIterator.java,v $ 
 * $Date: 2002/03/14 15:33:25 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.impl.Utility;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

// TODO: fit into test framework

public class TestCanonicalIterator extends TestFmwk {
	
    public static void main(String[] args) throws Exception {
        new TestCanonicalIterator().run(args);
    }
	
    static final String testArray[] = {
        "Åd\u0307\u0327",
        "\u010d\u017E",
        "x\u0307\u0327",
    };
    
    public void TestExhaustive() {
    	int counter = 0;
    	CanonicalIterator it = new CanonicalIterator("");
    	for (int i = 0; i < 0x10FFFF; ++i) {
    		
    		// skip characters we know don't have decomps
    		int type = UCharacter.getType(i);
    		if (type == Character.UNASSIGNED || type == Character.PRIVATE_USE
    			|| type == Character.SURROGATE) continue;
    			
    		if ((++counter % 5000) == 0) logln("Testing " + Utility.hex(i,0));
    		
    		String s = UTF16.valueOf(i) + "\u0345";
    		String decomp = Normalizer.decompose(s, false, 0);
    		String comp = Normalizer.compose(s, false, 0);
    		// skip characters that don't have either decomp.
    		// need quick test for this!
    		if (s.equals(decomp) && s.equals(comp)) continue;
    		
    		it.setSource(s);
    		boolean gotDecomp = false;
    		boolean gotComp = false;
    		boolean gotSource = false;
    		while (true) {
    			String item = it.next();
    			if (item == null) break;
    			if (!item.equals(s)) gotSource = true;
    			if (!item.equals(decomp)) gotDecomp = true;
    			if (!item.equals(comp)) gotComp = true;
    		}
    		if (!gotSource || !gotDecomp || !gotComp) {
    			errln("FAIL CanonicalIterator: " + s);
    		}
    	}
    }

    public void Test() {
        // set up for readable display
        Transliterator name = Transliterator.getInstance("name");
        Transliterator hex = Transliterator.getInstance("hex");
        
        // check build
        UnicodeSet ss = CanonicalIterator.getSafeStart();
        logln("Safe Start: " + ss.toPattern(true));
        logln("");
        ss = CanonicalIterator.getStarts('a');
        logln("Characters with 'a' at the start of their decomposition: " + ss.toPattern(true));
        
        // check permute
        logln(collectionToString(CanonicalIterator.permute("ABC")));
        
        // try samples
        for (int i = 0; i < testArray.length; ++i) {
            logln("");
            logln("Results for: " + name.transliterate(testArray[i]));
            CanonicalIterator it = new CanonicalIterator(testArray[i]);
            int counter = 0;
            while (true) {
                String result = it.next();
                if (result == null) break;
                logln(++counter + ": " + hex.transliterate(result));
                logln(" = " + name.transliterate(result));
            }
        }
    }
    
    static String collectionToString(Collection col) {
        StringBuffer result = new StringBuffer();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            if (result.length() != 0) result.append(", ");
            result.append(it.next().toString());
        }
        return result.toString();
    }
}