/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/WriteCharts.java,v $ 
* $Date: 2002/04/23 01:59:16 $ 
* $Revision: 1.5 $
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
    
    static final byte UNSUPPORTED = 120;
    static boolean HACK_KANA = false;
    
    static public void test(UCA uca) throws IOException {
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
        
        int lastPrimary = -1;
        
        String lastSortKey = "\u0000";
        
        int high = uca.getSortKey("a").charAt(0);
        int variable = UCA.getPrimary(uca.getVariableHigh());
        
        int columnCount = 0;
        
        Utility.copyTextFile("index.html", true, "CollationCharts\\index.html");
        Utility.copyTextFile("charts.css", false, "CollationCharts\\charts.css");
        Utility.copyTextFile("help.html", true, "CollationCharts\\help.html");
        
        indexFile = Utility.openPrintWriter("CollationCharts\\index_list.html", false, false);
        Utility.appendFile("index_header.html", true, indexFile);
        
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
            
            if (sortKey.length() < 4) script = -3;
            else if (primary == 0) script = -2;
            else if (primary < variable) script = -1;
            else if (primary < high) script = COMMON_SCRIPT;
            else if (primary >= UCA.UNSUPPORTED_BASE && primary <= UCA.UNSUPPORTED_TOP) script = UNSUPPORTED;
            
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
                        getChunkName(script) + ", " + Default.ucd.getCodeAndName(s));
                }
                output = openFile(scriptCount[script+3], "CollationCharts\\", script);
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
				if (w >= UCA.UNSUPPORTED_BASE && w <= UCA.UNSUPPORTED_TOP) {
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
                + " title='" + Utility.quoteXML(name) + ": " + UCA.toString(sortKey) + "'>"
                + Utility.quoteXML(comp)
                + "<br><tt>"
                + Utility.hex(s)
                //+ "<br>" + script
                + "</tt></td>";
            
            output.println(outline);
            ++columnCount;
        }
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        
        closeFile(output);
        indexFile.println("<hr><p>Last Modified: " + df.format(new Date()));
        indexFile.println("<br>UCA Version: " + uca.getDataVersion());
        indexFile.println("<br>UCD Version: " + Default.ucd.getVersion());
        indexFile.println("</p></body></html>");
        indexFile.close();
    }
    
    static int getFirstPrimary(String sortKey) {
        int result = sortKey.charAt(0);
		if (result >= UCA.UNSUPPORTED_BASE && result <= UCA.UNSUPPORTED_TOP) {
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
        String scriptName = getChunkName(script);
        if (script < 128) scriptName = Default.ucd.getCase(scriptName, FULL, TITLE);
        
        String fileName = "chart_" + scriptName + (count > 1 ? count + "" : "") + ".html";
        PrintWriter output = Utility.openPrintWriter(directory + fileName, false, false);
        Utility.fixDot();
        System.out.println("Writing: " + scriptName);
        
        indexFile.println(" <a href = '" + fileName + "'>" + scriptName + "</a>");
        String title = "UCA: " + scriptName;
        output.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        output.println("<title>" + title + "</title>");
        output.println("<link rel='stylesheet' href='charts.css' type='text/css'>");
        output.println("</head><body><h2>" + scriptName + "</h2>");
        output.println("<table>");
        return output;
    }
    
    static String getChunkName(int script) {
    	if (script >= 128) return Default.ucd.getCategoryID_fromIndex((byte)(script - 128), LONG);
        else if (script == -4) return "NoMapping";
        else if (script == -3) return "NULL";
        else if (script == -2) return "IGNORABLE";
        else if (script == -1) return "VARIABLE";
        else if (script == HIRAGANA_SCRIPT && HACK_KANA) return "KATAKANA-HIRAGANA";
        else if (script == UNSUPPORTED) return "UNSUPPORTED";
        else return Default.ucd.getScriptID_fromIndex((byte)script);
    }

    static void closeFile(PrintWriter output) {
        if (output == null) return;
        output.println("</table></body></html>");
        output.close();
    }


    static public void normalizationChart() throws IOException {
        Default.setUCD();
    	HACK_KANA = false;
        
        Set set = new TreeSet();
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	if (!Default.ucd.isRepresented(i)) continue;
        	byte cat = Default.ucd.getCategory(i);
        	if (cat == Cs || cat == Co) continue;
        	
        	if (!Default.nfkd.normalizationDiffers(i)) continue;
        	String decomp = Default.nfkd.normalize(i);
        	
        	byte script = getBestScript(decomp);
        	
            set.add(new Pair(new Integer(script == COMMON_SCRIPT ? cat + 128 : script),
            		new Pair(decomp,
            				 new Integer(i))));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        int oldScript = -127;
        
        int[] scriptCount = new int[128];
        
        int counter = 0;
        
        int lastPrimary = -1;
        
        String lastSortKey = "\u0000";
        
        Utility.copyTextFile("index.html", true, "NormalizationCharts\\index.html");
        Utility.copyTextFile("charts.css", false, "NormalizationCharts\\charts.css");
        Utility.copyTextFile("norm_help.html", true, "NormalizationCharts\\help.html");
        
        indexFile = Utility.openPrintWriter("NormalizationCharts\\index_list.html", false, false);
        Utility.appendFile("norm_index_header.html", true, indexFile);
        
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
                output = openFile(0, "NormalizationCharts\\", script);
                output.println("<tr><td class='z'>Code</td><td class='z'>C</td><td class='z'>D</td><td class='z'>KC</td><td class='z'>KD</td></tr>");

            }
            
            output.println("<tr>");
            
            String prefix;
            String code = UTF16.valueOf(cp);
            String c = Default.nfc.normalize(cp);
            String d = Default.nfd.normalize(cp);
            String kc = Default.nfkc.normalize(cp);
            String kd = Default.nfkd.normalize(cp);
            
            showCell(output, code, "<td class='z' ", "");
            
            prefix = c.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, c, prefix, "");
            
            prefix = d.equals(c) ? "<td class='g' " : "<td class='n' ";
            showCell(output, d, prefix, "");
            
            prefix = kc.equals(c) ? "<td class='g' " : "<td class='n' ";
            showCell(output, kc, prefix, "");
            
            prefix = (kd.equals(d) || kd.equals(kc)) ? "<td class='g' " : "<td class='n' ";
            showCell(output, kd, prefix, "");
            
            output.println("</tr>");
            
        }
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        
        closeFile(output);
        indexFile.println("<hr><p>Last Modified: " + df.format(new Date()));
        indexFile.println("<br>UCD Version: " + Default.ucd.getVersion());
        indexFile.println("</p></body></html>");
        indexFile.close();
    }
    
    static void showCell(PrintWriter output, String s, String prefix, String extra) {
        String name = Default.ucd.getName(s);
        String comp = Default.nfc.normalize(s);
            
        String outline = prefix 
            + " title='" + Utility.quoteXML(name) + extra + "'>"
            + Utility.quoteXML(comp)
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
        	byte script = 0;
            if (lower.equals(code) && upper.equals(code) && fold.equals(code)) {
            	if (decomp contains Lu, Lo, Lt, or Lowercase or Uppercase) script = -4;
            	else continue;
        	}
        	
        	
        	if (script == 0) script = getBestScript(decomp);
        	
            set.add(new Pair(new Integer(script == COMMON_SCRIPT ? cat + 128 : script),
            		new Pair(decomp,
            				 new Integer(i))));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        int oldScript = -127;
        
        int[] scriptCount = new int[128];
        
        int counter = 0;
        
        int lastPrimary = -1;
        
        String lastSortKey = "\u0000";
        
        Utility.copyTextFile("index.html", true, "CaseCharts\\index.html");
        Utility.copyTextFile("charts.css", false, "CaseCharts\\charts.css");
        Utility.copyTextFile("norm_help.html", true, "CaseCharts\\help.html");
        
        indexFile = Utility.openPrintWriter("CaseCharts\\index_list.html", false, false);
        Utility.appendFile("norm_index_header.html", true, indexFile);
        
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
                output = openFile(0, "CaseCharts\\", script);
                output.println("<tr><td class='z'>Code</td><td class='z'>Lower</td><td class='z'>Title</td><td class='z'>Upper</td><td class='z'>Fold</td></tr>");

            }
            
            output.println("<tr>");
            
            String prefix;
            String code = UTF16.valueOf(cp);
            String lower = Default.ucd.getCase(cp, FULL, LOWER);
            String title = Default.ucd.getCase(cp, FULL, TITLE);
            String upper = Default.ucd.getCase(cp, FULL, UPPER);
            String fold = Default.ucd.getCase(cp, FULL, FOLD);
            
            showCell(output, code, "<td class='z' ", "");
            
            prefix = lower.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, lower, prefix, "");
            
            prefix = title.equals(upper) ? "<td class='g' " : "<td class='n' ";
            showCell(output, title, prefix, "");
            
            prefix = upper.equals(code) ? "<td class='g' " : "<td class='n' ";
            showCell(output, upper, prefix, "");
            
            prefix = (fold.equals(lower)) ? "<td class='g' " : "<td class='n' ";
            showCell(output, fold, prefix, "");
            
            output.println("</tr>");
            
        }
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        
        closeFile(output);
        indexFile.println("<hr><p>Last Modified: " + df.format(new Date()));
        indexFile.println("<br>UCD Version: " + Default.ucd.getVersion());
        indexFile.println("</p></body></html>");
        indexFile.close();
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