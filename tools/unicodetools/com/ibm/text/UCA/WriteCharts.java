/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/WriteCharts.java,v $ 
* $Date: 2002/06/02 05:07:08 $ 
* $Revision: 1.10 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

import java.util.*;

import java.io.*;
import com.ibm.text.UCD.*;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import java.text.SimpleDateFormat;

public class WriteCharts implements UCD_Types {
    
    static boolean HACK_KANA = false;
    
    static public void special() {
    	Default.setUCD();
    	for (int i = 0xE000; i < 0x10000; ++i) {
    		if (!Default.ucd.isRepresented(i)) continue;
    		if (!Default.nfkc.isNormalized(i)) continue;
    		System.out.println(Default.ucd.getCodeAndName(i));
    	}
    }
    
    static public void collationChart(UCA uca) throws IOException {
    	Default.setUCD(uca.getUCDVersion());
    	HACK_KANA = true;
    	
        uca.setAlternate(UCA.NON_IGNORABLE);
        
        //Normalizer nfd = new Normalizer(Normalizer.NFD);
        //Normalizer nfc = new Normalizer(Normalizer.NFC);
          
        UCA.UCAContents cc = uca.getContents(UCA.FIXED_CE, null); // nfd instead of null if skipping decomps
        cc.enableSamples();
          
        Set set = new TreeSet();
        
        while (true) {
            String x = cc.next();
            if (x == null) break;
            if (x.equals("\u2F00")) {
            	System.out.println("debug");
            }
            
            set.add(new Pair(uca.getSortKey(x), x));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        byte oldScript = -127;
        
        int[] scriptCount = new int[128];
        
        int counter = 0;
        
        String lastSortKey = "\u0000";
        
        int high = uca.getSortKey("a").charAt(0);
        int variable = UCA.getPrimary(uca.getVariableHigh());
        
        int columnCount = 0;
        
        String[] replacement = new String[] {"%%%", "Collation Charts"};
        String folder = "charts\\uca\\";
        
        Utility.copyTextFile("index.html", true, folder + "index.html", replacement);
        Utility.copyTextFile("charts.css", false, folder + "charts.css");
        Utility.copyTextFile("help.html", true, folder + "help.html");
        
        indexFile = Utility.openPrintWriter(folder + "index_list.html", false, false);
        Utility.appendFile("index_header.html", true, indexFile, replacement);
        
        /*
        indexFile.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        indexFile.println("<title>UCA Default Collation Table</title>");
        indexFile.println("<base target='main'>");
        indexFile.println("<style><!-- p { font-size: 90% } --></style>");
        indexFile.println("</head><body><h2 align='center'>UCA Default Collation Table</h2>");
        indexFile.println("<p align='center'><a href = 'help.html'>Help</a>");
        */
        
        while (it.hasNext()) {
            Utility.dot(counter);
            
            Pair p = (Pair) it.next();
            String sortKey = (String) p.first;
            String s = (String) p.second;
            
            int cp = UTF16.charAt(s,0);
            
            byte script = Default.ucd.getScript(cp);
            
            // get first non-zero primary
            int currentPrimary = getFirstPrimary(sortKey);
            int primary = currentPrimary >>> 16;
            
            if (sortKey.length() < 4) script = NULL_ORDER;
            else if (primary == 0) script = IGNORABLE_ORDER;
            else if (primary < variable) script = VARIABLE_ORDER;
            else if (primary < high) script = COMMON_SCRIPT;
            else if (UCA.isImplicitLeadPrimary(primary)) script = UNSUPPORTED;
            
            if (script == KATAKANA_SCRIPT) script = HIRAGANA_SCRIPT;
            else if ((script == INHERITED_SCRIPT || script == COMMON_SCRIPT) && oldScript >= 0) script = oldScript;

            if (script != oldScript 
                    // && (script != COMMON_SCRIPT && script != INHERITED_SCRIPT)
                    ) {
                closeFile(output);
                output = null;
                oldScript = script;
            }
            
            if (output == null) {
                ++scriptCount[script+3];
                if (scriptCount[script+3] > 1) {
                    System.out.println("\t\tFAIL: " + scriptCount[script+3] + ", " + 
                        getChunkName(script, LONG) + ", " + Default.ucd.getCodeAndName(s));
                }
                output = openFile(scriptCount[script+3], folder, script);
            }
            
            boolean firstPrimaryEquals = currentPrimary == getFirstPrimary(lastSortKey);
            
            int strength = uca.strengthDifference(sortKey, lastSortKey);
            if (strength < 0) strength = -strength;
            lastSortKey = sortKey;
            
            // find out if this is an expansion: more than one primary weight
            
            int primaryCount = 0;
            for (int i = 0; i < sortKey.length(); ++i) {
                char w = sortKey.charAt(i);
                if (w == 0) break;
				if (UCA.isImplicitLeadPrimary(w)) {
					++i; // skip next
				}
                ++ primaryCount;
            }
            
            String breaker = "";
            if (columnCount > 10 || !firstPrimaryEquals) {
                columnCount = 0;
                if (!firstPrimaryEquals || script == UNSUPPORTED) breaker = "</tr><tr>";
                else {
                	breaker = "</tr><tr><td></td>"; // indent 1 cell
                	++columnCount;
                }
            }
            
            String classname = primaryCount > 1 ? XCLASSNAME[strength] : CLASSNAME[strength];
            
            String name = Default.ucd.getName(s);
            
         
            if (s.equals("\u1eaf")) {
            	System.out.println("debug");
            }
            
            String comp = Default.nfc.normalize(s);
            
            String outline = breaker + classname 
                + " title='" + Utility.quoteXML(name, true) + ": " + UCA.toString(sortKey) + "'>"
                + Utility.quoteXML(comp, true)
                + "<br><tt>"
                + Utility.hex(s)
                //+ "<br>" + script
                + "</tt></td>";
            
            output.println(outline);
            ++columnCount;
        }
        
        closeFile(output);
        closeIndexFile(indexFile, "<br>UCA: " + uca.getDataVersion(), COLLATION);
    }
    
    static public void normalizationChart() throws IOException {
        Default.setUCD();
    	HACK_KANA = false;
        
        Set set = new TreeSet();
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	if (!Default.ucd.isRepresented(i)) {
        		if (i < 0xAC00) continue;
        		if (i > 0xD7A3) continue;
        		if (i > 0xACFF && i < 0xD700) continue;
        	}
        	byte cat = Default.ucd.getCategory(i);
        	if (cat == Cs || cat == Co) continue;
        	
        	if (Default.nfkd.isNormalized(i)) continue;
        	String decomp = Default.nfkd.normalize(i);
        	
        	byte script = getBestScript(decomp);
        	
            set.add(new Pair(new Integer(script == COMMON_SCRIPT ? cat + CAT_OFFSET : script),
            		new Pair(Default.ucd.getCase(decomp, FULL, FOLD),
            				 new Integer(i))));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        int oldScript = -127;
        
        int counter = 0;
        
        String[] replacement = new String[] {"%%%", "Normalization Charts"};
        String folder = "charts\\normalization\\";

        Utility.copyTextFile("index.html", true, folder + "index.html", replacement);
        Utility.copyTextFile("charts.css", false, folder + "charts.css");
        Utility.copyTextFile("norm_help.html", true, folder + "help.html");
        
        indexFile = Utility.openPrintWriter(folder + "index_list.html", false, false);
        Utility.appendFile("index_header.html", true, indexFile, replacement);
        
        /*
        indexFile.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        indexFile.println("<title>UCA Default Collation Table</title>");
        indexFile.println("<base target='main'>");
        indexFile.println("<style><!-- p { font-size: 90% } --></style>");
        indexFile.println("</head><body><h2 align='center'>UCA Default Collation Table</h2>");
        indexFile.println("<p align='center'><a href = 'help.html'>Help</a>");
        */
        
        while (it.hasNext()) {
            Utility.dot(counter);
            
            Pair p = (Pair) it.next();
            int script = ((Integer) p.first).intValue();
            int cp = ((Integer)((Pair) p.second).second).intValue();
            
            if (script != oldScript 
                    // && (script != COMMON_SCRIPT && script != INHERITED_SCRIPT)
                    ) {
                closeFile(output);
                output = null;
                oldScript = script;
            }
            
            if (output == null) {
                output = openFile(0, folder, script);
                output.println("<tr><td class='z'>Code</td><td class='z'>C</td><td class='z'>D</td><td class='z'>KC</td><td class='z'>KD</td></tr>");

            }
            
            output.println("<tr>");
            
            String prefix;
            String code = UTF16.valueOf(cp);
            String c = Default.nfc.normalize(cp);
            String d = Default.nfd.normalize(cp);
            String kc = Default.nfkc.normalize(cp);
            String kd = Default.nfkd.normalize(cp);
            
            showCell(output, code, "<td class='z' ", "", false);
            
            prefix = c.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, c, prefix, "", c.equals(code));
            
            prefix = d.equals(c) ? "<td class='g' " : "<td class='n' ";
            showCell(output, d, prefix, "", d.equals(c));
            
            prefix = kc.equals(c) ? "<td class='g' " : "<td class='n' ";
            showCell(output, kc, prefix, "", kc.equals(c));
            
            prefix = (kd.equals(d) || kd.equals(kc)) ? "<td class='g' " : "<td class='n' ";
            showCell(output, kd, prefix, "", (kd.equals(d) || kd.equals(kc)));
            
            output.println("</tr>");
            
        }
        
        closeFile(output);
        closeIndexFile(indexFile, "", NORMALIZATION);
    }
    
