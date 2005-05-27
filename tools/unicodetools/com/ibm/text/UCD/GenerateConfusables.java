/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateConfusables.java,v $
* $Date: 2005/05/27 21:40:51 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodePropertySource;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.Utility;

public class GenerateConfusables {
	static PrintWriter log;
	static final String ARROW = "\u2192";

	static class Data2 {
		String source;
		String target;
		int count;
		Data2(String target, int count) {
			this.target = target;
			this.count = count;
		}
	}
	
	static ToolUnicodePropertySource ups = ToolUnicodePropertySource.make("");
	static UnicodeSet skipSet = ups.getSet("gc=Cn").addAll(ups.getSet("gc=Co")).addAll(ups.getSet("gc=Cc")).addAll(ups.getSet("gc=Cf"));
	
	static class Data implements Comparable {
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
	
	static UnicodeSet controls = new UnicodeSet("[:Cc:]");
	
	static class DataSet {
		Set dataSet = new TreeSet();
		Map dataMap = new TreeMap(new ArrayComparator(new Comparator[] {new UTF16.StringComparator(), new UTF16.StringComparator()}));

		public DataSet add(String source, String target, String type, String errorLine) {
			if (skipSet.containsAll(source) || skipSet.containsAll(target)) return this;
			String nsource = Default.nfkd().normalize(source);
			String ntarget = Default.nfkd().normalize(target);
			
			// if it is just a compatibility match, return
			if (nsource.equals(ntarget)) return this;
			
			if (type.startsWith("confusables-")) type = type.substring("confusables-".length());
			if (type.endsWith(".txt")) type = type.substring(0,type.length() - ".txt".length());

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
			
			// swap order
			if (preferSecondAsSource(source, target)) {
				String temp = target;
				target = source;
				source = temp;
			}
			if (target.indexOf('\u203D') >= 0) type += "-skip";
			Data newData = new Data(source, target, type);
			return add(newData, errorLine);
		}
		/**
		 * @param errorLine TODO
		 * 
		 */
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
		// Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt"
		static final int NORMAL = 0, FOLDING = 1, OLD = 2;
		
		public DataSet addFile(String directory, String filename) throws IOException {
			BufferedReader in = BagFormatter.openUTF8Reader(directory, filename);
			int kind = NORMAL;
			if (filename.indexOf("Folding") >= 0) kind = FOLDING;
			else if (false && filename.indexOf("-old") >= 0) kind = OLD;
			while (true) {
				String line = Utility.readDataLine(in);
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
						add(source, target, type, line);
					}
				} else if (kind == OLD) {
					String target = pieces[0].trim();
					for (int i = 1; i < pieces.length; ++i) {
						add(pieces[i].trim(), target, type, line);
					}
				} else {
					String source = Utility.fromHex(pieces[0].trim(),true);
					String target = Utility.fromHex(pieces[1].trim(),true);
					if (pieces.length > 2) type = pieces[2].trim();
					add(source, target, type, line);
				}
			}
			in.close();
			return this;			
		}
		public void write(String directory, String filename, boolean appendFile) throws IOException {
			PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
			if (appendFile) {
				String[] replacements = {"%date%", Default.getDate()};
				Utility.appendFile("com/ibm/text/UCD/confusablesHeader.txt", 
						Utility.UTF8_WINDOWS, out, replacements);
			}
			for (Iterator it = dataSet.iterator(); it.hasNext();) {
				Data item = (Data) it.next();
				out.println(
						Utility.hex(item.source)
						+ " ;\t" + Utility.hex(item.target)
						+ " ;\t" + item.type
						+ "\t# "
						+ "( " + item.source + " " + ARROW + " " + item.target + ") " 
						+ Default.ucd().getName(item.source) + " " + ARROW + " "
						+ Default.ucd().getName(item.target));

			}
			out.close();
		}
		/**
		 * 
		 */
		public void add(DataSet ds) {
			for (Iterator it = ds.dataSet.iterator(); it.hasNext();) {
				add((Data)it.next(), "");
			}
		}
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
		/**
		 * 
		 */
		private void remove(Data already) {
			String[] key = {already.source, already.target};
			dataMap.remove(key);
			dataSet.remove(already);
		}
	}
	public static void main(String[] args) throws IOException {
		String indir = Utility.BASE_DIR + "confusables/";
		String outdir = Utility.GEN_DIR + "confusables/";
		log = BagFormatter.openUTF8Writer(outdir, "log.txt");
		//fixMichel(indir, outdir);
		generateConfusables(indir, outdir);
		log.close();
		System.out.println("Done");
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
	private static void generateConfusables(String indir, String outdir) throws IOException {
		File dir = new File(indir);
		String[] names = dir.list();
		DataSet total = new DataSet();
		for (int i = 0; i < names.length; ++i) {
			if (new File(indir + names[i]).isDirectory()) continue;
			System.out.println(names[i]);
			DataSet ds = new DataSet();
			ds.addFile(indir, names[i]);
			ds.write(outdir, "new-" + names[i], false);
			total.add(ds);
		}
		total.write(outdir, "confusables-raw.txt", false);
		DataSet clean = total.clean();
		clean.write(outdir, "confusables.txt", true);		
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
	private static void add(Map m, String source, String target, int count) {
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
	
	static private boolean preferSecondAsSource(String a, String b) {
		// if first is longer, prefer second
		int ca = UTF16.countCodePoint(a);
		int cb = UTF16.countCodePoint(b);
		if (ca != cb) {
			return ca > cb;
		}
		// if first is lower, prefer second
		return a.compareTo(b) < 0;
	}
	
	static String getCodeCharName(String a) {
		return Default.ucd().getCode(a) + "(  " + a + "  ) " + Default.ucd().getName(a);
	}
	
}