/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/normalizer/TestCanonicalIterator.java,v $ 
 * $Date: 2002/03/14 22:43:03 $ 
 * $Revision: 1.6 $
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
	
    static final boolean SHOW_NAMES = false;

    public static void main(String[] args) throws Exception {
        new TestCanonicalIterator().run(args);
    }
	
    static final String testArray[][] = {
        {"Åd\u0307\u0327", "A\u030Ad\u0307\u0327, A\u030Ad\u0327\u0307, A\u030A\u1E0B\u0327, "
        	+ "A\u030A\u1E11\u0307, \u00C5d\u0307\u0327, \u00C5d\u0327\u0307, "
        	+ "\u00C5\u1E0B\u0327, \u00C5\u1E11\u0307, \u212Bd\u0307\u0327, "
        	+ "\u212Bd\u0327\u0307, \u212B\u1E0B\u0327, \u212B\u1E11\u0307"},
        {"\u010d\u017E", "c\u030Cz\u030C, c\u030C\u017E, \u010Dz\u030C, \u010D\u017E"},
        {"x\u0307\u0327", "x\u0307\u0327, x\u0327\u0307, \u1E8B\u0327"},
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
    
    public void TestBasic() {
        // check build
        UnicodeSet ss = CanonicalIterator.getSafeStart();
        logln("Safe Start: " + ss.toPattern(true));
        ss = CanonicalIterator.getStarts('a');
        expectEqual("Characters with 'a' at the start of their decomposition: ", "", CanonicalIterator.getStarts('a'),
        	new UnicodeSet("[\u00E0-\u00E5\u0101\u0103\u0105\u01CE\u01DF\u01E1\u01FB"
        	+ "\u0201\u0203\u0227\u1E01\u1EA1\u1EA3\u1EA5\u1EA7\u1EA9\u1EAB\u1EAD\u1EAF\u1EB1\u1EB3\u1EB5\u1EB7]")
        		);
        
        // check permute
        // NOTE: we use a TreeSet below to sort the output, which is not guaranteed to be sorted!
        
        expectEqual("Simple permutation ", "", collectionToString(new TreeSet(CanonicalIterator.permute("ABC"))), "ABC, ACB, BAC, BCA, CAB, CBA");
        
        // try samples
        SortedSet set = new TreeSet();
        for (int i = 0; i < testArray.length; ++i) {
            //logln("Results for: " + name.transliterate(testArray[i]));
            CanonicalIterator it = new CanonicalIterator(testArray[i][0]);
            int counter = 0;
            set.clear();
            while (true) {
                String result = it.next();
                if (result == null) break;
                set.add(result); // sort them
                //logln(++counter + ": " + hex.transliterate(result));
                //logln(" = " + name.transliterate(result));
            }
            expectEqual(i + ": ", testArray[i][0], collectionToString(set), testArray[i][1]);

        }
    }
    
    public void expectEqual(String message, String item, Object a, Object b) {
    	if (!a.equals(b)) {
    		errln("FAIL: " + message + getReadable(item));
    		errln("\t" + getReadable(a));
    		errln("\t" + getReadable(b));
    	} else {
    		logln("Checked: " + message + getReadable(item));
    		logln("\t" + getReadable(a));
    		logln("\t" + getReadable(b));
    	}
    }
    
    Transliterator name = null;
    Transliterator hex = null;
        
    public String getReadable(Object obj) {
    	if (obj == null) return "null";
    	String s = obj.toString();
    	if (s.length() == 0) return "";
        // set up for readable display
        if (name == null) name = Transliterator.getInstance("[^\\ -\\u007F] name");
        if (hex == null) hex = Transliterator.getInstance("[^\\ -\\u007F] hex");
        return "[" + (SHOW_NAMES ? name.transliterate(s) + "; " : "") + hex.transliterate(s) + "]";
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