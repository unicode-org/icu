/*
 *******************************************************************************
 * Copyright (C) 1997-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/chart/UnicodeChart.java,v $ 
 * $Date: 2003/11/26 23:36:39 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.dev.demo.chart;
import java.io.*;
import com.ibm.icu.dev.test.util.*;
import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.impl.*;

public class UnicodeChart {
    public static void main(String[] args) throws IOException {
        int rowWidth = 256;
        PrintWriter pw = BagFormatter.openUTF8Writer("", "UnicodeChart.html", BagFormatter.CONSOLE);
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<title>Unicode 4.0 Chart</title><style><!--");
        pw.println("th { width: 1%; font-family: monospace; font-size: 50%; text-align: Center; border: 1px solid black; margin: 0; padding: 0}");
        pw.println("td { font-size: 50%; text-align: Center; border-right: 1px solid blue; border-top: 1px solid blue; margin: 0; padding: 0 }");
        pw.println("table { border-spacing: 0; border-collapse: collapse; border: 1px solid black; margin: 0; padding: 0 }");
        pw.println(".d { border-top: 2px double blue }");
        pw.println(".u { background-color: #DDDDDD }");
        pw.println(".du { border-top: 2px double blue; background-color: #DDDDDD }");
        pw.println(".n { background-color: black }");
        pw.println(".dn { border-top: 2px double blue; background-color: black }");
        pw.println(".i { background-color: yellow }");
        pw.println(".di { border-top: 2px double blue; background-color: yellow }");
        
        pw.println("--></style></head><body bgcolor='#FFFFFF'>");
        pw.println("<table border='1' cellspacing='0'>");
        pw.println("<caption><h1>Unicode 4.0 Chart</h1></caption>");

        pw.println("<tr><th></th>");
        for (int j = 0; j < rowWidth; ++j) {
            pw.print("<th>" + hex(j,2) + "</th>");
        }
        pw.println("</tr>");
        // TODO: fix Utility to take ints
        
        int surrogateType = UCharacter.getType('\ud800');
        int privateUseType = UCharacter.getType('\ue000');
        System.out.println("Surrogate Type: Java=" + Character.SURROGATE + ", ICU=" + surrogateType);
        System.out.println("Private-Use Type: Java=" + Character.PRIVATE_USE + ", ICU=" + privateUseType);
        
        boolean gotOne = true;
        int columns = 0;
        for (int i = 0; i < 0x10FFFF; i += rowWidth) {
            boolean gotLast = gotOne;
            gotOne = false;
            int limit = i + rowWidth;
            for (int j = i; j < limit; ++j) {
                int type = UCharacter.getType(j);
                if (type == Character.UNASSIGNED 
                    && !UCharacter.hasBinaryProperty(j, UProperty.NONCHARACTER_CODE_POINT)) continue;
                if (type == surrogateType || type == privateUseType) continue;
                gotOne = true;
            }
            if (!gotOne) {
                if (false) {
                    System.out.println("Skipping: " + hex(i>>8,2) + "__");
                    for (int j = i; j < limit; ++j) {
                        System.out.print(UCharacter.getType(j) + ",");
                    }
                    System.out.println();
                }
                continue;
            };
            System.out.println(hex(i>>8,2) + "__");
            columns++;
            pw.print("<tr><th>" + hex(i>>8,2) + "__</th>");
            for (int j = i; j < limit; ++j) {
                String value = "\u00a0";
                String cellclass = gotLast ? "" : " class='d'";
                if (UCharacter.hasBinaryProperty(j, UProperty.NONCHARACTER_CODE_POINT)) {
                    cellclass = gotLast ? " class='n'" : " class='dn'";
                } else if (!UCharacter.isDefined(j)) {
                    cellclass = gotLast ? " class='u'" : " class='du'";
                } else if (UCharacter.isUWhiteSpace(j)) {
                    // nothing
                } else if (UCharacter.hasBinaryProperty(j, UProperty.DEFAULT_IGNORABLE_CODE_POINT)) {
                    cellclass = gotLast ? " class='i'" : " class='di'";
                } else {
                    value = UTF16.valueOf(j);
                }
                pw.print("<td" + cellclass + ">" + value + "</td>");
                if ((j & 0xF) == 0xF) {
                    pw.println();
                }
            }
            pw.println("</tr>");
        }
        pw.println("</table><p>Copyright \u00A9 2003, Mark Davis. All Rights Reserved.</body></html>");
        pw.close();
        System.out.println("columns: " + columns);
    }
    
    static String hex(int i, int padTo) {
        String result = Integer.toHexString(i).toUpperCase(java.util.Locale.ENGLISH);
        while (result.length() < padTo) result = "0" + result;
        return result;
    }
}