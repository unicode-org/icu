/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/TestData.java,v $
* $Date: 2006/04/05 22:12:43 $
* $Revision: 1.25 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UCharArrayIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.StringPrep;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

import java.math.BigDecimal;

import java.util.regex.*;

import com.ibm.icu.text.*;
import com.ibm.text.utility.*;

public class TestData implements UCD_Types {
    
    static UnicodeProperty.Factory upf;
    
	public static void main (String[] args) throws IOException {
		tryConsole2();
		if (true) return;
		
		showNonCompatFull(false);
		showNonCompatFull(true);
		

		checkForCaseStability(false);
		//countChars();
		foo();
       
        System.out.println("main: " + Default.getDate());
        upf = ICUPropertyFactory.make();
        System.out.println("after factory: " + Default.getDate());
 
        showPropDiff(
            "gc=mn", null,
            "script=inherited", null);
            
        // upf.getProperty("gc")
        //.getPropertySet(new ICUPropertyFactory.RegexMatcher("mn|me"),null)
        
        showPropDiff(
        "gc=mn|me", null,
        "script=inherited", null);

        if (true) return;

        showPropDiff(
            "General_Category=L", null,
            "Script!=Inherited|Common", 
                upf.getSet("script=inherited")
                .addAll(UnifiedBinaryProperty.getSet("script=common", Default.ucd()))
                .complement()
        );
       
        
        UnicodeSet sterm = UnifiedProperty.getSet("STerm", Default.ucd());
        UnicodeSet term = UnifiedProperty.getSet("Terminal_Punctuation", Default.ucd());
        UnicodeSet po = new UnicodeSet("[:po:]");
        UnicodeSet empty = new UnicodeSet();
        
        Utility.showSetDifferences(
            "Sentence_Terminal", sterm, 
            "Empty", empty, 
            true, Default.ucd());

        Utility.showSetDifferences(
            "Sentence_Terminal", sterm, 
            "Terminal_Punctuation", term, 
            true, Default.ucd());

        Utility.showSetDifferences(
            "Terminal_Punctuation", term, 
            "Punctuation_Other", po, 
            true, Default.ucd());
        
        if (true) return;
        
        UnicodeSet us = getSetForName("LATIN LETTER.*P");
        Utility.showSetNames("",us,false,Default.ucd());
        
        us = getSetForName(".*VARIA(TION|NT).*");
        Utility.showSetNames("",us,false,Default.ucd());
       
        if (true) return;
        
        /*showSet();
        */
        String x = "[[[:s:][:p:]&[:ascii:]] | [\\u2190-\\u2BFF] | "
        + "[[:s:][:p:]"
   //     + "&[:decompositiontype=none:]"
   // + "- [:id_continue:]"
        + "-[:sk:]"
        + "]]";
        PrintWriter pw = Utility.openPrintWriter("Syntax.txt", Utility.UTF8_WINDOWS);
        showSet(pw, x, false);
        showSet(pw, "[[\\u2000-\\u205F]-" + x + "]", true);
        showSet(pw, "[[:whitespace:]&[:decompositiontype=none:]]", false);
        pw.close();
        
        if (true) return;
		
		testFormatHack();
		if (true) return;
		testConvertToBDD();
		if (true) return;
		
		System.out.println("Shift: " + SHIFT + ", Mask: " + Long.toHexString(MASK));
		showNumber(-5);
		showNumber(0);
		showNumber(5);
		showNumber(500);
		showNumber(5000000);
		if (true) return;
		
		String script = args[0];
		PrintWriter log = Utility.openPrintWriter("TranslitSkeleton_" + script + ".txt", Utility.UTF8_WINDOWS);
		try {
			UnicodeSet base = new UnicodeSet("[:" + script + ":]");
			UnicodeSetIterator it = new UnicodeSetIterator(base);
			while (it.next()) {
				String s = UTF16.valueOf(it.codepoint);
				String norm = Default.nfd().normalize(s);
				if (s.equals(norm) && Default.nfkd().isNormalized(norm)) {
					log.println("# " + s + " <> XXX # " + Default.ucd().getName(it.codepoint));
				}
			}
		} finally {
			log.close();
				}
	}
	
	private static void showNonCompatFull(boolean compat) {
		UCD ucd = UCD.make("4.1.0");
		Normalizer nfkc = new Normalizer(Normalizer.NFKC, ucd.getVersion());
		System.out.println();
		System.out.println(compat ? "Full Fold = Simple Lower of NFKC" : "Full Fold != Simple Lower of NFKC");
		System.out.println();
		int count = 0;
		for (int i = 0; i <= 0x10FFFF; ++i) {
			int gc = ucd.getCategory(i);
			if (gc == Cn || gc == PRIVATE_USE) continue;
			//if (compat == (ucd.getDecompositionType(i) > UCD.CANONICAL)) continue;
			String str = UTF16.valueOf(i);
			String simpleLower = ucd.getCase(str, SIMPLE, LOWER);
			String fullFold = ucd.getCase(str, FULL, FOLD);
			
			if (!simpleLower.equals(fullFold)) {
				String nfkcStr = nfkc.normalize(str);
				String simpleLowerNfkc = ucd.getCase(nfkcStr, SIMPLE, LOWER);
				if (compat != (fullFold.equals(simpleLowerNfkc))) continue;
				System.out.println(ucd.getCodeAndName(i));
				System.out.println("\tSimple Lower:\t" + ucd.getCodeAndName(simpleLower));
				System.out.println("\tFull Fold:\t" + ucd.getCodeAndName(fullFold));
				count++;
			}
		}
		System.out.println("Count:\t" + count);
	}

	private static void tryConsole() throws UnsupportedEncodingException {
		for (int i = 1; i < 0xFFFF; ++i) {
			String s = UTF32.valueOf32(i);
			byte[] bytes = s.getBytes("UTF-8");
			String utf8bytes = "";
			for (int j = 0; j < bytes.length; ++j) {
				if (j != 0) utf8bytes += " ";
				utf8bytes += Utility.hex(bytes[j]&0xFF,2);
			}
			String name = UCharacter.getExtendedName(i);
			System.out.println(Utility.hex(i) + "\t(" + s + ")\t[" + utf8bytes + "]\t" + name);
		}
	}
	
