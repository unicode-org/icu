package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodePropertySource;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.text.utility.Utility;
import com.ibm.text.utility.Utility.Encoding;

public class MakeNamesChart {
	
	static int lastCodePoint = -1;
	static boolean lastCodePointIsOld = false;
	static int lastDecompType = UCD.NONE;
	
	static final String chartPrefix = "c_";
	static final String namePrefix = "n_";
	
	static UnicodeSet skipChars;// = new UnicodeSet("[[:gc=cn:]-[:noncharactercodepoint:]]");
	static UnicodeSet rtl;// = new UnicodeSet("[[:bidiclass=r:][:bidiclass=al:]]");
	static UnicodeSet usePicture;// = new UnicodeSet("[[:whitespace:][:defaultignorablecodepoint:]]");
	
	static UCD ucd41;

	public static void main(String[] args) throws Exception {
		//ConvertUCD.main(new String[]{"5.0.0"});
		BlockInfo blockInfo = new BlockInfo("5.0.0", "NamesList.txt");
		// http://www.unicode.org/~book/incoming/kenfiles/U50M051010.lst
		Default.setUCD("5.0.0");
		ucd41 = UCD.make("4.1.0");
		ToolUnicodePropertySource up = ToolUnicodePropertySource.make("5.0.0");
		skipChars = new UnicodeSet(up.getSet("gc=cn")).removeAll(up.getSet("gc=cn"));
		//"[[:gc=cn:]-[:noncharactercodepoint:]]");
		rtl = new UnicodeSet(up.getSet("bidiclass=r")).addAll(up.getSet("bidiclass=al"));// "[[:bidiclass=r:][:bidiclass=al:]]");
		usePicture = new UnicodeSet(up.getSet("whitespace=true")).addAll(up.getSet("defaultignorablecodepoint=true"));// new UnicodeSet("[[:whitespace:][:defaultignorablecodepoint:]]");

		List nameList = new ArrayList();
		ArrayList lines = new ArrayList();
		UnicodeSet collectedCodePoints = new UnicodeSet();
		BitSet nameListNew = new BitSet();

		int limit = Integer.MAX_VALUE;
		for (int count = 0; count < limit; ++count) {
			if (!blockInfo.next(lines)) break;
			String firstLine = (String)lines.get(0);
			if (firstLine.startsWith("@@@")) continue;
			String[] lineParts = firstLine.split("\t");
			String fileName = lineParts[1] + ".html";
			nameList.add(firstLine);
			System.out.println();
			System.out.println("file: " + chartPrefix + fileName);
			PrintWriter out = BagFormatter.openUTF8Writer("C:/DATA/GEN/charts/namelist/", chartPrefix + fileName);
			out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><title>" +
					TransliteratorUtilities.toHTML.transliterate(getHeading(lineParts[2])) +
					"</title><link rel='stylesheet' type='text/css' href='namelist.css'>" +
					"<base target='names'></head><body>");

			// header
			out.println("<table class='headerTable'><tr><td class='headerLeft'>" +
					lineParts[1] + 
					" <a href='help.html'>help</a></td><td class='headerCenter'>" +				
					getHeading(lineParts[2]) +
					"</td><td class='headerRight'><a href='mainList.html'>index</a> " +
					lineParts[3] +
					"</td></tr></table>");

			if ("Unassigned".equals(lineParts[2])) {
				System.out.println("debug");
			}
			// first pass through and collect all the code points
			collectedCodePoints.clear();
			for (int i = 1; i < lines.size(); ++i) {
				String line = (String)lines.get(i);
				int cp1 = line.charAt(0);
				if (cp1 != '@' && cp1 != '\t') {
					int cp = Integer.parseInt(line.split("\t")[0],16);
					collectedCodePoints.add(cp);
				}
			}
			collectedCodePoints.removeAll(skipChars);
			if (collectedCodePoints.size() == 0) {
				out.println("<p align='center'>No Names List</p>");
			} else {
				out.println("<div align='center'><table class='chart'><tr>");
				int counter = 0;
				for (UnicodeSetIterator it = new UnicodeSetIterator(collectedCodePoints); it.next();) {
					if ((counter % 16) == 0 && counter != 0) {
						out.println("</tr><tr>");
					}
					String tdclass = "cell";
					if (counter < 16) tdclass = "cellw";
					if (it.codepoint == 0x242) {
						System.out.println("debug");
					}
					boolean isNew = isNew(it.codepoint);
					if (isNew) tdclass += "new";
					String hexcp = Utility.hex(it.codepoint, 4);
					String title = "";
					String name = Default.ucd().getName(it.codepoint);
					if (name != null) title = " title='" + TransliteratorUtilities.toHTML.transliterate(name.toLowerCase()) + "'";
					out.println("<td class='" + tdclass + "'"
							+ title
							+ ">\u00A0"
							+ showChar(it.codepoint) + "\u00A0<br><tt><a href='" + namePrefix + fileName + "#"+ hexcp + "'>" + 
							hexcp + "</a></tt></td>");
					counter++;
				}
				if (counter > 16) {
					counter &= 0xF;
					if (counter != 0) for (; counter < 16; ++counter) out.println("<td class='cell'>\u00A0</td>");
					out.println("</tr></table></div>");
				}
			}
			out.close();
			out = BagFormatter.openUTF8Writer("C:/DATA/GEN/charts/namelist/", namePrefix + fileName);
			out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" +
					"<link rel='stylesheet' type='text/css' href='namelist.css'></head><body>");

