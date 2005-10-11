/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/QuickTest.java,v $
* $Date: 2005/10/11 19:39:15 $
* $Revision: 1.7 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodePropertySource;
import com.ibm.icu.dev.test.util.UnicodeMap.MapIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import com.ibm.text.utility.*;

public class QuickTest implements UCD_Types {
	public static void main(String[] args) throws IOException {
		getBidiMirrored();
		if (true) return;
		getLengths("NFC", Default.nfc());
		getLengths("NFD", Default.nfd());
		getLengths("NFKC", Default.nfkc());
		getLengths("NFKD", Default.nfkd());
		System.out.println("Done");
	}
	
	
	
	private static void getBidiMirrored() {
		ToolUnicodePropertySource foo = ToolUnicodePropertySource.make("");
		UnicodeMap status = new UnicodeMap();
		status.putAll(foo.getSet("generalcategory=ps"), "*open/close*");
		status.putAll(foo.getSet("generalcategory=pe"), "*open/close*");
		status.putAll(foo.getSet("generalcategory=pi"), "*open/close*");
		status.putAll(foo.getSet("generalcategory=pf"), "*open/close*");
		
		UnicodeSet bidiMirroredSet = foo.getSet("bidimirrored=true");
		status.putAll(bidiMirroredSet, "*core*");
		UnicodeSet bidiMirroringSet = new UnicodeSet();
		UnicodeProperty x = foo.getProperty("bidimirroringglyph");
		for (int i = 0; i < 0x10FFFF; ++i) {
			String s = x.getValue(i);
			if (!s.equals(UTF16.valueOf(i))) bidiMirroringSet.add(i);
		}
		status.putAll(new UnicodeSet(bidiMirroredSet).removeAll(bidiMirroringSet), "no bidi mirroring");
		UnicodeSet mathSet = foo.getSet("generalcategory=sm");
		status.putAll(mathSet, "math");
		
		UnicodeSet special = new UnicodeSet("[<>]");
		for (UnicodeSetIterator it = new UnicodeSetIterator(mathSet); it.next();) {
			String s = Default.nfkd().normalize(it.codepoint);
			if (special.containsSome(s)) status.put(it.codepoint, "*special*");
		}
		//showStatus(status);
		// close under nfd
		for (int i = 0; i < 0x10FFFF; ++i) {
			if (!Default.ucd().isAssigned(i)) continue;
			if (!Default.ucd().isPUA(i)) continue;
			if (Default.nfkc().isNormalized(i)) continue;
			String oldValue = (String) status.getValue(i);
			if (oldValue != null) continue;
			String s = Default.nfkc().normalize(i);
			if (UTF16.countCodePoint(s) != 1) continue;
			int cp = UTF16.charAt(s, 0);
			String value = (String)status.getValue(cp);
			if (value != null) status.put(i, "nfc-closure-" + value);
		}
		showStatus(status, bidiMirroredSet);
	}

	static BagFormatter bf = new BagFormatter();
	private static void showStatus(UnicodeMap status, UnicodeSet x) {
		Collection list = new TreeSet(status.getAvailableValues());
		for (Iterator it = list.iterator(); it.hasNext(); ) {
			String value = (String) it.next();
			if (value == null) continue;
			UnicodeSet set = status.getSet(value);
			for (UnicodeSetIterator umi = new UnicodeSetIterator(set); umi.next();) {
				System.out.println(Utility.hex(umi.codepoint) 
						+ ";\t" + value
						+ ";\t" + (x.contains(umi.codepoint) ? "O" : "")
						+ ";\t" + Default.ucd().getName(umi.codepoint));
			}
		}
	}


	public static class Length {
		String title;
		int bytesPerCodeUnit;
		int longestCodePoint = -1;
		int longestLength = 0;
		UnicodeSet longestSet = new UnicodeSet();
		Length(String title, int bytesPerCodeUnit) {
			this.title = title;
			this.bytesPerCodeUnit = bytesPerCodeUnit;
		}
		void add(int codePoint, int codeUnitLength) {
			if (codeUnitLength > longestLength) {
				longestCodePoint = codePoint;
				longestLength = codeUnitLength;
				longestSet.clear();
				longestSet.add(codePoint);
				System.out.println(title + " \t(" + codeUnitLength*bytesPerCodeUnit + " bytes, "
						+ codeUnitLength + " code units) \t"
						+ Default.ucd().getCodeAndName(codePoint));				
			} else if (codeUnitLength == longestLength) {
				longestSet.add(codePoint);
			}
		}
	}
	
