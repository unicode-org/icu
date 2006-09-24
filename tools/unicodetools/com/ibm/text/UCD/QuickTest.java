/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/QuickTest.java,v $
* $Date: 2006/09/24 23:32:45 $
* $Revision: 1.13 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;

import com.ibm.icu.dev.demo.translit.CaseIterator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty.UnicodeMapProperty;
import com.ibm.icu.impl.PrettyPrinter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Collator;
//import com.ibm.icu.text.Normalizer;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class QuickTest implements UCD_Types {
	public static void main(String[] args) throws IOException {
		try {
            
            getHangulDecomps();

            if (true) return;

			
      showLeadingTrailingNonStarters();
      //checkBufferStatus(true);
      
      
			checkNormalization("NFC", Default.nfc());
			//checkNormalization("NFKC", Default.nfkc());
			
			if (true) return;
			
			checkCaseChanges();
			if (true) return;
			
			
			
			checkCase();

			getCaseFoldingUnstable();

			getCaseLengths("Lower", UCD.LOWER);
			getCaseLengths("Upper", UCD.UPPER);
			getCaseLengths("Title", UCD.TITLE);
			getCaseLengths("Fold", UCD.FOLD);

			checkUnicodeSet();
			getLengths("NFC", Default.nfc());
			getLengths("NFD", Default.nfd());
			getLengths("NFKC", Default.nfkc());
			getLengths("NFKD", Default.nfkd());

			
			if (true) return;
			tem();
			//checkPrettyPrint();
			Collection l = new CaseVariantMaker().getVariants("abc");
			for (Iterator it = l.iterator(); it.hasNext();) {
				System.out.println(it.next());
			}
			String propName = UCharacter.getPropertyName(3, UProperty.NameChoice.LONG);
			//testProps();
			
			getBidiMirrored();
			getHasAllNormalizations();
		} finally {
			System.out.println("Done");
		}
	}
	
	private static void getHangulDecomps() {
        //Normalizer nfkd500 = new Normalizer(Normalizer.NFKD, "5.0.0");
        Normalizer nfkd218 = new Normalizer(Normalizer.NFKD, "2.1.8");
        UnicodeMap diff = new UnicodeMap();
        Map compose = new HashMap();
        Map decompose = new HashMap();
        // UnicodeSet applicable = // new UnicodeSet("[:HangulSyllable=NA:]");
        UnicodeSet applicable = new UnicodeSet("[[\u1100-\u11FF \uAC00-\uD7FF]&[:assigned:]]");
        for (UnicodeSetIterator it = new UnicodeSetIterator(applicable); it.next(); ) {
            String source = it.getString();
            String v218 = nfkd218.normalize(source);
            //String v500 = nfkd500.normalize(source);
            if (v218.equals(source)) continue;
            decompose.put(source, v218);
            compose.put(v218, source);
        }
        // now try recomposing

        for (Iterator it = decompose.keySet().iterator(); it.hasNext();) {
            String source = (String) it.next();
            String decomposition = (String) decompose.get(source);
            if (decomposition.length() > 2) {
                String trial = decomposition.substring(0, decomposition.length() - 1);
                String composition = (String) compose.get(trial);
                if (composition != null) {
                    decomposition = composition + decomposition.substring(decomposition.length() - 1);
                }
            }
            if (decomposition.length() != 2) System.out.println("Failed decomp: " + Default.ucd().getCodeAndName(source));
            diff.put(source.charAt(0), com.ibm.text.utility.Utility.hex(decomposition, " "));
        }
        UnicodeMapProperty p = new UnicodeMapProperty().set(diff);
        BagFormatter bf = new BagFormatter().setValueSource(p);
        System.out.println(bf.showSetNames(diff.keySet()));
    }

    static void checkNormalization(String title, Normalizer nfx) {
		UnicodeSet trailing = new UnicodeSet();
		UnicodeSet leading = new UnicodeSet();
		UnicodeSet starter = new UnicodeSet();
		UnicodeSet nonStarter = new UnicodeSet();
		UnicodeSet disallowed = new UnicodeSet();
		for (int i = 0; i <= 0x10FFFF; ++i) {
			if (!nfx.isNormalized(i)) {
				disallowed.add(i);
				continue;
			}
			if (nfx.isLeading(i)) leading.add(i);
			if (nfx.isTrailing(i)) trailing.add(i);
			if (Default.ucd().getCombiningClass(i) == 0) starter.add(i);
			else nonStarter.add(i);
		}
		UnicodeSet allowed = new UnicodeSet(disallowed).complement();		
		UnicodeSet leadingOnly = new UnicodeSet(leading).removeAll(trailing);
		UnicodeSet trailingOnly = new UnicodeSet(trailing).removeAll(leading);
		UnicodeSet both = new UnicodeSet(trailing).retainAll(leading);
		UnicodeSet stable = new UnicodeSet(allowed).removeAll(leading).removeAll(trailing);
		
		UnicodeSet starterLeadingOnly = new UnicodeSet(starter).retainAll(leadingOnly);
		UnicodeSet starterTrailingOnly = new UnicodeSet(starter).retainAll(trailingOnly);
		UnicodeSet starterStable = new UnicodeSet(starter).retainAll(stable);
		UnicodeSet starterBoth = new UnicodeSet(starter).retainAll(both);

		UnicodeSet nonStarterTrailing = new UnicodeSet(nonStarter).retainAll(trailing);
		UnicodeSet nonStarterNonTrailing = new UnicodeSet(nonStarter).removeAll(trailing);
		
		System.out.println();
		System.out.println(title);
		System.out.println("Starter, CWF-Only: " + starterLeadingOnly.size());
		System.out.println("Starter, CWP-Only: " + starterTrailingOnly.size());
		System.out.println("Starter, Stable: " + starterStable.size());
		System.out.println("Starter, Both: " + starterBoth.size());
		System.out.println("Non-Starter, CWP: " + nonStarterTrailing.size());
		System.out.println("Non-Starter, Non-CWP: " + nonStarterNonTrailing.size());
		System.out.println("Disallowed: " + disallowed.size());
		
		BagFormatter bf = new BagFormatter();

		ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("5.0.0");
		bf.setUnicodePropertyFactory(ups);

		System.out.println("Starter, CWF-Only: " + "\r\n" + bf.showSetNames(starterLeadingOnly));
		System.out.println("Starter, CWP-Only: " + "\r\n" + bf.showSetNames(starterTrailingOnly));
		System.out.println("Starter, Stable: " + "\r\n" + bf.showSetNames(starterStable));
		System.out.println("Starter, Both: " + "\r\n" + bf.showSetNames(starterBoth));
		System.out.println("Non-Starter, CWP: " + "\r\n" + bf.showSetNames(nonStarterTrailing));
		System.out.println("Non-Starter, Non-CWP: " + "\r\n" + bf.showSetNames(nonStarterNonTrailing));
		System.out.println("Disallowed: " + "\r\n" + bf.showSetNames(disallowed));
		
//		System.out.println(bf.showSetDifferences("NFC CWP", leadingC, "NFC Trailing", trailingC));
	}
	
	private static void checkCaseChanges() {
		String first = "3.0.0";
		String last = "4.1.0";
		UCD ucd30 = UCD.make(first);
		UCD ucd50 = UCD.make(last);
		
		UnicodeSet sameBehavior = new UnicodeSet();
		UnicodeSet newIn50 = new UnicodeSet();
		UnicodeSet differentBehavior = new UnicodeSet();
		for (int i = 0; i < 0x10FFFF; ++i) {
			int type = ucd50.getCategory(i);
			if (type == UCD.UNASSIGNED || type == UCD.PRIVATE_USE || type == UCD.SURROGATE) continue;
			String c1 = UTF16.valueOf(i);
			String c3 = ucd30.getCase(i,UCD.FULL,UCD.FOLD);
			String c5 = ucd50.getCase(i,UCD.FULL,UCD.FOLD);
			if (c1.equals(c3) && c1.equals(c5)) continue;
			if (!ucd30.isAssigned(i)) {
				newIn50.add(i);
			} else if (c3.equals(c5)) {
				sameBehavior.add(i);
			} else {
				differentBehavior.add(i);
				System.out.println(ucd50.getCodeAndName(i));
				System.out.println("3.0=>" + ucd50.getCodeAndName(c3));
				System.out.println("5.0=>" + ucd50.getCodeAndName(c5));
			}
		}
		BagFormatter bf = new BagFormatter();
		ToolUnicodePropertySource ups = ToolUnicodePropertySource.make(last);
		bf.setUnicodePropertyFactory(ups);
		System.out.println("In 5.0 but not 3.0: " + newIn50);
		System.out.println(bf.showSetNames(newIn50));
		System.out.println();
		System.out.println("Same Behavior in 3.0 and 5.0: " + sameBehavior);
		System.out.println(bf.showSetNames(sameBehavior));
		System.out.println();
		System.out.println("Different Behavior in 3.0 and 5.0: " + differentBehavior);
		System.out.println(bf.showSetNames(differentBehavior));
	}

	private static void checkUnicodeSet() {
		UnicodeSet uset = new UnicodeSet("[a{bc}{cd}pqr\u0000]");
		System.out.println(uset + " ~ " + uset.getRegexEquivalent());
		String[][] testStrings = {
				{"x", "none"},
				{"bc", "all"},
				{"cdbca", "all"},
				{"a", "all"},
				{"bcx", "some"},
				{"ab", "some"},
				{"acb", "some"},
				{"bcda", "some"},
				{"dccbx", "none"},
			};
		for (int i = 0; i < testStrings.length; ++i) {
			check(uset, testStrings[i][0], testStrings[i][1]);
		}
	}

	private static void check(UnicodeSet uset, String string, String desiredStatus) {
		boolean shouldContainAll = desiredStatus.equals("all");
		boolean shouldContainNone = desiredStatus.equals("none");
	    System.out.println((uset.containsAll(string) == shouldContainAll ? "" : "FAILURE:") + "\tcontainsAll " +  string + " = " + shouldContainAll);
	    System.out.println((uset.containsNone(string) == shouldContainNone ? "" : "FAILURE:") + "\tcontainsNone " +  string + " = " + shouldContainNone);
	}

	private static void getCaseFoldingUnstable() {
		for (int i = 3; i < com.ibm.text.utility.Utility.searchPath.length - 1; ++i) {
			String newName = com.ibm.text.utility.Utility.searchPath[i];
			String oldName = com.ibm.text.utility.Utility.searchPath[i+1];
			showMemoryUsage();		
			UCD ucdNew = UCD.make(newName);
			showMemoryUsage();
			UCD ucdOld = UCD.make(oldName);
			showMemoryUsage();
			UnicodeMap differences = new UnicodeMap();
			UnicodeSet differenceSet = new UnicodeSet();
			for (int j = 0; j < 0x10FFFF; ++j) {
				if (!ucdOld.isAssigned(j)) continue;
				String oldString = ucdOld.getCase(j, UCD.FULL, UCD.FOLD);
				String newString = ucdNew.getCase(j, UCD.FULL, UCD.FOLD);
				if (!oldString.equals(newString)) {
					differenceSet.add(j);
					differences.put(j, new String[]{oldString, newString});
					System.out.println(".");
				}
			}
			if (differenceSet.size() != 0) {
				System.out.println("Differences in " + com.ibm.text.utility.Utility.searchPath[i]);
				for (UnicodeSetIterator it = new UnicodeSetIterator(differenceSet); it.next();) {
					System.out.println(ucdNew.getCodeAndName(it.codepoint));
					String[] strings = (String[]) differences.getValue(it.codepoint);
					System.out.println("\t" + oldName + ": " + ucdNew.getCodeAndName(strings[0]));
					System.out.println("\t" + newName + ": " + ucdNew.getCodeAndName(strings[1]));
				}
			}
		}
	}

	  static public void showMemoryUsage() {
		    System.gc(); System.gc(); System.gc(); System.gc();
		    System.gc(); System.gc(); System.gc(); System.gc();
		    System.gc(); System.gc(); System.gc(); System.gc();
		    System.gc(); System.gc(); System.gc(); System.gc();
		    System.out.println("total:\t" + Runtime.getRuntime().totalMemory() + ";\tfree:\t" + 
		      Runtime.getRuntime().freeMemory());
		  }

	private static void getHasAllNormalizations() {
		UnicodeSet items = new UnicodeSet();
		Set s = new LinkedHashSet();
		for (int i = 0; i <= 0x10FFFF; ++i) {
			if (!Default.ucd().isAssigned(i)) continue;
			if (Default.ucd().getDecompositionType(i) == UCD.NONE) continue;
			String source = UTF16.valueOf(i);
			String nfc = Default.nfc().normalize(source);
			String nfd = Default.nfd().normalize(source);
			String nfkd = Default.nfkd().normalize(source);
			String nfkc = Default.nfkc().normalize(source);
			s.clear();
			s.add(source);
			s.add(nfc);
			s.add(nfd);
			s.add(nfkd);
			s.add(nfkc);
			if (s.size() > 3) {
				System.out.println(Utility.hex(source) + "\t" + Utility.escape(source)
					+ "\t" + Default.ucd().getName(source)
					+ "\tnfd\t" + Utility.hex(nfd) + "\t" + Utility.escape(nfd)
					+ "\tnfc\t" + Utility.hex(nfc) + "\t" + Utility.escape(nfc)
					+ "\tnfkd\t" + Utility.hex(nfkd) + "\t" + Utility.escape(nfkd)
					+ "\tnfkc\t" + Utility.hex(nfkc) + "\t" + Utility.escape(nfkc));
			}
		}
	}

	static UnicodeMap.Composer MyComposer = new UnicodeMap.Composer(){
		public Object compose(int codePoint, Object a, Object b) {
			if (a == null) return b;
			if (b == null) return a;
			return a + "; " + b;
		}		
	};

	static void add(UnicodeMap map, int cp, String s) {
		String x = (String) map.getValue(cp);
		if (x == null) map.put(cp, s);
		else map.put(cp, x + "; " + s);
	}
	
	private static void getBidiMirrored() throws IOException {
		//UnicodeMap.Composer composer;
		//ToolUnicodePropertySource foo = ToolUnicodePropertySource.make("");
		UnicodeSet proposed = new UnicodeSet("[\u0F3A-\u0F3D\u169B\u169C\u2018-\u201F\u301D-\u301F\uFD3E\uFD3F\uFE59-\uFE5E\uFE64\uFE65\\U0001D6DB\\U0001D715\\U0001D74F\\U0001D789\\U0001D7C3]");
		//UnicodeSet proposed = new UnicodeSet("[\u0F3A-\u0F3D\u169B\u169C\u2018-\u201F\u301D-\u301F\uFD3E\uFD3F\uFE59-\uFE5E\uFE64\uFE65]");
		UnicodeMap status = new UnicodeMap();
		UCD ucd31 = UCD.make("3.1.0");
		for (int cp = 0; cp < 0x10FFFF; ++cp) {
			if (!Default.ucd().isAssigned(cp)) continue;
			if (Default.ucd().isPUA(cp)) continue;
			
			if (proposed.contains(cp)) {
				add(status, cp, "***");
			}

			int type = Default.ucd().getCategory(cp);
			if (type == UCD.Ps || type == Pe || type == Pi || type == Pf) {
				add(status, cp, "Px");
			}
			
			String s = Default.ucd().getBidiMirror(cp);
			if (!s.equals(UTF16.valueOf(cp))) add(status, cp, "bmg");
			
			if (ucd31.getBinaryProperty(cp,BidiMirrored)) {
				add(status, cp, "bmp3.1");
			} else if (Default.ucd().getBinaryProperty(cp,BidiMirrored)) {
				add(status, cp, "bmp5.0");
			} else if (!Default.nfkc().isNormalized(cp)) {
				String ss = Default.nfkc().normalize(cp);
				if (isBidiMirrored(ss)) {
					add(status, cp, "bmp(" + Utility.hex(ss) + ")");
					String name = Default.ucd().getName(cp);
					if (name.indexOf("VERTICAL") < 0) proposed.add(cp);
				}

			}
			
			if (type == Sm) {
				add(status, cp, "Sm");
			}
			else if (Default.ucd().getBinaryProperty(cp,Math_Property)) {
				String ss = Default.nfkc().normalize(cp);
				if (UTF16.countCodePoint(ss) == 1) {
					int cp2 = UTF16.charAt(ss, 0);
					int type2 = Default.ucd().getCategory(cp2);
					if (type2 == UCD.Lu || type2 == Ll || type2 == Lo || type2 == Nd) {
						//System.out.println("Skipping: " + Default.ucd().getCodeAndName(cp));
					} else {
						add(status, cp, "S-Math");
					}
				} else {
					add(status, cp, "S-Math");
				}
			}
		
//		temp = new UnicodeMap();
//		UnicodeSet special = new UnicodeSet("[<>]");
//		for (UnicodeSetIterator it = new UnicodeSetIterator(mathSet); it.next();) {
//			String s = Default.nfkd().normalize(it.codepoint);
//			if (special.containsSome(s)) temp.put(it.codepoint, "*special*");
//		}
//		status.composeWith(temp, MyComposer);
		
		//showStatus(status);
		// close under nfd

		}
		//proposed = status.getSet("Px");
		System.out.println(proposed);
		//showStatus(status);
		PrintWriter pw = BagFormatter.openUTF8Writer(UCD.GEN_DIR, "bidimirroring_chars.txt");
		showStatus(pw, status);
		pw.close();
	}

	private static boolean isBidiMirrored(String ss) {
		int cp;
		for (int i = 0; i < ss.length(); i += UTF16.getCharCount(cp)) {
			cp = UTF16.charAt(ss, i);
			if (!Default.ucd().getBinaryProperty(cp,BidiMirrored)) return false;
		}
		return true;
	}

	static BagFormatter bf = new BagFormatter();
	private static void showStatus(PrintWriter pw, UnicodeMap status) {
		Collection list = new TreeSet(status.getAvailableValues());
		for (Iterator it = list.iterator(); it.hasNext(); ) {
			String value = (String) it.next();
			if (value == null) continue;
			UnicodeSet set = status.getSet(value);
			for (UnicodeSetIterator umi = new UnicodeSetIterator(set); umi.next();) {
				pw.println(Utility.hex(umi.codepoint) 
						//+ (value.startsWith("*") ? ";\tBidi_Mirrored" : "")
						+ "\t# " + value
						+ "\t\t( " + UTF16.valueOf(umi.codepoint) + " ) "
						//+ ";\t" + (x.contains(umi.codepoint) ? "O" : "")
						+ "\t" + Default.ucd().getName(umi.codepoint));
			}
		}
	}


	public static class Length {
		String title;
		int bytesPerCodeUnit;
		int longestCodePoint = -1;
		double longestLength = 0;
		UnicodeMap longestSet = new UnicodeMap();
		Length(String title, int bytesPerCodeUnit) {
			this.title = title;
			this.bytesPerCodeUnit = bytesPerCodeUnit;
		}
		void add(int codePoint, int cuLen, int processedUnitLength, String processedString) {
			double codeUnitLength = processedUnitLength / (double) cuLen;
			if (codeUnitLength > longestLength) {
				longestCodePoint = codePoint;
				longestLength = codeUnitLength;
				longestSet.clear();
				longestSet.put(codePoint, processedString);
				System.out.println(title + " \t(" + codeUnitLength*bytesPerCodeUnit + " bytes, "
						+ codeUnitLength + " code units) \t"
						+ longestLength + " expansion) \t"
						+ Default.ucd().getCodeAndName(codePoint)
						+ "\r\n\t=> " + Default.ucd().getCodeAndName(processedString)
						);				
			} else if (codeUnitLength == longestLength) {
				longestSet.put(codePoint, processedString);
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
			String is = UTF16.valueOf(i);
			String norm = normalizer.normalize(i);
			utf8Len.add(i, getUTF8Length(is), getUTF8Length(norm), norm);
			utf16Len.add(i, is.length(), norm.length(), norm);
			utf32Len.add(i, 1, UTF16.countCodePoint(norm), norm);
		}
		UnicodeSet common = new UnicodeSet(utf8Len.longestSet.keySet())
			.retainAll(utf16Len.longestSet.keySet())
			.retainAll(utf32Len.longestSet.keySet());
		if (common.size() > 0) {
			UnicodeSetIterator it = new UnicodeSetIterator(common);
			it.next();
			System.out.println("Common Exemplar: " + Default.ucd().getCodeAndName(it.codepoint));
		}
	}
	
	private static void getCaseLengths(String title, byte caseType) throws IOException {
		System.out.println();
		Length utf8Len = new Length(title + "\tUTF8", 1);
		Length utf16Len = new Length(title + "\tUTF16", 1);
		Length utf32Len = new Length(title + "\tUTF32", 1);
		for (int i = 0; i <= 0x10FFFF; ++i) {
			int type = Default.ucd().getCategoryMask(i);
			if ((type & skip) != 0) continue;
			String is = UTF16.valueOf(i);
			String norm = Default.ucd().getCase(i, UCD.FULL, caseType);
			utf8Len.add(i, getUTF8Length(is), getUTF8Length(norm), norm);
			utf16Len.add(i, is.length(), norm.length(), norm);
			utf32Len.add(i, 1, UTF16.countCodePoint(norm), norm);
		}
		UnicodeSet common = new UnicodeSet(utf8Len.longestSet.keySet())
			.retainAll(utf16Len.longestSet.keySet())
			.retainAll(utf32Len.longestSet.keySet());
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
        //Utility.showSetNames("", set1, false, Default.ucd());
    }
    
	
	private static void checkPrettyPrint() {
		//System.out.println("Test: " + fixTransRule("\\u0061"));
		UnicodeSet s = new UnicodeSet("[^[:script=common:][:script=inherited:]]");
		UnicodeSet quoting = new UnicodeSet("[[:Mn:][:Me:]]");
		String ss = new PrettyPrinter().setToQuote(quoting).toPattern(s);
		System.out.println("test: " + ss);
	}
	
	static class CaseVariantMaker {
		private ULocale locale = ULocale.ROOT;
		private String string = null;
		private Collection output;
		
		private Collection getVariants(String string) {
			return getVariants(string, null);
		}
		
		private Collection getVariants(String string, Collection output) {
			this.string = string;
			if (output == null)  output = new ArrayList();
			this.output = output;
			getSimpleCaseVariants(0, "");
			return output;
		}
		
		private void getSimpleCaseVariants(int i, String soFar) {
			if (i == string.length()) {
				output.add(soFar);
				return;
			}
			// can optimize later
			String s = UTF16.valueOf(string, i);
			i += s.length();
			getSimpleCaseVariants(i, soFar + s);
			String upper = UCharacter.toUpperCase(locale, s);
			if (!upper.equals(s)) {
				getSimpleCaseVariants(i, soFar + upper);
			}
			String title = UCharacter.toTitleCase(locale, s, null);
			if (!title.equals(s) && !title.equals(upper)) {
				getSimpleCaseVariants(i, soFar + title);
			}
			String lower = UCharacter.toLowerCase(locale, s);
			if (!lower.equals(s) && !lower.equals(upper) && !lower.equals(title)) {
				getSimpleCaseVariants(i, soFar + lower);
			}
		}
		
		public ULocale getLocale() {
			return locale;
		}
		
		public void setLocale(ULocale locale) {
			this.locale = locale;
		}
	}
	
	private static void tem() {
		PrintStream out = System.out;
		String text = "\ufb03";
		
		String BASE_RULES =
			"'<' > '&lt;' ;" +
			"'<' < '&'[lL][Tt]';' ;" +
			"'&' > '&amp;' ;" +
			"'&' < '&'[aA][mM][pP]';' ;" +
			"'>' < '&'[gG][tT]';' ;" +
			"'\"' < '&'[qQ][uU][oO][tT]';' ; " +
			"'' < '&'[aA][pP][oO][sS]';' ; ";
		
		String CONTENT_RULES =
			"'>' > '&gt;' ;";
		
		String HTML_RULES = BASE_RULES + CONTENT_RULES + 
		"'\"' > '&quot;' ; ";
		
		String HTML_RULES_CONTROLS = HTML_RULES + 
		"([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]-[\\u0020]]) > &hex/xml($1) ; ";
		
		
		Transliterator toHTML = Transliterator.createFromRules(
				"any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
		
		int[][] ranges = {{UProperty.BINARY_START, UProperty.BINARY_LIMIT},
				{UProperty.INT_START, UProperty.INT_LIMIT},
				{UProperty.DOUBLE_START, UProperty.DOUBLE_START},
				{UProperty.STRING_START, UProperty.STRING_LIMIT},
		};
		Collator col = Collator.getInstance(ULocale.ROOT);
		((RuleBasedCollator)col).setNumericCollation(true);
		Map alpha = new TreeMap(col);
		
		String HTML_INPUT = "::hex-any/xml10; ::hex-any/unicode; ::hex-any/java;";
		Transliterator fromHTML = Transliterator.createFromRules(
				"any-xml", HTML_INPUT, Transliterator.FORWARD);
		
		text = fromHTML.transliterate(text);
		
		int cp = UTF16.charAt(text, 0);
		text = UTF16.valueOf(text,0);
		for (int range = 0; range < ranges.length; ++range) {
			for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
				String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
				String propValue = null;
				int ival;
				switch (range) {
				default: propValue = "???"; break;
				case 0: ival = UCharacter.getIntPropertyValue(cp, propIndex);
				if (ival != 0) propValue = "True";
				break;
				case 2: propValue = String.valueOf(UCharacter.getNumericValue(cp)); break;
				case 3: 
					propValue = UCharacter.getStringPropertyValue(propIndex, cp, UProperty.NameChoice.LONG); 
					if (text.equals(propValue)) propValue = null;
					break;
				case 1: ival = UCharacter.getIntPropertyValue(cp, propIndex);
				if (ival != 0) {
					propValue = UCharacter.getPropertyValueName(propIndex, ival, UProperty.NameChoice.LONG);
					if (propValue == null) propValue = String.valueOf(ival);
				}
				break;					
				}
				if (propValue != null) {
					alpha.put(propName, propValue);
				}
			}
		}
		String x;
		String upper = x = UCharacter.toUpperCase(ULocale.ENGLISH,text);
		if (!text.equals(x)) alpha.put("Uppercase", x);
		String lower = x = UCharacter.toLowerCase(ULocale.ENGLISH,text);
		if (!text.equals(x)) alpha.put("Lowercase", x);
		String title = x = UCharacter.toTitleCase(ULocale.ENGLISH,text,null);
		if (!text.equals(x)) alpha.put("Titlecase", x);
		String nfc = x = com.ibm.icu.text.Normalizer.normalize(text,com.ibm.icu.text.Normalizer.NFC);
		if (!text.equals(x)) alpha.put("NFC", x);
		String nfd = x = com.ibm.icu.text.Normalizer.normalize(text,com.ibm.icu.text.Normalizer.NFD);
		if (!text.equals(x)) alpha.put("NFD", x);
		x = com.ibm.icu.text.Normalizer.normalize(text,com.ibm.icu.text.Normalizer.NFKD);
		if (!text.equals(x)) alpha.put("NFKD", x);
		x = com.ibm.icu.text.Normalizer.normalize(text,com.ibm.icu.text.Normalizer.NFKC);
		if (!text.equals(x)) alpha.put("NFKC", x);
		
		CanonicalIterator ci = new CanonicalIterator(text);
		int count = 0;
		for (String item = ci.next(); item != null; item = ci.next()) {
			if (item.equals(text)) continue;
			if (item.equals(nfc)) continue;
			if (item.equals(nfd)) continue;
			alpha.put("Other_Canonical_Equivalent#" + (++count), item);
		}
		
		CaseIterator cai = new CaseIterator();
		cai.reset(text);
		count = 0;
		for (String item = cai.next(); item != null; item = cai.next()) {
			if (item.equals(text)) continue;
			if (item.equals(upper)) continue;
			if (item.equals(lower)) continue;
			if (item.equals(title)) continue;
			alpha.put("Other_Case_Equivalent#" + (++count), item);
		}
		
		out.println("<table>");
		out.println("<tr><td><b>" + "Character" + "</b></td><td><b>" + toHTML.transliterate(text) + "</b></td></tr>");
		out.println("<tr><td><b>" + "Code_Point" + "</b></td><td><b>" + com.ibm.icu.impl.Utility.hex(cp,4) + "</b></td></tr>");
		out.println("<tr><td><b>" + "Name" + "</b></td><td><b>" + toHTML.transliterate((String)alpha.get("Name")) + "</b></td></tr>");
		alpha.remove("Name");
		for (Iterator it = alpha.keySet().iterator(); it.hasNext();) {
			String propName = (String) it.next();
			String propValue = (String) alpha.get(propName);
			out.println("<tr><td>" + propName + "</td><td>" + toHTML.transliterate(propValue) + "</td></tr>");
		}
		out.println("</table>");
		
		
	}

	private static void checkCase() {
		System.out.println("Getting Values1");

		UnicodeSet hasFrom = new UnicodeSet();
		UnicodeSet hasTo = new UnicodeSet();
		UnicodeSet isLower = new UnicodeSet();
		UnicodeSet isUpper = new UnicodeSet();
		UnicodeSet isTitle = new UnicodeSet();
		for (int i = 0; i < 0x10FFFF; ++i) {
			String si = UTF16.valueOf(i);
			String xx;
			xx = UCharacter.toLowerCase(si);
			if (si.equals(xx)) {
				isLower.add(i);
			} else {
				hasFrom.add(i);
				hasTo.add(xx);
			}

			xx = UCharacter.toUpperCase(si);
			if (si.equals(xx)) {
				isUpper.add(i);
			} else {
				hasFrom.add(i);
				hasTo.add(xx);
			}

			xx = UCharacter.toTitleCase(si,null);
			if (si.equals(xx)) {
				isTitle.add(i);
			} else {
				hasFrom.add(i);
				hasTo.add(xx);
			}
		}
		
		PrettyPrinter pp = new PrettyPrinter();
		
		showDifferences(pp, "hasFrom", hasFrom, "hasTo", hasTo, "xxx", new UnicodeSet());
		
		System.out.println("Getting Values2");
		isLower.retainAll(hasFrom);
		isUpper.retainAll(hasFrom);
		isTitle.retainAll(hasFrom);
		hasFrom.removeAll(isLower).removeAll(isUpper).removeAll(isTitle);
		UnicodeSet upperAndTitle = new UnicodeSet(isUpper).retainAll(isTitle);
		isUpper.removeAll(upperAndTitle);
		isTitle.removeAll(upperAndTitle);
		
		System.out.println("isLower: " + isLower.size());
		System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(isLower)));
		System.out.println("isUpper (alone): " + isUpper.size());
		System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(isUpper)));
		System.out.println("isTitle (alone): " + isTitle.size());
		System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(isTitle)));
		System.out.println("isUpperAndTitle: " + upperAndTitle.size());
		System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(upperAndTitle)));
		System.out.println("other: " + hasFrom.size());
		System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(hasFrom)));
		
		UnicodeSet LowercaseProperty = new UnicodeSet("[:Lowercase:]");
		UnicodeSet LowercaseCategory = new UnicodeSet("[:Lowercase_Letter:]");
		//System.out.println(pp.toPattern(isLower));
		
		showDifferences(pp, "Lowercase", LowercaseProperty, 
				"Functionally Lowercase", isLower, 
				"Lowercase_Letter", LowercaseCategory);

		UnicodeSet TitlecaseProperty = new UnicodeSet();
		UnicodeSet TitlecaseCategory = new UnicodeSet("[:Titlecase_Letter:]");

		showDifferences(pp, "Titlecase", TitlecaseProperty, 
				"Functionally Titlecase", isTitle, 
				"Titlecase_Letter", TitlecaseCategory);

		UnicodeSet UppercaseProperty = new UnicodeSet("[:Uppercase:]");
		UnicodeSet UppercaseCategory = new UnicodeSet("[:Uppercase_Letter:]");

		showDifferences(pp, "Uppercase", UppercaseProperty, 
				"Functionally Uppercase", new UnicodeSet(isUpper).addAll(upperAndTitle), 
				"Uppercase_Letter", UppercaseCategory);