			// now do the characters
			boolean inTable = false;
			for (int i = 1; i < lines.size(); ++i) {
				String line = (String)lines.get(i);
				try {
					if (line.startsWith("@")) {
						finishItem(out);
						if (inTable) {
							out.println("</table>");
							inTable = false;
						}
						if (line.startsWith("@+")) {
							line = line.substring(2).trim();
							out.println("<p class='comment'>"
									+ line
									+ "</p>");
						} else {
							line = line.substring(1).trim();
							out.println("<h2>"
									+ line
									+ "</h2>");
						}
					} else {
						if (!inTable) {
							out.println("<table>");
							inTable = true;
						}
						//String line2 = lineParts[1];
						if (line.startsWith("\t")) {
							String body = line.trim();
							if (false && line.indexOf(body) != 1) {
								System.out.println("Format error: too much inital whitespace: <" + line + ">");
							}
							char firstChar = body.charAt(0);
							switch (firstChar) {
							case '*': body = "\u2022 " + body.substring(2); break;
							case ':': body = checkCanonical(lastCodePoint, body); break;
							case '#': body = checkCompatibility(lastCodePoint, body); break;
							case 'x': body = getOther(body); break;
							case '=': break;
							default: throw new IllegalArgumentException("Huh? " + body);
							}  
							out.println("<tr><td>\u00A0</td><td>\u00A0</td><td>"
									+ maybeNameStyle(showTextConvertingHex(body, firstChar != '='), firstChar == '=')
									+ "</td></tr>");
						} else {
							finishItem(out);
							lineParts = line.split("\t");
							String x = lineParts[0];
							lastCodePoint = Integer.parseInt(x,16);
							boolean lastCodePointIsNew = isNew(lastCodePoint);
							if (lastCodePointIsNew) nameListNew.set(nameList.size()-1, true);
							out.println("<tr><td" 
									+ (lastCodePointIsNew ? " class='new'" : "")
									+ "><code><a name='" + x + "'>" + x + "</a></code></td><td>\u00A0"
									+ showChar(lastCodePoint) + "\u00A0</td><td"
									+ (lastCodePointIsNew ? " class='new'" : "") + ">"
									+ nameStyle(showTextConvertingHex(lineParts[1], false)) + "</td></tr>");
							lastDecompType = Default.ucd().getDecompositionType(lastCodePoint);
						}
					}
				} catch (Exception e) {
					throw (IllegalArgumentException) new IllegalArgumentException("Error on line: " + line)
					.initCause(e);
				}
			}
			finishItem(out);
			out.close();
		}
		blockInfo.in.close();
		PrintWriter out = BagFormatter.openUTF8Writer("C:/DATA/GEN/charts/namelist/", "mainList.html");
		out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" +
				"<title>Main List</title><link rel='stylesheet' type='text/css' href='namelist.css'>" +
				"<base target='chart'></head><body><table>");
		for (int i = 0; i < nameList.size(); ++i) {
			String line = (String) nameList.get(i);
			String[] lineParts = line.split("\t");
			String fileName = lineParts[1] + ".html";
			out.println("<tr><td><code>" + lineParts[1] +
					"</code></td><td"
					+ (nameListNew.get(i) ? " class='new'" : "") 
					+ "><a href='" + chartPrefix + fileName + "'>" + getHeading(lineParts[2]) + "</a></td><td><code>" +
					lineParts[3] +"</code></td></tr>");
		}
		out.println("</table></body></html>");
		out.close();
		BagFormatter bf = new BagFormatter();
		//System.out.println(bf.showSetDifferences("Has name in decomps", hasName, "Has no name in decomps", hasNoName));
		System.out.println("Name differences: Canonical");
		showNameDifferences(hasNameCan, hasNoNameCan);
		System.out.println("Name differences: Compatibility");
		showNameDifferences(hasNameComp, hasNoNameComp);
