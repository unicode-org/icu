/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateStandardizedVariants.java,v $
* $Date: 2003/02/26 00:35:09 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.*;
import java.io.*;

public final class GenerateStandardizedVariants implements UCD_Types {
    
    static public String showVarGlyphs(String code0, String code1, String shape) {
        System.out.println(code0 + ", " + code1 + ", [" + shape + "]");
        
        String abbShape = "";
        if (shape.length() != 0) {
            abbShape = '-' + shape.substring(0,4);
            if (shape.endsWith("-feminine")) abbShape += "fem";
        }
        
        return "<img alt='U+" + code0 + "+U+" + code1 + "/" + shape 
            + "' src='http://www.unicode.org/cgi-bin/varglyph?24-" +code0 + "-" + code1 + abbShape + "'>";
    }
    
/*
#   Field 0: the variation sequence
#   Field 1: the description of the desired appearance
#   Field 2: where the appearance is only different in in particular shaping environments
#	this field lists them. The possible values are: isolated, initial, medial, final.
#	If more than one is present, there are spaces between them.
*/
    static public void generate() throws IOException {
        Default.setUCD();
        
        // read the data and compose the table
        
        String table = "<table><tr><th>Rep Glyph</th><th>Character Sequence</th><th>Context</th><th width='10%'>Alt Glyph</th><th>Description of variant appearance</th></tr>";
        
        String[] splits = new String[4];
        String[] codes = new String[2];
        String[] shapes = new String[4];
        
        BufferedReader in = Utility.openUnicodeFile("StandardizedVariants", Default.ucdVersion, true, Utility.LATIN1);
        while (true) {
            String line = Utility.readDataLine(in);
            if (line == null) break;
            if (line.length() == 0) continue;
            
            int count = Utility.split(line, ';', splits);
            int codeCount = Utility.split(splits[0], ' ', codes);
            int code = Utility.codePointFromHex(codes[0]);
            
            // <img alt="03E2" src="http://www.unicode.org/cgi-bin/refglyph?24-03E2" style="vertical-align:middle">
            
            table += "<tr><td><img alt='U+" + codes[0] + "' src='http://www.unicode.org/cgi-bin/refglyph?24-" + codes[0] + "'></td>\n";
            table += "<td>" + splits[0] + "</td>\n";
            
            String shape = splits[2].trim();
            if (shape.equals("all")) shape = "";
            
            table += "<td>" + Utility.replace(shape, " ", "<br>") + "</td>\n";
            
            // http://www.unicode.org/cgi-bin/varglyph?24-1820-180B-fina
            // http://www.unicode.org/cgi-bin/varglyph?24-222A-FE00
            
            table += "<td>";
            if (shape.length() == 0) {
                table += showVarGlyphs(codes[0], codes[1], "");
            } else {
                int shapeCount = Utility.split(shape, ' ', shapes);
                for (int i = 0; i < shapeCount; ++i) {
                    if (i != 0) table += " ";
                    table += showVarGlyphs(codes[0], codes[1], shapes[i]);
                }
            }
            table += "</td>\n";
            
            table += "<td>" + Default.ucd.getName(code) + " " + splits[1] + "</td>\n";
            table += "</tr>";
        }
        in.close();            
        table += "</table>";
     
        // now write out the results
        
        String directory = "DerivedData/";
        String filename = directory + "StandardizedVariants.html";
        PrintWriter out = Utility.openPrintWriter(filename, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = GenerateData.generateBat(directory, filename, GenerateData.getFileSuffix(true), batName);
        
        String[] replacementList = {
            "@revision@", Default.ucd.getVersion(),
            "@date@", Default.getDate(),
            "@table@", table};
                
        Utility.appendFile("StandardizedVariants-Template.html", Utility.UTF8, out, replacementList);
     
        out.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(filename), batName[0]);
    }
}
