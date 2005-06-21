/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateConfusables.java,v $
* $Date: 2005/06/21 21:28:31 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.transaction.xa.Xid;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodePropertySource;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.text.utility.Utility;
import com.ibm.text.utility.XEquivalenceClass;

public class GenerateConfusables {

	public static void main(String[] args) throws IOException {
		Set arg2 = new HashSet(Arrays.asList(args));
		try {
			if (arg2.contains("-b")) generateIDN();
			if (arg2.contains("-c")) generateConfusables();
			if (arg2.contains("-d")) generateDecompFile();
			if (arg2.contains("-s")) generateSource();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done");
		}
	}
	static PrintWriter log;
	static final String ARROW = "\u2192";
	static ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("");
	static UnicodeSet UNASSIGNED = ups.getSet("gc=Cn")
		.addAll(ups.getSet("gc=Co"))
		.addAll(ups.getSet("gc=Cs"));
	static UnicodeSet skipSet = ups.getSet("gc=Cc")
		.addAll(ups.getSet("gc=Cf"))
		.addAll(UNASSIGNED);
	static UnicodeSet whiteSpace = ups.getSet("Whitespace=TRUE");
	static UnicodeSet _skipNFKD;
	
	static Map gatheredNFKD = new TreeMap();
	static UnicodeMap nfcMap = new UnicodeMap();
	
	static String indir = "C:\\Unicode-CVS2\\draft\\reports\\tr36\\data\\source\\";
	static String outdir = "C:\\Unicode-CVS2\\draft\\reports\\tr36\\data\\";
	
	static Comparator codepointComparator = new UTF16.StringComparator();

	static UnicodeSet setsToAbbreviate = new UnicodeSet("[" +
			"\\u3400-\\u4DB5" +
			"\\u4E00-\\u9FA5" +
			"\\uA000-\\uA48C" +
			"\\uAC00-\\uD7A3" +
			"\\u1100-\\u11FF" +
			"\\uFB00-\\uFEFC" +
			"\\u2460-\\u24FF" +
			"\\u3251-\\u33FF" +
			"\\u4DC0-\\u4DFF" +
			"\\U0001D300-\\U0001D356" +
			"\\U0001D000-\\U0001D1DD" +
			"\\U00020000-\\U0002A6D6" +
			"\\U0001D400-\\U0001D7FF" +
			"[:script=Canadian_Aboriginal:]" +
			"[:script=ETHIOPIC:]" +
				"[:script=Tagalog:]" +
				"[:script=Hanunoo:]" +
				"[:script=Buhid:]" +
				"[:script=Tagbanwa:]" +
				"[:script=Deseret:]" +
				"[:script=Shavian:]" +
				"[:script=Ogham:]" +
				"[:script=Old Italic:]" +
				"[:script=Runic:]" +
				"[:script=Gothic:]" +
				"[:script=Ugaritic:]" +
				"[:script=Linear B:]" +
				"[:script=Cypriot:]" +
				"[:script=Coptic:]" +
				"[:script=Syriac:]" +
				"[:script=Glagolitic:]" +
				"[:script=Glagolitic:]" +
				"[:script=Old Persian:]" +
				"[:script=Kharoshthi:]" +
				"[:script=Osmanya:]" +
				"[:default ignorable code point:]" +
				"]");


/**
 * @throws IOException
	 * 
	 */
	private static void generateIDN() throws IOException {
		UnicodeSet propNFKCSet = ups.getSet("NFKC_QuickCheck=N").complement();
		UnicodeSet propXIDContinueSet = ups.getSet("XID_Continue=TRUE");
		UnicodeSet allocated = ups.getSet("generalcategory=cn").complement();
		
		// get the word chars
		UnicodeMap additions = new UnicodeMap();
		UnicodeMap remap = new UnicodeMap();
		BufferedReader br = BagFormatter.openUTF8Reader(indir, "wordchars.txt");
		String line = null;
		try {
			while (true) {
				line = Utility.readDataLine(br);
				if (line == null) break;
				if (line.length() == 0) continue;
				String[] pieces = Utility.split(line, ';');
				int code = Integer.parseInt(pieces[0].trim(),16);
				if (pieces[1].trim().equals("remap-to")) {
					remap.put(code, UTF16.valueOf(Integer.parseInt(pieces[2].trim(),16)));
				} else {
					if (XIDContinueSet.contains(code)) {
						System.out.println("Already in XID continue: " + line);
						continue;
					}
					additions.put(code, "addition");
				}
			}
		} catch (Exception e) {
			throw (RuntimeException) new RuntimeException("Failure on line " + line).initCause(e);
		}
		br.close();
		UnicodeSet xidPlus = new UnicodeSet(propXIDContinueSet)
		.addAll(additions.getSet(null).complement())
		.retainAll(propNFKCSet);
		
		// get all the removals.
		UnicodeMap removals = new UnicodeMap();
		br = BagFormatter.openUTF8Reader(indir, "removals.txt");
		UnicodeSet sources = new UnicodeSet();
		line = null;
		try {
			while (true) {
				line = Utility.readDataLine(br);
				if (line == null) break;
				if (line.length() == 0) continue;
				sources.clear();
				String[] pieces = Utility.split(line, ';');
				if (pieces.length < 2) {
					System.out.println("Missing line " + line);
					continue;
				}
				String codelist = pieces[0].trim();
				String reasons = pieces[1].trim();
				if (pieces[0].startsWith("[")) {
					sources = new UnicodeSet(codelist).retainAll(allocated);
				} else {
					String[] codes = Utility.split(codelist, ' ');
					for (int i = 0; i < codes.length; ++i) {
						if (codes[i].length() == 0) continue;
						String[] range = codes[i].split("\\.\\.");
						int start = Integer.parseInt(range[0], 16);
						int end = start;
						if (range.length > 1) end = Integer.parseInt(range[1],16);
						sources.add(start, end);
					}
				}
				removals.putAll(sources, PROHIBITED + reasons);
			}
		} catch (Exception e) {
			throw (RuntimeException) new RuntimeException("Failure on line " + line).initCause(e);
		}
		br.close();
		getIdentifierSet();
		UnicodeSet notInXID = new UnicodeSet(IDNOutputSet).removeAll(xidPlus);
		removals.putAll(notInXID, PROHIBITED + "not in XID+");
		UnicodeSet removalSet = removals.getSet(null).complement();

		printIDNStuff(true, additions, remap, removals, removalSet);
		//printIDNStuff("new-idnchars-full.txt", false, additions, removals, removalSet);
	}