//		System.out.println("Characters with names in decomps: " + hasName.toPattern(true));
//		System.out.println("Characters without names in decomps: " + hasNoName.toPattern(true));
//		System.out.println("Characters sometimes with, sometimes without names in decomps: " + both.toPattern(true));
		System.out.println("Done");
	}

	private static boolean isNew(int codepoint) {
		return Default.ucd().isAllocated(codepoint) && !ucd41.isAllocated(codepoint);
	}

	private static void showNameDifferences(Map hasName, Map hasNoName) {
		Set both = new TreeSet(hasNoName.keySet());
		both.retainAll(hasName.keySet());
		//hasNoName.removeAll(both);
		//hasName.removeAll(both);
		for (Iterator it = both.iterator(); it.hasNext();) {
			String decomp = (String) it.next();
			System.out.println();
			System.out.println("decomp: " + Utility.hex(decomp));
			System.out.println("Has name in: " + Utility.hex((String)hasName.get(decomp)));
			System.out.println("Has no name in: " + Utility.hex((String)hasNoName.get(decomp)));
		}
		System.out.println("Count: " + both.size());
	}
	
	static TestIdentifiers ti;
	static {
		try {
			ti = new TestIdentifiers("L");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void finishItem(PrintWriter out) {
		if (lastCodePoint < 0) return;
		if (lastDecompType != UCD.NONE) {
			System.out.println("Alert: missing decomp for " + Utility.hex(lastCodePoint));
		}
		String str = UTF16.valueOf(lastCodePoint);
		String upper = showForm(out, str, null, null, Default.ucd().getCase(str,UCD.FULL,UCD.UPPER), "\u2191");
		showForm(out, str, upper, null, Default.ucd().getCase(str,UCD.FULL,UCD.TITLE), "\u2195");
		String lower = showForm(out, str, null, null, Default.ucd().getCase(str,UCD.FULL,UCD.LOWER), "\u2193");
		showForm(out, lower, null, null, Default.ucd().getCase(str,UCD.FULL,UCD.FOLD), "\u2194");
		
		String dc = Default.ucd().getDecompositionMapping(lastCodePoint);
		String nfd = showForm(out, dc, str, null, Default.nfd().normalize(lastCodePoint), "\u21DB");
		//String nfc = showForm(out, dc, null, Default.nfc().normalize(lastCodePoint), "\u21DB");
		String nfkd = showForm(out, dc, str, nfd, Default.nfkd().normalize(lastCodePoint), "\u21DD");
		
		if (nfkd.equals(str)) {
			Set s = ti.getConfusables(lastCodePoint, "MA");
			if (s.size() > 1) {
				sortedSet.clear();
				for (Iterator it = s.iterator(); it.hasNext();) {
					sortedSet.add(Default.nfkd().normalize((String)it.next()));
				}
				sortedSet.remove(nfkd); // remove me
				for (Iterator it = sortedSet.iterator(); it.hasNext();) {
					String other = (String)it.next();
					if (nfkd.equals(Default.nfkd().normalize(other))) continue;
					out.println("<tr><td>\u00A0</td><td>\u00A0</td><td class='conf'>\u279F\u00A0"
							+ showTextConvertingHex(Utility.hex(other, 4, " + "), true)
							+ " "
							+ Default.ucd().getName(other, UCD.NORMAL, " + ").toLowerCase()
							// maybeNameStyle(showTextConvertingHex(upper, firstChar != '='), firstChar == '=')
							+ "</td></tr>");
				}
			}
		}
		lastCodePoint = -1;
	}
	
	static Set sortedSet = new TreeSet(Collator.getInstance(ULocale.ENGLISH));

	private static String showForm(PrintWriter out, String str, String str2, String str3, String transformed, String symbol) {
		if (!transformed.equals(str) && !transformed.equals(str2) && !transformed.equals(str3)) {
			out.println("<tr><td>\u00A0</td><td>\u00A0</td><td class='c'>" + symbol + "\u00A0"
				+ showTextConvertingHex(Utility.hex(transformed, 4, " + "), true)
				+ (UTF16.countCodePoint(transformed) != 1 ? "" : 
					" " + Default.ucd().getName(transformed, UCD.NORMAL, " + ").toLowerCase())
				// maybeNameStyle(showTextConvertingHex(upper, firstChar != '='), firstChar == '=')
				+ "</td></tr>");
		}
		return transformed;
	}
	
	static public String getHeading(String name) {
		int pos = name.lastIndexOf(" (");
		if (pos < 0) return name;
		return name.substring(0, pos);
	}
	
	private static String maybeNameStyle(String string, boolean b) {
		if (b && string.equals(string.toUpperCase(Locale.ENGLISH))) return nameStyle(string);
		return string;
	}


	private static String nameStyle(String string) {
		// TODO Auto-generated method stub
		String result = "<i>" + Default.ucd().getCase(string, UCD.FULL, UCD.TITLE) + "</i>";
		// if it has any &xxx;, then restore them.
		int position = 0;
		while (true) {
			if (!escapeMatch.reset(result).find(position)) break;
			int start = escapeMatch.start();
			position = escapeMatch.end();
			result = result.substring(0,start) 
			+ result.substring(start, position).toLowerCase() 
			+ result.substring(position);
		}
		return result;
	}

	static Matcher escapeMatch = Pattern.compile("\\&[A-Z][a-z]*\\;").matcher("");
	
	private static String showTextConvertingHex(String body, boolean addCharToHex) {
		body = TransliteratorUtilities.toHTML.transliterate(body);
		if (addCharToHex) {
			int position = 0;
			while (position < body.length()) {
				if (!findHex.reset(body).find(position)) break;
				position = findHex.end();
				int start = findHex.start();
				int len = position - start;
				if (len < 4 || len > 6) continue;
				int cp = Integer.parseInt(findHex.group(),16);
				if (cp > 0x10FFFF) continue;
				String insert = "\u00A0" + showChar(cp);
				String beginning = body.substring(0,start)
					+ "<code>" + body.substring(start, position) + "</code>"
					+ insert;
				body = beginning + body.substring(position);
				position = beginning.length();
			}
		}
		return body;
	}
	
	static Matcher pointer = Pattern.compile("x \\((.*) - ([0-9A-F]+)\\)").matcher("");
	static Matcher pointer2 = Pattern.compile("x ([0-9A-F]{4,6})").matcher("");
	static Matcher findHex = Pattern.compile("[0-9A-F]+").matcher("");
	
	private static String getOther(String body) {
		// of form: 	x (hyphenation point - 2027)
		// => arrow 2027 X hyphenation point
		int cp;
		String name = null;
		if (pointer.reset(body).matches()) {
			cp = Integer.parseInt(pointer.group(2),16);
			name = pointer.group(1);
			String name2 = Default.ucd().getName(cp);
			if (name2 == null) name2 = "<not a character>";
			if (!name.equalsIgnoreCase(name2)) {
				System.out.println("Mismatch in name for " + body + " in " + Utility.hex(lastCodePoint));
				System.out.println("\tName is: " + name2);
			}
		} else if (pointer2.reset(body).matches()) {
			cp = Integer.parseInt(pointer2.group(1),16);
			// name = UCharacter.getName(cp).toLowerCase();
			// System.out.println("Irregular format: " + body);
		} else {
			throw new IllegalArgumentException("Bad format: " + body);
		}
		return "\u2192 " + Utility.hex(cp,4) /*+ " " + showChar(cp)*/ + (name != null ? " " + name : "");
	}
	
	static String showChar(int cp) {
		if (usePicture.contains(cp)) {
			int rep = '\u2588';
			if (cp <= 0x20) rep = 0x2400 + cp;
			else if (cp == 0x7F) rep = 0x2421;
			return "<span class='inv'>" + (char)rep + "</span>";
			//String hex = Utility.hex(cp);
			//return "<img alt='" + hex + "' src='http://www.unicode.org/cgi-bin/refglyph?24-" + hex + "'>";
		}
		
		int type = Default.ucd().getCategory(cp);
		if (type == UCD.Cn || type == UCD.Co || type == UCD.Cs) {
			return "\u2588";
		}
		String result = TransliteratorUtilities.toHTML.transliterate(UTF16.valueOf(cp));
		if (type == UCD.Me || type == UCD.Mn) {
			result = "\u25CC" + result;
		} else if (rtl.contains(cp)) {
			result = "\u200E" + result + "\u200E";
		}
		return result;
	}
	
	//static final UnicodeSet noname = new UnicodeSet("[[:ascii:][:ideographic:]]");
	static final Map hasNoNameCan = new TreeMap();
	static final Map hasNameCan = new TreeMap();
	static final Map hasNoNameComp = new TreeMap();
	static final Map hasNameComp = new TreeMap();

	private static String checkCanonical(int codePoint, String body) {
		body = body.substring(2);
		if (lastDecompType != UCD.CANONICAL) {
			System.out.println("Mismatching Decomposition Type: " + body + " in " + Utility.hex(codePoint));
		}
		String lastDecomp = Default.ucd().getDecompositionMapping(lastCodePoint);
		String hexed = Utility.hex(lastDecomp, 4, " ");
		String hexed2 = hexed;
		if (UTF16.countCodePoint(lastDecomp) == 1) {
			hexed2 += " " + Default.ucd().getName(lastDecomp).toLowerCase();
		}
		if (hexed.equalsIgnoreCase(body)) {
			hasNoNameCan.put(lastDecomp, UTF16.valueOf(codePoint));
		} else if (hexed2.equalsIgnoreCase(body)) {
			hasNameCan.put(lastDecomp, UTF16.valueOf(codePoint));
		} else {
			System.out.println("Mismatching Decomposition: " + body + " in " + Utility.hex(codePoint));
			System.out.println("\tShould be: " + hexed);
		}
		lastDecompType = UCD.NONE;
		return "\u2261 " + body;
	}

	private static String checkCompatibility(int codePoint, String body) {
		body = body.substring(2);
		if (lastDecompType <= UCD.CANONICAL) {
			System.out.println("Mismatching Decomposition Type: " + body + " in " + Utility.hex(codePoint));
		}
		String lastDecomp = Default.ucd().getDecompositionMapping(lastCodePoint);
		String hexed = Utility.hex(lastDecomp, 4, " ");
		if (lastDecompType != UCD.COMPAT_UNSPECIFIED) {
			String lastDecompID = Default.ucd().getDecompositionTypeID(lastCodePoint);
			hexed = "<" + lastDecompID + "> " + hexed;
		}
		String hexed2 = hexed;
		if (UTF16.countCodePoint(lastDecomp) == 1) {
			hexed2 += " " + Default.ucd().getName(lastDecomp).toLowerCase();
		}
		if (hexed.equalsIgnoreCase(body)) {
			hasNoNameComp.put(lastDecomp, UTF16.valueOf(codePoint));
		} else if (hexed2.equalsIgnoreCase(body)) {
			hasNameComp.put(lastDecomp, UTF16.valueOf(codePoint));
		} else {
			System.out.println("Mismatching Decomposition: " + body + " in " + Utility.hex(codePoint));
			System.out.println("\tShould be: " + hexed);
		}
		lastDecompType = UCD.NONE;
		return "\u2248 " + body;
	}

	static class BlockInfo {
		BufferedReader in;
		String lastLine;
		BlockInfo (String version, String filename) throws IOException {
			in = Utility.openUnicodeFile(filename, version, true, Utility.LATIN1_WINDOWS);
			//in = BagFormatter.openUTF8Reader(dir, filename);
		}
		boolean next(List inout) throws IOException {
			inout.clear();
			if (lastLine != null) {
				inout.add(lastLine);
				lastLine = null;
			}
			while (true) {
				String line = in.readLine();
				if (line == null) break;
				if (line.startsWith("@@\t")) {
					lastLine = line;
					break;
				}
				inout.add(line);
			}
			return inout.size() > 0;
		}

	}
}