/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/Compare14652.java,v $
* $Date: 2003/04/23 19:01:21 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

// quick and dirty function for grabbing contents of ISO 14652 file

public class Compare14652 implements UCD_Types {
    
    public static UnicodeSet getSet(int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue).getSet();
    }
    
    static UnicodeSet
        titleSet = getSet(CATEGORY, Lt),
        combiningSet = getSet(CATEGORY, Mc)
            .addAll(getSet(CATEGORY, Me))
            .addAll(getSet(CATEGORY, Mn)),
        alphaSet = getSet(DERIVED, PropAlphabetic).addAll(combiningSet),
        lowerSet = getSet(DERIVED, PropLowercase).addAll(titleSet),
        upperSet = getSet(DERIVED, PropUppercase).addAll(titleSet),
        digitSet = getSet(CATEGORY, Nd),
        xdigitSet = new UnicodeSet("[a-fA-F\uFF21-\uFF26\uFF41-\uFF46]").addAll(digitSet),
        spaceSet = getSet(BINARY_PROPERTIES, White_space),
        controlSet = getSet(CATEGORY, Cc),
        punctSet = getSet(CATEGORY, Pd)
            .addAll(getSet(CATEGORY, Ps))
            .addAll(getSet(CATEGORY, Pe))
            .addAll(getSet(CATEGORY, Pc))
            .addAll(getSet(CATEGORY, Po))
            .addAll(getSet(CATEGORY, Pi))
            .addAll(getSet(CATEGORY, Pf)),
        graphSet = new UnicodeSet(0,0x10ffff)
            .removeAll(controlSet)
            //.removeAll(getSet(CATEGORY, Cf))
            .removeAll(getSet(CATEGORY, Cs))
            .removeAll(getSet(CATEGORY, Cn))
            .removeAll(getSet(CATEGORY, Zs))
            .removeAll(getSet(CATEGORY, Zl))
            .removeAll(getSet(CATEGORY, Zp)),
            // Cc, Cf, Cs, Cn, Z
        blankSet = new UnicodeSet(spaceSet).removeAll(new UnicodeSet("[\\u000A-\\u000D\\u0085]"))
            .removeAll(getSet(CATEGORY, Zl))
            .removeAll(getSet(CATEGORY, Zp));
    

    static class Prop {
        String name;
        UnicodeSet contents = new UnicodeSet();
        String guess = "???";
        UnicodeSet guessContents = new UnicodeSet();
        
        Prop(String name) {
            this.name = name;
            if (name.equals("alpha")) {
                guess = "Alphabetic + gc=M";
                guessContents = alphaSet;
            } else if (name.equals("lower")) {
                guess = "Lowercase + gc=Lt";
                guessContents = lowerSet;
            } else if (name.equals("upper")) {
                guess = "Uppercase + gc=Lt";
                guessContents = upperSet;
            } else if (name.equals("digit")) {
                guess = "gc=Nd";
                guessContents = digitSet;
            } else if (name.equals("xdigit")) {
                guess = "gc=Nd+a..f (upper/lower,normal/fullwidth)";
                guessContents = xdigitSet;
            } else if (name.equals("space")) {
                guess = "Whitespace";
                guessContents = spaceSet;
                Utility.showSetNames("Whitespace", spaceSet, true, Default.ucd);
            } else if (name.equals("cntrl")) {
                guess = "gc=Cc";
                guessContents = controlSet;
            } else if (name.equals("punct")) {
                guess = "gc=P";
                guessContents = punctSet;
            } else if (name.equals("graph")) {
                guess = "All - gc=Cc, Cs, Cn, or Z";
                guessContents = graphSet;
            } else if (name.equals("blank")) {
                guess = "Whitespace - (LF,VT,FF,CR,NEL + gc=Zl,Zp)";
                guessContents = blankSet;
            } else if (name.equals("ISO_14652_class \"combining\"")) {
                guess = "gc=M";
                guessContents = combiningSet;
            }
            
            
/*upper
lower
alpha
digit
outdigit
space
cntrl
punct
graph
xdigit
blank
toupper
tolower
*/
        }
        
        void show(PrintWriter pw) {
            if (name.equals("ISO_14652_LC_CTYPE")) return;
            if (name.equals("ISO_14652_toupper")) return;
            if (name.equals("ISO_14652_tolower")) return;
            if (name.equals("ISO_14652_outdigit")) return;
            if (name.equals("ISO_14652_outdigit")) return;
            if (name.startsWith("ISO_14652_class")) return;
            
            pw.println();
            pw.println("**************************************************");
            pw.println(name);
            pw.println("**************************************************");
            Utility.showSetDifferences(pw, name, contents, guess, guessContents, false, true, null, Default.ucd);
            //pw.println(props[i].contents);
        }
    }
    
    static Prop[] props = new Prop[100];
    static int propCount = 0;
    
    public static void main(String[] args) throws IOException {
        BufferedReader br = Utility.openReadFile("C:\\DATA\\ISO14652_CTYPE.txt", Utility.LATIN1);
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.charAt(line.length() - 1) == '/') {
                line = line.substring(0, line.length() - 1);
            }
            line = line.trim();
            if (line.length() == 0) continue;
            
            char ch = line.charAt(0);
            if (ch == '%') continue;
            if (ch == '(') continue;
            if (ch == '<') {
                addItems(line, props[propCount-1].contents);
            } else {
                // new property
                System.out.println(line);
                if (line.equals("width")) break;
                props[propCount] = new Prop(line);
                props[propCount].name = "ISO_14652_" + line;
                props[propCount].contents = new UnicodeSet();
                propCount++;
            }
        }
        
        PrintWriter log = Utility.openPrintWriter("Diff14652.txt", Utility.UTF8_WINDOWS);
        log.write('\uFEFF');
        try {
            for (int i = 0; i < propCount; ++i) props[i].show(log);
        } finally {
            log.close();
        }
    }
    
    static String[] pieces = new String[100];
    
    // example: <U1F48>..<U1F4D>;<U1F59>;<U1F5B>;<U1F5D>;<U1F5F>;<U1F68>..<U1F6F>;/
    static void addItems(String line, UnicodeSet contents) {
        int len = Utility.split(line, ';', pieces);
        for (int i = 0; i < len; ++i) {
            String piece = pieces[i].trim();
            if (piece.length() == 0) continue;
            if (piece.equals("<0>")) continue;
            int start, end;
            int rangePoint = piece.indexOf("..");
            if (rangePoint >= 0) {
                start = parse(piece.substring(0,rangePoint));
                end = parse(piece.substring(rangePoint+2));
            } else {
                start = end = parse(piece);
            }
            contents.add(start, end);
        }
    }
    
    static int parse(String piece) {
        if (!piece.startsWith("<U") || !piece.endsWith(">")) {
            throw new IllegalArgumentException("Bogus code point: " + piece);
        }
        return Integer.parseInt(piece.substring(2,piece.length()-1), 16);
    }
    
    // oddities: 
        // extra space after ';' <U0300>..<U036F>; <U20D0>..<U20FF>; <UFE20>..<UFE2F>;/
        // <0>?? <0>;<U0BE7>..<U0BEF>;/
        // <U202C>; <U202D>;<U202E>; <UFEFF> : 0;/
       // % "print" is by default "graph", and the <space> character
       // print is odd, since it includes space but not other spaces.
       // alnum not defined.

}