	static final String PROHIBITED = "prohibited ; ";
	/**
 * 
 */
private static void printIDNStuff(boolean mergeRanges, UnicodeMap additions, UnicodeMap remap, UnicodeMap removals, UnicodeSet removalSet) throws IOException {
	BagFormatter bf = new BagFormatter();
	UnicodeProperty scriptProp = ups.getProperty("Script");
	bf.setUnicodePropertyFactory(ups);
//		bf.setLabelSource(scriptProp);
	bf.setLabelSource(null);
	bf.setShowLiteral(bf.toHTMLControl);
	bf.setMergeRanges(mergeRanges);

	PrintWriter out = BagFormatter.openUTF8Writer(outdir, "xidmodifications.txt");

	out.println("# Security Profile for General Identifiers");
	out.println("# $Revision: 1.2 $");
	out.println("# $Date: 2005/06/21 21:28:31 $");
	out.println("");

	out.println("# Characters added");
	out.println("");
	bf.setValueSource("addition");
	bf.showSetNames(out, additions.getSet(null).complement());	

	showRemapped(out, "Characters remapped on input", remap);
	
	out.println("");
	out.println("# Characters removed");
	out.println("");
	Set values = new TreeSet(removals.getAvailableValues());
	/*
	for (Iterator it = values.iterator(); it.hasNext();) {
		String reason1 = (String)it.next();
		bf.setValueSource(reason1);
		out.println("");
		bf.showSetNames(out, removals.getSet(reason1));	
	}
	*/
	bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {})
		.set(removals)
		.setMain("Removals", "GCB", UnicodeProperty.ENUMERATED, "1.0"));
	bf.showSetNames(out, removalSet);
	
	out.close();
	out = BagFormatter.openUTF8Writer(outdir, "idnchars.txt");

	out.println("# Recommended Identifier Profiles for IDN");
	out.println("# $Revision: 1.2 $");
	out.println("# $Date: 2005/06/21 21:28:31 $");
	out.println("");

	showRemapped(out, "Characters remapped on input (strict & lenient)", remap);
	
	out.println("");
	out.println("# Output Characters (strict & lenient)");
	out.println("");
	UnicodeSet remainingOutputSet = new UnicodeSet(IDNOutputSet).removeAll(removalSet);
	bf.setValueSource("output");
	bf.showSetNames(out, remainingOutputSet);
	
	out.println("");
	UnicodeSet remainingInputSet1 = new UnicodeSet(IDNInputSet)
		.removeAll(removalSet)
		.removeAll(remainingOutputSet);
	UnicodeSet remainingInputSet = new UnicodeSet();
	UnicodeSet specialRemove = new UnicodeSet();
	// remove any others that don't normalize/case fold to something in the output set
	for (UnicodeSetIterator usi = new UnicodeSetIterator(remainingInputSet1); usi.next();) {
		String nss = Default.nfkc().normalize(usi.getString());
		String cf = Default.ucd().getCase(nss, UCD.FULL, UCD.FOLD);
		String cf2 = Default.nfkc().normalize(cf);
		if (remainingOutputSet.containsAll(cf2)) remainingInputSet.add(usi.codepoint);
		else specialRemove.add(usi.codepoint);
	}
	// filter out the items that are case foldings of items in output
	UnicodeSet inputSet_strict = new UnicodeSet();
	for (UnicodeSetIterator usi = new UnicodeSetIterator(remainingInputSet); usi.next();) {
		String ss = usi.getString();
		String nss = Default.nfkc().normalize(ss);
		String cf = Default.ucd().getCase(ss, UCD.FULL, UCD.FOLD);
		if (usi.codepoint == 0x2126 || usi.codepoint == 0x212B) {
			System.out.println("check");
		}
		//> > 2126          ; retained-input-only-CF #      (?)  OHM SIGN
		//> > 212B          ; retained-input-only-CF #      (?)  ANGSTROM SIGN

		if (!remainingOutputSet.containsAll(nss)
				&& remainingOutputSet.containsAll(cf)
				) inputSet_strict.add(ss);
	}
	// hack
	inputSet_strict.remove(0x03F4).remove(0x2126).remove(0x212B);
	
	out.println("");
	out.println("# Input Characters (strict & lenient)");
	out.println("");
	bf.setValueSource("input");
	bf.showSetNames(out, inputSet_strict);

	out.println("");
	out.println("# Input Characters (lenient)");
	out.println("");
	bf.setValueSource("input-lenient");
	UnicodeSet inputSet_lenient = new UnicodeSet(remainingInputSet).removeAll(inputSet_strict);
	bf.showSetNames(out, inputSet_lenient);
	
	out.println("");
	out.println("# Not allowed at start of identifier (input & output, strict & lenient)");
	out.println("");
	UnicodeSet nonstarting = new UnicodeSet(remainingOutputSet)
	.addAll(remainingInputSet)
	.retainAll(new UnicodeSet("[:M:]"));
	bf.setValueSource("nonstarting");
	bf.showSetNames(out, nonstarting);

	out.close();
	out = BagFormatter.openUTF8Writer(outdir, "review.txt");
	UnicodeMap reviews = new UnicodeMap().putAll(removals);
	reviews.putAll(remainingOutputSet, "output");
	reviews.putAll(inputSet_strict, "input");
	reviews.putAll(inputSet_lenient, "input-lenient");
	reviews.putAll(specialRemove, PROHIBITED + "output-disallowed");
	UnicodeMap nonstartingmap = new UnicodeMap().putAll(nonstarting, "nonstarting");
	UnicodeMap.Composer composer = new UnicodeMap.Composer() {
		public Object compose(Object a, Object b) {
			if (a == null) return b;
			else if (b == null) return a;
			else return a.toString() + "-" + b.toString();
		}		
	};
	reviews.composeWith(nonstartingmap, composer);
	reviews.putAll(new UnicodeSet(IDNInputSet).complement(), "");
	UnicodeMap.Composer composer2 = new UnicodeMap.Composer() {
		public Object compose(Object a, Object b) {
			if (b == null) return a;
			return "remap-to-" + Utility.hex(b.toString());
		}		
	};
	reviews.composeWith(remap, composer2);
	//reviews.putAll(UNASSIGNED, "");
	out.print("\uFEFF");
	out.println("# Review List for IDN");
	out.println("# $Revision: 1.2 $");
	out.println("# $Date: 2005/06/21 21:28:31 $");
	out.println("");
	
	UnicodeSet fullSet = reviews.getSet("").complement();
	
	bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {})
			.set(reviews)
			.setMain("Reviews", "GCB", UnicodeProperty.ENUMERATED, "1.0"));
	//bf.setMergeRanges(false);
	
	FakeBreak fakeBreak = new FakeBreak();
	fakeBreak.nobreakSet = setsToAbbreviate;
	bf.setRangeBreakSource(fakeBreak);
	out.println("");
	out.println("# Characters allowed in IDNA");
	out.println("");
	bf.showSetNames(out, new UnicodeSet(fullSet)); // .removeAll(bigSets)
	//bf.setMergeRanges(true);
