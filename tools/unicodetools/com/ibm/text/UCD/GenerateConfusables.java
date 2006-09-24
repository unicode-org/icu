/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateConfusables.java,v $
* $Date: 2006/09/24 23:32:44 $
* $Revision: 1.12 $
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.text.utility.Utility;


public class GenerateConfusables {
    public static String version = "2.0";
	public static boolean EXCLUDE_CONFUSABLE_COMPAT = true;

	public static void main(String[] args) throws IOException {
        quickTest();
        
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

    private static void quickTest() {
        int script = getSingleScript("\u0430\u0061");
        script = getSingleScript("\u0061\u0430"); //0323 ;  093C
        String a = "\u0323";
        String b = "\u093C";
        int isLess = betterTargetIsLess.compare(a, b); // ("\u0045", "\u13AC");
        MyEquivalenceClass test = new MyEquivalenceClass();
        test.add(a, b, "none");
        Set x = test.getEquivalences(a);
        String result = (String) CollectionUtilities.getBest(x, betterTargetIsLess, -1);
    }
    
	/**
	 * 
	 */
	static UnicodeSet _Non_IICore;
	
	private static UnicodeSet getNonIICore() {	
		//Main + IICore + (Ext-A intersect Chinese)
		//blk; n/a       ; CJK_Unified_Ideographs
		//blk; n/a       ; CJK_Unified_Ideographs_Extension_A
		//blk; n/a       ; CJK_Unified_Ideographs_Extension_B

		if (_Non_IICore == null) {
			// stuff to remove
			_Non_IICore = ups.getSet("block=CJK_Unified_Ideographs_Extension_A");
			_Non_IICore.addAll(ups.getSet("block=CJK_Unified_Ideographs_Extension_B"));
			_Non_IICore.removeAll(UNASSIGNED); // remove unassigned
			// stuff to restore
			UnicodeMap um = Default.ucd().getHanValue("kIICore");
			um.put(0x34E4, "2.1");
			um.put(0x3007, "2.1");
			_Non_IICore.removeAll(um.getSet("2.1"));
			
			// add Chinese?
            if (true) {
    			UnicodeSet cjk_nic = new UnicodeSet();
    			String line = null;
    			try {
    				BufferedReader br = BagFormatter.openUTF8Reader(indir, "cjk_nic.txt");
    				while (true) {
    					line = Utility.readDataLine(br);
    					if (line == null) break;
    					if (line.length() == 0) continue;
    					String[] pieces = Utility.split(line, ';');
    					// part 0 is range
    					String range = pieces[0].trim();
    					int rangeDivider = range.indexOf("..");
    					int start, end;
    					if (rangeDivider < 0) {
    						start = end = Integer.parseInt(range, 16);
    					} else {
    						start = Integer.parseInt(range.substring(0, rangeDivider), 16);
    						end = Integer.parseInt(range.substring(rangeDivider+2), 16);
    					}
    					cjk_nic.add(start, end);
    				}
    				br.close();
    			} catch (Exception e) {
    				throw (RuntimeException) new RuntimeException("Failure on line " + line).initCause(e);
    			}
    			_Non_IICore.removeAll(cjk_nic);
            }
		}
		return _Non_IICore;
//		for (Iterator it = um.getAvailableValues().iterator(); it.hasNext();) {
//			Object value = it.next();
//			UnicodeSet set = um.getSet(value);
//			System.out.println(value + "\t" + set);
//		}
	}
	
	static PrintWriter log;
	static final String ARROW = "\u2192"; // \u2194
	static UnicodeProperty.Factory ups = ToolUnicodePropertySource.make(""); // ICUPropertyFactory.make();
	static UnicodeSet UNASSIGNED = ups.getSet("gc=Cn")
		.addAll(ups.getSet("gc=Co"))
		.addAll(ups.getSet("gc=Cs"));
	static UnicodeSet skipSet = ups.getSet("gc=Cc")
		.addAll(ups.getSet("gc=Cf"))
		.addAll(UNASSIGNED);
	static UnicodeSet whiteSpace = ups.getSet("Whitespace=TRUE");
	static UnicodeSet lowercase = ups.getSet("gc=Ll");
	static UnicodeSet _skipNFKD;
	
	static Map gatheredNFKD = new TreeMap();
    static UnicodeMap nfcMap;
    static UnicodeMap nfkcMap;
	
	static String indir = "C:\\cvsdata\\unicode\\draft\\reports\\tr36\\data\\source\\";
	static String outdir = "C:\\cvsdata\\unicode\\draft\\reports\\tr36\\data\\";
	
	static Comparator codepointComparator = new UTF16.StringComparator(true,false,0);
    static Comparator UCAComparator = new CollectionUtilities.MultiComparator(new Comparator[] {Collator.getInstance(ULocale.ROOT), codepointComparator});

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
			"\\u3165-\\u318E" +
			"\\uA490-\\uA4C6" +
			"\\U00010140-\\U00010174" +
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
		IdentifierInfo info = IdentifierInfo.getIdentifierInfo();
		info.printIDNStuff();
	}
	
	private static class IdentifierInfo {
		static private IdentifierInfo info;
		
		static IdentifierInfo getIdentifierInfo() {
			try {
				if (info == null) info = new IdentifierInfo();
				return info;
			} catch (Exception e) {
				throw (RuntimeException) new IllegalArgumentException("Unable to access data").initCause(e);
			}
		}
		
		private boolean mergeRanges = true;

		private UnicodeSet removalSet, remainingOutputSet, inputSet_strict, inputSet_lenient, nonstarting;
		UnicodeSet propNFKCSet, notInXID, xidPlus;

		private UnicodeMap additions = new UnicodeMap(), remap = new UnicodeMap(), removals = new UnicodeMap(),
		reviews, removals2, lowerIsBetter;
        
        private UnicodeSet isCaseFolded;
		
		private IdentifierInfo() throws IOException {
            isCaseFolded = new UnicodeSet();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                int cat = Default.ucd().getCategory(cp);
                if (cat == UCD.Cn || cat == UCD.Co || cat == UCD.Cs) continue;
                String source = UTF16.valueOf(cp);
                String cf = Default.ucd().getCase(source, UCD.FULL, UCD.FOLD);
                if (cf.equals(source)) isCaseFolded.add(cp);
            }
            