	private static void tryConsole2() throws UnsupportedEncodingException {
		UnicodeSet failures = new UnicodeSet();
		check:
		for (int i = 1; i <= 0x10FFFF; ++i) {
			String s = UTF32.valueOf32(i);
			byte[] bytes = s.getBytes("UTF-8");
			for (int j = 0; j < bytes.length; ++j) {
				switch (bytes[j]&0xFF) {
				case 0x81: case 0x8D: case 0x8F: case 0x90: case 0x9D:
					failures.add(i);
					continue check;
				}
			}
		}
		System.out.println("Total corrupted characters: " + failures.size());
		System.out.println("Percent corrupted characters: " + ((failures.size() + 0.0) / 0x110000 * 100.0 + "%"));
		//BagFormatter bf = new BagFormatter();
		//System.out.println(bf.showSetNames(failures));
	}


	private static void countChars() {
		int[][] count = new int[AGE_VERSIONS.length][50];
		for (int j = 1; j < AGE_VERSIONS.length; ++j) {
			UCD ucd = UCD.make(AGE_VERSIONS[j]);
			UCDProperty alpha = DerivedProperty.make(ucd.PropAlphabetic, ucd);

			int alphaCount = 0;
			for (int i = 0; i <=0x10FFFF; ++i) {
				int type = ucd.getCategory(i);
				if (ucd.isNoncharacter(i)) type = LIMIT_CATEGORY;
				++count[j][type];
				if (alpha.hasValue(i) || type == ucd.Nd) ++count[j][LIMIT_CATEGORY+1];
			}
		}

		for (byte i = -1; i < LIMIT_CATEGORY+2; ++i) {
			switch(i) {
			case -1: System.out.print("\t\t"); break;
			default: System.out.print(UCD.getCategoryID_fromIndex(i,UCD.LONG) + "\t" + UCD.getCategoryID_fromIndex(i)); break;
			case LIMIT_CATEGORY: System.out.print("Noncharacter" + "\t" + "NCCP"); break;
			case LIMIT_CATEGORY+1: System.out.print("Alphabetic" + "\t" + "alpha"); break;
			}
			for (int j = 1; j < AGE_VERSIONS.length; ++j) {
				if (i < 0) System.out.print("\t*" + AGE_VERSIONS[j] + "*");
				else System.out.print("\t" + count[j][i]);
			}
			System.out.println();
		}

	}

	private static void foo() {
		String[] test = {
				"vicepresident",
				"vice president",
				"vice-president",
				"vice-président",
				"vice-president's offices",
				"vice-presidents' offices",
				"vice-presidents offices",
				"vice-presidentsoffices",
		};
		RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(new ULocale("fr"));
		col.setStrength(col.QUATERNARY);
		col.setAlternateHandlingShifted(false);

		Arrays.sort(test, col);
		List s = Arrays.asList(test);
		String last = "";
		int[] level = new int[1];
		for (Iterator it = s.iterator(); it.hasNext();) {
			String current = (String) it.next();
			int order = levelCompare(col, last, current, level);
			//System.out.print(levelStrings[level[0]]);
			//System.out.print(order < 0 ? "<" : order == 0 ? "=" : ">");
			System.out.println("\t" + current);
			last = current;
		}
		for (int i = 0; i < test.length; ++i) {
			System.out.print(test[i] + ";");
		}
		System.out.println();
	}
	
	static String[] levelStrings = {".", "..", "...", "....", "....."};
	
	static int levelCompare(RuleBasedCollator col, String a, String b, int[] level) {
		int diff = 0;
		level[0] = 0;
		for (int i = 0; i < 15; ++i) {
			col.setStrength(i);
			diff = col.compare(a, b);
			if (diff != 0) {
				level[0] = i;
				break;
			}
		}
		return diff;
	}

	Matcher m;
	
	/**
	 * @param inbuffer
	 * @param outbuffer
	 * @return
	 */
	public static boolean equals(StringBuffer inbuffer, StringBuffer outbuffer) {
		if (inbuffer.length() != outbuffer.length()) return false;
		for (int i = inbuffer.length() - 1; i >= 0; --i) {
			if (inbuffer.charAt(i) != outbuffer.charAt(i)) return false;
		}
		return true;
	}