    static public void caseChart() throws IOException {
        Default.setUCD();
    	HACK_KANA = false;
        
        Set set = new TreeSet();
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	if (!Default.ucd.isRepresented(i)) continue;
        	byte cat = Default.ucd.getCategory(i);
        	if (cat == Cs || cat == Co) continue;
        	
            String code = UTF16.valueOf(i);
            String lower = Default.ucd.getCase(i, FULL, LOWER);
            String title = Default.ucd.getCase(i, FULL, TITLE);
            String upper = Default.ucd.getCase(i, FULL, UPPER);
            String fold = Default.ucd.getCase(i, FULL, FOLD);
            
        	String decomp = Default.nfkd.normalize(i);
        	int script = 0;
            if (lower.equals(code) && upper.equals(code) && fold.equals(code) && title.equals(code)) {
            	if (!containsCase(decomp)) continue;
            	script = NO_CASE_MAPPING;
        	}
        	
        	if (script == 0) script = getBestScript(decomp);
        	
            set.add(new Pair(new Integer(script == COMMON_SCRIPT ? cat + CAT_OFFSET : script),
            		new Pair(Default.ucd.getCase(decomp, FULL, FOLD),
            				 new Integer(i))));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        int oldScript = -127;
        
        int counter = 0;
        String[] replacement = new String[] {"%%%", "Case Charts"};
        String folder = "charts\\case\\";
        
        Utility.copyTextFile("index.html", true, folder + "index.html", replacement);
        Utility.copyTextFile("charts.css", false, folder + "charts.css");
        Utility.copyTextFile("case_help.html", true, folder + "help.html");
        
        indexFile = Utility.openPrintWriter(folder + "index_list.html", false, false);
        Utility.appendFile("index_header.html", true, indexFile, replacement);
        
        /*
        indexFile.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        indexFile.println("<title>UCA Default Collation Table</title>");
        indexFile.println("<base target='main'>");
        indexFile.println("<style><!-- p { font-size: 90% } --></style>");
        indexFile.println("</head><body><h2 align='center'>UCA Default Collation Table</h2>");
        indexFile.println("<p align='center'><a href = 'help.html'>Help</a>");
        */
        
        int columnCount = 0;
        
        while (it.hasNext()) {
            Utility.dot(counter);
            
            Pair p = (Pair) it.next();
            int script = ((Integer) p.first).intValue();
            int cp = ((Integer)((Pair) p.second).second).intValue();
            
            if (script != oldScript 
                    // && (script != COMMON_SCRIPT && script != INHERITED_SCRIPT)
                    ) {
                closeFile(output);
                output = null;
                oldScript = script;
            }
            
            if (output == null) {
                output = openFile(0, folder, script);
                if (script == NO_CASE_MAPPING) output.println("<tr>");
                else output.println("<tr><td class='z'>Code</td><td class='z'>Lower</td><td class='z'>Title</td>"
                	+"<td class='z'>Upper</td><td class='z'>Fold</td></tr>");

            }
            
            if (script == NO_CASE_MAPPING) {
            	if (columnCount > 10) {
            		output.println("</tr><tr>");
            		columnCount = 0;
            	}
            	showCell(output, UTF16.valueOf(cp), "<td ", "", false);
            	++columnCount;
            	continue;
            }
            
            output.println("<tr>");
            
            String prefix;
            String code = UTF16.valueOf(cp);
            String lower = Default.ucd.getCase(cp, FULL, LOWER);
            String title = Default.ucd.getCase(cp, FULL, TITLE);
            String upper = Default.ucd.getCase(cp, FULL, UPPER);
            String fold = Default.ucd.getCase(cp, FULL, FOLD);
            
            showCell(output, code, "<td class='z' ", "", false);
            
            prefix = lower.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, lower, prefix, "", lower.equals(code));
            
            prefix = title.equals(upper) ? "<td class='g' " : "<td class='n' ";
            showCell(output, title, prefix, "", title.equals(upper));
            
            prefix = upper.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, upper, prefix, "", upper.equals(code));
            
