/*
 *******************************************************************************
 * Copyright (C) 1997-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.chart;
import java.io.*;
import com.ibm.icu.dev.test.util.*;
import com.ibm.icu.lang.*;

public class UnicodeChart {
    public static void main(String[] args) throws IOException {
        int rowWidth = 256;
        PrintWriter pw = BagFormatter.openUTF8Writer("", "UnicodeChart.html");
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<script type='text/javascript' src='UnicodeChart.js'></script>");
        pw.println("<link rel='stylesheet' type='text/css' href='UnicodeChart.css'>");
        pw.println("<title>Unicode 4.0 Chart</title>");
        pw.println("</head><body bgcolor='#FFFFFF'>");
        pw.println("<table border='1' cellspacing='0'><caption><h1>Unicode 4.0 Chart</h1></caption>");

        /*pw.println("<tr><th></th>");
        for (int j = 0; j < rowWidth; ++j) {
            pw.print("<th>" + hex(j,2) + "</th>");
        }
        pw.println("</tr>");
        */
        // TODO: fix Utility to take ints
        
        int surrogateType = UCharacter.getType('\ud800');
        int privateUseType = UCharacter.getType('\ue000');
        System.out.println("Surrogate Type: Java=" + Character.SURROGATE + ", ICU=" + surrogateType);
        System.out.println("Private-Use Type: Java=" + Character.PRIVATE_USE + ", ICU=" + privateUseType);
        
        boolean gotOne = true;
        int columns = 0;
        int limit = 0x10FFFF;
        char lastType = 'x';
        int lastCount = 0;
        pw.println("<script>");
        pw.print("top();");
        int itemCount = 1;
        for (int i = 0; i <= limit; ++i) {
            if ((i & 0xFF) == 0) System.out.println(hex(i>>8,2) + "__");
            columns++;
            //pw.print("<tr><th>" + hex(i>>8,2) + "__</th>");
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
            if (type != lastType) {
                if (lastCount != 0) pw.print(lastType + "(" + lastCount + ");");
                lastType = type;
                lastCount = 0;
                ++itemCount;
                if ((itemCount & 0xF) == 0) pw.println();
            }
            ++lastCount;
        }
        pw.println(lastType + "(" + lastCount + ");"); // finish last row
        pw.println("</script></tr></table><p></p>");
        pw.println("<table><caption>Key</caption>");
        pw.println("<tr><td>X</td><td class='left'>Graphic characters</td></tr>");
        pw.println("<tr><td>\u00A0</td><td class='left'>Whitespace</td></tr>");
        pw.println("<tr><td class='i'>&nbsp;</td><td class='left'>Other Default Ignorable</td></tr>");
        pw.println("<tr><td class='u'>&nbsp;</td><td class='left'>Undefined, Private Use, or Surrogates</td></tr>");
        pw.println("<tr><td class='n'>&nbsp;</td><td class='left'>Noncharacter</td></tr>");
        pw.println("</table>");
        pw.println("<p>Copyright \u00A9 2003, Mark Davis. All Rights Reserved.</body></html>");
        pw.close();
        System.out.println("columns: " + columns);
    }
    
    static String hex(int i, int padTo) {
        String result = Integer.toHexString(i).toUpperCase(java.util.Locale.ENGLISH);
        while (result.length() < padTo) result = "0" + result;
        return result;
    }
}