	private static void checkForCaseStability(boolean mergeRanges) {
		UCD ucd = Default.ucd();
		ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("");
		UnicodeSet propUppercase = ups.getSet("Uppercase=true");
		UnicodeSet propLowercase = ups.getSet("Lowercase=true");
		UnicodeSet isGcLt = ups.getSet("gc=Lt");
		UnicodeSet otherAlphabetic = ups.getSet("Alphabetic=true").addAll(ups.getSet("gc=Sk"));
		// create the following
		UnicodeSet hasFold = new UnicodeSet();
		UnicodeSet hasUpper = new UnicodeSet();
		UnicodeSet hasLower = new UnicodeSet();
		UnicodeSet hasTitle = new UnicodeSet();
		UnicodeSet compat = new UnicodeSet();
		UnicodeSet bicameralsScripts = new UnicodeSet();
		
		UnicodeSet isFUppercase = new UnicodeSet();
		UnicodeSet isFLowercase = new UnicodeSet();
		UnicodeSet isFTitlecase = new UnicodeSet();

		UCD u40 = UCD.make("4.0.0");
		BitSet scripts = new BitSet();
		for (int i = 0; i <= 0x10FFFF; ++i) {
			int gc = ucd.getCategory(i);
			if (gc == Cn || gc == PRIVATE_USE) continue;
			String str = UTF16.valueOf(i);
			if (!str.equals(ucd.getCase(str, FULL, FOLD))) {
				hasFold.add(i);
				scripts.set(ucd.getScript(i));
			}
			if (!str.equals(ucd.getCase(str, FULL, UPPER))) {
				hasUpper.add(i);
				scripts.set(ucd.getScript(i));
			} else {
				isFUppercase.add(i);
			}
			if (!str.equals(ucd.getCase(str, FULL, LOWER))) {
				hasLower.add(i);
				scripts.set(ucd.getScript(i));
			} else {
				isFLowercase.add(i);
			}
			if (!str.equals(ucd.getCase(str, FULL, TITLE))) {
				hasTitle.add(i);
				scripts.set(ucd.getScript(i));
			} else {
				isFTitlecase.add(i);
			}
			if (!str.equals(Default.nfkd().normalize(str))) compat.add(i);
			//System.out.println(ucd.getCodeAndName(i) + "\t" + (u40.isAllocated(i) ? "already in 4.0" : "new in 4.1"));
		}
		BagFormatter bf = new BagFormatter();
		Transliterator nullTrans = Transliterator.getInstance("null");
		bf.setShowLiteral(nullTrans);
		bf.setMergeRanges(mergeRanges);
		bf.setUnicodePropertyFactory(ups);
		
		UnicodeSet allCased = new UnicodeSet().addAll(hasUpper).addAll(hasLower).addAll(hasTitle);
		isFUppercase.retainAll(allCased);
		isFLowercase.retainAll(allCased);
		isFTitlecase.retainAll(allCased);
		System.out.println(Utility.BOM);

		printItems(bf, compat, "Uppercase=true or gc=Lt without hasLower", 
				new UnicodeSet(propUppercase).addAll(isGcLt).removeAll(hasLower));
		printItems(bf, compat, "hasLower, but not (Uppercase=true or gc=Lt)", 
				new UnicodeSet(hasLower).removeAll(isGcLt).removeAll(propUppercase));
		printItems(bf, compat, "Lowercase=true without hasUpper", 
				new UnicodeSet(propLowercase).addAll(isGcLt).removeAll(hasUpper));
		printItems(bf, compat, "hasUpper, but not (Lowercase=true or gc=Lt)", 
				new UnicodeSet(hasUpper).removeAll(isGcLt).removeAll(propLowercase));

		
		printItems(bf, compat, "Functionally Uppercase, but not Uppercase=true", 
				new UnicodeSet(isFUppercase).removeAll(propUppercase));
		printItems(bf, compat, "Uppercase=true, but not functionally Uppercase", 
				new UnicodeSet(propUppercase).removeAll(isFUppercase));
		
		printItems(bf, compat, "Functionally Lowercase, but not Lowercase=true", 
				new UnicodeSet(isFLowercase).removeAll(propLowercase));
		printItems(bf, compat, "Lowercase=true, but not functionally Lowercase", 
				new UnicodeSet(propLowercase).removeAll(isFLowercase));


		UnicodeSet scriptSet = new UnicodeSet();
		UnicodeProperty scriptProp = ups.getProperty("Script");
		bf.setMergeRanges(true);
		System.out.println();
		System.out.println("Bicameral Scripts: those with at least one functionally cased character.");
		System.out.println();
		for (int i = 0; i < scripts.size(); ++i) {
			if (!scripts.get(i)) continue;
			//if (i == COMMON_SCRIPT) continue;
			String scriptName = ucd.getScriptID_fromIndex((byte)i);
			UnicodeSet scriptUSet = scriptProp.getSet(scriptName);
			scriptSet.addAll(scriptUSet);
			printItems(bf, compat, "Bicameral Script: " + scriptName,
					new UnicodeSet(allCased).retainAll(scriptUSet));
		}
		bf.setMergeRanges(false);
		printItems(bf, compat, "Bicameral Script: isAlpha or Symbol Modifier, but not isCased", 
				new UnicodeSet(scriptSet).retainAll(otherAlphabetic).removeAll(allCased));
		printItems(bf, compat, "Bicameral Script: isCased, but not isAlpha or Symbol Modifier", 
				new UnicodeSet(scriptSet).retainAll(allCased).removeAll(otherAlphabetic));
	}

	
	/**
	 * @param bf
	 * @param compat
	 * @param temp
	 */
	private static void printItems(BagFormatter bf, UnicodeSet compat, String title, UnicodeSet temp) {
		System.out.println();
		System.out.println(title + " -- (non compat)");		
		UnicodeSet temp2 = new UnicodeSet(temp).removeAll(compat);
		System.out.println(bf.showSetNames(temp2));
		System.out.println();
		temp2 = new UnicodeSet(temp).retainAll(compat);
		System.out.println(title + " -- (compat)");
		System.out.println(bf.showSetNames(temp2));
	}

	static PrintWriter log;
	
