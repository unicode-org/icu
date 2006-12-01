/*
 * Created on May 3, 2005
 * Copyright (C) 2004-2005, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMap.Composer;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.util.ULocale;
import com.ibm.text.UCD.GenerateHanTransliterator.MultiComparator;
import com.ibm.text.UCD.TestData.RegexMatcher;
import com.ibm.text.utility.Utility;


class GenerateStringPrep implements UCD_Types {
	
	public static void main (String[] args) throws IOException {
		//checkChars(false);
		new GenerateStringPrep().genStringPrep();
		System.out.println("Done");
	}
	
	UnicodeSet[] coreChars = new UnicodeSet[100];
	UnicodeSet decomposable = new UnicodeSet();
	UnicodeMap suspect = new UnicodeMap();
	
	ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("");
	ToolUnicodePropertySource ups32 = ToolUnicodePropertySource.make("3.2.0");
	//UnicodeSet id_continue = ups.getSet("ID_Continue=true");
	UnicodeSet xid_continue = ups.getSet("XID_Continue=true");
	UnicodeSet wordChars = new UnicodeSet();
	{
		if (false) {
			wordChars.addAll(ups.getSet("name=.*MODIFIER LETTER.*", new RegexMatcher()));
			wordChars.retainAll(ups.getSet("gc=Sk"));
		}
		wordChars.addAll(new UnicodeSet("[\\u0027 \\u002D \\u002E \\u003A \\u00B7 \\u058A \\u05F3" +
		" \\u05F4 \\u200C \\u200D \\u2010 \\u2019 \\u2027 \\u30A0 \\u04C0" +
		" \\u055A \\u02B9 \\u02BA]"));
		//wordChars.removeAll(xid_continue);
	}
	
	UnicodeSet patternProp = ups.getSet("Pattern_Syntax=true").removeAll(wordChars);
	UnicodeSet isNFKC = ups.getSet("NFKC_Quickcheck=NO").complement();
	UnicodeSet non_spacing = new UnicodeSet(ups.getSet("gc=Me"))
		.addAll(ups.getSet("gc=Mn"))
		.removeAll(ups.getSet("Default_Ignorable_Code_Point=true"));
	
	UnicodeSet not_xid_continue = new UnicodeSet(xid_continue).complement().removeAll(wordChars);
	
	//UnicodeSet[] decompChars = new UnicodeSet[100];
	UCD ucd = Default.ucd();

	static Collator uca0 = Collator.getInstance(ULocale.ENGLISH);
	{
		uca0.setStrength(Collator.IDENTICAL);
	}
	static GenerateHanTransliterator.MultiComparator uca 
		= new GenerateHanTransliterator.MultiComparator(new Comparator[] {
				uca0, new UTF16.StringComparator()});

	UnicodeSet bidiR = new UnicodeSet(
			"[[:Bidi_Class=AL:][:Bidi_Class=R:]]");

	UnicodeSet bidiL = new UnicodeSet("[:Bidi_Class=l:]");
	UnicodeSet hasNoUpper = new UnicodeSet();
	UnicodeSet hasNoUpperMinus = new UnicodeSet();
	BagFormatter bf = new BagFormatter();
	UnicodeSet inIDN = new UnicodeSet();
	UnicodeSet isCaseFolded = new UnicodeSet();

	void genStringPrep() throws IOException {
		//showScriptToBlock();
		bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
		bf.setUnicodePropertyFactory(ups);
		//bf.setValueSource(UnicodeLabel.NULL);
		if (false) {
			
			System.out.println("word chars: " + bf.showSetNames(wordChars));
			System.out.println("pat: " + bf.showSetNames(patternProp));
			System.out.println("xid: " + bf.showSetNames(not_xid_continue));
		}
		for (int cp = 0; cp <= 0x10FFFF; ++cp) {
			Utility.dot(cp);
			int cat = Default.ucd().getCategory(cp);
			if (cat == UCD.Cn || cat == UCD.Co || cat == UCD.Cs) continue;
			if (!Default.nfd().isNormalized(cp)) decomposable.add(cp);
			// get IDNA
			int idnaType = getIDNAType(cp);
			idnaTypeSet[idnaType].add(cp);
			
			String str = UTF16.valueOf(cp);
			if (str.equals(ucd.getCase(str, FULL, UPPER))) hasNoUpper.add(cp);
			if (str.equals(ucd.getCase(str, FULL, FOLD))) isCaseFolded.add(cp);
			
			// scripts
			int script = ucd.getScript(cp);
			if (coreChars[script] == null)
				coreChars[script] = new UnicodeSet();
			coreChars[script].add(cp);
		}
		// fix characters with no uppercase
		hasNoUpperMinus = new UnicodeSet(hasNoUpper).removeAll(wordChars);
		System.out.println(bf.showSetNames(hasNoUpper));
		
		Utility.fixDot();
		PrintWriter htmlOut = BagFormatter.openUTF8Writer(GEN_DIR, "idn-chars.html");
		PrintWriter htmlOut2 = BagFormatter.openUTF8Writer(GEN_DIR, "script-chars.html");
		PrintWriter textOut = BagFormatter.openUTF8Writer(GEN_DIR, "idn-chars.txt");
		textOut.println('\uFEFF');
		textOut.println("For documentation, see idn-chars.html");
		
		Utility.appendFile("./com/ibm/text/UCD/idn-charsHeader.html", Utility.UTF8_WINDOWS, htmlOut, 
				new String[] {"%date%", Default.getDate()});
		/*
		out
				.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
		out.println("<title>IDN Characters</title><style>");
		out.println("<!--");
		out.println(".script       { font-size: 150%; background-color: #CCCCCC }");
		out.println(".Atomic       { background-color: #CCCCFF }");
		out.println(".Atomic-no-uppercase       { background-color: #CCFFCC }");
		out.println(".Non-XID       { background-color: #FFCCCC }");
		out.println(".Decomposable       { background-color: #FFFFCC }");
		out.println(".Pattern_Syntax       { background-color: #FFCCFF }");
		
		out.println("th           { text-align: left }");
		out.println("-->");
		out.println("</style></head><body><table>");
		*/
		htmlOut.println("<table border='1' cellpadding='2' cellspacing='0'>");
		htmlOut2.println("<html><body><table border='1' cellpadding='2' cellspacing='0'>");

		for (int scriptCode = 0; scriptCode < coreChars.length; ++scriptCode) {
			if (scriptCode == COMMON_SCRIPT
					|| scriptCode == INHERITED_SCRIPT)
				continue;
			showCodes(htmlOut, textOut, scriptCode, htmlOut2);
		}
		showCodes(htmlOut, textOut, COMMON_SCRIPT, htmlOut2);
		showCodes(htmlOut, textOut, INHERITED_SCRIPT, htmlOut2);

		showCodes(htmlOut, textOut, non_spacing);
		htmlOut.println("</table></body></html>");
		htmlOut.close();
		htmlOut2.println("</table></body></html>");
		htmlOut2.close();
		bf.setMergeRanges(false);

		textOut.println();
		textOut.println("# *** ADDITIONAL WORD CHARACTERS ***");
		textOut.println();
		bf.setValueSource("word-chars");
		bf.showSetNames(textOut, wordChars);
		
		textOut.println();
		textOut.println("# *** FOR REVIEW ***");
		bf.setLabelSource(UnicodeLabel.NULL);
		for (Iterator it = new TreeSet(suspect.getAvailableValues()).iterator(); it.hasNext();) {
			textOut.println();
			String value = (String)it.next();
			bf.setValueSource(value);
			bf.showSetNames(textOut, suspect.getSet(value));
		}
		textOut.close();
		textOut = BagFormatter.openUTF8Writer(GEN_DIR, "idn_vs_cfnfkcid.txt");
		bf = new BagFormatter();
		bf.setUnicodePropertyFactory(ups);
		textOut.println();
		textOut.println("# *** Comparison of IDN with CF_NFKC_ID (case-folded, NFKC, XID), U3.2 only ***");
		UnicodeSet U32 = ups32.getSet("gc=cn").complement();
		UnicodeSet CF_NFKC_ID = new UnicodeSet(xid_continue).retainAll(isNFKC).retainAll(isCaseFolded).retainAll(U32);		
		bf.showSetDifferences(textOut, "CF_NFKC_ID", CF_NFKC_ID, "IDN", idnaTypeSet[OK]);
		textOut.close();

	}
	
	/**
	 * 
	 */
	private void showScriptToBlock() {
		UnicodeMap scripts = ToolUnicodePropertySource.make("").getProperty("script").getUnicodeMap();
		UnicodeMap blocks = ToolUnicodePropertySource.make("").getProperty("block").getUnicodeMap();
		UnicodeMap.Composer myCompose = new UnicodeMap.Composer() {
			public Object compose(int codePoint, Object a, Object b) {
				return a + "\t" + b;
			}
		};
		UnicodeMap sb = ((UnicodeMap)scripts.cloneAsThawed()).composeWith(blocks, myCompose);
		for (Iterator it = sb.getAvailableValues(new TreeSet()).iterator(); it.hasNext();) {
			System.out.println(it.next());
		}
		throw new IllegalArgumentException();
	}
	
	Map scriptToGif = CollectionUtilities.asMap(script_to_gif);
	
	static String[][] script_to_gif = {
			
		{"Common","common.gif"}, //Miscellaneous_Symbols
		{"Inherited","combiningdiacritics.gif"}, //Combining_Diacritical_Marks
		{"Arabic","arabic.gif"}, //Arabic
		{"Armenian","armenian.gif"}, //Armenian
		{"Bengali","bengali.gif"}, //Bengali
		{"Bopomofo","bopomofo.gif"}, //Bopomofo
		{"Braille","braillesymbols.gif"}, //Braille_Patterns
		{"Buginese","buginese.gif"}, //Buginese
		{"Buhid","buhid.gif"}, //Buhid
		{"Canadian_Aboriginal","canadiansyllabics.gif"}, //Unified_Canadian_Aboriginal_Syllabics
		{"Cherokee","cherokee.gif"}, //Cherokee
		{"Coptic","coptic.gif"}, //Coptic
		{"Cypriot","cypriot.gif"}, //Cypriot_Syllabary
		{"Cyrillic","cyrillic.gif"}, //Cyrillic
		{"Deseret","deseret.gif"}, //Deseret
		{"Devanagari","devanagari.gif"}, //Devanagari
		{"Ethiopic","ethiopic.gif"}, //Ethiopic
		{"Georgian","georgian.gif"}, //Georgian
		{"Glagolitic","glagolitic.gif"}, //Glagolitic
		{"Gothic","gothic.gif"}, //Gothic
		{"Greek","greek.gif"}, //Greek_and_Coptic
		{"Gujarati","gujarati.gif"}, //Gujarati
		{"Gurmukhi","gurmukhi.gif"}, //Gurmukhi
		{"Han","cjkideographcompat.gif"}, //CJK_Compatibility_Ideographs
		{"Han","kangxiradicals.gif"}, //Kangxi_Radicals
		{"Hangul","hangulsyllables.gif"}, //Hangul_Syllables
		{"Hanunoo","hanunoo.gif"}, //Hanunoo
		{"Hebrew","hebrew.gif"}, //Hebrew
		{"Hiragana","hiragana.gif"}, //Hiragana
		{"Kannada","kannada.gif"}, //Kannada
		{"Katakana","katakana.gif"}, //Katakana
		{"Kharoshthi","kharoshthi.gif"}, //Kharoshthi
		{"Khmer","khmer.gif"}, //Khmer
		{"Lao","lao.gif"}, //Lao
		{"Latin","latin.gif"}, //Basic_Latin
		{"Limbu","limbu.gif"}, //Limbu
		{"Linear_B","linearbsyllabary.gif"}, //Linear_B_Syllabary
		{"Malayalam","malayalam.gif"}, //Malayalam
		{"Mongolian","mongolian.gif"}, //Mongolian
		{"Myanmar","myanmar.gif"}, //Myanmar
		{"New_Tai_Lue","newtailu.gif"}, //New_Tai_Lue
		{"Ogham","ogham.gif"}, //Ogham
		{"Old_Italic","olditalic.gif"}, //Old_Italic
		{"Old_Persian","oldpersiancuneiform.gif"}, //Old_Persian
		{"Oriya","oriya.gif"}, //Oriya
		{"Osmanya","osmanya.gif"}, //Osmanya
		{"Runic","runic.gif"}, //Runic
		{"Shavian","shavian.gif"}, //Shavian
		{"Sinhala","sinhala.gif"}, //Sinhala
		{"Syloti_Nagri","silotinagri.gif"}, //Syloti_Nagri
		{"Syriac","syriac.gif"}, //Syriac
		{"Tagalog","tagalog.gif"}, //Tagalog
		{"Tagbanwa","tagbanwa.gif"}, //Tagbanwa
		{"Tai_Le","taile.gif"}, //Tai_Le
		{"Tamil","tamil.gif"}, //Tamil
		{"Telugu","telugu.gif"}, //Telugu
		{"Thaana","thaana.gif"}, //Thaana
		{"Thai","thai.gif"}, //Thai
		{"Tibetan","tibetan.gif"}, //Tibetan
		{"Tifinagh","tifinagh.gif"}, //Tifinagh
		{"Ugaritic","ugaritic.gif"}, //Ugaritic
		{"Yi","yi.gif"}, //Yi_Syllables

	};
	
	UnicodeSet idnaTypeSet[] = new UnicodeSet[IDNA_TYPE_LIMIT];
	{
		for (int i = 0; i < idnaTypeSet.length; ++i) idnaTypeSet[i] = new UnicodeSet();
	}
	static final int OK = 0, DELETED = 1, ILLEGAL = 2, REMAPPED = 3, IDNA_TYPE_LIMIT = 4;
	/**
	 * 
	 */
	static public int getIDNAType(int cp) {
		inbuffer.setLength(0);
		UTF16.append(inbuffer, cp);
		try {
			intermediate = IDNA.convertToASCII(inbuffer,
					IDNA.DEFAULT); // USE_STD3_RULES
			if (intermediate.length() == 0)
				return DELETED;
			outbuffer = IDNA.convertToUnicode(intermediate,
					IDNA.USE_STD3_RULES);
		} catch (StringPrepParseException e) {
			return ILLEGAL;
		} catch (Exception e) {
			System.out.println("Failure at: " + Utility.hex(cp));
			return ILLEGAL;
		}
		if (!TestData.equals(inbuffer, outbuffer))
			return REMAPPED;
		return OK;
	}
	static StringBuffer inbuffer = new StringBuffer();
	static StringBuffer intermediate, outbuffer;

	UnicodeSet lowercase = new UnicodeSet("[:Lowercase:]");

	/**
	 * @param htmlOut
	 * @param textOut TODO
	 * @param scriptCode
	 * @param htmlOut2 TODO
	 * @param ucd
	 * @param coreChars
	 * @param decompChars
	 */
	private void showCodes(PrintWriter htmlOut, PrintWriter textOut, int scriptCode, PrintWriter htmlOut2) {
		if (coreChars[scriptCode] == null) return;
		String script = Default.ucd().getScriptID_fromIndex((byte) scriptCode);
		script = Utility.getUnskeleton(script.toLowerCase(),true);
		System.out.println(script);
		
		htmlOut.println();
		String scriptLine = "<tr><th class='script'><img src='images/" + ((String)scriptToGif.get(script)).toLowerCase()
		+ "'> Script: " + script + "</th></tr>";
		htmlOut.println(scriptLine);
		htmlOut2.println(scriptLine);
		textOut.println();
		textOut.println("#*** Script: " + script + " ***");
		UnicodeSet core = new UnicodeSet(coreChars[scriptCode]);
		
		UnicodeSet deleted = extract(idnaTypeSet[DELETED], core);
		UnicodeSet illegal = extract(idnaTypeSet[ILLEGAL], core);
		UnicodeSet remapped = extract(idnaTypeSet[REMAPPED], core);
		
		UnicodeSet remappedIsNFKC = extract(isNFKC, remapped);
		UnicodeSet remappedIsNFKCDecomp = extract(decomposable, remappedIsNFKC);
		
		UnicodeSet decomp = extract(decomposable, core);
		UnicodeSet pattern = extract(patternProp, core);
		UnicodeSet non_id = extract(not_xid_continue, core);
		
		UnicodeSet bicameralNoupper = new UnicodeSet();
		if (!hasNoUpper.containsAll(core)) {
			bicameralNoupper = extract(hasNoUpperMinus, core);
		}

		UnicodeSet foo = new UnicodeSet(bicameralNoupper).addAll(non_id);
		for (UnicodeSetIterator it = new UnicodeSetIterator(foo); it.next(); ) {
			String cat = Default.ucd().getCategoryID(it.codepoint);
			String name = Default.ucd().getName(it.codepoint);
			if (name.indexOf("MUSICAL SYMBOL") >= 0 
					|| name.indexOf("DINGBA") >= 0 
					|| name.indexOf("RADICAL ") >= 0 
					 						) cat = "XX";
			suspect.put(it.codepoint, cat);
		}
		
		if (core.size() != 0) printlnSet(htmlOut, textOut, script, "Atomic", core, scriptCode, uca);
		if (bicameralNoupper.size() != 0) printlnSet(htmlOut, textOut, script, "Atomic-no-uppercase", bicameralNoupper, scriptCode, uca);
		if (pattern.size() != 0) printlnSet(htmlOut, textOut, script, "Pattern_Syntax", pattern, scriptCode, uca);
		if (non_id.size() != 0) printlnSet(htmlOut, textOut, script, "Non-XID", non_id, scriptCode, uca);
		if (decomp.size() != 0) printlnSet(htmlOut, textOut, script, "NFD-Decomposable", decomp, scriptCode, uca);

		if (remappedIsNFKC.size() != 0) printlnSet(htmlOut, textOut, script, "IDN-Remapped-Case-Atomic", remappedIsNFKC, scriptCode, uca);
		if (remappedIsNFKCDecomp.size() != 0) printlnSet(htmlOut, textOut, script, "IDN-Remapped-Case-NFD-Decomposable", remappedIsNFKCDecomp, scriptCode, uca);
		if (remapped.size() != 0) printlnSet(htmlOut, textOut, script, "IDN-Remapped-Compat", remapped, scriptCode, uca);
		if (deleted.size() != 0) printlnSet(htmlOut, textOut, script, "IDN-Deleted", deleted, scriptCode, uca);
		if (illegal.size() != 0) printlnSet(htmlOut, textOut, script, "IDN-Prohibited", illegal, scriptCode, uca);
	}
	
	private void showCodes(PrintWriter htmlOut, PrintWriter textOut, UnicodeSet uset) throws IOException {
		String script = Default.ucd().getScriptID_fromIndex((byte) INHERITED_SCRIPT);
		script = Utility.getUnskeleton(script.toLowerCase(),true);
		String scriptLine = "<tr><th class='script'><img src='images/" 
			+ ((String)scriptToGif.get(script)).toLowerCase()
			+ "'> Script: " + script + "</th></tr>";
		htmlOut.println(scriptLine);
		UnicodeMap m = getPositions();
		
		for (Iterator it = m.getAvailableValues(new TreeSet(uca)).iterator(); it.hasNext(); ) {
			String type = (String) it.next();
			UnicodeSet current = m.getSet(type).retainAll(non_spacing);
			if (current.size() == 0) continue;
			printlnSet(htmlOut, textOut, script, "Visible_Combining_Marks_" + type, current, INHERITED_SCRIPT, positionComparator);
		}
	}
	
	/**
	 * @throws IOException
	 * 
	 */
	private UnicodeMap getPositions() throws IOException {
		UnicodeMap result = new UnicodeMap();
		BufferedReader in = bf.openUTF8Reader("C:\\DATA\\confusables\\", "positions.txt");
		String type="Undetermined";
		while (true) {
			String line = Utility.readDataLine(in);
			if (line == null) break;
			if (line.length() == 0) continue;
			if (line.startsWith("@")) {
				type = line.substring(1);
				continue;
			}
			String[] pieces = Utility.split(line, ';');
			String code = Utility.fromHex(pieces[0]);
			result.put(UTF16.charAt(code,0), type);
		}
		return result;
	}

	static Comparator positionComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return Default.ucd().getName(s1).compareTo(Default.ucd().getName(s2));
		}
	};

	/**
	 * 
	 */
	private UnicodeSet extract(UnicodeSet other, UnicodeSet core) {
		UnicodeSet decomp = new UnicodeSet(core).retainAll(other);
		core.removeAll(decomp);
		return decomp;
	}

	/**
	 * @param htmlOut
	 * @param textOut TODO
	 * @param script TODO
	 * @param unicodeset
	 * @param scriptCode
	 * @param comparator TODO
	 * @param uca
	 */
	private  void printlnSet(PrintWriter htmlOut, PrintWriter textOut,
			String script, String title, UnicodeSet unicodeset, int scriptCode, Comparator comparator) {
		if (unicodeset == null)
			return;
		int size = unicodeset.size();
		String dir = unicodeset.containsSome(bidiR)
				&& unicodeset.containsNone(bidiL) ? " dir='rtl'" : "";
		htmlOut.println("<tr><th class='" + title + "'><a href='#" +
				title + "'>" + title + "</a> ("
				+ TestData.nf.format(size) + ")</th></tr>");
		htmlOut.print("<tr><td class='" + title + "'" + dir + ">");
		// <a href="#Atomic">categorization</a>
		textOut.println();
		textOut.println("# " + title);
		bf.setValueSource(script + " ; " + title);
		UnicodeSetIterator usi = new UnicodeSetIterator();
		if (scriptCode == HAN_SCRIPT || scriptCode == HANGUL_SCRIPT) {
			usi.reset(unicodeset);
			while (usi.nextRange()) {
				if (usi.codepoint == usi.codepointEnd) {
					htmlOut.print(formatCode(UTF16
							.valueOf(usi.codepoint)));
				} else {
					htmlOut.print(formatCode(UTF16
							.valueOf(usi.codepoint))
							+ ".. "
							+ formatCode(UTF16
									.valueOf(usi.codepointEnd)));
				}
			}
			bf.showSetNames(textOut, unicodeset);
		} else {
			Set reordered = new TreeSet(comparator);
			usi.reset(unicodeset);
			while (usi.next()) {
				String x = usi.getString();
				boolean foo = reordered.add(x);
				if (!foo)
					throw new IllegalArgumentException("Collision with "
							+ Default.ucd().getCodeAndName(x));
			}
			for (Iterator it = reordered.iterator(); it.hasNext();) {
				Object key = it.next();
				htmlOut.print(formatCode((String)key));
			}
			bf.showSetNames(textOut, reordered);
		}
		htmlOut.println("</td></tr>");
	}

	/**
	 * @param string
	 * @return
	 */
	private String formatCode(String string) {
		int cat = ucd.getCategory(UTF16.charAt(string,0));
		String pad = "\u00A0", pad1 = pad;
		if (cat == Me || cat == Mn) {
			pad = "\u00A0\u00A0";
			pad1 = "\u00A0\u00A0\u25cc";
		}	
		return "<span title='" + ucd.getCodeAndName(string) + "'>"
		+ pad1
		+ TransliteratorUtilities.toHTMLControl.transliterate(string)
		+ pad
		+ "</span> ";
	}
}