//	out.println("");
//	out.println("# Large Ranges");
//	out.println("");
//	bf.showSetNames(out, new UnicodeSet(fullSet).retainAll(bigSets));
	out.println("");
	out.println("# Characters disallowed in IDNA");
	out.println("# The IDNA spec doesn't allow any of these characters,");
	out.println("# so don't report any of them as being missing from the above list.");
	out.println("# Some possible future additions, once IDNA updates to Unicode 4.1, are given.");
	out.println("");
	//bf.setRangeBreakSource(UnicodeLabel.NULL);
	removals.putAll(ups.getSet("XID_Continue=TRUE").complement(), PROHIBITED + "not in XID+");
	removals.setMissing("future?");
	bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {})
			.set(removals)
			.setMain("Removals", "GCB", UnicodeProperty.ENUMERATED, "1.0"));
	//bf.setValueSource(UnicodeLabel.NULL);
	bf.showSetNames(out, new UnicodeSet(IDNInputSet).complement().removeAll(UNASSIGNED));
	out.close();
	generateDecompFile();
}

	/**
	 * 
	 */
	private static void generateDecompFile() throws IOException {
		PrintWriter out = BagFormatter.openUTF8Writer(outdir, "decomps.txt");
		UnicodeProperty dt = ups.getProperty("Decomposition_Type");
		for (Iterator it = dt.getAvailableValues().iterator(); it.hasNext();) {
			String value = (String) it.next();
			if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("canonical")) continue;
			UnicodeSet s = dt.getSet(value);
			out.println("");
			out.println("# Decomposition_Type = " + value);
			out.println("");
			for (UnicodeSetIterator usi = new UnicodeSetIterator(s); usi.next();) {
				String source = usi.getString();
				String target = Default.nfkc().normalize(source);
				writeSourceTargetLine(out, source, null, target, value);
			}
			//bf.showSetNames(out, s);
			out.flush();
		}
		out.close();
	}

	static class FakeBreak extends UnicodeLabel {
		UnicodeSet nobreakSet;
		public String getValue(int codepoint, boolean isShort) {
			return nobreakSet.contains(codepoint) ? ""
			 : (codepoint & 1) == 0 ? "O"
			 : "E";
		}
	}

	/**
	 * 
	 */
	private static void showRemapped(PrintWriter out, String title, UnicodeMap remap) {
		out.println("");
		out.println("# " + title);
		out.println("");
		int count = 0;
		for (UnicodeSetIterator usi = new UnicodeSetIterator(remap.getSet(null).complement()); usi.next();) {
			writeSourceTargetLine(out, usi.getString(), "remap-to", (String)remap.getValue(usi.codepoint), null);
			count++;
		}
		out.println("");
		out.println("# Total code points: " + count);
	}
	/**
	 * 
	 */
	static UnicodeSet XIDContinueSet = new UnicodeSet("[:XID_Continue:]");
	private static UnicodeSet IDNOutputSet, IDNInputSet, _preferredIDSet;
	
	static UnicodeSet getIdentifierSet() {
		if (_preferredIDSet == null) {
			IDNOutputSet = new UnicodeSet();
			IDNInputSet = new UnicodeSet();
			IDNOutputSet.add('-'); // HACK
			IDNInputSet.add('-');
			for (int cp = 0; cp <= 0x10FFFF; ++cp) {
				Utility.dot(cp);
				int cat = Default.ucd().getCategory(cp);
				if (cat == UCD.Cn || cat == UCD.Co || cat == UCD.Cs) continue;
				// get IDNA
				int idnaType = GenerateStringPrep.getIDNAType(cp);
				if (idnaType == GenerateStringPrep.OK) IDNOutputSet.add(cp);
				if (idnaType != GenerateStringPrep.ILLEGAL) IDNInputSet.add(cp);
			}
			_preferredIDSet = new UnicodeSet(IDNOutputSet).addAll(XIDContinueSet);
		}
		_preferredIDSet.add(0x2018).add(0x2019);
		return _preferredIDSet;
	}
	
	private static UnicodeSet getSkipNFKD() {
		if (_skipNFKD == null) {
			_skipNFKD = new UnicodeSet();
			UnicodeSet idSet = getIdentifierSet();
			for (int cp = 0; cp <= 0x10FFFF; ++cp) {
				Utility.dot(cp);
				int cat = Default.ucd().getCategory(cp);
				if (cat == UCD.Cn || cat == UCD.Co || cat == UCD.Cs) continue;
				int decompType = Default.ucd().getDecompositionType(cp);
				if (decompType == UCD.COMPAT_CIRCLE
						|| decompType == UCD.COMPAT_SUPER
						|| decompType == UCD.COMPAT_SUB
						|| decompType == UCD.COMPAT_VERTICAL
						|| decompType == UCD.COMPAT_SMALL
						|| decompType == UCD.COMPAT_SQUARE
						|| decompType == UCD.COMPAT_FRACTION) {
					_skipNFKD.add(cp);
					continue;
				}
				String mapped = Default.nfkd().normalize(cp);
				if (mapped.equals(UTF16.valueOf(cp))) continue;
				if (idSet.contains(cp) && !idSet.contains(mapped)) _skipNFKD.add(cp);
				else if (!whiteSpace.contains(cp) && whiteSpace.containsSome(mapped)) _skipNFKD.add(cp);
				if (decompType == UCD.CANONICAL) nfcMap.put(cp, Default.nfd().normalize(cp));
			}
		}
		nfcMap.setMissing("");
		return _skipNFKD;
	}
	
	private static boolean isMixedScript(String source) {
		int lastScript = UScript.INVALID_CODE;
		int cp;
		for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
			cp = UTF16.charAt(source, i);
			int script = UScript.getScript(cp);
			if (script == UScript.COMMON || script == UScript.INHERITED) {
				if (XIDContinueSet.contains(cp)) continue; // skip if not identifier
				script = UScript.COMMON;
			}
			if (lastScript == UScript.INVALID_CODE) lastScript = script;
			else if (script != lastScript) return true;
		}
		return false;
	}

	/**
	 * 
	 */
	private static void generateConfusables() throws IOException {
		log = BagFormatter.openUTF8Writer(outdir, "log.txt");
		//fixMichel(indir, outdir);
		generateConfusables(indir, outdir);
		log.close();
		if (false) for (Iterator it = gatheredNFKD.keySet().iterator(); it.hasNext();) {
			String source = (String)it.next();
			System.out.println(Default.ucd().getCodeAndName(source) 
					+ " => " + Default.ucd().getCodeAndName((String)gatheredNFKD.get(source)));
		}
	}

	/*	static class Data2 {
		String source;
		String target;
		int count;
		Data2(String target, int count) {
			this.target = target;
			this.count = count;
		}
	}
*/	
/*	static class Data implements Comparable {
		String source;
		String target;
		String type;
		Data(String source, String target, String type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
		public int compareTo(Object o) {
			int result;
			Data that = (Data)o;
			if (0 != (result = target.compareTo(that.target))) return result;
			if (0 != (result = source.compareTo(that.source))) return result;
			if (0 != (result = type.compareTo(that.type))) return result;
			return 0;
		}
	}
*/	
	
	/**
	 * 
	 */
	static void writeSourceTargetLine(PrintWriter out, String source, String tag, String target, String reason) {
		out.print(
				Utility.hex(source)
				+ " ;\t" + Utility.hex(target)
				+ (tag == null ? "" : " ;\t" + tag)
				//+ " ;\t" + (preferredID.contains(source) ? "ID" : "")
				+ "\t# "
				+ "( " + source + " " + ARROW + " " + target + ") " 
				+ Default.ucd().getName(source) + " " + ARROW + " "
				+ Default.ucd().getName(target)
				);
		if (reason != null) out.print("\t# " + reason);
		out.println();
	}

	static UnicodeSet controls = new UnicodeSet("[:Cc:]");
	
	static class MyEquivalenceClass extends XEquivalenceClass {
		public MyEquivalenceClass() {
			super(preferredTarget,"NONE");
		}
		public boolean addCheck(String a, String b, String reason) {
			// quick check for illegal containment, before changing object
			if (checkForBad(a, b, reason) || checkForBad(b, a, reason)) {
				return false;
			}
			super.add(a, b, reason);
			// full check for any resulting illegal containment.
			// illegal if for any x, y, x is a proper superstring of y
			Set equivalences = getEquivalences(a);
			for (Iterator it = equivalences.iterator(); it.hasNext();) {
				String x = (String)it.next();
				if (!UTF16.hasMoreCodePointsThan(x,1)) continue;
				for (Iterator it2 = equivalences.iterator(); it2.hasNext();) {
					String y = (String)it2.next();
					if (x.equals(y)) continue;
					if (x.indexOf(y) >= 0) throw new RuntimeException("Illegal containment: "
							+ Default.ucd().getCodeAndName(x) + " contains "
							+ Default.ucd().getCodeAndName(y) + " because "
							+ Default.ucd().getCodeAndName(a) + " ~ "
							+ Default.ucd().getCodeAndName(b) + " because of "
							+ reason);
				}
			}
			return true;
		}
		
		/**
		 * 
		 */
		private boolean checkForBad(String a, String b, String reason) {
			Set equivalences = getEquivalences(b);
			for (Iterator it = equivalences.iterator(); it.hasNext();) {
				String b2 = (String)it.next();
				if (a.equals(b2)) continue;
				if (b2.indexOf(a) >= 0 || a.indexOf(b2) >= 0) {
					log.println("Illegal containment: "
							+ Default.ucd().getCodeAndName(a)
							+ " overlaps "
							+ Default.ucd().getCodeAndName(b2)
							+ "\r\n\tfrom "
							+ Default.ucd().getCodeAndName(b)
							+ "\r\n\twith reason "
							+ reason + " plus "
							+ getReasons(b2, b));
					return true;
				}
			}
			return false;
		}
		
		public XEquivalenceClass add(Object a1, Object b1, String reason) {
			String a = (String)a1;
			String b = (String)b1;
			try {
				addCheck(a, b, reason);
				return this;
			} catch (RuntimeException e) {
				throw (RuntimeException) new RuntimeException("Failure adding "
						+ Default.ucd().getCodeAndName(a) + "; "
						+ Default.ucd().getCodeAndName(b)
						+ "; " + reason).initCause(e);
			}
		}
		/**
		 * Only NFKD if the result doesn't cross from ID set to nonID set, and space is not added
		 */
//		private String specialNFKD(String item) {
//			UnicodeSet skipSet = getSkipNFKD();
//			StringBuffer result = new StringBuffer();
//			int cp;
//			for (int i = 0; i < item.length(); i += UTF16.getCharCount(cp)) {
//				cp = UTF16.charAt(item, i);
//				if (skipSet.contains(cp)) {
//					UTF16.append(result, cp);
//					continue;
//				}
//				String cps = UTF16.valueOf(cp);
//				String mapped = Default.nfkd().normalize(cps);
//				if (cps.equals(mapped)) {
//					UTF16.append(result, cp);
//					continue;
//				}
//				result.append(mapped);
//				gatheredNFKD.put(cps, mapped);
//			}
//			return result.toString();
//		}
		
		public void close(String reason) {
			boolean addedItem;
			StringBuffer reasons = new StringBuffer();
			do {
				addedItem = false;
				Set cloneForSafety = new TreeSet(getExplicitItems());
				for (Iterator it = cloneForSafety.iterator(); it.hasNext();) {
					String item = (String) it.next();
					if (!UTF16.hasMoreCodePointsThan(item,1)) continue; // just for speed
					reasons.setLength(0);
					String mapped = mapString(item, reasons);
					if (!isEquivalent(item, mapped)) {
						if (addCheck(item, mapped, reasons.toString())) {
							// System.out.println("Closing: " + Default.ucd().getCodeAndName(item) + " => " + Default.ucd().getCodeAndName(mapped));
							addedItem = true;
						}
					}
				}
			} while (addedItem);
		}
		
		/**
		 * 
		 */
		private String mapString(String item, StringBuffer reasons) {
			if (false && item.startsWith("\u03D2")) {
				System.out.println("foo");
			}
			StringBuffer result = new StringBuffer();
			int cp;
			for (int i = 0; i < item.length(); i += UTF16.getCharCount(cp)) {
				cp = UTF16.charAt(item, i);
				String cps = UTF16.valueOf(cp);
				String mapped = (String) getParadigm(cps);
				if (mapped.indexOf(cps) >= 0) result.append(cps);
				else {
					result.append(mapped);
					reasons.append("[" + getReasons(cps, mapped) + "]");
				}
			}
			return result.toString();
		}
		/**
		 * 
		 */
		public void writeSource(PrintWriter out) {
			Set items = new TreeSet(getParadigmComparator().setSourceComparator(codepointComparator));
			items.addAll(getExplicitItems());
			for (Iterator it = items.iterator(); it.hasNext();) {
				String item = (String) it.next();
				String paradigm = (String) getParadigm(item);
				if (item.equals(paradigm)) continue;
				writeSourceTargetLine(out, item, null, paradigm, null);
			}
		}
	}
	
	static class DataSet {
		MyEquivalenceClass dataMixedLowercase = new MyEquivalenceClass();
		MyEquivalenceClass dataMixedAnycase = new MyEquivalenceClass();
		MyEquivalenceClass dataSingleLowercase = new MyEquivalenceClass();
		MyEquivalenceClass dataSingleAnycase = new MyEquivalenceClass();
		
		public DataSet add(String source, String target, String type, int lineCount, String errorLine) {
			if (skipSet.containsAll(source) || skipSet.containsAll(target)) return this;
			String nsource = Default.nfd().normalize(source);
			String ntarget = Default.nfd().normalize(target);
			
			// if it is just a compatibility match, return
			//if (nsource.equals(ntarget)) return this;
			if (type.indexOf("skip") >= 0) return this;
			if (target.indexOf('\u203D') >= 0) return this;
			
			type = getReasonFromFilename(type);

			// if it is base + combining sequence => base2 + same combining sequence, do just the base
			int nsourceFirst = UTF16.charAt(nsource,0);
			String nsourceRest = nsource.substring(UTF16.getCharCount(nsourceFirst));
			int ntargetFirst = UTF16.charAt(ntarget,0);
			String ntargetRest = ntarget.substring(UTF16.getCharCount(ntargetFirst));
			
			if (nsourceRest.length() != 0 && nsourceRest.equals(ntargetRest)) {
				source = UTF16.valueOf(nsourceFirst);
				target = UTF16.valueOf(ntargetFirst);
				type += "-base";
			}
			type += ":" + lineCount;
			
			String combined = source + target;
			boolean isLowercase = combined.equals(Default.ucd().getCase(combined, UCD.FULL, UCD.FOLD));
			boolean isMixed = isMixedScript(combined);
			dataMixedAnycase.add(source, target, type);
			if (isLowercase) dataMixedLowercase.add(source, target, type);
			if (!isMixed) dataSingleAnycase.add(source, target, type);
			if (!isMixed && isLowercase) dataSingleLowercase.add(source, target, type);
			return this;
		}
		
/*		*//**
		 * @param errorLine TODO
		 * 
		 *//*
		private DataSet add(Data newData, String errorLine) {
			if (controls.containsSome(newData.source) || controls.containsSome(newData.target)) {
				System.out.println("Problem with " + errorLine);
				System.out.println(getCodeCharName(newData.source) + " => " + getCodeCharName(newData.target));
			}
			String[] key = {newData.source, newData.target};
			Data old = (Data) dataMap.get(key);
			if (old == null) {
				dataSet.add(newData);
				dataMap.put(key, newData);
			}else {
				old.type = old.type + "/" + newData.type;
			}
			return this;
		}
*/		// Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt"
		static final int NORMAL = 0, FOLDING = 1, OLD = 2;
		
		public DataSet addFile(String directory, String filename) throws IOException {
			String line = null;
			int count = 0;
			try {
				BufferedReader in = BagFormatter.openUTF8Reader(directory, filename);
				int kind = NORMAL;
				if (filename.indexOf("Folding") >= 0) kind = FOLDING;
				else if (false && filename.indexOf("-old") >= 0) kind = OLD;
				while (true) {
					count++;
					line = Utility.readDataLine(in);
					if (line == null) break;
					if (line.length() == 0) continue;
					String[] pieces = Utility.split(line,';');
					if (pieces.length < 2) {
						System.out.println("Error on: " + line);
						continue;
					}
					String type = filename;
					if (kind==FOLDING) {
						String source = Utility.fromHex(pieces[0].trim(),true);
						String target = Utility.fromHex(pieces[1].trim(),true);
						String nsource = Default.nfkd().normalize(source);
						String first = UTF16.valueOf(UTF16.charAt(nsource, 0));
						if (!first.equals(target)) {
							add(source, target, type, count, line);
						}
					} else if (kind == OLD) {
						String target = pieces[0].trim();
						for (int i = 1; i < pieces.length; ++i) {
							add(pieces[i].trim(), target, type, count, line);
						}
					} else {
						String source = Utility.fromHex(pieces[0].trim(),true);
						String target = Utility.fromHex(pieces[1].trim(),true);
						//if (pieces.length > 2) type = pieces[2].trim();
						add(source, target, type, count, line);
					}
				}
				in.close();
				return this;
			} catch (Exception e) {
				throw (RuntimeException) new RuntimeException("Failure with file: " 
						+ directory + filename + " on line: " + count
						+ ": " + line).initCause(e);
			}			
		}
		
		public void writeSource(String directory, String filename) throws IOException {
			PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
			out.println("# Source File for IDN Confusables");
			out.println("# $Revision: 1.2 $");
			out.println("# $Date: 2005/06/21 21:28:31 $");
			out.println("");
			dataMixedAnycase.writeSource(out);
			out.close();
		}
		
		public void write(String directory, String filename, boolean appendFile, boolean skipNFKEquivs) throws IOException {
			PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
			out.print('\uFEFF');
			out.println("# Recommended confusable mapping for IDN");
			out.println("# $Revision: 1.2 $");
			out.println("# $Date: 2005/06/21 21:28:31 $");
			out.println("");

			if (appendFile) {
				String[] replacements = {"%date%", Default.getDate()};
				Utility.appendFile("com/ibm/text/UCD/confusablesHeader.txt", 
						Utility.UTF8_WINDOWS, out, replacements);
			}
			writeData(out, dataSingleLowercase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs);
			writeData(out, dataSingleAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs);
			writeData(out, dataMixedLowercase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs);
			writeData(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs);
			out.close();
		}
		/**
		 * @param skipNFKEquivs TODO
		 * 
		 */
		private void writeData(PrintWriter out, XEquivalenceClass data, String tag, String title, boolean skipNFKEquivs) {
			Set items = new TreeSet(data.getParadigmComparator().setSourceComparator(codepointComparator));
			items.addAll(data.getExplicitItems());
			out.println();
			out.println("# " + title);
			out.println();
			int count = 0;
			UnicodeSet preferredID = getIdentifierSet();
			for (Iterator it = items.iterator(); it.hasNext();) {
				String source = (String) it.next();
				if (UTF16.hasMoreCodePointsThan(source,1)) continue;
				String target = (String) data.getParadigm(source);
				if (source.equals(target)) continue;
				if (skipNFKEquivs) {
					if (!Default.nfkd().normalize(source).equals(source)) continue;
				}
				String reason = fixReason(data.getReasons(source, target));
				writeSourceTargetLine(out, source, tag, target, reason);
				count++;
			}
			out.println();
			out.println("# total for (" + tag + "): " + count);
			out.println();
		}

		/**
		 * 
		 */
		private String fixReason(List reasons) {
			List first = (List)reasons.get(0);
			String result = "";
			for (int i = 0; i < first.size(); ++i) {
				if (i != 0) result += " ";
				Object item = first.get(i);
				if (item instanceof String) {
					result += item;
				} else {
					String temp = "";
					for (Iterator it = ((Set)item).iterator(); it.hasNext();) {
						if (temp.length() != 0) temp += "|";
						temp += it.next();
					}
					result += "{" + temp + "}";
				}
			}
			return result.toString();
		}
		
		public void addAll(DataSet ds) {
			dataMixedAnycase.addAll(ds.dataMixedAnycase);
			dataMixedLowercase.addAll(ds.dataMixedLowercase);
			dataSingleAnycase.addAll(ds.dataSingleAnycase);
			dataSingleLowercase.addAll(ds.dataSingleLowercase);
		}
		/*		*//**
		 * 
		 *//*
		public DataSet clean() {
			// remove all skips
			DataSet tempSet = new DataSet();
			Map m = new HashMap();
			for (Iterator it = dataSet.iterator(); it.hasNext();) {
				Data d = (Data) it.next();
				if (d.type.indexOf("skip") >= 0) continue;
				String newTarget = Default.nfkd().normalize(d.target);
				String newSource = Default.nfkd().normalize(d.source);
				String type = d.type;
				if (!d.target.equals(newTarget) || !d.source.equals(newSource)) {
					type += "-nf";
					log.println("Norm:\t" + getCodeCharName(d.source) + " " + ARROW + " " + getCodeCharName(newSource));
					log.println("\t" + getCodeCharName(d.target) + " " + ARROW + " " + getCodeCharName(newTarget) + " \t" + type);
					continue;
				}
				// swap order
				if (preferSecondAsSource(newSource, newTarget)) {
					String temp = newTarget;
					newTarget = newSource;
					newSource = temp;
				}

				Data already = (Data) m.get(newSource);
				if (already != null && !newTarget.equals(already.target)) {
					log.println("X " + getCodeCharName(newSource) + " " + ARROW);
					log.println("\t" + getCodeCharName(newTarget) + " \t" + type);
					log.println("\t" + getCodeCharName(already.target) + " \t" + already.type);
					if (preferSecondAsSource(already.target, newTarget)) {
						// just fix new guy
						type += "[" + newSource + "]" + already.type;
						newSource = newTarget;
						newTarget = already.target;
					} else {
						// need to fix new guy, AND fix old guy.
						tempSet.remove(already);
						type += "[" + newSource + "]" + already.type;
						newSource = already.target;
						already.type += "[" + already.target + "]" + type;
						already.target = newTarget;
						tempSet.add(already, "");
					}
				}
				Data newData = new Data(newSource, newTarget, type);
				m.put(newSource, newData);
				tempSet.add(newData, "");
			}
			// now recursively apply
			DataSet s = new DataSet();
			for (Iterator it = tempSet.dataSet.iterator(); it.hasNext();) {
				Data d = (Data) it.next();
				int cp = 0;
				StringBuffer result = new StringBuffer();
				for (int i = 0; i < d.target.length(); i += UTF16.getCharCount(cp)) {
					cp = UTF16.charAt(d.target, i);
					String src = UTF16.valueOf(cp);
					while (true) {
						Data rep = (Data) m.get(src);
						if (rep == null) break;
						src = rep.target;
					}
					result.append(src);
				}
				String newTarget = result.toString();
				newTarget = Default.nfkd().normalize(newTarget);
				s.add(d.source, newTarget, d.type + (newTarget.equals(newTarget) ? "" : "-rec"), "");
			}
			return s;
		}
		*//**
		 * 
		 *//*
		private void remove(Data already) {
			String[] key = {already.source, already.target};
			dataMap.remove(key);
			dataSet.remove(already);
		}*/
		/**
		 * 
		 */
		public void close(String reason) {
			dataMixedAnycase.close(reason);
			dataMixedLowercase.close(reason);
			dataSingleAnycase.close(reason);
			dataSingleLowercase.close(reason);
		}
		/**
		 * 
		 */
		public void addUnicodeMap(UnicodeMap decompMap, String type, String errorLine) {
			int count = 0;
			for (UnicodeSetIterator it = new UnicodeSetIterator(decompMap.getSet(null).complement()); it.next(); ) {
				add(it.getString(), (String)decompMap.getValue(it.codepoint), type, ++count, errorLine);
			}
		}

		/**
		 * @throws IOException
		 * 
		 */
		public void writeSummary(String outdir, String string, boolean b, boolean c) throws IOException {
			PrintWriter out = BagFormatter.openUTF8Writer(outdir, string);
			out.print('\uFEFF');
			out.println("# Summary: Recommended confusable mapping for IDN");
			out.println("# $Revision: 1.2 $");
			out.println("# $Date: 2005/06/21 21:28:31 $");
			out.println("");
			MyEquivalenceClass data = dataMixedAnycase;
			Set items = new TreeSet(data.getParadigmComparator().setSourceComparator(codepointComparator));
			items.addAll(data.getExplicitItems());
			int count = 0;
			UnicodeSet preferredID = getIdentifierSet();
			String lastTarget = "";
			for (Iterator it = items.iterator(); it.hasNext();) {
				String source = (String) it.next();
				if (UTF16.hasMoreCodePointsThan(source,1)) continue;
				String target = (String) data.getParadigm(source);
				if (source.equals(target)) continue;
				boolean compatEqual = Default.nfkd().normalize(source).equals(Default.nfkd().normalize(target));
				if (compatEqual) continue;
				if (!target.equals(lastTarget)) {
					lastTarget = target;
					out.println("(\u200E " + target + " \u200E)\t" + Utility.hex(target) + "\t " + Default.ucd().getName(target));
				}
				String reason = fixReason(data.getReasons(source, target));
				if (compatEqual) {
					out.print("\u21D0");
				} else {
					out.print("\u2190");
				}
				out.println("\t" + "(\u200E " + source + " \u200E)\t" + Utility.hex(source) + "\t " + Default.ucd().getName(source)
						+ "\t# " + reason);
				count++;
			}
			out.println();
			out.println("# total : " + count);
			out.println();

			out.close();
		}
	}
		/**
		 * @throws IOException
	 * 
	 */
	private static void fixMichel(String indir, String outdir) throws IOException {
		BufferedReader in = BagFormatter.openUTF8Reader(indir + "michel/", "tr36comments-annex.txt");
		PrintWriter out = BagFormatter.openUTF8Writer(outdir, "new-tr36comments-annex.txt");
		while (true) {
			String line = Utility.readDataLine(in);
			if (line == null) break;
			String[] pieces = Utility.split(line,'\t');
			if (pieces.length < 2) {
				out.println(line);
				continue;
			}
			String source = Utility.fromHex(pieces[0].trim());
			if (Default.nfkd().isNormalized(source)) {
				out.println(line);
			}
		}
		in.close();
		out.close();
	}
		/**
	 * 
	 */
	
	private static void generateSource() throws IOException {
		File dir = new File(indir);
		String[] names = dir.list();
		Set sources = new TreeSet(new ArrayComparator(
				new Comparator[] {codepointComparator, codepointComparator}));
		
		int[] count = new int[1];
		for (int i = 0; i < names.length; ++i) {
			if (new File(indir + names[i]).isDirectory()) continue;
			if (!names[i].startsWith("confusables")) continue;
			String reason = getReasonFromFilename(names[i]);
			System.out.println(names[i]);
			BufferedReader in = BagFormatter.openUTF8Reader(indir, names[i]);
			String line;
			count[0] = 0;
			while (true) {
				line = Utility.readDataLine(in, count);
				if (line == null) break;
				if (line.length() == 0) continue;
				String[] pieces = Utility.split(line,';');
				if (pieces.length < 2) {
					System.out.println("Error on: " + line);
					continue;
				}
				String source = Utility.fromHex(pieces[0].trim(),true);
				String target = Utility.fromHex(pieces[1].trim(),true);
				
				if (source.length() == 0 || target.length() == 0) {
					throw new IllegalArgumentException("zero-length item: " + count[0] + ":\t" + line);
				}
				
				// check for identical combining sequences
				String nsource = Default.nfc().normalize(source);
				String ntarget = Default.nfc().normalize(target);
				if (nsource.equals(ntarget)) continue;
				
				if (true) {
					int nsourceFirst = UTF16.charAt(nsource,0);
					String nsourceRest = nsource.substring(UTF16.getCharCount(nsourceFirst));
					int ntargetFirst = UTF16.charAt(ntarget,0);
					String ntargetRest = ntarget.substring(UTF16.getCharCount(ntargetFirst));
					if (nsourceRest.equals(ntargetRest)) {
						source = UTF16.valueOf(nsourceFirst);
						target = UTF16.valueOf(ntargetFirst);
					}
				}
				
				if (preferredTarget.compare(source, target) < 0) {
					String temp = source;
					source = target;
					target = temp;
				}
				sources.add(new String[] {source, target});
			}
			in.close();
		}
		PrintWriter out = BagFormatter.openUTF8Writer(outdir, "confusableSource.txt");
		for (Iterator it = sources.iterator(); it.hasNext();) {
			String[] sourceItem = (String[]) it.next();
			writeSourceTargetLine(out, sourceItem[0], null, sourceItem[1], null);
		}
		out.close();
	}
	
	private static void generateConfusables(String indir, String outdir) throws IOException {
		File dir = new File(indir);
		String[] names = dir.list();
		DataSet total = new DataSet();
		for (int i = 0; i < names.length; ++i) {
			if (new File(indir + names[i]).isDirectory()) continue;
			if (!names[i].startsWith("confusables")) continue;
			System.out.println(names[i]);
			DataSet ds = new DataSet();
			ds.addFile(indir, names[i]);
			ds.writeSource(outdir, "new-" + names[i]);
			ds.close("*");
			total.addAll(ds);
			total.close("t*" + names[i]);
		}
		DataSet ds = new DataSet();
		ds.addUnicodeMap(nfcMap, "nfc", "nfc");
		ds.close("*");
		ds.write(outdir, "new-decomp.txt", false, false);
		total.addAll(ds);
		total.close("*");
		total.writeSummary(outdir, "confusablesSummary.txt", false, false);
		total.write(outdir, "confusables.txt", false, false);
		//DataSet clean = total.clean();
		//clean.write(outdir, "confusables.txt", true);		
	}
	/*
		BufferedReader in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt");
		Set set = new TreeSet(new ArrayComparator(new Comparator[] {new UTF16.StringComparator(), 
				new UTF16.StringComparator()}));
		while (true) {
			String line = Utility.readDataLine(in);
			if (line == null) break;
			if (line.length() == 0) continue;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 2) {
				System.out.println("Error on: " + line);
				continue;
			}
			String source = Utility.fromHex(pieces[0].trim());
			String target = Utility.fromHex(pieces[1].trim());
			String nsource = Default.nfkd().normalize(source);
			String first = UTF16.valueOf(UTF16.charAt(nsource, 0));
			if (!first.equals(target)) {
				set.add(new String[]{source, target});
			}
		}
		in.close();

	}
	public static void gen() throws IOException {
		Map m = new TreeMap();
		BufferedReader in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables.txt");
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 3) {
				System.out.println("Error on: " + line);
				continue;
			}
			int codepoint = Integer.parseInt(pieces[1], 16);
			int cat = Default.ucd().getCategory(codepoint);
			if (cat == UCD_Types.Co || cat == UCD_Types.Cn) continue; // skip private use
			if (!Default.nfkd().isNormalized(codepoint)) continue; //skip non NFKC
			String result = Utility.fromHex(pieces[0]);
			if (!Default.nfkd().isNormalized(result)) continue; //skip non NFKC
			int count = Integer.parseInt(pieces[2]);
			String source = UTF16.valueOf(codepoint);
			add(m, source, result, count);
		}
		in.close();

		in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables2.txt");
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			line = line.trim();
			int pos = line.indexOf("#");
			if (pos >= 0) line = line.substring(0,pos).trim();
			if (line.length() == 0) continue;
			if (line.startsWith("@")) continue;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 2) {
				System.out.println("Error on: " + line);
				continue;
			}
			String source = pieces[0].trim();
			for (int i = 1; i < pieces.length; ++i) {
				add(m, source, pieces[i].trim(), -1);
			}
		}
		in.close();

		boolean gotOne;
		// close the set
		do {
			gotOne = false;
			for (Iterator it = m.keySet().iterator(); it.hasNext();) {
				String source = (String) it.next();
				Data2 data = (Data2) m.get(source);
				Data2 data2 = (Data2) m.get(data.target);
				if (data2 == null) continue;
				data.target = data2.target;
				gotOne = true;
				break;
			}
		} while (gotOne);
		// put into different sorting order
		Set s = new TreeSet();
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String source = (String) it.next();
			Data2 data = (Data2) m.get(source);
			s.add(new Data(source, data.target, data.count));
		}
		// write it out
		PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIR, "confusables.txt");
		String[] replacements = {"%date%", Default.getDate()};
		Utility.appendFile("com/ibm/text/UCD/confusablesHeader.txt", 
				Utility.UTF8_WINDOWS, out, replacements);
		for (Iterator it = s.iterator(); it.hasNext();) {
			Data d = (Data) it.next();
			if (d == null) continue;
			out.println(formatLine(d.source, d.target, d.count));
		}
		
		out.close();
		System.out.println("Done");
	}
	/**
	 * 
	 */
	private static String formatLine(String source, String target, int count) {
		return Utility.hex(source) + " ; " + Utility.hex(target," ")
				+ " ; " + count
				+ " # "
				+ "(" + source + " " + ARROW + " " + target + ") "
				+ Default.ucd().getName(source) 
				+ " " + ARROW + " " + Default.ucd().getName(target);
	}
	/**
	 * 
	 */
