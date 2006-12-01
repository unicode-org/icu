/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateNamedSequences.java,v $
* $Date: 2006/04/05 22:12:45 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.*;
import java.io.*;

public final class GenerateNamedSequences implements UCD_Types {
    
    static final boolean DEBUG = false;
    
    static public String showVarGlyphs(String code0, String code1, String shape, String description) {
        if (DEBUG) System.out.println(code0 + ", " + code1 + ", [" + shape + "]");
        
        String abbShape = "";
        if (shape.length() != 0) {
            abbShape = '-' + shape.substring(0,4);
            if (description.indexOf("feminine") >= 0) abbShape += "fem";
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
        
        
        // read the data and compose the table
        
        String table = "<table><tr><th width='10%'>Rep Glyph</th><th>Hex Sequence</th><th>Name</th><th>Copyable</th></tr>";
        
        String[] splits = new String[4];
        String[] codes = new String[20];
        String[] shapes = new String[4];
        
        BufferedReader in = Utility.openUnicodeFile("NamedSequences", Default.ucdVersion(), true, Utility.LATIN1);
        Transliterator unicodexml = Transliterator.getInstance("hex/xml");
        while (true) {
            String line = Utility.readDataLine(in);
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) continue;
            
            int count = Utility.split(line, ';', splits);
            String name = splits[0];
            int codeCount = Utility.split(splits[1], ' ', codes);
            StringBuffer codeBuffer = new StringBuffer();
            for (int i = 0; i < codeCount; ++i) {
            	UTF16.append(codeBuffer, Integer.parseInt(codes[i],16));
            }
            String codeWithHyphens = splits[1].replaceAll("\\s", "-");
            String codeAlt = "U+" + splits[1].replaceAll("\\s", " U+");
            String codeString = unicodexml.transliterate(codeBuffer.toString());
            
            // <img alt="03E2" src="http://www.unicode.org/cgi-bin/refglyph?24-03E2" style="vertical-align:middle">
            
            //table += "<tr><td><img alt='U+" + codes[0] + "' src='http://www.unicode.org/cgi-bin/refglyph?24-" + codes[0] + "'></td>\n";
            String imageName = "images/U" + codeWithHyphens + ".gif";
            if (splits[1].compareTo("1780") >= 0 && splits[1].compareTo("1800") < 0) {
                String codeNoSpaces2 = splits[1].replaceAll("\\s", "");
            	imageName = "http://www.unicode.org/reports/tr28/images/" + codeNoSpaces2 + ".gif";
            }
            table += "<tr>"
               		+ "<td class='copy'><img alt='(" + codeAlt + ")' src='" + imageName + "'><br><tt>"
					+ splits[1] + "</tt></td>"
 					+ "<td>" + splits[1] + "</td>"
					+ "</td><td>" + name + "</td>" 
              		+ "<td class='copy'>" + codeString + "</td>"
					+ "</tr>\n";
            System.out.println(splits[1] + "\t" + codeString);
        }
        in.close();            
        table += "</table>";
     
        // now write out the results
        
        String directory = "DerivedData/";
        String filename = directory + "NamedSequences" + UnicodeDataFile.getHTMLFileSuffix(true);
        PrintWriter out = Utility.openPrintWriter(filename, Utility.LATIN1_UNIX);
        /*
        String[] batName = {""};
        String mostRecent = UnicodeDataFile.generateBat(directory, filename, UnicodeDataFile.getFileSuffix(true), batName);
        
        String version = Default.ucd().getVersion();
        int lastDot = version.lastIndexOf('.');
        String updateDirectory = version.substring(0,lastDot) + "-Update";
        int updateV = version.charAt(version.length()-1) - '0';
        if (updateV != 0) updateDirectory += (char)('1' + updateV);
        if (DEBUG) System.out.println("updateDirectory: " + updateDirectory);
        */
        
        String[] replacementList = {
            "@revision@", Default.ucd().getVersion(),
            //"@updateDirectory@", updateDirectory,
            "@date@", Default.getDate(),
            "@table@", table};
                
        Utility.appendFile("com/ibm/text/UCD/NamedSequences-Template.html", Utility.UTF8, out, replacementList);
     
        out.close();
        //Utility.renameIdentical(mostRecent, Utility.getOutputName(filename), batName[0]);
    }
}
