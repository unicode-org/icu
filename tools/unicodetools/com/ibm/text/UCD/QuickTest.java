/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/QuickTest.java,v $
* $Date: 2002/10/05 01:28:58 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

import com.ibm.text.utility.*;

public class QuickTest implements UCD_Types {
    static final void test() {
        Default.setUCD();
/*
         [4]    NameStartChar := ":" | [A-Z] | "_" | [a-z] |
		 		 [#xC0 - #x2FF] | [#x370 - #x37D] | [#x37F - #x1FFF] |
		 		 [#x200C - #x200D] | [#x2070 - #x218F] | [#x2C00 - #x2FEF] | 
		 		 [#x3001 - #xD7FF] | [#xF900 - #xF9FF] | [#x10000 - #xDFFFF]

 [4a]    NameChar := NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F]
*/
        UnicodeSet nameStartChar = new UnicodeSet("[\\: A-Z \\_ a-z"
            + "\\u00c0-\\u02FF \\u0370-\\u037D \\u037F-\\u1FFF"
            + "\\u200C-\\u200D \\u2070-\\u218F \\u2C00-\\u2FEF"
		 	+ "\\u3001-\\uD7FF \\uF900-\\uF9FF \\U00010000-\\U000DFFFF]");
		 	
        UnicodeSet nameChar = new UnicodeSet("[\\- \\. 0-9 \\u00B7 \\u0300-\\u036F]")
            .addAll(nameStartChar);
            
		showSet("NameStartChar", nameStartChar);
		showDiffs("NameChar", nameChar, "NameStartChar", nameStartChar);
		

        UnicodeSet defaultIgnorable = UnifiedBinaryProperty.make(DERIVED | DefaultIgnorable).getSet();
        UnicodeSet whitespace = UnifiedBinaryProperty.make(BINARY_PROPERTIES | White_space).getSet();
        
        UnicodeSet notNFKC = new UnicodeSet();
        UnicodeSet privateUse = new UnicodeSet();
        UnicodeSet noncharacter = new UnicodeSet();
        UnicodeSet format = new UnicodeSet("[:Cf:]");
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (!Default.nfkc.isNormalized(i)) notNFKC.add(i);
            if (Default.ucd.isNoncharacter(i)) noncharacter.add(i);
            if (Default.ucd.getCategory(i) == PRIVATE_USE) privateUse.add(i);
        }
        
		showSet("notNFKC in NameChar", new UnicodeSet(notNFKC).retainAll(nameChar));
		showSet("notNFKC outside of NameChar", new UnicodeSet(notNFKC).removeAll(nameChar));
		
		showSet("Whitespace in NameChar", new UnicodeSet(nameChar).retainAll(whitespace));
		showSet("Whitespace not in NameChar", new UnicodeSet(whitespace).removeAll(nameChar));
		

		showSet("Noncharacters in NameChar", new UnicodeSet(noncharacter).retainAll(noncharacter));
		showSet("Noncharacters outside of NameChar", new UnicodeSet(noncharacter).removeAll(nameChar));

		showSet("Format in NameChar", new UnicodeSet(nameChar).retainAll(format));
		showSet("Other Default_Ignorables in NameChar", new UnicodeSet(defaultIgnorable).removeAll(format).retainAll(nameChar));
		showSet("PrivateUse in NameChar", new UnicodeSet(defaultIgnorable).retainAll(privateUse));

        UnicodeSet CID_Start = new UnicodeSet("[:ID_Start:]").removeAll(notNFKC);
        UnicodeSet CID_Continue = new UnicodeSet("[:ID_Continue:]")
            .removeAll(notNFKC).removeAll(format);
        
        UnicodeSet CID_Continue_extras = new UnicodeSet(CID_Continue).removeAll(CID_Start);
        
        showDiffs("NoK_ID_Start", CID_Start, "NameStartChar", nameStartChar);
        showDiffs("NoK_ID_Continue_Extras", CID_Continue_extras, "NameChar", nameChar);
        
        System.out.println("Removing canonical singletons");
    }
    
    static void showDiffs(String title1, UnicodeSet set1, String title2, UnicodeSet set2) {
        showSet(title1 + " - " + title2, new UnicodeSet(set1).removeAll(set2));
    }
    
    static void showSet(String title1, UnicodeSet set1) {
        System.out.println();
        System.out.println(title1);
        if (set1.size() == 0) {
            System.out.println("\tNONE");
            return;
        }
        System.out.println("\tCount:" + set1.size());
        System.out.println("\tSet:" + set1.toPattern(true));
        System.out.println("\tDetails:");
        Utility.showSetNames("", set1, false, Default.ucd);
    }
}