            prefix = fold.equals(lower) ? "<td class='g' " : "<td class='n' ";
            showCell(output, fold, prefix, "", fold.equals(lower));
            
            output.println("</tr>");
            
        }
        
        closeFile(output);
        closeIndexFile(indexFile, "", CASE);
    }
    
    static public void addMapChar(Map m, Set stoplist, String key, String ch) {
    	if (stoplist.contains(key)) return;
    	for (int i = 0; i < key.length(); ++i) {
    		char c = key.charAt(i);
    		if ('0' <= c && c <= '9') return;
    	}
    	Set result = (Set)m.get(key);
    	if (result == null) {
    		result = new TreeSet();
    		m.put(key, result);
    	}
    	result.add(ch);
    }
        
    static public void indexChart() throws IOException {
        Default.setUCD();
    	HACK_KANA = false;
        
        Map map = new TreeMap();
        Set stoplist = new TreeSet();
        
        String[] stops = {"LETTER", "CHARACTER", "AND", "CAPITAL", "SMALL", "COMPATIBILITY", "WITH"};
        stoplist.addAll(Arrays.asList(stops));
        System.out.println("Stop-list: " + stoplist);
        
        for (int i = 0; i < LIMIT_SCRIPT; ++i) {
        	stoplist.add(Default.ucd.getScriptID_fromIndex((byte)i));
        }
        System.out.println("Stop-list: " + stoplist);
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	if (!Default.ucd.isRepresented(i)) continue;
        	if (0xAC00 <= i && i <= 0xD7A3) continue;
        	if (Default.ucd.hasComputableName(i)) continue;

        	String s = Default.ucd.getName(i);
        	if (s == null) continue;
        	
        	if (s.startsWith("<")) {
        		System.out.println("Wierd character at " + Default.ucd.getCodeAndName(i));
        	}
        	String ch = UTF16.valueOf(i);
        	int last = -1;
        	int j;
        	for (j = 0; j < s.length(); ++j) {
        		char c = s.charAt(j);
        		if ('A' <= c && c <= 'Z' || '0' <= c && c <= '9') {
        			if (last == -1) last = j;
        		} else {
        			if (last != -1) {
        				String word = s.substring(last, j);
        				addMapChar(map, stoplist, word, ch);
        				last = -1;
        			}
        		}
        	}
        	if (last != -1) {
        				String word = s.substring(last, j);
        				addMapChar(map, stoplist, word, ch);
        	}
        }
          
        PrintWriter output = null;
        
        Iterator it = map.keySet().iterator();
        
        int oldScript = -127;
        
        int counter = 0;
        String[] replacement = new String[] {"%%%", "Name Charts"};
        String folder = "charts\\name\\";
        
        Utility.copyTextFile("index.html", true, folder + "index.html", replacement);
        Utility.copyTextFile("charts.css", false, folder + "charts.css");
        Utility.copyTextFile("name_help.html", true, folder + "help.html");
        
        indexFile = Utility.openPrintWriter(folder + "index_list.html", false, false);
        Utility.appendFile("index_header.html", true, indexFile, replacement);
        
        int columnCount = 0;
        char lastInitial = 0;
        
        while (it.hasNext()) {
            Utility.dot(counter);
            
            String key = (String) it.next();
            
            Set chars = (Set) map.get(key);
            
            char initial = key.charAt(0);
            
            if (initial != lastInitial) {
                closeFile(output);
                output = null;
                lastInitial = initial;
            }
            
            if (output == null) {
                output = openFile2(0, folder, String.valueOf(initial));
            }
            
            output.println("<tr><td class='h'>" + key + "</td>");
            columnCount = 1;
            
            Iterator sublist = chars.iterator();
            while (sublist.hasNext()) {
            	 
            	String ch = (String) sublist.next();
            	if (columnCount > 10) {
            		output.println("</tr><tr><td></td>");
            		columnCount = 1;
            	}
            	showCell(output, ch, "<td ", "", true);
            	++columnCount;
            	continue;
            }
            
            output.println("</tr>");
            
        }
        
        closeFile(output);
        closeIndexFile(indexFile, "", CASE);
    }
    
    static void showCell(PrintWriter output, String s, String prefix, String extra, boolean skipName) {
        String name = Default.ucd.getName(s);
        String comp = Default.nfc.normalize(s);
            
        String outline = prefix 
            + (skipName ? "" : " title='" + Utility.quoteXML(name, true) + "'")
            + extra + ">"
            + Utility.quoteXML(comp, true)
            + "<br><tt>"
            + Utility.hex(s)
            //+ "<br>" + script
            + "</tt></td>";
            
        output.println(outline);
    }
    
    static byte getBestScript(String s) {
    	int cp;
    	byte result = COMMON_SCRIPT;
    	for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
    		cp = UTF16.charAt(s, i);
    		result = Default.ucd.getScript(cp);
    		if (result != COMMON_SCRIPT && result != INHERITED_SCRIPT) return result;
    	}
    	return COMMON_SCRIPT;
    }

    static int getFirstPrimary(String sortKey) {
        int result = sortKey.charAt(0);
		if (UCA.isImplicitLeadPrimary(result)) {
			return (result << 16) | sortKey.charAt(1);
		}
		return (result << 16);
    }
    
    static final String[] CLASSNAME = {
        "<td class='q'", 
        "<td class='q'", 
        "<td class='q'", 
        "<td class='t'", 
        "<td class='s'", 
        "<td class='p'"};
        
    static final String[] XCLASSNAME = {
        "<td class='eq'", 
        "<td class='eq'", 
        "<td class='eq'", 
        "<td class='et'", 
        "<td class='es'", 
        "<td class='ep'"};
        

    static PrintWriter indexFile;
    
    static PrintWriter openFile(int count, String directory, int script) throws IOException {
        String scriptName = getChunkName(script, LONG);
        String shortScriptName = getChunkName(script, SHORT);
        String hover = scriptName.equals(shortScriptName) ? "" : "' title='" + shortScriptName;
        
        String fileName = "chart_" + scriptName + (count > 1 ? count + "" : "") + ".html";
        PrintWriter output = Utility.openPrintWriter(directory + fileName, false, false);
        Utility.fixDot();
        System.out.println("Writing: " + scriptName);
        indexFile.println(" <a href = '" + fileName + hover + "'>" + scriptName + "</a>");
        String title = "UCA: " + scriptName;
        output.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        output.println("<title>" + title + "</title>");
        output.println("<link rel='stylesheet' href='charts.css' type='text/css'>");
        output.println("</head><body><h2>" + scriptName + "</h2>");
        output.println("<table>");
        return output;
    }
    
    static PrintWriter openFile2(int count, String directory, String name) throws IOException {
        String fileName = "chart_" + name + (count > 1 ? count + "" : "") + ".html";
        PrintWriter output = Utility.openPrintWriter(directory + fileName, false, false);
        Utility.fixDot();
        System.out.println("Writing: " + name);
        indexFile.println(" <a href = '" + fileName + "'>" + name + "</a>");
        String title = name;
        output.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        output.println("<title>" + title + "</title>");
        output.println("<link rel='stylesheet' href='charts.css' type='text/css'>");
        output.println("</head><body>");
        output.println("<table>");
        return output;
    }
    
    static final int 
    	NULL_ORDER = -3,
    	IGNORABLE_ORDER = -2,
    	VARIABLE_ORDER = -1,
    	// scripts in here
    	UNSUPPORTED = 120,
    	CAT_OFFSET = 128,
    	// categories in here
    	NO_CASE_MAPPING = 200;
    
    static String getChunkName(int script, byte length) {
    	switch(script) {
    		case NO_CASE_MAPPING: return "NoCaseMapping";
        	case NULL_ORDER: return "Null";
        	case IGNORABLE_ORDER: return "Ignorable";
        	case VARIABLE_ORDER: return "Variable";
        	case UNSUPPORTED: return "Unsupported";
        	default: 
    		if (script >= CAT_OFFSET) return Default.ucd.getCategoryID_fromIndex((byte)(script - CAT_OFFSET), length);
        	else if (script == HIRAGANA_SCRIPT && HACK_KANA) return length == SHORT ? "Kata-Hira" : "Katakana-Hiragana";
        	else return Default.ucd.getCase(Default.ucd.getScriptID_fromIndex((byte)script, length), FULL, TITLE);
    	}
    }

    static void closeFile(PrintWriter output) {
        if (output == null) return;
        output.println("</table></body></html>");
        output.close();
    }


	static final byte COLLATION = 0, NORMALIZATION = 1, CASE = 2;
	
    static void closeIndexFile(PrintWriter indexFile, String extra, byte choice) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        indexFile.println("</p><hr width='50%'><p>");
        boolean gotOne = false;
        if (choice != COLLATION) {
        	indexFile.println("<a href='..\\uca\\index.html' target='_top'>Collation&nbsp;Charts</a>");
        	gotOne = true;
        }
        if (choice != NORMALIZATION) {
        	if (gotOne) indexFile.println("<br>");
        	indexFile.println("<a href='..\\normalization\\index.html' target='_top'>Normalization&nbsp;Charts</a>");
        	gotOne = true;
        }
        if (choice != CASE) {
        	if (gotOne) indexFile.println("<br>");
        	indexFile.println("<a href='..\\case\\index.html' target='_top'>Case&nbsp;Charts</a>");
        	gotOne = true;
        }
        indexFile.println("</p><hr width='50%'><p style='font-size: 70%'>");
        indexFile.println("UCD: " + Default.ucd.getVersion() + extra);
        indexFile.println("<br>" + df.format(new Date()) + " <a href='http://www.macchiato.com/' target='_top'>MED</a>");
        indexFile.println("</p></body></html>");
        indexFile.close();
    }
    
    static boolean containsCase(String s) {
    	int cp;
    	for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
    		cp = UTF16.charAt(s, i);
			// contains Lu, Lo, Lt, or Lowercase or Uppercase 
			byte cat = Default.ucd.getCategory(cp);
			if (cat == Lu || cat == Ll || cat == Lt) return true;
			if (Default.ucd.getBinaryProperty(cp, Other_Lowercase)) return true;
			if (Default.ucd.getBinaryProperty(cp, Other_Uppercase)) return true;
		}
		return false;
	}
    
}



    /*
    static final IntStack p1 = new IntStack(30);
    static final IntStack s1 = new IntStack(30);
    static final IntStack t1 = new IntStack(30);
    static final IntStack p2 = new IntStack(30);
    static final IntStack s2 = new IntStack(30);
    static final IntStack t2 = new IntStack(30);
    
    static int getStrengthDifference(CEList ceList, CEList lastCEList) {
        extractNonzeros(ceList, p1, s1, t1);
        extractNonzeros(lastCEList, p2, s2, t2);
        int temp = p1.compareTo(p2);
        if (temp != 0) return 3;
        temp = s1.compareTo(s2);
        if (temp != 0) return 2;
        temp = t1.compareTo(t2);
        if (temp != 0) return 1;
        return 0;
    }
    
    static void extractNonzeros(CEList ceList, IntStack primaries, IntStack secondaries, IntStack tertiaries) {
        primaries.clear();
        secondaries.clear();
        tertiaries.clear();
        
        for (int i = 0; i < ceList.length(); ++i) {
            int ce = ceList.at(i);
            int temp = UCA.getPrimary(ce);
            if (temp != 0) primaries.push(temp);
            temp = UCA.getSecondary(ce);
            if (temp != 0) secondaries.push(temp);
            temp = UCA.getTertiary(ce);
            if (temp != 0) tertiaries.push(temp);
        }
    }
    */