/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/WriteCharts.java,v $ 
* $Date: 2001/10/25 20:35:41 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

import java.util.*;

import java.io.*;
import com.ibm.text.UCD.*;
import com.ibm.text.utility.*;
import com.ibm.text.UTF16;

public class WriteCharts implements UCD_Types {
    
    static UCD ucd;
    
    static public void test(UCA uca) throws IOException {
  
        uca.setAlternate(UCA.NON_IGNORABLE);
        
        ucd = UCD.make();
        Normalizer nfd = new Normalizer(Normalizer.NFD);
        Normalizer nfc = new Normalizer(Normalizer.NFC);
          
        UCA.UCAContents cc = uca.getContents(UCA.FIXED_CE, null); // nfd instead of null if skipping decomps
          
        Set set = new TreeSet();
        
        while (true) {
            String x = cc.next();
            if (x == null) break;
            set.add(new Pair(uca.getSortKey(x), x));
        }
          
        PrintWriter output = null;
        
        Iterator it = set.iterator();
        
        int oldScript = -999;
        
        int[] scriptCount = new int[LIMIT_SCRIPT];
        
        int counter = 0;
        
        int lastPrimary = -1;
        
        String lastSortKey = "\u0000";
        
        int high = uca.getSortKey("a").charAt(0);
        int variable = UCA.getPrimary(uca.getVariableHigh());
        
        int columnCount = 0;
        
        Utility.copyTextFile("index.html", true, "CollationCharts\\index.html");
        Utility.copyTextFile("charts.css", false, "CollationCharts\\charts.css");
        Utility.copyTextFile("help.html", true, "CollationCharts\\help.html");
        
        indexFile = Utility.openPrintWriter("CollationCharts\\index_list.html");

        indexFile.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        indexFile.println("<title>UCA Default Collation Table</title>");
        indexFile.println("<base target='main'>");
        indexFile.println("<style><!-- p { font-size: 90% } --></style>");
        indexFile.println("</head><body><h2 align='center'>UCA Default Collation Table</h2>");
        indexFile.println("<p align='center'><a href = 'help.html'>Help</a>");
        
        while (it.hasNext()) {
            Utility.dot(counter);
            
            Pair p = (Pair) it.next();
            String sortKey = (String) p.first;
            String s = (String) p.second;
            
            int cp = UTF16.charAt(s,0);
            byte script = ucd.getScript(cp);
            if (script == KATAKANA_SCRIPT) script = HIRAGANA_SCRIPT;
            else if (script == INHERITED_SCRIPT) script = COMMON_SCRIPT;
            
            // get first non-zero primary
            int primary = sortKey.charAt(0);
            if (sortKey.length() < 4) script = -3;
            else if (primary == 0) script = -2;
            else if (primary < variable) script = -1;
            else if (primary < high) script = COMMON_SCRIPT;
            
            if (script != oldScript 
                    && (oldScript < COMMON_SCRIPT || script != COMMON_SCRIPT && script != INHERITED_SCRIPT)) {
                closeFile(output);
                output = null;
            }
            if (output == null) {
                ++scriptCount[script+3];
                if (scriptCount[script+3] > 1) {
                    System.out.println("\t\tFAIL: " + scriptCount[script+3] + ", " + 
                        ucd.getScriptID_fromIndex(script) + ", " + ucd.getCodeAndName(s));
                }
                output = openFile(scriptCount[script+3], script);
                oldScript = script;
            }
            
            boolean firstPrimaryEquals = sortKey.charAt(0) == lastSortKey.charAt(0);
            
            int strength = uca.strengthDifference(sortKey, lastSortKey);
            if (strength < 0) strength = -strength;
            lastSortKey = sortKey;
            
            // find out if this is an expansion: more than one primary weight
            
            int primaryCount = 0;
            for (int i = 0; i < sortKey.length(); ++i) {
                char w = sortKey.charAt(i);
                if (w == 0) break;
                ++ primaryCount;
            }
            
            String breaker = "";
            if (columnCount > 10 || !firstPrimaryEquals) {
                if (!firstPrimaryEquals) breaker = "</tr><tr>";
                else breaker = "</tr><tr><td></td>"; // indent 1 cell
                columnCount = 0;
            }
            
            String classname = primaryCount > 1 ? XCLASSNAME[strength] : CLASSNAME[strength];
            
            output.println(breaker + classname + nfc.normalize(s) 
                + "<br><tt>" + Utility.hex(s) 
                //+ "<br>" + script
                //+ "<br>" + UCA.toString(sortKey) 
                + "</tt></td>");
            ++columnCount;
        }
        
        closeFile(output);
        indexFile.println("</body></html>");
        indexFile.close();
    }
    
    static final String[] CLASSNAME = {
        "<td class='q'>", 
        "<td class='q'>", 
        "<td class='q'>", 
        "<td class='t'>", 
        "<td class='s'>", 
        "<td class='p'>"};
        
    static final String[] XCLASSNAME = {
        "<td class='eq'>", 
        "<td class='eq'>", 
        "<td class='eq'>", 
        "<td class='et'>", 
        "<td class='es'>", 
        "<td class='ep'>"};
        

    static PrintWriter indexFile;
    
    static PrintWriter openFile(int count, byte script) throws IOException {
        String scriptName = getChunkName(script);
        scriptName = ucd.getCase(scriptName, FULL, TITLE);
        
        String fileName = "chart_" + scriptName + (count > 1 ? count + "" : "") + ".html";
        PrintWriter output = Utility.openPrintWriter("CollationCharts\\" + fileName);
        Utility.fixDot();
        System.out.println("Writing: " + scriptName);
        
        indexFile.println(" | <a href = '" + fileName + "'>" + scriptName + "</a>");
        String title = "UCA: " + scriptName;
        output.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        output.println("<title>" + title + "</title>");
        output.println("<link rel='stylesheet' href='charts.css' type='text/css'>");
        output.println("</head><body><h2>" + scriptName + "</h2>");
        output.println("<table>");
        return output;
    }
    
    static String getChunkName(byte script) {
        if (script == -3) return "NULL";
        else if (script == -2) return "IGNORABLE";
        else if (script == -1) return "VARIABLE";
        else if (script == HIRAGANA_SCRIPT) return "KATAKANA-HIRAGANA";
        else return ucd.getScriptID_fromIndex(script);
    }

    static void closeFile(PrintWriter output) {
        if (output == null) return;
        output.println("</body></table></html>");
        output.close();
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