//		UnicodeMap compare = new UnicodeMap();
//		compare.putAll(isLower,"isLowercase&isCased");
//		
//		compare.composeWith(new UnicodeMap().putAll(LowercaseProperty,"Lowercase"), new MyComposer());
//		compare.composeWith(new UnicodeMap().putAll(LowercaseProperty,"Lowercase_Letter"), new MyComposer());
//		for (Iterator it = compare.getAvailableValues().iterator(); it.hasNext();) {
//			String value = (String) it.next();
//			UnicodeSet chars = compare.getSet(value);
//			System.out.println(value + ", size: " + chars.size());
//			System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(chars)));
//		}
	}

	private static void showDifferences(PrettyPrinter pp, 
			String lowercaseTitle, UnicodeSet LowercaseProperty, 
			String funcLowerTitle, UnicodeSet isLower,
			String lowercaseCatTitle, UnicodeSet LowercaseCategory) {
		System.out.println("Getting Values3");
		UnicodeSet[] categories = new UnicodeSet[8];
		for (int i = 0; i < categories.length; ++i) categories[i] = new UnicodeSet();
		for (int i = 0; i < 0x10FFFF; ++i) {
			int sum = 0;
			if (isLower.contains(i)) sum |= 1;
			if (LowercaseCategory.contains(i)) sum |= 2;
			if (LowercaseProperty.contains(i)) sum |= 4;
			categories[sum].add(i);
		}
		System.out.println("Printing Values");
		for (int i = 1; i < categories.length; ++i) {
			if (categories[i].size() == 0) continue;
			String name = "";
			if ((i & 4) != 0) name += " & " + lowercaseTitle;
			if ((i & 1) != 0) name += " & " + funcLowerTitle;
			if ((i & 2) != 0) name += " & " + lowercaseCatTitle;
			name = name.substring(3); // skip " & "
			System.out.println(name + ", size: " + categories[i].size());
			System.out.println(com.ibm.icu.impl.Utility.escape(pp.toPattern(categories[i])));
		}
	}
	
	static class MyComposer implements UnicodeMap.Composer {

		public Object compose(int codePoint, Object a, Object b) {
			if (a == null) return b;
			if (b == null) return a;
			return a + " & " + b;
		}
		
	}
	
	static Counter bufferTypes = new Counter();
	
	static class BufferData {
    byte starterIsZero;
		int initials;
		int medials;
		int finals;
		int sample;
		public boolean equals(Object other) {
			BufferData that = (BufferData)other;
			return starterIsZero == that.starterIsZero && initials == that.initials && medials == that.medials && finals == that.finals;
		}
		public int hashCode() {
			return ((starterIsZero * 37 + initials)*37 + medials)*37 + finals;
		}
		public BufferData set(int codepoint) {
			String s = Default.nfkd().normalize(codepoint);
			int cp;
      starterIsZero = (byte)(UCharacter.getCombiningClass(codepoint) == 0 ? 0 : 1);
			boolean isInitial = true;
			for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
				cp = UTF16.charAt(s, i);
				int ccc = UCharacter.getCombiningClass(cp);
				if (ccc != 0) {
					if (isInitial) {
						++initials;
					} else {
						++finals;
					}
				} else {
					isInitial = false;
					medials += finals + 1;
					finals = 0;
				}
			}
      if (medials != 0) medials = 1;
			sample = codepoint;
      if (starterIsZero == 0 && medials == 0) {
        System.out.println("WARNING: BAD CHARACTER");
        cp = sample;
        int ccc = UCharacter.getCombiningClass(cp);
        System.out.println("U+" +  Utility.hex(cp) + "\t" + UCharacter.getName(cp) + " (ccc=" + ccc + ")");
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
          cp = UTF16.charAt(s, i);
          ccc = UCharacter.getCombiningClass(cp);
          System.out.println("\tU+" +  Utility.hex(cp) + "\t" + UCharacter.getName(cp) + " (ccc=" + ccc + ")");
        }
      }
			return this;
		}
    public static String getHeader() {
      return "Starter?" + "\t" + "initials" + "\t" + "Contains Starter?" + "\t" + "finals" + "\t"  + "sample hex" + "\t" + "sample name";
    }
		public String toString() {
      String result = (starterIsZero == 0 ? "Y" : "") + "\t" + initials + "\t" + (medials != 0 ? "Y" : "") + "\t" + finals + "\t";
			if (sample == 0) {
				return  result + "-" + "\t" + "all others";
			}
			return result  + Utility.hex(sample) + "\t" + UCharacter.getName(sample);
		}
	}
	static class BufferDataComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			BufferData a0 = (BufferData)arg0;
			BufferData a1 = (BufferData)arg1;
			int result;
      if (0 != (result = a0.starterIsZero - a1.starterIsZero)) return result;
      if (0 != (result = a0.initials - a1.initials)) return result;
			if (0 != (result = a0.finals - a1.finals)) return result;
			if (0 != (result = a0.medials - a1.medials)) return result;
			return 0;
		}
	}
	private static void showLeadingTrailingNonStarters() {
    BufferData non = new BufferData().set(0);
		Tabber tabber = new Tabber.HTMLTabber();
		for (int i = 0; i <= 0x10ffff; ++i) {
			int type = Default.ucd().getCategory(i);
			if (type == UCD.UNASSIGNED || type == UCD.PRIVATE_USE || type == UCD.SURROGATE) {
				bufferTypes.add(non,1);
				continue;
			}
			bufferTypes.add(new BufferData().set(i),1);
		}
		Map m = bufferTypes.getMap();
		TreeSet sorted = new TreeSet(new BufferDataComparator());
		NumberFormat nf = NumberFormat.getInstance();
		sorted.addAll(m.keySet());
    System.out.println(tabber.process("total\t" + BufferData.getHeader()));
		for (Iterator it = sorted.iterator(); it.hasNext();) {
			Object key = it.next();
			Object value = bufferTypes.getCount(key);
			System.out.println(tabber.process(nf.format(value) + "\t" + key));
		}
	}


}