/*	private static void add(Map m, String source, String target, int count) {
		if (source.length() == 0 || target.length() == 0) return;
		if (preferSecondAsSource(source, target)) {
			String temp = target;
			target = source;
			source = temp;
		}
		Data2 other = (Data2) m.get(source);
		if (other != null) {
			if (target.equals(other.target)) return;
			System.out.println("conflict");
			System.out.println(formatLine(source, target, count));
			System.out.println(formatLine(source, other.target, other.count));
			// skip adding this, and instead add result -> other.target
			add(m, target, other.target, count);
		} else {
			m.put(source, new Data2(target, count));
		}
	};
*/	
	static Comparator preferredTarget = new Comparator() {
		Comparator stringComp = new UTF16.StringComparator();
		UnicodeSet preferredSet = getIdentifierSet();
		public int compare(Object o1, Object o2) {
			String a = (String)o1;
			String b = (String)o2;
			int ca = UTF16.countCodePoint(a);
			int cb = UTF16.countCodePoint(b);
			if (ca != cb) return ca < cb ? 1 : -1;
			boolean aok = preferredSet.contains(a);
			boolean bok = preferredSet.contains(b);
			if (aok != bok) {
				return aok ? -1 : 1;
			}
			return stringComp.compare(a, b);
		}
		
	};
	
/*	static private boolean preferSecondAsSource(String a, String b) {
		// if first is longer, prefer second
		int ca = UTF16.countCodePoint(a);
		int cb = UTF16.countCodePoint(b);
		if (ca != cb) {
			return ca > cb;
		}
		// if first is lower, prefer second
		return a.compareTo(b) < 0;
	}
*/	
	static String getCodeCharName(String a) {
		return Default.ucd().getCode(a) + "(  " + a + "  ) " + Default.ucd().getName(a);
	}
	/**
	 * Returns the part between - and .
	 */
	public static String getReasonFromFilename(String type) {
		int period = type.lastIndexOf('.');
		if (period < 0) period = type.length();
		int dash = type.lastIndexOf('-', period);
		return type.substring(dash+1,period);
	}

}