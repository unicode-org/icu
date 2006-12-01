/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/Compare14652.java,v $
* $Date: 2004/02/07 01:01:16 $
* $Revision: 1.3 $
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
    
    static final boolean oldVersion = false;
    
    public static UnicodeSet getSet(int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue).getSet();
    }
    
    static UnicodeSet
        titleSet = getSet(CATEGORY, Lt),
        combiningSet = getSet(CATEGORY, Mc)
            .addAll(getSet(CATEGORY, Me))
            .addAll(getSet(CATEGORY, Mn)),
        zSet = getSet(CATEGORY, Zs)
            .addAll(getSet(CATEGORY, Zl))
            .addAll(getSet(CATEGORY, Zp)),
        pSet = getSet(CATEGORY, Pd)
            .addAll(getSet(CATEGORY, Ps))
            .addAll(getSet(CATEGORY, Pe))
            .addAll(getSet(CATEGORY, Pc))
            .addAll(getSet(CATEGORY, Po))
            .addAll(getSet(CATEGORY, Pi))
            .addAll(getSet(CATEGORY, Pf)),
        sSet = getSet(CATEGORY, Sm)
            .addAll(getSet(CATEGORY, Sc))
            .addAll(getSet(CATEGORY, Sk))
            .addAll(getSet(CATEGORY, So)),
        noSet = getSet(CATEGORY, No),
        csSet = getSet(CATEGORY, Cs),
        cfSet = getSet(CATEGORY, Cf),
        cnSet = getSet(CATEGORY, Cn),
        circled = getSet(DECOMPOSITION_TYPE, COMPAT_CIRCLE),
        whitespaceSet = getSet(BINARY_PROPERTIES, White_space),
        alphaSet = getSet(DERIVED, PropAlphabetic).addAll(combiningSet),
        lowerSet = getSet(DERIVED, PropLowercase).addAll(titleSet).removeAll(circled),
        upperSet = getSet(DERIVED, PropUppercase).addAll(titleSet).removeAll(circled),
        digitSet = getSet(CATEGORY, Nd),
        xdigitSet = new UnicodeSet("[a-fA-F\uFF21-\uFF26\uFF41-\uFF46]").addAll(digitSet),
        spaceSet = whitespaceSet.size() == 0 ? zSet : whitespaceSet,
        controlSet = getSet(CATEGORY, Cc),
        punctSet = new UnicodeSet(pSet).addAll(sSet),
        graphSet = new UnicodeSet(0,0x10ffff)
            .removeAll(controlSet)
            //.removeAll(getSet(CATEGORY, Cf))
            .removeAll(csSet)
            .removeAll(cnSet)
            .removeAll(zSet),
            // Cc, Cf, Cs, Cn, Z
        blankSet = new UnicodeSet(spaceSet).removeAll(new UnicodeSet("[\\u000A-\\u000D\\u0085]"))
            .removeAll(getSet(CATEGORY, Zl))
            .removeAll(getSet(CATEGORY, Zp));
    

    static class Prop {
        String name;
        UnicodeSet contents = new UnicodeSet();
        String guess = "???";
        UnicodeSet guessContents = new UnicodeSet();
        
        String wsname = whitespaceSet.size() == 0 ? "gc=Z" : "Whitespace";
        
        Prop(String name) {
            this.name = name;
            if (name.equals("alpha")) {
                guess = "Alphabetic + gc=M";
                guessContents = alphaSet;
            } else if (name.equals("lower")) {
                guess = "Lowercase + gc=Lt - dt=circle";
                guessContents = lowerSet;
            } else if (name.equals("upper")) {
                guess = "Uppercase + gc=Lt - dt=circle";
                guessContents = upperSet;
            } else if (name.equals("digit")) {
                guess = "gc=Nd";
                guessContents = digitSet;
            } else if (name.equals("xdigit")) {
                guess = "gc=Nd+a..f (upper/lower,normal/fullwidth)";
                guessContents = xdigitSet;
            } else if (name.equals("space")) {
                guess = wsname;
                guessContents = spaceSet;
                //Utility.showSetNames("Whitespace", spaceSet, true, Default.ucd);
            } else if (name.equals("cntrl")) {
                guess = "gc=Cc";
                guessContents = controlSet;
            } else if (name.equals("punct")) {
                guess = "gc=P,S";
                guessContents = punctSet;
            } else if (name.equals("graph")) {
                guess = "All - gc=Cc, Cs, Cn, or Z";
                guessContents = graphSet;
            } else if (name.equals("blank")) {
                guess = wsname + " - (LF,VT,FF,CR,NEL + gc=Zl,Zp)";
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
            Utility.showSetDifferences(pw, name, contents, guess, guessContents, false, true, null, Default.ucd());
            //pw.println(props[i].contents);
        }
    }
    
    static Prop[] props = new Prop[100];
    static int propCount = 0;
    
    public static void main(String[] args) throws IOException {
        
        String version = Default.ucd().getVersion();
        PrintWriter log = Utility.openPrintWriter("Diff14652_" + version + ".txt", Utility.UTF8_WINDOWS);
        try {
            log.write('\uFEFF');
            log.print("Version: " + version);
            
            if (false) {
                UnicodeSet ID = getSet(DERIVED, ID_Start).addAll(getSet(DERIVED, ID_Continue_NO_Cf));
                UnicodeSet XID = getSet(DERIVED, Mod_ID_Start).addAll(getSet(DERIVED, Mod_ID_Continue_NO_Cf));
                UnicodeSet alphanumSet = new UnicodeSet(alphaSet).addAll(digitSet).addAll(getSet(CATEGORY, Pc));
                
                Utility.showSetDifferences("ID", ID, "XID", XID, false, Default.ucd());
                Utility.showSetDifferences("ID", ID, "Alphabetic+Digit+Pc", alphanumSet, false, Default.ucd());
            }
            
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
        
            for (int i = 0; i < propCount; ++i) props[i].show(log);
            
            log.println();
            log.println("**************************************************");
            log.println("Checking POSIX requirements for inclusion and disjointness.");
            log.println("**************************************************");
            log.println();
/*
alpha, digit, punct, cntrl are all disjoint
space, cntrl, blank are pairwise disjoint with any of alpha, digit, xdigit
alpha includes upper, lower
graph includes alpha, digit, punct
print includes graph
xdigit includes digit
*/
            Prop
                alpha = getProp("ISO_14652_alpha"),
                upper = getProp("ISO_14652_upper"),
                lower = getProp("ISO_14652_lower"),
                graph = getProp("ISO_14652_graph"),
                //print = getProp("ISO_14652_print"),
                punct = getProp("ISO_14652_punct"),
                digit = getProp("ISO_14652_digit"),
                xdigit = getProp("ISO_14652_xdigit"),
                space = getProp("ISO_14652_space"),
                blank = getProp("ISO_14652_blank"),
                cntrl = getProp("ISO_14652_cntrl");
                
            checkDisjoint(log, new Prop[] {alpha, digit, punct, cntrl});
            
            Prop [] l1 = new Prop[] {space, cntrl, blank};
            Prop [] l2 = new Prop[] {alpha, digit, xdigit};
            for (int i = 0; i < l1.length; ++i) {
                for (int j = i + 1; j < l2.length; ++j) {
                    checkDisjoint(log, l1[i], l2[j]);
                }
            }
            checkIncludes(log, alpha, upper);
            checkIncludes(log, alpha, lower);
            checkIncludes(log, graph, alpha);
            checkIncludes(log, graph, digit);
            checkIncludes(log, graph, punct);
            //checkIncludes(log, print, graph);
            checkIncludes(log, xdigit, digit);
            
            
            // possibly alpha, digit, punct, cntrl, space cover the !(Cn,Cs)
            
            UnicodeSet trRemainder = new UnicodeSet(cnSet)
                .complement()
                .removeAll(csSet)
                .removeAll(digit.contents)
                .removeAll(punct.contents)
                .removeAll(alpha.contents)
                .removeAll(cntrl.contents)
                .removeAll(space.contents);
            Utility.showSetNames(log, "TR Remainder: ", trRemainder, false, false, Default.ucd());
                
            UnicodeSet propRemainder = new UnicodeSet(cnSet)
                .complement()
                .removeAll(csSet)
                //.removeAll(noSet)
                //.removeAll(cfSet)
                .removeAll(digit.guessContents)
                .removeAll(punct.guessContents)
                .removeAll(alpha.guessContents)
                .removeAll(cntrl.guessContents)
                .removeAll(space.guessContents);
            Utility.showSetNames(log, "Prop Remainder: ", propRemainder, false, false, Default.ucd());
                
            /*
            checkDisjoint(new Prop[] {alpha, digit, punct, cntrl});
            UnicodeSet remainder = cnSet.complement();
            UnicodeSet guessRemainder = new UnicodeSet(remainder);
            for (int i = 0; i < list.length; ++i) {
                for (int j = i + 1; j < list.length; ++j) {
                    compare(log, list[i].name, list[i].contents, list[j].name, list[j].contents);
                    compare(log, list[i].guess, list[i].guessContents, list[j].guess, list[j].guessContents);
                }
                remainder.removeAll(list[i].contents);
                guessRemainder.removeAll(list[i].guessContents);
            }
            if (remainder.size() != 0) {
                log.println();
                log.println("Incomplete (TR): " + remainder);
            }
            if (guessRemainder.size() != 0) {
                log.println();
                log.println("Incomplete (Prop): " + guessRemainder);
            }
            */
            
        } finally {
            log.close();
        }
    }
    
    static void checkDisjoint(PrintWriter log, Prop[] list) {
        for (int i = 0; i < list.length; ++i) {
            for (int j = i + 1; j < list.length; ++j) {
                checkDisjoint(log, list[i], list[j]);
            }
        }
    }
    
    static void checkDisjoint(PrintWriter log, Prop prop1, Prop prop2) {
        checkDisjoint(log, prop1.name, prop1.contents, prop2.name, prop2.contents);
        checkDisjoint(log, prop1.guess, prop1.guessContents, prop2.guess, prop2.guessContents);
    }
    
    static void checkDisjoint(PrintWriter log, String name, UnicodeSet set, String name2, UnicodeSet set2) {
        if (set.containsSome(set2)) {
            log.println();
            log.println("Fails test: " + name + " disjoint-with " + name2);
            UnicodeSet diff = new UnicodeSet(set).retainAll(set2);
            Utility.showSetNames(log, "", diff, false, false, Default.ucd());
        }
    }
    
    static void checkIncludes(PrintWriter log, Prop prop1, Prop prop2) {
        checkIncludes(log, prop1.name, prop1.contents, prop2.name, prop2.contents);
        checkIncludes(log, prop1.guess, prop1.guessContents, prop2.guess, prop2.guessContents);
    }
    
    static void checkIncludes(PrintWriter log, String name, UnicodeSet set, String name2, UnicodeSet set2) {
        if (!set.containsAll(set2)) {
            log.println();
            log.println("Fails test:" + name + " includes " + name2);
            UnicodeSet diff = new UnicodeSet(set2).removeAll(set);
            Utility.showSetNames(log, "", diff, false, false, Default.ucd());
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
    
    static Prop getProp(String name) {
        //System.out.println("Searching for: " + name);
        for (int i = 0; i < propCount; ++i) {
            //System.out.println("Checking: " + props[i].name);
            if (props[i].name.equals(name)) {
                return props[i];
            }
        }
        //System.out.println("Missed");
        return null;
    }
    
    // oddities: 
        // extra space after ';' <U0300>..<U036F>; <U20D0>..<U20FF>; <UFE20>..<UFE2F>;/
        // <0>?? <0>;<U0BE7>..<U0BEF>;/
        // <U202C>; <U202D>;<U202E>; <UFEFF> : 0;/
       // % "print" is by default "graph", and the <space> character
       // print is odd, since it includes space but not other spaces.
       // alnum not defined.

}