    public static void checkShaping() throws IOException {
    	log = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR, "checklog.txt");
    	checkProperty("Joining_Type", "Non_Joining", "Joining_Type", "Transparent");
    	checkProperty("Joining_Group", "No_Joining_Group", "Joining_Type", "Transparent");  	
       	checkProperty("Line_Break", "Unknown", "Line_Break", "Combining_Mark");
       	checkProperty("East_Asian_Width", null, "Line_Break", "Combining_Mark");
       	checkProperty("Bidi_Class", null, "Line_Break", "Combining_Mark");
       	checkProperty("Script", null, "Script", new String[]{"Common", "Inherited"});
       	checkProperty("General_Category", null, "General_Category", new String[]{"Spacing_Mark", 
       			"Enclosing_Mark", "Nonspacing_Mark"});
    	log.close();
    }

    /**
	 * @param propertyName
     * @param exclusion
     * @param ignorePropertyName TODO
     * @param ignoreValue
	 */
	private static void checkProperty(String propertyName, String exclusion, String ignorePropertyName, Object ignoreValueList) {
		log.println();
		log.println(propertyName + " Check");
		log.println();
		Set ignoreValueSet = new HashSet();
		if (ignoreValueList instanceof String) ignoreValueSet.add(ignoreValueList);
		else ignoreValueSet.addAll(Arrays.asList((Object[])ignoreValueList));
		
		ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("4.0.1");
    	UnicodeProperty up = ups.getProperty(propertyName);
    	UnicodeProperty ignProp = ups.getProperty(ignorePropertyName);
    	UnicodeProperty name = ups.getProperty("Name");
    	UnicodeSet significant = (exclusion != null ? up.getSet(exclusion) : new UnicodeSet()).complement();
    	UnicodeSetIterator it = new UnicodeSetIterator(significant);
    	Normalizer n = new Normalizer(Normalizer.NFD, "4.0.1");
    	int counter = 0;
    	while (it.next()) {
    		String baseValue = up.getValue(it.codepoint);
    		String nfd = n.normalize(it.codepoint);
    		if (n.isNormalized(it.codepoint)) continue;
    		//if (nfd.equals(it.getString())) continue;
    		int cp;
    		for (int i = 0; i < nfd.length(); i += UTF16.getCharCount(cp)) {
    			cp = UTF16.charAt(nfd, i);
        		boolean shown = false;
    			String newValue = up.getValue(cp);    		
    			String possIgnValue = ignProp.getValue(cp);
				if (ignoreValueSet.contains(possIgnValue)) {
					//log.println("--- " + newValue + "\t" + Utility.hex(cp) + " " + name.getValue(cp));
					continue;
				}
				//log.println("*** " + newValue + "\t" + Utility.hex(cp) + " " + name.getValue(cp));
				
				if (!baseValue.equals(newValue)) {
    				if (!shown) log.println((++counter) + "\tCONFLICT\t" + baseValue + "\t" + Utility.hex(it.codepoint) + " " + name.getValue(it.codepoint));
					log.println("\tNFD(" + Utility.hex(it.codepoint) + ") contains:\t" + newValue + "\t" + Utility.hex(cp) + " " + name.getValue(cp));
					shown = true;
				}
    		}    		
    	}
	}

	public static class RegexMatcher implements UnicodeProperty.PatternMatcher {
        private Matcher matcher;
        
        public UnicodeProperty.PatternMatcher set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }
        public boolean matches(Object value) {
            matcher.reset((String)value);
            return matcher.matches();
        }       
    }

    static BagFormatter bf = new BagFormatter();
    static UnicodeProperty.PatternMatcher matcher = new RegexMatcher();

    private static void showPropDiff(String p1, UnicodeSet s1, String p2, UnicodeSet s2) {
        System.out.println("Property Listing");
        if (s1 == null) {
            s1 = upf.getSet(p1, matcher, null);
        }
        if (s2 == null) {
            s2 = upf.getSet(p2, matcher, null);
        } 
        bf.showSetDifferences(bf.CONSOLE,p1,s1,p2,s2);
    }
    
    static private UnicodeSet getSetForName(String regexPattern) {
        UnicodeSet result = new UnicodeSet();
        Pattern p = Pattern.compile(regexPattern);
        Matcher m = p.matcher("");
        for (int i = 0; i < 0x10FFFF; ++i) {
            Utility.dot(i);
            if (!Default.ucd().isAssigned(i)) continue;
            byte cat = Default.ucd().getCategory(i);
            if (cat == PRIVATE_USE) continue;
            m.reset(Default.ucd().getName(i));
            if (m.matches()) {
                result.add(i);
            }
        }
        return result;
    }

    private static void showSet(PrintWriter pw, String x, boolean separateLines) {
        pw.println("****************************");
        System.out.println(x);
        UnicodeSet ss = new UnicodeSet(x);
        pw.println(x);
        Utility.showSetNames(pw,"",ss,separateLines,false,Default.ucd());
        pw.println("****************************");
    }
	
	static int SHIFT = 6;
	static int MASK = (1<<6) - 1;
	static int OTHER = 0xFF & ~MASK;

	static void showNumber(float x) {
		System.out.println("Number: " + x);
		//long bits = Double.doubleToLongBits(x);
		long bits = (Float.floatToIntBits(x) + 0L) << 32;
		System.out.println("IEEE: " + Long.toBinaryString(bits));
		System.out.print("Broken: ");
		long lastShift = 64-SHIFT;
		for (long shift = 64-SHIFT; shift > 0; shift -= SHIFT) {
			long temp = bits >>> shift;
			temp &= MASK;
			if (temp != 0) lastShift = shift;
			temp |= OTHER;
			String piece = Long.toBinaryString(temp);
			System.out.print(" " + piece);
		}
		System.out.println();
		System.out.print("Bytes: 1B");
		for (long shift = 64-SHIFT; shift >= lastShift; shift -= SHIFT) {
			long temp = bits >>> shift;
			temp &= MASK;
			temp |= OTHER;
			if (shift == lastShift) {
				temp &= ~0x80;
			} 
			String piece = Long.toHexString(temp).toUpperCase();
			System.out.print(" " + piece);
		}
		System.out.println();
	}
	
	static int findFirstNonZero(String digits) {
		for (int i = 0; i < digits.length(); ++i) {
			if (digits.charAt(i) != '0') return i;
		}
		return digits.length();
	}
	
	static String remove(String s, int start, int limit) {
		return s.substring(0, start) + s.substring(limit);
	}
	
	static String hexByte(int i) {
		String result = Integer.toHexString(i).toUpperCase();
		if (result.length() == 1) result = '0' + result;
		return result;
	}
	
	// dumb implementation
	static String convertToBCD(String digits) {
		
		// fix negatives, remove leading zeros, get decimal
		
		int[] pairs = new int[120];
		boolean negative = false;
		boolean removedNegative = false;
		boolean removedDecimal = false;
		int leadZeros = 0;
		int trailZeros = 0;

		if (digits.charAt(0) == '-') {
			negative = true;
			removedNegative = true;
			digits = remove(digits, 0, 1);
		}
		while (digits.length() > 0 && digits.charAt(0) == '0') {
			digits = remove(digits, 0, 1);
			leadZeros++;
		}
		int decimalOffset = digits.indexOf('.');
		if (decimalOffset < 0) {
			decimalOffset = digits.length();
		} else {
			digits = digits = remove(digits, decimalOffset, decimalOffset+1);
			removedDecimal = true;
		}
		
		// remove trailing zeros
		while (digits.length() > 0 && digits.charAt(digits.length() - 1) == '0') {
			digits = remove(digits, digits.length() - 1, digits.length());
			trailZeros++;
		}
		
		// make the digits even (in non-fraction part)
		if (((decimalOffset) & 1) != 0) {
			digits = '0' + digits; // make even
			++decimalOffset;
			leadZeros--;
		}
		if (((digits.length()) & 1) != 0) {
			digits = digits + '0'; // make even
			trailZeros--;
		}
		
		// handle 0
		if (digits.length() == 0) {
			negative = false;
			digits = "00";
			leadZeros -= 2;
		} 
		
		// store exponent
		int exp = decimalOffset/2;
		if (!negative) exp |= 0x80;
		else exp = (~exp) & 0x7F;
		String result = hexByte(exp);
		for (int i = 0; i < digits.length(); i += 2) {
			int base100 = ((digits.charAt(i) - '0')*10 + (digits.charAt(i+1) - '0')) << 1;
			if (i < digits.length() - 2) base100 |= 0x1; // mark all but last
			if (negative) base100 = (~base100) & 0xFF;
			result += "." + hexByte(base100);
		}
		
		/**
		// add a secondary weight
		// assume we don't care about more than too many leads/trails
		leadZeros += 2; // make non-negative; might have padded by 2, for 0
		trailZeros += 2; // make non-negative; might have padded by 1
		if (leadZeros > 7) leadZeros = 7;
		if (trailZeros > 7) trailZeros = 7;
		int secondary = (removedNegative ? 0 : 0x80) // only for zero
						| (leadZeros << 4)
						| (removedDecimal ? 0 : 0x08)
						| (trailZeros);
		result += ";" + hexByte(secondary);
		*/
		
		return result;
	}
	
	static int stamp = 0;
	static void add(Map m, String s) {
		add2(m, s);
		add2(m, "0" + s);
		if (s.indexOf('.') >= 0) {
			add2(m, s + "0");
			add2(m, "0" + s + "0");
		} else {
			add2(m, s + ".");
			add2(m, "0" + s + ".");
			add2(m, s + ".0");
			add2(m, "0" + s + ".0");
		}
	}
	
	static void add2(Map m, String s) {
		add3(m,s);
		if (s.indexOf('-') < 0) add3(m, "-" + s);
	}

	private static void add3(Map m, String s) {
		String base = convertToBCD(s);
		base += "|" + Math.random() + stamp++; // just something for uniqueness
		m.put(base, s);
	}
	
	static boolean SHOW_ALL = true;
	
	static NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		nf.setGroupingUsed(false);
	}
	
	static String cleanToString(double d) {
		return nf.format(d);
	}
	
	static void testConvertToBDD() {
		System.out.println("Starting Test");
		double[] testList = {0, 0.00000001, 0.001, 5, 10, 50, 100, 1000, 100000000};
		Map m = new TreeMap();
		
		for (int i = 0; i < testList.length; ++i) {
			double d = testList[i];
			add(m, cleanToString(d));
			add(m, cleanToString(d + 0.1));
			add(m, cleanToString(d + 1));
			add(m, cleanToString(d + 1.1));
			if (d > 0.1) add(m, cleanToString(d - 0.1));
			if (d > 1.0) add(m, cleanToString(d - 1.0));
			if (d > 1.1) add(m, cleanToString(d - 1.1));
		}
		Iterator it = m.keySet().iterator();
		String lastKey = "";
		String lastValue = "";
		boolean lastPrinted = false;
		double lastNumber = Double.NEGATIVE_INFINITY;
		int errorCount = 0;
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) m.get(key);
			key = key.substring(0, key.indexOf('|')); // remove stamp
			double number = Double.parseDouble(value);
			if (lastNumber > number) {
				if (!lastPrinted) System.out.println("\t" + lastValue + "\t" + lastKey);
				System.out.println("Fail:\t" + value + "\t" + key);
				lastPrinted = true;
				errorCount++;
			} else if (SHOW_ALL) {
				System.out.println("\t" + value + "\t" + key);
				lastPrinted = true;
			}
			lastNumber = number;
			lastKey = key;
			lastValue = value;
		}
		System.out.println("Done Test, " + errorCount + " Errors");
	}
	
	static void testFormatHack() {
		String[] testCurrencies = {"USD","GBP","JPY","EUR"};
		Locale[] testLocales = NumberFormat.getAvailableLocales();
		for (int i = 0; i < testLocales.length; ++i) {
			// since none of this should vary by country, we'll just do by language
			if (!testLocales[i].getCountry().equals("")) continue;
			System.out.println(testLocales[i].getDisplayName());
			for (int j = 0; j < testCurrencies.length; ++j) {
				NumberFormat nf = getCurrencyFormat(
					Currency.getInstance(testCurrencies[j]), testLocales[i], true);
				String newVersion = nf.format(1234.567);
				System.out.print("\t" + newVersion);
				nf = getCurrencyFormat(
					Currency.getInstance(testCurrencies[j]), testLocales[i], false);
				String oldVersion = nf.format(1234.567);
				if (!oldVersion.equals(newVersion)) {
					System.out.print(" (" + oldVersion + ")");
				}
			}
			System.out.println();
		}
	}
	
	static NumberFormat getCurrencyFormat(Currency currency, Locale displayLocale, boolean ICU26) {
		// code for ICU 2.6
		if (ICU26) {
			NumberFormat result = NumberFormat.getCurrencyInstance();
			result.setCurrency(currency);
			return result;
		}

		// ugly work-around for 2.4
		DecimalFormat result = (DecimalFormat)NumberFormat.getCurrencyInstance(displayLocale);
		HackCurrencyInfo hack = (HackCurrencyInfo)(hackData.get(currency.getCurrencyCode()));
		result.setMinimumFractionDigits(hack.decimals);
		result.setMaximumFractionDigits(hack.decimals);
		result.setRoundingIncrement(hack.rounding);
		DecimalFormatSymbols symbols = result.getDecimalFormatSymbols();
		symbols.setCurrencySymbol(hack.symbol);
		result.setDecimalFormatSymbols(symbols);
		return result;
	}
	
	static Map hackData = new HashMap();
	static class HackCurrencyInfo {
		int decimals;
		double rounding;
		String symbol;
		HackCurrencyInfo(int decimals, double rounding, String symbol) {
			this.decimals = decimals;
			this.rounding = rounding;
			this.symbol = symbol;
		}
	}
	static {
		hackData.put("USD", new HackCurrencyInfo(2, 0.01, "$"));
		hackData.put("GBP", new HackCurrencyInfo(2, 0.01, "\u00a3"));
		hackData.put("JPY", new HackCurrencyInfo(0, 1, "\u00a5"));
		hackData.put("EUR", new HackCurrencyInfo(2, 0.01, "\u20AC"));
	}
    /*

        System.out.println("START");
        ucd = UCD.make();
        System.out.println("Loaded UCD " + ucd.getVersion() + " " + (new Date(ucd.getDate())));

        checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
        checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");

        long mask = 0;

        if (false) {

        generateVerticalSlice(BIDI_CLASS, BIDI_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedBidiClass-3.1.1d1.txt");


        mask = Utility.setBits(0, DerivedProperty.FC_NFKC_Closure, DerivedProperty.ExpandsOnNFKC);
        mask = Utility.clearBit(mask, DerivedProperty.FullCompInclusion);
        generateDerived(mask, HEADER_DERIVED, "DerivedNormalizationProperties-3.1.0d1.txt");

        generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedEastAsianWidth-3.1.0d1.txt");

        generateVerticalSlice(CATEGORY, CATEGORY+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedGeneralCategory-3.1.0d1.txt");
        generateVerticalSlice(COMBINING_CLASS, COMBINING_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedCombiningClass-3.1.0d1.txt");
        generateVerticalSlice(DECOMPOSITION_TYPE, DECOMPOSITION_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedDecompositionType-3.1.0d1.txt");
        generateVerticalSlice(NUMERIC_TYPE, NUMERIC_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedNumericType-3.1.0d1.txt");
        generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedEastAsianWidth-3.1.0d1.txt");
        generateVerticalSlice(JOINING_TYPE, JOINING_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedJoiningType-3.1.0d1.txt");
        generateVerticalSlice(JOINING_GROUP, JOINING_GROUP+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedJoiningGroup-3.1.0d1.txt");
        generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES+1, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedBinaryProperties-3.1.0d1.txt");
        generateVerticalSlice(LIMIT_ENUM, LIMIT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedNumericValues-3.1.0d1.txt");

        mask = Utility.setBits(0, DerivedProperty.PropMath, DerivedProperty.Mod_ID_Continue_NO_Cf);
        generateDerived(mask, HEADER_DERIVED, "DerivedCoreProperties-3.1.0d1.txt");

        generateVerticalSlice(LINE_BREAK, LINE_BREAK+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedLineBreak-3.1.0d1.txt");

        generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM, KEEP_SPECIAL, HEADER_SCRIPTS, "Scripts-3.1.0d4.txt");

        generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + Noncharacter_Code_Point + 1,
                KEEP_SPECIAL, HEADER_EXTEND, "PropList-3.1.0d5.txt");


            writeNormalizerTestSuite("NormalizationTest-3.1.0d1.txt");

        }




            //generateDerived(Utility.setBits(0, DerivedProperty.PropMath, DerivedProperty.Mod_ID_Continue_NO_Cf),
            //    HEADER_DERIVED, "DerivedPropData2-3.1.0d1.txt");
        //generateVerticalSlice(SCRIPT, SCRIPT+1, KEEP_SPECIAL, "ScriptCommon-3.1.0d1.txt");
        //listStrings("LowerCase-3.1.0d1.txt", 0,0);
        //generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedPropData1-3.1.0d1.txt");

        // AGE stuff
        //UCD ucd = UCD.make();
        //System.out.println(ucd.getAgeID(0x61));
        //System.out.println(ucd.getAgeID(0x2FA1D));


        //generateCompExclusions();
        System.out.println("END");
    }

   static Normalizer nfkc = new Normalizer(Normalizer.NFKC);

    public static void checkHoffman(String test) {
        String result = nfkc.normalize(test);
        System.out.println(Utility.hex(test) + " => " + Utility.hex(result));
        System.out.println();
        show(test, 0);
        System.out.println();
        show(result, 0);
    }

    public static void show(String s, int indent) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            String cc = " " + ucd.getCombiningClass(cp);
            cc = Utility.repeat(" ", 4 - cc.length()) + cc;
            System.out.println(Utility.repeat(" ", indent) + ucd.getCode(cp) + cc + " " + ucd.getName(cp));
            String decomp = nfkc.normalize(cp);
            if (!decomp.equals(UTF32.valueOf32(cp))) {
                show(decomp, indent + 4);
            }
        }
    }


    //Remove "d1" from DerivedJoiningGroup-3.1.0d1.txt type names

    public static String fixFile(String s) {
        int len = s.length();
        if (!s.endsWith(".txt")) return s;
        if (s.charAt(len-6) != 'd') return s;
        char c = s.charAt(len-5);
        if (c < '0' || '9' < c) return s;
        System.out.println("Fixing File Name");
        return s.substring(0,len-6) + s.substring(len-4);
    }

    static final int HEADER_EXTEND = 0, HEADER_DERIVED = 1, HEADER_SCRIPTS = 2;

    public static void doHeader(String fileName, PrintWriter output, int headerChoice) {
        output.println("# " + fixFile(fileName));
        output.println("#");
        if (headerChoice == HEADER_SCRIPTS) {
            output.println("# For documentation, see UTR #24: Script Names");
            output.println("#   http://www.unicode.org/unicode/reports/tr24/");
        } else if (headerChoice == HEADER_EXTEND) {
            output.println("# Unicode Character Database: Extended Properties");
            output.println("# For documentation, see PropList.html");
        } else {
            output.println("# Unicode Character Database: Derived Property Data");
            output.println("# Generated algorithmically from the Unicode Character Database");
            output.println("# For documentation, see DerivedProperties.html");
        }
        output.println("# Date: " + myDateFormat.format(new Date()) + " [MD]");
        output.println("# Note: Unassigned and Noncharacter codepoints are omitted,");
        output.println("#       except when listing Noncharacter or Cn.");
        output.println("# ================================================");
        output.println();
    }

    public static void generateDerived (long bitMask, int headerChoice, String fileName) throws IOException {
        ucd = UCD.make("3.1.0");
        PrintWriter output = Utility.openPrintWriter(fileName);
        doHeader(fileName, output, headerChoice);
        for (int i = 0; i < 32; ++i) {
            if ((bitMask & (1<<i)) == 0) continue;
            if (i >= DERIVED_PROPERTY_LIMIT) break;
            System.out.print('.');
            output.println("# ================================================");
            output.println();
            new DerivedPropertyLister(ucd, i, output).print();
        }
        output.close();
    }

    /*
    public static void listStrings(String file, int type, int subtype) throws IOException {
        ucd = UCD.make("3.1.0");
        UCD ucd30 = UCD.make("3.0.0");
        PrintWriter output = new PrintStream(new FileOutputStream(GEN_DIR + file));

        for (int i = 0; i < 0x10FFFF; ++i) {
            if ((i & 0xFFF) == 0) System.out.println("# " + i);
            if (!ucd.isRepresented(i)) continue;
            if (ucd30.isRepresented(i)) continue;
            String string = "";
            switch(type) {
                case 0: string = ucd.getSimpleLowercase(i);
            }
            if (UTF32.length32(string) == 1 && UTF32.char32At(string,0) == i) continue;
            output.println(Utility.hex(i) + "; C; " + Utility.hex(string) + "; # " + ucd.getName(i));
        }
        output.close();
    }

    public static void generateCompExclusions() throws IOException {
        PrintWriter output = Utility.openPrintWriter("CompositionExclusionsDelta.txt");
        new CompLister(output).print();
        output.close();
    }

    static class CompLister extends PropertyLister {
        UCD oldUCD;
        int oldLength = 0;

        public CompLister(PrintWriter output) {
            this.output = output;
            ucdData = UCD.make("3.1.0");
            oldUCD = UCD.make("3.0.0");
            showOnConsole = true;
        }
        public String valueName(int cp) {
            return UTF32.length32(ucdData.getDecompositionMapping(cp)) + "";
        }
        public byte status(int cp) {
            if (ucdData.getDecompositionType(cp) == CANONICAL
              && oldUCD.getDecompositionType(cp) != CANONICAL) {
                int temp = oldLength;
                oldLength = UTF32.length32(ucdData.getDecompositionMapping(cp));
                if (temp != oldLength) return BREAK;
                return INCLUDE;
            }
            return EXCLUDE;
        }
    }

    static final byte KEEP_SPECIAL = 0, SKIP_SPECIAL = 1;

    public static void generateVerticalSlice(int startEnum, int endEnum, byte skipSpecial, int headerChoice, String file) throws IOException {

        //System.out.println(ucd.toString(0x1E0A));
        /*
        System.out.println(ucd.getData(0xFFFF));
        System.out.println(ucd.getData(0x100000));
        System.out.println(ucd.getData(0x100000-1));
        System.out.println(ucd.getData(0x100000-2));
        System.out.println(ucd.getData(0x100000-3));
        if (true) return;
        String test2 = ucd.getName(0x2A6D6);
        //* /


        PrintWriter output = Utility.openPrintWriter(file);
        doHeader(file, output, headerChoice);
        
        int last = -1;
        for (int i = startEnum; i < endEnum; ++i) {
            UnicodeProperty up = UnifiedBinaryProperty.make(i, ucd);
            if (up == null) continue;
            
            if (i == DECOMPOSITION_TYPE || i == NUMERIC_TYPE
                || i == (CATEGORY | UNUSED_CATEGORY)
                || i == (BINARY_PROPERTIES | Non_break)
                || i == (JOINING_TYPE | JT_U)
                || i == (SCRIPT | UNUSED_SCRIPT)
                || i == (JOINING_GROUP | NO_SHAPING)
                ) continue; // skip zero case
            if (skipSpecial == SKIP_SPECIAL
                    && i >= (BINARY_PROPERTIES | CompositionExclusion)
                    && i < (AGE + NEXT_ENUM)) continue;
            if ((last & 0xFF00) != (i & 0xFF00) && (i <= BINARY_PROPERTIES || i >= SCRIPT)) {
                output.println();
                output.println("# ================================================");
                output.println("# " + UCD_Names.UNIFIED_PROPERTIES[i>>8]);
                output.println("# ================================================");
                output.println();
                System.out.println();
                System.out.println(UCD_Names.UNIFIED_PROPERTIES[i>>8]);
                last = i;
            } else {
                output.println("# ================================================");
                output.println();
            }
            System.out.print(".");
            new MyPropertyLister(ucd, i, output).print();
        }
        if (endEnum == LIMIT_ENUM) {
            output.println();
                output.println("# ================================================");
            output.println("# Numeric Values (from UnicodeData.txt, field 6/7/8)");
                output.println("# ================================================");
            output.println();
            System.out.println();
            System.out.println("@NUMERIC VALUES");

            Set floatSet = new TreeSet();
            for (int i = 0; i < 0x10FFFF; ++i) {
                float nv = ucd.getNumericValue(i);
                if (Float.isNaN(nv)) continue;
                floatSet.add(new Float(nv));
            }
            Iterator it = floatSet.iterator();
            while(it.hasNext()) {
                new MyFloatLister(ucd, ((Float)it.next()).floatValue(), output).print();
                output.println();
                System.out.print(".");
            }
        }
        output.close();
        System.out.println();
    }

    static UCD ucd;

    static public Normalizer formC, formD, formKC, formKD;

    static public void writeNormalizerTestSuite(String fileName) throws IOException {

        PrintWriter log = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(GEN_DIR + fileName),
                "UTF8"),
            32*1024));
	    formC = new Normalizer(Normalizer.NFC);
	    formD = new Normalizer(Normalizer.NFD);
	    formKC = new Normalizer(Normalizer.NFKC);
	    formKD = new Normalizer(Normalizer.NFKD);

        log.println("# " + fixFile(fileName));
        log.println("#");
        log.println("# Normalization Test Suite");
        log.println("# Date: " + myDateFormat.format(new Date()) + " [MD]");
        log.println("# Format:");
        log.println("#");
        log.println("#   Columns (c1, c2,...) are separated by semicolons");
        log.println("#   Comments are indicated with hash marks");
        log.println("#");
        log.println("# CONFORMANCE:");
        log.println("# 1. The following invariants must be true for all conformant implementations");
        log.println("#");
        log.println("#    NFC");
        log.println("#      c2 ==  NFC(c1) ==  NFC(c2) ==  NFC(c3)");
        log.println("#      c4 ==  NFC(c4) ==  NFC(c5)");
        log.println("#");
        log.println("#    NFD");
        log.println("#      c3 ==  NFD(c1) ==  NFD(c2) ==  NFD(c3)");
        log.println("#      c5 ==  NFD(c4) ==  NFD(c5");
        log.println("#");
        log.println("#    NFKC");
        log.println("#      c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)");
        log.println("#");
        log.println("#    NFKD");
        log.println("#      c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)");
        log.println("#");
        log.println("# 2. For every assigned Unicode 3.1.0 code point X that is not specifically");
        log.println("#    listed in Part 1, the following invariants must be true for all conformant");
        log.println("#    implementations:");
        log.println("#");
        log.println("#      X == NFC(X) == NFD(X) == NFKC(X) == NFKD(X)");

        System.out.println("Writing Part 1");

        log.println("#");
        log.println("@Part0 # Specific cases");
        log.println("#");

        for (int j = 0; j < testSuiteCases.length; ++j) {
            writeLine(testSuiteCases[j], log, false);
        }

        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part1 # Character by character test");
        log.println("# All characters not explicitly occurring in c1 of Part 1 have identical NFC, D, KC, KD forms.");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            String cc = UTF32.valueOf32(ch);
            writeLine(cc,log, true);
        }
        Utility.fixDot();

        System.out.println("Finding Examples");

        String[] example = new String[256];

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            int cc = ucd.getCombiningClass(ch);
            if (example[cc] == null) example[cc] = UTF32.valueOf32(ch);
        }

        Utility.fixDot();
        System.out.println("Writing Part 3");

        log.println("#");
        log.println("@Part2 # Canonical Order Test");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            short c = ucd.getCombiningClass(ch);
            if (c == 0) continue;

            // add character with higher class, same class, lower class

            String sample = "";
            for (int i = c+1; i < example.length; ++i) {
                if (example[i] == null) continue;
                sample += example[i];
                break;
            }
            sample += example[c];
            for (int i = c-1; i > 0; --i) {
                if (example[i] == null) continue;
                sample += example[i];
                break;
            }

            writeLine("a" + sample + UTF32.valueOf32(ch) + "b", log, false);
            writeLine("a" + UTF32.valueOf32(ch) + sample + "b", log, false);
        }
        Utility.fixDot();
        log.println("#");
        log.println("# END OF FILE");
        log.close();
    }

    static void writeLine(String cc, PrintWriter log, boolean check) {
        String c = formC.normalize(cc);
        String d = formD.normalize(cc);
        String kc = formKC.normalize(cc);
        String kd = formKD.normalize(cc);
        if (check & cc.equals(c) && cc.equals(d) && cc.equals(kc) && cc.equals(kd)) return;
        log.println(
            Utility.hex(cc," ") + ";" + Utility.hex(c," ") + ";" + Utility.hex(d," ") + ";"
            + Utility.hex(kc," ") + ";" + Utility.hex(kd," ")
            + "; # ("
            + comma(cc) + "; " + comma(c) + "; " + comma(d) + "; " + comma(kc) + "; " + comma(kd) + "; "
            + ") " + ucd.getName(cc));
    }

    static StringBuffer commaResult = new StringBuffer();

    // not recursive!!!
    static final String comma(String s) {
        commaResult.setLength(0);
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(i)) {
            cp = UTF32.char32At(s, i);
            if (ucd.getCategory(cp) == Mn) commaResult.append('\u25CC');
            UTF32.append32(commaResult, cp);
        }
        return commaResult.toString();
    }

    static final String[] testSuiteCases = {
        "\u1E0A",
        "\u1E0C",
        "\u1E0A\u0323",
        "\u1E0C\u0307",
        "D\u0307\u0323",
        "D\u0323\u0307",
        "\u1E0A\u031B",
        "\u1E0C\u031B",
        "\u1E0A\u031B\u0323",
        "\u1E0C\u031B\u0307",
        "D\u031B\u0307\u0323",
        "D\u031B\u0323\u0307",
        "\u00C8",
        "\u0112",
        "E\u0300",
        "E\u0304",
        "\u1E14",
        "\u0112\u0300",
        "\u1E14\u0304",
        "E\u0304\u0300",
        "E\u0300\u0304",
    };
//*/
}