	static final int skip = (1<<UCD.UNASSIGNED) | (1<<UCD.PRIVATE_USE) | (1<<UCD.SURROGATE);
	/**
	 * 
	 */
	private static void getLengths(String title, Normalizer normalizer) throws IOException {
		System.out.println();
		Length utf8Len = new Length(title + "\tUTF8", 1);
		Length utf16Len = new Length(title + "\tUTF16", 1);
		Length utf32Len = new Length(title + "\tUTF32", 1);
		for (int i = 0; i <= 0x10FFFF; ++i) {
			int type = Default.ucd().getCategoryMask(i);
			if ((type & skip) != 0) continue;
			String norm = normalizer.normalize(i);
			utf8Len.add(i, getUTF8Length(norm));
			utf16Len.add(i, norm.length());
			utf32Len.add(i, UTF16.countCodePoint(norm));
		}
		UnicodeSet common = new UnicodeSet(utf8Len.longestSet)
			.retainAll(utf16Len.longestSet)
			.retainAll(utf32Len.longestSet);
		if (common.size() > 0) {
			UnicodeSetIterator it = new UnicodeSetIterator(common);
			it.next();
			System.out.println("Common Exemplar: " + Default.ucd().getCodeAndName(it.codepoint));
		}
	}

	static ByteArrayOutputStream utf8baos;
	static Writer utf8bw;
	static int getUTF8Length(String source) throws IOException {
		if (utf8bw == null) {
			utf8baos = new ByteArrayOutputStream();
			utf8bw = new OutputStreamWriter(utf8baos, "UTF-8");
		}
		utf8baos.reset();
		utf8bw.write(source);
		utf8bw.flush();
		return utf8baos.size();
	}
	static final void test() {
		String test2 = "ab\u263ac";
		StringTokenizer st = new StringTokenizer(test2, "\u263a");
		try {
			while (true) {
				String s = st.nextToken();
				System.out.println(s);
			}
		} catch (Exception e) {		}
		StringReader r = new StringReader(test2);
		StreamTokenizer s = new StreamTokenizer(r);
		try {
			while (true) {
				int x = s.nextToken();
				if (x == StreamTokenizer.TT_EOF) break;
				System.out.println(s.sval);
			}
		} catch (Exception e) {		}
		
		String testString = "en-Arab-200-gaulish-a-abcd-def-x-abcd1234-12345678";
		for (int i = testString.length() + 1; i > 0; --i) {
			String trunc = truncateValidLanguageTag(testString, i);
			System.out.println(i + "\t" + trunc + "\t" + trunc.length());
		}
	}
	
	static String truncateValidLanguageTag(String tag, int limit) {
		if (tag.length() <= limit) return tag;
		// legit truncation point has - after, and two letters before
		do { 
			if (tag.charAt(limit) == '-' && tag.charAt(limit-1) != '-' && tag.charAt(limit-2) != '-') break;
		} while (--limit > 2);
		return tag.substring(0,limit);
	}
	
    static final void test2() {
        
        UnicodeSet format = new UnicodeSet("[:Cf:]");
/*
 [4]     NameStartChar := ":" | [A-Z] | "_" | [a-z] |
            [#xC0-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] |
            [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] |
            [#x3001-#xD7FF] | [#xF900-#xEFFFF]
 [4a]    NameChar := NameStartChar | "-" | "." | [0-9] | #xB7 |
            [#x0300-#x036F] | [#x203F-#x2040]
*/
        UnicodeSet nameStartChar = new UnicodeSet("[\\: A-Z \\_ a-z"
            + "\\u00c0-\\u02FF \\u0370-\\u037D \\u037F-\\u1FFF"
            + "\\u200C-\\u200D \\u2070-\\u218F \\u2C00-\\u2FEF"
		 	+ "\\u3001-\\uD7FF \\uF900-\\U000EFFFF]");
		 	
        UnicodeSet nameChar = new UnicodeSet("[\\- \\. 0-9 \\u00B7 "
            + "\\u0300-\\u036F \\u203F-\\u2040]")
            .addAll(nameStartChar);
            
        UnicodeSet nameAll = new UnicodeSet(nameChar).addAll(nameStartChar);
            
		showSet("NameStartChar", nameStartChar);
		showDiffs("NameChar", nameChar, "NameStartChar", nameStartChar);
		
		
        UnicodeSet ID_Start = new UnicodeSet("[:ID_Start:]");
        UnicodeSet ID_Continue = new UnicodeSet("[:ID_Continue:]").removeAll(format);	
        
        UnicodeSet ID_All = new UnicodeSet(ID_Start).addAll(ID_Continue);
        
		showDiffs("ID_All", ID_All, "nameAll", nameAll);
		showDiffs("ID_Start", ID_Start, "nameStartChar", nameStartChar);
		

        UnicodeSet defaultIgnorable = UnifiedBinaryProperty.make(DERIVED | DefaultIgnorable).getSet();
        UnicodeSet whitespace = UnifiedBinaryProperty.make(BINARY_PROPERTIES | White_space).getSet();
        
        UnicodeSet notNFKC = new UnicodeSet();
        UnicodeSet privateUse = new UnicodeSet();
        UnicodeSet noncharacter = new UnicodeSet();
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!Default.ucd().isAllocated(i)) continue;
            if (!Default.nfkc().isNormalized(i)) notNFKC.add(i);
            if (Default.ucd().isNoncharacter(i)) noncharacter.add(i);
            if (Default.ucd().getCategory(i) == PRIVATE_USE) privateUse.add(i);
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
        Utility.showSetNames("", set1, false, Default.ucd());
    }
}