			propNFKCSet = ups.getSet("NFKC_QuickCheck=N").complement();
			UnicodeSet propXIDContinueSet = ups.getSet("XID_Continue=TRUE");

            //removals.putAll(propNFKCSet.complement(), PROHIBITED + "compat variant");
			loadFileData();
			xidPlus = new UnicodeSet(propXIDContinueSet).addAll(additions.keySet()).retainAll(propNFKCSet);

			getIdentifierSet();
			notInXID = new UnicodeSet(IDNOutputSet).removeAll(xidPlus);
			removals.putAll(notInXID, PROHIBITED + NOT_IN_XID);
            //UnicodeSet notNfkcXid = new UnicodeSet(xidPlus).removeAll(removals.keySet()).removeAll(propNFKCSet);
            //removals.putAll(notNfkcXid, PROHIBITED + "compat variant");
			removalSet = removals.keySet();

			remainingOutputSet = new UnicodeSet(IDNOutputSet).removeAll(removalSet);

			UnicodeSet remainingInputSet1 = new UnicodeSet(IDNInputSet)
					.removeAll(removalSet).removeAll(remainingOutputSet);
			UnicodeSet remainingInputSet = new UnicodeSet();
			UnicodeSet specialRemove = new UnicodeSet();
			// remove any others that don't normalize/case fold to something in
			// the output set
			for (UnicodeSetIterator usi = new UnicodeSetIterator(
					remainingInputSet1); usi.next();) {
				String nss = getModifiedNKFC(usi.getString());
				String cf = Default.ucd().getCase(nss, UCD.FULL, UCD.FOLD);
				String cf2 = getModifiedNKFC(cf);
				if (remainingOutputSet.containsAll(cf2))
					remainingInputSet.add(usi.codepoint);
				else
					specialRemove.add(usi.codepoint);
			}
			// filter out the items that are case foldings of items in output
			inputSet_strict = new UnicodeSet();
			for (UnicodeSetIterator usi = new UnicodeSetIterator(
					remainingInputSet); usi.next();) {
				String ss = usi.getString();
				String nss = getModifiedNKFC(ss);
				String cf = Default.ucd().getCase(ss, UCD.FULL, UCD.FOLD);
				if (usi.codepoint == 0x2126 || usi.codepoint == 0x212B) {
					System.out.println("check");
				}
				//> > 2126 ; retained-input-only-CF # (?) OHM SIGN
				//> > 212B ; retained-input-only-CF # (?) ANGSTROM SIGN

				if (!remainingOutputSet.containsAll(nss)
						&& remainingOutputSet.containsAll(cf))
					inputSet_strict.add(ss);
			}
			// hack
			inputSet_strict.remove(0x03F4).remove(0x2126).remove(0x212B);
			inputSet_lenient = new UnicodeSet(remainingInputSet)
					.removeAll(inputSet_strict);
			nonstarting = new UnicodeSet(remainingOutputSet).addAll(
					remainingInputSet).retainAll(new UnicodeSet("[:M:]"));
			reviews = new UnicodeMap().putAll(removals);
			reviews.putAll(remainingOutputSet, "output");
			reviews.putAll(inputSet_strict, "input");
			reviews.putAll(inputSet_lenient, "input-lenient");
			reviews.putAll(specialRemove, PROHIBITED + "output-disallowed");
			
			lowerIsBetter = new UnicodeMap();

			lowerIsBetter.putAll(propNFKCSet, MARK_NFC); // nfkc is better than the alternative
			lowerIsBetter.putAll(inputSet_lenient, MARK_INPUT_LENIENT);
			lowerIsBetter.putAll(inputSet_strict, MARK_INPUT_STRICT);
			lowerIsBetter.putAll(remainingOutputSet, MARK_OUTPUT);
			lowerIsBetter.putAll(remainingOutputSet, MARK_ASCII);
			lowerIsBetter.setMissing(MARK_NOT_NFC);
			
			lowerIsBetter.freeze();
			// add special values:
			//lowerIsBetter.putAll(new UnicodeSet("["), new Integer(0));
			
			UnicodeMap nonstartingmap = new UnicodeMap().putAll(nonstarting,
					"nonstarting");
			UnicodeMap.Composer composer = new UnicodeMap.Composer() {
				public Object compose(int codePoint, Object a, Object b) {
					if (a == null)
						return b;
					else if (b == null)
						return a;
					else
						return a.toString() + "-" + b.toString();
				}
			};
			reviews.composeWith(nonstartingmap, composer);
			reviews.putAll(new UnicodeSet(IDNInputSet).complement(), "");
			UnicodeMap.Composer composer2 = new UnicodeMap.Composer() {
				public Object compose(int codePoint, Object a, Object b) {
					if (b == null)
						return a;
					return "remap-to-" + Utility.hex(b.toString());
				}
			};
			//reviews.composeWith(remap, composer2);
			removals2 = new UnicodeMap().putAll(removals);
			removals2.putAll(ups.getSet("XID_Continue=TRUE").complement(),
					PROHIBITED + NOT_IN_XID);
			removals2.setMissing("future?");
			
			additions.freeze();
			remap.freeze();
			removals.freeze();
			reviews.freeze();
			removals2.freeze();
		}

