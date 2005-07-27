/*
 *******************************************************************************
 * Copyright (C) 2002-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.IOException;

import com.ibm.icu.text.Transliterator;

public class TransliteratorUtilities {
	public static boolean DEBUG = false;
	
	public static void registerTransliteratorFromFile(String dir, String id) {
		try {
			String filename = id.replace('-', '_') +  ".txt";
			String rules = getFileContents(dir, filename);
			Transliterator t;
			int pos = id.indexOf('-');
			String rid;
			if (pos < 0) {
				rid = id + "-Any";
				id = "Any-" + id;
			} else {
				rid = id.substring(pos+1) + "-" + id.substring(0, pos);
			}
			t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
			Transliterator.unregister(id);
			Transliterator.registerInstance(t);

			/*String test = "\u049A\u0430\u0437\u0430\u049B";
			System.out.println(t.transliterate(test));
			t = Transliterator.getInstance(id);
			System.out.println(t.transliterate(test));
			*/

			t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
			Transliterator.unregister(rid);
			Transliterator.registerInstance(t);
			if (DEBUG) System.out.println("Registered new Transliterator: " + id + ", " + rid);
		} catch (IOException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Can't open " + dir + ", " + id).initCause(e);
		}
	}

	/**
	 * 
	 */
	public static String getFileContents(String dir, String filename) throws IOException {
		BufferedReader br = BagFormatter.openUTF8Reader(dir, filename);
		StringBuffer buffer = new StringBuffer();
		while (true) {
			String line = br.readLine();
			if (line == null) break;
			if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
			buffer.append(line).append("\r\n");
		}
		br.close();
		return buffer.toString();
	}
}