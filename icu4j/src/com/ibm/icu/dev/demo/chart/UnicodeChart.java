/*
 *******************************************************************************
 * Copyright (C) 1997-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.chart;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.dev.test.util.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.util.VersionInfo;

public class UnicodeChart {
    static int surrogateType = UCharacter.getType('\ud800');
    static int privateUseType = UCharacter.getType('\ue000');

    public static void main(String[] args) throws IOException {
        //int rowWidth = 256;
    	VersionInfo vi = UCharacter.getUnicodeVersion();
    	String version = vi.getMajor() + "." + vi.getMinor() + "." + vi.getMilli();
        PrintWriter pw = BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\", "UnicodeChart.html");
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<script type='text/javascript' src='UnicodeChart.js'></script>");
        pw.println("<link rel='stylesheet' type='text/css' href='UnicodeChart.css'>");
        pw.println("<title>Unicode " + version + " Chart</title>");
        pw.println("</head><body bgcolor='#FFFFFF'>");
        pw.println("<table border='1' cellspacing='0'><caption><h1>Unicode " + version + " Chart</h1></caption>");

        /*pw.println("<tr><th></th>");
        for (int j = 0; j < rowWidth; ++j) {
            pw.print("<th>" + hex(j,2) + "</th>");
        }
        pw.println("</tr>");
        */
        // TODO: fix Utility to take ints
        
        System.out.println("//Surrogate Type: Java=" + Character.SURROGATE + ", ICU=" + surrogateType);
        System.out.println("//Private-Use Type: Java=" + Character.PRIVATE_USE + ", ICU=" + privateUseType);
        
        //boolean gotOne = true;
        int columns = 0;
        int limit = 0x110000/16;
        pw.println("<script>");
        pw.print("top();");
        // an array that maps String (containing column information) to UnicodeSet (containing column numbers)
        Map info_number = new TreeMap();
        List number_info = new ArrayList();
        StringBuffer sb = new StringBuffer();
        int lastInfo = -1;
        int sameCount = 0;
        System.out.println("var charRanges = [");
        for (int i = 0; i < limit; ++i) {
        	// get the string of info, and get its number
            sb.setLength(0);
            for (int j = 0; j < 16; ++j) {
            	int cp = i*16+j;
            	char type = getType(cp);
            	sb.append(type);
            }
            String info = sb.toString();
            Integer s = (Integer) info_number.get(info);
            if (s == null) {
            	info_number.put(info, s=new Integer(number_info.size()));
            	number_info.add(info);
            }
            
            // write a line whenever the value changes
            if (lastInfo == s.intValue()) {
            	sameCount++;
            } else {
            	if (lastInfo != -1) System.out.println(sameCount + "," + lastInfo + ",");
            	sameCount = 1;
            	lastInfo = s.intValue();
            }
        }
        // write last line
        System.out.println(sameCount + "," + lastInfo);
        System.out.println("];");
       
        // now write out array
        System.out.println("var charInfo = [");
        for (Iterator it = number_info.iterator(); it.hasNext();) {
        	String info = (String) it.next();
        	System.out.println("'" + info + "',");
        }
        System.out.println("];");
        
        // write out blocks
        Map blockMap = new TreeMap();
        int startValue = -1;
        int lastEnum = -1;
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	int prop = UCharacter.getIntPropertyValue(i,UProperty.BLOCK);
        	if (prop == lastEnum) continue;
        	if (lastEnum != -1) {
        		String s = UCharacter.getPropertyValueName(UProperty.BLOCK, lastEnum, UProperty.NameChoice.LONG);
        		blockMap.put(s, hex(startValue,0) + "/" + hex(i - startValue,0));
        		System.out.println(s + ": " + blockMap.get(s));
        	}
        	lastEnum = prop;
        	startValue = i;
        }
		String s = UCharacter.getPropertyValueName(UProperty.BLOCK, lastEnum, UProperty.NameChoice.LONG);
		blockMap.put(s, hex(startValue,0) + "/" + hex(0x110000 - startValue,0));
		blockMap.remove("No_Block");
		for (Iterator it = blockMap.keySet().iterator(); it.hasNext();) {
			String blockName = (String)it.next();
			String val = (String) blockMap.get(blockName);
			System.out.println("<option value='" + val + "'>" + blockName + "</option>");
		}
        
        //      <option value="4DC0">Yijing Hexagram Symbols</option>
        
        
        pw.println("</script></tr></table><p></p>");
        pw.println("<table><caption>Key</caption>");
        pw.println("<tr><td>X</td><td class='left'>Graphic characters</td></tr>");
        pw.println("<tr><td>\u00A0</td><td class='left'>Whitespace</td></tr>");
        pw.println("<tr><td class='i'>\u00A0</td><td class='left'>Other Default Ignorable</td></tr>");
        pw.println("<tr><td class='u'>\u00A0</td><td class='left'>Undefined, Private Use, or Surrogates</td></tr>");
        pw.println("<tr><td class='n'>\u00A0</td><td class='left'>Noncharacter</td></tr>");
        pw.println("</table>");
        pw.println("<p>Copyright \u00A9 2003, Mark Davis. All Rights Reserved.</body></html>");
        pw.close();
        System.out.println("//columns: " + columns);
    }

	private static char getType(int i) {
		char type = 'v';
		int cat = UCharacter.getType(i);
		if (UCharacter.hasBinaryProperty(i, UProperty.NONCHARACTER_CODE_POINT)) {
		    type = 'n';
		} else if (cat == Character.UNASSIGNED || cat == surrogateType || cat == privateUseType) {
		    type = 'u';
		} else if (UCharacter.isUWhiteSpace(i)) {
		    type = 'w';
		} else if (UCharacter.hasBinaryProperty(i, UProperty.DEFAULT_IGNORABLE_CODE_POINT)) {
		    type = 'i';
		} else {
		    type = 'v';
		}
		return type;
	}
    
    static String hex(int i, int padTo) {
        String result = Integer.toHexString(i).toUpperCase(java.util.Locale.ENGLISH);
        while (result.length() < padTo) result = "0" + result;
        return result;
    }
}