		/**
		 * 
		 */
		private void loadFileData() throws IOException {
			// get the word chars
			BufferedReader br = BagFormatter.openUTF8Reader(indir,
					"wordchars.txt");
			String line = null;
			try {
				while (true) {
					line = Utility.readDataLine(br);
					if (line == null)
						break;
					if (line.length() == 0)
						continue;
					String[] pieces = Utility.split(line, ';');
					int code = Integer.parseInt(pieces[0].trim(), 16);
					if (pieces[1].trim().equals("remap-to")) {
						remap.put(code, UTF16.valueOf(Integer.parseInt(
								pieces[2].trim(), 16)));
					} else {
						if (XIDContinueSet.contains(code)) {
							System.out.println("Already in XID continue: "
									+ line);
							continue;
						}
						additions.put(code, "addition");
					}
				}
			} catch (Exception e) {
				throw (RuntimeException) new RuntimeException(
						"Failure on line " + line).initCause(e);
			}
			br.close();

			// get all the removals.
			br = BagFormatter.openUTF8Reader(indir, "removals.txt");
			UnicodeSet allocated = ups.getSet("generalcategory=cn").complement();

			UnicodeSet sources = new UnicodeSet();
			line = null;
			try {
				while (true) {
					line = Utility.readDataLine(br);
					if (line == null)
						break;
					if (line.length() == 0)
						continue;
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
							if (codes[i].length() == 0)
								continue;
							String[] range = codes[i].split("\\.\\.");
							int start = Integer.parseInt(range[0], 16);
							int end = start;
							if (range.length > 1)
								end = Integer.parseInt(range[1], 16);
							sources.add(start, end);
						}
					}
					removals.putAll(sources, PROHIBITED + reasons);
				}
			} catch (Exception e) {
				throw (RuntimeException) new RuntimeException(
						"Failure on line " + line).initCause(e);
			}
			removals.putAll(getNonIICore(), PROHIBITED + "~IICore");
			br.close();
		}
		
		void printIDNStuff() throws IOException {
			PrintWriter out;
			printIDModifications();
			writeIDChars();
			writeIDReview();
			generateDecompFile();
		}

		/**
		 * 
		 */
		private void writeIDReview() throws IOException {
			BagFormatter bf = new BagFormatter();
			bf.setUnicodePropertyFactory(ups);
			bf.setLabelSource(null);
			bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
			bf.setMergeRanges(true);

            PrintWriter out = openAndWriteHeader("review.txt", "Review List for IDN");
//			PrintWriter out = BagFormatter.openUTF8Writer(outdir, "review.txt");
			//reviews.putAll(UNASSIGNED, "");
//			out.print("\uFEFF");
//			out.println("# Review List for IDN");
//			out.println("# $Revision: 1.12 $");
//			out.println("# $Date: 2006/09/24 23:32:44 $");
//			out.println("");

			UnicodeSet fullSet = reviews.getSet("").complement();

			bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
			}).set(reviews).setMain("Reviews", "GCB",
					UnicodeProperty.ENUMERATED, "1.0"));
			//bf.setMergeRanges(false);

			FakeBreak fakeBreak = new FakeBreak();
			bf.setRangeBreakSource(fakeBreak);
			out.println("");
			out.println("# Characters allowed in IDNA");
			out.println("");
			bf.showSetNames(out, new UnicodeSet(fullSet)); // .removeAll(bigSets)
			//bf.setMergeRanges(true);
			//			out.println("");
			//			out.println("# Large Ranges");
			//			out.println("");
			//			bf.showSetNames(out, new UnicodeSet(fullSet).retainAll(bigSets));
			out.println("");
			out.println("# Characters disallowed in IDNA");
			out
					.println("# The IDNA spec doesn't allow any of these characters,");
			out
					.println("# so don't report any of them as being missing from the above list.");
			out
					.println("# Some possible future additions, once IDNA updates to Unicode 4.1, are given.");
			out.println("");
			//bf.setRangeBreakSource(UnicodeLabel.NULL);
			bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
			}).set(removals2).setMain("Removals", "GCB",
					UnicodeProperty.ENUMERATED, "1.0"));
			//bf.setValueSource(UnicodeLabel.NULL);
			bf.showSetNames(out, new UnicodeSet(IDNInputSet).complement()
					.removeAll(UNASSIGNED));
			out.close();
		}

		/**
		 * 
		 */
		private void writeIDChars() throws IOException {
			BagFormatter bf = new BagFormatter();
			bf.setUnicodePropertyFactory(ups);
			bf.setLabelSource(null);
			bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
			bf.setMergeRanges(true);
			
			UnicodeSet letters = new UnicodeSet("[[:Alphabetic:][:Mark:][:Nd:]]");
			
            PrintWriter out = openAndWriteHeader("idnchars.txt", "Recommended Identifier Profiles for IDN");

			out.println("# Allowed as output characters");
			out.println("");
			bf.setValueSource("output");
			bf.showSetNames(out, remainingOutputSet);
			showExtras(bf, remainingOutputSet, letters);

            /*
			out.println("");

			out.println("");
			out.println("# Input Characters");
			out.println("");
			bf.setValueSource("input");
			bf.showSetNames(out, inputSet_strict);
			showExtras(bf, inputSet_strict, letters);

			out.println("");
			out.println("# Input Characters (lenient)");
			out.println("");
			bf.setValueSource("input-lenient");
			bf.showSetNames(out, inputSet_lenient);
			showExtras(bf, inputSet_lenient, letters);
			*/
            
			out.println("");
			out.println("# Not allowed at start of identifier");
			out.println("");
			bf.setValueSource("nonstarting");
			bf.showSetNames(out, nonstarting);

			//out.println("");

			//showRemapped(out, "Characters remapped on input in GUIs -- Not required by profile!", remap);

			out.close();
		}


		/**
		 * 
		 */
		private void showExtras(BagFormatter bf, UnicodeSet source, UnicodeSet letters) {
			UnicodeSet extra = new UnicodeSet(source).removeAll(letters);
			if (extra.size() != 0) {
				UnicodeSet fixed = new UnicodeSet();
				for (UnicodeSetIterator it = new UnicodeSetIterator(extra); it.next();) {
					if (!letters.containsAll(Default.nfkd().normalize(it.getString()))) {
						fixed.add(it.codepoint);
					}
				}
				System.out.println(bf.showSetNames(fixed));
			}
		}

		/**
		 * 
		 */
		private void printIDModifications() throws IOException {
			BagFormatter bf = new BagFormatter();
			bf.setUnicodePropertyFactory(ups);
			bf.setLabelSource(null);
			bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
			bf.setMergeRanges(true);

            PrintWriter out = openAndWriteHeader("xidmodifications.txt", "Security Profile for General Identifiers");
			/* PrintWriter out = BagFormatter.openUTF8Writer(outdir, "xidmodifications.txt");

			out.println("# Security Profile for General Identifiers");
			out.println("# $Revision: 1.12 $");
			out.println("# $Date: 2006/09/24 23:32:44 $");
            */


			out.println("# Characters restricted");
			out.println("");
			/*
			 * for (Iterator it = values.iterator(); it.hasNext();) { String
			 * reason1 = (String)it.next(); bf.setValueSource(reason1);
			 * out.println(""); bf.showSetNames(out, removals.getSet(reason1)); }
			 */
			bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
			}).set(removals).setMain("Removals", "GCB",
					UnicodeProperty.ENUMERATED, "1.0"));
			bf.showSetNames(out, removalSet);

			out.println("");
			out.println("# Characters added");
			out.println("");
			bf.setValueSource("addition");
			bf.showSetNames(out, additions.keySet());

			//showRemapped(out, "Characters remapped on input", remap);

			out.close();
            
           out = openAndWriteHeader("xidAllowed.txt", "Security Profile for General Identifiers");
           UnicodeSet allowed = new UnicodeSet(xidPlus).removeAll(removals.keySet());
            UnicodeSet cfAllowed = new UnicodeSet().addAll(allowed).retainAll(isCaseFolded).retainAll(propNFKCSet);
            allowed.removeAll(cfAllowed);
            bf.setValueSource("case_folded");
            out.println("# XID characters allowed (no uppercase)");
            out.println("");
            bf.showSetNames(out, cfAllowed);
            bf.setValueSource("not_case_folded");
            out.println("");
            out.println("# XID characters allowed (uppercase)");
            out.println("");
            bf.showSetNames(out, allowed);
            out.close();
			
			UnicodeMap someRemovals = new UnicodeMap();
			UnicodeMap.Composer myComposer = new UnicodeMap.Composer() {
				public Object compose(int codePoint, Object a, Object b) {
					if (b == null) return null;
					String x = (String)b;
					if (false) {
						if (!IDNOutputSet.contains(codePoint)) {
							return "~IDNA";
						}
						if (!xidPlus.contains(codePoint)) {
							return "~Unicode Identifier";
						}
					}
					if (x.startsWith(PROHIBITED)) x = x.substring(PROHIBITED.length());
					//if (!propNFKCSet.contains(codePoint)) x += "*";
					if (lowercase.contains(codePoint)) {
						String upper = Default.ucd().getCase(codePoint, UCD.FULL, UCD.UPPER);
						if (upper.equals(UTF16.valueOf(codePoint)) 
								&& x.equals("technical symbol (phonetic)")) x = "technical symbol (phonetic with no uppercase)";
					}
					return x;
				}				
			};
			someRemovals.composeWith(removals, myComposer);
			UnicodeSet nonIDNA = new UnicodeSet(IDNOutputSet).addAll(IDNInputSet).complement();
			someRemovals.putAll(nonIDNA, "~IDNA");
			someRemovals.putAll(new UnicodeSet(xidPlus).complement(), "~Unicode Identifier");
			someRemovals.putAll(UNASSIGNED, null); // clear extras
			//someRemovals = removals;
			out = BagFormatter.openUTF8Writer(outdir, "draft-restrictions.txt");
			out.println("# Characters restricted in domain names");
			out.println("# $Revision: 1.12 $");
			out.println("# $Date: 2006/09/24 23:32:44 $");
			out.println("#");
			out.println("# This file contains a draft list of characters for use in");
			out.println("#     UTR #36: Unicode Security Considerations");
			out.println("#     http://unicode.org/draft/reports/tr36/tr36.html");
			out.println("# According to the recommendations in that document, these characters");
			out.println("# would be restricted in domain names: people would only be able to use them");
			out.println("# by using lenient security settings.");
			out.println("#");
			out.println("# If you have any feedback on this list, please use the submission form at:");
			out.println("#     http://unicode.org/reporting.html.");
			out.println("#");
			out.println("# Notes:");
			out.println("# - Characters are listed along with a reason for their removal.");
			out.println("# - Characters listed as ~IDNA are excluded at this point in domain names,");
			out.println("#   in many cases because the international domain name specification does not contain");
			out.println("#   characters beyond Unicode 3.2. At this point in time, feedback on those characters");
			out.println("#   is not relevant.");
			out.println("# - Characters listed as ~Unicode Identifiers are restricted because they");
			out.println("#   do not fit the specification of identifiers given in");
			out.println("#      UAX #31: Identifier and Pattern Syntax");
			out.println("#      http://unicode.org/reports/tr31/");
			out.println("# - Characters listed as ~IICore are restricted because they are Ideographic,");
			out.println("#   but not part of the IICore set defined by the IRG as the minimal set");
			out.println("#   of required ideographs for East Asian use.");
			out.println("# - The files in this directory are 'live', and may change at any time.");
			out.println("#   Please include the above Revision number in your feedback.");
			
			bf.setRangeBreakSource(new FakeBreak2());
			if (true) {
				Set values = new TreeSet(someRemovals.getAvailableValues());
				for (Iterator it = values.iterator(); it.hasNext();) {
					String reason1 = (String) it.next();
					bf.setValueSource(reason1);
					out.println("");
					bf.showSetNames(out, someRemovals.getSet(reason1));
				}
			} else {
				bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
				}).set(someRemovals).setMain("Removals", "GCB",
						UnicodeProperty.ENUMERATED, "1.0"));
				bf.showSetNames(out, someRemovals.keySet());
			}
			out.close();
		}
	}

	static final String PROHIBITED = "restricted ; ";
	static final String NOT_IN_XID = "not in XID+";
    public static final boolean suppress_NFKC = true;
	/**
 * 
 */


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
				String target = getModifiedNKFC(source);
				writeSourceTargetLine(out, source, null, target, value);
			}
			//bf.showSetNames(out, s);
			out.flush();
		}
		out.close();
	}

	static class FakeBreak extends UnicodeLabel {
		UnicodeSet nobreakSet = setsToAbbreviate;
		public String getValue(int codepoint, boolean isShort) {
			return nobreakSet.contains(codepoint) ? ""
			 : (codepoint & 1) == 0 ? "O"
			 : "E";
		}
	}

	static class FakeBreak2 extends UnicodeLabel {
		UnicodeSet nobreakSet = new UnicodeSet(setsToAbbreviate)
			.addAll(new UnicodeSet(IDNOutputSet).complement())
			.addAll(new UnicodeSet(IdentifierInfo.getIdentifierInfo().xidPlus).complement());

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
		for (UnicodeSetIterator usi = new UnicodeSetIterator(remap.keySet()); usi.next();) {
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
        nfcMap = new UnicodeMap();
        nfkcMap = new UnicodeMap();
		if (_skipNFKD == null) {
			_skipNFKD = new UnicodeSet();
			UnicodeSet idSet = getIdentifierSet();
			for (int cp = 0; cp <= 0x10FFFF; ++cp) {
				Utility.dot(cp);
				int cat = Default.ucd().getCategory(cp);
				if (cat == UCD.Cn || cat == UCD.Co || cat == UCD.Cs) continue;
				int decompType = Default.ucd().getDecompositionType(cp);
                String nfc = Default.nfc().normalize(cp);
                if (decompType == UCD.CANONICAL) nfcMap.put(cp, nfc);
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
                String source = UTF16.valueOf(cp);
				String mapped = Default.nfkd().normalize(cp);
                String kmapped = getModifiedNKFC(source);
                if (!kmapped.equals(source) && !kmapped.equals(nfc)) {
                    if (kmapped.startsWith(" ") || kmapped.startsWith("\u0640")) {
                        System.out.println("?? " + Default.ucd().getCodeAndName(cp));
                        System.out.println("\t" + Default.ucd().getCodeAndName(kmapped));
                        kmapped = getModifiedNKFC(source); // for debugging
                    }
                    nfkcMap.put(cp,kmapped);
                }
				if (mapped.equals(source)) continue;
				if (idSet.contains(cp) && !idSet.contains(mapped)) _skipNFKD.add(cp);
				else if (!whiteSpace.contains(cp) && whiteSpace.containsSome(mapped)) _skipNFKD.add(cp);
			}
		}
        nfcMap.setMissing("");
        nfcMap.freeze();
        nfkcMap.setMissing("");
        nfkcMap.freeze();
		return _skipNFKD;
	}
	
	private static boolean isMixedScript(String source) {
		return getSingleScript(source) == UScript.INVALID_CODE;
	}

/**
 * Returns the script of the input text. Script values of COMMON and INHERITED are ignored.
 * @param source Input text.
 * @return Script value found in the text.
 * If more than one script values are found, then UScript.INVALID_CODE is returned.
 * If no script value is found (other than COMMON or INHERITED), then UScript.COMMON is returned.
 */
public static int getSingleScript(String source) {
    if (source.length() == 0) return UScript.COMMON;
	int lastScript = UScript.COMMON; // temporary value
	int cp;
	for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
		cp = UTF16.charAt(source, i);
		int script = UScript.getScript(cp);
		if (script == UScript.COMMON || script == UScript.INHERITED) {
            continue;
		}
		if (lastScript == UScript.COMMON) {
            lastScript = script;
        } else if (script != lastScript) {
            return UScript.INVALID_CODE;
        }
	}
	return lastScript;
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
				+ "\t#"
                + (isXid(source) ? "" : "*")
				+ " ( " + source + " " + ARROW + " " + target + " ) " 
				+ Default.ucd().getName(source) + " " + ARROW + " "
				+ Default.ucd().getName(target)
				);
		if (reason != null) out.print("\t# " + reason);
		out.println();
	}

	static UnicodeSet controls = new UnicodeSet("[:Cc:]");
	
	static class MyEquivalenceClass extends XEquivalenceClass {
		public MyEquivalenceClass() {
			super("NONE");
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
				Set cloneForSafety = getOrderedExplicitItems();
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
				String mapped = getParadigm(cps, false, false);
				if (mapped.indexOf(cps) >= 0) result.append(cps);
				else {
					result.append(mapped);
                    List x = getReasons(cps, mapped);
					reasons.append(getBestForm(x));
				}
			}
			return result.toString();
		}
		
		private Object getBestForm(Collection x) {
            if (x.size() != 1)  return "[" +  x + "]";
            Object item = x.iterator().next();
            if (!(item instanceof Collection))  return x.toString();
            return getBestForm((Collection)item);
        }
        
        public String getParadigm(String item, boolean onlyLowercase, boolean onlySameScript) {
            Set filteredSet;
            if (onlyLowercase == false && onlySameScript == false) {
                filteredSet = getEquivalences(item);
            } else {
                filteredSet = new HashSet();
                for (Iterator it = getEquivalences(item).iterator(); it.hasNext();) {
                    String other = (String) it.next();
                    String combined = item + other;
                    if (onlyLowercase) {
                        boolean isLowercase = combined.equals(Default.ucd().getCase(combined, UCD.FULL, UCD.FOLD));
                        if (!isLowercase) continue;
                    }
                    if (onlySameScript) {
                        boolean isMixed = isMixedScript(combined);
                        if (isMixed) continue;                      
                    }
                    filteredSet.add(other);
                }
            }
			return (String) CollectionUtilities.getBest(filteredSet, betterTargetIsLess, -1);
		}
		
		public Set getOrderedExplicitItems() {
			Set cloneForSafety = new TreeSet(codepointComparator);
			cloneForSafety.addAll(getExplicitItems());
			return cloneForSafety;
		}
		/**
		 * 
		 */
		public void writeSource(PrintWriter out) {
			Set items = getOrderedExplicitItems();
			for (Iterator it = items.iterator(); it.hasNext();) {
				String item = (String) it.next();
				String paradigm = (String) CollectionUtilities.getBest(getEquivalences(item), betterTargetIsLess, -1);
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
            if (combined.indexOf("\u0430") >= 0) {
                System.out.println(Default.ucd().getCodeAndName(combined));
            }
			boolean isLowercase = combined.equals(Default.ucd().getCase(combined, UCD.FULL, UCD.FOLD));
			boolean isMixed = isMixedScript(combined);
			dataMixedAnycase.add(source, target, type);
			if (isLowercase) {
                dataMixedLowercase.add(source, target, type);
            }
			if (!isMixed) {
                dataSingleAnycase.add(source, target, type);
            }
			if (!isMixed && isLowercase) {
                dataSingleLowercase.add(source, target, type);
            }
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
                        String nfkdSource = Default.nfkd().normalize(source);
                        String nfkdTarget = Default.nfkd().normalize(target);
                        if (suppress_NFKC && nfkdSource.equals(nfkdTarget)) {
                           System.out.println("Suppressing nfkc for: " + Default.ucd().getCodeAndName(source));
                        } else {
                            add(source, target, type, count, line);
                        }
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
            PrintWriter out = openAndWriteHeader(filename, "Source File for IDN Confusables");
//			PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
//			out.println("# Source File for IDN Confusables");
//			out.println("# $Revision: 1.12 $");
//			out.println("# $Date: 2006/09/24 23:32:44 $");
//			out.println("");
			dataMixedAnycase.writeSource(out);
			out.close();
		}
		
		public void writeSourceOrder(String directory, String filename, boolean appendFile, boolean skipNFKEquivs) throws IOException {
            PrintWriter out = openAndWriteHeader(filename, "Recommended confusable mapping for IDN");
//            PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
//			out.println("# Recommended confusable mapping for IDN");
//			out.println("# $Revision: 1.12 $");
//			out.println("# $Date: 2006/09/24 23:32:44 $");
//			out.println("");

			if (appendFile) {
				String[] replacements = {"%date%", Default.getDate()};
				Utility.appendFile("com/ibm/text/UCD/confusablesHeader.txt", 
						Utility.UTF8_WINDOWS, out, replacements);
			}
            if (true) {
                writeSourceOrder(out, dataMixedAnycase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, true, true);
                writeSourceOrder(out, dataMixedAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, false, true);
                writeSourceOrder(out, dataMixedAnycase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, true, false);
                writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, false, false);                
            } else {
    			writeSourceOrder(out, dataSingleLowercase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, false, false);
    			writeSourceOrder(out, dataSingleAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, false, false);
    			writeSourceOrder(out, dataMixedLowercase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, false, false);
    			writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, false, false);
            }
			out.close();
		}
		/**
		 * @param skipNFKEquivs TODO
		 * @param onlyLowercase TODO
		 * @param onlySingleScript TODO
		 * 
		 */
		private void writeSourceOrder(PrintWriter out, MyEquivalenceClass data, String tag, String title, boolean skipNFKEquivs, boolean onlyLowercase, boolean onlySingleScript) {
			// first get all the sets. Then get the best paradigm from each. Then sort.
//			Set setOfSets = data.getEquivalenceSets();
//			Map orderedResults = new TreeMap(betterTargetIsLess);
//			for (Iterator it = setOfSets.iterator(); it.hasNext();) {
//				Set setOfEquivs = (Set) it.next();
//				Object item = CollectionUtilities.getBest(setOfEquivs, betterTargetIsLess, -1);
//				
//			}
			//int c = codepointComparator.compare("\uFFFF", "\uD800\uDC00");
			//System.out.println("Code Point Compare: " + c);
			Set items = data.getOrderedExplicitItems();
			out.println();
			out.println("# " + title);
			out.println();
			int count = 0;
			UnicodeSet preferredID = getIdentifierSet();
            ArrayComparator ac = new ArrayComparator(new Comparator[] {UCAComparator, UCAComparator});
            Set orderedPairs = new TreeSet(ac);
			for (Iterator it = items.iterator(); it.hasNext();) {
				String source = (String) it.next();
                if (UTF16.hasMoreCodePointsThan(source,1)) continue;
				String target = data.getParadigm(source, onlyLowercase, onlySingleScript);
                if (target == null) continue;
				if (source.equals(target)) continue;
				if (skipNFKEquivs) {
					if (!Default.nfkd().normalize(source).equals(source)) continue;
				}
                orderedPairs.add(new String[] {target, source});
            }
            String lastTarget = null;
            for (Iterator it = orderedPairs.iterator(); it.hasNext();) {
                String[] pair = (String[]) it.next();
                String source = pair[1];
                String target = pair[0];
				String reason = fixReason(data.getReasons(source, target));
                if (lastTarget != null && !lastTarget.equals(target)) {
                    out.println();
                }
				writeSourceTargetLine(out, source, tag, target, reason);
                lastTarget = target;
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
			for (UnicodeSetIterator it = new UnicodeSetIterator(decompMap.keySet()); it.next(); ) {
				add(it.getString(), (String)decompMap.getValue(it.codepoint), type, ++count, errorLine);
			}
		}
		
		static class MyFilter implements XEquivalenceClass.Filter {
			UnicodeSet output;
			public boolean matches(Object o) {
				return output.containsAll((String)o);
			}				
		}

		static class MyCollectionFilter implements CollectionUtilities.ObjectMatcher {
			UnicodeSet outputAllowed;
			int minLength;
			public boolean matches(Object o) {
				String item = (String)o;
				if (!outputAllowed.containsAll(item)) return false;
				int len = UTF16.countCodePoint(item);
				if (len < minLength) minLength = len;
				return true;
			}				
		};
		/**
		 * @param script TODO
		 * @throws IOException
		 * 
		 */
		public void writeSummary(String outdir, String filename, boolean outputOnly, UnicodeSet script) throws IOException {
            PrintWriter out = openAndWriteHeader(filename, "Summary: Recommended confusable mapping for IDN");
//			PrintWriter out = BagFormatter.openUTF8Writer(outdir, filename);
//			out.print('\uFEFF');
//			out.println("# Summary: Recommended confusable mapping for IDN");
//			out.println("# $Revision: 1.12 $");
//			out.println("# $Date: 2006/09/24 23:32:44 $");
//			out.println("");
            UnicodeSet representable = new UnicodeSet();
			MyEquivalenceClass data = dataMixedAnycase;
			Set items = data.getOrderedExplicitItems();
//			for (Iterator it = items.iterator(); it.hasNext();) {
//				System.out.println(Default.ucd().getCodeAndName((String)it.next()));
//			}
			int count = 0;
			UnicodeSet preferredID = getIdentifierSet();
			String lastTarget = "";
			Set itemsSeen = new HashSet();
			Set equivalents = new TreeSet(betterTargetIsLess);
			MyCollectionFilter myFilter = new MyCollectionFilter();
			myFilter.outputAllowed= new UnicodeSet("[[\u0021-\u007E]-[:letter:]]")
			.addAll(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
			.addAll(IdentifierInfo.getIdentifierInfo().inputSet_strict);
			
			for (Iterator it = items.iterator(); it.hasNext();) {
				String target = (String) it.next();
				if (itemsSeen.contains(target)) continue;
				equivalents.clear();
				equivalents.addAll(data.getEquivalences(target));
				itemsSeen.addAll(equivalents);
				if (outputOnly) { // remove non-output
					myFilter.minLength = 1000;
					CollectionUtilities.retainAll(equivalents, myFilter);
					if (equivalents.size() <= 1) continue;
					if (myFilter.minLength > 1) continue;
					if (!equivalents.contains(target)) { // select new target if needed
						target = (String) equivalents.iterator().next();
					}
				}
				scriptTest:
				if (script != null) {
					// see if at least one item contains the target script
					for (Iterator it2 = equivalents.iterator(); it2.hasNext();) {
						String item = (String) it2.next();
						if (script.containsAll(item)) {
							target = item;
							for (Iterator it3 = equivalents.iterator(); it3.hasNext();) {
								representable.addAll((String)it3.next());
							}
							break scriptTest;
						}
					}
					continue; // skip this one
				}
				out.println();
				out.println(getStatus(target) + "\t" + "(\u200E " + target + " \u200E)\t" + Utility.hex(target) + "\t " + Default.ucd().getName(target));
				//if (UTF16.hasMoreCodePointsThan(source,1)) continue;
				for (Iterator it2 = equivalents.iterator(); it2.hasNext();) {					
					String source = (String) it2.next();
					if (source.equals(target)) continue;
					//boolean compatEqual = Default.nfkd().normalize(source).equals(Default.nfkd().normalize(target));
					//if (EXCLUDE_CONFUSABLE_COMPAT && compatEqual) continue;
					String reason = fixReason(data.getReasons(source, target));
					//if (!outputAllowed.containsAll(source)) continue;
//					if (compatEqual) {
//						out.print("\u21D0");
//					} else {
//						out.print("\u2190");
//					}
					out.println("\u2190" + getStatus(source) + "\t" + "(\u200E " + source + " \u200E)\t" + Utility.hex(source) + "\t " + Default.ucd().getName(source)
							+ "\t# " + reason);
					count++;
				}
			}
			out.println();
			out.println("# total : " + count);
			out.println();
			if (script != null) {
				out.println();
				out.println("# Base Letters Representable with Script");
				out.println();
				representable.removeAll(script);
				BagFormatter bf = new BagFormatter();
				bf.setValueSource(ups.getProperty("script"));
				bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
				bf.showSetNames(out, representable);
			}
			out.close();
		}
		


		public void writeWholeScripts(String outdir, String filename) throws IOException {
			UnicodeSet commonAndInherited = new UnicodeSet(
			"[[:script=common:][:script=inherited:]]");

			WholeScript wsLower = new WholeScript(
					new UnicodeSet(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
					.removeAll(new UnicodeSet("[A-Z]")), "L");
			WholeScript wsAny = new WholeScript(
					new UnicodeSet(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
					.addAll(IdentifierInfo.getIdentifierInfo().inputSet_strict), "A");

			MyEquivalenceClass data = new MyEquivalenceClass();
			for (Iterator it = dataMixedAnycase.getSamples().iterator(); it.hasNext();) {
				String target = (String) it.next();
				Set equivalents = dataMixedAnycase.getEquivalences(target);
				boolean first = true;
				for (Iterator it2 = equivalents.iterator(); it2.hasNext();) {
					String cleaned = CollectionUtilities.remove((String)it2.next(), commonAndInherited);
					if (cleaned.length() == 0) continue;
					if (first) {
						target = cleaned;
						first = false;
					} else {
						data.add(target, cleaned);
					}
				}
			}
			Set itemsSeen = new HashSet();
			for (Iterator it = data.getOrderedExplicitItems().iterator(); it.hasNext();) {
				String target = (String) it.next();
				if (itemsSeen.contains(target)) continue;
				Set equivalents = data.getEquivalences(target);
				itemsSeen.addAll(equivalents);
				wsAny.addEquivalents(equivalents);
				wsLower.addEquivalents(equivalents);
			}
            PrintWriter out = openAndWriteHeader(filename, "Summary: Whole-Script Confusables");
//			PrintWriter out = BagFormatter.openUTF8Writer(outdir, filename);
//			out.print('\uFEFF');
//			out.println("# Summary: Whole-Script Confusables");
//			out.println("# $Revision: 1.12 $");
//			out.println("# $Date: 2006/09/24 23:32:44 $");
			out.println("# This data is used for determining whether a strings is a");
			out.println("# whole-script or mixed-script confusable.");
			out.println("# The mappings here ignore common and inherited script characters,");
			out.println("# such as accents.");
			out.println("");
			out.println("# Lowercase Only");
			out.println("");
			wsLower.write(out);
			out.println("");
			out.println("# Any-Case");
			out.println("");
			wsAny.write(out);
			out.close();
		}
		/**
		 * 
		 */
		private String getStatus(String source) {
			// TODO Auto-generated method stub
			int val = betterTargetIsLess.getValue(source);
			if (val == MARK_NOT_NFC.intValue()) return "[x]";
			if (val == MARK_NFC.intValue()) return "[x]";
			if (val == MARK_INPUT_LENIENT.intValue()) return "[L]";
			if (val == MARK_INPUT_STRICT.intValue()) return "[I]";
			if (val == MARK_OUTPUT.intValue()) return "[O]";
			if (val == MARK_ASCII.intValue()) return "[A]";
			
			return "?";
		}
	}

	static class WholeScript {
		private static UnicodeSet commonAndInherited = new UnicodeSet("[[:script=common:][:script=inherited:]]");
		private UnicodeSet filterSet;
		private UnicodeSet[] script_representables = new UnicodeSet[UScript.CODE_LIMIT];
		private UnicodeSet[] script_set = new UnicodeSet[UScript.CODE_LIMIT];
		private BagFormatter bf = new BagFormatter();
		private String label;
		{
			for (int i = 0; i < UScript.CODE_LIMIT; ++i) {
				script_representables[i] = new UnicodeSet();
				script_set[i] = new UnicodeSet("[:script=" + UScript.getName(i) + ":]"); // ugly hack
			}
			bf.setValueSource(ups.getProperty("script"));
			bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
			bf.setLabelSource(UnicodeLabel.NULL);
		}
		WholeScript(UnicodeSet filterSet, String label) {
			this.filterSet = filterSet;
			this.label = label;
			finished = false;
		}
		void addEquivalents(Set set) {
			finished = false;
			// if we have y ~ x, and both are single scripts
			// that means that x can be represented in script(y), 
			// and y can be represented in script(x).
			for (Iterator it = set.iterator(); it.hasNext();) {
				String item1 = (String)it.next();
				if (!filterSet.containsAll(item1)) continue;
				int script1 = getSingleScript(item1);
				if (script1 == UScript.INVALID_CODE) continue;
				for (Iterator it2 = set.iterator(); it2.hasNext();) {
					String item2 = (String)it2.next();
					if (!filterSet.containsAll(item2)) continue;
					int script2 = getSingleScript(item2);
					if (script2 == UScript.INVALID_CODE || script2 == script1) continue;						
					script_representables[script1].addAll(item2).removeAll(commonAndInherited);
				}
			}
		}
		
		public static class UnicodeSetToScript {
			public int getScript() {
				return script;
			}
			public UnicodeSetToScript setScript(int script) {
				this.script = script;
				return this;
			}
			public UnicodeSet getSet() {
				return set;
			}
			public UnicodeSetToScript setSet(UnicodeSet set) {
				this.set = set;
				return this;
			}
			private UnicodeSet set;
			private int script;
		}
		
		UnicodeSetToScript[][] scriptToUnicodeSetToScript = new UnicodeSetToScript[UScript.CODE_LIMIT][];
		UnicodeSet[] fastReject = new UnicodeSet[UScript.CODE_LIMIT];
		boolean finished = false;
		
		void finish() {
			if (finished) return;
			for (int j = 0; j < UScript.CODE_LIMIT; ++j) {
				if (j == UScript.COMMON || j == UScript.INHERITED) continue;
				if (script_representables[j].size() == 0) continue;
				UnicodeSet accept = new UnicodeSet();
				List curr = new ArrayList();
				for (int k = 0; k < UScript.CODE_LIMIT; ++k) {
					if (k == UScript.COMMON || k == UScript.INHERITED) continue;
					if (script_representables[k].size() == 0) continue;

					if (script_set[j].containsNone(script_representables[k])) continue;					
					UnicodeSet items = new UnicodeSet(script_set[j]).retainAll(script_representables[k]);
					UnicodeSetToScript uss = new UnicodeSetToScript().setScript(k).setSet(items);
					curr.add(uss);
				}
				scriptToUnicodeSetToScript[j] = (UnicodeSetToScript[]) curr.toArray(new UnicodeSetToScript[curr.size()]);
				fastReject[j] = accept.complement();
			}
			finished = true;
		}
		
		void write(PrintWriter out) throws IOException {
			finish();
			for (int j = 0; j < UScript.CODE_LIMIT; ++j) {
				if (scriptToUnicodeSetToScript[j] == null) continue;			
				for (int q = 0; q < scriptToUnicodeSetToScript[j].length; ++q) {
					UnicodeSetToScript uss = scriptToUnicodeSetToScript[j][q];
					int k = uss.getScript();
					UnicodeSet items = uss.getSet();
					String sname = UScript.getShortName(j) + "; " + UScript.getShortName(k) + "; " + label;
					String name = UScript.getName(j) + "; " + UScript.getName(k);
					out.println("# " + name + ": " + items.toPattern(false));
					out.println("");
					bf.setValueSource(sname);
					bf.showSetNames(out, items);
					out.println("");
				}
			}
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
				
				if (betterTargetIsLess.compare(source, target) < 0) {
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
        // add normalized data
//        for (int i = 0; i <= 0x10FFFF; ++i) {
//            if (Default.nfkc().isNormalized(i)) continue;
//            String result = getModifiedNKFC(UTF16.valueOf(i));
//            ds.foo();
//        }
        getSkipNFKD();
		DataSet ds = new DataSet();
		ds.addUnicodeMap(nfcMap, "nfc", "nfc");
		ds.close("*");
        total.addAll(ds);
        total.close("*");

        ds = new DataSet();
        ds.addUnicodeMap(nfkcMap, "nfkc", "nfkc");
        ds.close("*");
		//ds.write(outdir, "new-decomp.txt", false, false);
		total.addAll(ds);
		total.close("*");
        
		total.writeSummary(outdir, "confusablesSummary.txt", false, null);
		total.writeSummary(outdir, "confusablesSummaryIdentifier.txt", true, null);
		//total.writeSummary(outdir, "confusablesSummaryCyrillic.txt", true, 
		//		new UnicodeSet("[[:script=Cyrillic:][:script=common:][:script=inherited:]]"));
		total.writeWholeScripts(outdir, "confusablesWholeScript.txt");
		total.writeSourceOrder(outdir, "confusables.txt", false, false);
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
	
	static Integer
		MARK_NOT_NFC = new Integer(50),
		MARK_NFC = new Integer(40),
		MARK_INPUT_LENIENT = new Integer(30),
		MARK_INPUT_STRICT = new Integer(20),
		MARK_OUTPUT = new Integer(10),
		MARK_ASCII = new Integer(10);

	static _BetterTargetIsLess betterTargetIsLess = new _BetterTargetIsLess();
    
    static UnicodeSet XID = new UnicodeSet("[:xidcontinue:]");
    
    static boolean isXid(String x) {
        return  XID.containsAll(x);
    }
	
	static class _BetterTargetIsLess implements Comparator {
		IdentifierInfo info = IdentifierInfo.getIdentifierInfo();
		
		public int compare(Object o1, Object o2) {
			String a = (String)o1;
			String b = (String)o2;
            // longer is better (less)
			int ca = UTF16.countCodePoint(a);
			int cb = UTF16.countCodePoint(b);
			if (ca != cb)  {
                return ca > cb ? -1 : 1;
            }
            
            // is Identifier is better
            boolean ba = isXid(a);
            boolean bb = isXid(b);
            if (ba != bb) {
                return ba ? -1 : 1;
            }
            
			int aok = getValue(a);
			int bok = getValue(b);
			if (aok != bok) return aok < bok ? -1 : 1;
			return codepointComparator.compare(a, b);
		}
		static final int BAD = 1000;
		
		private int getValue(String a) { // lower is better
			int cp;
			int lastValue = 0;
			for (int i = 0; i < a.length(); i += UTF16.getCharCount(cp)) {
				cp = UTF16.charAt(a, i);
				Object objValue = info.lowerIsBetter.getValue(cp);
				int value = ((Integer) objValue).intValue();
				if (value > lastValue) lastValue = value;
			}
			return lastValue;
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

    static Normalizer modNFKC ;

     private static String getModifiedNKFC(String cf) {
         if (modNFKC == null) {
             modNFKC =  new Normalizer(Normalizer.NFKC, Default.ucdVersion());
             modNFKC.setSpacingSubstitute();
         }
         return modNFKC.normalize(cf);
     }
     
     private static PrintWriter openAndWriteHeader(String filename, String title) throws IOException {
         PrintWriter out = BagFormatter.openUTF8Writer(outdir, filename);
         out.print('\uFEFF');
         out.println("# " + title);
         out.println("# File: " + filename);
         out.println("# Version: " + version);
         out.println("# Generated: " + Default.getDate());
         out.println("# Checkin: $Revision: 1.12 $");
         out.println("#");
         out.println("# For documentation and usage, see http://www.unicode.org/reports/tr39/");
         out.println("#");
         return out;
     }

}